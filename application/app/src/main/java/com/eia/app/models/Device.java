package com.eia.app.models;

import java.util.ArrayList;
import java.util.List;

public class Device {
    private String id;
    private String name;
    private boolean isOnline;
    private List<Sensor> sensorList;

    public Device(String id, String name, boolean isOnline, List<Sensor> sensorList) {
        this.id = id;
        this.name = name;
        this.isOnline = isOnline;
        this.sensorList = sensorList;
    }
    public Device(String id, String name) {
        this.id = id;
        this.name = name;
        this.isOnline = false;
        this.sensorList = new ArrayList<>();
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

    public Device copy() {
        List<Sensor> sensorsCopy = new ArrayList<>();
        if (this.sensorList != null) {
            for (Sensor s : this.sensorList) {
                sensorsCopy.add(new Sensor(s.getId(), s.getType(), s.getName(), s.getUnit(), s.getValue()));
            }
        }
        return new Device(this.id, this.name, this.isOnline, sensorsCopy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return isOnline == device.isOnline &&
                id.equals(device.id) &&
                name.equals(device.name) &&
                java.util.Objects.equals(sensorList, device.sensorList);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, isOnline, sensorList);
    }
}
