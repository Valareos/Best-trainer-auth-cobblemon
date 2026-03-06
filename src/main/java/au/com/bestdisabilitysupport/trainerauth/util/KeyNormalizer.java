package au.com.bestdisabilitysupport.trainerauth.util;

import java.util.Locale;

public final class KeyNormalizer {
    private KeyNormalizer() {
    }

    public static String normalize(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Trainer key cannot be null.");
        }

        String normalized = key.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Trainer key cannot be empty.");
        }

        if (!normalized.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Trainer key may only contain letters, numbers, underscore, and dash.");
        }

        return normalized;
    }
}
