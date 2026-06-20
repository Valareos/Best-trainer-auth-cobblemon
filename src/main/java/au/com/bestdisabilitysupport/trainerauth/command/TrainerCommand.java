package au.com.bestdisabilitysupport.trainerauth.command;

import au.com.bestdisabilitysupport.trainerauth.BestTrainerAuthMod;
import au.com.bestdisabilitysupport.trainerauth.model.TrainerAccount;
import au.com.bestdisabilitysupport.trainerauth.util.KeyNormalizer;
import au.com.bestdisabilitysupport.trainerauth.util.PinHasher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.Optional;

public final class TrainerCommand {
    private TrainerCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        registerRoot(dispatcher, "trainer");
        registerRoot(dispatcher, "profile");
    }

    private static boolean canRunAdmin(ServerCommandSource source) {
        return source.hasPermissionLevel(BestTrainerAuthMod.config().adminBypassPermissionLevel());
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher, String root) {
        var builder = CommandManager.literal(root);
            builder
                        .then(CommandManager.literal("login")
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("password", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String password = StringArgumentType.getString(context, "password");

                                                    String currentProfile = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                                            .flatMap(state -> Optional.ofNullable(state.trainerKey()))
                                                            .orElse(null);

                                                    if (currentProfile != null) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("You are already logged into profile: " + currentProfile + ". Use /" + root + " logout first.")
                                                        ).create();
                                                    }

                                                    TrainerAccount account = BestTrainerAuthMod.trainerStore().get(key)
                                                            .orElseThrow(() -> new SimpleCommandExceptionType(
                                                                    Text.literal("Profile key not found.")
                                                            ).create());

                                                    if (!account.enabled()) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Profile key is disabled.")
                                                        ).create();
                                                    }

                                                    if (!PinHasher.matches(password, account.pinHash())) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Incorrect password.")
                                                        ).create();
                                                    }

                                                    BestTrainerAuthMod.trainerBridge().requestLogin(player.getUuid(), account.key());
                                                    disconnectNextTick(player, "Profile " + account.key() + " selected. Reconnect now.");
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("logout")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

                                    String trainerKey = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                            .flatMap(state -> Optional.ofNullable(state.trainerKey()))
                                            .orElse(null);

                                    if (trainerKey == null) {
                                        throw new SimpleCommandExceptionType(
                                                Text.literal("You are not logged into a profile.")
                                        ).create();
                                    }

                                    boolean stillExists = BestTrainerAuthMod.trainerStore().exists(trainerKey);
                                    BestTrainerAuthMod.trainerBridge().onDisconnect(player, trainerKey, stillExists);
                                    BestTrainerAuthMod.sessionService().clear(player.getUuid());

                                    disconnectNextTick(player, "Profile session saved. Reconnect to choose another profile.");
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("whoami")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    String trainerKey = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                            .flatMap(state -> Optional.ofNullable(state.trainerKey()))
                                            .orElse("none selected");
                                    context.getSource().sendFeedback(() -> Text.literal("Profile: " + trainerKey), false);
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("claimcurrent")
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("password", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String password = StringArgumentType.getString(context, "password");

                                                    String currentProfile = BestTrainerAuthMod.sessionService().state(player.getUuid())
                                                            .flatMap(state -> Optional.ofNullable(state.trainerKey()))
                                                            .orElse(null);

                                                    if (currentProfile != null) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("You are already logged into a profile: " + currentProfile + ". Use /" + root + " logout first.")
                                                        ).create();
                                                    }

                                                    if (BestTrainerAuthMod.migrationStore().hasMigrated(player.getUuid())) {
                                                        String existing = BestTrainerAuthMod.migrationStore()
                                                                .migratedProfile(player.getUuid())
                                                                .orElse("unknown");
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("This Minecraft account has already claimed existing server data into profile: " + existing)
                                                        ).create();
                                                    }

                                                    if (!BestTrainerAuthMod.trainerBridge().hasLiveData(player.getUuid())) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("No existing live server data was found to import.")
                                                        ).create();
                                                    }

                                                    if (BestTrainerAuthMod.trainerStore().exists(key)) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Profile key already exists.")
                                                        ).create();
                                                    }

                                                    TrainerAccount created = BestTrainerAuthMod.trainerStore()
                                                            .create(key, PinHasher.hash(password), context.getSource().getName());

                                                    BestTrainerAuthMod.trainerBridge().importCurrentLiveData(player, created.key());
                                                    BestTrainerAuthMod.migrationStore().markMigrated(player.getUuid(), created.key());
                                                    BestTrainerAuthMod.trainerBridge().requestLogin(player.getUuid(), created.key());

                                                    disconnectNextTick(player, "Current server progress imported into profile " + created.key() + ". Reconnect now.");
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("register")
                                .then(CommandManager.argument("key", StringArgumentType.word())
                                        .then(CommandManager.argument("password", StringArgumentType.word())
                                                .executes(context -> {
                                                    if (!BestTrainerAuthMod.config().allowSelfRegistration()) {
                                                        throw new SimpleCommandExceptionType(
                                                                Text.literal("Profile registration is disabled on this server.")
                                                        ).create();
                                                    }

                                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String password = StringArgumentType.getString(context, "password");

                                                    TrainerAccount created = createProfile(
                                                            key,
                                                            password,
                                                            player.getName().getString()
                                                    );

                                                    player.sendMessage(
                                                            Text.literal("Profile created: " + created.key() + ". Use /" + root + " login " + created.key() + " <password>"),
                                                            false
                                                    );
                                                    return 1;
                                                }))
                                )
                        )

                        .then(CommandManager.literal("staff")
                                .requires(source -> {
                                    if (BestTrainerAuthMod.config() == null) {
                                        return false;
                                    }
                                    String password = BestTrainerAuthMod.config().adminOverridePassword();
                                    return password != null && !password.isBlank();
                                })
                                .then(buildStaffPasswordNode()));

        dispatcher.register(appendAdminCommands(builder, root, true));
    }

    private static void ensureStaffPassword(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String configured = BestTrainerAuthMod.config().adminOverridePassword();
        if (configured == null || configured.isBlank()) {
            throw new SimpleCommandExceptionType(Text.literal("Staff commands are not enabled on this server.")).create();
        }

        String provided = StringArgumentType.getString(context, "staffPassword");
        if (!configured.equals(provided)) {
            throw new SimpleCommandExceptionType(Text.literal("Incorrect staff password.")).create();
        }
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<ServerCommandSource, String> buildStaffPasswordNode() {
        return CommandManager.argument("staffPassword", StringArgumentType.word())
                .suggests((context, suggestionBuilder) -> suggestionBuilder.buildFuture())
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(context -> {
                                            ensureStaffPassword(context);
                                            return executeCreate(context);
                                        }))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(context -> {
                                    ensureStaffPassword(context);
                                    return executeDelete(context);
                                })))
                .then(CommandManager.literal("setpassword")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(context -> {
                                            ensureStaffPassword(context);
                                            return executeSetPassword(context);
                                        }))))
                .then(CommandManager.literal("setpin")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(context -> {
                                            ensureStaffPassword(context);
                                            return executeSetPassword(context);
                                        }))))
                .then(CommandManager.literal("disable")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(context -> {
                                    ensureStaffPassword(context);
                                    return executeDisable(context);
                                })))
                .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(context -> {
                                    ensureStaffPassword(context);
                                    return executeEnable(context);
                                })))
                .then(CommandManager.literal("list")
                        .executes(context -> {
                            ensureStaffPassword(context);
                            return executeList(context);
                        }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> appendAdminCommands(
            com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> builder,
            String root,
            boolean includePlayerCommands
    ) {
        return builder
                .then(CommandManager.literal("create")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(TrainerCommand::executeCreate))))
                .then(CommandManager.literal("delete")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(TrainerCommand::executeDelete)))
                .then(CommandManager.literal("setpassword")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(TrainerCommand::executeSetPassword))))
                .then(CommandManager.literal("setpin")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(TrainerCommand::executeSetPassword))))
                .then(CommandManager.literal("disable")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(TrainerCommand::executeDisable)))
                .then(CommandManager.literal("enable")
                        .requires(TrainerCommand::canRunAdmin)
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .executes(TrainerCommand::executeEnable)))
                .then(CommandManager.literal("list")
                        .requires(TrainerCommand::canRunAdmin)
                        .executes(TrainerCommand::executeList))
                .then(CommandManager.literal("help")
                        .executes(context -> {
                            final String helpText;
                            if (BestTrainerAuthMod.config().allowSelfRegistration()) {
                                helpText =
                                        "/" + root + " login <key> <password>, " +
                                                "/" + root + " logout, " +
                                                "/" + root + " whoami, " +
                                                "/" + root + " claimcurrent <key> <password>, " +
                                                "/" + root + " register <key> <password>, " +
                                                "/" + root + " create <key> <password>, " +
                                                "/" + root + " delete <key>, " +
                                                "/" + root + " setpassword <key> <password>";
                            } else {
                                helpText =
                                        "/" + root + " login <key> <password>, " +
                                                "/" + root + " logout, " +
                                                "/" + root + " whoami, " +
                                                "/" + root + " claimcurrent <key> <password>, " +
                                                "/" + root + " create <key> <password>, " +
                                                "/" + root + " delete <key>, " +
                                                "/" + root + " setpassword <key> <password>";
                            }

                            context.getSource().sendFeedback(() -> Text.literal(helpText), false);
                            return 1;
                        }));
    }

    private static int executeCreate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        String password = StringArgumentType.getString(context, "password");
        TrainerAccount created = createProfile(key, password, context.getSource().getName());
        context.getSource().sendFeedback(() -> Text.literal("Created profile: " + created.key()), true);
        return 1;
    }

    private static int executeDelete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        boolean removed = BestTrainerAuthMod.trainerStore().delete(key);
        boolean removedData = BestTrainerAuthMod.trainerBridge().deleteTrainerData(key);

        if (!removed && !removedData) {
            throw new SimpleCommandExceptionType(Text.literal("Profile key not found.")).create();
        }

        context.getSource().sendFeedback(() -> Text.literal("Deleted profile: " + key), true);
        return 1;
    }

    private static int executeSetPassword(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String key = StringArgumentType.getString(context, "key");
        String password = StringArgumentType.getString(context, "password");
        BestTrainerAuthMod.trainerStore().updatePin(key, PinHasher.hash(password));
        context.getSource().sendFeedback(() -> Text.literal("Updated password for: " + key), true);
        return 1;
    }

    private static int executeDisable(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key");
        BestTrainerAuthMod.trainerStore().setEnabled(key, false);
        context.getSource().sendFeedback(() -> Text.literal("Disabled profile: " + key), true);
        return 1;
    }

    private static int executeEnable(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key");
        BestTrainerAuthMod.trainerStore().setEnabled(key, true);
        context.getSource().sendFeedback(() -> Text.literal("Enabled profile: " + key), true);
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> context) {
        String joined = BestTrainerAuthMod.trainerStore().all().stream()
                .sorted(Comparator.comparing(TrainerAccount::key))
                .map(account -> account.key() + (account.enabled() ? "" : " [disabled]"))
                .reduce((left, right) -> left + ", " + right)
                .orElse("No profiles exist yet.");
        context.getSource().sendFeedback(() -> Text.literal(joined), false);
        return 1;
    }

    private static void disconnectNextTick(ServerPlayerEntity player, String message) {
        player.getServer().execute(() -> player.networkHandler.disconnect(Text.literal(message)));
    }

    private static TrainerAccount createProfile(String key, String password, String createdBy) throws CommandSyntaxException {
        try {
            KeyNormalizer.normalize(key);
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType(Text.literal(e.getMessage())).create();
        }

        if (BestTrainerAuthMod.trainerStore().exists(key)) {
            throw new SimpleCommandExceptionType(
                    Text.literal("Profile key already exists.")
            ).create();
        }

        return BestTrainerAuthMod.trainerStore().create(key, PinHasher.hash(password), createdBy);
    }
}
