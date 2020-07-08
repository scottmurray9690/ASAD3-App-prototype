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


    public SpectrogramHelper(int width, int height) {
        this.width = width;
        this.height = height;

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
    public void addColumn(double[] amplitudes) {
        System.arraycopy(amplitudes, 0, spectrogram[index], 0, height);
        setColumnColours(amplitudes);
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
        return Bitmap.createBitmap(colourArray, DISPLAY_WIDTH, DISPLAY_HEIGHT, Bitmap.Config.ARGB_8888);
    }
}
