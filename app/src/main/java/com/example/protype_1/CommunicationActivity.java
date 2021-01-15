package com.example.protype_1;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity implements CommunicationFragment.TimerCallback, SaveRecordingDialog.SaveRecordingDialogListener{
    ImageView spectrogram;
    Button btn_startstop;
    ImageButton btn_adj_up;
    ImageButton btn_adj_down;
    Button btn_snr_reset;
    TextView stateText;
    TextView timerText;

    TextView lowFreqSNRtext;
    TextView midFreqSNRtext;
    TextView highFreqSNRtext;

    CommunicationFragment communicationFragment;

    private final String TAG = "CommAct";
    private static final String TAG_COMMUNICATION_FRAGMENT = "comm_fragment";

    // Model used to keep track of whether or not the app is recording audio.
    private CommunicationViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        // Set up the model so the fragment and activity can both know recording status
        model = new ViewModelProvider(this).get(CommunicationViewModel.class);
        // Observe whether or not it is recording
        final Observer<Boolean> recordingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isRecording) {
                if(isRecording) {
                    displaySoundData();
                    btn_startstop.setText("STOP");
                } else {
                    btn_startstop.setText("START");
                }
            }
        };
        model.getRecording().observe(this, recordingObserver);

        initFragment();
        initWork();
        initOnClicks();

        new AndroidFFMPEGLocator(this);
    }


    // Sets up Communication Fragment (where the processing is done)
    // This allows the communication to continue even if the phone is rotated or if activity is restarted.
    private void initFragment(){
        FragmentManager fm = getSupportFragmentManager();
        communicationFragment = (CommunicationFragment) fm.findFragmentByTag(TAG_COMMUNICATION_FRAGMENT);
        if(communicationFragment == null){
            communicationFragment = new CommunicationFragment();
            fm.beginTransaction().add(communicationFragment, TAG_COMMUNICATION_FRAGMENT).commit();
        }
    }

    private void initWork() {
        spectrogram     = findViewById(R.id.spectrogram);
        btn_startstop   = findViewById(R.id.btn_startstop);
        btn_adj_down    = findViewById(R.id.adjustDownButton);
        btn_adj_up      = findViewById(R.id.adjustUpButton);
        btn_snr_reset   = findViewById(R.id.snrResetButton);
        lowFreqSNRtext  = findViewById(R.id.lowFreqSNR);
        midFreqSNRtext  = findViewById(R.id.midFreqSNR);
        highFreqSNRtext = findViewById(R.id.highFreqSNR);
        stateText       = findViewById(R.id.stateTextView);
        timerText       = findViewById(R.id.timerTextView);
        updateTimer(0);
//        stateText.setText("Hold your breath");
    }

    private void initOnClicks() {
        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    toggleRecording();
                }
        });
        btn_adj_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                communicationFragment.getSpectrogramHelper().changeIntensity(0.50);
            }
        });
        btn_adj_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                communicationFragment.getSpectrogramHelper().changeIntensity(1.50);
            }
        });
        btn_snr_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                communicationFragment.initSnr();
            }
        });
    }

    public void toggleRecording() {
        if(!model.getRecording().getValue()) {
            model.getRecording().setValue(true);
            communicationFragment.startRecording();
            communicationFragment.initSnr();
        } else {
            model.getRecording().setValue(false);
            communicationFragment.stopRecording();
            communicationFragment.saveRecordingPrompt();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        //User wants to discard the recording
        communicationFragment.deleteTempFiles();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        //User wants to save the recording
        communicationFragment.saveRecording();
    }

    @Override
    // Sets up timer to use the fragment, so it isn't reset on device rotation
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof CommunicationFragment) {
            CommunicationFragment commFrag = (CommunicationFragment)fragment;
            commFrag.setTimerCallback(this);
        }
    }

    //Display instructions for getting SNR to the user
    public void updateTimer(long milliseconds){
        if(milliseconds == 0){
            timerText.setText("");
            stateText.setText("Breathe normally");
        } else {
            timerText.setText(String.format("%d", milliseconds / 1000));
            stateText.setText("Hold your breath");
        }
    }

    // Display the spectrogram & SNR Data
    private void displaySoundData() {
        new Thread() {
            public void run() {
                // Update UI as long as its recording
                while (model.getRecording().getValue()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Formatting for SNR Values
                            DecimalFormat df = new DecimalFormat();
                            df.setMaximumFractionDigits(3);
                            df.setMinimumFractionDigits(3);

                            // White paint for background color
                            Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            whitePaint.setColor(Color.WHITE);
                            // Black paint for Text
                            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            textPaint.setColor(Color.BLACK);
                            textPaint.setTextSize(32f);

                            // Set up canvas
                            Bitmap tempBitmap = Bitmap.createBitmap(spectrogram.getWidth(), spectrogram.getHeight(), Bitmap.Config.RGB_565);
                            Canvas tempCanvas = new Canvas(tempBitmap);
                            tempCanvas.drawRect(0,0,tempCanvas.getWidth(),tempCanvas.getHeight(),whitePaint);

                            // Draw in spectrogram bitmap
                            Bitmap spectrogramBMap = communicationFragment.getSpectrogramHelper().getBitmap();
                            tempCanvas.drawBitmap(spectrogramBMap, null, new RectF(48,32, tempCanvas.getWidth(), tempCanvas.getHeight()),null);
                            // Draw the scale
                            for (int i=0;i<11;i++) {
                                if(i<10)
                                    tempCanvas.drawText(" "+i+"k _",0,(int) ((10-i)*(tempCanvas.getHeight()-32)/10) + 32, textPaint);
                                else
                                    tempCanvas.drawText(i+"k _",0,(int) ((10-i)*(tempCanvas.getHeight()-32)/10) + 32, textPaint);
                            }
                            spectrogram.setImageBitmap(tempBitmap);

                            double lowFreqSNR = communicationFragment.getSnrHelper().getSNR(44,1000);
                            double midFreqSNR = communicationFragment.getSnrHelper().getSNR(1000,3000);
                            double highFreqSNR = communicationFragment.getSnrHelper().getSNR(3000,10000);
                            if (lowFreqSNR < 3.0 || midFreqSNR < 3.0 || highFreqSNR < 3.0){
                                lowSNRWarning();
                            }

                            CommunicationActivity.this.lowFreqSNRtext.setText  ("44Hz-1kHz: \t"+df.format(lowFreqSNR));
                            CommunicationActivity.this.midFreqSNRtext.setText  ("1kHz-3kHz: \t"+df.format(midFreqSNR));
                            CommunicationActivity.this.highFreqSNRtext.setText ("3kHz-10kHz:\t"+df.format(highFreqSNR));

                            }

                    });
                    try {
                        // update UI every 100ms, 10Hz refresh rate for the spectrogram
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public void lowSNRWarning(){
        // TODO: display a popup warning the user about the low SNR value and instructing them to move the microphone
        //Log.i(TAG, "Low SNR Value");
    }

}
