package com.example.protype_1;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;

/**
 * SNRHelper
 * A helper class which keeps track of psd data in order to generate an SNR value at different frequency ranges.
 *
 * This works by measuring the psd of a recording of the user holding their breath and comparing it to
 * a recording of when the user is breathing normally.
 */
public class SNRHelper {
    public static final String TAG = "SNRHelper";

    private double[][] noiseSNR;
    private double[][] sampleSNR;

    // the state when the user is holding their breath
    public static final int NOISE_STATE = 1;
    // the state where the user is breathing normally
    public static final int SAMPLE_STATE = 2;

    private int state;

    // where we are currently
    private int index;

    private boolean pauseRecording;

    /**
     *
     * @param width the amount of samples to analyze
     * @param height the height of one sample
     */
    public SNRHelper(int width, int height) {
        state = SAMPLE_STATE;
        index = 0;
        noiseSNR = new double[width][height];
        sampleSNR = new double[width][height];
        pauseRecording = false;

    }

    /**
     * Adds data to the current index of the SNR tables depending on the state.
     * @param data the psd values to add
     */
    public void addColumn(double[] data) {
        if(pauseRecording){
            return;
        }
        switch (state){
            case NOISE_STATE:
                System.arraycopy(data, 0, noiseSNR[index], 0, noiseSNR[index].length);
                index++;
                // ensure the index stays within the bounds of the array
                index = index%noiseSNR.length;
                break;
            case SAMPLE_STATE:
                System.arraycopy(data, 0, sampleSNR[index], 0, sampleSNR[index].length);
                index++;
                // ensure the index stays within the bounds of the array
                index = index%sampleSNR.length;
                break;
        }
    }

    /**
     * calculates the average PSD within the frequency ranges given of the noise data and the sample data,
     * and compares them to get the SNR.
     *
     * @param lowfreq lower end of the frequency band
     * @param highfreq higher end of the frequency band
     * @return the average SNR of the last `width` samples of sampleSNR
     */
    public double getSNR(int lowfreq, int highfreq){
        int lowIndex;
        int highIndex;
        //the size of a frequency bin
        double binFreq =  512.0/22050.0;
        //convert from frequency to index (bin #)
        lowIndex =  (int)(lowfreq*binFreq);
        highIndex =  (int)(highfreq*binFreq);

        if (lowIndex >= 0 && highIndex < noiseSNR.length) {
            //don't overwrite data while calculating SNR
            pauseRecording = true;
            // find the mean PSD of samples and noise
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
            // return the SNR as the average PSD of the samples divided by the average PSD of the noise.
            return samplePSDmean/noisePSDmean;
        } else {
            return -1;
        }
    }

    //getters and setters
    public int getState() {
        return state;
    }
    public void setState(int newState) {
        state = newState;
    }
}
