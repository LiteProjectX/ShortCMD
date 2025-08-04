# ShortCmd - Plugin for executing commands from URLs | Плагин для выполнения команд из URL

## English Version

### Description

ShortCmd is a Minecraft server plugin that allows executing commands from external sources (like Pastebin) or saving them for later use. The plugin provides a convenient interface for managing and executing commands.

### Key Features

- **Execute commands from URLs/Pastebin**
- **Save commands and links for reuse**
- **Manage saved commands (run/delete)**
- **Flexible security settings**
- **Multi-language support (English/Russian)**
- **Different execution modes (as console or player)**

### Installation

1. Download the plugin file (ShortCmd.jar)
2. Place it in your server's `plugins` folder
3. Restart the server

### Commands

Main command: `/shortcmd` or `/scmd` (alias)

Available subcommands:

| Command       | Description                                      | Permission          |
|---------------|-----------------------------------------------|---------------------|
| help          | Show help menu                              | shortcmd.help       |
| run <url>     | Execute commands from URL/Pastebin            | shortcmd.run        |
| save <url> <name> | Save command link               | shortcmd.save       |
| savecmd <url> [name] | Download and save commands from URL      | shortcmd.savecmd    |
| storage <name> <delete/run> | Manage saved commands | shortcmd.storage    |
| lang <ru/en>  | Change language                                 | shortcmd.lang       |
| mode <console/player> | Change execution mode      | shortcmd.mode       |
| reload        | Reload configuration                    | shortcmd.reload     |

### Permissions

All permissions default to server operators (`default: op`):

- `shortcmd.help` - access to help
- `shortcmd.run` - execute commands from URLs
- `shortcmd.save` - save links
- `shortcmd.savecmd` - save commands
- `shortcmd.storage` - manage storage
- `shortcmd.lang` - change language
- `shortcmd.mode` - change execution mode
- `shortcmd.reload` - reload configuration
- `shortcmd.*` - all plugin permissions

### Configuration

Main settings in `config.yml`:

```yaml
# Blocked commands
blocked-commands:
  - op
  - stop
  - reload
  - plugman

language: en # en/ru

# Timeouts in milliseconds
timeouts:
  connect: 10000    # Connection timeout
  read: 10000       # Read timeout
  internet-check: 3000  # Internet check
command-delay: 100  # Delay between commands
```

---

## Русская версия

### Описание

ShortCmd - это плагин для Minecraft серверов, который позволяет выполнять команды из внешних источников (например, Pastebin) или сохранять их для последующего использования. Плагин предоставляет удобный интерфейс для управления командами и их выполнения.

### Основные возможности

- **Выполнение команд из URL/Pastebin**
- **Сохранение команд и ссылок для повторного использования**
- **Управление сохранёнными командами (запуск/удаление)**
- **Гибкие настройки безопасности**
- **Поддержка двух языков (русский/английский)**
- **Различные режимы выполнения команд (от имени консоли или игрока)**

### Установка

1. Скачайте файл плагина (ShortCmd.jar)
2. Поместите его в папку `plugins` вашего сервера
3. Перезапустите сервер

### Команды

Основная команда: `/shortcmd` или `/scmd` (алиас)

Доступные подкоманды:

| Команда       | Описание                                      | Разрешение          |
|---------------|-----------------------------------------------|---------------------|
| help          | Показать справку                              | shortcmd.help       |
| run <url>     | Выполнить команды из URL/Pastebin            | shortcmd.run        |
| save <url> <name> | Сохранить ссылку на команды               | shortcmd.save       |
| savecmd <url> [name] | Скачать и сохранить команды из URL      | shortcmd.savecmd    |
| storage <name> <delete/run> | Управление сохранёнными командами | shortcmd.storage    |
| lang <ru/en>  | Изменить язык                                 | shortcmd.lang       |
| mode <console/player> | Изменить режим выполнения команд      | shortcmd.mode       |
| reload        | Перезагрузить конфигурацию                    | shortcmd.reload     |

### Разрешения

Все разрешения по умолчанию выдаются операторам сервера (`default: op`):

- `shortcmd.help` - доступ к справке
- `shortcmd.run` - выполнение команд из URL
- `shortcmd.save` - сохранение ссылок
- `shortcmd.savecmd` - сохранение команд
- `shortcmd.storage` - управление хранилищем
- `shortcmd.lang` - изменение языка
- `shortcmd.mode` - изменение режима выполнения
- `shortcmd.reload` - перезагрузка конфигурации
- `shortcmd.*` - все разрешения плагина

### Конфигурация

Основные настройки в `config.yml`:

```yaml
# Заблокированные команды
blocked-commands:
  - op
  - stop
  - reload
  - plugman

language: ru # ru/en

# Таймауты в миллисекундах
timeouts:
  connect: 10000    # Таймаут подключения
  read: 10000       # Таймаут чтения
  internet-check: 3000  # Проверка интернета
command-delay: 100  # Задержка между командами
```

### Лицензия ©

Плагин распространяется под лицензией MIT. Подробнее см. в файле LICENSE.
