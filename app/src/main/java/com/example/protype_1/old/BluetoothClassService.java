package com.example.protype_1.old;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.util.UUID;
import java.util.Arrays;

public class BluetoothClassService extends Service {
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public static LocalBroadcastManager broadcaster;
    public static final String BT_DEVICE = "btdevice";
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int STATE_NONE = 0; // we're doing nothing
    static final public String RPI_RESULT = "com.rpi.REQUEST_PROCESSED";
    static final public String RPI_MESSAGE = "com.rpi.RPI_MSG";
    static final public String RPI_MESSAGE_BYTES = "com.rpi.RPI_MSG_BYTES";
    private int reConnect = 100;
    private boolean stopThread;
    public static int mState = STATE_NONE;
    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    public boolean connected = false;

    /**
     *
     * @param message
     * This fuction broadcasts the information recieved from the server
     *
     */
    public void sendResult(String message) {
        Intent intent = new Intent(RPI_RESULT);
        if(message != null)
            intent.putExtra(RPI_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    /**
     *
     * @param message
     * This fuction broadcasts the information recieved from the server
     */
    public void sendResult(byte[] message) {
        Intent intent = new Intent(RPI_RESULT);
        if(message != null)
            intent.putExtra(RPI_MESSAGE_BYTES, message);
        broadcaster.sendBroadcast(intent);
    }

    /**
     * This function initializes the bluetooth client
     * @return
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        if (mBluetoothAdapter == null) {
            Log.e("BT Service", "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        stopThread = false;
        Log.d("BLuetoothClassicService", "Service started");
        Toast.makeText(getApplicationContext(), "BL Classic Service started ",Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
        }

        if (mConnectThread != null) {
            mConnectThread.closeStreams();
        }
        Log.d("SERVICE", "onDestroy");
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BluetoothClassService getService() {
            return BluetoothClassService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    private final IBinder mBinder = new LocalBinder();


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BT SERVICE", "SERVICE STARTED");
        Toast.makeText(getApplicationContext(), "onStart Executed ",Toast.LENGTH_LONG).show();
        return super.onStartCommand(intent, flags, startId);
    }

//    private void startForeground() {
//        Intent notificationIntent = new Intent(this, ConnectActivity.class);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                notificationIntent, 0);
//
//        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
//                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
//                .setOngoing(true)
//                //.setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText("Service is running background")
//                .setContentIntent(pendingIntent)
//                .build());
//    }

    /**
     *
     * @param address
     * @return
     *
     * This function allows the client to connect to the server with the
     * give mac address
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("DEBUG BT", "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothDeviceAddress = address;
        if(checkBTState())
            return true;
        else
            return false;
    }

    /**
     *
     * @param data
     *
     * This function sends a string data to the server
     */
    public void writeData(String data){
        if (mConnectThread != null) {
            mConnectThread.write(data);
        }
    }

    /**
     *
     * @param data
     *
     * This function sends a byte value to the server
     */
    public void writeData(byte data){
        if (mConnectThread != null) {
            mConnectThread.write(data);
        }
    }


    private boolean checkBTState() {
        if (mBluetoothAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
            stopSelf();
            return false;
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                Log.d("DEBUG BT", "BT ENABLED! BT ADDRESS : " + mBluetoothAdapter.getAddress() + " , BT NAME : " + mBluetoothAdapter.getName());
                try {
                    while((!connected) && (reConnect >= 0)){
                        Toast.makeText(getApplicationContext(), "Trial: "+reConnect,Toast.LENGTH_LONG).show();
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
                        Log.d("DEBUG BT", "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + mBluetoothAdapter);
                        mConnectThread = new ConnectThread(device);
                        mConnectThread.start();
                        if (connected)
                            return true;

                        else
                            reConnect--;
                    }

                    return false;

                } catch (IllegalArgumentException e) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : " + e.toString());
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE");
                    stopSelf();
                    return false;
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE");
                stopSelf();
                return false;
            }
        }

    }


    /**
     * This class manages the bluetooth connection.
     */
    private class ConnectThread extends Thread {
        private  InputStream mmInStream;
        private  OutputStream mmOutStream;
        private  BluetoothSocket mmSocket;
        private  BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            Log.d("DEBUG BT", "BT UUID : " + MY_UUID);
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("DEBUG BT", "SOCKET CREATED : " + mmSocket.toString());
                mmSocket.connect();
            } catch (Exception e) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopThread = true;
                stopSelf();
            }
            Toast.makeText(getApplicationContext(),"Socket ID: "+temp,Toast.LENGTH_LONG).show();

            if(mmSocket != null && mmSocket.isConnected()) {
                connected = true;
                //reConnect = 0;
                Connected(mmSocket);
            }
            else
            {
                if (mmSocket != null)
                    cancel();
                connected = false;
                stopThread = true;
            }


        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Socket ERROR", "Could not close the client socket", e);
            }
        }

        //creation of the connect thread
        public void Connected(BluetoothSocket socket) {
            Log.d("DEBUG BT", "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[1024];
            int bytes;
            int end = 0;
            // Keep looping to listen for received messages
            while (!stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer

                    if(mmInStream.available() <= 0) {
                        if((new String(getBytes(buffer,bytes), 0, bytes)).equals("END")) {
                            write("EOF");
                            end ++;
                        }
                        else {
                            mmOutStream.write((byte) 1);
                            end = 0;
                        }
                    }
                    if(end <= 1) {
                        byte[] toSend = getBytes(buffer, bytes);
                        sendResult(toSend);
                    }
                } catch (Exception e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }

        public void write(byte i){
            try {
                mmOutStream.flush();
                mmOutStream.write(i);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }

        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.flush();
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }
            //toSend = "";
        }

        public byte[] getBytes(byte[] in, int size){
            return Arrays.copyOfRange(in, 0, size);

        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                if(mmInStream != null
                && mmOutStream != null) {
                    mmInStream.close();
                    mmOutStream.close();
                }
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }
    //////////////////////////////////////////////////////////////////



    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            //Log.d("DEBUG BT", "MAC ADDRESS : " + MAC_ADDRESS);
            Log.d("DEBUG BT", "BT UUID : " + MY_UUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("DEBUG BT", "SOCKET CREATED : " + temp.toString());
            } catch (IOException e) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN");
            // Establish the Bluetooth socket connection.
            // Cancelling discovery as it may slow down connection
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("DEBUG BT", "BT SOCKET CONNECTED");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED");
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                mConnectedThread.write("x");
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
                    stopSelf();
                } catch (IOException e2) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :" + e2.toString());
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                    //insert code to deal with this
                }
            } catch (IllegalStateException e) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : " + e.toString());
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }

        public void closeSocket() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                stopSelf();

            }
        }
    }


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            Log.d("DEBUG BT", "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true && !stopThread) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    //Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    sendResult(readMessage);

                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }




        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }
            //toSend = "";
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }



}
