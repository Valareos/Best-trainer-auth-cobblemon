package au.com.bestdisabilitysupport.trainerauth.service;

import au.com.bestdisabilitysupport.trainerauth.config.ModConfig;
import au.com.bestdisabilitysupport.trainerauth.model.ConnectionState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class LockService {
    private LockService() {
    }

    public static boolean isLocked(ServerPlayerEntity player, TrainerSessionService sessionService) {
        if (player == null || sessionService == null) {
            return false;
        }

        return sessionService.state(player.getUuid())
                .map(state -> !state.loggedIn())
                .orElse(false);
    }

    public static void tickLockedPlayer(ServerPlayerEntity player,
                                        TrainerSessionService sessionService,
                                        ModConfig config) {
        if (player == null || sessionService == null || config == null) {
            return;
        }

        ConnectionState state = sessionService.state(player.getUuid()).orElse(null);
        if (state == null || state.loggedIn()) {
            return;
        }

        if (config.lockMovement()) {
            keepPlayerStill(player, state);
        }
    }

    private static void keepPlayerStill(ServerPlayerEntity player, ConnectionState state) {
        Vec3d lockPos = state.lockPosition();
        if (lockPos == null) {
            return;
        }

        if (player.squaredDistanceTo(lockPos) > 0.01D) {
            player.teleport(
                    player.getServerWorld(),
                    lockPos.getX(),
                    lockPos.getY(),
                    lockPos.getZ(),
                    java.util.Set.of(),
                    state.yaw(),
                    state.pitch()
            );
        }

        player.setVelocity(0.0, 0.0, 0.0);
        player.velocityModified = true;
    }
}
