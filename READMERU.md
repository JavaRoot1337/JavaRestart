# JavaRestart

> Что это?

Плагин для Paper и Folia (`1.21` - `26.4`) для рестартов сервера по расписанию, вручную и через голосование.

> Что изменилось в 2.0?

- Сборка переведена с Maven на Gradle Kotlin DSL (`Java 21`).
- Все задачи, тайтлы, звуки и кики переведены на Folia-совместимые планировщики (`GlobalRegionScheduler`, `EntityScheduler`, `AsyncScheduler`, `folia-supported: true`).
- Добавлено кросс-версионное определение звуков через Adventure `Sound` и `RegistryAccess` (`1.21` - `26.4`).
- Команда `/jrestart` (алиас `/jr`) переведена на Paper `LifecycleEvents.COMMANDS` (Brigadier).
- Пакеты приведены к структуре `plat`, `cfg`, `restart`, `vote`, `cmd` с потокобезопасным состоянием и immutable-конфигом.
- Добавлена очистка голосов при выходе игрока и модульные тесты.

> Под какую версию он собран?

Paper и Folia `1.21` - `26.4`, `Java 21+`.

> Какие команды есть?

```text
/jrestart
/jrestart reload
/jrestart now <technical|daily|vote> [секунды]
/jrestart cancel
/jrestart time
/jrestart vote
```

> Как собрать?

```text
./gradlew build
```
