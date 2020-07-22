package com.example.protype_1;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;

public class SNRHelper {
    public static final String TAG = "SNRHelper";

    private double[][] noiseSNR;
    private double[][] sampleSNR;

    public static final int NOISE_STATE = 1;
    public static final int SAMPLE_STATE = 2;

    private int state;
    private int index;

    // Debug stuff
    String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String fileName = "psdAnalysis.csv";
    String filePath = baseDir + File.separator + fileName;
    File file = new File(filePath);
    BufferedWriter writer;
    private boolean fileiswritten;

    // End Debug stuff


    private boolean pauseRecording;
    public SNRHelper(int width, int height) {
        state = SAMPLE_STATE;
        index = 0;
        noiseSNR = new double[width][height];
        sampleSNR = new double[width][height];
        pauseRecording = false;

        // Debug stuff
        try {
            Log.i(TAG, "initializing file writer, is file ready? "+file.exists());
            writer = new BufferedWriter(new FileWriter(filePath,false));
            writer.write("Noise PSD,Sample PSD\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // End Debug stuff
    }

    public void addColumn(double[] data) {
        if(pauseRecording){
            return;
        }
        switch (state){
            case NOISE_STATE:
                System.arraycopy(data, 0, noiseSNR[index], 0, data.length);

                index++;
                index = index%noiseSNR.length;
                break;
            case SAMPLE_STATE:
                System.arraycopy(data, 0, sampleSNR[index], 0, data.length);
                if(!fileiswritten && index == noiseSNR.length/2) {
                    Log.i(TAG,"Writing to file "+filePath);
                    fileiswritten=true;

                        try {
                            for (int i=0; i < sampleSNR[index].length; i++) {
                                writer.write(noiseSNR[index][i] + "," + sampleSNR[index][i] + "\n");
                            }
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
                index++;
                index = index%sampleSNR.length;
                break;
        }
    }

    public double getSNR(int lowfreq, int highfreq){

        int lowIndex = (int) (lowfreq*1024/44100);  //(int)(10000*1024/44100); // and 10kHz on the y axis
        int highIndex = (int) (highfreq*1024/44100);
        if (lowIndex >= 0 && highIndex < noiseSNR.length) {
            pauseRecording = true;
            double noisePSDmean = 0;
            double samplePSDmean = 0;
            for (int i=0; i<noiseSNR.length; i++) {
                double tempNoisePSD = 0;
                double tempSamplePSD = 0;
                for( int j=lowIndex;j< highIndex; j++) {
                    tempNoisePSD += noiseSNR[i][j];
                    tempSamplePSD += sampleSNR[i][j];
                }
                noisePSDmean += tempNoisePSD/noiseSNR.length;
                samplePSDmean += tempSamplePSD/sampleSNR.length;
            }
            pauseRecording = false;
            if(noisePSDmean == 0 ) {
                return -1;
            }
            return samplePSDmean/noisePSDmean;
        } else {
            return -1;
        }
    }


    public int getState() {
        return state;
    }
    public void setState(int newState) {
        state = newState;
    }
}
