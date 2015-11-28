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
import android.view.View;
import android.widget.Button;
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
    List<GraphPoint> graphPoints;
    LineGraphSeries<DataPoint> series;
    final int maxPointsOnGraph = 100;
    boolean isSavingDefaults = false;

    TextView statusTextView;
    Button startStopButton;

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
        startStopButton = (Button)findViewById(R.id.startStopButton);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinY(-4);
        graphView.getViewport().setMaxY(24);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(maxPointsOnGraph);
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        //graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        //graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
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

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double sum = Math.sqrt(x*x+y*y+z*z);
            sum = Math.round(sum*100)/100.0;

            graphPoints.add(new GraphPoint(sum, x, y, z));

            int size = graphPoints.size();
            if (size > 1){
                GraphPoint mean = new GraphPoint((graphPoints.get(size-2).value + graphPoints.get(size-1).value)/2, x,y,z);
                graphPoints.add(graphPoints.size()-1, mean);
            }

            if (size > maxPointsOnGraph){
                graphPoints.remove(0);
                graphPoints.remove(0);
            }

            series.resetData(Graph.generateDataForSeries(graphPoints));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onChangeButtonClick(View view){
        Intent intent = new Intent(getApplicationContext(), AddActivity.class);
        startActivity(intent);
    }

    public void onStartStopClick(View view){
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
