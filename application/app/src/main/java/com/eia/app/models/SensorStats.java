package com.eia.app.models;

public class SensorStats {
    private final float min;
    private final float max;
    private final float avg;
    private final int count;

    public SensorStats(float min, float max, float avg, int count) {
        this.min = min;
        this.max = max;
        this.avg = avg;
        this.count = count;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getAvg() { return avg; }
    public int getCount() { return count; }
}
