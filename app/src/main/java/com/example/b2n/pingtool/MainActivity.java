package com.example.b2n.pingtool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    int a=0;

    //
    EditText input_ip;
    EditText input_repeats;
    EditText input_interval;
    EditText input_size;
    EditText ShowLog;

    Button button_save;
    Button button_clear;
    Button button_run;



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ShowLog = findViewById(R.id.LogView);
        input_ip = findViewById(R.id.InputIP);
        input_repeats = findViewById(R.id.InputRepeats);
        input_interval = findViewById(R.id.InputInterval);
        input_size = findViewById(R.id.InputSize);

        //Button Click
        button_save = findViewById(R.id.SaveButton);
        button_save.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShowLog.setText("Save button Clicked");
                    }
                }
        );

        button_clear = findViewById(R.id.ClearButton);
        button_clear.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShowLog.setText("Clear button Clicked");
                    }
                }
        );

        button_run = findViewById(R.id.RunButton);
        button_run.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ping(getTtoS(input_ip), getTtoS(input_repeats), getTtoS(input_interval), getTtoS(input_size));
                    }
                }
        );
    }

    public void ping(String dest_ip, String dest_repeats, String dest_interval, String dest_size){
        Process process = null;
        String cmd = "ping -c " + dest_repeats + " -W " + dest_interval + " -i " + dest_size + " " + dest_ip;
        ShowLog.setText(cmd.toString());
        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int result = process.exitValue();

        if(result == 0) {
            ShowLog.setText("Test Pass" + result);
        }else{
            ShowLog.setText("Test Fail" + result);
        }

    }

    public String getTtoS(EditText et){
        return et.getText().toString();
    }
}
