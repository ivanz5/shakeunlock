package com.ivanzhur.shakeunlock;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MonitorService extends IntentService implements SensorEventListener {
    final String TAG = "WAKE_TEST";

    SensorManager sensorManager;
    Sensor accelerometer;
    List<Graph> defaults;
    List<List<LiveGraph>> liveGraphs;
    long timeGraphOk[];
    //SharedPreferences preferences;

    public MonitorService(String name) {
        super(name);
    }

    public MonitorService(){
        super("MonitorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent() called with: " + "intent = [" + intent + "]");
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        timeGraphOk = new long[3];

        loadDefaults();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG, "sensor changed");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void loadDefaults(){
        defaults = new ArrayList<>();

        for (int i=0; i<3; i++) {
            String json = MainActivity.preferences.getString(MainActivity.DEFAULT[i], "");
            if (!json.isEmpty()) // temporary, REMOVE WHEN REALIZE SAVING NORMALLY
                defaults.add(new Graph(json));
        }
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

    private void patternRecognized(){

    }
}
