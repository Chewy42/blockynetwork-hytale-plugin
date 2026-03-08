package com.blockynetworks.commands;

import com.blockynetworks.BlockyNetworksPlugin;
import com.blockynetworks.config.BlockyNetworksConfig;
import com.blockynetworks.net.BlockyNetworksApi;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LinkCommand extends AbstractCommand {
    private final BlockyNetworksPlugin plugin;

    public LinkCommand(BlockyNetworksPlugin plugin) {
        super("link", "Generate a code to link your in-game account to BlockyNetworks");
        this.plugin = plugin;

        addSubCommand(new LinkServerCommand(plugin));
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        BlockyNetworksConfig cfg = plugin.getConfigStore().get();
        if (cfg.convexHttpUrl == null || cfg.convexHttpUrl.trim().isEmpty()) {
            ctx.sendMessage(Message.raw("BlockyNetworks is not configured: set convexHttpUrl in the plugin config."));
            return CompletableFuture.completedFuture(null);
        }

        if (cfg.serverId == null || cfg.serverId.trim().isEmpty()) {
            cfg.serverId = UUID.randomUUID().toString();
            plugin.getConfigStore().save();
        }

        CommandSender sender = ctx.sender();
        String playerUuid = sender.getUuid().toString();
        String username = sender.getDisplayName();

        return CompletableFuture.runAsync(() -> {
            try {
                BlockyNetworksApi api = plugin.getApi();
                BlockyNetworksApi.PlayerLinkCodeResponse res =
                        api.createPlayerLinkCode(cfg.serverId, playerUuid, username);

                String expires = DateTimeFormatter.ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(res.expiresAt));

                ctx.sendMessage(Message.raw("Your link code: " + res.code + " (expires " + expires + ")"));
                ctx.sendMessage(Message.raw("Enter this code on the BlockyNetworks website to link your account."));
            } catch (Exception e) {
                plugin.getLogger().at(Level.WARNING).withCause(e).log("BlockyNetworks: Failed to create player link code");
                ctx.sendMessage(Message.raw("Failed to generate link code. Check server logs."));
            }
        });
    }
}
