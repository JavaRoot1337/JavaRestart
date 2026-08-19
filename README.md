# JavaRestart

> What is it?

A Paper 26.2 plugin for scheduled, manual, and vote-based server restarts.

> Why was it rewritten?

The old code worked, but its package layout and restart state were harder to follow. The plugin was rebuilt from the ground up with a smaller structure and explicit lifecycle ownership.

> What changed?

- Moved the main class to `ru.javaroot.JRestart`.
- Split configuration, commands, restart logic, voting, and time formatting into focused packages.
- Reworked scheduler, reload, bossbar, shutdown, and vote cleanup paths.
- Prevented repeated interval notifications and stale player votes.
- Added validation for invalid schedule entries.
- Removed unnecessary wrappers, comments, and global state access.
- Kept the existing YAML keys and user-facing behavior.

> What does it target?

Paper 26.2 and Java 25.

> Which commands exist?

```text
/jrestart
/jrestart reload
/jrestart now
/jrestart cancel
/jrestart time
/jrestart vote
```

> Where is the configuration?

In `config.yml` and `messages.yml`. Both files remain customizable and were kept unchanged during the rewrite.

> How is it built?

```text
mvn clean package
```
