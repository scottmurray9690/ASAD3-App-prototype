package com.example.protype_1;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
/**
 * Ins4Activity
 * Simple instructions for how to use the app & device (page 4)
 *
 * This page leads to the SocketConnectActivity, which is where the more complicated stuff begins
 *
 */
public class Ins4Activity extends AppCompatActivity {
    Button btn_next;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruct4);
        btn_next = findViewById(R.id.btn_next);
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextActivity();
            }
        });
    }
    private void nextActivity(){
        startActivity(new Intent(this, SocketConnectActivity.class));
    }
}