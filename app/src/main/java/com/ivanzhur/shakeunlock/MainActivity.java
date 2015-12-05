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
import android.widget.Toast;

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
    boolean watchingAccData, defaultStarted, defaultButtonStart;
    int[] graphNumber;

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
        series.setColor(Color.BLUE);

        graphView.removeAllSeries();
        graphView.addSeries(series);
        graphPoints = new ArrayList<>();

        watchingAccData = true;
        graphNumber = new int[]{R.string.pattern_1, R.string.pattern_2, R.string.pattern_3};
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

        setTextAndButtons();
    }

    private void setTextAndButtons(){
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

        // Start measuring default values
        if (savingDefault > 0 && !defaultStarted && Math.abs(sum-Graph.GRAVITY) < Graph.START_THRESHOLD) return;
        if (!defaultStarted){
            defaultStarted = true;
            instructionsTextView.setVisibility(View.GONE);
            graphView.setVisibility(View.VISIBLE);
        }

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
        // Initial state. Show instructions.
        if (savingDefault == -1){
            graphView.setVisibility(View.GONE);
            statusTextView.setVisibility(View.INVISIBLE);
            instructionsTextView.setVisibility(View.VISIBLE);
            instructionsTextView.setGravity(Gravity.LEFT);
            instructionsTextView.setText(R.string.instructions_text);
            changeButton.setText(R.string.pattern_cancel);
            startStopButton.setText(R.string.pattern_start);
            changeButton.setTextColor(ContextCompat.getColor(this, R.color.red));
            startStopButton.setTextColor(ContextCompat.getColor(this, R.color.green));
            savingDefault = 0;
            watchingAccData = false;
            defaultStarted = false;
            defaultButtonStart = true;
        }
        // User cancelled saving defaults. Return interface to initial state.
        else if (savingDefault == 0 || savingDefault > 3){
            graphView.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
            instructionsTextView.setVisibility(View.GONE);
            changeButton.setText(R.string.pattern_change);
            changeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
            savingDefault = -1;
            watchingAccData = true;
            setTextAndButtons();
        }
    }

    public void onStartStopClick(View view){
        // Saving defaults
        if (savingDefault > -1){
            // Start measurement. Show next instruction, change buttons, clear graphPoints if first time.
            if (defaultButtonStart){
                statusTextView.setVisibility(View.VISIBLE);
                graphView.setVisibility(View.INVISIBLE);
                instructionsTextView.setVisibility(View.VISIBLE);
                changeButton.setVisibility(View.INVISIBLE);

                statusTextView.setText(graphNumber[savingDefault]);
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.black));
                instructionsTextView.setText(R.string.instructions_start);
                instructionsTextView.setGravity(Gravity.CENTER);
                startStopButton.setText(R.string.pattern_stop);
                startStopButton.setTextColor(ContextCompat.getColor(this, R.color.black));

                if (savingDefault == 0) defaults = new ArrayList<>();
                graphPoints.clear();
                savingDefault++;
                watchingAccData = true;
                defaultStarted = false;
                defaultButtonStart = false;
            }
            else { // Stop measurement
                if (!defaultStarted) return; // Graph not measured

                watchingAccData = false;
                if (savingDefault <= 3) defaults.add(new Graph(graphPoints)); // Add measured graph

                if (savingDefault < 3){ // NOT all 3 graphs are measured
                    startStopButton.setText(R.string.pattern_next);
                    defaultButtonStart = true;
                }
                else if (savingDefault < 4){ // All 3 graphs are just measured. Compare them.
                    boolean sim_12 = Graph.compareGraphs(defaults.get(0), defaults.get(1));
                    boolean sim_13 = Graph.compareGraphs(defaults.get(0), defaults.get(2));
                    boolean sim_23 = Graph.compareGraphs(defaults.get(1), defaults.get(2));
                    boolean similar = sim_12 && sim_13 && sim_23;

                    statusTextView.setVisibility(View.VISIBLE);
                    statusTextView.setText((similar) ? R.string.pattern_similar : R.string.pattern_not_similar);
                    statusTextView.setTextColor(ContextCompat.getColor(this, (similar) ? R.color.green : R.color.red));

                    changeButton.setVisibility(View.VISIBLE);
                    changeButton.setText((similar) ? R.string.pattern_dont_save : R.string.pattern_cancel);

                    startStopButton.setText((similar) ? R.string.pattern_save : R.string.pattern_retry);
                    startStopButton.setTextColor(ContextCompat.getColor(this, (similar) ? R.color.green : R.color.black));
                    savingDefault = (similar) ? 4 : 5;
                }
                else if (savingDefault == 4){ // 'Save' pressed. Save defaults and return interface to initial state.
                    saveDefaults();
                    setTextAndButtons();
                    changeButton.setText(R.string.pattern_change);
                    changeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
                    graphView.setVisibility(View.VISIBLE);
                    watchingAccData = true;
                    savingDefault = -1;
                    Toast.makeText(this, R.string.pattern_saved, Toast.LENGTH_SHORT).show();
                }
                else if (savingDefault == 5){ // 'Retry' pressed. Start saving again.
                    savingDefault = 0;
                    defaultButtonStart = true;
                    onStartStopClick(view);
                }
            }

            return;
        }

        // Not saving defaults. Switching service on and off.
        if (preferences.getBoolean(SERVICE_ACTIVE, false)){
            editor.putBoolean(SERVICE_ACTIVE, false);
            stopService(accelerometerService);
        }
        else {
            editor.putBoolean(SERVICE_ACTIVE, true);
            startService(accelerometerService);
        }
        editor.apply();
        setTextAndButtons();
    }

    private void saveDefaults(){
        for (int i=0; i<3; i++) {
            MainActivity.editor.putString(MainActivity.DEFAULT[i], Graph.getJson(defaults.get(i)));
            MainActivity.editor.apply();
        }
    }
}
