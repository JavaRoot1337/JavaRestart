# JavaRestart

> What is it?

A Paper & Folia (`1.21` - `26.4`) plugin for scheduled, manual, and vote-based server restarts.

> What changed in 2.0?

- Migrated the build from Maven to Gradle Kotlin DSL (`Java 21`).
- Ported all schedulers, player notifications, sounds, and kicks to Folia-safe APIs (`GlobalRegionScheduler`, `EntityScheduler`, `AsyncScheduler`, `folia-supported: true`).
- Added cross-version Adventure `Sound` resolution (`1.21` - `26.4`) without legacy `org.bukkit.Sound` enum coupling.
- Switched command registration to Paper `LifecycleEvents.COMMANDS` (Brigadier) with `/jrestart` and `/jr` alias.
- Reorganized packages into `plat`, `cfg`, `restart`, `vote`, and `cmd` with immutable config snapshots and thread-safe state.
- Added `PlayerQuitEvent` vote cleanup and unit tests.

> What does it target?

Paper and Folia `1.21` - `26.4` on `Java 21+`.

> Which commands exist?

```text
/jrestart
/jrestart reload
/jrestart now <technical|daily|vote> [seconds]
/jrestart cancel
/jrestart time
/jrestart vote
```

> How is it built?

```text
./gradlew build
```
