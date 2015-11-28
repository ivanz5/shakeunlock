package com.ivanzhur.shakeunlock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor accelerometer;
    GraphView graphView;
    long lastTime = 0;
    boolean logging = false;
    List<GraphPoint> graphPoints;
    LineGraphSeries<DataPoint> series;
    final int maxPointsOnGraph = 100;

    static final String NAME_PREFERENCES = "com.ivanzhur.shakeunlock.sharedpreferences";
    static SharedPreferences preferences;
    static SharedPreferences.Editor editor;
    static final String DEFAULT[] = {"GRAPH_DEFAULT_1", "GRAPH_DEFAULT_2", "GRAPH_DEFAULT_3"};

    final String TAG = "WAKE_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        preferences = getSharedPreferences(NAME_PREFERENCES, Context.MODE_PRIVATE);
        editor = preferences.edit();

        graphView = (GraphView)findViewById(R.id.graphMain);
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

        series = new LineGraphSeries<>();
        series.setColor(Color.BLACK);

        graphView.removeAllSeries();
        graphView.addSeries(series);
        graphPoints = new ArrayList<>();
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        Intent service = new Intent(this, UpdateService.class);
        startService(service);
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
}
