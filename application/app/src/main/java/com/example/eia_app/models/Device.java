package com.example.eia_app.models;

public class Device {
    private String id;
    private String name;

    private String description;
    private boolean isOnline;

    public Device(String id, String name, boolean isOnline) {
        this.id = id;
        this.name = name;
        this.isOnline = isOnline;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isOnline() { return isOnline; }

    public String getDescription() {
        return description;
    }
}
