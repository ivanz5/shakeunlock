package com.ivanzhur.shakeunlock;

import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    public int numPoints, numPeaks; // check if public needed
    //public List<Double> values, peaks;
    public List<GraphPoint> points;
    public List<Integer> peaks;

    static final double GRAVITY = 9.8;
    static final double PEAK_THRESHOLD = 2.0;
    static final double START_THRESHOLD = 5.0;
    static final double MAX_NUM_PEAKS_DIFF_RATIO = 0.55;
    static final int MAX_PEAKS_SKIP_NUM = 3;
    static final double MAX_PEAKS_DIFF = 3.0;
    static final double MIN_PEAKS_COMPARED_RATIO = 0.65;
    static final double MIN_PEAKS_OK_RATIO = 0.65;

    public Graph(List<GraphPoint> points){
        this.points = new ArrayList<>(points);
        peaks = getPeaks(points);
        numPoints = points.size();
        numPeaks = peaks.size();
    }

    public Graph(String json){
        points = new ArrayList<>();
        peaks = new ArrayList<>();

        try{
            JSONObject root = new JSONObject(json);
            JSONArray jsonPoints = root.getJSONArray("points");
            JSONArray jsonPointsX = root.getJSONArray("pointsX");
            JSONArray jsonPointsY = root.getJSONArray("pointsY");
            JSONArray jsonPointsZ = root.getJSONArray("pointsZ");
            JSONArray jsonPeaks = root.getJSONArray("peaks");

            numPoints = root.getInt("numPoints");
            numPeaks = root.getInt("numPeaks");
            for (int i=0; i<numPoints; i++){
                points.add(new GraphPoint(
                        jsonPoints.getDouble(i),
                        jsonPointsX.getDouble(i),
                        jsonPointsY.getDouble(i),
                        jsonPointsZ.getDouble(i)
                ));
            }

            for (int i=0; i<numPeaks; i++) peaks.add(jsonPeaks.getInt(i));
        }
        catch (JSONException ex){
            //return null;
        }
    }

    public static String getJson(Graph graph){
        try{
            JSONObject root = new JSONObject();
            JSONArray jsonPoints = new JSONArray();
            JSONArray jsonPointsX = new JSONArray();
            JSONArray jsonPointsY = new JSONArray();
            JSONArray jsonPointsZ = new JSONArray();
            JSONArray jsonPeaks = new JSONArray();
            for (GraphPoint p : graph.points){
                jsonPoints.put(p.value);
                jsonPointsX.put(p.x);
                jsonPointsY.put(p.y);
                jsonPointsZ.put(p.z);
            }
            for (Integer i : graph.peaks) jsonPeaks.put(i);

            root.put("numPoints", graph.numPoints);
            root.put("numPeaks", graph.numPeaks);
            root.put("points", jsonPoints);
            root.put("pointsX", jsonPointsX);
            root.put("pointsY", jsonPointsY);
            root.put("pointsZ", jsonPointsZ);
            root.put("peaks",jsonPeaks);
            return root.toString();
        }
        catch (JSONException ex){
            return null;
        }
    }

    public static List<Integer> getPeaks(List<GraphPoint> points){
        int size = points.size();
        List<Integer> peaks = new ArrayList<>();

        for (int i=1; i<size-1; i++){
            if (Math.abs(points.get(i).value - GRAVITY) > PEAK_THRESHOLD)
                if ( GraphPoint.isPeak(points.get(i-1).value, points.get(i).value, points.get(i+1).value) )
                    peaks.add(i);
        }

        return peaks;
    }

    public static DataPoint[] generateDataForSeries(List<GraphPoint> values){
        if (values == null) return new DataPoint[0];

        int count = Math.min(100, values.size());
        DataPoint[] points = new DataPoint[count];
        for (int i=0; i<count; i++){
            points[i] = new DataPoint(i, values.get(i).value);
        }

        return points;
    }


    public static LineGraphSeries<DataPoint> getSeries(List<GraphPoint> values, int color, boolean drawPoints){
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(generateDataForSeries(values));
        if (color != -1) series.setColor(color);
        series.setDrawDataPoints(drawPoints);
        return series;
    }

    public static boolean compareGraphs(List<LivePeak> livePeaks, Graph defaultGraph){
        int numPeaksCompared = livePeaks.size();
        //int numPeaksOverall = numPeaksCompared;
        int numPeaksOk = 0;

        String message = "Comparing livePeaks with default:\nlivePeaks: ";
        for (int i=0; i<numPeaksCompared; i++) message += livePeaks.get(i).value + "; ";
        message += "\ndefault: ";
        for (int i=0; i<defaultGraph.numPeaks; i++) message += defaultGraph.peaks.get(i) + "; ";
        Log.i("GRAPH", message);

        if (livePeaks.get(numPeaksCompared-1).skippedAfter == -1 || livePeaks.get(numPeaksCompared-1).skippedDefaults == -1)
            return false;

        Log.i("GRAPH", "---skips ok---");

        for (int i=0; i<numPeaksCompared; i++){
            if (livePeaks.get(i).comparedTo >= defaultGraph.numPeaks) continue; // костыль, убрать
            if ( Math.abs(livePeaks.get(i).value - defaultGraph.peaks.get(livePeaks.get(i).comparedTo)) <= MAX_PEAKS_DIFF )
                numPeaksOk++;
            //numPeaksOverall += livePeaks.get(i).skippedAfter;
            //numPeaksOverall += livePeaks.get(i).skippedDefaults;
        }

        message = "numPeaks: " + defaultGraph.peaks.size() + "\ncompared: " + numPeaksCompared + "\nOK: " + numPeaksOk +
                "\nsimilar: " + graphsSimilar(defaultGraph.peaks.size(), numPeaksCompared, numPeaksOk);
        Log.i("GRAPH", message);
        Log.i("GRAPH", "----------------------------------");

        return graphsSimilar(defaultGraph.peaks.size(), numPeaksCompared, numPeaksOk);
    }

    public static boolean compareGraphs(Graph graph1, Graph graph2){
        int numPeaks1 = graph1.numPeaks;
        int numPeaks2 = graph2.numPeaks;
        if ((double)Math.abs(numPeaks1-numPeaks2)/Math.min(numPeaks1, numPeaks2) > MAX_NUM_PEAKS_DIFF_RATIO) return false;

        List<Integer> peaks1 = new ArrayList<>(graph1.peaks);
        List<Integer> peaks2 = new ArrayList<>(graph2.peaks);

        int numPeaksCompared = 0, numPeaksOk = 0;
        String[] c1 = new String[numPeaks1];
        String[] c2 = new String[numPeaks2];

        int i1 = 0, i2 = 0;
        while (i1 < numPeaks1 && i2 < numPeaks2){
            if (graph1.points.get(peaks1.get(i1)).value > GRAVITY && graph2.points.get(peaks2.get(i2)).value < GRAVITY){
            //if (peaks1.get(i1) > GRAVITY && peaks2.get(i2) < GRAVITY){
                int skip = 1;
                while (i2 + skip < numPeaks2 && graph2.points.get(peaks2.get(i2 + skip)).value < GRAVITY) skip++;
                if (skip > MAX_PEAKS_SKIP_NUM) return false;
                i2 += skip;
            }
            if (graph1.points.get(peaks1.get(i1)).value < GRAVITY && graph2.points.get(peaks2.get(i2)).value > GRAVITY){
                int skip = 1;
                while (i1 + skip < numPeaks1 && graph1.points.get(peaks1.get(i1 + skip)).value < GRAVITY) skip++;
                if (skip > MAX_PEAKS_SKIP_NUM) return false;
                i1 += skip;
            }
            else {
                double peak1 = graph1.points.get(peaks1.get(i1)).value;
                double peak2 = graph2.points.get(peaks2.get(i2)).value;
                if (Math.abs(peak1 - peak2) <= MAX_PEAKS_DIFF) numPeaksOk++;
                numPeaksCompared++;

                c1[i1] = "+";
                c2[i2] = "+";
                i1++;
                i2++;
            }
        }

        String message = "Comparing two graphs:\nnumPeaks1: " + numPeaks1 + "\nnumPeaks2: " + numPeaks2 + "\n";
        for (int i=0; i<numPeaks1; i++) message += (c1[i] == null ? "" : "+") + peaks1.get(i) + "; ";
        message += "\n";
        for (int i=0; i<numPeaks2; i++) message += (c2[i] == null ? "" : "+") + peaks2.get(i) + "; ";
        message += "\nnumPeaksCompared: " + numPeaksCompared + "\nnumPeaksOk: " + numPeaksOk;
        message += "\n-----------------------------";
        Log.i("GRAPH", message);

        return graphsSimilar(Math.min(numPeaks1, numPeaks2), numPeaksCompared, numPeaksOk);
    }

    public static boolean graphsSimilar(int numPeaks, int numPeaksCompared, int numPeaksOk){
        return ((double)numPeaksCompared/numPeaks >= MIN_PEAKS_COMPARED_RATIO
                && (double)numPeaksOk/numPeaksCompared >= MIN_PEAKS_OK_RATIO);
    }



    public static int peaksComparable(double peak1, double peak2){
        if (peak1 > GRAVITY && peak2 > GRAVITY || peak1 < GRAVITY && peak2 < GRAVITY)
            return 0;
        if (peak1 > GRAVITY)
            return 1; // Returns 1 if peak1 is ABOVE gravity and peak2 is below
        return -1; // Returns -1 if peak1 is BELOW gravity and peak2 is above
    }
}
