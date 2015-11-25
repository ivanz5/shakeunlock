package com.ivanzhur.shakeunlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.util.List;

public class ScreenOnReceiver extends BroadcastReceiver implements SensorEventListener {

    final String TAG = "WAKE_TEST";

    @Override
    public void onReceive(Context context, Intent intent){
        Log.d(TAG, "onReceive() called with: " + "context = [" + context + "], intent = [" + intent + "]");
        Intent service = new Intent(context, UpdateService.class);
        service.putExtra("screenOn", true);
        context.startService(service);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, "yes");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}