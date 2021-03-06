package com.example.protype_1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * AudioRecorder
 * This class is used to save the audio that is being processed by the Application.
 *
 * it works by writing the raw data into a temporary file, and if the user chooses to save the file
 * it is copied into permanent memory and the temporary file is deleted.
 */
public class AudioRecorder {
    private DataOutputStream rawOutputStream;
    private boolean hasHeader;
    private int audioLength;
    private File tempFile; // File that holds the raw bytes while the audio is being recorded
    private File saveFile;

    /**
     * Sets up the temporary file as an DataOutputStream
     * @param file the file given by another class, likely an activity or fragment with access to internal storage.
     * @throws FileNotFoundException If the given file does not exist
     */
    public void setTempFile(File file) throws FileNotFoundException {
        tempFile = file;
        OutputStream os;
        os = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        rawOutputStream = new DataOutputStream(bos);
    }

    /**
     * Saves the recording to permanent memory
     * @param location the location of the recording
     * @throws IOException
     */
    public void saveRecording(File location) throws IOException {
        // sets up the file location as DataOutputStream
        OutputStream os;
        os = new FileOutputStream(location);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        DataOutputStream fileOutputStream = new DataOutputStream(bos);
        fileOutputStream.write(getHeader());

        byte[] audioData;
        // shortcut for newer android builds
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioData = Files.readAllBytes(tempFile.toPath());
        } else {
            // manually read all the files from the tempfile to the fileOutputStream
            int size = (int) tempFile.length();
            audioData = new byte[size];
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(tempFile));
            buf.read(audioData, 0, audioData.length);
        }

        fileOutputStream.write(audioData);
    }

    // Writes to the file while keeping track of its size
    public void writeData(byte[] data) throws IOException{
        audioLength += data.length;
        rawOutputStream.write(data);
    }

    /**
     * get a valid .wav header based on the size of the audio
     *
     * reference: http://soundfile.sapp.org/doc/WaveFormat/
     */
    private byte[] getHeader(){
        byte[] header = new byte[44];
        int channels = 2;
        long longSampleRate = 44100;
        byte RECORDER_BPP = 16;
        int totalDataLen = audioLength + 36;

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) (channels & 0xff);
        header[23] = (byte) ((channels >> 8) &0xff);
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (0x10 & 0xff);
        header[29] = (byte) (0xb1);
        header[30] = (byte) (0x02);
        header[31] = (byte) (0x00);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (audioLength & 0xff);
        header[41] = (byte) ((audioLength >> 8) & 0xff);
        header[42] = (byte) ((audioLength >> 16) & 0xff);
        header[43] = (byte) ((audioLength >> 24) & 0xff);

        return header;
    }

}
