package com.example.protype_1;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

public class CommunicationFragment extends Fragment  {
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
    // Recording stuff
    private AudioRecorder audioRecorder;

    private CommunicationViewModel model;

    private File tempFile;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = new ViewModelProvider(requireActivity()).get(CommunicationViewModel.class);

        setRetainInstance(true);
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 512);
        snrHelper = new SNRHelper(5*44100/256, 512);
        socketHandler = new SocketHandler();

        model.getRecording().setValue(false);

        sendReceive = new SendReceive(socketHandler.getSocket());
        sendReceive.start();

        audioRecorder = new AudioRecorder();
    }

    @Override
    public void onDestroy() {
        try {
            socketHandler.getSocket().close();
            deleteTempFiles(getActivity().getCacheDir());
            Log.i(TAG,"closed socket: "+socketHandler.getSocket().isClosed());
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void startRecording(){
        sendReceive.clean = true;

        model.getRecording().setValue(true);
        sendReceive.write("STARTRECORD".getBytes());
        try {
            // consider making a global variable for safe deletion
            tempFile = getTempFile();
            audioRecorder.setTempFile(tempFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void saveRecordingPrompt() {
        DialogFragment saveRecordingDialog = new SaveRecordingDialog("Recording Stopped.");
        saveRecordingDialog.show(getFragmentManager(), "Save_Recording");
    }

    public void stopRecording(){
        model.getRecording().postValue(false);
        sendReceive.write("STOPRECORD".getBytes());
    }

    public void saveRecording() {
        try {
            // need to prompt user to find location for saving
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"ASAD3"+File.separator+"recordings";
            String fileName = "Recording"+ LocalTime.now().toString().replace(".",":") +".wav";
            String filePath = baseDir +  File.separator + fileName;
            File directory = new File(baseDir);
            directory.mkdirs();
            File saveFile = new File(filePath);

            audioRecorder.saveRecording(saveFile);
            Log.i(TAG, "Saved recording at " + filePath);

            deleteTempFiles(tempFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
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

    private File getTempFile() throws IOException {
        String filename = UUID.randomUUID().toString().replaceAll("-", "");
        File tempFileDir = getActivity().getCacheDir();
        File tempFile = File.createTempFile(filename, ".wav", tempFileDir);
        return tempFile;
    }

    public void deleteTempFiles(){
        deleteTempFiles(tempFile);
    }

    private void deleteTempFiles(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteTempFiles(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        Log.i(TAG, "Deleting file: "+file.getPath());
        file.delete();
    }



    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public boolean clean = false;

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
            int size;
            //keep thread alive if not recording
            while (true) {
                // if (recording)
                if (model.getRecording().getValue()) {
                    try {
                           // read the incoming file into a byte array
                            if (inputStream.read(header_buffer) == 44) {
                                ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer, 40, 44)).order(ByteOrder.LITTLE_ENDIAN);
                                size = wrappedSize.getInt() + 44;
                                if (size != 17684) {
                                    byte[] riff = Arrays.copyOfRange(header_buffer, 0, 4);
                                    Log.i(TAG, "READING FRAME ISSUE, got size as:" + size + "\nHeader starts with: " + new String(riff, StandardCharsets.UTF_8));
                                    StringBuilder incorrectHeader = new StringBuilder("Header in bytes: ");
                                    for (int i = 0; i < 44; i++) {
                                        incorrectHeader.append(String.format("%02x ", header_buffer[i]));
                                    }
                                    Log.i(TAG, incorrectHeader.toString());
                                    // stop the recording.
                                    stopRecording();
                                    long skipped = inputStream.skip(inputStream.available());
                                    Log.i(TAG, "Skipped " + skipped + " bytes");
                                    // display a popup
                                    DialogFragment commErrorDialog = new SaveRecordingDialog("Communication error occurred. Recording Stopped.");
                                    commErrorDialog.show(getFragmentManager(), "Communication_Error");
                                } else {
                                    //Read the data
                                    audioByteArray = new byte[size];
                                    System.arraycopy(header_buffer, 0, audioByteArray, 0, 44); // add the header to the audio byte array
                                    int pointer = 44;
                                    while (pointer < size) {
                                        int count = inputStream.read(audioByteArray, pointer, size - pointer);
                                        pointer += count;
                                    }

                                    audioRecorder.writeData(Arrays.copyOfRange(audioByteArray, 44, audioByteArray.length));
                                    // end audioRecorder
                                    BufferedInputStream audioInputStream = new BufferedInputStream(new ByteArrayInputStream(audioByteArray));
                                    audioAnalyzer.initDispatcher(audioInputStream);
                                    audioAnalyzer.startAnalyzer();
                                }
                            }


                    } catch (IOException e) {
                        Log.i(TAG, "IOException occurred: " + e.getMessage());
                        Intent intent = new Intent(getActivity(), Ins4Activity.class);
                        startActivity(intent);
                        break;
                    } catch (NegativeArraySizeException ne) {
                        Log.i(TAG, "NegativeArraySizeException occured:" + ne.getMessage());
                        getActivity().onBackPressed();
                        Intent intent = new Intent(getActivity(), Ins4Activity.class);
                        startActivity(intent);
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
