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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity {
    ImageView spectrogram;
    Button btn_startstop;
    ImageButton btn_adj_up;
    ImageButton btn_adj_down;
    Button btn_snr_reset;
    TextView stateText;
    TextView timerText;

    TextView lowFreqSNR;
    TextView midFreqSNR;
    TextView highFreqSNR;

    SpectrogramHelper spectrogramHelper;
    SNRHelper snrHelper;
    AudioAnalyzer audioAnalyzer;
    SendReceive sendReceive;
    SocketHandler socketHandler;

    boolean recording;
    private final String TAG = "CommAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);
        initWork();
        initOnClicks();

        new AndroidFFMPEGLocator(this);
    }

    private void initWork() {
        spectrogram     = findViewById(R.id.spectrogram);
        btn_startstop   = findViewById(R.id.btn_startstop);
        btn_adj_down    = findViewById(R.id.adjustDownButton);
        btn_adj_up      = findViewById(R.id.adjustUpButton);
        btn_snr_reset = findViewById(R.id.snrResetButton);
        lowFreqSNR      = findViewById(R.id.lowFreqSNR);
        midFreqSNR      = findViewById(R.id.midFreqSNR);
        highFreqSNR     = findViewById(R.id.highFreqSNR);
        stateText       = findViewById(R.id.stateTextView);
        timerText       = findViewById(R.id.timerTextView);
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 512);
        snrHelper = new SNRHelper(5*44100/256, 512);
        stateText.setText("Hold your breath");
        socketHandler = new SocketHandler();
    }

    private void initOnClicks() {
        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if(!recording) {
                        recording = true;
                        btn_startstop.setText("STOP");
                        sendReceive = new SendReceive(socketHandler.getSocket());
                        sendReceive.start();
                        sendReceive.write("STARTRECORD".getBytes());
                        displaySoundData();
                        startTimer();
                    } else {
                        recording = false;
                        btn_startstop.setText("START");
                        sendReceive.write("STOPRECORD".getBytes());
                    }
                }
        });
        btn_adj_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spectrogramHelper != null){
                    spectrogramHelper.changeIntensity(0.90);
                }
            }
        });
        btn_adj_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spectrogramHelper != null){
                    spectrogramHelper.changeIntensity(1.10);
                }
            }
        });
        btn_snr_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(snrHelper.getState() == SNRHelper.SAMPLE_STATE) {
                    startTimer();
                }
            }
        });
    }

    private void startTimer() {
        snrHelper.setState(SNRHelper.NOISE_STATE);
        stateText.setText("Hold your breath");
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

    private void displaySoundData() {
        new Thread() {
            public void run() {
                while (recording) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DecimalFormat df = new DecimalFormat();
                            df.setMaximumFractionDigits(3);
                            df.setMinimumFractionDigits(3);
                            Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            whitePaint.setColor(Color.WHITE);
                            if (audioAnalyzer != null) {
                                Bitmap spectrogramBMap = spectrogramHelper.getBitmap();
                                Bitmap tempBitmap = Bitmap.createBitmap(spectrogram.getWidth(), spectrogram.getHeight(), Bitmap.Config.RGB_565);

                                Canvas tempCanvas = new Canvas(tempBitmap);
                                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                paint.setColor(Color.BLACK);
                                paint.setTextSize(32f);
                                tempCanvas.drawRect(0,0,tempCanvas.getWidth(),tempCanvas.getHeight(),whitePaint);
                                tempCanvas.drawBitmap(spectrogramBMap, null, new RectF(48,32, tempCanvas.getWidth(), tempCanvas.getHeight()),null);
                                for (int i=0;i<11;i++) {
                                    if(i<10)
                                        tempCanvas.drawText(" "+i+"k _",0,(int) ((10-i)*(tempCanvas.getHeight()-32)/10) + 32, paint);
                                    else
                                        tempCanvas.drawText(i+"k _",0,(int) ((10-i)*(tempCanvas.getHeight()-32)/10) + 32, paint);
                                }
                                spectrogram.setImageBitmap(tempBitmap);

                                double lowSNR = snrHelper.getSNR(44,1000);
                                double midSNR = snrHelper.getSNR(1000,3000);
                                double highSNR = snrHelper.getSNR(3000,10000);
                                if (lowSNR < 3.0 || midSNR < 3.0 || highSNR < 3.0){
                                    lowSNRWarning();
                                }

                                lowFreqSNR.setText  ("44Hz-1kHz: \t"+df.format(lowSNR));
                                midFreqSNR.setText  ("1kHz-3kHz: \t"+df.format(midSNR));
                                highFreqSNR.setText ("3kHz-10kHz:\t"+df.format(highSNR));

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

    public void lowSNRWarning(){
        // TODO: display a popup warning the user about the low SNR value and instructing them to move the microphone
        Log.i(TAG, "Low SNR Value");
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
            while (recording) {
                try {
                    // read the incoming file into a byte array
                    if(inputStream.read(header_buffer) == 44){
//                        String debug_RIFF = new String(Arrays.copyOfRange(header_buffer,0,4), StandardCharsets.UTF_8);
//                        Log.d(TAG,"Should be RIFF: " + debug_RIFF);
                        ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer,40,44)).order(ByteOrder.LITTLE_ENDIAN);
                        int size = wrappedSize.getInt() + 44;
                        audioByteArray = new byte[size];
                        System.arraycopy(header_buffer,0,audioByteArray,0,44); // add the header to the audio byte array
                        int pointer = 44;
                        //Log.i(TAG,"Ready to read file of size: "+size);
                        while( pointer < size) {
                            // Log.i(TAG, "Read "+pointer+ "/"+size+" bytes");
                            int count = inputStream.read(audioByteArray, pointer, size-pointer);
                            pointer += count;
                        }
//                        Log.i(TAG, "Starting AudioDispatcher from recieved file.");
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
