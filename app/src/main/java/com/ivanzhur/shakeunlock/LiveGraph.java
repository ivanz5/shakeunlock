package com.ivanzhur.shakeunlock;

import java.util.ArrayList;
import java.util.List;

public class LiveGraph extends Graph {
    public int currentPeak, numPeaksCompared, numPeaksOk;
    List<GraphPoint> mainGraph;

    public static final int NEED_MORE_POINTS = -1;
    public static final int GRAPHS_NOT_EQUAL = 0;
    public static final int GRAPHS_EQUAL = 1;

    public LiveGraph(List<GraphPoint> graphToCompareWith){
        super();

        mainGraph = graphToCompareWith;
        currentPeak = 0;
        numPeaksCompared = 0;
        numPeaksOk = 0;
    }

    /*public int compareNext(GraphPoint nextPoint){
        peakPoints.add(nextPoint);
        // If need more point to make comparison
        if (peakPoints.size() < Graph.MAX_PEAKS_SKIP_NUM + 1) return NEED_MORE_POINTS;

        int i1 = 0, i2 = 0;

        // Watch though peaks until the end of at least one graph (it's peaks) is not reached
        while (i1 < numPeaks1 && i2 < numPeaks2){
            // Number of peaks to skip in both graphs
            int skip1 = 0, skip2 = 0;

            // Skip peaks in graph1 until there is no peak equal to current in graph2, there are peaks to skip and skipped no more than MAX
            while (i1 + skip1 < graph1.numPeaks && !GraphPoint.peaksEqual(graph1, graph2, i1 + skip1, i2) && skip1 <= MAX_PEAKS_SKIP_NUM) skip1++;

            // If skipped more than MAX peaks in graph1 OR the end of graph1 peaks reached
            if (skip1 > MAX_PEAKS_SKIP_NUM || i1 + skip1 >= graph1.numPeaks){
                skip1 = 0;

                // Skip peaks in graph2 by the same rule as in graph1
                while (i2 + skip2 < graph2.numPeaks && !GraphPoint.peaksEqual(graph1, graph2, i1, i2 + skip2) && skip2 <= MAX_PEAKS_SKIP_NUM) skip2++;

                // If skipped more than MAX in both graph1 and graph2 OR the end of graph2 peaks reached
                // skip1=skip2=0
                if (skip2 > MAX_PEAKS_SKIP_NUM || i2 + skip2 >= graph2.numPeaks)
                    skip2 = 0;
                else
                    i2 += skip2; // Move iterator of graph2 to match compared peak
            }
            // If skipped no more than MAX peaks in graph1
            else
                i1 += skip1; // Move iterator of graph1 to match compared peak

            // If peaks equal after skipping
            if (GraphPoint.peaksEqual(graph1, graph2, i1, i2)){
                numPeaksOk++; // Increase number of equal peaks by 1
                c1[i1] = "+"; // Just for logs
                c2[i2] = "+"; // Just for logs

            }
            numPeaksCompared++; // Increase number of compared peaks by 1

            // Move iterators of both graphs to next peaks
            i1++;
            i2++;
        }
    }*/
}
