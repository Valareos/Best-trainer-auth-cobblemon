package au.com.bestdisabilitysupport.trainerauth.service;

import au.com.bestdisabilitysupport.trainerauth.util.KeyNormalizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TrainerSelectionStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final PersistedData data;

    public TrainerSelectionStore(MinecraftServer server) {
        this.path = server.getRunDirectory()
                .resolve("config")
                .resolve("best-trainer-auth")
                .resolve("selections.json");
        this.data = load();
    }

    public synchronized void setPending(UUID liveUuid, String trainerKey) {
        data.pending.put(liveUuid.toString(), KeyNormalizer.normalize(trainerKey));
        save();
    }

    public synchronized Optional<String> consumePending(UUID liveUuid) {
        String removed = data.pending.remove(liveUuid.toString());
        save();
        return Optional.ofNullable(removed);
    }

    public synchronized void setActivated(UUID liveUuid, String trainerKey) {
        data.activated.put(liveUuid.toString(), KeyNormalizer.normalize(trainerKey));
        save();
    }

    public synchronized Optional<String> consumeActivated(UUID liveUuid) {
        String removed = data.activated.remove(liveUuid.toString());
        save();
        return Optional.ofNullable(removed);
    }

    public synchronized void clearAll(UUID liveUuid) {
        data.pending.remove(liveUuid.toString());
        data.activated.remove(liveUuid.toString());
        save();
    }

    private PersistedData load() {
        try {
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                PersistedData empty = new PersistedData();
                Files.writeString(path, GSON.toJson(empty));
                return empty;
            }

            String json = Files.readString(path);
            PersistedData loaded = GSON.fromJson(json, PersistedData.class);
            return loaded != null ? loaded : new PersistedData();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load trainer selections: " + path, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save trainer selections: " + path, e);
        }
    }

    private static final class PersistedData {
        Map<String, String> pending = new HashMap<>();
        Map<String, String> activated = new HashMap<>();
    }
}
