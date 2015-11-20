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
    //List<Double> savedVals, previousVals;
    List<GraphPoint> points;
    GraphView graphView;
    LineGraphSeries<DataPoint> series;

    Button button, buttonNew;
    TextView bottomTextView;

    boolean isSaving = false, isStarted = false;
    int savingDefault = -1;

    List<Graph> defaults;
    //List< List<List<LivePeak>> > livePeaks;
    List<List<LivePeak>>[] livePeaks;

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

        livePeaks = new ArrayList[3];
        for (int i=0; i<3; i++) {
            //List< List<LivePeak> > list = new ArrayList<>();
            //livePeaks.add(list);
            livePeaks[i] = new ArrayList<>();
        }

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

            //if (savingDefault < 0) addNewLivePeaks();
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
                //savedVals.clear();
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
                //graphView.addSeries(Graph.getSeries(savedVals, Color.RED, true));
                //graphView.addSeries(Graph.getSeries(previousVals, Color.GREEN, true));
                graphView.addSeries(series);

                //previousVals = savedVals;
                //savedVals = new ArrayList<>();
                buttonNew.setVisibility(View.VISIBLE);
            }
        }
        else if (!isStarted) {
            isStarted = true;
            button.setVisibility(View.GONE);
            buttonNew.setVisibility(View.GONE);
            bottomTextView.setVisibility(View.VISIBLE);
            button.setText("Stop");


            //livePeaks = new ArrayList<>();
            for (int i=0; i<3; i++) {
                livePeaks[i].clear();
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

    private void updateLivePeaks(){

    }
/*
    private void addNewLivePeaks(){
        if (vals.size() < 3) return;
        int pos = vals.size()-2;
        double value = vals.get(pos);
        if (!GraphPoint.isPeak(vals.get(pos-1), value, vals.get(pos+1))) return;
        if (Math.abs(value - Graph.GRAVITY) < Graph.PEAK_THRESHOLD) return;


        for (int k=0; k<3; k++){
            livePeaks.get(k).add(new ArrayList<LivePeak>());
            int numLiveGraphs = livePeaks.get(k).size();

            List<Integer> listsToRemove = new ArrayList<>();

            for (int i=0; i<numLiveGraphs; i++){
                int listSize = livePeaks.get(k).get(i).size();

                int defaultToCompareI = (listSize > 0)
                        ? livePeaks.get(k).get(i).get(listSize-1).comparedTo +
                        livePeaks.get(k).get(i).get(listSize-1).skippedDefaults + 1
                        : 0;

                if (defaultToCompareI >= defaults.get(k).numPeaks){
                    boolean similar = Graph.compareGraphs(livePeaks.get(k).get(i), defaults.get(k));
                    if (similar) {
                        Toast.makeText(this, "Similar", Toast.LENGTH_SHORT).show();
                        if (isSaving) onButtonClick(findViewById(R.id.button1));
                    }
                    listsToRemove.add(i);
                }
                else {
                    int comparable = Graph.peaksComparable(value, defaults.get(k).peaks.get(defaultToCompareI));
                    int position = livePeaks.get(k).get(i).size();
                    switch (comparable){
                        case 0:{
                            livePeaks.get(k).get(i).add(new LivePeak(value, position, defaultToCompareI, 0));
                        }
                            break;
                        case 1:{
                            if (position > 0) {
                                int skipDefaults = 1;
                                while (skipDefaults <= Graph.MAX_PEAKS_SKIP_NUM
                                        && defaultToCompareI + skipDefaults < defaults.get(k).peaks.size()
                                        && Graph.peaksComparable(value, defaults.get(k).peaks.get(defaultToCompareI + skipDefaults)) != 0)
                                    skipDefaults++;
                                if (skipDefaults <= Graph.MAX_PEAKS_SKIP_NUM)
                                    livePeaks.get(k).get(i).add(new LivePeak(value, position, defaultToCompareI + skipDefaults, 0));
                                else {
                                    livePeaks.get(k).get(i).get(position-1).skippedDefaults = skipDefaults;
                                    livePeaks.get(k).get(i).add(new LivePeak(value, position, defaults.get(k).numPeaks, -1));
                                }
                            }
                        }
                            break;
                        case -1:{
                            if (position > 0) {
                                livePeaks.get(k).get(i).get(position - 1).skippedAfter++;
                                if (livePeaks.get(k).get(i).get(position - 1).skippedAfter > Graph.MAX_PEAKS_SKIP_NUM)
                                    livePeaks.get(k).get(i).get(position - 1).skippedAfter = -1;
                            }
                        }
                            break;
                    }


                }
            }

            int numListsToRemove = listsToRemove.size();
            for (int i=0; i<numListsToRemove; i++) livePeaks.get(k).remove((int)listsToRemove.get(i)-i);
        }
    }
*/
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
