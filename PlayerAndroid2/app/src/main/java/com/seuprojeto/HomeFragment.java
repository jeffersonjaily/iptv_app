package com.seuprojeto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeFragment extends Fragment implements ChannelAdapter.OnChannelClickListener {

    private static final String TAG = "HomeFragment";
    private static final String PLAYLIST_URL = "https://raw.githubusercontent.com/jeffersonjaily/iptv_app/main/PlayerAndroid2/app/src/main/assets/LISTA_IPTV.TXT";

    private RecyclerView recyclerView;
    private ChannelAdapter channelAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla o layout para este fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.homeRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Carrega a lista da URL em uma thread separada
        new Thread(() -> {
            try {
                List<Channel> playlists = fetchPlaylistsFromUrl(PLAYLIST_URL);

                if (getActivity() != null && playlists != null && !playlists.isEmpty()) {
                    Log.d(TAG, "Playlists carregadas: " + playlists.size());
                    // AGORA SIM: Atualiza a UI com os dados carregados
                    getActivity().runOnUiThread(() -> {
                        channelAdapter = new ChannelAdapter(playlists, this);
                        recyclerView.setAdapter(channelAdapter);
                    });
                } else {
                    Log.w(TAG, "A lista de playlists da URL está vazia.");
                }

            } catch (IOException e) {
                Log.e(TAG, "Erro de I/O ao carregar lista: " + e.getMessage(), e);
            }
        }).start();
    }

    private List<Channel> fetchPlaylistsFromUrl(String urlString) throws IOException {
        // O método de busca que estava na MainActivity agora está aqui
        List<Channel> playlists = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;

            Pattern pattern = Pattern.compile("Link M3U: (https?://\\S+)");
            Pattern namePattern = Pattern.compile("Servidor ➤\\s*(http[s]?://[^:]+:[^\\s]+)");
            String currentName = null;

            while ((line = reader.readLine()) != null) {
                Matcher nameMatcher = namePattern.matcher(line);
                if (nameMatcher.find()) {
                    currentName = nameMatcher.group(1);
                    if (currentName.contains(":")) {
                        currentName = currentName.substring(0, currentName.indexOf(":"));
                    }
                    currentName = currentName.replace("http://", "").replace("https://", "").trim();
                    currentName = "Playlist - " + currentName;
                }

                Matcher urlMatcher = pattern.matcher(line);
                if (urlMatcher.find()) {
                    String m3uUrl = urlMatcher.group(1).trim();
                    if (currentName != null) {
                        playlists.add(new Channel(currentName, m3uUrl));
                        currentName = null;
                    }
                }
            }
        } finally {
            if (reader != null) reader.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return playlists;
    }

    // O clique agora é tratado aqui, no Fragment
    @Override
    public void onChannelClick(Channel playlist) {
        Log.d(TAG, "Playlist clicada: " + playlist.getName() + " - URL: " + playlist.getUrl());
        Intent intent = new Intent(getActivity(), ChannelListActivity.class);
        intent.putExtra("playlist_url", playlist.getUrl());
        intent.putExtra("playlist_name", playlist.getName());
        startActivity(intent);
    }
}