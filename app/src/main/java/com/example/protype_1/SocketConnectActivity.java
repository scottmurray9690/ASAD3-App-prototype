package com.example.protype_1;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;

/**
 * SocketConnectActivity
 * Attempts to connect to the ASAD3 Rpi device
 */
public class SocketConnectActivity extends AppCompatActivity {
    SocketHandler socketHandler;
    TextView connectionStatus;
    TextView errorMessage;
    Button tryAgainButton;
    ProgressBar progressBar;

    @Override
    /**
     * Sets up socket and attempts to start socket connection.
     *
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_connect);

        // Create sockethandler so socket can persist between activities
        socketHandler = new SocketHandler();

        // set up UI elements
        connectionStatus = findViewById(R.id.connectionStatus);
        errorMessage = findViewById(R.id.errorMessage);
        progressBar = findViewById(R.id.progressBar);
        tryAgainButton = findViewById(R.id.retryButton);
        // set up "try again" button
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

        // Set up the socket
        try {
            socketHandler.newSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Start the connection
        new Thread(startConnection).start();
    }

    /**
     * Starts the connection
     * if it fails it notifies the user and allows them to try again
     * if it succeeds then proceed to CommunicationActivity
     */
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
                    // try to connect
                    socketHandler.connectTo(InetAddress.getByName("192.168.50.1"), 8888);
                    startCommunicationActivity();
                } catch (IOException e) {
                    //if it fails, prompt user to try again
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