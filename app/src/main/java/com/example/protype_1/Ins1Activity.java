package com.example.protype_1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Ins1Activity
 * Simple instructions for how to use the app & device (page 1)
 *
 * Most of the UI needs to be updated, but this is still functional
 */
public class Ins1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruct1);

    }

    /**
     *
     * @param View
     * Go to next instruction page (Executed when "NEXT" is clicked)
     */
    public void ins1_actions (View View) {
        String button_text;
        button_text = ((Button) View).getText().toString();

        switch (button_text) {
            case "Prev":
                startActivity(new Intent(this, feature1Activity.class));
                break;

            case "Next":
                startActivity(new Intent(this, Ins2Activity.class));
                break;

            case "Skip":
                //startActivity(new Intent(this, TestActivity.class));
                break;

            default:
                break;
        }
    }
}
