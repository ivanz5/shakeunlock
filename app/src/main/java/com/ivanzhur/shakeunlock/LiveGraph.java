package com.ivanzhur.shakeunlock;

import java.util.ArrayList;
import java.util.List;

public class LiveGraph extends Graph {
    Graph mainGraph;

    public static final int NEED_MORE_POINTS = -1;
    public static final int GRAPHS_NOT_EQUAL = 0;
    public static final int GRAPHS_EQUAL = 1;
    public static final int IN_PROGRESS = 2;

    public LiveGraph(Graph graphToCompareWith){
        super();
        mainGraph = graphToCompareWith;
    }

    public int addPoint(GraphPoint point){
        // Add new point
        points.add(point);
        numPoints++;
        // Return -1 if not enough points to calculate peak
        if (numPoints < 3) return NEED_MORE_POINTS; // Possible to return IN_PROGRESS

        // If previous point is a peak, add it's index to 'peaks'
        if (GraphPoint.isPeak(points.get(numPoints-3).value, points.get(numPoints-2).value, points.get(numPoints-1).value)){
            peaks.add(numPoints-2);
            numPeaks++;
        }

        // Graph can be equal to 'main' if it has not less peaks than 'main'
        // Use 'compareGraphs' to compare them, if equal return success
        double peaksDiffRatio = (double)(numPeaks-mainGraph.numPeaks)/mainGraph.numPeaks;
        if (peaksDiffRatio >= 0 && compareGraphs(this, mainGraph))
            return GRAPHS_EQUAL;

        // If not equal and too much peaks return fail
        if (peaksDiffRatio >= MAX_NUM_PEAKS_DIFF_RATIO)
            return GRAPHS_NOT_EQUAL;

        // Too early to compare OR
        // Not equal at this moment but peaks number is acceptable
        // so can be equal next time
        return IN_PROGRESS;
    }
}
