package com.example.protype_1;

import androidx.annotation.Nullable;

import java.io.InputStream;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.filters.BandPass;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HannWindow;
/**
 * AudioAnalyzer
 * This helper class uses the TarsosDSP library to compute the Fast Fourier Transform of the audio that
 * is fed to it from CommunicationFragment.
 *
 * In TarsosDSP audio processing is done by dispatchers, which run the audio that is fed to them
 * through a series of processors. This class uses the built-in filter from TarsosDSP as well as
 * a custom fftProcessor, which sends data to SpectrogramHelper and SNRHelper to generate the SNR
 * and spectrogram.
 *
 */
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
     * The constuctor sets up the fftProcessor based on the parameters given
     *
     * @param samplerate sample rate of audio
     * @param buffersize The size of the buffer defines how much samples are processed in one step. Common values are 1024,2048.
     * @param overlap How much consecutive buffers overlap (in samples). Half of the AudioBufferSize is common.
     * @param mspectrogramHelper The SpectrogramHelper to send data to
     * @param msnrHelper The SNRHelper to send data to
     */
    public AudioAnalyzer( int samplerate, int buffersize, int overlap, SpectrogramHelper mspectrogramHelper, SNRHelper msnrHelper){
        this.samplerate = samplerate;
        this.buffersize = buffersize;
        this.overlap = overlap;
        this.spectrogramHelper = mspectrogramHelper;
        snrHelper = msnrHelper;


        final int fBufferSize = buffersize;
        fftProcessor = new AudioProcessor() {
            // FFT calculator from TarsosDSP
            FFT fft = new FFT(fBufferSize*2, new HannWindow());
            float[] amplitudes = new float[fBufferSize];
            @Override
            public boolean process(AudioEvent audioEvent) {
                if(audioEvent != null) {
                    // set up buffers for transform
                    float[] audioFloatBuffer = audioEvent.getFloatBuffer();
                    float[] transformBuffer = new float[fBufferSize * 2];
                    System.arraycopy(audioFloatBuffer, 0, transformBuffer, 0, audioFloatBuffer.length);
                    // do the transform
                    fft.forwardTransform(transformBuffer);
                    // get the modulus (absolute value)
                    fft.modulus(transformBuffer, amplitudes);

                    //square each modulus to get psd
                    double[] psd = new double[amplitudes.length];
                    for(int i = 0; i<amplitudes.length; i++) {
                        psd[i] = Math.pow(amplitudes[i], 2);
                    }
                    // send it to the SNRHelper
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
     * This method sets up the dispatcher with the audio input. It can also be called with null parameters
     * in the case that you want to use the phone's microphone rather than the file inputstream from the rpi
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
        // Add the processors
        // for 10-3000Hz, midpoint = (3000+10)/2, bandwidth = (3000-10)
        int lowCutoff = 10;
        int highCutoff = 3000;
        int midpoint = (lowCutoff+highCutoff)/2;
        int bandwidth = highCutoff-lowCutoff;
        dispatcher.addAudioProcessor(new BandPass(midpoint, bandwidth, 44100));
        dispatcher.addAudioProcessor(fftProcessor);


    }

    // starts the analyzer
    public void startAnalyzer(){
        Thread dispatchThread = new Thread(dispatcher);
        dispatchThread.start();
    }

    public boolean isStopped() { return dispatcher.isStopped(); }


}
