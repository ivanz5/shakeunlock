package com.ivanzhur.shakeunlock;

public class GraphCompareResult {
    public boolean equal;
    public int skipFirst;
    public int skipSecond;
    public GraphCompareResult(boolean equal, int skipInFirst, int skipInSecond){
        this.equal = equal;
        this.skipFirst = skipInFirst;
        this.skipSecond = skipInSecond;
    }
}
