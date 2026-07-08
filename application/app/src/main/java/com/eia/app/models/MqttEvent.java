package com.eia.app.models;

public class MqttEvent {
    public enum Type { DATA, STATUS, CONFIG }

    private final String deviceId;
    private final String sensorId;
    private final String payload;
    private final Type type;

    public MqttEvent(String deviceId, String sensorId, String payload, Type type) {
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        this.payload = payload;
        this.type = type;
    }

    public String getDeviceId() { return deviceId; }
    public String getSensorId() { return sensorId; }
    public String getPayload() { return payload; }
    public Type getType() { return type; }
}
