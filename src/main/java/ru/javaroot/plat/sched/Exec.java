package ru.javaroot.plat.sched;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Exec {
    private final Plugin plug;

    public Exec(Plugin plug) {
        this.plug = plug;
    }

    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        long delay = Math.max(1L, delayTicks);
        long period = Math.max(1L, periodTicks);
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plug, t -> task.run(), delay, period);
    }

    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        if (delayTicks <= 0L) {
            return Bukkit.getGlobalRegionScheduler().run(plug, t -> task.run());
        }
        return Bukkit.getGlobalRegionScheduler().runDelayed(plug, t -> task.run(), delayTicks);
    }

    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plug, t -> task.run());
    }

    public void runAtPlayer(Player p, Runnable task) {
        p.getScheduler().run(plug, t -> task.run(), null);
    }

    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plug, t -> task.run());
    }

    public void cancelAll() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plug);
        Bukkit.getAsyncScheduler().cancelTasks(plug);
    }
}
