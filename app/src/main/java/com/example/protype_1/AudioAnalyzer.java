package com.example.protype_1;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
import be.tarsos.dsp.util.fft.HannWindow;

public class AudioAnalyzer {

    public static String TAG = "AudioAnalyze";

    private AudioDispatcher dispatcher;

    private int samplerate; // possibly unnecessary
    private int buffersize;
    private int overlap;


    private SpectrogramHelper spectrogramHelper;
    private SNRHelper snrHelper;

    private AudioProcessor fftProcessor;


    /**
     * @param samplerate sample rate of audio
     * @param buffersize The size of the buffer defines how much samples are processed in one step. Common values are 1024,2048.
     * @param overlap How much consecutive buffers overlap (in samples). Half of the AudioBufferSize is common.
     */
    public AudioAnalyzer( int samplerate, int buffersize, int overlap, SpectrogramHelper mspectrogramHelper, SNRHelper msnrHelper){
        this.samplerate = samplerate;
        this.buffersize = buffersize;
        this.overlap = overlap;
        this.spectrogramHelper = mspectrogramHelper;
        snrHelper = msnrHelper;


        final int fBufferSize = buffersize;
        fftProcessor = new AudioProcessor() {
            FFT fft = new FFT(fBufferSize*2, new HannWindow());
            float[] amplitudes = new float[fBufferSize];
            @Override
            public boolean process(AudioEvent audioEvent) {
                if(audioEvent != null) {
                    float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                    float[] transformBuffer = new float[fBufferSize * 2];
                    System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length);

                    fft.forwardTransform(transformBuffer);

                    fft.modulus(transformBuffer, amplitudes);

                    //square each modulus to get psd
                    double[] psd = new double[amplitudes.length];
                    for(int i = 0; i<amplitudes.length; i++) {
                        psd[i] = Math.pow(amplitudes[i], 2);
                    }
                    snrHelper.addColumn(psd);

                    // Fill in the spectrogram data with amplitudes
                    spectrogramHelper.addColumn(amplitudes);

                }
                return true;
            }
            @Override
            public void processingFinished() {
            //    Log.i(TAG, "Finished processing a file");.
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
            TarsosDSPAudioFormat audioFormat = new TarsosDSPAudioFormat(samplerate, 16, 2, true, false);
            UniversalAudioInputStream formattedStream = new UniversalAudioInputStream(audioStream, audioFormat);
            dispatcher = new AudioDispatcher(formattedStream, buffersize, overlap);
        }
// for 10-3000Hz midpoint = (3000+10)/2, bandwidth = (3000-10)
        dispatcher.addAudioProcessor(new BandPass(1505, 2990, 44100));
        dispatcher.addAudioProcessor(fftProcessor);
    }


    public void startAnalyzer(){
        Thread dispatchThread = new Thread(dispatcher);
        dispatchThread.start();
    }

    public boolean isStopped() { return dispatcher.isStopped(); }


}
