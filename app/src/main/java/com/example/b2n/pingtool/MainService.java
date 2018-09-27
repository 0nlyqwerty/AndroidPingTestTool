package com.example.b2n.pingtool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.administrator.pingtestingtool.R;

public class MainService extends Service {
    TextView tv;
    private View mView;
    private static WindowManager mManager;
    private static WindowManager.LayoutParams mParams;

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private boolean isMove = false;

    private boolean isEND = false;

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

        //Thread A = new Thread(new A());
        //A.start();

        LayoutInflater mInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflator.inflate(R.layout.always_on_top_view, null);

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
        //@@

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        tv = (TextView) mView.findViewById(R.id.tv);
        tv.setText("abc");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if(mView != null) {
            mManager.removeView(mView);
            mView = null;
        }

        isEND = true;

    }


    class temptemp implements Runnable{
        @Override
        public void run() {

        }
    }

    public static void ping(int dest){
        int lineNumber = 0;
    }
}