package au.com.bestdisabilitysupport.trainerauth.command;

import au.com.bestdisabilitysupport.trainerauth.BestTrainerAuthMod;
import au.com.bestdisabilitysupport.trainerauth.model.TrainerAccount;
import au.com.bestdisabilitysupport.trainerauth.util.PinHasher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;

public final class TrainerCommand {
    private TrainerCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(
                CommandManager.literal("trainer")

                        .then(CommandManager.literal("login")
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("pin", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String pin = StringArgumentType.getString(context, "pin");

                                                    String currentTrainer = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                                            .flatMap(state -> java.util.Optional.ofNullable(state.trainerKey()))
                                                            .orElse(null);

                                                    if (currentTrainer != null) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("You are already logged into trainer: " + currentTrainer + ". Use /trainer logout first.")
                                                        ).create();
                                                    }

                                                    TrainerAccount account = BestTrainerAuthMod.trainerStore().get(key)
                                                            .orElseThrow(() -> new SimpleCommandExceptionType(
                                                                    Text.literal("Trainer key not found.")
                                                            ).create());

                                                    if (!account.enabled()) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Trainer key is disabled.")
                                                        ).create();
                                                    }

                                                    if (!PinHasher.matches(pin, account.pinHash())) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Incorrect PIN.")
                                                        ).create();
                                                    }

                                                    BestTrainerAuthMod.trainerBridge().requestLogin(player.getUuid(), account.key());
                                                    disconnectNextTick(player, "Trainer " + account.key() + " selected. Reconnect now.");
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("logout")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

                                    String trainerKey = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                            .flatMap(state -> java.util.Optional.ofNullable(state.trainerKey()))
                                            .orElse(null);

                                    if (trainerKey == null) {
                                        throw new SimpleCommandExceptionType(
                                                Text.literal("You are not logged into a trainer.")
                                        ).create();
                                    }

                                    boolean stillExists = BestTrainerAuthMod.trainerStore().exists(trainerKey);
                                    BestTrainerAuthMod.trainerBridge().onDisconnect(player, trainerKey, stillExists);
                                    BestTrainerAuthMod.sessionService().clear(player.getUuid());

                                    disconnectNextTick(player, "Trainer session saved. Reconnect to choose another trainer.");
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("whoami")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    String trainerKey = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                            .flatMap(state -> java.util.Optional.ofNullable(state.trainerKey()))
                                            .orElse("none selected");
                                    context.getSource().sendFeedback(() -> Text.literal("Trainer: " + trainerKey), false);
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("create")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("pin", StringArgumentType.word())
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String pin = StringArgumentType.getString(context, "pin");

                                                    if (BestTrainerAuthMod.trainerStore().exists(key)) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Trainer key already exists.")
                                                        ).create();
                                                    }

                                                    TrainerAccount created = BestTrainerAuthMod.trainerStore()
                                                            .create(key, PinHasher.hash(pin), context.getSource().getName());
                                                    context.getSource().sendFeedback(() -> Text.literal("Created trainer: " + created.key()), true);
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("delete")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");

                                            boolean removed = BestTrainerAuthMod.trainerStore().delete(key);
                                            boolean removedData = BestTrainerAuthMod.trainerBridge().deleteTrainerData(key);

                                            if (!removed && !removedData) {
                                                throw new SimpleCommandExceptionType(
                                                        Text.literal("Trainer key not found.")
                                                ).create();
                                            }

                                            context.getSource().sendFeedback(() -> Text.literal("Deleted trainer: " + key), true);
                                            return 1;
                                        }))
                        )

                        .then(CommandManager.literal("setpin")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("pin", StringArgumentType.word())
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String pin = StringArgumentType.getString(context, "pin");
                                                    BestTrainerAuthMod.trainerStore().updatePin(key, PinHasher.hash(pin));
                                                    context.getSource().sendFeedback(() -> Text.literal("Updated PIN for: " + key), true);
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("disable")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            BestTrainerAuthMod.trainerStore().setEnabled(key, false);
                                            context.getSource().sendFeedback(() -> Text.literal("Disabled trainer: " + key), true);
                                            return 1;
                                        })
                                )
                        )

                        .then(CommandManager.literal("enable")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            BestTrainerAuthMod.trainerStore().setEnabled(key, true);
                                            context.getSource().sendFeedback(() -> Text.literal("Enabled trainer: " + key), true);
                                            return 1;
                                        })
                                )
                        )

                        .then(CommandManager.literal("list")
                                .requires(source -> source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel()))
                                .executes(context -> {
                                    String joined = BestTrainerAuthMod.trainerStore().all().stream()
                                            .sorted(Comparator.comparing(TrainerAccount::key))
                                            .map(account -> account.key() + (account.enabled() ? "" : " [disabled]"))
                                            .reduce((left, right) -> left + ", " + right)
                                            .orElse("No trainer keys exist yet.");
                                    context.getSource().sendFeedback(() -> Text.literal(joined), false);
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("help")
                                .executes(context -> {
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("/trainer login <key> <pin>, /trainer logout, /trainer whoami, /trainer create <key> <pin>, /trainer delete <key>"),
                                            false
                                    );
                                    return 1;
                                })
                        )
        );
    }

    private static void disconnectNextTick(ServerPlayerEntity player, String message) {
        player.getServer().execute(() -> player.networkHandler.disconnect(Text.literal(message)));
    }
}