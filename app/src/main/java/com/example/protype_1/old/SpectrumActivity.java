package com.example.protype_1.old;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.example.protype_1.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class for visualizing  the audio data
 */
public class SpectrumActivity extends AppCompatActivity {

    private ImageView spec;
    private double[] toplot;
    private double[][] plot;
    double max ;
    double min;
    private int h,w;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spectrum);
        initViews();
        start_process("Building Spectrum");
        Bundle bundle = getIntent().getExtras();
        h = bundle.getInt("PLOT HEIGHT");
        w = bundle.getInt("PLOT WIDTH");
        toplot = bundle.getDoubleArray("TO PLOT");
        max = getMaxValue(toplot);
        min = getMinValue(toplot);

        plot = stretch(ret_non_Zero(toTwoDimension(toplot,h,w)));
        drawSpectrum();
        stop_process();
    }

    /**
     * method that displays a progress dialog
     * @param msg
     *
     */
    public void start_process(String msg){
        progress.setMessage(msg);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

    }

    /**
     * Closes any open progess dialogs
     */
    public void stop_process(){
        progress.dismiss();
    }

    /**
     * checks if all entries in an array are zeros
     * @param in
     * @return
     *
     *
     */
    private boolean zeros(double [] in){
        for (int i = 0; i < in.length ; i++){
            if(in[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * finds all the non zero rows in a matrix
     * @param in
     * @return matrix of non zero rows
     */
    private double[][] ret_non_Zero(double[][] in){
        ArrayList<Integer> index = new ArrayList<Integer>();
        int count = 0;
        for(int i = 0; i < in.length; i++){
            if(!zeros(in[i])) {
                count++;
                index.add(i);
            }
        }
        double[][] ret = new double[count][in[0].length];
        for(int i =0; i < ret.length; i++){
            ret[i] = Arrays.copyOf(in[index.get(i)],in[0].length);
        }

        w = ret[0].length;
        h = ret.length;

        return ret;
    }

    /**
     * stretches an image
     * @param mat
     * @return array containing the stretched image
     *
     *
     */
    private double[][] stretch(double[][] mat){
        int index = w/h;
        h = h * index;
        double[][] new_mat = new  double[h][w];

        for(int i = 0; i < new_mat.length;i++){

            for(int j = 0; j < new_mat[0].length;j++){
                new_mat[i][j] = mat[i/index][j];

            }
        }

        return new_mat;
    }


    /**
     * initialize elements on spectrum page
     */
    private void initViews() {
        spec = (ImageView) findViewById(R.id.spec);
        progress = new ProgressDialog(this);

    }

    /**
     * finds the maximum value
     * @param array
     * @return the maximum value
     *
     *
     */
    private double getMaxValue(double[] array) {
        double maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
            }
        }
        return maxValue;
    }

    /**
     * finds the minimum value
     * @param array
     * @return the minimum value
     *
     */
    private double getMinValue(double[] array) {
        double minValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
            }
        }
        return minValue;
    }

    /**
     * converts a matrix to an image
     */
    public void drawSpectrum(){

        //define the array size
        int[] rgbValues;
        ;
        rgbValues = new int[plot.length * plot[0].length];

        for(int i = 0; i < h; i++)
        {
            for(int j = 0; j < w; j++) {
                double alph = (plot[i][j] - getMinValue(plot[i]))/(getMaxValue(plot[i]) - getMinValue(plot[i]));
                int c = Color.argb((int)((alph) * 255), 0, 0, 0);
                rgbValues[i * w + j] = c;

            }
        }

        Bitmap bitmap = Bitmap.createBitmap(rgbValues, w, h, Bitmap.Config.ARGB_8888);
        spec.setImageBitmap(bitmap);
    }

    /**
     * concerts a 1D array to a 2D array (matrix)
     * @param input
     * @param row
     * @param col
     * @return a 2D representation of the 1D array
     *
     * concerts a 1D array to a 2D array (matrix)
     */
    private double[][] toTwoDimension(double[] input, int row, int col){
        double[][] output = new double[row][col];
        int count = 0;
        for(int i = 0; i < row; i++){
            for(int j = 0; j < col; j++){
                output[i][j] = input[(i * col) + j];
                if(input[(i * col) + j] != 0)
                    count++;
            }
        }

        return output;
    }




//    public void drawSpectrum(double[][] matrix){
//
//        //define the array size
//        int[] rgbValues;
//        int values;
//        rgbValues = new int[matrix[0].length * matrix.length];
//
//        Log.i("Pixel Value", "Top Left pixel: " + (matrix[0][0]));
//        //Top Right
//        Log.i("Pixel Value", "Top Right pixel: " + (matrix[0][matrix[0].length-1]));
//        //Bottom Left
//        Log.i("Pixel Value", "Bottom Left pixel: " + (matrix[matrix.length-1][0]));
//        //Bottom Right
//        Log.i("Pixel Value", "Bottom Right pixel: " + (matrix[matrix.length-1][matrix[0].length-1]));
//
//
//        for(int i=0; i < h; i++)
//        {
//            for(int j=0; j < w; j++) {
//
//                double alph =  ((matrix[i][j] - 4)/(247 - 4));
//                int c = Color.argb((int)(alph * 255), 0, 0, 0);
//                rgbValues[i * matrix.length + j] = c;
//
//            }
//        }
//
//        Bitmap bitmap = Bitmap.createBitmap(rgbValues, matrix.length, matrix[0].length, Bitmap.Config.ARGB_8888);
//        spec.setImageBitmap(bitmap);
//
//
//    }
}
