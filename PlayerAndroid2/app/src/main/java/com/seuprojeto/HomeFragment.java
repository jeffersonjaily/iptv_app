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

public class HomeFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener {

    private static final String TAG = "HomeFragment";
    private static final String PLAYLIST_URL = "https://raw.githubusercontent.com/jeffersonjaily/iptv_app/main/PlayerAndroid2/app/src/main/assets/LISTA_IPTV.TXT";

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.homeRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fetchPlaylists();
    }

    private void fetchPlaylists() {
        new Thread(() -> {
            try {
                List<Channel> playlists = fetchPlaylistsFromUrl(PLAYLIST_URL);
                if (getActivity() == null) return;

                if (!playlists.isEmpty()) {
                    Log.d(TAG, "Playlists carregadas com sucesso: " + playlists.size());
                    getActivity().runOnUiThread(() -> {
                        PlaylistAdapter playlistAdapter = new PlaylistAdapter(playlists, this);
                        recyclerView.setAdapter(playlistAdapter);
                    });
                } else {
                    Log.w(TAG, "Nenhuma playlist encontrada no servidor (verifique o formato do arquivo).");
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Nenhuma playlist encontrada no servidor.", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                Log.e(TAG, "Erro de rede ao carregar playlists: " + e.getMessage(), e);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Erro de rede. Verifique sua conexão.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private List<Channel> fetchPlaylistsFromUrl(String urlString) throws IOException {
        List<Channel> playlists = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "IPTV Player/1.0");
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String line;
            String currentName = null;
            String currentUrl = null;

            Pattern namePattern = Pattern.compile("^Nome:\\s*(.*)");
            Pattern urlPattern = Pattern.compile("^Link M3U:\\s*(.*)");

            while ((line = reader.readLine()) != null) {
                Matcher nameMatcher = namePattern.matcher(line);
                if (nameMatcher.matches()) {
                    currentName = nameMatcher.group(1).trim();
                }

                Matcher urlMatcher = urlPattern.matcher(line);
                if (urlMatcher.matches()) {
                    currentUrl = urlMatcher.group(1).trim();
                }

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

    @Override
    public void onPlaylistClick(Channel playlist) {
        if (getActivity() == null) return;
        
        Intent intent = new Intent(getActivity(), ChannelListActivity.class);
        intent.putExtra("playlist_url", playlist.getUrl());
        intent.putExtra("playlist_name", playlist.getName());
        startActivity(intent);
    }
}