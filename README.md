# JavaRestart

Scheduled, manual, and vote-based server restarts for Paper and Folia `1.21–26.4`.

## Compatibility

| Parameter | Value |
| --- | --- |
| Core | Paper, Folia (`folia-supported: true`) |
| Minecraft | `1.21 – 26.4` |
| Java | `21+` |

## Build

```powershell
.\gradlew.bat build
```

Output JAR: `build/libs/JavaRestart-2.0.0.jar`.

## Commands

Main command: `/jrestart` (alias: `/jr`).

| Command | Action | Permission |
| --- | --- | --- |
| `/jrestart` | Show usage | `jrestart.use` |
| `/jrestart time` | Show time until next restart | `jrestart.use` |
| `/jrestart vote` | Vote for server restart | `jrestart.use` |
| `/jrestart now <technical\|daily\|vote> [seconds]` | Start manual restart | `jrestart.admin` |
| `/jrestart cancel` | Cancel active manual restart | `jrestart.admin` |
| `/jrestart reload` | Reload config and messages | `jrestart.admin` |

## Permissions

| Permission | Purpose | Default |
| --- | --- | --- |
| `jrestart.use` | Check timer and vote | all |
| `jrestart.admin` | Schedule, cancel, and reload | op |

## Configuration

`plugins/JavaRestart/config.yml`:

- `RestartCMD` — console commands executed before restart (`settings.restart_cmd_offset` seconds prior)
- `restarts` — weekly schedule entries in `Day;HH;MM` format
- `TitleAtIntervals` / `soundAtIntervals` — countdown intervals in seconds
- `types` — enable/disable `technical`, `daily`, `vote`
- `vote` — `min_players`, `percentage`, `restart_time`

[Русская документация](READMERU.md)
