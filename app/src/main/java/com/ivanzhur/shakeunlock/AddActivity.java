package com.ivanzhur.shakeunlock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class AddActivity extends Activity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor accelerometer;
    long lastTime = 0;
    List<GraphPoint> points;
    GraphView graphView;
    LineGraphSeries<DataPoint> series;

    Button button, buttonNew;
    TextView bottomTextView;

    boolean isSaving = false, isStarted = false;
    int savingDefault = -1;

    List<Graph> defaults;
    List<List<LiveGraph>> liveGraphs;

    final String DEFAULT[] = {"GRAPH_DEFAULT_1", "GRAPH_DEFAULT_2", "GRAPH_DEFAULT_3"};
    final int COLOR[] = {Color.RED, Color.GREEN, Color.BLUE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //savedVals = new ArrayList<>();
        //previousVals = new ArrayList<>();
        defaults = new ArrayList<>();
        points = new ArrayList<>();

        liveGraphs = new ArrayList<>(); // Maybe move to where the (defaults.size > 0) ?

        graphView = (GraphView)findViewById(R.id.graph);
        button = (Button)findViewById(R.id.button1);
        buttonNew = (Button)findViewById(R.id.button2);
        bottomTextView = (TextView)findViewById(R.id.bottomTextView);
        bottomTextView.setVisibility(View.GONE);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(20);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(100);

        series = new LineGraphSeries<>(Graph.generateDataForSeries(points));
        series.setColor(Color.BLACK);
        //series.setDrawDataPoints(false);
        graphView.addSeries(series);

        loadDefaults();
        if (defaults.size() > 0) {
            Graph.compareGraphs(defaults.get(0), defaults.get(1));
            Graph.compareGraphs(defaults.get(0), defaults.get(2));
            Graph.compareGraphs(defaults.get(1), defaults.get(2));
            for (int i=0; i<3; i++)
                liveGraphs.add(new ArrayList<LiveGraph>());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isStarted) return;
        if (!isSaving) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                double sum = Math.sqrt(x * x + y * y + z * z);
                if (Math.abs(sum - Graph.GRAVITY) > Graph.START_THRESHOLD) isSaving = true;
                else return;

                bottomTextView.setVisibility(View.GONE);
                button.setVisibility(View.VISIBLE);
            }
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 25) return;
        lastTime = currentTime;

        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && isSaving) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double sum = Math.sqrt(x*x+y*y+z*z);
            sum = Math.round(sum*100)/100.0;

            points.add(new GraphPoint(sum, x, y, z));
            if (points.size() > 100) points.remove(0);

            //savedVals.add(sum);
            series.resetData(Graph.generateDataForSeries(points));

            if (savingDefault < 0) updateLiveGraphs(new GraphPoint(sum, x, y, z));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onButtonClick(View view){
        if (isSaving){
            series.resetData(new DataPoint[0]);
            isSaving = false;
            isStarted = false;
            button.setText("Start");

            if (savingDefault >= 0){
                defaults.add(new Graph(points));
                savingDefault++;
                points.clear();

                if (savingDefault == 3){
                    savingDefault = -1;
                    saveDefaults();

                    graphView.removeAllSeries();
                    graphView.addSeries(Graph.getSeries(defaults.get(0).points, Color.RED, false));
                    graphView.addSeries(Graph.getSeries(defaults.get(1).points, Color.GREEN, false));
                    graphView.addSeries(Graph.getSeries(defaults.get(2).points, Color.BLUE, false));
                    graphView.addSeries(series);
                    for (int i=0; i<3; i++){
                        String message = "Graph " + i + "\nnumPeaks: " + defaults.get(i).numPeaks + "\npeaks: ";
                        for (int j=0; j<defaults.get(i).numPeaks; j++) message += defaults.get(i).points.get(defaults.get(i).peaks.get(j)) + "; ";
                        message += "\n";
                        Log.i("GRAPH", message);
                    }

                    Graph.compareGraphs(defaults.get(0), defaults.get(1));
                    Graph.compareGraphs(defaults.get(0), defaults.get(2));
                    Graph.compareGraphs(defaults.get(1), defaults.get(2));
                }
            }
            else {
                graphView.removeAllSeries();
                graphView.addSeries(series);

                buttonNew.setVisibility(View.VISIBLE);
            }
        }
        else if (!isStarted) {
            isStarted = true;
            button.setVisibility(View.GONE);
            buttonNew.setVisibility(View.GONE);
            bottomTextView.setVisibility(View.VISIBLE);
            button.setText("Stop");

            if (savingDefault < 0) {
                liveGraphs.clear();
                for (int i = 0; i < 3; i++) {
                    liveGraphs.add(new ArrayList<LiveGraph>());
                }
            }
        }
    }

    public void onButtonStartClick(View view){
        defaults.clear();
        savingDefault = 0;
        isStarted = true;
        button.setVisibility(View.GONE);
        buttonNew.setVisibility(View.GONE);
        bottomTextView.setVisibility(View.VISIBLE);
        button.setText("Stop");
    }

    private void updateLiveGraphs(GraphPoint point){
        if (liveGraphs.get(0).size() < 1)
            liveGraphs.get(0).add(new LiveGraph(defaults.get(0)));

        int result0 = liveGraphs.get(0).get(0).addPoint(point);
        Log.i("GRAPH", result0 + "  peaks: " + liveGraphs.get(0).get(0).numPeaks);


        for (int i=0; i<3; i++){
            // Add new LiveGraph to start watching from current point
            if (Math.abs(point.value - Graph.GRAVITY) >= Graph.START_THRESHOLD)
                liveGraphs.get(i).add(new LiveGraph(defaults.get(i)));

            //Log.i("GRAPH", "Updating... " + liveGraphs.get(i).size());
            int j = 0;
            while (j < liveGraphs.get(i).size()){
                int result = liveGraphs.get(i).get(j).addPoint(point);
                if (result == LiveGraph.GRAPHS_EQUAL || result == LiveGraph.GRAPHS_NOT_EQUAL){
                    Log.i("GRAPH", result + "");
                    liveGraphs.get(i).remove(j);
                    j--;
                    if (result == LiveGraph.GRAPHS_EQUAL)
                        onButtonClick(null);
                }

                j++;
            }

        }
    }

    private void saveDefaults(){
        for (int i=0; i<3; i++) {
            MainActivity.editor.putString(DEFAULT[i], Graph.getJson(defaults.get(i)));
            MainActivity.editor.apply();
        }
    }

    private void loadDefaults(){
        defaults = new ArrayList<>();

        for (int i=0; i<3; i++) {
            String json = MainActivity.preferences.getString(DEFAULT[i], "");
            if (!json.isEmpty()) // temporary, REMOVE WHEN REALIZE SAVING NORMALLY
            defaults.add(new Graph(json));
        }
    }
}
