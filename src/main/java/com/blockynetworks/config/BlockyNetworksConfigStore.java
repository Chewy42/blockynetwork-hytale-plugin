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
    private final Path legacyPath;
    private final Gson gson;
    private final HytaleLogger logger;
    private BlockyNetworksConfig config;

    public BlockyNetworksConfigStore(Path path, HytaleLogger logger) {
        this(path, null, logger);
    }

    public BlockyNetworksConfigStore(Path path, Path legacyPath, HytaleLogger logger) {
        this.path = path;
        this.legacyPath = legacyPath;
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
            Path sourcePath = resolveSourcePath();
            if (sourcePath == null) {
                save();
                return;
            }

            String json = Files.readString(sourcePath);
            BlockyNetworksConfig loaded = gson.fromJson(json, BlockyNetworksConfig.class);
            if (loaded != null) {
                this.config = loaded;
            }

            if (legacyPath != null && legacyPath.equals(sourcePath)) {
                save();
                logger.at(Level.INFO).log("BlockyNetworks: Migrated legacy config from %s to %s", legacyPath, path);
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

    private Path resolveSourcePath() {
        if (Files.exists(path)) {
            return path;
        }
        if (legacyPath != null && Files.exists(legacyPath)) {
            return legacyPath;
        }
        return null;
    }
}
