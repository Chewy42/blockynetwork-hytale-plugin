package com.blockynetwork.commands;

import com.blockynetwork.BlockyNetworkPlugin;
import com.blockynetwork.config.BlockyNetworkConfig;
import com.blockynetwork.net.BlockyNetworksApi;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LinkServerCommand extends AbstractCommand {
    private final BlockyNetworkPlugin plugin;

    public LinkServerCommand(BlockyNetworkPlugin plugin) {
        super("server", "Generate a code to link this server to a BlockyNetworks organization");
        this.plugin = plugin;

        // Let server owners decide who can use this via permissions.json.
        requirePermission("blockynetwork.linkserver");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command must be run by a player."));
            return CompletableFuture.completedFuture(null);
        }

        BlockyNetworkConfig cfg = plugin.getConfigStore().get();
        if (cfg.convexHttpUrl == null || cfg.convexHttpUrl.trim().isEmpty()) {
            ctx.sendMessage(Message.raw("BlockyNetwork is not configured: set convexHttpUrl in the plugin config."));
            return CompletableFuture.completedFuture(null);
        }

        if (cfg.serverId == null || cfg.serverId.trim().isEmpty()) {
            cfg.serverId = UUID.randomUUID().toString();
            plugin.getConfigStore().save();
        }

        String serverName = cfg.serverName != null && !cfg.serverName.trim().isEmpty()
                ? cfg.serverName.trim()
                : "Hytale Server";

        return CompletableFuture.runAsync(() -> {
            try {
                BlockyNetworksApi api = plugin.getApi();
                BlockyNetworksApi.ServerLinkCodeResponse res = api.createServerLinkCode(cfg.serverId, serverName);

                cfg.serverSecret = res.serverSecret;
                plugin.getConfigStore().save();

                String expires = DateTimeFormatter.ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(res.expiresAt));

                ctx.sendMessage(Message.raw("Server link code: " + res.code + " (expires " + expires + ")"));
                ctx.sendMessage(Message.raw("Enter this code in your Organization Settings -> Link Game Server."));
            } catch (Exception e) {
                plugin.getLogger().at(Level.WARNING).withCause(e).log("BlockyNetwork: Failed to create server link code");
                ctx.sendMessage(Message.raw("Failed to generate server link code. Check server logs."));
            }
        });
    }
}

