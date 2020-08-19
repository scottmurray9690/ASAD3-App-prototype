package com.example.protype_1;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class featureActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Button b1;
    EditText lastName, firstName, age, neckWidth, BMI;
    Spinner sex;
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
        lastName =(EditText)findViewById(R.id.l_name);
        firstName =(EditText)findViewById(R.id.f_name);
        age =(EditText)findViewById(R.id.age);
        neckWidth =(EditText)findViewById(R.id.neck);
        BMI =(EditText)findViewById(R.id.BMI);

        // Spinner element
        sex = (Spinner) findViewById(R.id.Gender);
        // Spinner click listener
        sex.setOnItemSelectedListener(this);

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
        sex.setAdapter(dataAdapter);

    }

    /**
     * method that checks if all features have been entered correctly
     *
     */
    private boolean check(){
        boolean valid = true;
        if(lastName.getText().toString().length() == 0){
            lastName.setError("Input Last Name");
            valid = false;
        }


        if(firstName.getText().toString().length() == 0){
            firstName.setError("Input First Name");
            valid = false;
        }


        if(age.getText().toString().length() == 0){
            age.setError("Input Valid Age");
            valid = false;
        }


        if(neckWidth.getText().toString().length() == 0){
            neckWidth.setError("Input Valid Neck Size");
            valid = false;
        }


        if(BMI.getText().toString().length() == 0){
            BMI.setError("Input Valid BMI");
            valid = false;
        }

        return valid;
    }

    public String writeCsv() {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String initials = firstName.getText().charAt(0) +""+ lastName.getText().charAt(0);
        String fileName = initials +"_"+ currentDate + "Recording1.csv";
        String filePath = baseDir +  File.separator + fileName;
        File tempFile = new File(filePath);

        int recNum = 1;
        while(tempFile.exists()){
            recNum++;

            fileName = initials +"_"+ currentDate + "Recording"+recNum+".csv";
            filePath = baseDir + File.separator + fileName;
            tempFile = new File(filePath);

        }

        Log.i("FeatAct","File path: "+ filePath);
        Log.i("FeatAct","First name: "+ firstName.getText().charAt(0)+ "// "+ firstName.getText());

        FileWriter csvWriter;

        try {
            csvWriter = new FileWriter(tempFile, false);
            csvWriter.append("FirstName,LastName,Sex,Age,NeckWidth,BMI,HoursSlept,Smoking,Snoring,SleepWhileDriving,Tiredness0-5");
            csvWriter.append("\n");
            csvWriter.append(firstName.getText() + ","
                    +lastName.getText()+","
                    +sex.getSelectedItem().toString() + ","
                    +age.getText()+","
                    +neckWidth.getText()+","
                    +BMI.getText()+",");
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    /**
     * go to next feature page (Executed when "NEXT" is clicked)
     * @param View
     *
     */
    public void to_feat1 (View View) {
        //if all entries
        if(check()) {
            Intent i = new Intent(this, feature1Activity.class);
            i.putExtra("FILE_PATH",writeCsv());
            startActivity(i);
        }
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
