package com.blockynetwork.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class BlockyNetworkConfigStore {
    private final Path path;
    private final Gson gson;
    private final HytaleLogger logger;
    private BlockyNetworkConfig config;

    public BlockyNetworkConfigStore(Path path, HytaleLogger logger) {
        this.path = path;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = new BlockyNetworkConfig();
    }

    public synchronized BlockyNetworkConfig get() {
        return config;
    }

    public synchronized void load() {
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                save();
                return;
            }

            String json = Files.readString(path);
            BlockyNetworkConfig loaded = gson.fromJson(json, BlockyNetworkConfig.class);
            if (loaded != null) {
                this.config = loaded;
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("BlockyNetwork: Failed to load config, using defaults");
            this.config = new BlockyNetworkConfig();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config));
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("BlockyNetwork: Failed to save config");
        }
    }
}
