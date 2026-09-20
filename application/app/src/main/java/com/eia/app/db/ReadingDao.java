package com.eia.app.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.eia.app.models.SensorStats;

import java.util.List;

@Dao
public interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(SensorReading reading);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<SensorReading> readings);

    @Query("SELECT * FROM sensor_readings WHERE sensorId = :sensorId ORDER BY timestamp ASC")
    LiveData<List<SensorReading>> getReadingsForSensor(String sensorId);

    @Query("SELECT * FROM sensor_readings WHERE sensorId = :sensorId AND timestamp > :since ORDER BY timestamp ASC")
    LiveData<List<SensorReading>> getReadingsSince(String sensorId, long since);

    @Query("SELECT MIN(value) as min, MAX(value) as max, AVG(value) as avg, COUNT(*) as count FROM sensor_readings WHERE sensorId = :sensorId AND timestamp > :since")
    SensorStats getStats(String sensorId, long since);

    @Query("DELETE FROM sensor_readings WHERE timestamp < :threshold")
    void deleteOldReadings(long threshold);

    @Query("DELETE FROM sensor_readings WHERE sensorId = :sensorId")
    void deleteReadingsForSensor(String sensorId);
}
