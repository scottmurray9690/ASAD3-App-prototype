package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;

import java.io.InputStream;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;

public class AudioAnalyzer {

    private AudioDispatcher dispatcher;

    private InputStream audioStream;
    private int samplerate; // possibly unnecessary
    private int buffersize;
    private int overlap;
    private int windowstep;

    private double maxAmp;
    private double minAmp;

    private int displayWidth;
    private int displayHeight;
    private int[] displayColors;

    private int maxSamplesHeld;

    private double[][] spectrogram; // possibly change to an arraylist

    private AudioProcessor fftProcessor;

    private boolean finished;

    /**
     * @param audioStream Inputstream of the audio file, if null AudioAnalyzer uses the default microphone
     * @param samplerate samplerate of audio
     * @param buffersize The size of the buffer defines how much samples are processed in one step. Common values are 1024,2048.
     * @param overlap How much consecutive buffers overlap (in samples). Half of the AudioBufferSize is common.
     */
    public AudioAnalyzer(@Nullable InputStream audioStream, int samplerate, int buffersize, int overlap){
        this.audioStream = audioStream;
        this.samplerate = samplerate;
        this.buffersize = buffersize;
        this.overlap = overlap;
        finished = false;

        windowstep = buffersize-overlap;

        maxAmp = Double.MIN_VALUE;
        minAmp = Double.MAX_VALUE;

        final int fBufferSize = buffersize;
        fftProcessor = new AudioProcessor() {
            FFT fft = new FFT(fBufferSize*2, new HammingWindow());
            float[] amplitudes = new float[fBufferSize];
            @Override
            public boolean process(AudioEvent audioEvent) {
                final int i = ((int) (audioEvent.getSamplesProcessed() - audioEvent.getBufferSize()) / windowstep) % maxSamplesHeld; // Current index of data

                if(i >= 0) {
                    float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                    float[] transformBuffer = new float[fBufferSize * 2];
                    System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length);

                    fft.forwardTransform(transformBuffer);
                    fft.modulus(transformBuffer, amplitudes);

                    double[] normalized_amps = normalize(amplitudes); //turn amplitudes into doubles and normalize

                    // Fill in the spectrogram data with normalized amplitudes
                    System.arraycopy(normalized_amps, 0, spectrogram[i], 0, spectrogram[0].length);
                    for (int j=0; j < displayHeight; j++) {
                        int c = getColor(spectrogram[i][j]);
                        int x = i % displayWidth;
                        displayColors[x + displayWidth * (displayHeight - 1 - j)] = c;
                    }
                }
                return true;
            }
            @Override
            public void processingFinished() {
                finished = true;
            }
        };
    }

    /**
     * Initializes the dispatcher for the InputStream if given, otherwise uses the default microphone
     */
    public void initDispatcher(){
        if(audioStream == null){
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100 / 2, buffersize, overlap);
        } else {
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(samplerate, 16, 1, true, false);
            UniversalAudioInputStream formattedStream = new UniversalAudioInputStream(audioStream, audioFormat);
            dispatcher = new AudioDispatcher(formattedStream, buffersize, overlap);
        }
    }

    /**
     * @param xAxisSeconds number of seconds to show on x axis
     * @param yAxisHz number of hertz to show on the y axis
     */
    public void startAnalyzer(int xAxisSeconds, int yAxisHz){
        double unit_time = (double) windowstep / samplerate;
        double unit_frequency = (double) samplerate / buffersize;

        maxSamplesHeld = (int)(20/unit_time); // Only store 20s of data at a time

        spectrogram = new double[maxSamplesHeld][buffersize];

        displayWidth = (int)(xAxisSeconds/unit_time);
        displayHeight = (int)(yAxisHz/unit_frequency);
        displayColors = new int[displayWidth*displayHeight];

        dispatcher.addAudioProcessor(new BandPass(3010, 2990, 44100));
        dispatcher.addAudioProcessor(fftProcessor);
        Thread dispatchThread = new Thread(dispatcher);
        dispatchThread.start();
    }

    public void stopAnalyzer(){
        dispatcher.stop();
    }

    public Bitmap getSpectrogramBitmap(){
        return Bitmap.createBitmap(displayColors, displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
    }

    public boolean isFinished(){ return finished;   }

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

    private int getColor(double power) {
        float[] HSV = new float[3];
        HSV[0] = (float) (180 * (1 - power)); // Hue (note 180 = Cyan)
        HSV[1] = (float) (power); // Saturation
        HSV[2] = (float) 1.0; // Value

        return Color.HSVToColor(HSV);
    }
}
