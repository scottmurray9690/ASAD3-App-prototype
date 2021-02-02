package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

/**
 * SpectrogramHelper
 * A helper class for generating the spectrogram image from the FFT data of the audio.
 *
 * This generates the spectrogram column by column, adding onto it whenever there is new data.
 */
public class SpectrogramHelper {
    public final static String TAG = "SpecHelp";

    final static int DISPLAY_WIDTH = (int)(10*44100/256); // this is messy, but it ensures the display shows 10s of data at a time ...
    final static int DISPLAY_HEIGHT = (int)(10000*512/22050); // and 10kHz on the y axis

    int width;
    int height;

    int[] colourArray;
    double[][] spectrogram;
    private int index;

    private double maxAmp;
    private double minAmp;

    boolean isFirstColumn;

    /**
     * Creates the SpectrogramHelper of the given width and height, this should be larger or equal
     * to the display width and height for it to look best
     * @param width amount of columns to store in memory
     * @param height height of each column
     */
    public SpectrogramHelper(int width, int height) {
        this.width = width;
        this.height = height;

        // keeping track of the max and min values in the spectrogram is important for normalization
        maxAmp = Double.MIN_VALUE;
        minAmp = Double.MAX_VALUE;

        // set up the arrays for both numerical data and the colours.
        colourArray = new int[DISPLAY_HEIGHT*DISPLAY_WIDTH];
        spectrogram = new double[width][height];

        index = 0;
        isFirstColumn = true;
    }

    // Adds the current column (pointed to by index) to the colourArray.
    public void setColumnColours( double[] amplitudes) {
        for (int i=0; i < DISPLAY_HEIGHT;i++) {
            int c = getColor(amplitudes[i]/maxAmp);
            colourArray[index % DISPLAY_WIDTH + DISPLAY_WIDTH*(DISPLAY_HEIGHT - 1 - i)] = c;
        }
    }


    /**
     * Adds a column to the spectrogram from the given amplitudes, uses a hardcoded minAmp because after
     * a long time of testing it looked the best just using a constant 0.015.
     * @param amplitudes the amplitudes of the FFT of the audio data
     */
    public void addColumn(float[] amplitudes) {
        if(isFirstColumn){
            // The old way I tried:
//            for (float amplitude : amplitudes) {
//                if (amplitude < minAmp) {
//                    minAmp = amplitude;
//                }
//            }

            // These are hardcoded because it looked the best, can be revisited.
            minAmp = 0.015;
            maxAmp = minAmp*50;
            maxAmp = Math.log10(maxAmp/minAmp)*10;

            isFirstColumn = false;
        }
        double[] dbAmps = hzToDb(amplitudes); //turn amplitudes into doubles and normalize
        dbAmps[0] = 0;  //Do not graph the DC component, it is too large and messes everything up

        // Copy the amplitudes into the spectrogram
        System.arraycopy(dbAmps, 0, spectrogram[index], 0, height);
        setColumnColours(dbAmps);
        index = (index+1)%width;
    }

    // Adjusts the hardcoded minAmp value to change the appearene of the spectrogram.
    public void changeIntensity(double amount){
        minAmp *= amount;
    }

    /**
     * Generates a color with a different hue depending on the amplitude.
     *
     * @param power The ratio of the current amplitude to the maximum amplitude
     * @return the generated color
     */
    private int getColor(double power) {
        float[] HSV = new float[3];
        // This makes higher power amplitudes be red and lower power amplitudes be green/cyan
        HSV[0] = (float) (180 * (1.0 - power)); // Hue (note 180 = Cyan)
        HSV[1] = (float) (1.0); // Saturation
        HSV[2] = (float) 1.0; // Value

        return Color.HSVToColor(HSV);
    }

    // Generates a bitmap from the colourArray, used to display the spectrogram in an activity
    public Bitmap getBitmap(){
        return Bitmap.createBitmap(colourArray, DISPLAY_WIDTH, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    /**
     * Converts an array of fourier-transformed audio data from Hz to decibels.
     * @param hzAmps the data in Hz
     * @return the data in dB
     */
    private double[] hzToDb(float[] hzAmps) {
        double[] dbAmps = new double[hzAmps.length];
        for (int i=0;i<hzAmps.length;i++){
            dbAmps[i] = Math.log10(hzAmps[i]/minAmp)*10;
        }
        return dbAmps;
    }
}
