package com.blockynetwork.commands;

import com.blockynetwork.BlockyNetworkPlugin;
import com.blockynetwork.config.BlockyNetworkConfig;
import com.blockynetwork.config.BlockyNetworkConfigStore;
import com.blockynetwork.net.BlockyNetworksApi;
import com.blockynetwork.testutil.TestLoggers;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkServerCommandTest {

    @Test
    void setsExpectedPermissionNode() {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        LinkServerCommand cmd = new LinkServerCommand(plugin);
        assertEquals("blockynetwork.linkserver", cmd.getPermission());
    }

    @Test
    void execute_savesServerSecret(@TempDir Path tempDir) throws Exception {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        when(plugin.getLogger()).thenReturn(TestLoggers.noop());

        Path configPath = tempDir.resolve("blockynetwork.json");
        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        BlockyNetworkConfig cfg = store.get();
        cfg.convexHttpUrl = "http://localhost:1234";
        cfg.serverId = "";
        cfg.serverName = "  My Server  ";

        when(plugin.getConfigStore()).thenReturn(store);

        BlockyNetworksApi api = mock(BlockyNetworksApi.class);
        BlockyNetworksApi.ServerLinkCodeResponse apiRes = new BlockyNetworksApi.ServerLinkCodeResponse();
        apiRes.code = "654321";
        apiRes.serverSecret = "secret-xyz";
        apiRes.expiresAt = 1700000000000L;
        when(api.createServerLinkCode(anyString(), anyString())).thenReturn(apiRes);
        when(plugin.getApi()).thenReturn(api);

        CommandContext ctx = mock(CommandContext.class);
        when(ctx.isPlayer()).thenReturn(true);

        LinkServerCommand cmd = new LinkServerCommand(plugin);
        cmd.execute(ctx).join();

        assertNotNull(cfg.serverId);
        assertTrue(!cfg.serverId.isBlank());
        assertEquals("secret-xyz", cfg.serverSecret);

        verify(api).createServerLinkCode(cfg.serverId, "My Server");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(ctx, times(2)).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().get(0).getRawText().startsWith("Server link code: 654321"));
        assertTrue(captor.getAllValues().get(1).getRawText().contains("Link Game Server"));

        String savedJson = Files.readString(configPath);
        assertTrue(savedJson.contains(cfg.serverId));
        assertTrue(savedJson.contains("secret-xyz"));
    }
}

