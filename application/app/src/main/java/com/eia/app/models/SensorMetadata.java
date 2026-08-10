package com.eia.app.models;

import java.util.HashMap;
import java.util.Map;

public class SensorMetadata {
    private static final Map<String, String> UNIT_MAP = new HashMap<>();

    static {
        UNIT_MAP.put("T", "°C");
        UNIT_MAP.put("H", "%");
        UNIT_MAP.put("P", "hPa");
        UNIT_MAP.put("V", "V");
        UNIT_MAP.put("L", "lx");
    }

    public static String getUnitForPrefix(String prefix) {
        return UNIT_MAP.getOrDefault(prefix.toUpperCase(), "");
    }
    
    public static boolean hasPrefix(String prefix) {
        return UNIT_MAP.containsKey(prefix.toUpperCase());
    }
}
