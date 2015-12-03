package com.ivanzhur.shakeunlock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    Intent accelerometerService;
    SensorManager sensorManager;
    Sensor accelerometer;
    GraphView graphView;
    List<Graph> defaults;
    List<GraphPoint> graphPoints;
    LineGraphSeries<DataPoint> series;
    final int maxPointsOnGraph = 100;
    int savingDefault = -1;
    long lastUpdateTime = 0;
    boolean watchingAccData = true;

    TextView statusTextView, instructionsTextView;
    Button changeButton, startStopButton;

    static final String NAME_PREFERENCES = "com.ivanzhur.shakeunlock.sharedpreferences";
    static SharedPreferences preferences;
    static SharedPreferences.Editor editor;
    static final String DEFAULT[] = {"GRAPH_DEFAULT_1", "GRAPH_DEFAULT_2", "GRAPH_DEFAULT_3"};
    static final String SERVICE_ACTIVE = "SERVICE_ACTIVE";

    final String TAG = "WAKE_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accelerometerService = new Intent(this, UpdateService.class);
        preferences = getSharedPreferences(NAME_PREFERENCES, Context.MODE_PRIVATE);
        editor = preferences.edit();

        setUI();

        series = new LineGraphSeries<>();
        series.setColor(Color.BLACK);

        graphView.removeAllSeries();
        graphView.addSeries(series);
        graphPoints = new ArrayList<>();
    }

    public void setUI(){
        graphView = (GraphView)findViewById(R.id.graphMain);
        statusTextView = (TextView)findViewById(R.id.statusTextView);
        instructionsTextView = (TextView)findViewById(R.id.instructionsTextView);
        changeButton = (Button)findViewById(R.id.changeButton);
        startStopButton = (Button)findViewById(R.id.startStopButton);

        instructionsTextView.setVisibility(View.GONE);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinY(-4);
        graphView.getViewport().setMaxY(24);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(maxPointsOnGraph);
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graphView.getGridLabelRenderer().setPadding(0);

        if (preferences.getBoolean(SERVICE_ACTIVE, false)){
            statusTextView.setText(R.string.service_running);
            startStopButton.setText(R.string.service_stop);
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.green));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
        else {
            statusTextView.setText(R.string.service_stopped);
            startStopButton.setText(R.string.service_start);
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.green));
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (preferences.getBoolean(SERVICE_ACTIVE, false))
            startService(accelerometerService);
    }

    @Override
    protected void onStop(){
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (!watchingAccData || sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        if (savingDefault > 0 && System.currentTimeMillis() - lastUpdateTime < Graph.MIN_MEASURE_TIME_DELTA) return;
        lastUpdateTime = System.currentTimeMillis();

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double sum = Math.sqrt(x*x+y*y+z*z);
        sum = Math.round(sum*100)/100.0;

        graphPoints.add(new GraphPoint(sum, x, y, z));

        int size = graphPoints.size();
        if (savingDefault <= 0 && size > 1){ // Add mean between two last points to smoother look, only if not saving default
            GraphPoint mean = new GraphPoint((graphPoints.get(size-2).value + graphPoints.get(size-1).value)/2, x,y,z);
            graphPoints.add(graphPoints.size()-1, mean);
        }

        if (size > maxPointsOnGraph){
            if (savingDefault > 0) onStartStopClick(null); // No more than max points in default graph
            else {
                graphPoints.remove(0);
                graphPoints.remove(0);
            }
        }

        series.resetData(Graph.generateDataForSeries(graphPoints));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onChangeButtonClick(View view){
        Intent intent = new Intent(getApplicationContext(), AddActivity.class);
        //startActivity(intent);

        if (savingDefault == -1){
            graphView.setVisibility(View.GONE);
            statusTextView.setVisibility(View.GONE);
            instructionsTextView.setVisibility(View.VISIBLE);
            changeButton.setText(R.string.pattern_cancel);
            startStopButton.setText(R.string.pattern_start);
            changeButton.setTextColor(ContextCompat.getColor(this, R.color.red));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.green));
            savingDefault = 0;
            watchingAccData = false;
        }
        else if (savingDefault == 0){
            graphView.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
            instructionsTextView.setVisibility(View.GONE);
            changeButton.setText(R.string.pattern_change);
            changeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
            boolean serviceActive = preferences.getBoolean(SERVICE_ACTIVE, false);
            startStopButton.setText((serviceActive) ? R.string.service_stop : R.string.service_start);
            startStopButton.setTextColor(ContextCompat.getColor(this, (serviceActive) ? R.color.red : R.color.green));
            savingDefault = -1;
            watchingAccData = true;
        }
    }

    public void onStartStopClick(View view){
        if (savingDefault > -1){
            if (savingDefault == 0){
                instructionsTextView.setText(R.string.instructions_start);
                instructionsTextView.setGravity(Gravity.CENTER);
                defaults = new ArrayList<>();
                graphPoints.clear();
                savingDefault = 1;
            }

            return;
        }


        Log.i(TAG, "act: " + preferences.getBoolean(SERVICE_ACTIVE, false));
        if (preferences.getBoolean(SERVICE_ACTIVE, false)){
            statusTextView.setText(R.string.service_stopped);
            startStopButton.setText(R.string.service_start);
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.green));
            editor.putBoolean(SERVICE_ACTIVE, false);

            stopService(accelerometerService);
        }
        else {
            statusTextView.setText(R.string.service_running);
            startStopButton.setText(R.string.service_stop);
            statusTextView.setTextColor(ContextCompat.getColor(this, R.color.green));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.red));
            editor.putBoolean(SERVICE_ACTIVE, true);

            startService(accelerometerService);
        }
        editor.apply();
    }
}
