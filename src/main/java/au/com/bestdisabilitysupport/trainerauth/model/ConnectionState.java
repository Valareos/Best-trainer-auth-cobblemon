package au.com.bestdisabilitysupport.trainerauth.model;

import net.minecraft.util.math.Vec3d;

public record ConnectionState(
        boolean loggedIn,
        String trainerKey,
        Vec3d lockPosition,
        float yaw,
        float pitch
) {
}
