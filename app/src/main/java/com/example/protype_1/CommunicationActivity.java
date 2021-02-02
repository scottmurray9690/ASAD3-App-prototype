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

/**
 * CommunicationActivity
 * Activity which manages and displays all the data being exchanged with the raspberry pi.
 * This activity is mostly in charge of the UI, while most of the processing is done on CommunicationFragment.
 *
 * There are some helper classes that work with this Activity:
 *  CommunicationFragment
 *      Manages most of the processing so that nothing is lost when the device is rotated (IE this activity is restarted)
 *  CommunicationViewModel
 *      Very simple ViewModel that keeps track of whether or not the ASAD3 device is recording audio and shares it with
 *      CommunicationFragment and CommunicationActivity
 *
 * Interfaces:
 *  CommunicationFragment.TimerCallback
 *      Used to update the countdown timer in the UI, maintaining its value when this activity is restarted.
 *  SaveRecordingDialog.SaveRecordingDialogListener
 *      Used so the activity can respond to the user pressing a button on the dialog that prompts "do you want to save the recording"
 *
 */
public class CommunicationActivity extends AppCompatActivity implements CommunicationFragment.TimerCallback, SaveRecordingDialog.SaveRecordingDialogListener{
    // UI elements
    ImageView spectrogram;
    Button btn_startstop;
    Button btn_snr_reset;
    ImageButton btn_adj_up;
    ImageButton btn_adj_down;
    TextView stateText;
    TextView timerText;
    TextView lowFreqSNRtext;
    TextView midFreqSNRtext;
    TextView highFreqSNRtext;

    // CommunicationFragment
    CommunicationFragment communicationFragment;
    private static final String TAG_COMMUNICATION_FRAGMENT = "comm_fragment";

    // Tag for logging
    private final String TAG = "CommAct";


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


    /**
     * Sets up Communication Fragment (where the processing is done)
     * This allows the communication to continue even if the phone is rotated or if activity is restarted.
     */
    private void initFragment(){
        FragmentManager fm = getSupportFragmentManager();
        communicationFragment = (CommunicationFragment) fm.findFragmentByTag(TAG_COMMUNICATION_FRAGMENT);
        // if this is the first time launching the activity (fragment not initialized), set up the fragment.
        if(communicationFragment == null){
            communicationFragment = new CommunicationFragment();
            fm.beginTransaction().add(communicationFragment, TAG_COMMUNICATION_FRAGMENT).commit();
        }
    }

    // Sets up all the UI elements
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
    }

    // Sets up onClicks
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
    /**
     * Toggles recording,
     * resets SNR if its starting
     * prompts to save if its stopping
     */
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

    /**
     * The next 2 methods are for the dialog that pops up prompting users to save their recording or not
     * onDialogNegativeClick: user clicked 'cancel'
     * onDialogPositiveClick: user clicked 'save'
     */
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

    /**
     * This method sets up the timer/countdown UI element to work in the fragment
     */
    @Override
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof CommunicationFragment) {
            CommunicationFragment commFrag = (CommunicationFragment)fragment;
            commFrag.setTimerCallback(this);
        }
    }
    /**
     * Display instructions for getting SNR to the user
     */
    public void updateTimer(long milliseconds){
        if(milliseconds == 0){
            timerText.setText("");
            stateText.setText("Breathe normally");
        } else {
            timerText.setText(String.format("%d", milliseconds / 1000));
            stateText.setText("Hold your breath");
        }
    }

    /**
     * Displays the current SNR and spectrogram to the user
     */
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

                            // get and display the SNR
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
