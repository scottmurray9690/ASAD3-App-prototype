package com.example.protype_1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class feature1Activity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Button b1;
    EditText ed1;
    Spinner spinner1,spinner2,spinner3,spinner4;
    boolean valid_entries = false;
    public static int prof_num = 1;
    String profile = "Profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feat_1);
        initViews();

    }

    /**
     * Initializes elements on feature 2 page
     */
    private void initViews(){
        ed1  =  (EditText) findViewById(R.id.hours_of_sleep);
        spinner1 = (Spinner) findViewById(R.id.spinner_drive);
        spinner2 = (Spinner) findViewById(R.id.spinner_snore2);
        spinner3 = (Spinner) findViewById(R.id.spinner_rate);
        spinner4 = (Spinner) findViewById(R.id.spinner_smoking);

        // Spinner click listener
        spinner1.setOnItemSelectedListener(this);
        spinner2.setOnItemSelectedListener(this);
        spinner3.setOnItemSelectedListener(this);
        spinner4.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();

        categories.clear();
        //categories.add("");
        categories.add("Don't Know");
        categories.add("Never");
        categories.add("Occasionally");
        categories.add("Usually");


        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner1.setAdapter(dataAdapter1);
        spinner2.setAdapter(dataAdapter2);


        List<String> categories1 = new ArrayList<String>();
        categories1.clear();
        //categories.add("");
        categories1.add("Yes");categories1.add("No");
        ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories1);
        // Drop down layout style - list view with radio button
        dataAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner4.setAdapter(dataAdapter3);

        List<String> categories2 = new ArrayList<String>();
        categories2.clear();
        //categories.add("");
        for(int i = 0; i < 6; i++){categories2.add(""+i);}
        ArrayAdapter<String> dataAdapter4 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories2);
        // Drop down layout style - list view with radio button
        dataAdapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner3.setAdapter(dataAdapter4);


    }


//////EDIT HERE//////////////////////////////////////////////////
    private void read(View view) {
        try {
            FileInputStream fileInputStream= openFileInput("profile");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String lines;
            while ((lines=bufferedReader.readLine())!=null) {
                stringBuffer.append(lines+"\n");
            }
            //textView.setText(stringBuffer.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
////////////////////////////////////////////////////////////////////

    /**
     *
     * @return
     * Method that checks if all features have been entered correctly
     */
    private boolean check(){
        if(ed1.getText().toString().length() == 0){
            ed1.setError("Input Hours of Sleep");
            return false;
        }

        return true;
    }

    /**
     *
     * @param View
     * Go to next Analysis page (Executed when "NEXT" is clicked)
     */
    public void to_ins (View View) {
        //if all entries
        if(check())
            startActivity(new Intent(this, Ins1Activity.class));
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
        if(item.equals("")) {
            Toast.makeText(parent.getContext(), "Please Make a Valid Selection " + item, Toast.LENGTH_LONG).show();
        }

        // Showing selected spinner item
        //Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
}
