package com.example.protype_1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
/**
 * MainActivity
 * This is just a simple title screen that will ask for permissions if they are not granted.
 * NOTE: permissions could be moved to the points which they are needed.
 */

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 11;

    Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
            }
        });

    }

    // Go to next activity
    public void nextActivity() {
        Intent intent = new Intent(this, featureActivity.class);
        startActivity(intent);
    }

    // Asks for permission required for the app to run.
    public void checkPermissions() {
        int permissionCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL );
        } else {
            nextActivity();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, do your work....
                    nextActivity();

                } else {
                    // permission denied
                    // Disable the functionality that depends on this permission.
                }
                return;
            }

            // other 'case' statements for other permssions
        }
    }



}
