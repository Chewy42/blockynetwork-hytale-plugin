package com.blockynetworks.config;

import com.blockynetworks.testutil.TestLoggers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockyNetworksConfigStoreTest {

    @Test
    void load_createsFileWithDefaultsWhenMissing(@TempDir Path tempDir) {
        Path configPath = tempDir.resolve("blockynetworks.json");

        BlockyNetworksConfigStore store = new BlockyNetworksConfigStore(configPath, TestLoggers.noop());
        store.load();

        assertTrue(Files.exists(configPath));

        BlockyNetworksConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("", cfg.convexHttpUrl);
        assertEquals("", cfg.serverId);
        assertEquals("", cfg.serverSecret);
        assertEquals("", cfg.serverName);
    }

    @Test
    void load_readsExistingConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetworks.json");
        Files.writeString(
                configPath,
                """
                        {
                          "convexHttpUrl": "http://localhost:1234",
                          "serverId": "server-123",
                          "serverSecret": "secret-abc",
                          "serverName": "My Server"
                        }
                        """
        );

        BlockyNetworksConfigStore store = new BlockyNetworksConfigStore(configPath, TestLoggers.noop());
        store.load();

        BlockyNetworksConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("http://localhost:1234", cfg.convexHttpUrl);
        assertEquals("server-123", cfg.serverId);
        assertEquals("secret-abc", cfg.serverSecret);
        assertEquals("My Server", cfg.serverName);
    }

    @Test
    void load_invalidJson_fallsBackToDefaults(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetworks.json");
        Files.writeString(configPath, "{ not valid json");

        BlockyNetworksConfigStore store = new BlockyNetworksConfigStore(configPath, TestLoggers.noop());
        store.load();

        BlockyNetworksConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("", cfg.convexHttpUrl);
        assertEquals("", cfg.serverId);
        assertEquals("", cfg.serverSecret);
        assertEquals("", cfg.serverName);
    }

    @Test
    void save_writesUpdatedConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetworks.json");

        BlockyNetworksConfigStore store = new BlockyNetworksConfigStore(configPath, TestLoggers.noop());
        BlockyNetworksConfig cfg = store.get();
        cfg.convexHttpUrl = "http://localhost:4444";
        cfg.serverId = "server-1";
        cfg.serverSecret = "secret-1";
        cfg.serverName = "Example";

        store.save();

        String json = Files.readString(configPath);
        assertTrue(json.contains("\"convexHttpUrl\": \"http://localhost:4444\""));
        assertTrue(json.contains("\"serverId\": \"server-1\""));
        assertTrue(json.contains("\"serverSecret\": \"secret-1\""));
        assertTrue(json.contains("\"serverName\": \"Example\""));
    }

    @Test
    void load_migratesLegacyConfigWhenPrimaryMissing(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("BlockyNetworks").resolve("blockynetworks.json");
        Path legacyPath = tempDir.resolve("BlockyNetwork").resolve("blockynetwork.json");
        Files.createDirectories(legacyPath.getParent());
        Files.writeString(
                legacyPath,
                """
                        {
                          "convexHttpUrl": "http://localhost:9876",
                          "serverId": "legacy-server",
                          "serverSecret": "legacy-secret",
                          "serverName": "Legacy Server"
                        }
                        """
        );

        BlockyNetworksConfigStore store = new BlockyNetworksConfigStore(configPath, legacyPath, TestLoggers.noop());
        store.load();

        BlockyNetworksConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("http://localhost:9876", cfg.convexHttpUrl);
        assertEquals("legacy-server", cfg.serverId);
        assertEquals("legacy-secret", cfg.serverSecret);
        assertEquals("Legacy Server", cfg.serverName);
        assertTrue(Files.exists(configPath));
        assertTrue(Files.exists(legacyPath));

        String migratedJson = Files.readString(configPath);
        assertTrue(migratedJson.contains("\"convexHttpUrl\": \"http://localhost:9876\""));
        assertTrue(migratedJson.contains("\"serverId\": \"legacy-server\""));
        assertTrue(migratedJson.contains("\"serverSecret\": \"legacy-secret\""));
        assertTrue(migratedJson.contains("\"serverName\": \"Legacy Server\""));
    }
}
