package com.bin.shortcmd;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ShortCmdPlugin extends JavaPlugin {
    private FileConfiguration config;
    private File configFile;
    private File storageFile;
    private FileConfiguration storage;
    private File modesFile;
    private FileConfiguration modes;
    private boolean placeholderApiEnabled = false;
    private PlatformType platformType;

    public enum PlatformType {
        BUKKIT,
        BUNGEECORD,
        VELOCITY
    }

    @Override
    public void onEnable() {
        try {
            // Detect platform type
            detectPlatform();
            
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IOException("Failed to create plugin directory");
            }

            // Load config with UTF-8
            configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                saveResource("config.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(
                    new FileInputStream(configFile),
                    StandardCharsets.UTF_8
                )
            );

            // Set default values
            config.addDefault("timeouts.connect", 10000);
            config.addDefault("timeouts.read", 10000);
            config.addDefault("timeouts.internet-check", 3000);
            config.addDefault("command-delay", 100);
            config.addDefault("enable-placeholderapi", true);
            config.options().copyDefaults(true);
            saveConfig();

            // Load storage
            storageFile = new File(getDataFolder(), "storage.yml");
            if (!storageFile.exists()) {
                saveResource("storage.yml", false);
            }
            storage = YamlConfiguration.loadConfiguration(storageFile);

            // Load modes
            modesFile = new File(getDataFolder(), "modes.yml");
            if (!modesFile.exists()) {
                saveResource("modes.yml", false);
            }
            modes = YamlConfiguration.loadConfiguration(modesFile);

            // Check for PlaceholderAPI (only on Bukkit platforms)
            if (platformType == PlatformType.BUKKIT && config.getBoolean("enable-placeholderapi", true)) {
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    placeholderApiEnabled = true;
                    getLogger().info("PlaceholderAPI found and enabled!");
                } else {
                    getLogger().info("PlaceholderAPI not found, placeholder support disabled.");
                }
            } else if (platformType != PlatformType.BUKKIT) {
                getLogger().info("PlaceholderAPI is not available on " + platformType + " platform.");
            } else {
                getLogger().info("PlaceholderAPI support disabled in config.");
            }

            // Register command based on platform
            registerCommands();

            getLogger().info("ShortCmd enabled successfully! Version: " + getDescription().getVersion() + " (Platform: " + platformType + ")");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin", e);
            setEnabled(false);
        }
    }

    private void detectPlatform() {
        try {
            Class.forName("org.bukkit.Bukkit");
            platformType = PlatformType.BUKKIT;
            getLogger().info("Detected Bukkit-based platform (Bukkit/Spigot/Paper/Purpur/Folia)");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("net.md_5.bungee.api.ProxyServer");
                platformType = PlatformType.BUNGEECORD;
                getLogger().info("Detected BungeeCord platform");
            } catch (ClassNotFoundException e2) {
                try {
                    Class.forName("com.velocitypowered.api.proxy.ProxyServer");
                    platformType = PlatformType.VELOCITY;
                    getLogger().info("Detected Velocity platform");
                } catch (ClassNotFoundException e3) {
                    platformType = PlatformType.BUKKIT; // Default fallback
                    getLogger().warning("Unable to detect platform, defaulting to Bukkit");
                }
            }
        }
    }

    private void registerCommands() {
        switch (platformType) {
            case BUKKIT:
                ShortCmdCommand cmd = new ShortCmdCommand(this);
                getCommand("shortcmd").setExecutor(cmd);
                getCommand("shortcmd").setTabCompleter(cmd);
                break;
            case BUNGEECORD:
                // BungeeCord command registration will be handled by BungeeCordPlugin
                break;
            case VELOCITY:
                // Velocity command registration will be handled by VelocityPlugin
                break;
        }
    }

    @Override
    public void onDisable() {
        saveStorage();
        saveModesConfig();
        getLogger().info("ShortCmd disabled");
    }

    public void saveStorage() {
        try {
            if (storage != null && storageFile != null) {
                storage.save(storageFile);
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save storage.yml", e);
        }
    }

    public FileConfiguration getStorage() {
        return storage;
    }

    public FileConfiguration getModesConfig() {
        return modes;
    }

    public void saveModesConfig() {
        try {
            if (modes != null && modesFile != null) {
                modes.save(modesFile);
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save modes.yml", e);
        }
    }

    @Override
    public void saveConfig() {
        try {
            super.saveConfig();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }
}