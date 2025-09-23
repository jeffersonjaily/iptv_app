package com.seuprojeto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ChannelListActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

    private static final String TAG = "ChannelListActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ChannelAdapter channelAdapter;
    private List<Channel> channels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list); // Garanta que activity_channel_list.xml existe

        recyclerView = findViewById(R.id.channelRecyclerView); // Garanta que o RecyclerView tem este ID
        progressBar = findViewById(R.id.progressBar); // Garanta que o ProgressBar tem este ID

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        channelAdapter = new ChannelAdapter(channels, this);
        recyclerView.setAdapter(channelAdapter);

        Intent intent = getIntent();
        String playlistUrl = intent.getStringExtra("playlist_url");
        String playlistName = intent.getStringExtra("playlist_full_name");

        setTitle(playlistName); // Define o título da Activity com o nome da playlist

        if (playlistUrl != null && !playlistUrl.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    List<Channel> fetchedChannels = downloadM3uContent(playlistUrl);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (fetchedChannels != null && !fetchedChannels.isEmpty()) {
                            channels.clear();
                            channels.addAll(fetchedChannels);
                            channelAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Canais carregados: " + channels.size());
                        } else {
                            Toast.makeText(ChannelListActivity.this, "Nenhum canal encontrado na playlist.", Toast.LENGTH_LONG).show();
                            Log.w(TAG, "Nenhum canal encontrado na playlist para URL: " + playlistUrl);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao carregar conteúdo M3U: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ChannelListActivity.this, "Erro ao carregar canais: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } else {
            Toast.makeText(this, "URL da playlist não fornecida.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "URL da playlist é nula ou vazia.");
        }
    }

    private List<Channel> downloadM3uContent(String m3uUrl) throws Exception {
        List<Channel> fetchedChannels = new ArrayList<>();
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(m3uUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                String currentChannelName = null;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#EXTINF:")) {
                        // Extrai o nome do canal. A regex pode ser ajustada para maior precisão
                        int commaIndex = line.indexOf(",");
                        if (commaIndex != -1) {
                            currentChannelName = line.substring(commaIndex + 1).trim();
                        } else {
                            currentChannelName = "Canal Desconhecido";
                        }
                    } else if (currentChannelName != null && !line.trim().isEmpty() && (line.startsWith("http://") || line.startsWith("https://"))) {
                        // Se encontramos uma URL e já temos um nome, adicionamos o canal
                        fetchedChannels.add(new Channel(currentChannelName, line.trim()));
                        currentChannelName = null; // Reseta para o próximo canal
                    }
                }
            } else {
                throw new Exception("Falha na conexão: Código de resposta " + responseCode);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao fechar o reader: " + e.getMessage(), e);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return fetchedChannels;
    }

    @Override
    public void onChannelClick(Channel channel) {
        Log.d(TAG, "Canal clicado: " + channel.getName() + " - URL: " + channel.getUrl());
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_url", channel.getUrl());
        intent.putExtra("channel_name", channel.getName());
        startActivity(intent);
    }
}