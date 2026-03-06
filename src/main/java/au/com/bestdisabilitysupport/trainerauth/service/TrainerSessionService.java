package au.com.bestdisabilitysupport.trainerauth.service;

import au.com.bestdisabilitysupport.trainerauth.model.ConnectionState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrainerSessionService {
    private final Map<UUID, ConnectionState> sessions = new ConcurrentHashMap<>();

    public void markPending(ServerPlayerEntity player, Vec3d lockPosition, float yaw, float pitch) {
        sessions.put(player.getUuid(), new ConnectionState(false, null, lockPosition, yaw, pitch));
    }

    public void setLoggedIn(ServerPlayerEntity player, String trainerKey) {
        ConnectionState old = sessions.get(player.getUuid());
        Vec3d pos = old != null ? old.lockPosition() : player.getPos();
        float yaw = old != null ? old.yaw() : player.getYaw();
        float pitch = old != null ? old.pitch() : player.getPitch();

        sessions.put(player.getUuid(), new ConnectionState(true, trainerKey, pos, yaw, pitch));
    }

    public Optional<ConnectionState> state(UUID uuid) {
        return Optional.ofNullable(sessions.get(uuid));
    }

    public void clear(UUID uuid) {
        sessions.remove(uuid);
    }

    public void copyConnectionState(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        ConnectionState old = sessions.remove(oldPlayer.getUuid());
        if (old != null) {
            sessions.put(newPlayer.getUuid(), old);
        }
    }
}
