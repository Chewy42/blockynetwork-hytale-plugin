package com.blockynetwork.commands;

import com.blockynetwork.BlockyNetworkPlugin;
import com.blockynetwork.config.BlockyNetworkConfig;
import com.blockynetwork.config.BlockyNetworkConfigStore;
import com.blockynetwork.net.BlockyNetworksApi;
import com.blockynetwork.testutil.TestLoggers;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCommandTest {

    @Test
    void registersServerSubcommand() {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        LinkCommand cmd = new LinkCommand(plugin);

        assertTrue(cmd.getSubCommands().containsKey("server"));
    }

    @Test
    void execute_whenNotPlayer_sendsMessage() {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        LinkCommand cmd = new LinkCommand(plugin);

        CommandContext ctx = mock(CommandContext.class);
        when(ctx.isPlayer()).thenReturn(false);

        cmd.execute(ctx).join();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(ctx).sendMessage(captor.capture());
        assertEquals("This command must be run by a player.", captor.getValue().getRawText());
    }

    @Test
    void execute_whenNotConfigured_sendsMessage(@TempDir Path tempDir) {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        when(plugin.getLogger()).thenReturn(TestLoggers.noop());

        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(
                tempDir.resolve("blockynetwork.json"),
                TestLoggers.noop()
        );
        when(plugin.getConfigStore()).thenReturn(store);

        LinkCommand cmd = new LinkCommand(plugin);

        CommandContext ctx = mock(CommandContext.class);
        when(ctx.isPlayer()).thenReturn(true);

        cmd.execute(ctx).join();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(ctx).sendMessage(captor.capture());
        assertTrue(captor.getValue().getRawText().contains("BlockyNetwork is not configured"));
    }

    @Test
    void execute_generatesServerId_andCallsApi(@TempDir Path tempDir) throws Exception {
        BlockyNetworkPlugin plugin = mock(BlockyNetworkPlugin.class);
        when(plugin.getLogger()).thenReturn(TestLoggers.noop());

        Path configPath = tempDir.resolve("blockynetwork.json");
        BlockyNetworkConfigStore store = new BlockyNetworkConfigStore(configPath, TestLoggers.noop());
        BlockyNetworkConfig cfg = store.get();
        cfg.convexHttpUrl = "http://localhost:1234";
        cfg.serverId = "";

        when(plugin.getConfigStore()).thenReturn(store);

        BlockyNetworksApi api = mock(BlockyNetworksApi.class);
        BlockyNetworksApi.PlayerLinkCodeResponse apiRes = new BlockyNetworksApi.PlayerLinkCodeResponse();
        apiRes.code = "123456";
        apiRes.expiresAt = 1700000000000L;
        when(api.createPlayerLinkCode(anyString(), anyString(), anyString())).thenReturn(apiRes);
        when(plugin.getApi()).thenReturn(api);

        CommandContext ctx = mock(CommandContext.class);
        when(ctx.isPlayer()).thenReturn(true);

        CommandSender sender = mock(CommandSender.class);
        UUID playerUuid = UUID.fromString("00000000-0000-0000-0000-000000000123");
        when(sender.getUuid()).thenReturn(playerUuid);
        when(sender.getDisplayName()).thenReturn("PlayerName");
        when(ctx.sender()).thenReturn(sender);

        LinkCommand cmd = new LinkCommand(plugin);
        cmd.execute(ctx).join();

        assertNotNull(cfg.serverId);
        assertTrue(!cfg.serverId.isBlank());

        verify(api).createPlayerLinkCode(cfg.serverId, playerUuid.toString(), "PlayerName");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(ctx, times(2)).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().get(0).getRawText().startsWith("Your link code: 123456"));
        assertTrue(captor.getAllValues().get(1).getRawText().contains("BlockyNetworks website"));

        String savedJson = Files.readString(configPath);
        assertTrue(savedJson.contains(cfg.serverId));
    }
}
