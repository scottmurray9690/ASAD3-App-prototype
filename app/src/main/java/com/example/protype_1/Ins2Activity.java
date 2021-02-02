package com.example.protype_1;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Ins2Activity
 * Simple instructions for how to use the app & device (page 2)
 *
 */
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
            // This case doesn't exist right now, but should be implemented in the future
            case "Prev":
                startActivity(new Intent(this, Ins1Activity.class));
                break;

            case "Next":
                    startActivity(new Intent(this, Ins3Activity.class));
                break;
            // Used for debugging to skip instructions, not necessary for final app.
            case "Skip":
                //startActivity(new Intent(this, Ins4Activity.class));
                break;

            default:
                break;
        }
    }
}
