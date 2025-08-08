package com.bin.shortcmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Level;

public class ShortCmdCommand implements CommandExecutor, TabCompleter {
    private final ShortCmdPlugin plugin;
    private final Map<UUID, Boolean> playerModes = new HashMap<>(); // true = console, false = player

    public ShortCmdCommand(ShortCmdPlugin plugin) {
        this.plugin = plugin;
        loadPlayerModes();
    }

    private void loadPlayerModes() {
        FileConfiguration modesConfig = plugin.getModesConfig();
        for (String key : modesConfig.getKeys(false)) {
            playerModes.put(UUID.fromString(key), modesConfig.getBoolean(key));
        }
    }

    private void savePlayerMode(UUID uuid, boolean mode) {
        playerModes.put(uuid, mode);
        plugin.getModesConfig().set(uuid.toString(), mode);
        plugin.saveModesConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args == null || args.length == 0) {
                sendMessage(sender, "no-args");
                return true;
            }

            String subCommand = args[0].toLowerCase(Locale.ROOT);
            switch (subCommand) {
                case "help":
                    return handleHelp(sender);
                case "run":
                    return handleRun(sender, args);
                case "save":
                    return handleSave(sender, args);
                case "savecmd":
                    return handleSaveCmd(sender, args);
                case "storage":
                    return handleStorage(sender, args);
                case "lang":
                    return handleLang(sender, args);
                case "reload":
                    return handleReload(sender);
                case "mode":
                    return handleMode(sender, args);
                default:
                    sendMessage(sender, "unknown");
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Command error", e);
            sendMessage(sender, "command-error");
            return true;
        }
    }

    private boolean handleHelp(CommandSender sender) {
        if (!checkPermission(sender, "shortcmd.help")) {
            return true;
        }

        sendMessage(sender, "help");
        sendFormatted(sender, "help", "help-desc");
        sendFormatted(sender, "run", "run-desc");
        sendFormatted(sender, "save", "save-desc");
        sendFormatted(sender, "savecmd", "savecmd-desc");
        sendFormatted(sender, "storage", "storage-desc");
        sendFormatted(sender, "lang", "lang-desc");
        sendFormatted(sender, "mode", "mode-desc");
        if (sender.hasPermission("shortcmd.reload") || sender.hasPermission("shortcmd.*")) {
            sendFormatted(sender, "reload", "reload-desc");
        }
        return true;
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission) && !sender.hasPermission("shortcmd.*")) {
            sendMessage(sender, "no-permission");
            return false;
        }
        return true;
    }

    private boolean handleMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "player-only");
            return true;
        }

        if (!checkPermission(sender, "shortcmd.mode")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "mode-usage");
            return true;
        }

        Player player = (Player) sender;
        String mode = args[1].toLowerCase(Locale.ROOT);

        switch (mode) {
            case "console":
                savePlayerMode(player.getUniqueId(), true);
                sendMessage(sender, "mode-set", "%mode%", "CONSOLE");
                break;
            case "player":
                savePlayerMode(player.getUniqueId(), false);
                sendMessage(sender, "mode-set", "%mode%", "PLAYER");
                break;
            default:
                sendMessage(sender, "mode-invalid");
        }
        return true;
    }

    private boolean handleRun(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "shortcmd.run")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "run-error");
            return true;
        }

        if (!checkInternetConnection()) {
            sendMessage(sender, "no-internet");
            return true;
        }

        String link = args[1];
        if (!link.startsWith("http")) {
            link = "https://pastebin.com/raw/" + link;
        }

        sendMessage(sender, "run-start", "%link%", link);

        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(plugin.getConfig().getInt("timeouts.connect", 10000));
            conn.setReadTimeout(plugin.getConfig().getInt("timeouts.read", 10000));

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                sendMessage(sender, "run-fail", "%error%", "HTTP " + responseCode);
                return true;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                List<String> blockedCommands = plugin.getConfig().getStringList("blocked-commands");
                int executedCount = 0;
                String line;
                int delay = plugin.getConfig().getInt("command-delay", 100);

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        executeCommand(sender, line, blockedCommands);
                        executedCount++;
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    }
                }

                if (executedCount > 0) {
                    sendMessage(sender, "run-success", "%count%", String.valueOf(executedCount));
                } else {
                    sendMessage(sender, "run-empty");
                }
            }
        } catch (Exception e) {
            handleRunError(sender, e);
        }
        return true;
    }

    private boolean handleSave(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "shortcmd.save")) {
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "save-error");
            return true;
        }

        String link = args[1];
        String name = args[2];

        plugin.getStorage().set("commands." + name, link);
        plugin.saveStorage();

        sendMessage(sender, "save-success", "%name%", name, "%link%", link);
        return true;
    }

    private boolean handleSaveCmd(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "shortcmd.save")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "savecmd-error");
            return true;
        }

        String link = args[1];
        String name = args.length > 2 ? args[2] : "cmd_" + System.currentTimeMillis();

        if (!link.startsWith("http")) {
            link = "https://pastebin.com/raw/" + link;
        }

        sendMessage(sender, "savecmd-start", "%link%", link);

        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(plugin.getConfig().getInt("timeouts.connect", 10000));
            conn.setReadTimeout(plugin.getConfig().getInt("timeouts.read", 10000));

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                sendMessage(sender, "savecmd-fail", "%error%", "HTTP " + responseCode);
                return true;
            }

            StringBuilder commands = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        commands.append(line).append("\n");
                    }
                }
            }

            if (commands.length() > 0) {
                plugin.getStorage().set("saved_commands." + name, commands.toString().trim());
                plugin.saveStorage();
                sendMessage(sender, "savecmd-success", "%name%", name, "%count%", 
                    String.valueOf(commands.toString().split("\n").length));
            } else {
                sendMessage(sender, "savecmd-empty");
            }
        } catch (Exception e) {
            handleSaveCmdError(sender, e);
        }
        return true;
    }

    private void handleSaveCmdError(CommandSender sender, Exception e) {
        String error = e.getMessage();
        if (error.contains("403")) error = "Pastebin blocked request";
        else if (error.contains("404")) error = "Paste not found";
        else if (error.contains("connect timed out")) error = "Connection timeout";
        
        sendMessage(sender, "savecmd-fail", "%error%", error);
        plugin.getLogger().log(Level.SEVERE, "Command save failed", e);
    }

    private boolean handleStorage(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "shortcmd.storage")) {
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "storage-error");
            return true;
        }

        String name = args[1];
        String action = args[2].toLowerCase(Locale.ROOT);
        
        String savedCommands = plugin.getStorage().getString("saved_commands." + name);
        if (savedCommands != null) {
            switch (action) {
                case "delete":
                    plugin.getStorage().set("saved_commands." + name, null);
                    plugin.saveStorage();
                    sendMessage(sender, "storage-delete", "%name%", name);
                    break;
                case "run":
                    sendMessage(sender, "storage-run-start", "%name%", name);
                    for (String cmd : savedCommands.split("\n")) {
                        executeCommand(sender, cmd.trim(), plugin.getConfig().getStringList("blocked-commands"));
                    }
                    sendMessage(sender, "storage-run-success", "%name%", name);
                    break;
                default:
                    sendMessage(sender, "storage-action-error");
            }
            return true;
        }
        
        String link = plugin.getStorage().getString("commands." + name);
        if (link == null) {
            sendMessage(sender, "storage-not-found", "%name%", name);
            return true;
        }

        switch (action) {
            case "delete":
                plugin.getStorage().set("commands." + name, null);
                plugin.saveStorage();
                sendMessage(sender, "storage-delete", "%name%", name);
                break;
            case "run":
                sendMessage(sender, "storage-run-start", "%name%", name);
                executeStoredCommand(sender, link);
                sendMessage(sender, "storage-run-success", "%name%", name);
                break;
            default:
                sendMessage(sender, "storage-action-error");
        }
        return true;
    }

    private boolean handleLang(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "shortcmd.lang")) {
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "lang-error");
            return true;
        }

        String newLang = args[1].toLowerCase(Locale.ROOT);
        if (!Arrays.asList("ru", "en").contains(newLang)) {
            sendMessage(sender, "lang-invalid");
            return true;
        }

        plugin.getConfig().set("language", newLang);
        plugin.saveConfig();
        sendMessage(sender, "lang-set", "%lang%", newLang);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!checkPermission(sender, "shortcmd.reload")) {
            return true;
        }

        plugin.reloadConfig();
        plugin.saveStorage();
        plugin.saveModesConfig();
        sendMessage(sender, "reload-success");
        return true;
    }

    private boolean checkInternetConnection() {
        try {
            URLConnection connection = new URL("https://google.com").openConnection();
            connection.setConnectTimeout(plugin.getConfig().getInt("timeouts.internet-check", 3000));
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleRunError(CommandSender sender, Exception e) {
        String error = e.getMessage();
        if (error.contains("403")) error = "Pastebin blocked request";
        else if (error.contains("404")) error = "Paste not found";
        else if (error.contains("connect timed out")) error = "Connection timeout";
        
        sendMessage(sender, "run-fail", "%error%", error);
        plugin.getLogger().log(Level.SEVERE, "Command execution failed", e);
    }

    private void executeCommand(CommandSender sender, String command, List<String> blockedCommands) {
        String baseCommand = command.split("\\s+")[0].toLowerCase(Locale.ROOT);
        
        if (blockedCommands.stream().anyMatch(cmd -> baseCommand.startsWith(cmd))) {
            sendMessage(sender, "blocked", "%cmd%", command);
            return;
        }
        
        boolean consoleMode = true;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            consoleMode = playerModes.getOrDefault(player.getUniqueId(), true);
        }

        try {
            CommandSender executor = consoleMode 
                ? plugin.getServer().getConsoleSender() 
                : sender;
                
            plugin.getServer().dispatchCommand(executor, command);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to execute command: " + command, e);
            sendMessage(sender, "command-error");
        }
    }

    private void executeStoredCommand(CommandSender sender, String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(plugin.getConfig().getInt("timeouts.connect", 10000));
            conn.setReadTimeout(plugin.getConfig().getInt("timeouts.read", 10000));
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                List<String> blockedCommands = plugin.getConfig().getStringList("blocked-commands");
                String line;
                int delay = plugin.getConfig().getInt("command-delay", 100);
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        executeCommand(sender, line, blockedCommands);
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to execute stored command", e);
            sendMessage(sender, "command-error");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String lang = plugin.getConfig().getString("language", "en");

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (hasPermission(sender, "help")) commands.add("help");
            if (hasPermission(sender, "run")) commands.add("run");
            if (hasPermission(sender, "save")) {
                commands.add("save");
                commands.add("savecmd");
            }
            if (hasPermission(sender, "storage")) commands.add("storage");
            if (hasPermission(sender, "lang")) commands.add("lang");
            if (hasPermission(sender, "mode")) commands.add("mode");
            if (hasPermission(sender, "reload")) commands.add("reload");
            
            return StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "run":
            case "save":
            case "savecmd":
                if (args.length == 2) {
                    completions.add("url");
                } else if (args.length == 3 && !subCommand.equals("run")) {
                    completions.add("name");
                }
                break;
                
            case "storage":
                if (args.length == 2) {
                    Set<String> keys = new HashSet<>();
                    if (plugin.getStorage().getConfigurationSection("commands") != null) {
                        keys.addAll(plugin.getStorage().getConfigurationSection("commands").getKeys(false));
                    }
                    if (plugin.getStorage().getConfigurationSection("saved_commands") != null) {
                        keys.addAll(plugin.getStorage().getConfigurationSection("saved_commands").getKeys(false));
                    }
                    return StringUtil.copyPartialMatches(args[1], new ArrayList<>(keys), completions);
                } else if (args.length == 3) {
                    return StringUtil.copyPartialMatches(args[2], Arrays.asList("delete", "run"), completions);
                }
                break;
                
            case "lang":
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], Arrays.asList("ru", "en"), completions);
                }
                break;
                
            case "mode":
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], Arrays.asList("console", "player"), completions);
                }
                break;
        }
        
        return completions;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission("shortcmd." + permission) || sender.hasPermission("shortcmd.*");
    }

    private void sendMessage(CommandSender sender, String key, String... placeholders) {
        String lang = plugin.getConfig().getString("language", "ru");
        String message = plugin.getConfig().getString("messages." + lang + "." + key);
        
        if (message == null) {
            message = plugin.getConfig().getString("messages.en." + key);
            if (message == null) {
                plugin.getLogger().warning("Message not found for key: " + key);
                sender.sendMessage("§cMessage error: " + key);
                return;
            }
        }
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i+1]);
            }
        }
        
        sender.sendMessage(message.replace("&", "§"));
    }

    private void sendFormatted(CommandSender sender, String cmd, String descKey) {
        String lang = plugin.getConfig().getString("language", "ru");
        String line = plugin.getConfig().getString("messages." + lang + ".help-line");
        String desc = plugin.getConfig().getString("messages." + lang + "." + descKey);
        
        if (line == null || desc == null) {
            plugin.getLogger().warning("Help message not found for: " + descKey);
            return;
        }
        
        sender.sendMessage(line.replace("%cmd%", cmd)
                         .replace("%desc%", desc)
                         .replace("&", "§"));
    }
}