package com.blockynetwork.testutil;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.logging.Level;

public final class TestLoggers {
    private TestLoggers() {
    }

    public static HytaleLogger noop() {
        // The Hytale logger backend is safe to use for unit tests as long as the
        // JUL log manager is configured via JVM args (see build.gradle).
        HytaleLogger logger = HytaleLogger.get("BlockyNetworkTest");
        logger.setLevel(Level.OFF);
        return logger;
    }
}
