package au.com.bestdisabilitysupport.trainerauth.model;

public record TrainerAccount(
        String key,
        String pinHash,
        String createdBy,
        long createdAt,
        boolean enabled
) {
}
