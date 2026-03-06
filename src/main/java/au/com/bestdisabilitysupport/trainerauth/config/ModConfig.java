package au.com.bestdisabilitysupport.trainerauth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record ModConfig(
        int adminBypassPermissionLevel,
        boolean lockMovement,
        boolean lockInteractions,
        boolean lockBlockBreaking,
        boolean lockCombat
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "config.json";

    public static ModConfig defaults() {
        return new ModConfig(
                4,
                true,
                true,
                true,
                true
        );
    }

    public static ModConfig loadOrCreate(MinecraftServer server) {
        Path path = configPath(server);

        try {
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                ModConfig defaults = defaults();
                Files.writeString(path, GSON.toJson(defaults));
                return defaults;
            }

            String json = Files.readString(path);
            ModConfig loaded = GSON.fromJson(json, ModConfig.class);
            return loaded != null ? loaded : defaults();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + path, e);
        }
    }

    private static Path configPath(MinecraftServer server) {
        return server.getRunDirectory()
                .resolve("config")
                .resolve("best-trainer-auth")
                .resolve(FILE_NAME);
    }
}
