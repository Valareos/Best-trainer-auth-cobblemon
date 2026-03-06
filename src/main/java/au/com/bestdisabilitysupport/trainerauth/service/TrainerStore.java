package au.com.bestdisabilitysupport.trainerauth.service;

import au.com.bestdisabilitysupport.trainerauth.model.TrainerAccount;
import au.com.bestdisabilitysupport.trainerauth.util.KeyNormalizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class TrainerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, TrainerAccount>>() {}.getType();

    private final Path path;
    private final Map<String, TrainerAccount> accounts;

    public TrainerStore(MinecraftServer server) {
        this.path = server.getRunDirectory()
                .resolve("config")
                .resolve("best-trainer-auth")
                .resolve("accounts.json");
        this.accounts = load(path);
    }

    public Optional<TrainerAccount> get(String key) {
        return Optional.ofNullable(accounts.get(KeyNormalizer.normalize(key)));
    }

    public boolean exists(String key) {
        return accounts.containsKey(KeyNormalizer.normalize(key));
    }

    public TrainerAccount create(String key, String pinHash, String createdBy) {
        String normalized = KeyNormalizer.normalize(key);
        TrainerAccount account = new TrainerAccount(
                normalized,
                pinHash,
                createdBy,
                System.currentTimeMillis(),
                true
        );
        accounts.put(normalized, account);
        save();
        return account;
    }

    public void updatePin(String key, String pinHash) {
        String normalized = KeyNormalizer.normalize(key);
        TrainerAccount old = require(normalized);
        accounts.put(normalized, new TrainerAccount(
                old.key(),
                pinHash,
                old.createdBy(),
                old.createdAt(),
                old.enabled()
        ));
        save();
    }

    public void setEnabled(String key, boolean enabled) {
        String normalized = KeyNormalizer.normalize(key);
        TrainerAccount old = require(normalized);
        accounts.put(normalized, new TrainerAccount(
                old.key(),
                old.pinHash(),
                old.createdBy(),
                old.createdAt(),
                enabled
        ));
        save();
    }

    public boolean delete(String key) {
        String normalized = KeyNormalizer.normalize(key);
        TrainerAccount removed = accounts.remove(normalized);
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    public Collection<TrainerAccount> all() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    private TrainerAccount require(String normalizedKey) {
        TrainerAccount account = accounts.get(normalizedKey);
        if (account == null) {
            throw new IllegalArgumentException("Trainer key not found: " + normalizedKey);
        }
        return account;
    }

    private static Map<String, TrainerAccount> load(Path path) {
        try {
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                Files.writeString(path, "{}");
                return new HashMap<>();
            }

            String json = Files.readString(path);
            Map<String, TrainerAccount> loaded = GSON.fromJson(json, MAP_TYPE);
            return loaded != null ? new HashMap<>(loaded) : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load trainer accounts: " + path, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(accounts, MAP_TYPE));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save trainer accounts: " + path, e);
        }
    }
}
