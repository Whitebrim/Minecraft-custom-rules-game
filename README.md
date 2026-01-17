# Death Game - Fabric Mod

Серверный мод для Minecraft 1.21.1 с системой секретных правил.

## Версии

- Minecraft: 1.21.1
- Fabric Loader: 0.16.5+
- Fabric API: 0.103.0+1.21.1
- Gradle: 8.8
- Loom: 1.7

## Команды

### Для игроков

| Команда | Описание |
|---------|----------|
| `/participate` | Присоединиться к игре |

### Для администраторов (OP level 2+)

| Команда | Описание |
|---------|----------|
| `/deathgame start` | Запустить игру |
| `/deathgame stop` | Остановить игру |
| `/deathgame reset` | Полный сброс |
| `/deathgame kick <player>` | Удалить игрока |
| `/deathgame status` | Статус игры |
| `/rule enable <id>` | Включить правило |
| `/rule disable <id>` | Отключить правило |
| `/rule reveal <id>` | Раскрыть правило |
| `/rule list` | Список правил |

## Сборка

1. Скачайте gradle-wrapper.jar:
```
https://github.com/gradle/gradle/raw/v8.8/gradle/wrapper/gradle-wrapper.jar
```

2. Положите в `gradle/wrapper/gradle-wrapper.jar`

3. Соберите:
```cmd
gradlew.bat build
```

JAR: `build/libs/deathgame-1.0.0.jar`
