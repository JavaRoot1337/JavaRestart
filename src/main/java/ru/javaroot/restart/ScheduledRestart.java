package ru.javaroot.restart;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduledRestart(DayOfWeek day, LocalTime time) {
}
