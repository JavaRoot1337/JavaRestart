package ru.javaroot.plat.snd;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public final class SoundKey {
    private SoundKey() {
    }

    public static Sound resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        Registry<?> reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);

        NamespacedKey direct = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
        if (direct != null) {
            Keyed found = reg.get(direct);
            if (found != null) {
                return toAdventure(found.key());
            }
        }

        if (!trimmed.contains(".") && !trimmed.contains(":")) {
            String dotted = trimmed.toLowerCase(Locale.ROOT).replace('_', '.');
            Keyed found = reg.get(NamespacedKey.minecraft(dotted));
            if (found != null) {
                return toAdventure(found.key());
            }
            for (Keyed entry : reg) {
                if (entry.key().value().replace('.', '_').equalsIgnoreCase(trimmed)) {
                    return toAdventure(entry.key());
                }
            }
        }
        return null;
    }

    private static Sound toAdventure(Key key) {
        return Sound.sound(key, Sound.Source.MASTER, 1f, 1f);
    }
}
