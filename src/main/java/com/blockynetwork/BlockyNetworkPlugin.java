package com.blockynetwork;

import com.blockynetwork.commands.LinkCommand;
import com.blockynetwork.config.BlockyNetworkConfig;
import com.blockynetwork.config.BlockyNetworkConfigStore;
import com.blockynetwork.net.BlockyNetworksApi;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BlockyNetworkPlugin extends JavaPlugin {

    private static final long HEARTBEAT_SECONDS = 20;

    private BlockyNetworkConfigStore configStore;
    private BlockyNetworksApi api;
    private ScheduledExecutorService heartbeatExecutor;
    private final ConcurrentHashMap<UUID, String> onlinePlayers = new ConcurrentHashMap<>();

    public BlockyNetworkPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("BlockyNetwork: Setup phase...");

        Path configPath = getDataDirectory().resolve("blockynetwork.json");
        this.configStore = new BlockyNetworkConfigStore(configPath, getLogger());
        this.configStore.load();

        BlockyNetworkConfig cfg = this.configStore.get();
        if (cfg.convexHttpUrl == null || cfg.convexHttpUrl.trim().isEmpty()) {
            getLogger().at(Level.WARNING).log("BlockyNetwork: convexHttpUrl is not set in %s", configPath);
        } else {
            this.api = new BlockyNetworksApi(cfg.convexHttpUrl, getLogger());
        }
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("BlockyNetwork: Started successfully!");

        // Commands
        getCommandRegistry().registerCommand(new LinkCommand(this));

        // Track online players for heartbeat updates.
        getEventRegistry().register(PlayerConnectEvent.class, (event) -> {
            UUID uuid = event.getPlayerRef().getUuid();
            String username = event.getPlayerRef().getUsername();
            onlinePlayers.put(uuid, username);
        });
        getEventRegistry().register(PlayerDisconnectEvent.class, (event) -> {
            UUID uuid = event.getPlayerRef().getUuid();
            onlinePlayers.remove(uuid);
        });

        // Heartbeat to BlockyNetworks (best-effort).
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread t = new Thread(r, "BlockyNetwork-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        this.heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeatSafe, 5, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("BlockyNetwork: Shutting down...");

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
    }

    private void sendHeartbeatSafe() {
        if (configStore == null) return;

        BlockyNetworkConfig cfg = configStore.get();
        if (cfg.convexHttpUrl == null || cfg.convexHttpUrl.trim().isEmpty()) return;
        if (cfg.serverId == null || cfg.serverId.trim().isEmpty()) return;
        if (cfg.serverSecret == null || cfg.serverSecret.trim().isEmpty()) return;

        try {
            // Refresh API if the base URL changes on disk (rare, but helpful during setup).
            if (api == null) {
                api = new BlockyNetworksApi(cfg.convexHttpUrl, getLogger());
            }

            api.sendHeartbeat(cfg.serverId.trim(), cfg.serverSecret.trim(), onlinePlayers);
        } catch (Exception e) {
            getLogger().at(Level.FINE).withCause(e).log("BlockyNetwork: Heartbeat failed");
        }
    }

    public BlockyNetworkConfigStore getConfigStore() {
        return configStore;
    }

    public BlockyNetworksApi getApi() {
        BlockyNetworkConfig cfg = configStore != null ? configStore.get() : null;
        if (cfg == null) {
            throw new IllegalStateException("Config store not initialized");
        }
        if (api == null || cfg.convexHttpUrl == null || cfg.convexHttpUrl.trim().isEmpty()) {
            api = new BlockyNetworksApi(cfg.convexHttpUrl, getLogger());
        }
        return api;
    }
}
