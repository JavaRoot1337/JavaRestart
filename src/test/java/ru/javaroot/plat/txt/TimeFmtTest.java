package ru.javaroot.plat.txt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeFmtTest {
    private static final TimeFmt.Units RU = new TimeFmt.Units(
        "день", "дня", "дней",
        "час", "часа", "часов",
        "минута", "минуты", "минут",
        "секунда", "секунды", "секунд",
        "и "
    );

    @Test
    void formatsRussianPlurals() {
        assertEquals("0 секунд", TimeFmt.format(0, RU));
        assertEquals("1 секунда", TimeFmt.format(1, RU));
        assertEquals("2 секунды", TimeFmt.format(2, RU));
        assertEquals("5 секунд", TimeFmt.format(5, RU));
        assertEquals("21 минута и 1 секунда", TimeFmt.format(21 * 60 + 1, RU));
        assertEquals("1 час, 2 минуты и 4 секунды", TimeFmt.format(3600 + 120 + 4, RU));
    }

    @Test
    void replacesPlaceholders() {
        String out = TimeFmt.replace("%hours%:%minutes%:%seconds% (%time%)", 3665, RU);
        assertEquals("1:1:5 (1 час, 1 минута и 5 секунд)", out);
    }

    @Test
    void fallsBackToPluralWhenFewEmpty() {
        TimeFmt.Units en = new TimeFmt.Units(
            "day", "", "days",
            "hour", "", "hours",
            "minute", "", "minutes",
            "second", "", "seconds",
            "and "
        );
        assertEquals("2 hours and 3 seconds", TimeFmt.format(7203, en));
    }
}
