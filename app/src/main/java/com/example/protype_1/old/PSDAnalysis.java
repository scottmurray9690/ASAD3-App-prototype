package com.example.protype_1.old;

import java.util.ArrayList;

/**
 *
 * @author murta
 */
public class PSDAnalysis {

    private  double halfsec = 22050;
    private double[] sig;
    private Signal calc;

    public PSDAnalysis(double[] signal) {
        sig = signal;
        calc = new Signal();
    }

    /**
     * method that downsamples the signal
     * @param scale
     */
    public void downsampleSig(int scale){
        //System.out.println("Initial Size of halfsec: "+halfsec);
        double[] res = new double[(sig.length/scale)];
        for(int i = 0; i < (res.length); i+= 1)
            res[i] = sig[i*scale];
        sig = res;
        halfsec = (double)((int)halfsec/scale);
        //System.out.println("Final Size of halfsec: "+halfsec);

    }

    /**
     * method that returns a 2D matrix image of the data
     * @return
     */
    public spectogram getSpec() {
        spectogram spec = new spectogram();
        ArrayList<Double> arr = new ArrayList<Double>();
        int i = 0;
        while ((i < sig.length) && (i + halfsec/2 < sig.length)) {
            i = populate(arr, i);
            spec.add(calc.getPSD(convtodouble(arr)));
        }
        return spec;

    }

    private int populate(ArrayList<Double> in, int i) {
        int sz = in.size();


        if (i + halfsec/2 > sig.length) {
            System.out.println("Index i: "+i+" and signal Length: " + sig.length);
            System.exit(1);
        }

        if (sz > halfsec) {
            System.out.println("Error: Size of Array greater than halfsec");
            System.exit(1);
        }

        if ((sz > 0) && (sz == halfsec)) {
            while (in.size() > sz / 2) {
                in.remove(0);
            }
        }

        while (in.size() < halfsec) {
            in.add(sig[i++]);
            if(i >= sig.length)break;
        }

        while (in.size() < halfsec) {
            in.add((double)0);
            i++;
        }

        return i;

    }

    private double[] convtodouble(ArrayList<Double> arr) {
        double[] ret = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            ret[i] = Double.parseDouble("" + arr.get(i));
        }
        return ret;

    }

}

class spectogram {

    private ArrayList<double[]> data;
    private Signal calc;

    public spectogram() {

        data = new ArrayList<double[]>();
        calc = new Signal();
    }

    /**
     * appends data to the list
     * @param in
     */
    public void add(double[] in) {
        data.add(0,in);

    }

    /**
     * converts the list to a 2D array
     * @return 2D array representing the spectrum
     */
    public double[][] getMatrix() {
        int row = data.size();
        int col = data.get(0).length;
        double[][] array = new double[row][col/2];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col/2; j++) {
                array[i][j] = data.get(i)[j];
            }
        }
        return array;

    }

    public ArrayList retData(){
        return data;
    }

    /**
     * calculate the average power of each row of the spectrum
     * @return array of average power
     */
    public double[] getAvgPower_Row(){
        double[] avgPw = new double[data.size()];
        for (int i = 0; i < data.size();i++)
            avgPw[i] = calc.power(data.get(i));
        return avgPw;
    }

    /**
     * calculate the average power of each column of the spectrum
     * @return array of average power
     */
    public double[] getAvgPower_Col(){
        double[] avgPw = new double[data.get(0).length];
        for (int i = 0; i < data.get(0).length;i++)
            avgPw[i] = getAvgPower_Col(i);
        return avgPw;
    }

    private double getAvgPower_Col(int col){
        double[] temp = new double[data.size()];
        for(int i = 0 ; i < data.size();i++){
            temp[i] = data.get(i)[col];
        }
        return calc.power(temp);


    }

    /**
     * converts the matrix to a string
     * @return the string representation of the matrix
     */
    public String toString() {
        System.out.println("Matrix Size : "
                + data.size()+" x "+data.get(0).length);
        String mat = "";
        int row = data.size();
        int col = data.get(0).length;
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                mat +=(fmt(data.get(i)[j]) + "\t");
            }
            mat += "\n";
        }

        return mat;

    }

    private String fmt(double d)
    {
        if(d == (long) d)
            return String.format("%d",(long)d);
        else
            return String.format("%s",d);
    }

    /**
     * Scales the matrix
     * @param scale
     * @return scaled version of the matrix
     */
    public double[][] Scale(double scale) {

        int inc = (int) (1 / scale);
        int row = data.size();
        int col = data.get(0).length;

        double[][] array = new double[row][col];
        if (scale <= 1) {
            for (int i = 0; i < row; i += 1) {
                for (int j = 0; j < col; j += inc) {
                    array[i][j] = data.get(i)[j];
                }
            }
        } else {
            System.out.println("Scaling Failed: Scale greater than 1.0");
        }

        return array;
    }

}

