package com.example.protype_1;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
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

public class featureActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Button b1;
    EditText ed1, ed2, ed3, ed4, ed5;
    public static int prof_num = 1;
    String profile = "Profile";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feat);
        initViews();
        //android.support.v7.widget.Toolbar toolbar = (Toolbar) findViewById(R.id.set_up_2);
        //setSupportActionBar(toolbar);
    }


    /**
     * initializes elements on feature 1 page
     */
    private void initViews(){
        b1=(Button)findViewById(R.id.to_feat1);
        ed1=(EditText)findViewById(R.id.l_name);
        ed2=(EditText)findViewById(R.id.f_name);
        ed3=(EditText)findViewById(R.id.age);
        ed4=(EditText)findViewById(R.id.neck);
        ed5=(EditText)findViewById(R.id.BMI);

        // Spinner element
        Spinner spinner = (Spinner) findViewById(R.id.Gender);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("Male");
        categories.add("Female");
        categories.add("Other");
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

    }

    /**
     * method that checks if all features have been entered correctly
     *
     */
    private boolean check(){
        boolean valid = true;
        if(ed1.getText().toString().length() == 0){
            ed1.setError("Input Last Name");
            valid = false;
        }


        if(ed2.getText().toString().length() == 0){
            ed2.setError("Input First Name");
            valid = false;
        }


        if(ed3.getText().toString().length() == 0){
            ed3.setError("Input Valid Age");
            valid = false;
        }


        if(ed4.getText().toString().length() == 0){
            ed4.setError("Input Valid Neck Size");
            valid = false;
        }


        if(ed5.getText().toString().length() == 0){
            ed5.setError("Input Valid BMI");
            valid = false;
        }

        return valid;
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
     * go to next feature page (Executed when "NEXT" is clicked)
     * @param View
     *
     */
    public void to_feat1 (View View) {
        //if all entries
        if(check())
            startActivity(new Intent(this, feature1Activity.class));
   }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
}
