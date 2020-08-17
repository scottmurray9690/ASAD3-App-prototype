package com.example.protype_1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketConnectActivity extends AppCompatActivity {
    SocketHandler socketHandler;
    TextView connectionStatus;
    TextView errorMessage;
    Button tryAgainButton;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_connect);
        socketHandler = new SocketHandler();
        connectionStatus = findViewById(R.id.connectionStatus);
        errorMessage = findViewById(R.id.errorMessage);
        progressBar = findViewById(R.id.progressBar);
        try {
            socketHandler.newSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tryAgainButton = findViewById(R.id.retryButton);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    socketHandler.newSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Thread(startConnection).start();
            }
        });
        tryAgainButton.setVisibility(View.INVISIBLE);
        new Thread(startConnection).start();
    }

    private Runnable startConnection = new Runnable(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tryAgainButton.setVisibility(View.INVISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        connectionStatus.setText("Connecting to ASAD3 Device...");
                        errorMessage.setText("");
                    }});
                try {
                    socketHandler.connectTo(InetAddress.getByName("192.168.50.1"), 8888);
                    startCommunicationActivity();
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tryAgainButton.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            connectionStatus.setText("Connection failed.");
                            errorMessage.setText(e.getLocalizedMessage().replace("/192.168.50.1 (port 8888)", "server"));
                        }});
                    e.printStackTrace();
                }
            }};

    private void startCommunicationActivity(){
        Intent nextActivity = new Intent(this, CommunicationActivity.class);
        startActivity(nextActivity);
    }
}