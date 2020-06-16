package com.example.protype_1;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    // Go to next activity
    public void Ganesh(View View) {
        String button_text;
        button_text = ((Button) View).getText().toString();
        Intent ganesh = new Intent(this, featureActivity.class);
        startActivity(ganesh);
    }

}
