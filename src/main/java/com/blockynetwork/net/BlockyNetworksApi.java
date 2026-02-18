package com.blockynetwork.net;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BlockyNetworksApi {
    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson;
    private final HytaleLogger logger;

    public BlockyNetworksApi(String baseUrl, HytaleLogger logger) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.gson = new Gson();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.endsWith("/")) return trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    public ServerLinkCodeResponse createServerLinkCode(String serverId, String serverName) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("serverId", serverId);
        if (serverName != null && !serverName.trim().isEmpty()) {
            body.addProperty("serverName", serverName.trim());
        }

        String json = sendJsonPost("/game/server-link-code", body);
        JsonObject res = gson.fromJson(json, JsonObject.class);

        ServerLinkCodeResponse out = new ServerLinkCodeResponse();
        out.code = res.get("code").getAsString();
        out.serverSecret = res.get("serverSecret").getAsString();
        out.expiresAt = res.get("expiresAt").getAsLong();
        return out;
    }

    public PlayerLinkCodeResponse createPlayerLinkCode(String serverId, String playerUuid, String playerUsername) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("serverId", serverId);
        body.addProperty("playerUuid", playerUuid);
        body.addProperty("playerUsername", playerUsername);

        String json = sendJsonPost("/game/player-link-code", body);
        JsonObject res = gson.fromJson(json, JsonObject.class);

        PlayerLinkCodeResponse out = new PlayerLinkCodeResponse();
        out.code = res.get("code").getAsString();
        out.expiresAt = res.get("expiresAt").getAsLong();
        return out;
    }

    public void sendHeartbeat(String serverId, String serverSecret, Map<UUID, String> players) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("serverId", serverId);
        body.addProperty("serverSecret", serverSecret);

        JsonArray arr = new JsonArray();
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            JsonObject p = new JsonObject();
            p.addProperty("uuid", entry.getKey().toString());
            p.addProperty("username", entry.getValue());
            arr.add(p);
        }
        body.add("players", arr);

        sendJsonPost("/game/heartbeat", body);
    }

    private String sendJsonPost(String path, JsonObject body) throws Exception {
        if (baseUrl.isEmpty()) {
            throw new IllegalStateException("convexHttpUrl is not configured");
        }

        URI uri = URI.create(baseUrl + path);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            logger.at(Level.WARNING).log("BlockyNetwork: HTTP %s from %s: %s", status, uri, response.body());
            throw new RuntimeException("HTTP " + status);
        }

        return response.body();
    }

    public static class ServerLinkCodeResponse {
        public String code;
        public String serverSecret;
        public long expiresAt;
    }

    public static class PlayerLinkCodeResponse {
        public String code;
        public long expiresAt;
    }
}

