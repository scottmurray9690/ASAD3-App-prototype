package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity {
    ImageView spectrogram;
    Button btn_startstop;
    Button btn_reset;
    TextView message;
    TextView SNR_value;
    TextView stateText;
    TextView timerText;

    SpectrogramHelper spectrogramHelper;
    SNRHelper snrHelper;
    AudioAnalyzer audioAnalyzer;
    SendReceive sendReceive;
    ClientClass clientClass;

    boolean recording;
    private final String TAG = "CommAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);
        initWork();
        initOnClicks();

        new AndroidFFMPEGLocator(this);
        try {
            clientClass = new ClientClass(InetAddress.getByName("192.168.50.1"));
            clientClass.start();
        } catch (UnknownHostException e) {
            message.setText("Error: "+e.toString());
            e.printStackTrace();
        }

    }

    private void initWork() {
        spectrogram = findViewById(R.id.spectrogram);
        btn_reset = findViewById(R.id.btn_reset);
        btn_startstop = findViewById(R.id.btn_startstop);
        message = findViewById(R.id.textView);
        SNR_value = findViewById(R.id.SNR_value);
        stateText = findViewById(R.id.stateTextView);
        timerText = findViewById(R.id.timerTextView);
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 1024);
        snrHelper = new SNRHelper(5*44100/256, 1024);
        stateText.setText("Hold your breath");
    }

    private void initOnClicks() {
        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if(!recording) {
                        recording = true;
                        btn_startstop.setText("STOP");
                        sendReceive = new SendReceive(clientClass.getSocket());
                        sendReceive.start();
                        sendReceive.write("STARTRECORD".getBytes());
                        drawSpectrogram();
                        startTimer();
                    } else {
                        recording = false;
                        btn_startstop.setText("START");
                        sendReceive.write("STOPRECORD".getBytes());
                    }
                }
        });
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void startTimer() {
        snrHelper.setState(SNRHelper.NOISE_STATE);
        new CountDownTimer(10000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format("%d", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timerText.setText("");
                stateText.setText("Breathe normally");
                snrHelper.setState(SNRHelper.SAMPLE_STATE);
            }
        }.start();
    }

    private void drawSpectrogram() {
        new Thread() {
            public void run() {
                while (recording) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DecimalFormat df = new DecimalFormat();
                            df.setMaximumFractionDigits(3);
                            if (audioAnalyzer != null) {
                            //    Log.i(TAG, "Drawing Spectrogram to ImageView");
                                Bitmap spectrogramBMap = spectrogramHelper.getBitmap();
                                Bitmap tempBitmap = Bitmap.createBitmap(spectrogram.getWidth(), spectrogram.getHeight(), Bitmap.Config.RGB_565);

                                Canvas tempCanvas = new Canvas(tempBitmap);
                                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                paint.setTextSize(message.getTextSize());
                                paint.setColor(Color.BLACK);
                                tempCanvas.drawBitmap(spectrogramBMap, null, new RectF(0,0, tempCanvas.getWidth(), tempCanvas.getHeight()),null);
                                for (int i=0;i<11;i++) {
                                    tempCanvas.drawText("_ "+i+" kHz",0,(int) ((10-i)*(tempCanvas.getHeight())/10) + 1, paint);
                                }
                                spectrogram.setImageBitmap(tempBitmap);


                                SNR_value.setText("100-1k: "+df.format(snrHelper.getSNR(100,1000))+
                                        "\n1k-4k: "+df.format(snrHelper.getSNR(1000,4000))+
                                        "\n4k+: "+df.format(snrHelper.getSNR(4000,10000)));
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private class ClientClass extends Thread {
        private Socket socket;
        private String hostAddress;
        public ClientClass(InetAddress hostAddress){
            this.hostAddress = hostAddress.getHostAddress();
            socket = new Socket();
            message.setText("socket created");
        }

        public Socket getSocket() {
            return socket;
        }

        @Override
        public void run() {
            try {
                message.setText("Attempting to connect to "+hostAddress+":8888...");
                socket.connect(new InetSocketAddress(hostAddress,8888), 500);
                message.setText("Connected to "+hostAddress+":8888.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket socket) {
            this.socket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                int buffersize = 1024;
                audioAnalyzer = new AudioAnalyzer(44100, buffersize, buffersize * 3 / 4, spectrogramHelper, snrHelper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            byte [] header_buffer = new byte[44]; //used to detect the start of a file
            byte[] audioByteArray;
            int countChocula = 0;
            while (recording) {
                try {
                    // read the incoming file into a byte array
                    if(inputStream.read(header_buffer) == 44){
                        String debug_RIFF = new String(Arrays.copyOfRange(header_buffer,0,4), StandardCharsets.UTF_8);
                     //   Log.d(TAG,"Should be RIFF: " + debug_RIFF);

                        ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer,40,44)).order(ByteOrder.LITTLE_ENDIAN);
                        int size = wrappedSize.getInt() + 44;
                        audioByteArray = new byte[size];
                        System.arraycopy(header_buffer,0,audioByteArray,0,44); // add the header to the audio byte array
                        int pointer = 44;
                        //Log.i(TAG,"Ready to read file #"+countChocula++ + ", size: "+size);
                        while( pointer < size) {
                            // Log.i(TAG, "Read "+pointer+ "/"+size+" bytes");
                            int count = inputStream.read(audioByteArray, pointer, size-pointer);
                            pointer += count;
                        }
//                        Log.i(TAG, "Starting AudioDispatcher from recieved file.");
//                        wait();
                        BufferedInputStream audioInputStream = new BufferedInputStream(new ByteArrayInputStream(audioByteArray) );
                        audioAnalyzer.initDispatcher(audioInputStream);
                        audioAnalyzer.startAnalyzer();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Receive audio files from the server

            }
        }

        // Send commands to the server
        public void write(final byte[] bytes) {
            if (socket.isConnected()) {
                new Thread(new Runnable() {
                    public void run () {
                        try {
                            outputStream.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Message send failed: "+e.toString());
                        }
                    }
                }).start();
            }
        }
    }
}
