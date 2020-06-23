package com.example.protype_1;

import android.graphics.Bitmap;
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

    private int samplerate; // possibly unnecessary
    private int buffersize;
    private int overlap;

    private double maxAmp;
    private double minAmp;

    private SpectrogramHelper spectrogramHelper;

    private AudioProcessor fftProcessor;

    /**
     * @param samplerate samplerate of audio
     * @param buffersize The size of the buffer defines how much samples are processed in one step. Common values are 1024,2048.
     * @param overlap How much consecutive buffers overlap (in samples). Half of the AudioBufferSize is common.
     */
    public AudioAnalyzer( int samplerate, int buffersize, int overlap, SpectrogramHelper mspectrogramHelper){
        this.samplerate = samplerate;
        this.buffersize = buffersize;
        this.overlap = overlap;
        this.spectrogramHelper = mspectrogramHelper;

        maxAmp = Double.MIN_VALUE;
        minAmp = Double.MAX_VALUE;

        final int fBufferSize = buffersize;
        fftProcessor = new AudioProcessor() {
            FFT fft = new FFT(fBufferSize*2, new HammingWindow());
            float[] amplitudes = new float[fBufferSize];
            @Override
            public boolean process(AudioEvent audioEvent) {
                if(audioEvent != null) {
                    float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                    float[] transformBuffer = new float[fBufferSize * 2];
                    System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length);

                    fft.forwardTransform(transformBuffer);
                    fft.modulus(transformBuffer, amplitudes);

                    double[] normalized_amps = normalize(amplitudes); //turn amplitudes into doubles and normalize

                    // Fill in the spectrogram data with normalized amplitudes
                    spectrogramHelper.addColumn(normalized_amps);
                }
                return true;
            }
            @Override
            public void processingFinished() {
            }
        };
    }

    /**
     *
     */
    public void initDispatcher(@Nullable InputStream audioStream){
        //setup dispatcher
        if(audioStream == null){
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(44100 / 2, buffersize, overlap);
        } else {
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(samplerate, 16, 1, true, false);
            UniversalAudioInputStream formattedStream = new UniversalAudioInputStream(audioStream, audioFormat);
            dispatcher = new AudioDispatcher(formattedStream, buffersize, overlap);
        }

        dispatcher.addAudioProcessor(new BandPass(3010, 2990, 44100));
        dispatcher.addAudioProcessor(fftProcessor);
    }


    public void startAnalyzer(){

        Thread dispatchThread = new Thread(dispatcher);
        dispatchThread.start();
    }

    public boolean isStopped() { return dispatcher.isStopped(); }

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
