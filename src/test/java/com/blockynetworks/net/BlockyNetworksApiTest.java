package com.blockynetworks.net;

import com.blockynetworks.testutil.TestLoggers;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockyNetworksApiTest {

    private static final Gson GSON = new Gson();

    private record RecordedRequest(
            String method,
            String path,
            String contentType,
            String body
    ) {
    }

    private static RecordedRequest recordRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String path = exchange.getRequestURI().getPath();
        return new RecordedRequest(exchange.getRequestMethod(), path, contentType, body);
    }

    private static void respondJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Test
    void createServerLinkCode_postsExpectedJsonAndParsesResponse() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/game/server-link-code", (exchange) -> {
            recorded.set(recordRequest(exchange));
            respondJson(exchange, 200, "{\"code\":\"654321\",\"serverSecret\":\"secret\",\"expiresAt\":1700000000000}");
        });

        try {
            server.start();
            String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            BlockyNetworksApi api = new BlockyNetworksApi(baseUrl, TestLoggers.noop());

            BlockyNetworksApi.ServerLinkCodeResponse res = api.createServerLinkCode("server-1", "  My Server  ");
            assertEquals("654321", res.code);
            assertEquals("secret", res.serverSecret);
            assertEquals(1700000000000L, res.expiresAt);

            RecordedRequest req = recorded.get();
            assertNotNull(req);
            assertEquals("POST", req.method);
            assertEquals("/game/server-link-code", req.path);
            assertEquals("application/json", req.contentType);

            JsonObject payload = GSON.fromJson(req.body, JsonObject.class);
            assertEquals("server-1", payload.get("serverId").getAsString());
            assertEquals("My Server", payload.get("serverName").getAsString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void createPlayerLinkCode_postsExpectedJsonAndParsesResponse() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/game/player-link-code", (exchange) -> {
            recorded.set(recordRequest(exchange));
            respondJson(exchange, 200, "{\"code\":\"123456\",\"expiresAt\":1700000000000}");
        });

        try {
            server.start();
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            BlockyNetworksApi api = new BlockyNetworksApi(baseUrl, TestLoggers.noop());

            BlockyNetworksApi.PlayerLinkCodeResponse res =
                    api.createPlayerLinkCode("server-1", "player-uuid", "PlayerName");
            assertEquals("123456", res.code);
            assertEquals(1700000000000L, res.expiresAt);

            RecordedRequest req = recorded.get();
            assertNotNull(req);
            assertEquals("/game/player-link-code", req.path);

            JsonObject payload = GSON.fromJson(req.body, JsonObject.class);
            assertEquals("server-1", payload.get("serverId").getAsString());
            assertEquals("player-uuid", payload.get("playerUuid").getAsString());
            assertEquals("PlayerName", payload.get("playerUsername").getAsString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sendHeartbeat_postsPlayersList() throws Exception {
        AtomicReference<RecordedRequest> recorded = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/game/heartbeat", (exchange) -> {
            recorded.set(recordRequest(exchange));
            respondJson(exchange, 200, "{\"ok\":true}");
        });

        try {
            server.start();
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            BlockyNetworksApi api = new BlockyNetworksApi(baseUrl, TestLoggers.noop());

            UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

            api.sendHeartbeat(
                    "server-1",
                    "secret-1",
                    Map.of(a, "Alice", b, "Bob")
            );

            RecordedRequest req = recorded.get();
            assertNotNull(req);
            assertEquals("/game/heartbeat", req.path);

            JsonObject payload = GSON.fromJson(req.body, JsonObject.class);
            assertEquals("server-1", payload.get("serverId").getAsString());
            assertEquals("secret-1", payload.get("serverSecret").getAsString());

            JsonArray players = payload.getAsJsonArray("players");
            assertEquals(2, players.size());

            Map<String, String> byUuid = new HashMap<>();
            for (int i = 0; i < players.size(); i++) {
                JsonObject player = players.get(i).getAsJsonObject();
                byUuid.put(player.get("uuid").getAsString(), player.get("username").getAsString());
            }

            assertEquals(Map.of(a.toString(), "Alice", b.toString(), "Bob"), byUuid);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void non2xxResponses_throwRuntimeException() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/game/player-link-code", (exchange) -> respondJson(exchange, 500, "{\"error\":\"oops\"}"));

        try {
            server.start();
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            BlockyNetworksApi api = new BlockyNetworksApi(baseUrl, TestLoggers.noop());

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> api.createPlayerLinkCode("server-1", "player-uuid", "PlayerName")
            );
            assertTrue(ex.getMessage().contains("HTTP 500"));
        } finally {
            server.stop(0);
        }
    }
}
