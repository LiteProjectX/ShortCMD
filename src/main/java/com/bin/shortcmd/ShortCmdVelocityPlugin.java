package com.bin.shortcmd;

import java.io.BufferedWriter;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Plugin(
    id = "shortcmd",
    name = "ShortCmd",
    version = "1.3",
    description = "Execute commands from external sources",
    authors = {"LiteProjectX"}
)
public class ShortCmdVelocityPlugin {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private Map<String, Object> config;
    private Map<String, Object> storage;
    private Map<String, Object> modes;
    private final Map<UUID, Boolean> playerModes = new HashMap<>();
    private final Yaml yaml = new Yaml();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            loadConfigs();
            loadPlayerModes();

            // Register command
            server.getCommandManager().register("shortcmd", new ShortCmdVelocityCommand(), "scmd");

            logger.info("ShortCmd Velocity enabled successfully! Version: 1.3");
        } catch (Exception e) {
            logger.error("Failed to enable plugin", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        saveStorage();
        saveModesConfig();
        logger.info("ShortCmd Velocity disabled");
    }

    private void loadConfigs() throws IOException {
        // Load main config
        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            saveDefaultConfig(configFile);
        }
        
        try (InputStream input = Files.newInputStream(configFile)) {
            config = yaml.load(input);
            if (config == null) config = new HashMap<>();
        }

        // Load storage
        Path storageFile = dataDirectory.resolve("storage.yml");
        if (!Files.exists(storageFile)) {
            Files.createFile(storageFile);
            storage = new HashMap<>();
        } else {
            try (InputStream input = Files.newInputStream(storageFile)) {
                storage = yaml.load(input);
                if (storage == null) storage = new HashMap<>();
            }
        }

        // Load modes
        Path modesFile = dataDirectory.resolve("modes.yml");
        if (!Files.exists(modesFile)) {
            Files.createFile(modesFile);
            modes = new HashMap<>();
        } else {
            try (InputStream input = Files.newInputStream(modesFile)) {
                modes = yaml.load(input);
                if (modes == null) modes = new HashMap<>();
            }
        }
    }

    private void saveDefaultConfig(Path configFile) throws IOException {
        Map<String, Object> defaultConfig = new HashMap<>();
        
        defaultConfig.put("blocked-commands", Arrays.asList("op", "stop", "reload", "plugman"));
        defaultConfig.put("language", "ru");
        
        Map<String, Integer> timeouts = new HashMap<>();
        timeouts.put("connect", 10000);
        timeouts.put("read", 10000);
        timeouts.put("internet-check", 3000);
        defaultConfig.put("timeouts", timeouts);
        defaultConfig.put("command-delay", 100);
        
        // Messages
        Map<String, Object> messages = new HashMap<>();
        Map<String, String> ru = new HashMap<>();
        ru.put("no-args", "§7Используйте §6/shortcmd help");
        ru.put("help", "§6Доступные команды:");
        ru.put("help-line", "§7- §6/shortcmd %cmd% §7- %desc%");
        ru.put("help-desc", "показать это меню");
        ru.put("run-desc", "выполнить команды из URL");
        ru.put("save-desc", "сохранить ссылку");
        ru.put("savecmd-desc", "сохранить команды из URL");
        ru.put("storage-desc", "управление хранилищем");
        ru.put("lang-desc", "сменить язык");
        ru.put("mode-desc", "изменить режим выполнения");
        ru.put("reload-desc", "перезагрузить конфиг");
        ru.put("no-permission", "§cУ вас недостаточно прав!");
        ru.put("run-success", "§aВыполнено §6%count% §aкоманд");
        ru.put("run-error", "§cИспользуйте: §6/shortcmd run <код/url>");
        ru.put("command-error", "§cОшибка выполнения команды");
        ru.put("unknown", "§cНеизвестная команда");
        ru.put("player-only", "§cЭта команда только для игроков");
        ru.put("mode-usage", "§cИспользуйте: §6/shortcmd mode <console|player>");
        ru.put("mode-set", "§aРежим выполнения изменён на §6%mode%");
        ru.put("mode-invalid", "§cНедопустимый режим. Используйте console или player");
        ru.put("save-error", "§cИспользуйте: §6/shortcmd save <ссылка> <имя>");
        ru.put("save-success", "§aСсылка сохранена как §6%name%");
        ru.put("savecmd-error", "§cИспользуйте: §6/shortcmd savecmd <код/url> [имя]");
        ru.put("savecmd-success", "§aСохранено §6%count% §aкоманд как §6%name%");
        ru.put("savecmd-empty", "§6Не найдено команд для сохранения");
        ru.put("storage-error", "§cИспользуйте: §6/shortcmd storage <имя> <delete|run>");
        ru.put("storage-not-found", "§cКоманда §6%name% §cне найдена");
        ru.put("storage-delete", "§aУдалена ссылка §6%name%");
        ru.put("storage-run-success", "§aВыполнена команда §6%name%");
        ru.put("storage-action-error", "§cНеизвестное действие");
        ru.put("lang-error", "§cИспользуйте: §6/shortcmd lang <ru|en>");
        ru.put("lang-invalid", "§cНеправильный язык, используйте ru или en");
        ru.put("lang-set", "§aЯзык изменен на §6%lang%");
        ru.put("reload-success", "§aКонфиг перезагружен");
        ru.put("no-commands", "§6Нет команд для выполнения");
        
        Map<String, String> en = new HashMap<>();
        en.put("no-args", "§7Use §6/shortcmd help");
        en.put("help", "§6Available commands:");
        en.put("help-line", "§7- §6/shortcmd %cmd% §7- %desc%");
        en.put("help-desc", "show this menu");
        en.put("run-desc", "execute commands from URL");
        en.put("save-desc", "save link");
        en.put("savecmd-desc", "save commands from URL");
        en.put("storage-desc", "manage storage");
        en.put("lang-desc", "change language");
        en.put("mode-desc", "change execution mode");
        en.put("reload-desc", "reload config");
        en.put("no-permission", "§cYou don't have permission!");
        en.put("run-success", "§aExecuted §6%count% §acommands");
        en.put("run-error", "§cUsage: §6/shortcmd run <code/url>");
        en.put("command-error", "§cCommand execution error");
        en.put("unknown", "§cUnknown command");
        en.put("player-only", "§cThis command is for players only");
        en.put("mode-usage", "§cUsage: §6/shortcmd mode <console|player>");
        en.put("mode-set", "§aExecution mode set to §6%mode%");
        en.put("mode-invalid", "§cInvalid mode. Use console or player");
        en.put("save-error", "§cUsage: §6/shortcmd save <link> <name>");
        en.put("save-success", "§aLink saved as §6%name%");
        en.put("savecmd-error", "§cUsage: §6/shortcmd savecmd <code/url> [name]");
        en.put("savecmd-success", "§aSaved §6%count% §acommands as §6%name%");
        en.put("savecmd-empty", "§6No commands found to save");
        en.put("storage-error", "§cUsage: §6/shortcmd storage <name> <delete|run>");
        en.put("storage-not-found", "§cCommand §6%name% §cnot found");
        en.put("storage-delete", "§aLink deleted §6%name%");
        en.put("storage-run-success", "§aCommand executed §6%name%");
        en.put("storage-action-error", "§cUnknown action");
        en.put("lang-error", "§cUsage: §6/shortcmd lang <ru|en>");
        en.put("lang-invalid", "§cInvalid language, use ru or en");
        en.put("lang-set", "§aLanguage set to §6%lang%");
        en.put("reload-success", "§aConfig reloaded");
        en.put("no-commands", "§6No commands to execute");
        
        messages.put("ru", ru);
        messages.put("en", en);
        defaultConfig.put("messages", messages);
        
        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            yaml.dump(defaultConfig, writer);
        }
        
        config = defaultConfig;
    }

    @SuppressWarnings("unchecked")
    private void loadPlayerModes() {
        Map<String, Object> modesSection = (Map<String, Object>) modes.get("modes");
        if (modesSection != null) {
            for (Map.Entry<String, Object> entry : modesSection.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Boolean mode = (Boolean) entry.getValue();
                    playerModes.put(uuid, mode != null ? mode : true);
                } catch (IllegalArgumentException e) {
                    // Invalid UUID, skip
                }
            }
        }
    }

    public void savePlayerMode(UUID uuid, boolean mode) {
        playerModes.put(uuid, mode);
        Map<String, Object> modesSection = (Map<String, Object>) modes.computeIfAbsent("modes", k -> new HashMap<>());
        modesSection.put(uuid.toString(), mode);
        saveModesConfig();
    }

    private void saveStorage() {
        try (BufferedWriter writer = Files.newBufferedWriter(dataDirectory.resolve("storage.yml"))) {
            yaml.dump(storage, writer);
        } catch (IOException e) {
            logger.error("Could not save storage.yml", e);
        }
    }

    private void saveModesConfig() {
        try (BufferedWriter writer = Files.newBufferedWriter(dataDirectory.resolve("modes.yml"))) {
            yaml.dump(modes, writer);
        } catch (IOException e) {
            logger.error("Could not save modes.yml", e);
        }
    }

    public void reloadConfigs() {
        try {
            loadConfigs();
            loadPlayerModes();
        } catch (IOException e) {
            logger.error("Failed to reload configs", e);
        }
    }

    private class ShortCmdVelocityCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            CompletableFuture.runAsync(() -> {
                handleCommand(source, args);
            });
        }

        private void handleCommand(CommandSource source, String[] args) {
            try {
                if (args == null || args.length == 0) {
                    sendMessage(source, "no-args");
                    return;
                }

                String subCommand = args[0].toLowerCase();
                switch (subCommand) {
                    case "help":
                        handleHelp(source);
                        break;
                    case "run":
                        handleRun(source, args);
                        break;
                    case "save":
                        handleSave(source, args);
                        break;
                    case "savecmd":
                        handleSaveCmd(source, args);
                        break;
                    case "storage":
                        handleStorage(source, args);
                        break;
                    case "lang":
                        handleLang(source, args);
                        break;
                    case "mode":
                        handleMode(source, args);
                        break;
                    case "reload":
                        handleReload(source);
                        break;
                    default:
                        sendMessage(source, "unknown");
                        break;
                }
            } catch (Exception e) {
                logger.error("Command error", e);
                sendMessage(source, "command-error");
            }
        }

        private void handleHelp(CommandSource source) {
            if (!checkPermission(source, "shortcmd.help")) return;
            
            sendMessage(source, "help");
            sendFormattedHelp(source, "help", "help-desc");
            sendFormattedHelp(source, "run", "run-desc");
            sendFormattedHelp(source, "save", "save-desc");
            sendFormattedHelp(source, "savecmd", "savecmd-desc");
            sendFormattedHelp(source, "storage", "storage-desc");
            sendFormattedHelp(source, "lang", "lang-desc");
            sendFormattedHelp(source, "mode", "mode-desc");
            if (source.hasPermission("shortcmd.reload")) {
                sendFormattedHelp(source, "reload", "reload-desc");
            }
        }

        private void sendFormattedHelp(CommandSource source, String cmd, String descKey) {
            String lang = (String) config.getOrDefault("language", "ru");
            Map<String, Object> messages = (Map<String, Object>) config.get("messages");
            if (messages == null) return;
            
            Map<String, String> langMessages = (Map<String, String>) messages.get(lang);
            if (langMessages == null) langMessages = (Map<String, String>) messages.get("en");
            if (langMessages == null) return;
            
            String line = langMessages.get("help-line");
            String desc = langMessages.get(descKey);
            
            if (line != null && desc != null) {
                line = line.replace("%cmd%", cmd).replace("%desc%", desc);
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
        }

        @SuppressWarnings("unchecked")
        private void handleRun(CommandSource source, String[] args) {
            if (!checkPermission(source, "shortcmd.run")) return;
            
            if (args.length < 2) {
                sendMessage(source, "run-error");
                return;
            }

            String link = args[1];
            if (!link.startsWith("http")) {
                link = "https://pastebin.com/raw/" + link;
            }

            try {
                List<String> commands = downloadCommands(link);
                if (commands.isEmpty()) {
                    sendMessage(source, "no-commands");
                    return;
                }

                List<String> blockedCommands = (List<String>) config.get("blocked-commands");
                int executed = 0;
                int delay = (Integer) config.getOrDefault("command-delay", 100);
                
                for (String cmd : commands) {
                    if (!isBlocked(cmd, blockedCommands)) {
                        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), cmd);
                        executed++;
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    }
                }

                sendMessage(source, "run-success", "%count%", String.valueOf(executed));
            } catch (Exception e) {
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("§cОшибка: " + e.getMessage()));
                logger.error("Run command failed", e);
            }
        }

        @SuppressWarnings("unchecked")
        private void handleSave(CommandSource source, String[] args) {
            if (!checkPermission(source, "shortcmd.save")) return;
            
            if (args.length < 3) {
                sendMessage(source, "save-error");
                return;
            }

            String link = args[1];
            String name = args[2];
            
            Map<String, Object> commands = (Map<String, Object>) storage.computeIfAbsent("commands", k -> new HashMap<>());
            commands.put(name, link);
            saveStorage();
            
            sendMessage(source, "save-success", "%name%", name);
        }

        @SuppressWarnings("unchecked")
        private void handleSaveCmd(CommandSource source, String[] args) {
            if (!checkPermission(source, "shortcmd.savecmd")) return;
            
            if (args.length < 2) {
                sendMessage(source, "savecmd-error");
                return;
            }

            String link = args[1];
            String name = args.length > 2 ? args[2] : "cmd_" + System.currentTimeMillis();
            
            if (!link.startsWith("http")) {
                link = "https://pastebin.com/raw/" + link;
            }

            try {
                List<String> commands = downloadCommands(link);
                if (!commands.isEmpty()) {
                    Map<String, Object> savedCommands = (Map<String, Object>) storage.computeIfAbsent("saved_commands", k -> new HashMap<>());
                    savedCommands.put(name, String.join("\n", commands));
                    saveStorage();
                    sendMessage(source, "savecmd-success", "%count%", String.valueOf(commands.size()), "%name%", name);
                } else {
                    sendMessage(source, "savecmd-empty");
                }
            } catch (Exception e) {
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("§cОшибка: " + e.getMessage()));
            }
        }

        @SuppressWarnings("unchecked")
        private void handleStorage(CommandSource source, String[] args) {
            if (!checkPermission(source, "shortcmd.storage")) return;
            
            if (args.length < 3) {
                sendMessage(source, "storage-error");
                return;
            }

            String name = args[1];
            String action = args[2].toLowerCase();
            
            // Check saved commands first
            Map<String, Object> savedCommands = (Map<String, Object>) storage.get("saved_commands");
            if (savedCommands != null && savedCommands.containsKey(name)) {
                String commands = (String) savedCommands.get(name);
                if ("delete".equals(action)) {
                    savedCommands.remove(name);
                    saveStorage();
                    sendMessage(source, "storage-delete", "%name%", name);
                } else if ("run".equals(action)) {
                    for (String cmd : commands.split("\n")) {
                        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), cmd.trim());
                    }
                    sendMessage(source, "storage-run-success", "%name%", name);
                } else {
                    sendMessage(source, "storage-action-error");
                }
                return;
            }
            
            // Check saved links
            Map<String, Object> commands = (Map<String, Object>) storage.get("commands");
            if (commands == null || !commands.containsKey(name)) {
                sendMessage(source, "storage-not-found", "%name%", name);
                return;
            }

            String link = (String) commands.get(name);
            if ("delete".equals(action)) {
                commands.remove(name);
                saveStorage();
                sendMessage(source, "storage-delete", "%name%", name);
            } else if ("run".equals(action)) {
                try {
                    List<String> cmdList = downloadCommands(link);
                    List<String> blockedCommands = (List<String>) config.get("blocked-commands");
                    
                    for (String cmd : cmdList) {
                        if (!isBlocked(cmd, blockedCommands)) {
                            server.getCommandManager().executeAsync(server.getConsoleCommandSource(), cmd);
                        }
                    }
                    sendMessage(source, "storage-run-success", "%name%", name);
                } catch (Exception e) {
                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("§cОшибка выполнения: " + e.getMessage()));
                }
            } else {
                sendMessage(source, "storage-action-error");
            }
        }

        private void handleLang(CommandSource source, String[] args) {
            if (!checkPermission(source, "shortcmd.lang")) return;
            
            if (args.length < 2) {
                sendMessage(source, "lang-error");
                return;
            }

            String newLang = args[1].toLowerCase();
            if (!Arrays.asList("ru", "en").contains(newLang)) {
                sendMessage(source, "lang-invalid");
                return;
            }

            config.put("language", newLang);
            try (BufferedWriter writer = Files.newBufferedWriter(dataDirectory.resolve("config.yml"))) {
                yaml.dump(config, writer);
                sendMessage(source, "lang-set", "%lang%", newLang);
            } catch (IOException e) {
                sendMessage(source, "command-error");
            }
        }

        private void handleMode(CommandSource source, String[] args) {
            if (!(source instanceof Player)) {
                sendMessage(source, "player-only");
                return;
            }
            
            if (!checkPermission(source, "shortcmd.mode")) return;
            
            if (args.length < 2) {
                sendMessage(source, "mode-usage");
                return;
            }

            Player player = (Player) source;
            String mode = args[1].toLowerCase();
            
            if ("console".equals(mode)) {
                savePlayerMode(player.getUniqueId(), true);
                sendMessage(source, "mode-set", "%mode%", "CONSOLE");
            } else if ("player".equals(mode)) {
                savePlayerMode(player.getUniqueId(), false);
                sendMessage(source, "mode-set", "%mode%", "PLAYER");
            } else {
                sendMessage(source, "mode-invalid");
            }
        }

        private void handleReload(CommandSource source) {
            if (!checkPermission(source, "shortcmd.reload")) return;
            reloadConfigs();
            sendMessage(source, "reload-success");
        }

        private boolean checkPermission(CommandSource source, String permission) {
            if (!source.hasPermission(permission) && !source.hasPermission("shortcmd.*")) {
                sendMessage(source, "no-permission");
                return false;
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private List<String> downloadCommands(String link) throws Exception {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            Map<String, Object> timeouts = (Map<String, Object>) config.get("timeouts");
            if (timeouts != null) {
                conn.setConnectTimeout((Integer) timeouts.getOrDefault("connect", 10000));
                conn.setReadTimeout((Integer) timeouts.getOrDefault("read", 10000));
            }

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

        @SuppressWarnings("unchecked")
        private boolean isBlocked(String command, List<String> blockedCommands) {
            if (blockedCommands == null) return false;
            String baseCommand = command.split("\\s+")[0].toLowerCase();
            return blockedCommands.stream().anyMatch(cmd -> baseCommand.startsWith(cmd.toLowerCase()));
        }

        @SuppressWarnings("unchecked")
        private void sendMessage(CommandSource source, String key, String... placeholders) {
            String lang = (String) config.getOrDefault("language", "ru");
            Map<String, Object> messages = (Map<String, Object>) config.get("messages");
            if (messages == null) return;
            
            Map<String, String> langMessages = (Map<String, String>) messages.get(lang);
            if (langMessages == null) langMessages = (Map<String, String>) messages.get("en");
            if (langMessages == null) return;
            
            String message = langMessages.get(key);
            if (message == null) {
                source.sendMessage(Component.text("§cMessage error: " + key));
                return;
            }
            
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    message = message.replace(placeholders[i], placeholders[i + 1]);
                }
            }
            
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            List<String> completions = new ArrayList<>();
            
            if (args.length <= 1) {
                List<String> commands = Arrays.asList("help", "run", "save", "savecmd", "storage", "lang", "mode", "reload");
                return commands.stream().filter(cmd -> args.length == 0 || cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            return completions;
        }
    }
}