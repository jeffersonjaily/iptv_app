package com.seuprojeto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelListActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

    private static final String TAG = "ChannelListActivity";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private CategoryAdapter categoryAdapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);

        // Configura a Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializa as Views
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.channelRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Pega os dados passados da tela anterior
        Intent intent = getIntent();
        String playlistUrl = intent.getStringExtra("playlist_url");
        String playlistName = intent.getStringExtra("playlist_name");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(playlistName);
        }

        // Inicia o carregamento dos canais
        loadChannels(playlistUrl);
    }

    /**
     * Inicia uma thread para baixar o conteúdo da playlist da URL fornecida.
     */
    private void loadChannels(String playlistUrl) {
        if (playlistUrl != null && !playlistUrl.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    List<Channel> fetchedChannels = downloadM3uContent(playlistUrl);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (fetchedChannels != null && !fetchedChannels.isEmpty()) {
                            processAndDisplayChannels(fetchedChannels);
                        } else {
                            Toast.makeText(this, "Nenhum canal encontrado na playlist.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao carregar M3U: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Erro ao carregar canais.", Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }
    }

    /**
     * Agrupa os canais por categoria e os exibe na tela.
     */
    private void processAndDisplayChannels(List<Channel> fetchedChannels) {
        Map<String, List<Channel>> channelsByGroup = new LinkedHashMap<>();
        for (Channel channel : fetchedChannels) {
            String group = channel.getGroup();
            if (group == null || group.trim().isEmpty()) {
                group = "Outros";
            }
            // Adiciona o canal à lista de seu grupo
            channelsByGroup.computeIfAbsent(group, k -> new ArrayList<>()).add(channel);
        }

        List<ChannelCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<Channel>> entry : channelsByGroup.entrySet()) {
            categories.add(new ChannelCategory(entry.getKey(), entry.getValue()));
        }

        categoryAdapter = new CategoryAdapter(categories, this);
        recyclerView.setAdapter(categoryAdapter);
        
        setupTabs(categories);
    }

    /**
     * Configura as abas (Tabs) com as categorias principais dos canais.
     */
    private void setupTabs(List<ChannelCategory> categories) {
        tabLayout.removeOnTabSelectedListener(tabSelectedListener);
        tabLayout.removeAllTabs();

        Set<String> mainCategories = new LinkedHashSet<>();
        for (ChannelCategory category : categories) {
            String title = category.getTitle().trim();
            if (title.contains("|")) {
                mainCategories.add(title.split("\\|")[0].trim());
            } else {
                mainCategories.add(title);
            }
        }

        tabLayout.addTab(tabLayout.newTab().setText("Todos"));
        for (String mainCategory : mainCategories) {
            tabLayout.addTab(tabLayout.newTab().setText(mainCategory));
        }
        tabLayout.addOnTabSelectedListener(tabSelectedListener);
    }

    private final TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (categoryAdapter != null && tab.getText() != null) {
                categoryAdapter.filterByCategory(tab.getText().toString());
            }
        }
        @Override public void onTabUnselected(TabLayout.Tab tab) { }
        @Override public void onTabReselected(TabLayout.Tab tab) { }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (categoryAdapter != null) {
                    categoryAdapter.filter(newText);
                }
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Chamado quando um canal é clicado. Envia a lista de canais da categoria
     * e a posição do canal clicado para a PlayerActivity.
     */
    @Override
    public void onChannelClick(Channel channel) {
        ArrayList<Channel> channelListInCategory = new ArrayList<>();
        if (categoryAdapter != null) {
            for (ChannelCategory category : categoryAdapter.getCategoryList()) {
                if (category.getChannels().contains(channel)) {
                    channelListInCategory.addAll(category.getChannels());
                    break;
                }
            }
        }

        int clickedIndex = channelListInCategory.indexOf(channel);
        if (!channelListInCategory.isEmpty() && clickedIndex != -1) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("channel_list", channelListInCategory);
            intent.putExtra("start_position", clickedIndex);
            startActivity(intent);
        } else {
            // Fallback: se não encontrar a categoria, envia apenas o canal clicado
            ArrayList<Channel> singleChannelList = new ArrayList<>();
            singleChannelList.add(channel);
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("channel_list", singleChannelList);
            intent.putExtra("start_position", 0);
            startActivity(intent);
        }
    }

    /**
     * Faz o parsing do arquivo M3U para extrair os canais.
     */
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
                } else if (currentChannelName != null && !line.trim().isEmpty() && line.startsWith("http")) {
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