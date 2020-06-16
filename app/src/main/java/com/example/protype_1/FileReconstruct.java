package com.example.protype_1;

import android.util.Log;

import java.io.*;
import android.content.Context;

public class FileReconstruct {
    public File file;
    public File noise_file;
    public File right_file;
    public File right_noise_file;
    public final int NOISE_UPDATE = 0;
    public final int DATA_UPDATE = 1;
    public final int NONE_UPDATE = 2;
    private int which_file = NONE_UPDATE;
    private String wav = ".wav";


    public FileReconstruct(Context context,String pathtofile, boolean noise){
        String filename =  context.getFilesDir() + "/"+pathtofile+wav;
        String rfilename = context.getFilesDir() + "/"+pathtofile + "_right" + wav;
        // check if file exist, otherwise create the file before writing
        try {
            file = new File(filename);
            right_file = new File (rfilename);

            if (!file.exists()) {
                file.createNewFile();
            }
            else {
                file.delete();
                file.createNewFile();
            }

            if (!file.canRead())
                file.setReadable(true);

        }
        catch (Exception e){
            Log.d("FR SERVICE", "UNABLE TO CREATE DATA FILE");
        }

        if (noise) {

            filename = context.getFilesDir() + "/" + pathtofile + "_noise"+ wav;
            rfilename = context.getFilesDir() + "/"+pathtofile + "_right_noise"+ wav;
            // check if file exist, otherwise create the file before writing
            try {

                noise_file = new File(filename);
                right_noise_file = new File(rfilename);

                if (!noise_file.exists()) {
                    noise_file.createNewFile();
                }
                else {
                    noise_file.delete();
                    noise_file.createNewFile();
                }
                if (!noise_file.canRead())
                    noise_file.setReadable(true);


            } catch (Exception e) {
                Log.d("FR SERVICE", "UNABLE TO CREATE NOISE FILE");
            }
        }

    }
////////////////////////////WAV FILE UPDATE AND CREATION//////////////////////////////////
    /**
     * Coverts a file to an array of bytes
     **/
    private byte[] readFileToByteArray(File file){
        FileInputStream fis = null;
        // Creating a byte array using the length of the file
        // file.length returns long which is cast to int
        byte[] bArray = new byte[(int) file.length()];
        try{
            fis = new FileInputStream(file);
            fis.read(bArray);
            fis.close();

        }catch(IOException ioExp){
            ioExp.printStackTrace();
        }
        return bArray;
    }

    /**
     * Deletes all the files that were created
     **/
    public void delete(){
        file.delete();
        noise_file.delete();
        right_file.delete();
        right_noise_file.delete();
    }

    /**
     *  Update the size of the audio file after appending data to it
     **/
    private void updateFilesize(int size, File file){
        int sizea = 0x00000000;
        int sizeb = 0x00000000;
        //byte
        byte[] data = readFileToByteArray(file);
        //int size = 44 +
        sizea += data[7];       sizeb += data[43];
        sizea = sizea << 8;     sizeb = sizeb << 8;
        sizea += data[6];       sizeb += data[42];
        sizea = sizea << 16;    sizeb = sizeb << 16;
        sizea += data[5];       sizeb += data[41];
        sizea = sizea << 24;    sizeb = sizeb << 24;
        sizea += data[4];       sizeb += data[40];

        //System.out.println(String.format("Data[0]: 0x%08X, Data[1]: 0x%08X, Data[2]: 0x%08X, Data[3]: 0x%08X", data[0],data[1],data[2],data[3]));
        //System.out.println(String.format("SizeA: 0x%08X, SizeB: 0x%08X, Size of Data: 0x%08X", sizea,sizeb,size));
        //System.out.println(Arrays.toString(data));
        sizeb += size;
        sizea = (sizeb + ((byte)36 & 0xFF));

        byte[] s = toByteArray(sizea);
        //System.out.println()
        data[4] = s[3];
        data[5] = s[2];
        data[6] = s[1];
        data[7] = s[0];

        s = toByteArray(sizeb);

        data[40] = s[3];
        data[41] = s[2];
        data[42] = s[1];
        data[43] = s[0];

        remakeFile(data, file);
    }

    /**
     *  Converts an integer value to an array of bytes
     **/
    private byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value };
    }

    /**
     * Reconstruct the file after updating its content and file size
     **/
    private void remakeFile(byte[] useThis, File file){
        {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(useThis);
                fos.close();
            }
            catch(FileNotFoundException ex)   {
                System.out.println("FileNotFoundException : " + ex);
            }
            catch(IOException ioe)  {
                System.out.println("IOException : " + ioe);
            }

        }
    }

    /**
     * Append data to an exsisting file
     * **/
    public void appendWavdata(byte[] byteContent1, File file ){

        OutputStream opStream = null;
        int size = byteContent1.length;
        try {
        opStream = new FileOutputStream(file,true);
        opStream.write(byteContent1);
        opStream.flush();
    } catch (IOException e) {
        e.printStackTrace();
    } finally{
        try{
            if(opStream != null) opStream.close();
        } catch(Exception ex){

        }
    }
        updateFilesize(size, file);
    }


    /**
     * Append data to either the NOISE or BREATH file
     * **/
    public void append(byte[] buffer, int state) {
        //checkinfo(buffer);

            switch (state) {
                case (NOISE_UPDATE):
                    appendWavdata(buffer, noise_file);
                    Log.d("STATE DEBUG", "APPEND TO NOISE FILE");
                    break;
                case (DATA_UPDATE):
                    appendWavdata(buffer, file);
                    Log.d("STATE DEBUG", "APPEND TO DATA FILE");
                    break;
                default:
                    break;
            }



    }




    /**
     * Returns the NOISE or BREATH file depending on the state
     * **/
    public File getFile(int state) {
        if (state == NOISE_UPDATE) {
            Log.d("DEBUG","RETURN NOISE FILE");
            return noise_file;

        } else if (state == DATA_UPDATE) {
            Log.d("DEBUG","RETURN BREATH FILE");
            return file;
        } else {
            Log.d("DEBUG","RETURN NULL FILE");
            return null;
        }

    }



 /////////////////////////TXT FILE UPDATE AND CREATION (USED FOR TESTING)//////////////////////////////////

//    public void appendTxtdata(byte[] byteContent1 ){
//
//        OutputStream opStream = null;
//        try {
//            opStream = new FileOutputStream(file,true);
//            opStream.write(byteContent1);
//            opStream.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally{
//            try{
//                if(opStream != null) opStream.close();
//            } catch(Exception ex){
//
//            }
//        }
//    }

    //
//    public String readfile(){
//
//            String line = null;
//
//            try {
//                FileInputStream fileInputStream = new FileInputStream (file);
//                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
//                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//                StringBuilder stringBuilder = new StringBuilder();
//
//                while ( (line = bufferedReader.readLine()) != null )
//                {
//                    stringBuilder.append(line + System.getProperty("line.separator"));
//                }
//                fileInputStream.close();
//                line = stringBuilder.toString();
//
//                bufferedReader.close();
//            }
//            catch(FileNotFoundException ex) {
//                //Log.d(TAG, ex.getMessage());
//            }
//            catch(IOException ex) {
//                //Log.d(TAG, ex.getMessage());
//            }
//            return line;
//
//
//    }
//
//    public int getWhich_file(){
//        return which_file;
//    }
//
//

//    public void test(){
//        try {
//            // catches IOException below
//            final String TESTSTRING = new String("Hello Android");
//
//            /* We have to use the openFileOutput()-method
//             * the ActivityContext provides, to
//             * protect your file from others and
//             * This is done for security-reasons.
//             * We chose MODE_WORLD_READABLE, because
//             *  we have nothing to hide in our file */
//            FileOutputStream fOut = openFileOutput("samplefile.txt",
//                    MODE_PRIVATE);
//            OutputStreamWriter osw = new OutputStreamWriter(fOut);
//
//            // Write the string to the file
//            osw.write(TESTSTRING);
//
//            /* ensure that everything is
//             * really written out and close */
//            osw.flush();
//            osw.close();
//
////Reading the file back...
//
//            /* We have to use the openFileInput()-method
//             * the ActivityContext provides.
//             * Again for security reasons with
//             * openFileInput(...) */
//
//            FileInputStream fIn = openFileInput("samplefile.txt");
//            InputStreamReader isr = new InputStreamReader(fIn);
//
//            /* Prepare a char-Array that will
//             * hold the chars we read back in. */
//            char[] inputBuffer = new char[TESTSTRING.length()];
//
//            // Fill the Buffer with data from the file
//            isr.read(inputBuffer);
//
//            // Transform the chars to a String
//            String readString = new String(inputBuffer);
//
//            // Check if we read back the same chars that we had written out
//            boolean isTheSame = TESTSTRING.equals(readString);
//
//            Log.i("File Reading stuff", "success = " + isTheSame);
//
//        } catch (IOException ioe)
//        {ioe.printStackTrace();}
//    }


    //////////////////////////////////////UNUSED METHODS////////////////////////////////////////////////


    private void checkinfo(byte[] buffer){
        try {
            String readMessage = new String(buffer, 0, buffer.length);
            if (readMessage.equals("NOISE"))
                which_file = NOISE_UPDATE;
            else if (readMessage.equals("DATA"))
                which_file = DATA_UPDATE;
            else if (readMessage.equals("ENDTRANSMISSION"))
                which_file = NONE_UPDATE;
            else
                which_file = which_file;
        }
        catch(Exception e){
            Log.d("DEBUG FR", e.toString());
            Log.d("FR SERVICE", "UNABLE TO CONVERT TO STRING");
        }

    }

    public void append(byte[] buffer, int state, boolean left) {
        //checkinfo(buffer);
        if (left){
            switch (state) {
                case (NOISE_UPDATE):
                    appendWavdata(buffer, noise_file);
                    Log.d("STATE DEBUG", "APPEND TO NOISE FILE");
                    break;
                case (DATA_UPDATE):
                    appendWavdata(buffer, file);
                    Log.d("STATE DEBUG", "APPEND TO DATA FILE");
                    break;
                default:
                    break;
            }
    }
    else
    {
        switch (state) {
            case (NOISE_UPDATE):
                appendWavdata(buffer, right_noise_file);
                Log.d("STATE DEBUG", "APPEND TO NOISE FILE");
                break;
            case (DATA_UPDATE):
                appendWavdata(buffer, right_file);
                Log.d("STATE DEBUG", "APPEND TO DATA FILE");
                break;
            default:
                break;
        }

    }


    }

    public File getNoiseFile(){
        return noise_file;
    }



}
