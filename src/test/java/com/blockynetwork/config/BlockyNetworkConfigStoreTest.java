package com.blockynetwork.config;

import com.blockynetwork.testutil.TestLoggers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockyNetworkConfigStoreTest {

    @Test
    void load_createsFileWithDefaultsWhenMissing(@TempDir Path tempDir) {
        Path configPath = tempDir.resolve("blockynetwork.json");

        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        store.load();

        assertTrue(Files.exists(configPath));

        BlockyNetworkConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("", cfg.convexHttpUrl);
        assertEquals("", cfg.serverId);
        assertEquals("", cfg.serverSecret);
        assertEquals("", cfg.serverName);
    }

    @Test
    void load_readsExistingConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetwork.json");
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

        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        store.load();

        BlockyNetworkConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("http://localhost:1234", cfg.convexHttpUrl);
        assertEquals("server-123", cfg.serverId);
        assertEquals("secret-abc", cfg.serverSecret);
        assertEquals("My Server", cfg.serverName);
    }

    @Test
    void load_invalidJson_fallsBackToDefaults(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetwork.json");
        Files.writeString(configPath, "{ not valid json");

        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        store.load();

        BlockyNetworkConfig cfg = store.get();
        assertNotNull(cfg);
        assertEquals("", cfg.convexHttpUrl);
        assertEquals("", cfg.serverId);
        assertEquals("", cfg.serverSecret);
        assertEquals("", cfg.serverName);
    }

    @Test
    void save_writesUpdatedConfig(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("blockynetwork.json");

        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        BlockyNetworkConfig cfg = store.get();
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
}

