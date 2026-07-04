package com.example.eia_app.models;

import java.util.List;

public class Device {
    private String id;
    private String name;

    private String description;
    private boolean isOnline;

    private List<Sensor> sensorList;
    public Device(String id, String name, String description, boolean isOnline, List<Sensor> sensorList) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isOnline = isOnline;
        this.sensorList = sensorList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public List<Sensor> getSensorList() {
        return sensorList;
    }

    public void setSensorList(List<Sensor> sensorList) {
        this.sensorList = sensorList;
    }
}
