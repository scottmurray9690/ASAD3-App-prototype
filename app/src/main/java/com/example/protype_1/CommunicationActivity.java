package com.example.protype_1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Arrays;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity {
    ImageView spectrogram;
    Button btn_startstop;
    Button btn_reset;
    TextView message;

    SpectrogramHelper spectrogramHelper;
    AudioAnalyzer audioAnalyzer;
    SendReceive sendReceive;
    ClientClass clientClass;

    boolean recording;
    private final String TAG = "CommunicationActivity";

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
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 1024);
    }

    private void initOnClicks() {
        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendReceive != null) {
                    if(!recording) {
                        recording = true;
                        btn_startstop.setText("STOP");
                        sendReceive.write("STARTRECORD".getBytes());

                    } else {
                        recording = false;
                        btn_startstop.setText("START");
                        sendReceive.write("STOPRECORD".getBytes());
                    }
                }
            }
        });
        btn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    private void drawSpectrogram() {
        new Thread() {
            public void run() {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (audioAnalyzer != null) {
                                        spectrogram.setImageBitmap(spectrogramHelper.getBitmap());
                                    }
                                }
                            });
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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

        @Override
        public void run() {
            try {
                message.setText("Attempting to connect to "+hostAddress+":8888...");
                socket.connect(new InetSocketAddress(hostAddress,8888), 500);
                message.setText("Connected to "+hostAddress+":8888.");
                sendReceive = new SendReceive(socket);
                sendReceive.start();
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
                audioAnalyzer = new AudioAnalyzer(44100, buffersize, buffersize * 3 / 4, spectrogramHelper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            byte [] header_buffer = new byte[44]; //used to detect the start of a file
            byte [] read_buffer = new byte[1024];
            byte[] audioByteArray;
            while (true) {
                try {
                    // read the incoming file into a byte array
                    if(inputStream.read(header_buffer) == 44){
                        ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer,40,44));
                        int size = wrappedSize.getInt();
                         audioByteArray = new byte[size+44];
                        System.arraycopy(header_buffer,0,audioByteArray,0,44); // add the header to the audio byte array
                        int pointer = 44;
                        while(inputStream.read(read_buffer) != -1) {
                            System.arraycopy(read_buffer,0,audioByteArray,pointer,1024);
                            pointer += 1024;
                        }
                        ByteArrayInputStream audioInputStream = new ByteArrayInputStream(audioByteArray);
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
