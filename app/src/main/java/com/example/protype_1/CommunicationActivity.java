package com.example.protype_1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity {
    ImageView spectrogram;
    Button btn_startstop;
    Button btn_reset;
    TextView message;

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
                                        spectrogram.setImageBitmap(audioAnalyzer.getSpectrogramBitmap());
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

                inputStream = new BufferedInputStream(socket.getInputStream());
                outputStream = socket.getOutputStream();
                int buffersize = 1024;
                audioAnalyzer = new AudioAnalyzer(inputStream, 44100, buffersize, buffersize * 3 / 4);
                audioAnalyzer.initDispatcher(5, 10000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        @Override
        public void run() {
            byte [] buf = new byte[5]; //used to detect the "START" command
            while (true) {
                // Receive audio files from the server
                if(audioAnalyzer.isStopped()){
                    try {
                        inputStream.read(buf, 0, 5);
                        if (new String(buf, 0, buf.length).equals("START")) {
                            audioAnalyzer.startAnalyzer();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                drawSpectrogram();
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
