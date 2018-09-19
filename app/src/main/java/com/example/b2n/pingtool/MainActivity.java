package com.example.b2n.pingtool;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "APTT";
    Handler handler = new PingHandle();

    //init Views
    EditText input_ip;
    EditText input_repeats;
    EditText input_interval;
    EditText input_size;
    TextView ShowLog;

    Button button_save;
    Button button_clear;
    Button button_run;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    class PingHandle extends Handler{
        public void handleMessage(Message msg){
            if(msg.what == 1){
                //msg handle
            }
            if(msg.what == 2){
                ShowLog.setText(msg.getData().getString("result"));
                //msg handle
            }
        }
    }

    class RunPing implements Runnable{
        @Override
        public void run() {
            StringBuffer sbWritePingLog = new StringBuffer();
            try {
                ping(getTtoS(input_ip), getTtoI(input_repeats), getTtoI(input_interval), getTtoI(input_size), sbWritePingLog);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
    }

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
                        new Thread(new RunPing()).start();
                    }
                }
        );
    }

    public boolean ping(String dest_ip, int dest_repeats, int dest_interval, int dest_size, StringBuffer stringBuffer) throws IOException, InterruptedException {
        int lineNumber = 0;
        Process process = null;
        //BufferedReader bufferedReader = null;
        String cmd = "ping -c " + dest_repeats + " -i " + dest_interval + " -s " + dest_size + " " + dest_ip;
        Bundle bundlePingResult = new Bundle();
        process = Runtime.getRuntime().exec(cmd);
        append(stringBuffer, "Ping test START");
        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = pingReader.readLine()) != null){
            lineNumber++;
            if(lineNumber < 2 || lineNumber > dest_repeats + 1 ){
                append(stringBuffer, "● " + line);
            }else{
                append(stringBuffer, "[" + this.simpleDateFormat.format(new Date(System.currentTimeMillis())) + "]" + "\n" + line);
            }

            bundlePingResult.putString("result", stringBuffer.toString());

            if(this.handler != null){
                Message msg = this.handler.obtainMessage();
                msg.what = 2;
                msg.setData(bundlePingResult);
                this.handler.sendMessage(msg);
            }
        }

        int resultStatus = process.waitFor();
        if(resultStatus == 0) {
            append(stringBuffer, "exec cmd success! cmd : " + cmd);
        }else{
            append(stringBuffer, "exec cmd fail... resultStatus : " + resultStatus);
        }
        append(stringBuffer, "Ping test END");

        //below stringBuffer is above resultStatus and Ping test END string
        bundlePingResult.putString("result", stringBuffer.toString());
        Message msg = this.handler.obtainMessage();
        msg.what = 2;
        msg.setData(bundlePingResult);
        this.handler.sendMessage(msg);

        if(process != null){
            process.destroy();
        }
        if(pingReader != null){
            pingReader.close();
        }

        // 180920_012201
        // Have to implement test passed or failed base on return isSuccess or somethings
        // Have to implement close keyboard function
        // Have to implement SAVE and CLEAR button
        // Have to implement button and EditText disable function
        // Have to implement EmptyCheck function
        // onResume etc...

        // Let's try separating classes

        // Why s9+'s LogView initialized always when push RUN button
        // Check PingHandle part again
        // Check AutoScroll part
        // Check TestRunning part
        // Check several init codes like process, buffer clear or something
        // Check bufferedReader
        // Check exceptions and handler what example at ---
        // Check why start twice


        // study exception. especially, Throws....


        // + SKT ip : 210.220.163.82
        return true;
    }

    // append text to String Buffer
    public static void append(StringBuffer stringBuffer, String text){
        if(stringBuffer != null){
            stringBuffer.append(new StringBuffer(String.valueOf(text)).append("\n").toString());
        }else{
            Log.i(TAG, "StringBuffer is null");
        }
    }

    // convert EditText's text to String
    public String getTtoS(EditText et){
        return et.getText().toString();
    }

    public int getTtoI(EditText et){
        return Integer.parseInt(et.getText().toString());
    }
}
