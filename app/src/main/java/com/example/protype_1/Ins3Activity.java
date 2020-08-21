package com.example.protype_1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class Ins3Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruct3);

    }

    /**
     *
     * @param View
     * Go to next paired devices page (Executed when "NEXT" is clicked)
     */
    public void ins3_actions (View View) {
        String button_text;
        button_text = ((Button) View).getText().toString();

        switch (button_text) {
            case "Prev":
                startActivity(new Intent(this, Ins2Activity.class));
                break;

            case "Next":
                startActivity(new Intent(this, Ins4Activity.class));
                break;

            case "Skip":
                //startActivity(new Intent(this, TestActivity.class));
                break;

            default:
                break;
        }
    }
}
