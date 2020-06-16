package com.example.protype_1.old;

import java.io.File;

/**
 *
 * @author murta
 */
public class SNR_Calculation {

    private File noise;

    private        double[][] noise_data ;
    private        double[] l_noise ;
    private        double[] r_noise;

    private File data;

    private FileAnalyser fa;

    private Signal sig;

    private PSDAnalysis psd;

    private final int NOISE_DATA = 0;
    private final int BREATH_DATA = 1;

    /**
     * constructor
     */
    public SNR_Calculation() {
        sig = new Signal();
        fa = new FileAnalyser();
        noise = null;
        data = null;
    }

    /**
     * method that sets noise and data variables
     * @param in
     * @param state
     */
    public void setData(File in, int state) {
        if (in != null) {
            if (state == NOISE_DATA) {
                noise = in;
                noise_data =  fa.split(noise);
                filter(50,44100, Filter.PassType.Highpass, 1, noise_data[0]);
                filter(50,44100,Filter.PassType.Highpass, 1, noise_data[1]);

                PSDAnalysis psd1 = new PSDAnalysis(noise_data[0]);
                psd1.downsampleSig(20);

                PSDAnalysis psd2 = new PSDAnalysis(noise_data[1]);
                psd2.downsampleSig(20);

                l_noise = psd1.getSpec().getAvgPower_Row();
                r_noise = psd2.getSpec().getAvgPower_Row();

            } else if (state == BREATH_DATA) {
                data = in;
            }
        }
    }

    /**
     * method not used. Just for testing
     * @return
     */
    public double[] testSNR(){
        double[] SNR = new double[2];
        double[] breath = fa.getFileData(data);//fa.split(data)[0];
        double[] nois = fa.getFileData(noise);

        PSDAnalysis psd1 = new PSDAnalysis(breath);
        psd1.downsampleSig(20);
        spectogram sp1 = psd1.getSpec();

        PSDAnalysis psd2 = new PSDAnalysis(nois);
        psd2.downsampleSig(20);
        spectogram sp2 = psd2.getSpec();


        double[] b = sp1.getAvgPower_Row();
        double[] n = sp2.getAvgPower_Row();

        SNR[0] = (sig.mean(b)/sig.mean(n));
        SNR[1] = (sig.power(b)/sig.power(n));

        return SNR;





    }

    /**
     * method that converts a double to a float
     * @param in
     * @return float version of double
     */
    private float convtofloat(double in){
        return Float.parseFloat(""+in);
    }

    /**
     * method that applys low or high pass filter on data
     * @param freq
     * @param samplerate
     * @param pass
     * @param resonance
     * @param s
     */
    private void filter(int freq, int samplerate, Filter.PassType pass, int resonance,double[] s){
        Filter filter = new Filter(freq, samplerate, pass, resonance);
        for (int i = 0; i < s.length; i++) {
            filter.Update(convtofloat(s[i]));
            s[i] = filter.getValue();
        }
    }

    /**
     * method that calculates the SNR of the two audio channels
     * @return SNR value of channels 0 and 1
     */
    public double[] getSNR() {
        double[] SNR = new double[2];
        if (check()) {
            //double[][] noise_data =  fa.split_two(noise);
            double[][] breath_data =  fa.split(data);
            //double[] l_noise = (new PSDAnalysis(noise_data[0])).getSpec().getAvgPower_Row();
            //double[] r_noise = (new PSDAnalysis(noise_data[1])).getSpec().getAvgPower_Row();
            //double[] l_breath = (new PSDAnalysis(breath_data[0])).getSpec().getAvgPower_Row();
            //double[] r_breath = (new PSDAnalysis(breath_data[1])).getSpec().getAvgPower_Row();
            filter(50,44100,Filter.PassType.Highpass, 1, breath_data[0]);
            filter(50,44100,Filter.PassType.Highpass, 1, breath_data[1]);

            PSDAnalysis psd1 = new PSDAnalysis(breath_data[0]);
            psd1.downsampleSig(20);

            PSDAnalysis psd2 = new PSDAnalysis(breath_data[1]);
            psd2.downsampleSig(20);

            double[] l_breath = psd1.getSpec().getAvgPower_Row();
            double[] r_breath = psd2.getSpec().getAvgPower_Row();



            SNR[0] = snr_calc(l_noise,l_breath,true);
            SNR[1] = snr_calc(r_noise,r_breath,true);
        }
        return SNR;
    }


    private double snr_calc(double[] n, double[] d, boolean mean){
        if(mean)
            return (sig.mean(d)/sig.mean(n));
        else
            return (sig.power(d)/sig.power(n));
    }

    /**
     * method that checks is noise and breath data are valid
     * @return the result true or false
     */
    private boolean check(){
        return (noise != null
                && data != null
                && fa.getFileData(data).length > 0
        );

    }

    private float[] convtofloat(double[] arr) {
        float[] ret = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = Float.parseFloat("" + arr[i]);
        }
        return ret;

    }

}
