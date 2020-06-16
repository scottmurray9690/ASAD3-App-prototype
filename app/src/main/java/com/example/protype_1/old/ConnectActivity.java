package com.example.protype_1.old;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.os.CountDownTimer;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.protype_1.FileReconstruct;
import com.example.protype_1.R;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

@TargetApi(18)
public class ConnectActivity extends AppCompatActivity {

    private long timeCountInMilliSeconds = 1 * 60000;
    private boolean process_start;

    public enum TimerStatus {
        STARTED,
        STOPPED
    }

    private TimerStatus timerStatus = TimerStatus.STOPPED;

    private ProgressBar progressBarCircle;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothClassService mBluetoothLeService;
    private boolean mConnected = false;
    private FileReconstruct newrecon;
    /// for test
    private boolean press = false;
    public static BroadcastReceiver receiver;
    private Button send, next, reset;
    private TextView textViewTime;
    private TextView results, SNR_L, SNR_T;
    private ProgressDialog progress;
    private Intent gattServiceIntent;
    public boolean mBound = false;
    private Intent spec;
    private SNR_Calculation calc;

    // Data Collection States
    private final int HOLD_STATE = 0;
    private final int BREATH_STATE = 1;
    private int state;
    public double[][] specdata;

    // Data Analysis State
    private final int GOOD_SNR = 3;
    private final int BAD_SNR = 4;
    private int snrstate = -1;

    private CountDownTimer countDownTimer;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Bundle extras = getIntent().getExtras();
        state = HOLD_STATE;
        if(extras !=null) {
            mDeviceAddress = extras.getString(ConnectActivity.EXTRAS_DEVICE_ADDRESS);
        }
        initViews();

        calc = new SNR_Calculation();
        progress = new ProgressDialog(this);
        newrecon = new FileReconstruct(getApplicationContext(),"Test_1",true);
        results.setMovementMethod(new ScrollingMovementMethod());
        int count = 0;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] a = intent.getByteArrayExtra(BluetoothClassService.RPI_MESSAGE_BYTES);
                if((new String(a, 0, a.length)).equals("END")) {
                    results.append("\nSwitching to Hold State, End Signal Right\n ");

                    if(state == BREATH_STATE){
                        //start_process("Analysing Signal");
                        calc.setData(newrecon.getFile(newrecon.NOISE_UPDATE), HOLD_STATE);
                        calc.setData(newrecon.getFile(newrecon.DATA_UPDATE), BREATH_STATE);
                        updateSNR(calc.getSNR());
                        snrmsg();
                        //stop_process();
                        show_reset();
                        stop_process();
                        //send.setClickable(true);
                    }

                    if(state == HOLD_STATE){
                        state = BREATH_STATE;
                        updatemsg();
                        //send.setClickable(true);
                        //progress.dismiss();
                        send.setClickable(true);
                        stop_process();

                    }

                }
                else {
                    newrecon.append(a, state);
                }
            }
        };
        gattServiceIntent = new Intent(this, BluetoothClassService.class);;
        mBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        progress = new ProgressDialog(this);
        start_process("Connecting to :) "+mDeviceAddress);
        initmsg();
    }

    /**
     * go to record page
     * @param view
     *
     */
    public void to_record(View view){
        Intent intent = new Intent(this, RecordActivity.class);
        intent.putExtra(ConnectActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        progress.dismiss();
        startActivity(intent);

    }

    @Override
    public void onResume(){
        mBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        super.onResume();

    }

    /**
     * method that initializes elements on analysis page
     */
    private void initViews() {
        progressBarCircle = (ProgressBar) findViewById(R.id.progressBarCircle);
        textViewTime = (TextView) findViewById(R.id.time_min);
        send = (Button) findViewById(R.id.send);
        results = (TextView) findViewById(R.id.Results);
        SNR_L = (TextView) findViewById(R.id.SNR_lung);
        SNR_T = (TextView) findViewById(R.id.SNR_trachea);
        next = (Button) findViewById(R.id.to_record);
        reset = (Button) findViewById(R.id.reset);
    }

    /**
     * method makes the "NEXT" button visible
     */
    private void show_next(){
        next.setVisibility(View.VISIBLE);
    }

    /**
     * method makes the "RESET" button visible
     */
    private void show_reset(){
        reset.setVisibility(View.VISIBLE);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.spec_menu, menu);
        //menu.findItem(R.id.menu_stop).setVisible(true);
        menu.findItem(R.id.menu_spec).setVisible(true);
        //menu.findItem(R.id.menu_refresh).setActionView(
        //R.layout.actionbar_indeterminate_progress);
        return true;
    }

    /**
     * method that converts a double to a float
     * @param in
     * @return
     *
     */
    private float convtofloat(double in){
        return Float.parseFloat(""+in);
    }

    /**
     * method that applys a low or high pass filter to the data values
     * @param freq
     * @param samplerate
     * @param pass
     * @param resonance
     * @param s
     *
     */
    private void filter(int freq, int samplerate, Filter.PassType pass, int resonance, double[] s){
        Filter filter = new Filter(freq, samplerate, pass, resonance);
        for (int i = 0; i < s.length; i++) {
            filter.Update(convtofloat(s[i]));
            s[i] = filter.getValue();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_spec:
                //start_process("Transfering Spectrum Data");
                Intent spec = new Intent(this, SpectrumActivity.class);
                File temp = newrecon.getFile(newrecon.DATA_UPDATE);
                double[] da = (new FileAnalyser()).getFileData(temp);
                filter(50,44100,Filter.PassType.Highpass, 1, da);
                PSDAnalysis pd = (new PSDAnalysis(da));
                pd.downsampleSig(20);
                double[][] mat = pd.getSpec().getMatrix();

                spec.putExtra("PLOT HEIGHT",mat.length);
                spec.putExtra("PLOT WIDTH",mat[0].length);
                double[] oned = toOneDimension(mat);
                spec.putExtra("TO PLOT",oned);
                startActivity(spec);
                break;
            default:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private double[][] stretch(double[][] mat){
        double[][] new_mat = new  double[mat.length*50][mat[0].length];

        for(int i = 0; i < new_mat.length;i++){

            for(int j = 0; j < new_mat[0].length;j++){
                new_mat[i][j] = mat[i/50][j];
            }
        }
        return new_mat;
    }


    /**
     * method to convert a 2D array to a 1D array
     * @param input
     * @return
     *
     */
    private double[] toOneDimension(double[][] input){
        double[] output = new double[input.length * input[0].length];
        for(int i = 0; i < input.length; i++){
            for(int j = 0; j < input[i].length; j++){
                output[(i * input[0].length) + j] = input[i][j];
            }
        }

        return output;
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
     *
     * method that clears any open progress dialog
     */
    public void stop_process(){
        progress.dismiss();
        process_start = false;
    }


    /**
     * method to displays the result of the Signal Processing
     * @param snr_value
     *
     */
    private void updateSNR(double[] snr_value){
        SNR_L.setText(""+snr_value[0]);
        SNR_T.setText(""+snr_value[1]);

        if (snr_value[1] > 2){
            snrstate = GOOD_SNR;
        }
        else
            snrstate = BAD_SNR;

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        if (mBluetoothLeService.connected)
            mBluetoothLeService.writeData("Exit");
        if(mBound) {
            stopService(gattServiceIntent);
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onDestroy();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothClassService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(getApplicationContext(), "BL Classic Service Failed ",Toast.LENGTH_LONG).show();
                finish();
            }

            if(!mBluetoothLeService.connect(mDeviceAddress)) {
                Toast.makeText(getApplicationContext(), "Connection Failed ",Toast.LENGTH_LONG).show();
                //mBound = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                //start_process("Reconnecting ");

                //finish();
            }
            else
            {
                progress.dismiss();
                Toast.makeText(getApplicationContext(), "Connection Successful ",Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            getApplicationContext().unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }

    };

    /**
     * method which sends commands to the server
     * @param view
     *
     */
    public void sendCmd(View view){
        switch(state) {
            case (HOLD_STATE):
                startStop();
                mBluetoothLeService.writeData("ANALYSIS");
                break;
            case (BREATH_STATE):
                mBluetoothLeService.writeData("ANALYSIS");
                startStop();
                break;
        }

        send.setClickable(false);


    }

    /**
     * method which makes "NEXT" and "RESET" buttons invisible
     */
    private void clear_button(){
        reset.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
    }

    /**
     * method to restarts the analysis
     *
     * @param view
     *
     */
    public void reset(View view){
        textViewTime.setText("00");
        state = HOLD_STATE;
        snrstate = -1;
        newrecon.delete();
        send.setClickable(true);
        SNR_L.setText("0.0000");
        SNR_T.setText("0.0000");
        updatemsg();
        clear_button();


    }

    /**
     * method to set initial instructions
     */
    private void initmsg(){
        results.setText("Hold Breath for 6 seconds");
    }

    /**
     * method for providing user with instructions on how to use the app
     */
    private void updatemsg(){
        switch(state){
            case(HOLD_STATE):
                results.setText("Hold Breath for 6 seconds");
                break;
            case(BREATH_STATE):
                results.setText("Breath In and Out Continuously for 6 seconds");
                break;
            default:
                results.setText("");
                break;

        }
    }


    /**
     * method to start and stop count down timer
     */
    private void startStop() {
        if (timerStatus == TimerStatus.STOPPED) {
            setTimerValues();
            setProgressBarValues();
            timerStatus = TimerStatus.STARTED;
            startCountDownTimer();
            timeup = false;

        } else {

            timerStatus = TimerStatus.STOPPED;
            stopCountDownTimer();

        }

    }

    public void test(View view){
        if(!press) {
            mBluetoothLeService.writeData((byte) 2);
            press = !press;
        }
        else{
            mBluetoothLeService.writeData("STOPTRANSMIT");
            press = !press;
        }


    }

    /**
     * Method for providing user with feed back based on SNR value
     */
    private void snrmsg(){
        switch(snrstate) {
            case (GOOD_SNR):
                results.setText("Signal is Good! Click 'NEXT' to Proceed");
                show_next();

                //go to next Activity
                break;
            case (BAD_SNR):
                results.setText("" +
                        "Poor SNR Value." +
                        "\nPossible Solutions:" +
                        "\nAdjust Position of the Microphones please");
                progress.dismiss();
                break;
            default:
                break;
        }



    }

    private void sendStop(){
        mBluetoothLeService.writeData("STOPTRANSMIT");
    }


    private void reset() {
        stopCountDownTimer();
        startCountDownTimer();
    }

    public void st(View view){
        setTimerValues();
        startStop();
    }




    /**
     * method to initialize the values for count down timer
     */
    private void setTimerValues() {
        double time = 0;
        switch(state){
            case HOLD_STATE:
                time = 0.1;
                textViewTime.setText("06");
                break;
            case BREATH_STATE:
                time = 0.1;
                textViewTime.setText("06");
                break;
            default:
                time = 0;
                textViewTime.setText("00");
                break;
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
                textViewTime.setText(hmsTimeFormatter(millisUntilFinished).substring(6,8));
                progressBarCircle.setProgress((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                textViewTime.setText(hmsTimeFormatter(timeCountInMilliSeconds).substring(6,8));
                setProgressBarValues();
                timerStatus = TimerStatus.STOPPED;

                if(!timeup) {
                    mBluetoothLeService.writeData("TIMEUP");
                    if (state == HOLD_STATE)
                        start_process("Recieveing Data From Device");
                    else if (state == BREATH_STATE)
                        start_process("Analyzing Data");

                    timeup = true;
                    tup++;
                }
            }

        }.start();
        countDownTimer.start();
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