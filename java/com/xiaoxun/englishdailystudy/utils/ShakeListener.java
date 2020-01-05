package com.xiaoxun.englishdailystudy.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class ShakeListener implements SensorEventListener {

    private static final int FORCE_THRESHOLD = 2000;
    private static final int FORCE_THRESHOLD_SLOW = 600;
    private static final int TIME_THRESHOLD = 100;

    private Shakeable mActivity;
    public boolean isShaking = false;
    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;

    public ShakeListener(Shakeable ac){
        mActivity = ac;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        long now = System.currentTimeMillis();


        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(event.values[0] + event.values[1]
                    + event.values[2] - mLastX - mLastY - mLastZ)
                    / diff * 10000;


            if (speed > FORCE_THRESHOLD && !isShaking) {
                mActivity.onShake();
                isShaking = true;
                Log.e("xxxx","isShanking." + speed);
            }else if(speed < FORCE_THRESHOLD_SLOW && isShaking){
                isShaking = false;
                Log.e("xxxx","is not Shanking." + speed);
            }
            mLastTime = now;
            mLastX = event.values[0];
            mLastY = event.values[1];
            mLastZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface Shakeable{
        void onShake(Object... objs);
    }
}
