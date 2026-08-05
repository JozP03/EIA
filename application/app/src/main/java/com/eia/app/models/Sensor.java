package com.eia.app.models;

public class Sensor {
    private String id;
    private String type;
    private String name;
    private String unit;
    private float value;
    private boolean hasError;

    public Sensor(String id, String type, String name, String unit, float value) {
        this(id, type, name, unit, value, false);
    }

    public Sensor(String id, String type, String name, String unit, float value, boolean hasError) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.unit = unit;
        this.value = value;
        this.hasError = hasError;
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

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Float.compare(sensor.value, value) == 0 &&
                hasError == sensor.hasError &&
                id.equals(sensor.id) &&
                type.equals(sensor.type) &&
                name.equals(sensor.name) &&
                unit.equals(sensor.unit);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, type, name, unit, value, hasError);
    }
}
