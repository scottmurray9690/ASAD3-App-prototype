package com.example.protype_1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class Ins2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruct2);


    }

    /**
     *
     * @param View
     * Go to next instruction page (Executed when "NEXT" is clicked)
     */
    public void ins2_actions (View View) {
        String button_text;
        button_text = ((Button) View).getText().toString();

        switch (button_text) {
            case "Prev":
                startActivity(new Intent(this, Ins1Activity.class));
                break;

            case "Next":
                startActivity(new Intent(this, WifiConnectActivity.class));
                break;

            case "Skip":
                //startActivity(new Intent(this, TestActivity.class));
                break;

            default:
                break;
        }
    }
}
