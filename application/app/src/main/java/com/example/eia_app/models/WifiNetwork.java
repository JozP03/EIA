package com.example.eia_app.models;

import android.graphics.Color;

public class WifiNetwork {
    private final String ssid;
    private final int rssi;
    private final boolean isProtected;

    public WifiNetwork(String ssid, int rssi, boolean isProtected) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.isProtected = isProtected;
    }

    public String getSsid() { return ssid; }
    public int getRssi() { return rssi; }
    public boolean isProtected() { return isProtected; }

    public String getSignalStatusText() {
        if (rssi >= -60) return "SILNY SYGNAŁ";
        if (rssi >= -75) return "ŚREDNI SYGNAŁ";
        return "SŁABY SYGNAŁ";
    }

    public int getSignalColor() {
        if (rssi >= -60) return Color.parseColor("#10B981"); // Zielony
        if (rssi >= -75) return Color.parseColor("#F59E0B"); // Pomarańczowy
        return Color.parseColor("#EF4444"); // Czerwony
    }

}