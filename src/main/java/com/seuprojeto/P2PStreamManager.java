package com.seuprojeto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P2PStreamManager {

    private static final int ENGINE_PORT = 6878;
    private static final String ENGINE_HOST = "127.0.0.1";
    private final HttpClient httpClient;

    public P2PStreamManager() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isP2PStream(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("acestream://") || lowerUrl.startsWith("p2p://");
    }

    private String extractP2PHash(String url) {
        Pattern pattern = Pattern.compile("^(?:acestream|p2p)://([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) return matcher.group(1);
        throw new IllegalArgumentException("Formato de URL P2P inválido: " + url);
    }

    public CompletableFuture<String> resolveP2PToLocalHttp(String p2pUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String hash = extractP2PHash(p2pUrl);
                String apiUrl = String.format("http://%s:%d/ace/getstream?id=%s&format=json", ENGINE_HOST, ENGINE_PORT, hash);
                
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return String.format("http://%s:%d/ace/manifest.m3u8?id=%s", ENGINE_HOST, ENGINE_PORT, hash);
                } else {
                    throw new RuntimeException("Falha ao comunicar com a Engine P2P. Status: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[P2P_MANAGER] Erro ao resolver stream: " + e.getMessage());
                return null;
            }
        });
    }
}