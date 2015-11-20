package com.ivanzhur.shakeunlock;

import android.util.Log;
import android.widget.Toast;

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

    public Graph(){
        points = new ArrayList<>();
        peaks = new ArrayList<>();
        numPoints = 0;
        numPeaks = 0;
    }

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
    

    // Check if two graphs similar or not
    public static boolean compareGraphs(Graph graph1, Graph graph2){
        int numPeaks1 = graph1.numPeaks;
        int numPeaks2 = graph2.numPeaks;
        // If size of graphs differs too much return false
        if ((double)Math.abs(numPeaks1-numPeaks2)/Math.min(numPeaks1, numPeaks2) > MAX_NUM_PEAKS_DIFF_RATIO) return false;

        int numPeaksCompared = 0, numPeaksOk = 0;
        String[] c1 = new String[numPeaks1]; // For logs
        String[] c2 = new String[numPeaks2]; // For logs

        // Create iterators to watch peaks of both graphs
        int i1 = 0;
        int i2 = 0;

        // Watch though peaks until the end of at least one graph (it's peaks) is not reached
        while (i1 < numPeaks1 && i2 < numPeaks2){
            // Compare next points
            GraphCompareResult result = compareNextPoints(graph1, graph2, i1, i2);
            numPeaksCompared++;
            if (result.equal) numPeaksOk++;

            // Move iterator to new next points
            i1 += result.skipFirst + 1;
            i2 += result.skipSecond + 1;

            // For logs
            if (result.equal){
                c1[i1-1] = "+";
                c2[i2-1] = "+";
            }
        }

        // Logs
        String message = "Comparing two graphs:\nnumPeaks1: " + numPeaks1 + "\nnumPeaks2: " + numPeaks2 + "\n";
        for (int i=0; i<numPeaks1; i++) message += (c1[i] == null ? "" : "+") + graph1.points.get(graph1.peaks.get(i)).value + "; ";
        message += "\n";
        for (int i=0; i<numPeaks2; i++) message += (c2[i] == null ? "" : "+") + graph2.points.get(graph2.peaks.get(i)).value + "; ";
        message += "\nnumPeaksCompared: " + numPeaksCompared + "\nnumPeaksOk: " + numPeaksOk;
        message += "\nGraphs similar: " + graphsSimilar(Math.min(numPeaks1, numPeaks2), numPeaksCompared, numPeaksOk);
        message += "\n-----------------------------";
        Log.i("GRAPH", message);

        return graphsSimilar(Math.min(numPeaks1, numPeaks2), numPeaksCompared, numPeaksOk);
    }

    // Check if points (peaks) of two graphs 'equal', it is possible to skip some points due to noise in graphs
    public static GraphCompareResult compareNextPoints(Graph graph1, Graph graph2, int startPoint1, int startPoint2){
        int numPeaks1 = graph1.numPeaks;
        int numPeaks2 = graph2.numPeaks;
        // If size of graphs differs too much return false
        if ((double)Math.abs(numPeaks1-numPeaks2)/Math.min(numPeaks1, numPeaks2) > MAX_NUM_PEAKS_DIFF_RATIO)
            return new GraphCompareResult(false, 0, 0);

        // Number of peaks to skip in both graphs
        int skip1 = 0;
        int skip2 = 0;

        // Skip peaks in graph1 until there is no peak equal to current in graph2, there are peaks to skip and skipped no more than MAX
        while (startPoint1 + skip1 < graph1.numPeaks
                && !GraphPoint.peaksEqual(graph1, graph2, startPoint1 + skip1, startPoint2)
                && skip1 <= MAX_PEAKS_SKIP_NUM) skip1++;

        // If skipped more than MAX peaks in graph1 OR the end of graph1 peaks reached
        if (skip1 > MAX_PEAKS_SKIP_NUM || startPoint1 + skip1 >= graph1.numPeaks){
            skip1 = 0;

            // Skip peaks in graph2 by the same rule as in graph1
            while (startPoint2 + skip2 < graph2.numPeaks
                    && !GraphPoint.peaksEqual(graph1, graph2, startPoint1, startPoint2 + skip2)
                    && skip2 <= MAX_PEAKS_SKIP_NUM) skip2++;

            // If skipped more than MAX in both graph1 and graph2 OR the end of graph2 peaks reached
            // skip1=skip2=0
            if (skip2 > MAX_PEAKS_SKIP_NUM || startPoint2 + skip2 >= graph2.numPeaks)
                skip2 = 0;
        }

        // If peaks equal after skipping return true and number of points to skip, if else return false
        if (GraphPoint.peaksEqual(graph1, graph2, startPoint1 + skip1, startPoint2 + skip2))
            return new GraphCompareResult(true, skip1, skip2);
        else
            return new GraphCompareResult(false, 0, 0);
    }

    // Check if graphs similar by relations between number of peaks, compared peaks and peaks considered similar
    public static boolean graphsSimilar(int numPeaks, int numPeaksCompared, int numPeaksOk){
        return ((double)numPeaksCompared/numPeaks >= MIN_PEAKS_COMPARED_RATIO
                && (double)numPeaksOk/numPeaksCompared >= MIN_PEAKS_OK_RATIO);
    }
}
