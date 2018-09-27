package com.example.b2n.pingtool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.pingtestingtool.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.view.View.*;
public class MainActivity extends AppCompatActivity {
    String TAG = "APTT";
    String stringFileNewName;
    String logSavePath = (Environment.getExternalStorageDirectory() + "/APTT/PING_LOG/");

    StringBuffer sbWritePingLog = new StringBuffer();

    Boolean isTestRunning = Boolean.valueOf(false);
    Boolean pingTestEnd = false;

    Handler handler = new Handle();

    ScrollView mLogScrollView = null;

    // OBJECT 선언
    EditText editIp;
    EditText editRepeats;
    EditText editInterval;
    EditText editSize;

    Button saveBtn;
    Button clearBtn;
    Button startBtn;

    TextView showLog;
    Switch swRun;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    class Handle extends Handler {
        public void handleMessage(Message msg){
            if (msg.what == 1){

            }
            if ((msg.what == 2) && showLog != null){
                showLog.setText(msg.getData().getString("result"));

            }
            if (msg.what == 3){
                Toast.makeText(MainActivity.this, "msg.what is 3", Toast.LENGTH_LONG).show();
            }

            // @@
            if (mLogScrollView != null){
                mLogScrollView.fullScroll(130); // 자동 스크롤
            }
        }
    }

    class RunPing implements Runnable{
        @Override
        public void run() {
            try {
                enabledEditText(false);
                ping(Util.getTtoS(editIp), Util.getTtoI(editRepeats), Util.getTtoI(editInterval), Util.getTtoI(editSize), sbWritePingLog);
                enabledEditText(true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // @@ why return?
            return;
        }
    }

    // @@ have to implement LOGSAVE
    public void saveLog(){
        if(!isTestRunning){
            if("".equals(showLog.getText().toString()) || showLog == null){
                Toast.makeText(this, "There is no Log for SAVE", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
            stringFileNewName = Util.getDeviceBrand() + "_" + Util.getSystemModel() + "_"
                    + Util.getSystemVersion() + "_" + SDF.format(new Date(System.currentTimeMillis())) + "_Log.txt";
            renameAndSaveLog(stringFileNewName);
        }
    }

    // @@ have to implement LOGCLEAR
    public void clearLog(){
        if(!(isTestRunning.booleanValue()) && (showLog != null)){
            AlertDialog.Builder noticeAlert = new AlertDialog.Builder(this);
            noticeAlert.setTitle("CLEAR LOG");
            noticeAlert.setMessage("Will you clear LOG?");
            noticeAlert.setNegativeButton("NO", new dialogDismiss());
            noticeAlert.setPositiveButton("YES", new clearLogBuf());
            noticeAlert.create();
            noticeAlert.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // OBJECT 초기화
        editIp = (EditText) findViewById(R.id.editIp);
        editRepeats = (EditText) findViewById(R.id.editRepeats);
        editInterval = (EditText) findViewById(R.id.editInterval);
        editSize = (EditText) findViewById(R.id.editSize);

        showLog = (TextView) findViewById(R.id.textLog);

        saveBtn = (Button) findViewById(R.id.btnSave);
        saveBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveLog();
                    }
                }
        );

        clearBtn = (Button) findViewById(R.id.btnClear);
        clearBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearLog();
                    }
                }
        );

        startBtn = (Button) findViewById(R.id.btnStart);
        startBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread(new RunPing()).start();
                    }
                }
        );
        swRun = (Switch) findViewById(R.id.swRun);
        swRun.setBackgroundColor(Color.RED);
        swRun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    // start service code
                    StartService();
                }
                else{
                    // stop service code
                    StopService();
                }
            }
        });
    }

    //PING
    public void ping(String ip, int repeats, int interval, int size, StringBuffer stringBuffer) throws IOException, InterruptedException {
        int lineNumber = 0;
        this.isTestRunning = Boolean.valueOf(true);
        boolean isSuccess;
        Message msg1;   // handler 한테 보내줄때 쓸 msg3

        String cmd = "ping -c " + repeats + " " + "-i" + " " + interval
                + " " + "-s" + " " + size + " " + ip;

        Bundle bundlePingResult = new Bundle();

        Process process = Runtime.getRuntime().exec(cmd);
        Util.append(stringBuffer, "//////// Ping Test START ////////");

        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        this.pingTestEnd = false;

        while (!(this.pingTestEnd)) {
            line = pingReader.readLine();
            lineNumber++;
            if(line == null)
                this.pingTestEnd = true;

            if(!(this.pingTestEnd)){   //testing
                if(lineNumber < 2 || lineNumber > repeats + 1){
                    Util.append(stringBuffer, "● "+line);
                } else{
                    Util.append(stringBuffer, "[" + this.simpleDateFormat.format(new Date(System.currentTimeMillis())) + "]" + "\n" + line);
                }

                bundlePingResult.putString("result", stringBuffer.toString());
                if (this.handler != null) {
                    Message msg2 = this.handler.obtainMessage();
                    msg2.what = 2;
                    msg2.setData(bundlePingResult);
                    this.handler.sendMessage(msg2);
                }
            }else if(this.pingTestEnd){ // test end
                int resultStatus = process.waitFor(); // process.waitFor
                if (resultStatus == 0) {
                    Util.append(stringBuffer, "exec cmd success! cmd : " + cmd);
                    isSuccess = true;   // @@ have to check
                } else {
                    Util.append(stringBuffer, "exec cmd fail... resultStatus : " + resultStatus);
                    isSuccess = false;  // @@ have to check
                }
                Util.append(stringBuffer, "\n//////// Ping Test END ////////");

                bundlePingResult.putString("result", stringBuffer.toString());
                Message msg2 = this.handler.obtainMessage();
                msg2.what = 2;
                msg2.setData(bundlePingResult);
                this.handler.sendMessage(msg2);
            }
        }

        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }

        Message msg4 = this.handler.obtainMessage();
        msg4.what = 4;
        this.handler.sendMessage(msg4);

        // 테스트 종료
        this.isTestRunning = Boolean.valueOf(false);
    }

    public void onResume() {
        super.onResume();
        Util.closeKeyboard(this.editInterval, this);
    }

    // EditText enable or disable
    public void enabledEditText(Boolean enabledEditText){
        if(enabledEditText){
            this.editIp.setEnabled(true);
            this.editRepeats.setEnabled(true);
            this.editInterval.setEnabled(true);
            this.editSize.setEnabled(true);
        }else if(!(enabledEditText)){
            this.editIp.setEnabled(false);
            this.editRepeats.setEnabled(false);
            this.editInterval.setEnabled(false);
            this.editSize.setEnabled(false);
        }else{
            Log.i(TAG, "enableEditText can't enter any if function");
        }
    }

    // Rename And Save Log
    public void renameAndSaveLog(String oldName){
        final String mOldName = oldName;
        View textEntryView = LayoutInflater.from(this).inflate(R.layout.rename_dialog, null);
        final EditText mname_edit = (EditText) textEntryView.findViewById(R.id.rename_edit);
        mname_edit.setText(mOldName);

        new AlertDialog.Builder(this).setView(textEntryView).setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Action for 'yes' button
                        if(Util.getTtoS(mname_edit).equals("") || mname_edit.length() <= 4
                                || !Util.getTtoS(mname_edit).substring(mname_edit.length() - 4, mname_edit.length()).equals(".txt")) {
                            mname_edit.setText(mOldName);
                            Toast.makeText(MainActivity.this, "There is no file name\nor\nDoesn't include .txt at end of file name", Toast.LENGTH_LONG).show();
                            return;
                        }
                        stringFileNewName = mname_edit.getText().toString();
                        Util.writeTxtToFile(showLog.getText().toString(), logSavePath, stringFileNewName);
                        Toast.makeText(MainActivity.this, "LOG : " + logSavePath + stringFileNewName, Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton("NO", new dialogDismiss()).show();
    }



    // dialog dismiss
    class dialogDismiss implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(MainActivity.this, "dialog dismissed", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    // clear showLog and buffer
    class clearLogBuf implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            clearLogAndBuffer(showLog, sbWritePingLog);
            //dialog.dismiss();
        }
    }
    public void clearLogAndBuffer(TextView tv, StringBuffer stringBuffer){
        if(tv != null)
            tv.setText("");
        if(stringBuffer != null)
            stringBuffer.delete(0, stringBuffer.length());
    }


    private void StartService(){
//        if(!MainService.Run){
        Intent intent = new Intent(this, MainService.class);
        startService(intent);
//        }
    }

    private void StopService(){
//        if (MainService.Run) {
        Intent intent = new Intent(this, MainService.class);
        stopService(intent);
//        }
    }
}

// 180921_123202
// Have to implement close keyboard function -> finish. have to apply
// Have to implement EmptyCheck function -> finish. have to apply
// Have to add detail debug log like Log.e

// Have to check request permission

// Let's try separating classes

// Check AutoScroll part
// Check several init codes like process, buffer clear or something
// Check why start twice -> disable Run buttons

// + SKT ip : 210.220.163.82