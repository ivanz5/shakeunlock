package com.ivanzhur.shakeunlock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UpdateService extends Service {

    final String TAG = "WAKE_TEST";
    SharedPreferences preferences;
    SensorManager sensorManager;
    Sensor accelerometer;
    List<Graph> defaults;
    List<List<LiveGraph>> liveGraphs;
    long timeGraphOk[];
    boolean working;
    SensorEventListener listener;
    long lastUpdateTime;

    @Override
    public void onCreate(){
        super.onCreate();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        //filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver receiver = new ScreenOnReceiver();
        registerReceiver(receiver, filter);
        Log.i(TAG, "ScreenOnReceiver registered from service");

        preferences = getSharedPreferences(MainActivity.NAME_PREFERENCES, MODE_PRIVATE);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        lastUpdateTime = 0;
        timeGraphOk = new long[3];
        liveGraphs = new ArrayList<>();
        loadDefaults();
        if (defaults.size() > 0)
            for (int i = 0; i < 3; i++)
                liveGraphs.add(new ArrayList<LiveGraph>());
        working = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand() called with: " + "intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        if (intent == null || !intent.getBooleanExtra("screenOn", false)) return START_STICKY;

        working = true;
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (System.currentTimeMillis() - lastUpdateTime < 25) return; // FIXME: 25/11/2015 change 25 with constant variable
                lastUpdateTime = System.currentTimeMillis();

                Log.i(TAG, "sensor changed");
                Sensor sensor = sensorEvent.sensor;
                if (sensor.getType() != Sensor.TYPE_ACCELEROMETER || !working) return;

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                double sum = Math.sqrt(x * x + y * y + z * z);
                updateLiveGraphs(new GraphPoint(sum, x, y, z));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Log.i(TAG, "Screen ON");
        return START_STICKY;
    }

    /*@Override
    public void onDestroy(){
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d(TAG, "onDestroy() called with: " + "");
    }*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateLiveGraphs(GraphPoint point){
        // Do all for 3 default graphs
        for (int i=0; i<3; i++){
            // Add new LiveGraph to start watching from current point
            if (Math.abs(point.value - Graph.GRAVITY) >= Graph.START_THRESHOLD)
                liveGraphs.get(i).add(new LiveGraph(defaults.get(i)));

            // Iterate trough all LiveGraphs and compare them to default
            int j = 0;
            while (j < liveGraphs.get(i).size()){
                // Add new point to LiveGraph and compare it to default graph
                int result = liveGraphs.get(i).get(j).addPoint(point);

                if (result == LiveGraph.GRAPHS_EQUAL || result == LiveGraph.GRAPHS_NOT_EQUAL){
                    Log.i("GRAPH", result + "  peaks: " + liveGraphs.get(0).get(0).numPeaks); // Logs, 80% unnecessary
                    // If LiveGraph isn't equal to it's default remove it from 'watch list'
                    liveGraphs.get(i).remove(j);
                    j--;

                    // If equal check if others equal and if yes, stop monitoring, success
                    // If no, continue monitoring
                    if (result == LiveGraph.GRAPHS_EQUAL) {
                        timeGraphOk[i] = System.currentTimeMillis();
                        boolean allOk = true;
                        for (long time : timeGraphOk)
                            if (timeGraphOk[i] - time > Graph.MAX_TIME_OK_DIFF) allOk = false;

                        if (allOk) patternRecognized();
                    }
                }

                j++;
            }
        }
    }

    private void loadDefaults(){
        defaults = new ArrayList<>();

        for (int i=0; i<3; i++) {
            String json = preferences.getString(MainActivity.DEFAULT[i], "");
            if (!json.isEmpty()) // temporary, REMOVE WHEN REALIZE SAVING NORMALLY
                defaults.add(new Graph(json));
        }
    }

    private void patternRecognized(){
        Log.i(TAG, "Great! Pattern recognized!");
        working = false;
        sensorManager.unregisterListener(listener);
        for (List<LiveGraph> g : liveGraphs) g.clear();
    }
}
