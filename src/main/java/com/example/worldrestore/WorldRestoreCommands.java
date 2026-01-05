package com.example.worldrestore;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class WorldRestoreCommands {
    private WorldRestoreCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("worldrestore")
                .then(Commands.literal("status")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            String templatePath = WorldRestoreConfig.VALUES.templateZipPath.get();
                            boolean enabled = WorldRestoreConfig.VALUES.enabled.get();
                            boolean failHard = WorldRestoreConfig.VALUES.failHardIfMissing.get();
                            boolean preserveServerConfig = WorldRestoreConfig.VALUES.preserveServerConfigDir.get();

                            source.sendSuccess(() -> Component.literal("WorldRestore status:"), false);
                            source.sendSuccess(() -> Component.literal("- enabled: " + enabled), false);
                            source.sendSuccess(() -> Component.literal("- templateZipPath: " + templatePath), false);
                            source.sendSuccess(() -> Component.literal("- failHardIfMissing: " + failHard), false);
                            source.sendSuccess(() -> Component.literal("- preserveServerConfigDir: " + preserveServerConfig), false);
                            source.sendSuccess(() -> Component.literal("- lastResult: "
                                    + (WorldRestoreState.wasLastSkipped() ? "SKIPPED" : (WorldRestoreState.wasLastSuccess() ? "SUCCESS" : "FAILED"))), false);
                            source.sendSuccess(() -> Component.literal("- lastMessage: " + WorldRestoreState.getLastMessage()), false);
                            source.sendSuccess(() -> Component.literal("- lastDurationMs: " + WorldRestoreState.getLastDurationMs()), false);
                            WorldRestoreState.getLastRun()
                                    .ifPresent(instant -> source.sendSuccess(() -> Component.literal("- lastRun: " + instant), false));
                            return 1;
                        }))
                .then(Commands.literal("restore")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            MinecraftServer server = source.getServer();
                            source.sendSuccess(() -> Component.literal("World restore requested. The server will shut down and restore on next start."), false);
                            server.execute(() -> server.halt(false));
                            return 1;
                        }))
        );
    }
}
