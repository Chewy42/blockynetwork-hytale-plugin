package com.blockynetworks.commands;

import com.blockynetworks.BlockyNetworksPlugin;
import com.blockynetworks.config.BlockyNetworksConfig;
import com.blockynetworks.net.BlockyNetworksApi;
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
    private static final String LEGACY_PERMISSION_NODE = "blockynetwork.linkserver";

    private final BlockyNetworksPlugin plugin;

    public LinkServerCommand(BlockyNetworksPlugin plugin) {
        super("server", "Generate a code to link this server to a BlockyNetworks organization");
        this.plugin = plugin;

        // Keep the legacy permission node so existing permission configs keep working.
        requirePermission(LEGACY_PERMISSION_NODE);
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
                plugin.getLogger().at(Level.WARNING).withCause(e).log("BlockyNetworks: Failed to create server link code");
                ctx.sendMessage(Message.raw("Failed to generate server link code. Check server logs."));
            }
        });
    }
}
