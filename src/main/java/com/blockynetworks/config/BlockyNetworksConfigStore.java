package com.blockynetworks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class BlockyNetworksConfigStore {
    private final Path path;
    private final Gson gson;
    private final HytaleLogger logger;
    private BlockyNetworksConfig config;

    public BlockyNetworksConfigStore(Path path, HytaleLogger logger) {
        this.path = path;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.config = new BlockyNetworksConfig();
    }

    public synchronized BlockyNetworksConfig get() {
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
            BlockyNetworksConfig loaded = gson.fromJson(json, BlockyNetworksConfig.class);
            if (loaded != null) {
                this.config = loaded;
            }
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("BlockyNetworks: Failed to load config, using defaults");
            this.config = new BlockyNetworksConfig();
        }
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config));
        } catch (IOException e) {
            logger.at(Level.WARNING).withCause(e).log("BlockyNetworks: Failed to save config");
        }
    }
}
