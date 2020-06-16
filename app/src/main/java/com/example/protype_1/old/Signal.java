package com.example.protype_1.old;
import org.jtransforms.fft.DoubleFFT_1D;
import java.util.*;

/**
 *
 * @author murta
 */
public class Signal {

    private averaging ma;

    public Signal(){
        ma = new averaging(44100/2);
    }

    /**
     * calculates the mean of the signal
     * @param signal
     * @return mean
     */
    public double mean(double[] signal) {
        double mean = 0;
        for (int i = 0; i < signal.length; i++) {
            mean += signal[i];
        }
        mean /= signal.length;
        return mean;
    }

    /**
     * calculates the energy of the signal
     * @param signal
     * @return energy value
     */
    public double energy(double[] signal) {
        double totalEnergy = 0;
        for (int i = 0; i < signal.length; i++) {
            totalEnergy += (signal[i] * signal[i]);
        }
        return totalEnergy;
    }

    /**
     * calculates the power of the signal
     * @param signal
     * @return power value
     */
    public double power(double[] signal) {
        return energy(signal) / signal.length;
    }

    /**
     *
     * @param signal
     * @return
     */
    public double norm(double[] signal) {
        return Math.sqrt(energy(signal));
    }

    /**
     * finds minimum value
     * @param signal
     * @return minimum value
     */
    public double minimum(double[] signal) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < signal.length; i++) {
            min = Math.min(min, signal[i]);
        }
        return min;
    }

    /**
     * finds maximum value
     * @param signal
     * @return maximum value
     */
    public double maximum(double[] signal) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < signal.length; i++) {
            max = Math.max(max, signal[i]);
        }
        return max;
    }

    /**
     * method that downsamples the signal
     * @param scale
     * @return a double array of the downsampled signal
     */
    public double[] downsampleSig(double[] signal, int scale){
        //System.out.println("Initial Size of halfsec: "+halfsec);
        double[] res = new double[(signal.length/scale)];
        for(int i = 0; i < (res.length); i+= 1)
            res[i] = signal[i*scale];

        return res;
    }

    /**
     * scales the signal
     * @param signal
     * @param scale
     */
    public void scale(double[] signal, double scale) {
        for (int i = 0; i < signal.length; i++) {
            signal[i] *= scale;
        }
    }

    /**
     * applys fft on the signal
     * @param ddata
     * @return array containing the results of the fft
     */
    public double[] doFFT(double[] ddata) {
        DoubleFFT_1D fftDo = new DoubleFFT_1D(ddata.length);
        double[] fft = new double[ddata.length * 2];
        System.arraycopy(ddata, 0, fft, 0, ddata.length);
        fftDo.realForwardFull(fft);
        return fft;
    }

    /**
     * finds the amplitude of fft values of the signal
     * @param ddata
     * @return array of amplitudes
     */
    public double[] getAmpSpectrum(double[] ddata) {
        return absComplex(doFFT(ddata));
    }

    /**
     * calculates the power spectrum density of the signal
     * @param ddata
     * @return array of PSD values
     */
    public double[] getPSD(double[] ddata) {
        double[] xf =  absComplex(doFFT(ddata));
        double[] pd = new double[xf.length];

        for(int i = 0; i < xf.length;i++){
            pd[i] = (xf[i] * xf[i]);

        }

        return pd;

    }


    private double[] absComplex(double[] input) {
        double amplitude[] = new double[input.length / 2];
        for (int i = 0; i < (input.length - 1); i += 2) {
            amplitude[i / 2] = Math.sqrt((input[i] * input[i]) + (input[i + 1] * input[i + 1]));
        }
        return amplitude;
    }

    /**
     *  finds the absolute of all elements in an array
     * @param input
     * @return array of absolute value of all entries in array
     */
    public double[] abs(double[] input) {
        double amplitude[] = new double[input.length];
        for (int i = 0; i < input.length; i += 2) {
            amplitude[i] =Math.abs(input[i]);
        }
        return amplitude;
    }

    /**
     * apply a Hamming window filter to raw input data
     *
     * @param input an array containing unfiltered input data
     * @return a double array containing the filtered data
     */
    public double[] Hamming(double[] input) {
        double[] res = new double[input.length];
        double[] window = buildHammWindow(input.length);
        for (int i = 0; i < input.length; ++i) {
            res[i] = input[i] * window[i];
        }
        return res;
    }


    /**
     * apply a moving average window to raw input data
     *
     * @param input an array containing unfiltered input data
     * @return a double array containing the filtered data
     */
    public double[] smoothen(double[] input){
        return ma.smoothen(input);
    }


    private double[] buildHammWindow(int size) {
        double[] window = new double[size];
        for (int i = 0; i < size; ++i) {
            window[i] = .54 - .46 * Math.cos(2 * Math.PI * i / (size - 1.0));
        }
        return window;
    }

}

class averaging {

    // queue used to store list so that we get the average
    private final Queue<Double> Dataset = new LinkedList<Double>();
    private final int period;
    private double sum;

    // constructor to initialize period
    public averaging(int period) {
        this.period = period;
    }

    // function to add new data in the
    // list and update the sum so that
    // we get the new mean
    public void addData(double num) {
        sum += num;
        Dataset.add(num);

        // Updating size so that length
        // of data set should be equal
        // to period as a normal mean has
        if (Dataset.size() > period) {
            sum -= Dataset.remove();
        }
    }

    // function to calculate mean
    public double getMean() {
        return sum / period;
    }

    public ArrayList<Double> running_mean(double[] input_data) {
        ArrayList<Double> toreturn = new ArrayList<Double>();
        for (double x : input_data) {
            addData(x);
            //System.out.println("New number added is " + x
            //        + ", SMA = " + getMean());
            toreturn.add(getMean());
        }
        return toreturn;
    }

    public double[] smoothen(double[] input) {
        ArrayList<Double> get = running_mean(input);
        double[] toret = new double[get.size()];
        for (int i = 0; i < get.size(); i++) {
            toret[i] = get.get(i);
        }
        return Arrays.copyOfRange(toret, 22051, toret.length);
    }

    // Finds all the peaks in data
    public double[] peaks(double[] in, double delta) {

        ArrayList<Double> peakpoints = new ArrayList<Double>();
        double[] x = new double[in.length];

        if (delta <= 0) {
            System.out.println("Input argument delta must be positive");
            System.exit(1);
        }


        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        System.out.println("Initial max is "+max);
        double maxpos = 0, minpos = 0;
        boolean lookformax = true;
        for (int i = 0; i < in.length; i++) {

            double th = in[i];
            if (th > max) {
                max = th;
                maxpos = i;
            }

            if (th < min) {
                min = th;
                minpos = i;
            }

            if (lookformax) {
                if (th < (max - delta)) {
                    System.out.println("Found Max Point!");
                    peakpoints.add(maxpos);
                    min = th;
                    minpos = i;
                    lookformax = false;
                }
            } else {
                if (th > (min + delta)) {
                    System.out.println("Found Min Point!");
                    peakpoints.add(minpos);
                    max = th;
                    maxpos = i;
                    lookformax = true;
                }
            }

        }

        double[] peaks = new double[peakpoints.size()];
        for (int i = 0; i < peakpoints.size(); i++) {
            peaks[i] = peakpoints.get(i);
        }
        return peaks;
    }

}
