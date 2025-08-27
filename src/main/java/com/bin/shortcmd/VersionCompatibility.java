package com.bin.shortcmd;

import org.bukkit.Bukkit;

public class VersionCompatibility {
    private final String serverVersion;
    private final CompatibilityMode compatibilityMode;
    
    public VersionCompatibility() {
        this.serverVersion = detectServerVersion();
        this.compatibilityMode = determineCompatibilityMode();
    }
    
    private String detectServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private CompatibilityMode determineCompatibilityMode() {
        if (serverVersion.startsWith("v1_16") || serverVersion.startsWith("v1_17") || 
            serverVersion.startsWith("v1_18") || serverVersion.startsWith("v1_19") ||
            serverVersion.startsWith("v1_20") || serverVersion.startsWith("v1_21")) {
            return CompatibilityMode.MODERN;
        } else if (serverVersion.startsWith("v1_12") || serverVersion.startsWith("v1_13") ||
                  serverVersion.startsWith("v1_14") || serverVersion.startsWith("v1_15")) {
            return CompatibilityMode.LEGACY;
        } else {
            return CompatibilityMode.UNSUPPORTED;
        }
    }
    
    public String getServerVersion() {
        return serverVersion;
    }
    
    public CompatibilityMode getCompatibilityMode() {
        return compatibilityMode;
    }
    
    public boolean isModern() {
        return compatibilityMode == CompatibilityMode.MODERN;
    }
    
    public boolean isLegacy() {
        return compatibilityMode == CompatibilityMode.LEGACY;
    }
    
    public boolean isSupported() {
        return compatibilityMode != CompatibilityMode.UNSUPPORTED;
    }
    
    public enum CompatibilityMode {
        MODERN,      // 1.16+
        LEGACY,      // 1.12-1.15
        UNSUPPORTED  // Ниже 1.12
    }
}