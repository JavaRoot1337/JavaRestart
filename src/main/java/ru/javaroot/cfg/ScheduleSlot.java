package ru.javaroot.cfg;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleSlot(DayOfWeek day, LocalTime time) {
}
