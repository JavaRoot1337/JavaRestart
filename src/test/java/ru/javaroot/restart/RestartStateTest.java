package ru.javaroot.restart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.javaroot.cfg.ScheduleSlot;
import ru.javaroot.vote.VoteSvc;

class RestartStateTest {
    @Test
    void doesNotSkipTargetAtExactSecondWithNanos() {
        RestartState state = new RestartState();
        List<ScheduleSlot> slots = List.of(new ScheduleSlot(DayOfWeek.MONDAY, LocalTime.of(12, 0)));
        LocalDateTime now = LocalDateTime.of(2026, 11, 23, 12, 0, 0, 500_000_000);

        RestartState.Target target = state.findTarget(now, slots);
        assertNotNull(target);
        assertEquals(LocalDateTime.of(2026, 11, 23, 12, 0, 0), target.time());
        assertEquals(0L, state.secondsUntil(now, target.time()));
    }

    @Test
    void roundsSecondsUpForManualTarget() {
        RestartState state = new RestartState();
        LocalDateTime now = LocalDateTime.of(2026, 11, 23, 12, 0, 0, 10_000_000);
        LocalDateTime target = LocalDateTime.of(2026, 11, 23, 12, 1, 0, 0);
        assertEquals(60L, state.secondsUntil(now, target));
    }

    @Test
    void guardsOneShotAnnouncementAndRestartPhase() {
        RestartState state = new RestartState();
        assertTrue(state.markTitle(10));
        assertFalse(state.markTitle(10));
        assertTrue(state.markSaveTitle());
        assertFalse(state.markSaveTitle());
        assertTrue(state.startRestartPhase());
        assertFalse(state.startRestartPhase());
    }

    @Test
    void calculatesVoteThreshold() {
        assertEquals(7, VoteSvc.neededVotes(10, 70));
        assertEquals(1, VoteSvc.neededVotes(1, 50));
    }
}
