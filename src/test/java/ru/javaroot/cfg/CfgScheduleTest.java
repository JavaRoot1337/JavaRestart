package ru.javaroot.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CfgScheduleTest {
    private static final Logger LOG = Logger.getLogger("test");

    @Test
    void parsesRussianAndDailySlots() {
        List<ScheduleSlot> slots = Cfg.parseSchedules(
            List.of("Понедельник;00;00", "daily;12;30", "bad-row", "Воскресенье;25;00"),
            true,
            LOG
        );
        assertEquals(8, slots.size());
        assertEquals(new ScheduleSlot(DayOfWeek.MONDAY, LocalTime.of(0, 0)), slots.getFirst());
    }

    @Test
    void skipsWhenDailyDisabled() {
        List<ScheduleSlot> slots = Cfg.parseSchedules(List.of("Понедельник;00;00"), false, LOG);
        assertTrue(slots.isEmpty());
    }
}
