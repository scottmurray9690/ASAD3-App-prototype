package com.example.protype_1.old;


import com.badlogic.audio.io.WaveDecoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class FileAnalyser {

    private final float MAX_VALUE = 1.0f / Short.MAX_VALUE;

    /**
     *
     * @param sourceFile
     * @return
     * Seperates the audio data into two channels
     */
    public double[][] split(File sourceFile) {
        byte sample[] = new byte[2];
        byte[] bytes;
        byte[][] data;
        ByteArrayOutputStream leftbaos = new ByteArrayOutputStream();
        ByteArrayOutputStream rightbaos = new ByteArrayOutputStream();
        try {
            FileInputStream FI = new FileInputStream(sourceFile);
            while (true) {
                int readsize = FI.read(sample);
                if (readsize == -1) {
                    break;
                }
                rightbaos.write(sample, 0, sample.length / 2);                  //channel 0 is left channel in wav files, channel 1 is right channel
                leftbaos.write(sample, sample.length / 2, sample.length / 2);
            }
        } catch (Exception io) {
            System.out.println("Error When Fetching Data");
        } finally {
        }
        byte[] leftData = leftbaos.toByteArray();
        byte[] rightData = rightbaos.toByteArray();

        return retLeftnRight(leftData, rightData);

    }

    /**
     *
      * @param left
     * @param right
     * @return
     *
     * Takes two arrays and converts then to a matix (2 x N matrix)
     */
    private double[][] retLeftnRight(byte[] left, byte[] right) {
        byte[][] toret1 = new byte[2][left.length - 22];            // -22 because the first 44 bytes of a wav file are headers, and we split the wav file in 2 pieces

        for (int i = 22; i < left.length; i++) {
            toret1[0][i - 22] = left[i];
            toret1[1][i - 22] = right[i];
        }

        for (int j = 0; j < toret1[0].length - 1; j += 2) {
            byte temp = toret1[0][j];
            toret1[0][j] = toret1[1][j + 1];
            toret1[1][j + 1] = temp;
        }

        double[] LF = convtodouble(byteToFloat(toret1[0]));
        double[] RG = convtodouble(byteToFloat(toret1[1]));
        double[][] toret = new double[2][LF.length];

        for (int i = 0; i < LF.length; i++) {
            toret[0][i] = LF[i];
            toret[1][i] = RG[i];
        }
        return toret;
    }

    /**
     *
     * @param input
     * @return
     *
     * Converts a byte array to a float array
     */
    public float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length / 2];
        for (int x = 0; x < input.length; x += 2) {
            int temp = (input[x]  & 0xff);
            temp |= (input[x + 1] & 0xff) << 8;
            ret[x / 2] = ((short)temp * MAX_VALUE)/1;
        }

        return ret;
    }

    /**
     *
     * @param sourceFile
     * @return array of audio data
     *
     * Returns the data in the audio file
     */
    public double[] getFileData(File sourceFile) {
        float samples[] = new float[2];
        ArrayList<Float> data = new ArrayList<Float>();
        try {
            FileInputStream file = new FileInputStream(sourceFile);
            WaveDecoder decoder = new WaveDecoder(file);
            while (decoder.readSamples(samples) > 0) {
                addtoList(data, samples);
            }
        } catch (Exception io) {
            System.out.println("Error When Fetching Data");
        }
        return convtodouble(data);

    }

//    public double[] getWavData(File sourceFile) {
//        byte sample[] = new byte[2];
//        ArrayList<Byte> data = new ArrayList<Byte>();
//        ByteArrayOutputStream wavdata = new ByteArrayOutputStream();
//        try {
//            FileInputStream FI = new FileInputStream(sourceFile);
//            while (true) {
//                int readsize = FI.read(sample);
//                if (readsize == -1) {
//                    break;
//                }
//                add(data,sample);
//                if (check(data)){
//                    FI.read(sample);
//                    FI.read(sample);
//                    break;
//                }
//            }
//
//
//            while(true){
//                int readsize = FI.read(sample);
//                if (readsize == -1) {
//                    break;
//                }
//                wavdata.write(sample, 0, sample.length);
//            }
//        } catch (Exception io) {
//            System.out.println("Error When Fetching Data");
//        }
//        byte[] in = wavdata.toByteArray();
//        byte[] toret1 = new byte[in.length - 44];
//
//        return convtodouble(byteToFloat(in));
//    }

    /**
     *
     * @param in
     * @return
     *
     * Checks if the content of the byte is a header
     */
    public boolean check(ArrayList<Byte> in){
        return ((in.get(0) == 100)
                && (in.get(1) == 97)
                && (in.get(2) == 116)
                && (in.get(3) == 97)
        );
    }

    /**
     *
     * @param in
     * @param data
     *
     * Adds data to in.
     */
    public void add(ArrayList<Byte> in, byte[] data){
        if (in.size() == 4){
            in.remove(0);
            in.remove(0);
        }
        in.add(data[0]);
        in.add(data[1]);
    }

    /**
     *
     * @param arr
     * @return
     *
     * converts a Float to a double array
     */
    private double[] convtodouble(ArrayList<Float> arr) {
        double[] ret = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            ret[i] = Double.parseDouble("" + arr.get(i));
        }
        return ret;

    }

    /**
     *
     * @param arr
     * @return
     *
     * converts a float array to a double array
     */
    private double[] convtodouble(float[] arr) {
        double[] ret = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            ret[i] = Double.parseDouble("" + arr[i]);
        }
        return ret;

    }

    /**
     *
     * @param arr
     * @param samples
     *
     * Adds the data in an array to a List
     */
    private void addtoList(ArrayList<Float> arr, float[] samples) {
        for (int i = 0; i < samples.length; i++) {
            arr.add(samples[i]);
        }
    }


}
