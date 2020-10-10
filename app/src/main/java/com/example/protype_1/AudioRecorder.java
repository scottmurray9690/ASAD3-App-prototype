package com.example.protype_1;

import android.provider.ContactsContract;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AudioRecorder {
    private DataOutputStream outputStream;
    private boolean hasHeader;

    public AudioRecorder() {
        //possibly put the try/catch here, idk yet

    }

    public void setFile(File file) throws FileNotFoundException {
        OutputStream os;
        os = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        outputStream = new DataOutputStream(bos);
    }

    public boolean setHeader(){
        if(!hasHeader){
            try {
                outputStream.write(getHeader());
                return true;
            } catch (IOException e){
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private byte[] getHeader(){
        byte[] header = new byte[44];
        int channels = 2;
        long longSampleRate = 44100;
        byte RECORDER_BPP = 16;

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (0x64);
        header[5] = (byte) (0x7c);
        header[6] = (byte) (0x76);
        header[7] = (byte) (0x09);
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
        header[22] = (byte) channels;
        header[23] = 0;
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
        header[40] = (byte) (0x40);
        header[41] = (byte) (0x7c);
        header[42] = (byte) (0x76);
        header[43] = (byte) (0x09);

        return header;
    }

    public void startRecording(){}

    public void stopRecording(){}

    public void writeData(byte[] data) throws IOException{
        outputStream.write(data);
    }

}
