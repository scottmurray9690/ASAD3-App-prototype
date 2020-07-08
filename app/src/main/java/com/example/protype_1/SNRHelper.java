package com.example.protype_1;

import java.util.ArrayList;

public class SNRHelper {
    private double[][] noiseSNR;
    private double[][] sampleSNR;

    public static final int NOISE_STATE = 1;
    public static final int SAMPLE_STATE = 2;

    private int state;
    private int index;
    public SNRHelper(int width, int height) {
        state = SAMPLE_STATE;
        index = 0;
        noiseSNR = new double[width][height];
        sampleSNR = new double[width][height];
    }

    public void addColumn(double[] data) {
        switch (state){
            case NOISE_STATE:
                System.arraycopy(data, 0, noiseSNR[index], 0, data.length);
                index++;
                index = index%noiseSNR.length;
                break;
            case SAMPLE_STATE:
                System.arraycopy(data, 0, sampleSNR[index], 0, data.length);
                index++;
                index = index%sampleSNR.length;
                break;
        }
    }

    public int getState() {
        return state;
    }
    public void setState(int newState) {
        state = newState;
    }
}
