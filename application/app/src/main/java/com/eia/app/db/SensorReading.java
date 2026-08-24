package com.eia.app.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_readings")
public class SensorReading {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String sensorId;
    private float value;
    private long timestamp;

    public SensorReading(String sensorId, float value, long timestamp) {
        this.sensorId = sensorId;
        this.value = value;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public float getValue() { return value; }
    public void setValue(float value) { this.value = value; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
