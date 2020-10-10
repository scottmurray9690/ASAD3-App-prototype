package com.example.protype_1;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;

import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class CommunicationActivity extends AppCompatActivity implements CommunicationFragment.TimerCallback{
    ImageView spectrogram;
    Button btn_startstop;
    ImageButton btn_adj_up;
    ImageButton btn_adj_down;
    Button btn_snr_reset;
    TextView stateText;
    TextView timerText;

    TextView lowFreqSNR;
    TextView midFreqSNR;
    TextView highFreqSNR;

    CommunicationFragment communicationFragment;
//    SpectrogramHelper spectrogramHelper;
//    SNRHelper snrHelper;
//    AudioAnalyzer audioAnalyzer;
//    SendReceive sendReceive;
//    SocketHandler socketHandler;

    boolean recording;
    private final String TAG = "CommAct";
    private static final String TAG_COMMUNICATION_FRAGMENT = "comm_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);
        initFragment();
        initWork();
        initOnClicks();

        new AndroidFFMPEGLocator(this);
    }

    private void initWork() {
        spectrogram     = findViewById(R.id.spectrogram);
        btn_startstop   = findViewById(R.id.btn_startstop);
        btn_adj_down    = findViewById(R.id.adjustDownButton);
        btn_adj_up      = findViewById(R.id.adjustUpButton);
        btn_snr_reset   = findViewById(R.id.snrResetButton);
        lowFreqSNR      = findViewById(R.id.lowFreqSNR);
        midFreqSNR      = findViewById(R.id.midFreqSNR);
        highFreqSNR     = findViewById(R.id.highFreqSNR);
        stateText       = findViewById(R.id.stateTextView);
        timerText       = findViewById(R.id.timerTextView);

        if(communicationFragment.isRecording()){
            recording = true;
            btn_startstop.setText("STOP");
            displaySoundData();
        } else {
            recording = false;
            btn_startstop.setText("START");
        }
        updateTimer(0);

//        stateText.setText("Hold your breath");
    }

    private void initFragment(){
        FragmentManager fm = getSupportFragmentManager();
        communicationFragment = (CommunicationFragment) fm.findFragmentByTag(TAG_COMMUNICATION_FRAGMENT);
        if(communicationFragment == null){
            communicationFragment = new CommunicationFragment();
            fm.beginTransaction().add(communicationFragment, TAG_COMMUNICATION_FRAGMENT).commit();
        }
    }

    private void initOnClicks() {
        btn_startstop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if(!recording) { //!communicationFragment.isRecording()
                        recording = true;
                        btn_startstop.setText("STOP");
                        communicationFragment.startRecording();
                        displaySoundData();
                        communicationFragment.initSnr();
                    } else {
                        recording = false;
                        communicationFragment.stopRecording();
                        btn_startstop.setText("START");
                    }
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

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(fragment instanceof CommunicationFragment) {
            CommunicationFragment commFrag = (CommunicationFragment)fragment;
            commFrag.setTimerCallback(this);
        }
    }

    public void updateTimer(long milliseconds){
        if(milliseconds == 0){
            timerText.setText("");
            stateText.setText("Breathe normally");
        } else {
            timerText.setText(String.format("%d", milliseconds / 1000));
            stateText.setText("Hold your breath");
        }
    }


    private void displaySoundData() {
        new Thread() {
            public void run() {
                while (recording) {
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

                            double lowSNR = communicationFragment.getSnrHelper().getSNR(44,1000);
                            double midSNR = communicationFragment.getSnrHelper().getSNR(1000,3000);
                            double highSNR = communicationFragment.getSnrHelper().getSNR(3000,10000);
                            if (lowSNR < 3.0 || midSNR < 3.0 || highSNR < 3.0){
                                lowSNRWarning();
                            }

                            lowFreqSNR.setText  ("44Hz-1kHz: \t"+df.format(lowSNR));
                            midFreqSNR.setText  ("1kHz-3kHz: \t"+df.format(midSNR));
                            highFreqSNR.setText ("3kHz-10kHz:\t"+df.format(highSNR));

                            }

                    });
                    try {
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
