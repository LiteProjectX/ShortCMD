package com.bin.shortcmd;

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

    @Override
    public void onEnable() {
        try {
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

            // Register command
            ShortCmdCommand cmd = new ShortCmdCommand(this);
            getCommand("shortcmd").setExecutor(cmd);
            getCommand("shortcmd").setTabCompleter(cmd);

            getLogger().info("ShortCmd enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin", e);
            setEnabled(false);
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
}