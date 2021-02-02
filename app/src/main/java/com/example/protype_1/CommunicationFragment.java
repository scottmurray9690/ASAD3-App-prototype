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

/**
 * CommunicationFragment
 * Does all of the heavy lifting for CommunicationActivity and runs in the background
 *
 * Uses a bunch of helper classes:
 *  AudioAnalyzer
 *      Class which does all the FFT analysis required for the spectrogram. (using TarsosDSP library: https://github.com/JorenSix/TarsosDSP)
 *      Stores results in SNRHelper and SpectrogramHelper
 *  AudioRecorder
 *      Stores the raw audio data in a .wav file, can be saved or deleted after recording.
 *  SaveRecordingDialog
 *      A popup dialog that asks users if they want to save their recording when recording is stopped.
 *      The message is customizable so you can tell users what stopped the recording.
 *  SNRHelper
 *      Called by AudioAnalyzer
 *      Used to calculate the SNR based on silent recordings and breathing recordings.
 *  SpectrogramHelper
 *      Called by AudioAnalyzer
 *      generates the spectrogram from the data.
 */
public class CommunicationFragment extends Fragment  {
    // Tag for logging
    private static final String TAG = "CommFrag";

    // interface for the activity to update the timer
    interface TimerCallback {
        void updateTimer(long milliseconds);
    }
    // Used to refer to the activity with timer UI element
    private TimerCallback callback;

    // Used to generate the image for the spectrogram
    private SpectrogramHelper spectrogramHelper;

    // Used to calculate the SNR from data
    private SNRHelper snrHelper;

    // Same socketHandler from SocketConnectionActivity, keeps track of the socket connection
    private SocketHandler socketHandler;

    // Does all the FFT analysis and calls SNRHelper and SpectrogramHelper
    private AudioAnalyzer audioAnalyzer;

    // Class used to send and receive data over the socket via threads.
    private SendReceive sendReceive;

    // USed to save the raw audio bytes in a file
    private AudioRecorder audioRecorder;

    // Keeps track of whether or not the device is recording
    private CommunicationViewModel model;

    // Temporarily stores audio data before it is saved into a permanent fie or deleted.
    private File tempFile;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set up ViewModel
        model = new ViewModelProvider(requireActivity()).get(CommunicationViewModel.class);
        // Run in background
        setRetainInstance(true);
        // Set up spectrogram and SNR helper, width for spectrogram is 20s (of data), width for SNRHelper is 5s
        spectrogramHelper = new SpectrogramHelper(20*44100/256, 512);
        snrHelper = new SNRHelper(5*44100/256, 512);

        socketHandler = new SocketHandler();
        // Initially not recording.
        model.getRecording().setValue(false);

        // Start communicating immediately, socket connection already established in last activity
        sendReceive = new SendReceive(socketHandler.getSocket());
        sendReceive.start();

        audioRecorder = new AudioRecorder();
    }

    /**
     * Clean up temp files and close all connections when exiting the app.
     */
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

    /**
     * Send the device the signal to start recording, and get ready to start analyzing/saving the audio data
     */
    public void startRecording(){
        // This was an idea I had to clean out any data yet to be sent by the raspberry pi
        //sendReceive.clean = true;

        model.getRecording().setValue(true);
        // Tell the raspberry pi (ASAD3) to start recording
        sendReceive.write("STARTRECORD".getBytes());
        try {
            // consider making a global variable for safe deletion
            tempFile = getTempFile();
            audioRecorder.setTempFile(tempFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Prompts the user to save the recording using SaveRecordingDialog
     */
    public void saveRecordingPrompt() {
        DialogFragment saveRecordingDialog = new SaveRecordingDialog("Recording Stopped.");
        saveRecordingDialog.show(getFragmentManager(), "Save_Recording");
    }

    /**
     * Stops receiving data and tells the raspberry pi to stop sending data.
     */
    public void stopRecording(){
        model.getRecording().postValue(false);
        sendReceive.write("STOPRECORD".getBytes());
    }

    /**
     * Copies the recording to a file on the phones storage rather than a temporary file,
     * and deletes the temporary file.
     */
    public void saveRecording() {
        try {
            // need to prompt user to find location for saving
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"ASAD3"+File.separator+"recordings";
            String fileName = "Recording"+ LocalTime.now().toString().replace(".",":") +".wav";
            String filePath = baseDir +  File.separator + fileName;
            // Make the directory if it doesn't exist
            File directory = new File(baseDir);
            directory.mkdirs();

            File saveFile = new File(filePath);
            // Save the recording
            audioRecorder.saveRecording(saveFile);
            Log.i(TAG, "Saved recording at " + filePath);
            // Delete temporary files
            deleteTempFiles(tempFile);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Simple getter to be used by CommunicationActivity
     * @return spectrogramHelper which stores the current state of the spectrogram
     */
    public SpectrogramHelper getSpectrogramHelper(){
        return spectrogramHelper;
    }

    /**
     * Simple getter to be used by CommunicationActivity
     * @return snrHelper which stores current SNR values
     */
    public SNRHelper getSnrHelper() {
        return snrHelper;
    }

    /**
     * Setter used to set CommunicationActivity as the callback, so it responds to the countdown timer
     */
    public void setTimerCallback(TimerCallback callback){
        this.callback = callback;
    }

    /**
     * reset the SNR and instruct the user to hold their breath for 10s
     */
    public void initSnr(){
        if(snrHelper.getState() == SNRHelper.SAMPLE_STATE) {
            snrHelper.setState(SNRHelper.NOISE_STATE);
            new CountDownTimer(10000, 1000) {
                // Update the on screen timer
                @Override
                public void onTick(long millisUntilFinished) {
                    callback.updateTimer(millisUntilFinished);
                }
                // Collect sample data when finished with noise data
                @Override
                public void onFinish() {
                    callback.updateTimer(0);
                    snrHelper.setState(SNRHelper.SAMPLE_STATE);
                }
            }.start();
        }
    }

    /**
     * Gets a temporary file to store audio data in while recording.
     * @return temporary file with a random name
     */
    private File getTempFile() throws IOException {
        String filename = UUID.randomUUID().toString().replaceAll("-", "");
        File tempFileDir = getActivity().getCacheDir();
        return File.createTempFile(filename, ".wav", tempFileDir);
    }

    /**
     * a public version of deleteTempFiles(File file)
     * used by CommunicationActivity to remove the temporary files when user chooses not to keep the recording
     */
    public void deleteTempFiles(){
        deleteTempFiles(tempFile);
    }

    /**
     * Deletes the given file, if it is a directory it recursively deletes all files in the directory.
     * @param file the file to delete
     */
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

    /**
     * SendReceive
     * Thread that manages all the communication between the ASAD3 device and this app.
     */
    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        // idea to clean out leftover data sent by rpi
        public boolean clean = false;

        /**
         * Sets up socket (from SocketHandler), and input streams.
         * initialized audioAnalyzer
         */
        public SendReceive(Socket socket) {
            this.socket = socket;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                int bufferSize = 1024;
                audioAnalyzer = new AudioAnalyzer(44100, bufferSize,bufferSize * 3 / 4, spectrogramHelper, snrHelper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Receives
         */
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

                        // The idea was that this would clean out any leftover data in the case of a communication error
                        // but in practice it doesn't work with this implementation
                        //if(clean){
                        //      inputStream.skip(inputStream.available());
                        //}

                        // if there is data available (should be the header), read it
                        if (inputStream.read(header_buffer) == 44) {

                            // Get the size from the header
                            ByteBuffer wrappedSize = ByteBuffer.wrap(Arrays.copyOfRange(header_buffer, 40, 44)).order(ByteOrder.LITTLE_ENDIAN);
                            size = wrappedSize.getInt() + 44;

                            // Something went wrong, the size is wrong and therefore the header wasn't found properly.
                            // This happens quite often, an idea I had for a fix is changing the header to be something fixed and stored on the app.
                            // This can work because the size is always 17640 (100ms), plus 44 for the header.
                            if (size != 17684) {
                                // check if the start of the header is correct. should always be RIFF
                                byte[] riff = Arrays.copyOfRange(header_buffer, 0, 4);
                                Log.i(TAG, "READING FRAME ISSUE, got size as:" + size + "\nHeader starts with: " + new String(riff, StandardCharsets.UTF_8));
                                // Log the header for debugging purpose
                                StringBuilder incorrectHeader = new StringBuilder("Header in bytes: ");
                                for (int i = 0; i < 44; i++) {
                                    incorrectHeader.append(String.format("%02x ", header_buffer[i]));
                                }
                                Log.i(TAG, incorrectHeader.toString());
                                // stop the recording.
                                stopRecording();
                                // skip the rest of the data.
                                // this sometimes does not skip all of the data, causing another connection issue immediately.
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
