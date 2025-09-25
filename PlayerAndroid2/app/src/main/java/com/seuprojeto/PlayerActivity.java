package com.seuprojeto;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerActivity extends AppCompatActivity implements PlayerOvelayListener, PlayerCategoryAdapter.OnCategoryClickListener {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private RecyclerView rvCategoriesOverlay;
    private RecyclerView rvChannelsOverlay;
    private View overlayPanel;
    private PlayerCategoryAdapter categoryAdapter;
    private PlaylistAdapter channelAdapter;

    private ArrayList<Channel> allChannels;
    private List<String> categoryNames;
    private int startPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        allChannels = (ArrayList<Channel>) getIntent().getSerializableExtra("channel_list");
        startPosition = getIntent().getIntExtra("start_position", 0);

        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressBar);
        overlayPanel = findViewById(R.id.overlay_panel);
        rvCategoriesOverlay = findViewById(R.id.rvCategoriesOverlay);
        rvChannelsOverlay = findViewById(R.id.rvChannelsOverlay);

        hideSystemUI();
    }

    @Override
    public void onToggleOverlay() {
        if (overlayPanel.getVisibility() == View.VISIBLE) {
            overlayPanel.setVisibility(View.GONE);
        } else {
            overlayPanel.setVisibility(View.VISIBLE);
            updateOverlayLists();
        }
    }

    @Override
    public void onCategoryClick(String category, int position) {
        categoryAdapter.setSelectedPosition(position);
        List<Channel> filteredChannels;

        if (category.equalsIgnoreCase("TODOS")) {
            filteredChannels = new ArrayList<>(allChannels);
        } else {
            filteredChannels = allChannels.stream()
                .filter(c -> category.equals(c.getGroup()))
                .collect(Collectors.toList());
        }

        channelAdapter = new PlaylistAdapter(filteredChannels, clickedChannel -> {
            int globalIndex = allChannels.indexOf(clickedChannel);
            if (globalIndex != -1) {
                player.seekTo(globalIndex, 0);
                overlayPanel.setVisibility(View.GONE);
            }
        });
        rvChannelsOverlay.setAdapter(channelAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (allChannels != null && !allChannels.isEmpty()) {
            initializePlayer();
            setupChannelOverlay();
        } else {
            Toast.makeText(this, "Lista de canais não fornecida.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            CustomPlayerControlView controlView = playerView.findViewById(R.id.custom_controls);
            if (controlView != null) {
                controlView.setOverlayListener(this);
            }

            List<MediaItem> mediaItems = new ArrayList<>();
            for (Channel channel : allChannels) {
                mediaItems.add(MediaItem.fromUri(Uri.parse(channel.getUrl())));
            }
            player.setMediaItems(mediaItems);
            player.seekTo(startPosition, 0);
            player.prepare();
            player.play();
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    progressBar.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                }
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                     Toast.makeText(PlayerActivity.this, "Erro ao reproduzir o canal.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setupChannelOverlay() {
        categoryNames = allChannels.stream()
            .map(c -> c.getGroup() == null ? "Outros" : c.getGroup())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        categoryNames.add(0, "TODOS");

        rvCategoriesOverlay.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new PlayerCategoryAdapter(categoryNames, this);
        rvCategoriesOverlay.setAdapter(categoryAdapter);
        
        rvChannelsOverlay.setLayoutManager(new LinearLayoutManager(this));
    }

    private void updateOverlayLists() {
        if (player == null) return;
        
        int currentChannelIndex = player.getCurrentMediaItemIndex();
        Channel currentChannel = allChannels.get(currentChannelIndex);
        String currentGroup = currentChannel.getGroup() == null ? "Outros" : currentChannel.getGroup();
        int categoryPosition = categoryNames.indexOf(currentGroup);

        if(categoryPosition != -1) {
            rvCategoriesOverlay.scrollToPosition(categoryPosition);
            onCategoryClick(currentGroup, categoryPosition);
        } else {
            rvCategoriesOverlay.scrollToPosition(0);
            onCategoryClick("TODOS", 0);
        }
        
        rvChannelsOverlay.scrollToPosition(allChannels.indexOf(currentChannel));
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}