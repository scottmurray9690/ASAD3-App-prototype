package com.example.protype_1.old;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.protype_1.R;
import com.example.protype_1.old.BluetoothClassService;
import com.example.protype_1.old.ConnectActivity;

import java.util.concurrent.TimeUnit;

public class RecordActivity extends AppCompatActivity {

    private long timeCountInMilliSeconds = 1 * 60000;
    private Intent ServiceIntent;
    private BluetoothClassService mBluetoothClassService;
    private static BroadcastReceiver receiver;
    private boolean mBound = false;
    private boolean record_start = false;
    private Button start, reset;
    private EditText text_hr,text_min,text_sec, set_time;
    private ProgressBar progressBarCircle;
    private CountDownTimer countDownTimer;
    private ConnectActivity.TimerStatus timerStatus = ConnectActivity.TimerStatus.STOPPED;

    String res = "";
    private String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            mDeviceAddress = extras.getString(ConnectActivity.EXTRAS_DEVICE_ADDRESS);
        }
        initViews();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] a = intent.getByteArrayExtra(BluetoothClassService.RPI_MESSAGE_BYTES);
                res += (new String(a, 0, a.length) + "\n");

            }
        };
        ServiceIntent = new Intent(this, BluetoothClassService.class);
        doBindService();

    }

    /**
     * initializes elements on the record page
     */
    private void initViews() {
        progressBarCircle = (ProgressBar) findViewById(R.id.progressBarCircle);
        text_hr = (EditText) findViewById(R.id.time_hr);
        text_min = (EditText) findViewById(R.id.time_min);
        text_sec = (EditText) findViewById(R.id.time_sec);
        start= (Button) findViewById(R.id.record_start);
        reset= (Button) findViewById(R.id.record_reset);
        set_time = (EditText) findViewById(R.id.enter_Time);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(BluetoothClassService.RPI_RESULT)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        doUnbindService();
    }



    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothClassService = ((BluetoothClassService.LocalBinder) service).getService();
            Toast.makeText(getApplicationContext(), "Service Connected ",Toast.LENGTH_LONG).show();
            doConnect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothClassService = null;
            Toast.makeText(getApplicationContext(), "Service not Connected ",Toast.LENGTH_LONG).show();

        }

    };

    /**
     * bind to the bluetooth service
     */
    void doBindService() {
        if (getApplicationContext().bindService(ServiceIntent, mServiceConnection, BIND_AUTO_CREATE))
        {
            mBound = true;
        } else {
            Log.e("MY_APP_TAG", "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    /**
     * unbind from the bluetooth service
     */
    void doUnbindService() {
        if (mBound) {
            getApplicationContext().unbindService(mServiceConnection);
            mBound = false;
        }
    }


    private void doConnect(){
        if(mBluetoothClassService.connected) {
            Toast.makeText(getApplicationContext(), "Connection Successful ",Toast.LENGTH_LONG).show();
        }
        else
        {
            if (!mBluetoothClassService.initialize()) {
                Toast.makeText(getApplicationContext(), "BL Classic Service Failed ",Toast.LENGTH_LONG).show();
                finish();
            }

            mBluetoothClassService.connect(mDeviceAddress);
            if(mBluetoothClassService.connected) {
                Toast.makeText(getApplicationContext(), "Connection Successful ",Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Connection Failed ",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * restarts the recording
     * @param view
     */
    public void reset(View view){
        stopCountDownTimer();
        timeCountInMilliSeconds = 0;
        updateTime(timeCountInMilliSeconds);
    }

    /**
     * method to start and stop count down timer
     * @param view
     */
    public void play_n(View view){
        startStop();
    }

    /**
     * method to start and stop count down timer
     */
    private void startStop() {
        if (timerStatus == ConnectActivity.TimerStatus.STOPPED) {

            // call to initialize the timer values
            setTimer();//setTimerValues();
            // call to initialize the progress bar values
            setProgressBarValues();
            // showing the reset icon
//            imageViewReset.setVisibility(View.VISIBLE);
//            // changing play icon to stop icon
//            imageViewStartStop.setImageResource(R.drawable.icon_stop);
//            // making edit text not editable
            text_hr.setEnabled(false);
            text_min.setEnabled(false);
            text_sec.setEnabled(false);
//            // changing the timer status to started
            timerStatus = ConnectActivity.TimerStatus.STARTED;
            start.setText("STOP");
            reset.setVisibility(View.VISIBLE);
            reset.setClickable(true);
            // call to start the count down timer
            startCountDownTimer();

        } else {

            // hiding the reset icon
//            imageViewReset.setVisibility(View.GONE);
//            // changing stop icon to start icon
//            imageViewStartStop.setImageResource(R.drawable.icon_start);
//            // making edit text editable
//            editTextMinute.setEnabled(true);
//            // changing the timer status to stopped
            text_hr.setEnabled(true);
            text_min.setEnabled(true);
            text_sec.setEnabled(true);
            timerStatus = ConnectActivity.TimerStatus.STOPPED;
            start.setText("START");
            reset.setVisibility(View.INVISIBLE);
            reset.setClickable(false);
            stopCountDownTimer();

        }

    }

    private boolean check(){
        boolean valid  = true;
        if(set_time.getText().toString().isEmpty()){
            set_time.setError("Please Enter a Valid Time");
            valid = false;
        }

        String[] arr = set_time.getText().toString().split(":");

        try{
            Integer.parseInt(arr[0].trim());
            Integer.parseInt(arr[1].trim());
            Integer.parseInt(arr[2].trim());
        }

        catch(Exception e){
            set_time.setError("Improper Time Format");
            valid = false;
        }

        return false;

    }

    private void setTimer() {
        double time = 0;
        if (check()) {
            String[] arr = set_time.getText().toString().split(":");
            // fetching value from edit text and type cast to integer
            time +=  ((Double.parseDouble(arr[0].trim()) * 60)
                    + (Double.parseDouble(arr[1].trim()))
                    + (Double.parseDouble(arr[2].trim()) / 60));
        } else {
            // toast message to fill edit text
            Toast.makeText(getApplicationContext(), "Please Enter A time Value", Toast.LENGTH_LONG).show();
        }

        // assigning values after converting to milliseconds
        timeCountInMilliSeconds = (int)(time * 60 * 1000);
    }

    /**
     * method to initialize the values for count down timer
     */
    private void setTimerValues() {
        double time = 0;
        if (!text_hr.getText().toString().isEmpty()
         && !text_min.getText().toString().isEmpty()
         && !text_sec.getText().toString().isEmpty()
         ) {
            // fetching value from edit text and type cast to integer
            time += ((Double.parseDouble(text_hr.getText().toString().trim()) * 60)
                  + (Double.parseDouble(text_min.getText().toString().trim()))
                  + (Double.parseDouble(text_sec.getText().toString().trim()) / 60));
        } else {
            // toast message to fill edit text
            Toast.makeText(getApplicationContext(), "Please Enter A time Value", Toast.LENGTH_LONG).show();
        }

        // assigning values after converting to milliseconds
        timeCountInMilliSeconds = (int)(time * 60 * 1000);
    }

    /**
     * method to start count down timer
     */
    int tup = 0;
    boolean timeup = false;
    private void startCountDownTimer() {

        countDownTimer = new CountDownTimer(timeCountInMilliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateTime(millisUntilFinished);
                progressBarCircle.setProgress((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                updateTime(0);
                setProgressBarValues();
                timerStatus = ConnectActivity.TimerStatus.STOPPED;

                if(!timeup) {
                    mBluetoothClassService.writeData("TIMEUP");
                    timeup = true;
                    tup++;
                }
            }

        }.start();
        countDownTimer.start();
    }

    void updateTime(long millisUntilFinished){
        String time = hmsTimeFormatter(millisUntilFinished);
        text_hr.setText(time.substring(0,2));
        text_min.setText(time.substring(3,5));
        text_sec.setText(time.substring(6,8));
    }

    /**
     * method to stop count down timer
     */
    private void stopCountDownTimer() {
        countDownTimer.cancel();
    }

    /**
     * method to set circular progress bar values
     */
    private void setProgressBarValues() {

        progressBarCircle.setMax((int) timeCountInMilliSeconds / 1000);
        progressBarCircle.setProgress((int) timeCountInMilliSeconds / 1000);
    }


    /**
     * method to convert millisecond to time format
     *
     * @param milliSeconds
     * @return HH:mm:ss time formatted string
     */
    private String hmsTimeFormatter(long milliSeconds) {

        String hms = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));

        return hms;


    }

}
