# JavaRestart

Плагин рестартов сервера по расписанию, вручную и через голосование для Paper и Folia `1.21–26.4`.

## Совместимость

| Параметр | Значение |
| --- | --- |
| Ядро | Paper, Folia (`folia-supported: true`) |
| Minecraft | `1.21 – 26.4` |
| Java | `21+` |

## Сборка

```powershell
.\gradlew.bat build
```

Итоговый JAR: `build/libs/JavaRestart-2.0.0.jar`.

## Команды

Основная команда: `/jrestart` (алиас: `/jr`).

| Команда | Действие | Право |
| --- | --- | --- |
| `/jrestart` | Показать справку | `jrestart.use` |
| `/jrestart time` | Остаток времени до рестарта | `jrestart.use` |
| `/jrestart vote` | Проголосовать за рестарт | `jrestart.use` |
| `/jrestart now <technical\|daily\|vote> [секунды]` | Запустить ручной рестарт | `jrestart.admin` |
| `/jrestart cancel` | Отменить ручной рестарт | `jrestart.admin` |
| `/jrestart reload` | Перезагрузить конфиг и сообщения | `jrestart.admin` |

## Права

| Право | Назначение | По умолчанию |
| --- | --- | --- |
| `jrestart.use` | Просмотр таймера и голосование | всем |
| `jrestart.admin` | Управление рестартами и reload | op |

## Конфигурация

`plugins/JavaRestart/config.yml`:

- `RestartCMD` — команды консоли перед рестартом (за `settings.restart_cmd_offset` сек)
- `restarts` — расписание в формате `День;ЧЧ;ММ`
- `TitleAtIntervals` / `soundAtIntervals` — интервалы оповещений в секундах
- `types` — включение типов `technical`, `daily`, `vote`
- `vote` — `min_players`, `percentage`, `restart_time`

[English documentation](README.md)
