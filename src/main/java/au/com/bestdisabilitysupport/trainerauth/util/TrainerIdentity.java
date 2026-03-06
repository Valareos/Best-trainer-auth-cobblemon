package au.com.bestdisabilitysupport.trainerauth.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TrainerIdentity {
    private TrainerIdentity() {
    }

    public static String normalizeKey(String trainerKey) {
        return KeyNormalizer.normalize(trainerKey);
    }

    public static UUID stableUuid(String trainerKey) {
        String normalized = normalizeKey(trainerKey);
        return UUID.nameUUIDFromBytes(("best-trainer:" + normalized).getBytes(StandardCharsets.UTF_8));
    }
}
