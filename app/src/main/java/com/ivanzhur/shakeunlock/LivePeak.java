package com.ivanzhur.shakeunlock;

public class LivePeak {
    public double value;
    public int position;
    public int comparedTo;
    public int skippedAfter;
    public int skippedDefaults;

    public LivePeak(double value, int position, int comparedTo, int skippedAfter){
        this.value = value;
        this.position = position;
        this.comparedTo = comparedTo;
        this.skippedAfter = skippedAfter;
        skippedDefaults = 0;
    }
}
