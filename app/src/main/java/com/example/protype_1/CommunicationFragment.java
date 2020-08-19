package com.example.protype_1;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class CommunicationFragment extends Fragment {
    private static final String TAG = "CommFrag";

    interface TimerCallback {
        void updateTimer(long milliseconds);
    }

    private TimerCallback callback;
    private SpectrogramHelper spectrogramHelper;
    private SNRHelper snrHelper;
    private SocketHandler socketHandler;
    private AudioAnalyzer audioAnalyzer;
    private SendReceive sendReceive;

    private boolean recording;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 512);
        snrHelper = new SNRHelper(5*44100/256, 512);
        socketHandler = new SocketHandler();

        recording = false;

        sendReceive = new SendReceive(socketHandler.getSocket());
        sendReceive.start();

    }

    public boolean isRecording(){return recording;}

    public void startRecording(){
        recording = true;
        sendReceive.write("STARTRECORD".getBytes());
    }

    public void stopRecording(){
        recording = false;
        sendReceive.write("STOPRECORD".getBytes());
    }

    public SpectrogramHelper getSpectrogramHelper(){
        return spectrogramHelper;
    }

    public SNRHelper getSnrHelper() {
        return snrHelper;
    }

    public void setTimerCallback(TimerCallback callback){
        this.callback = callback;
    }

    public void initSnr(){
        if(snrHelper.getState() == SNRHelper.SAMPLE_STATE) {
            snrHelper.setState(SNRHelper.NOISE_STATE);
            new CountDownTimer(10000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    callback.updateTimer(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    callback.updateTimer(0);
                    snrHelper.setState(SNRHelper.SAMPLE_STATE);
                }
            }.start();
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
                int bufferSize = 1024;
                audioAnalyzer = new AudioAnalyzer(44100, bufferSize, bufferSize * 3 / 4, spectrogramHelper, snrHelper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            byte[] header_buffer = new byte[44]; //used to detect the start of a file
            byte[] audioByteArray;
            while (true) {
                //keep thread alive if not recording
                if (recording) {
                    try {
                        // read the incoming file into a byte array
                        if (inputStream.read(header_buffer) == 44) {
                            ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer, 40, 44)).order(ByteOrder.LITTLE_ENDIAN);
                            int size = wrappedSize.getInt() + 44;
                            audioByteArray = new byte[size];
                            System.arraycopy(header_buffer, 0, audioByteArray, 0, 44); // add the header to the audio byte array
                            int pointer = 44;
                            while (pointer < size) {
                                int count = inputStream.read(audioByteArray, pointer, size - pointer);
                                pointer += count;
                            }
                            BufferedInputStream audioInputStream = new BufferedInputStream(new ByteArrayInputStream(audioByteArray));
                            audioAnalyzer.initDispatcher(audioInputStream);
                            audioAnalyzer.startAnalyzer();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
