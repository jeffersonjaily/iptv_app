package com.seuprojeto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

    private static final String TAG = "MainActivity";
    private static final String FILE_NAME = "LISTA_IPTV.TXT"; // Alterei o nome do arquivo para .txt

    private RecyclerView recyclerView;
    private ChannelAdapter channelAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Garanta que activity_main.xml existe

        recyclerView = findViewById(R.id.recyclerView); // Garanta que o RecyclerView tem este ID em activity_main.xml
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Carrega o arquivo de texto em uma thread separada para não travar a UI
        new Thread(() -> {
            try {
                List<Channel> playlists = parseServerListFromAssets();
                
                if (playlists != null && !playlists.isEmpty()) {
                    Log.d(TAG, "Listas de servidores carregadas: " + playlists.size());
                    // Atualiza a UI na thread principal
                    runOnUiThread(() -> {
                        channelAdapter = new ChannelAdapter(playlists, this);
                        recyclerView.setAdapter(channelAdapter);
                    });
                } else {
                    Log.w(TAG, "A lista de playlists está vazia.");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "A lista de playlists está vazia.", Toast.LENGTH_LONG).show());
                }

            } catch (IOException e) {
                Log.e(TAG, "Erro de I/O ao carregar lista de servidores: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao carregar lista de servidores.", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e(TAG, "Erro inesperado ao carregar servidores: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Novo método para parsear o arquivo de texto e extrair as listas
    private List<Channel> parseServerListFromAssets() throws IOException {
        List<Channel> playlists = new ArrayList<>();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = getAssets().open(FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            
            // Regex para encontrar o "Link M3U" e um nome para a playlist
            Pattern pattern = Pattern.compile("Link M3U: (https?://\\S+)");
            Pattern namePattern = Pattern.compile("Servidor ➤\\s*(http[s]?://[^:]+:[^\\s]+)");

            String currentName = null;

            while ((line = reader.readLine()) != null) {
                // Tenta encontrar o nome do servidor primeiro
                Matcher nameMatcher = namePattern.matcher(line);
                if (nameMatcher.find()) {
                    currentName = nameMatcher.group(1);
                    // Como o nome pode ser longo, vamos simplificá-lo
                    if (currentName.contains(":")) {
                       currentName = currentName.substring(0, currentName.indexOf(":"));
                    }
                    currentName = currentName.replace("http://", "").replace("https://", "").trim();
                    currentName = "Playlist - " + currentName;
                }

                // Tenta encontrar o link M3U
                Matcher urlMatcher = pattern.matcher(line);
                if (urlMatcher.find()) {
                    String url = urlMatcher.group(1).trim();
                    if (currentName != null) {
                        playlists.add(new Channel(currentName, url));
                        currentName = null; // Reseta para a próxima playlist
                    }
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar reader", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao fechar InputStream", e);
                }
            }
        }
        return playlists;
    }

    // Este método é chamado quando o usuário clica em uma lista na tela principal
    @Override
    public void onChannelClick(Channel playlist) {
        Log.d(TAG, "Playlist clicada: " + playlist.getName() + " - URL: " + playlist.getUrl());
        Intent intent = new Intent(this, ChannelListActivity.class);
        intent.putExtra("playlist_url", playlist.getUrl());
        intent.putExtra("playlist_name", playlist.getName());
        startActivity(intent);
    }
}