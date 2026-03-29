package com.hyhorde.arenapve.horde;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class PlayerFaceCache {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final ConcurrentHashMap<UUID, CompletableFuture<byte[]>> cache = new ConcurrentHashMap<UUID, CompletableFuture<byte[]>>();

    public CompletableFuture<byte[]> getOrLoad(UUID playerId, String username) {
        if (playerId == null) {
            return CompletableFuture.completedFuture(null);
        }
        String lookup = username == null ? "" : username.trim();
        return this.cache.computeIfAbsent(playerId, ignored -> CompletableFuture.supplyAsync(() -> PlayerFaceCache.downloadHeadPng(lookup)).whenComplete((facePng, error) -> {
            if (error != null || facePng == null || facePng.length == 0) {
                this.cache.remove(playerId);
            }
        }));
    }

    public byte[] getIfPresent(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        CompletableFuture<byte[]> future = this.cache.get(playerId);
        if (future == null) {
            return null;
        }
        return future.getNow(null);
    }

    public void remove(UUID playerId) {
        if (playerId != null) {
            this.cache.remove(playerId);
        }
    }

    private static byte[] downloadHeadPng(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String url = PlayerFaceCache.buildHeadUrl(username, 128, 0);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .header("Accept", "image/png")
                .build();
        try {
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() == 200 && body != null && body.length > 0) {
                return body;
            }
        }
        catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    private static String buildHeadUrl(String username, int size, int rotate) {
        int clampedSize = Math.max(64, Math.min(2048, size));
        int clampedRotate = Math.max(0, Math.min(360, rotate));
        String encodedUsername = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://hyvatar.io/render/" + encodedUsername + "?size=" + clampedSize + "&rotate=" + clampedRotate;
    }
}
