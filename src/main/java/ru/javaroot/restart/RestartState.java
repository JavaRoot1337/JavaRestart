package ru.javaroot.restart;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.javaroot.cfg.ScheduleSlot;

public final class RestartState {
    private final Set<Integer> shownTitles = new HashSet<>();
    private final Set<Integer> playedSounds = new HashSet<>();
    private LocalDateTime manualTarget;
    private RestartType manualType;
    private LocalDateTime activeTarget;
    private LocalDateTime commandTarget;
    private boolean saveTitleShown;
    private boolean restartPhaseStarted;

    public synchronized void setManual(LocalDateTime target, RestartType type, int initialSec) {
        manualTarget = target;
        manualType = type;
        activeTarget = target;
        commandTarget = null;
        restartPhaseStarted = false;
        resetAnnouncements();
        shownTitles.add(initialSec);
    }

    public synchronized boolean cancelManual() {
        if (manualTarget == null) {
            return false;
        }
        clearAll();
        return true;
    }

    public synchronized void resetOnReload() {
        activeTarget = null;
        commandTarget = null;
        resetAnnouncements();
    }

    public synchronized void clearAll() {
        manualTarget = null;
        manualType = null;
        activeTarget = null;
        commandTarget = null;
        restartPhaseStarted = false;
        resetAnnouncements();
    }

    public synchronized boolean isManualScheduled() {
        return manualTarget != null;
    }

    public synchronized RestartType manualType() {
        return manualType;
    }

    public synchronized Target findTarget(LocalDateTime now, List<ScheduleSlot> schedules) {
        if (manualTarget != null) {
            return new Target(manualTarget, manualType);
        }
        LocalDateTime nowSec = now.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime nearest = null;
        for (ScheduleSlot slot : schedules) {
            LocalDateTime next = nextOccurrence(nowSec, slot.day(), slot);
            if (nearest == null || next.isBefore(nearest)) {
                nearest = next;
            }
        }
        return nearest == null ? null : new Target(nearest, RestartType.DAILY);
    }

    public synchronized long secondsUntil(LocalDateTime now, LocalDateTime target) {
        long millis = Duration.between(now, target).toMillis();
        if (millis <= 0) {
            return 0;
        }
        return (millis + 999) / 1000;
    }

    public synchronized void activateTarget(LocalDateTime target) {
        if (!target.equals(activeTarget)) {
            activeTarget = target;
            commandTarget = null;
            resetAnnouncements();
        }
    }

    public synchronized boolean markSaveTitle() {
        if (saveTitleShown) {
            return false;
        }
        saveTitleShown = true;
        return true;
    }

    public synchronized boolean markTitle(int interval) {
        return shownTitles.add(interval);
    }

    public synchronized boolean markSound(int interval) {
        return playedSounds.add(interval);
    }

    public synchronized boolean markCommands(LocalDateTime target) {
        if (target.equals(commandTarget)) {
            return false;
        }
        commandTarget = target;
        return true;
    }

    public synchronized boolean startRestartPhase() {
        if (restartPhaseStarted) {
            return false;
        }
        restartPhaseStarted = true;
        return true;
    }

    private void resetAnnouncements() {
        shownTitles.clear();
        playedSounds.clear();
        saveTitleShown = false;
    }

    private static LocalDateTime nextOccurrence(LocalDateTime nowSec, DayOfWeek day, ScheduleSlot slot) {
        LocalDateTime candidate = nowSec.with(TemporalAdjusters.nextOrSame(day))
            .withHour(slot.time().getHour())
            .withMinute(slot.time().getMinute())
            .withSecond(0)
            .withNano(0);
        if (candidate.isBefore(nowSec)) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }

    public record Target(LocalDateTime time, RestartType type) {
    }
}
