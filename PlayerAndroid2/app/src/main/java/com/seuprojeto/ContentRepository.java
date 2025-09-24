package com.seuprojeto;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentRepository {
    private static final String TAG = "ContentRepository";
    private static ContentRepository instance;
    private List<Channel> allChannels = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isLoaded = false;

    // Singleton Pattern
    public static synchronized ContentRepository getInstance() {
        if (instance == null) {
            instance = new ContentRepository();
        }
        return instance;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    // --- MÉTODO ATUALIZADO COM MELHOR LOG DE ERROS ---
    public void loadAllContent(String playlistIndexUrl, Runnable onComplete) {
        if (isLoading || isLoaded) {
            if (onComplete != null) {
                // Se já estiver carregado, apenas executa o callback
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
            return;
        }
        isLoading = true;

        new Thread(() -> {
            int successCount = 0;
            int failureCount = 0;
            try {
                List<Channel> playlists = fetchPlaylistsFromUrl(playlistIndexUrl);
                allChannels.clear();

                for (Channel playlist : playlists) {
                    try {
                        List<Channel> channelsFromM3u = downloadM3uContent(playlist.getUrl());
                        if (channelsFromM3u != null && !channelsFromM3u.isEmpty()) {
                            allChannels.addAll(channelsFromM3u);
                            successCount++;
                        } else {
                            failureCount++;
                            Log.w(TAG, "Playlist carregada, mas retornou vazia: " + playlist.getName());
                        }
                    } catch (Exception e) {
                        failureCount++;
                        Log.e(TAG, "Falha ao carregar o conteúdo da playlist: " + playlist.getName(), e);
                    }
                }
                isLoaded = true;
                Log.d(TAG, "Carregamento de conteúdo completo. Playlists com sucesso: " + successCount + ". Falhas: " + failureCount + ". Total de canais: " + allChannels.size());

            } catch (IOException e) {
                Log.e(TAG, "Erro fatal ao carregar o índice de playlists: " + e.getMessage(), e);
            } finally {
                isLoading = false;
                if (onComplete != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
                }
            }
        }).start();
    }

    // --- MÉTODOS DE FILTRO ---
    public List<Channel> getLiveTvChannels() {
        return allChannels.stream()
                .filter(channel -> {
                    String group = channel.getGroup() != null ? channel.getGroup().toLowerCase() : "";
                    return !group.contains("filme") && !group.contains("série") && !group.contains("series");
                })
                .collect(Collectors.toList());
    }

    public List<Channel> getMovieChannels() {
        return allChannels.stream()
                .filter(channel -> channel.getGroup() != null && channel.getGroup().toLowerCase().contains("filme"))
                .collect(Collectors.toList());
    }

    public List<Channel> getSeriesChannels() {
        return allChannels.stream()
                .filter(channel -> channel.getGroup() != null && (channel.getGroup().toLowerCase().contains("série") || channel.getGroup().toLowerCase().contains("series")))
                .collect(Collectors.toList());
    }

    // --- MÉTODOS DE PARSING ---
    private List<Channel> fetchPlaylistsFromUrl(String urlString) throws IOException {
        List<Channel> playlists = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            String currentName = null;
            String currentUrl = null;
            Pattern namePattern = Pattern.compile("^Nome:\\s*(.*)");
            Pattern urlPattern = Pattern.compile("^Link M3U:\\s*(.*)");
            while ((line = reader.readLine()) != null) {
                Matcher nameMatcher = namePattern.matcher(line);
                if (nameMatcher.matches()) currentName = nameMatcher.group(1).trim();
                Matcher urlMatcher = urlPattern.matcher(line);
                if (urlMatcher.matches()) currentUrl = urlMatcher.group(1).trim();
                if (currentName != null && currentUrl != null) {
                    playlists.add(new Channel(currentName, currentUrl));
                    currentName = null;
                    currentUrl = null;
                }
            }
        } finally {
            if (reader != null) reader.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return playlists;
    }

    private List<Channel> downloadM3uContent(String m3uUrl) throws Exception {
        List<Channel> fetchedChannels = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(m3uUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "IPTV Player/1.0");
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            String currentChannelName = null, currentLogoUrl = null, currentGroup = null;
            Pattern extinfPattern = Pattern.compile("#EXTINF:-1(?:\\s+tvg-id=\"(.*?)\")?(?:\\s+tvg-logo=\"(.*?)\")?(?:\\s+group-title=\"(.*?)\")?,(.*)");
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXTINF:")) {
                    Matcher matcher = extinfPattern.matcher(line);
                    if (matcher.matches()) {
                        currentLogoUrl = matcher.group(2);
                        currentGroup = matcher.group(3);
                        currentChannelName = matcher.group(4).trim();
                    }
                } else if (currentChannelName != null && !line.trim().isEmpty() && (line.startsWith("http"))) {
                    fetchedChannels.add(new Channel(currentChannelName, line.trim(), currentLogoUrl, currentGroup));
                    currentChannelName = null;
                    currentLogoUrl = null;
                    currentGroup = null;
                }
            }
        } finally {
            if (reader != null) reader.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return fetchedChannels;
    }
}