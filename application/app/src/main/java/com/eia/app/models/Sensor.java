package com.eia.app.models;

public class Sensor {
    private String id;
    private String type;
    private String name;
    private String unit;
    private float value;

    public Sensor(String id,String type, String name, String unit, float value) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.unit = unit;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
