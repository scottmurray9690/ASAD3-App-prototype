package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Color;

public class SpectrogramHelper {
    final static int DISPLAY_WIDTH = (int)(5*44100/256); // this is messy, but it ensures the display shows 5s of data at a time ...
    final static int DISPLAY_HEIGHT = (int)(10000*1024/44100); // and 10kHz on the y axis
    int width;
    int height;
    int[] colourArray;
    double[][] spectrogram;
    private int index;

    private double maxAmp;
    private double minAmp;


    public SpectrogramHelper(int width, int height) {
        this.width = width;
        this.height = height;
        maxAmp = Double.MIN_VALUE;
        minAmp = Double.MAX_VALUE;

        colourArray = new int[DISPLAY_HEIGHT*DISPLAY_WIDTH];
        spectrogram = new double[width][height];
        index = 0;
    }
    public void setColumnColours( double[] amplitudes) {
        for (int i=0; i < DISPLAY_HEIGHT;i++) {
            int c = getColor(amplitudes[i]);
            colourArray[index % DISPLAY_WIDTH + DISPLAY_WIDTH*(DISPLAY_HEIGHT - 1 - i)] = c;
        }
    }
    public void setColours(){
        for(int i=0; i<DISPLAY_WIDTH;i++) {
            for(int j=0; j<DISPLAY_HEIGHT; j++) {
                int c = getColor(spectrogram[(index+i)%DISPLAY_WIDTH][j]);
                colourArray[i + DISPLAY_WIDTH*(DISPLAY_HEIGHT-1-j)] = c;
            }
        }
    }
    public void addColumn(float[] amplitudes) {
        double[] normalized_amps = normalize(amplitudes); //turn amplitudes into doubles and normalize
        System.arraycopy(normalized_amps, 0, spectrogram[index], 0, height);
        setColumnColours(normalized_amps);
        index = (index+1)%width;
    }

    private int getColor(double power) {
        float[] HSV = new float[3];
        HSV[0] = (float) (180 * (1 - power)); // Hue (note 180 = Cyan)
        HSV[1] = (float) (power); // Saturation
        HSV[2] = (float) 1.0; // Value

        return Color.HSVToColor(HSV);
    }

    public Bitmap getBitmap(){
       //setColours();

        return Bitmap.createBitmap(colourArray, DISPLAY_WIDTH, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    private double[] normalize(float[] in) {
        // get max and min amplitudes
        double[] out = new double[in.length];
        for (int x = 0; x < in.length; x++) {
            if (in[x] > maxAmp) {
                maxAmp = in[x];
            } else if (in[x] < minAmp) {
                minAmp = in[x];
            }
        }
        // avoiding divided by zero
        double minValidAmp = 0.00000000001F;
        if (minAmp == 0) {
            minAmp = minValidAmp;
        }
        double diff = Math.log10(maxAmp / minAmp); // perceptual difference
        for (int x = 0; x < in.length; x++) {
            if (in[x] < minValidAmp) {
                out[x] = 0;
            } else {
                out[x] = (Math.log10(in[x] / minAmp)) / diff;
            }
        }
        // end normalization
        return out;
    }
}
