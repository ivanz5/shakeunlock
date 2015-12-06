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

public class ScreenOnOffReceiver extends BroadcastReceiver {

    final String TAG = "WAKE_TEST";

    @Override
    public void onReceive(Context context, Intent intent){
        Log.i(TAG, "onReceive() called with: " + "context = [" + context + "], intent = [" + intent + "]");
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            Intent service = new Intent(context, UpdateService.class);
            service.putExtra("screenOff", true);
            context.startService(service);
            Log.i(TAG, "onReceive: service started");
        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
            Intent service = new Intent(context, UpdateService.class);
            context.stopService(service);
            context.startService(service);
            Log.i(TAG, "onReceive: restart service");
        }
    }
}
