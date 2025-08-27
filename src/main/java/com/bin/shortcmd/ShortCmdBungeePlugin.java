package com.bin.shortcmd;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ShortCmdBungeePlugin extends Plugin {
    private Configuration config;
    private File configFile;
    private Configuration storage;
    private File storageFile;
    private Configuration modes;
    private File modesFile;
    private final Map<UUID, Boolean> playerModes = new HashMap<>();

    @Override
    public void onEnable() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                throw new IOException("Failed to create plugin directory");
            }

            loadConfigs();
            loadPlayerModes();
            
            // Register command
            getProxy().getPluginManager().registerCommand(this, new ShortCmdBungeeCommand());
            
            getLogger().info("ShortCmd BungeeCord enabled successfully! Version: 1.3");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin", e);
        }
    }

    private void loadConfigs() throws IOException {
        // Load main config
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

        // Load storage
        storageFile = new File(getDataFolder(), "storage.yml");
        if (!storageFile.exists()) {
            storageFile.createNewFile();
        }
        storage = ConfigurationProvider.getProvider(YamlConfiguration.class).load(storageFile);

        // Load modes
        modesFile = new File(getDataFolder(), "modes.yml");
        if (!modesFile.exists()) {
            modesFile.createNewFile();
        }
        modes = ConfigurationProvider.getProvider(YamlConfiguration.class).load(modesFile);
    }

    private void saveDefaultConfig() throws IOException {
        configFile.createNewFile();
        Configuration defaultConfig = new Configuration();
        
        // Set default values
        defaultConfig.set("blocked-commands", Arrays.asList("op", "stop", "reload", "plugman"));
        defaultConfig.set("language", "ru");
        defaultConfig.set("timeouts.connect", 10000);
        defaultConfig.set("timeouts.read", 10000);
        defaultConfig.set("timeouts.internet-check", 3000);
        defaultConfig.set("command-delay", 100);
        
        // Messages
        Configuration messages = new Configuration();
        Configuration ru = new Configuration();
        
        // Basic messages
        ru.set("no-args", "§7Используйте §6/shortcmd help");
        ru.set("help", "§6Доступные команды:");
        ru.set("help-line", "§7- §6/shortcmd %cmd% §7- %desc%");
        ru.set("help-desc", "показать это меню");
        ru.set("run-desc", "выполнить команды из Pastebin/URL");
        ru.set("save-desc", "сохранить ссылку на команды");
        ru.set("savecmd-desc", "скачать и сохранить команды из Pastebin/URL");
        ru.set("storage-desc", "управление сохранёнными командами (run/delete)");
        ru.set("lang-desc", "сменить язык (ru/en)");
        ru.set("mode-desc", "изменить режим выполнения (console/player)");
        ru.set("reload-desc", "перезагрузить конфигурацию");
        ru.set("no-permission", "§cУ вас недостаточно прав!");
        ru.set("unknown", "§cНеизвестная команда");
        ru.set("command-error", "§cОшибка выполнения");
        
        // mode command
        ru.set("mode-usage", "§cИспользуйте: §6/shortcmd mode <console|player>");
        ru.set("mode-set", "§aРежим выполнения изменён на §6%mode%");
        ru.set("mode-invalid", "§cНедопустимый режим. Используйте console или player");
        ru.set("player-only", "§cЭта команда только для игроков");

        // run command
        ru.set("run-start", "§7Загрузка из: §6%link%");
        ru.set("run-success", "§aВыполнено §6%count% §aкоманд");
        ru.set("run-fail", "§cОшибка: %error%");
        ru.set("run-error", "§cИспользуйте: §6/shortcmd run <код/url>");
        ru.set("run-empty", "§6Нет команд");
        ru.set("no-internet", "§cНет интернета");
        ru.set("blocked", "§cКоманда заблокирована: §6%cmd%");

        // save command
        ru.set("save-error", "§cИспользуйте: §6/shortcmd save <ссылка> <имя>");
        ru.set("save-success", "§aКоманда сохранена как §6%name%");
        
        // savecmd command
        ru.set("savecmd-start", "§7Скачивание команд из: §6%link%");
        ru.set("savecmd-success", "§aСохранено §6%count% §aкоманд как §6%name%");
        ru.set("savecmd-fail", "§cОшибка: %error%");
        ru.set("savecmd-error", "§cИспользуйте: §6/shortcmd savecmd <код/url> [имя]");
        ru.set("savecmd-empty", "§6Не найдено команд для сохранения");

        // storage command
        ru.set("storage-error", "§cИспользуйте: §6/shortcmd storage <имя> <delete|run>");
        ru.set("storage-not-found", "§cКоманда §6%name% §cне найдена");
        ru.set("storage-delete", "§aУдалена команда §6%name%");
        ru.set("storage-run-start", "§7Выполнение команды §6%name%");
        ru.set("storage-run-success", "§aЗавершено выполнение §6%name%");
        ru.set("storage-action-error", "§cНеизвестное действие, используйте delete или run");

        // lang command
        ru.set("lang-error", "§cИспользуйте: §6/shortcmd lang <ru|en>");
        ru.set("lang-invalid", "§cНеправильный язык, используйте ru или en");
        ru.set("lang-set", "§aЯзык изменен на §6%lang%");

        // reload command
        ru.set("reload-success", "§aКонфиг перезагружен");

        Configuration en = new Configuration();
        
        // Basic messages
        en.set("no-args", "§7Use §6/shortcmd help");
        en.set("help", "§6Available commands:");
        en.set("help-line", "§7- §6/shortcmd %cmd% §7- %desc%");
        en.set("help-desc", "show this menu");
        en.set("run-desc", "execute commands from Pastebin/URL");
        en.set("save-desc", "save command link");
        en.set("savecmd-desc", "download and save commands from Pastebin/URL");
        en.set("storage-desc", "manage saved commands (run/delete)");
        en.set("lang-desc", "change language (ru/en)");
        en.set("mode-desc", "change execution mode (console/player)");
        en.set("reload-desc", "reload configuration");
        en.set("no-permission", "§cYou don't have permission!");
        en.set("unknown", "§cUnknown command");
        en.set("command-error", "§cCommand error");
        
        // mode command
        en.set("mode-usage", "§cUsage: §6/shortcmd mode <console|player>");
        en.set("mode-set", "§aExecution mode set to §6%mode%");
        en.set("mode-invalid", "§cInvalid mode. Use console or player");
        en.set("player-only", "§cThis command is for players only");

        // run command
        en.set("run-start", "§7Loading from: §6%link%");
        en.set("run-success", "§aExecuted §6%count% §acommands");
        en.set("run-fail", "§cError: %error%");
        en.set("run-error", "§cUsage: §6/shortcmd run <code/url>");
        en.set("run-empty", "§6No commands");
        en.set("no-internet", "§cNo internet");
        en.set("blocked", "§cCommand blocked: §6%cmd%");

        // save command
        en.set("save-error", "§cUsage: §6/shortcmd save <link> <name>");
        en.set("save-success", "§aCommand saved as §6%name%");
        
        // savecmd command
        en.set("savecmd-start", "§7Downloading commands from: §6%link%");
        en.set("savecmd-success", "§aSaved §6%count% §acommands as §6%name%");
        en.set("savecmd-fail", "§cError: %error%");
        en.set("savecmd-error", "§cUsage: §6/shortcmd savecmd <code/url> [name]");
        en.set("savecmd-empty", "§6No commands found to save");

        // storage command
        en.set("storage-error", "§cUsage: §6/shortcmd storage <name> <delete|run>");
        en.set("storage-not-found", "§cCommand §6%name% §cnot found");
        en.set("storage-delete", "§aDeleted command §6%name%");
        en.set("storage-run-start", "§7Executing command §6%name%");
        en.set("storage-run-success", "§aFinished executing §6%name%");
        en.set("storage-action-error", "§cUnknown action, use delete or run");

        // lang command
        en.set("lang-error", "§cUsage: §6/shortcmd lang <ru|en>");
        en.set("lang-invalid", "§cInvalid language, use ru or en");
        en.set("lang-set", "§aLanguage set to §6%lang%");

        // reload command
        en.set("reload-success", "§aConfig reloaded");
        
        messages.set("ru", ru);
        messages.set("en", en);
        defaultConfig.set("messages", messages);
        
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(defaultConfig, configFile);
        config = defaultConfig;
    }

    private void loadPlayerModes() {
        if (modes.getSection("modes") != null) {
            for (String key : modes.getSection("modes").getKeys()) {
                try {
                    playerModes.put(UUID.fromString(key), modes.getBoolean("modes." + key, true));
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip
                }
            }
        }
    }

    public void savePlayerMode(UUID uuid, boolean mode) {
        playerModes.put(uuid, mode);
        modes.set("modes." + uuid.toString(), mode);
        saveModesConfig();
    }

    public void saveStorage() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(storage, storageFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save storage.yml", e);
        }
    }

    public void saveModesConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(modes, modesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save modes.yml", e);
        }
    }

    public void reloadConfigs() {
        try {
            loadConfigs();
            loadPlayerModes();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to reload configs", e);
        }
    }

    public Configuration getPluginConfig() {
        return config;
    }

    public Configuration getStorageConfig() {
        return storage;
    }

    public Map<UUID, Boolean> getPlayerModes() {
        return playerModes;
    }

    private class ShortCmdBungeeCommand extends Command implements TabExecutor {
        public ShortCmdBungeeCommand() {
            super("shortcmd", "shortcmd.help", "scmd");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            getProxy().getScheduler().runAsync(ShortCmdBungeePlugin.this, () -> {
                handleCommand(sender, args);
            });
        }

        private void handleCommand(CommandSender sender, String[] args) {
            try {
                if (args == null || args.length == 0) {
                    sendMessage(sender, "no-args");
                    return;
                }

                String subCommand = args[0].toLowerCase();
                switch (subCommand) {
                    case "help":
                        handleHelp(sender);
                        break;
                    case "run":
                        handleRun(sender, args);
                        break;
                    case "save":
                        handleSave(sender, args);
                        break;
                    case "savecmd":
                        handleSaveCmd(sender, args);
                        break;
                    case "storage":
                        handleStorage(sender, args);
                        break;
                    case "lang":
                        handleLang(sender, args);
                        break;
                    case "mode":
                        handleMode(sender, args);
                        break;
                    case "reload":
                        handleReload(sender);
                        break;
                    default:
                        sendMessage(sender, "unknown");
                        break;
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Command error", e);
                sendMessage(sender, "command-error");
            }
        }

        private void handleHelp(CommandSender sender) {
            if (!checkPermission(sender, "shortcmd.help")) return;
            
            sendMessage(sender, "help");
            sendHelpLine(sender, "help", "help-desc");
            sendHelpLine(sender, "run", "run-desc");
            sendHelpLine(sender, "save", "save-desc");
            sendHelpLine(sender, "savecmd", "savecmd-desc");
            sendHelpLine(sender, "storage", "storage-desc");
            sendHelpLine(sender, "lang", "lang-desc");
            sendHelpLine(sender, "mode", "mode-desc");
            
            if (sender.hasPermission("shortcmd.reload")) {
                sendHelpLine(sender, "reload", "reload-desc");
            }
        }

        private void sendHelpLine(CommandSender sender, String cmd, String descKey) {
            String message = getMessage("help-line");
            message = message.replace("%cmd%", cmd);
            message = message.replace("%desc%", getMessage(descKey));
            sender.sendMessage(message);
        }

        private void handleRun(CommandSender sender, String[] args) {
            if (!checkPermission(sender, "shortcmd.run")) return;
            
            if (args.length < 2) {
                sendMessage(sender, "run-error");
                return;
            }

            String link = args[1];
            if (!link.startsWith("http")) {
                link = "https://pastebin.com/raw/" + link;
            }

            try {
                sendMessage(sender, "run-start", "%link%", link);
                List<String> commands = downloadCommands(link);
                
                if (commands.isEmpty()) {
                    sendMessage(sender, "run-empty");
                    return;
                }

                List<String> blockedCommands = (List<String>) config.get("blocked-commands");
                int executed = 0;
                int delay = config.getInt("command-delay", 100);
                
                for (String cmd : commands) {
                    if (!isBlocked(cmd, blockedCommands)) {
                        getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), cmd);
                        executed++;
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    } else {
                        sendMessage(sender, "blocked", "%cmd%", cmd);
                    }
                }

                sendMessage(sender, "run-success", "%count%", String.valueOf(executed));
            } catch (Exception e) {
                sendMessage(sender, "run-fail", "%error%", e.getMessage());
                getLogger().log(Level.SEVERE, "Run command failed", e);
            }
        }

        private void handleSave(CommandSender sender, String[] args) {
            if (!checkPermission(sender, "shortcmd.save")) return;
            
            if (args.length < 3) {
                sendMessage(sender, "save-error");
                return;
            }

            String link = args[1];
            String name = args[2];
            
            storage.set("commands." + name, link);
            saveStorage();
            sendMessage(sender, "save-success", "%name%", name);
        }

        private void handleSaveCmd(CommandSender sender, String[] args) {
            if (!checkPermission(sender, "shortcmd.savecmd")) return;
            
            if (args.length < 2) {
                sendMessage(sender, "savecmd-error");
                return;
            }

            String link = args[1];
            String name = args.length > 2 ? args[2] : "cmd_" + System.currentTimeMillis();
            
            if (!link.startsWith("http")) {
                link = "https://pastebin.com/raw/" + link;
            }

            try {
                sendMessage(sender, "savecmd-start", "%link%", link);
                List<String> commands = downloadCommands(link);
                
                if (!commands.isEmpty()) {
                    storage.set("saved_commands." + name, String.join("\n", commands));
                    saveStorage();
                    sendMessage(sender, "savecmd-success", "%count%", String.valueOf(commands.size()), "%name%", name);
                } else {
                    sendMessage(sender, "savecmd-empty");
                }
            } catch (Exception e) {
                sendMessage(sender, "savecmd-fail", "%error%", e.getMessage());
            }
        }

        private void handleStorage(CommandSender sender, String[] args) {
            if (!checkPermission(sender, "shortcmd.storage")) return;
            
            if (args.length < 3) {
                sendMessage(sender, "storage-error");
                return;
            }

            String name = args[1];
            String action = args[2].toLowerCase();
            
            // Check saved commands first
            String savedCommands = storage.getString("saved_commands." + name);
            if (savedCommands != null) {
                if ("delete".equals(action)) {
                    storage.set("saved_commands." + name, null);
                    saveStorage();
                    sendMessage(sender, "storage-delete", "%name%", name);
                } else if ("run".equals(action)) {
                    sendMessage(sender, "storage-run-start", "%name%", name);
                    for (String cmd : savedCommands.split("\n")) {
                        getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), cmd.trim());
                    }
                    sendMessage(sender, "storage-run-success", "%name%", name);
                } else {
                    sendMessage(sender, "storage-action-error");
                }
                return;
            }
            
            // Check saved links
            String link = storage.getString("commands." + name);
            if (link == null) {
                sendMessage(sender, "storage-not-found", "%name%", name);
                return;
            }

            if ("delete".equals(action)) {
                storage.set("commands." + name, null);
                saveStorage();
                sendMessage(sender, "storage-delete", "%name%", name);
            } else if ("run".equals(action)) {
                try {
                    sendMessage(sender, "storage-run-start", "%name%", name);
                    List<String> commands = downloadCommands(link);
                    List<String> blockedCommands = (List<String>) config.get("blocked-commands");
                    
                    for (String cmd : commands) {
                        if (!isBlocked(cmd, blockedCommands)) {
                            getProxy().getPluginManager().dispatchCommand(getProxy().getConsole(), cmd);
                        } else {
                            sendMessage(sender, "blocked", "%cmd%", cmd);
                        }
                    }
                    sendMessage(sender, "storage-run-success", "%name%", name);
                } catch (Exception e) {
                    sendMessage(sender, "run-fail", "%error%", e.getMessage());
                }
            } else {
                sendMessage(sender, "storage-action-error");
            }
        }

        private void handleLang(CommandSender sender, String[] args) {
            if (!checkPermission(sender, "shortcmd.lang")) return;
            
            if (args.length < 2) {
                sendMessage(sender, "lang-error");
                return;
            }

            String newLang = args[1].toLowerCase();
            if (!Arrays.asList("ru", "en").contains(newLang)) {
                sendMessage(sender, "lang-invalid");
                return;
            }

            config.set("language", newLang);
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
                sendMessage(sender, "lang-set", "%lang%", newLang);
            } catch (IOException e) {
                sendMessage(sender, "command-error");
            }
        }

        private void handleMode(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sendMessage(sender, "player-only");
                return;
            }
            
            if (!checkPermission(sender, "shortcmd.mode")) return;
            
            if (args.length < 2) {
                sendMessage(sender, "mode-usage");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;
            String mode = args[1].toLowerCase();
            
            if ("console".equals(mode)) {
                savePlayerMode(player.getUniqueId(), true);
                sendMessage(sender, "mode-set", "%mode%", "CONSOLE");
            } else if ("player".equals(mode)) {
                savePlayerMode(player.getUniqueId(), false);
                sendMessage(sender, "mode-set", "%mode%", "PLAYER");
            } else {
                sendMessage(sender, "mode-invalid");
            }
        }

        private void handleReload(CommandSender sender) {
            if (!checkPermission(sender, "shortcmd.reload")) return;
            reloadConfigs();
            sendMessage(sender, "reload-success");
        }

        private boolean checkPermission(CommandSender sender, String permission) {
            if (!sender.hasPermission(permission) && !sender.hasPermission("shortcmd.*")) {
                sendMessage(sender, "no-permission");
                return false;
            }
            return true;
        }

        private List<String> downloadCommands(String link) throws Exception {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(config.getInt("timeouts.connect", 10000));
            conn.setReadTimeout(config.getInt("timeouts.read", 10000));

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }

            List<String> commands = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        commands.add(line);
                    }
                }
            }
            return commands;
        }

        private boolean isBlocked(String command, List<String> blockedCommands) {
            if (blockedCommands == null) return false;
            String baseCommand = command.split("\\s+")[0].toLowerCase();
            return blockedCommands.stream().anyMatch(cmd -> baseCommand.startsWith(cmd.toLowerCase()));
        }

        private String getMessage(String key) {
            String lang = config.getString("language", "ru");
            String message = config.getString("messages." + lang + "." + key);
            
            if (message == null) {
                message = config.getString("messages.en." + key);
                if (message == null) {
                    return "§cMessage error: " + key;
                }
            }
            
            return message.replace("&", "§");
        }

        private void sendMessage(CommandSender sender, String key, String... placeholders) {
            String message = getMessage(key);
            
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    message = message.replace(placeholders[i], placeholders[i + 1]);
                }
            }
            
            sender.sendMessage(message);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                List<String> commands = Arrays.asList("help", "run", "save", "savecmd", "storage", "lang", "mode", "reload");
                return commands.stream().filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase())).collect(java.util.stream.Collectors.toList());
            }
            
            return completions;
        }
    }
}