package com.ivanzhur.shakeunlock;

public class GraphPoint {
    public double x, y, z, value;

    public GraphPoint(double value, double x, double y, double z){
        this.value = value;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static boolean isPeak(double previousValue, double value, double nextValue){
        return (previousValue < value && value >= nextValue) || (previousValue >= value && value < nextValue);
    }

    public static int peaksComparable(double peak1, double peak2){
        if (peak1 > Graph.GRAVITY && peak2 > Graph.GRAVITY || peak1 < Graph.GRAVITY && peak2 < Graph.GRAVITY)
            return 0; // Returns 0 if peak1 and peak2 are both less or more than GRAVITY
        if (peak1 > Graph.GRAVITY)
            return 1; // Returns 1 if peak1 is ABOVE gravity and peak2 is below
        return -1; // Returns -1 if peak1 is BELOW gravity and peak2 is above
    }

    public static int peaksComparable(Graph graph1, Graph graph2, int i1, int i2){
        if (i1 >= graph1.numPeaks) return 2;
        if (i2 >= graph2.numPeaks) return -2;
        double peak1 = graph1.points.get(graph1.peaks.get(i1)).value;
        double peak2 = graph2.points.get(graph2.peaks.get(i2)).value;
        return peaksComparable(peak1, peak2);
    }

    public static boolean peaksEqual(double value1, double value2){
        return Math.abs(value1-value2) <= Graph.MAX_PEAKS_DIFF;
    }

    public static boolean peaksEqual(Graph graph1, Graph graph2, int i1, int i2){
        double value1 = graph1.points.get(graph1.peaks.get(i1)).value;
        double value2 = graph2.points.get(graph2.peaks.get(i2)).value;
        return Math.abs(value1-value2) <= Graph.MAX_PEAKS_DIFF;
    }
}
