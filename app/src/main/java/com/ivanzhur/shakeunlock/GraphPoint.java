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
}
