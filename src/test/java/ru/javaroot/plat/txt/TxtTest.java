package ru.javaroot.plat.txt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class TxtTest {
    @Test
    void parsesLegacyAndShortHex() {
        Component comp = Txt.color("&FF8B1DРестарт &7через &#3DFF245");
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        assertEquals("Рестарт через 5", plain);
        TextColor firstColor = comp.children().isEmpty() ? comp.color() : comp.children().getFirst().color();
        assertEquals(TextColor.color(0xFF8B1D), firstColor);
    }

    @Test
    void appliesPlaceholders() {
        assertEquals("Игрок Steve (2/5)", Txt.apply("Игрок %p% (%c%/%n%)", "%p%", "Steve", "%c%", "2", "%n%", "5"));
    }
}
