package com.example.b2n.pingtool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

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

    Handler handler = new PingHandle();

    ScrollView mLogScrollView = null;

    // declare
    EditText editIp;
    EditText editRepeats;
    EditText editInterval;
    EditText editSize;

    Button saveBtn;
    Button clearBtn;
    Button startBtn;

    TextView showLog;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    class PingHandle extends Handler {
        public void handleMessage(Message msg){
            if (msg.what == 1){
                if(false)   // @@ about progress bar
                    isTestRunning = Boolean.valueOf(false);
            }
            if ((msg.what == 2 || msg.what == 3 ) && showLog != null){
                showLog.setText(msg.getData().getString("result"));
            }

            // @@
            if (mLogScrollView != null){
                mLogScrollView.fullScroll(130); // 자동 스크롤
            }
        }
    }

    // Run Ping Thread (runnable)
    class RunPing implements Runnable{
        @Override
        public void run() {
            try {
                ping(getTtoS(editIp), getTtoI(editRepeats), getTtoI(editInterval), getTtoI(editSize), sbWritePingLog);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // @@ why return?
            return;
        }
    }

    // saveLog
    public void saveLog(){
        if(!isTestRunning){
            if("".equals(showLog.getText().toString()) || showLog == null){
                Toast.makeText(this, "There is no Log for SAVE", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
            stringFileNewName = getDeviceBrand() + "_" + getSystemModel() + "_"
                    + getSystemVersion() + "_" + SDF.format(new Date(System.currentTimeMillis())) + "_Log.txt";
            renameAndSaveLog(stringFileNewName);
        }
    }

    // clearLog
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

        // OBJECT init
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
    }

    //PING
    public boolean ping(String ip, int repeats, int interval, int size, StringBuffer stringBuffer) throws IOException, InterruptedException {

        enabledEditText(false);
        int lineNumber = 0;
        this.isTestRunning = Boolean.valueOf(true);
        boolean isSuccess;
        Message msg1;

        String cmd = "ping -c " + repeats + " " + "-i" + " " + interval
                + " " + "-s" + " " + size + " " + ip;

        Bundle bundlePingResult = new Bundle();

        Process process = Runtime.getRuntime().exec(cmd);
        append(stringBuffer, "//////// Ping Test START ////////");

        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = pingReader.readLine()) != null) {
            lineNumber++;
            if(lineNumber < 2 || lineNumber > repeats + 1){
                append(stringBuffer, "● "+line);
            } else{
                append(stringBuffer, "[" + this.simpleDateFormat.format(new Date(System.currentTimeMillis())) + "]" + "\n" + line);
            }

            bundlePingResult.putString("result", stringBuffer.toString());

            if (this.handler != null) {
                Message msg2 = this.handler.obtainMessage();
                msg2.what = 2;
                msg2.setData(bundlePingResult);
                this.handler.sendMessage(msg2);
            }
        }

        int resultStatus = process.waitFor(); // process.waitFor 기능
        if (resultStatus == 0) {
            append(stringBuffer, "exec cmd success! cmd : " + cmd);
            isSuccess = true;
        } else {
            append(stringBuffer, "exec cmd fail... resultStatus : " + resultStatus);
            isSuccess = false;
        }
        append(stringBuffer, "\n//////// Ping Test END ////////");

        //below stringBuffer is above resultStatus and Ping test END string
        bundlePingResult.putString("result", stringBuffer.toString());
        msg1 = this.handler.obtainMessage();
        msg1.what = 3;
        msg1.setData(bundlePingResult);
        this.handler.sendMessage(msg1);

        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }

        // test END
        this.isTestRunning = Boolean.valueOf(false);
        enabledEditText(true);
        return isSuccess;
    }

    public void onResume() {
        super.onResume();
        closeKeyboard(this.editInterval, this);
    }

    // close key board
    public static void closeKeyboard(EditText mEditText, Context mContext){
        ((InputMethodManager) mContext.getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mEditText.getWindowToken(),0);
    }

    // EditText Empty Check function
    public static boolean checkEmpty(EditText mEditText){
        if("".equals(mEditText.getText().toString()) || mEditText == null)
            return true;
        else
            return false;
    }

    // append
    private static void append(StringBuffer stringBuffer, String text){
        if (stringBuffer != null){
            stringBuffer.append(new StringBuffer(String.valueOf(text)).append("\n").toString());
        }
    }

    // write Txt To File
    public void writeTxtToFile(String strcontent, String filePath, String fileName){
        String strContent = new StringBuilder(String.valueOf(strcontent)).append("\r\n").toString();
        try{
            File file = makeFilePath(filePath, fileName);
            if(!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
                Toast.makeText(MainActivity.this, "aa ", Toast.LENGTH_LONG).show();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e(TAG, "Error on write File : " + e);
        }
    }

    // make File path
    public File makeFilePath(String filePath, String fileName){
        Exception e;
        File file = null;
        makeRootDir(filePath);
        try {
            File file2 = new File(new StringBuilder(String.valueOf(filePath)).append(fileName).toString());
            try {
                if (file2.exists()) {
                    Toast.makeText(MainActivity.this, "AA ", Toast.LENGTH_LONG).show();
                    return file2;
                }
                file2.createNewFile();
                return file2;
            } catch (Exception e2) {
                e = e2;
                file = file2;
                //Create file error
                e.printStackTrace();
                return file;
            }
        } catch (Exception e3) {
            e = e3;
            //Create file error
            e.printStackTrace();
            return file;
        }
    }

    // make Root Directory
    public void makeRootDir(String filePath){
        Exception e;
        try {
            File file = new File(filePath);
            try {
                if (file.exists()) {
                    Log.i(TAG, "dir is exist....: " + file.getPath());
                    return;
                }
                file.mkdirs();
                Log.i(TAG, "make dir" + file.getPath());
            } catch (Exception e2) {
                e = e2;
                Log.i(TAG, String.valueOf(e));
            }
        } catch (Exception e3) {
            e = e3;
            Log.i(TAG, String.valueOf(e));
        }
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
                        if(getTtoS(mname_edit).equals("") || mname_edit.length() <= 4
                                || !getTtoS(mname_edit).substring(mname_edit.length() - 4, mname_edit.length()).equals(".txt")) {
                            mname_edit.setText(mOldName);
                            Toast.makeText(MainActivity.this, "There is no file name\nor\nDoesn't include .txt at end of file name", Toast.LENGTH_LONG).show();
                            return;
                        }
                        stringFileNewName = mname_edit.getText().toString();
                        writeTxtToFile(showLog.getText().toString(), logSavePath, stringFileNewName);
                        Toast.makeText(MainActivity.this, "LOG : " + logSavePath + stringFileNewName, Toast.LENGTH_LONG).show();
                    }
                }).setNegativeButton("NO", new dialogDismiss()).show();
    }

    // convert EditText's texts
    public String getTtoS(EditText et){
        return et.getText().toString();
    }
    public int getTtoI(EditText et){
        return Integer.parseInt(et.getText().toString());
    }

    // dialogDismiss
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
        }
    }
    public void clearLogAndBuffer(TextView tv, StringBuffer stringBuffer){
        if(tv != null)
            tv.setText("");
        if(stringBuffer != null)
            stringBuffer.delete(0, stringBuffer.length());
    }

    // get device information
    public static String getSystemVersion() { return Build.VERSION.RELEASE; }
    public static String getSystemModel() { return Build.MODEL; }
    public static String getDeviceBrand() { return Build.BRAND; }

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