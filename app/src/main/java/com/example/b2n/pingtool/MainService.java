package com.example.b2n.pingtool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.administrator.pingtestingtool.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainService extends Service {
    public static String TAG = "APTT";
    StringBuffer sbWritePing = new StringBuffer();
    private static boolean RunTest = false;
    Handler handler = new Handle();
    TextView tv;
    private View mView;
    private static WindowManager mManager;
    private static WindowManager.LayoutParams mParams;

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private boolean isMove = false;

    private boolean isEND = false;

    class Handle extends Handler {
        public void handleMessage(Message msg){
            if (msg.what == 1 ){
                tv.setText(msg.getData().getString("test"));
            }
        }
    }

    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = mParams.x;
                    mViewY = mParams.y;

                    break;

                case MotionEvent.ACTION_MOVE:
                    isMove = true;

                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    final int num = 5;
                    if ((x > -num && x < num) && (y > -num && y < num)) {
                        isMove = false;
                        break;
                    }

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;

                    mManager.updateViewLayout(mView, mParams);

                    break;
            }

            return true;
        }
    };

    @Override
    public void onCreate(){
        super.onCreate();
        RunTest = true;
        //Thread A = new Thread(new A());
        //A.start();

        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.always_on_top_view, null);

        mView.setOnTouchListener(mViewTouchListener);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        tv = (TextView) mView.findViewById(R.id.tv);
        new Thread(new runPing()).start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        RunTest = false;

        if(mView != null) {
            mManager.removeView(mView);
            mView = null;
        }

        isEND = true;

    }


    class runPing implements Runnable{
        @Override
        public void run() {
            try {
                ping("127.0.0.1", sbWritePing);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void ping(String dest, StringBuffer sb) throws IOException, InterruptedException {
        dest = "127.0.0.1";
        String cmd = "ping -i "+ dest;
        Process process = Runtime.getRuntime().exec(cmd);

        Bundle bundlePingResult = new Bundle();

        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while(RunTest){
            line = pingReader.readLine();
            Log.d(TAG, line);
            if(line == null)
                break;
            bundlePingResult.putString("test", line.toString());
            Message msg = this.handler.obtainMessage();
            msg.what = 1;
            msg.setData(bundlePingResult);
            this.handler.sendMessage(msg);
        }
        process.waitFor();


        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }
    }
}

/*
HAVE TO IMPLEMENT
1. Get core data...
for example : 64 bytes from 127.0.0.1: icmp_seq=6 ttl=64 time=0.327 ms

2. Get dest ip address from MainActivity - (EditText)

3. calculate min, max, avr pings

4. and display calculated values at another view !

5. change WARNING color when delay too much
 */