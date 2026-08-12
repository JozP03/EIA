package com.eia.app.models;

public class Sensor {
    private String id;
    private String name;
    private String unit;
    private float value;
    private boolean hasError;
    private String prefix;
    private boolean isPrimary;

    public Sensor(String id, String name, String unit, float value) {
        this(id, name, unit, value, false, "", false);
    }

    public Sensor(String id, String name, String unit, float value, boolean hasError, String prefix, boolean isPrimary) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.value = value;
        this.hasError = hasError;
        this.prefix = prefix;
        this.isPrimary = isPrimary;
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Float.compare(sensor.value, value) == 0 &&
                hasError == sensor.hasError &&
                isPrimary == sensor.isPrimary &&
                id.equals(sensor.id) &&
                name.equals(sensor.name) &&
                unit.equals(sensor.unit) &&
                java.util.Objects.equals(prefix, sensor.prefix);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, unit, value, hasError, prefix, isPrimary);
    }
}
