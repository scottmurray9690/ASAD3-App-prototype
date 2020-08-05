package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Color;

public class SpectrogramHelper {
    final static int DISPLAY_WIDTH = (int)(5*44100/256); // this is messy, but it ensures the display shows 5s of data at a time ...
    final static int DISPLAY_HEIGHT = (int)(10000*512/22050); // and 10kHz on the y axis
    int width;
    int height;
    int[] colourArray;
    double[][] spectrogram;
    private int index;

    private double maxAmp;
    private double minAmp;

    boolean isFirstColumn;

    public SpectrogramHelper(int width, int height) {
        this.width = width;
        this.height = height;

        maxAmp = Double.MIN_VALUE;
        minAmp = Double.MAX_VALUE;

        colourArray = new int[DISPLAY_HEIGHT*DISPLAY_WIDTH];
        spectrogram = new double[width][height];
        index = 0;

        isFirstColumn = true;
    }
    public void setColumnColours( double[] amplitudes) {
        for (int i=0; i < DISPLAY_HEIGHT;i++) {
            int c = getColor(amplitudes[i]/maxAmp);
            colourArray[index % DISPLAY_WIDTH + DISPLAY_WIDTH*(DISPLAY_HEIGHT - 1 - i)] = c;
        }
    }
    public void setColours(){
        for(int i=1; i<DISPLAY_WIDTH;i++) {
            for(int j=0; j<DISPLAY_HEIGHT; j++) {
                int c;
                if (i == 0){
                    c = Color.BLACK;
                }else {
                    c = getColor(spectrogram[(index + i) % DISPLAY_WIDTH][j]);
                }
                colourArray[i + DISPLAY_WIDTH*(DISPLAY_HEIGHT-1-j)] = c;
            }
        }
    }
    public void addColumn(float[] amplitudes) {
        if(isFirstColumn){
            //double tempsum = 0;
            for (float amplitude : amplitudes) {
                //tempsum+=amplitude;
                if (amplitude < minAmp) {
                    minAmp = amplitude;
                }
            }
            //minAmp = tempsum/amplitudes.length;
            maxAmp = minAmp*50;
            maxAmp = Math.log10(maxAmp/minAmp)*10;
            isFirstColumn = false;
        }
        double[] dbAmps = hzToDb(amplitudes); //turn amplitudes into doubles and normalize
        dbAmps[0] = 0;  //Do not graph the DC component, it is too large and messes everything up
        System.arraycopy(dbAmps, 0, spectrogram[index], 0, height);
        setColumnColours(dbAmps);
        index = (index+1)%width;
    }

    private int getColor(double power) {
        float[] HSV = new float[3];
        HSV[0] = (float) (180 * (1.0 - power)); // Hue (note 180 = Cyan)
        HSV[1] = (float) (1.0); // Saturation
        HSV[2] = (float) 1.0; // Value

        return Color.HSVToColor(HSV);
    }

    public Bitmap getBitmap(){
       //setColours();

        return Bitmap.createBitmap(colourArray, DISPLAY_WIDTH, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
    }

    private double[] hzToDb(float[] hzAmps) {
        double[] dbAmps = new double[hzAmps.length];
        for (int i=0;i<hzAmps.length;i++){
            dbAmps[i] = Math.log10(hzAmps[i]/minAmp)*10;
        }
        return dbAmps;
    }

    private double[] normalize(float[] in) {
        //double maxAmp = Double.MIN_VALUE;
        //double minAmp = Double.MAX_VALUE;
        // get max and min amplitudes
        double[] out = new double[in.length];
        for (int x = 0; x < in.length; x++) {
            if (in[x] > maxAmp) {
                maxAmp = in[x];
            }
            if (in[x] < minAmp) {
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
