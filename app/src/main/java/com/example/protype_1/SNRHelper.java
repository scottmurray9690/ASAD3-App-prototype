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


    private boolean pauseRecording;
    public SNRHelper(int width, int height) {
        state = SAMPLE_STATE;
        index = 0;
        noiseSNR = new double[width][height];
        sampleSNR = new double[width][height];
        pauseRecording = false;

    }

    public void addColumn(double[] data) {
        if(pauseRecording){
            return;
        }
        switch (state){
            case NOISE_STATE:
                System.arraycopy(data, 0, noiseSNR[index], 0, noiseSNR[index].length);

                index++;
                index = index%noiseSNR.length;
                break;
            case SAMPLE_STATE:
                System.arraycopy(data, 0, sampleSNR[index], 0, sampleSNR[index].length);
                index++;
                index = index%sampleSNR.length;
                break;
        }
    }

    public double getSNR(int lowfreq, int highfreq){
        int lowIndex;
        int highIndex;
        double binFreq =  512.0/22050.0;
        lowIndex =  (int)(lowfreq*binFreq);  //(int)(10000*1024/44100); // and 10kHz on the y axis
        highIndex =  (int)(highfreq*binFreq);
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
