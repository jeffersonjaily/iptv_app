package com.seuprojeto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
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

public class PlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private RecyclerView rvChannelsOverlay;

    private ArrayList<Channel> channelList;
    private int startPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        channelList = (ArrayList<Channel>) getIntent().getSerializableExtra("channel_list");
        startPosition = getIntent().getIntExtra("start_position", 0);

        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressBar);
        rvChannelsOverlay = findViewById(R.id.rvChannelsOverlay);

        hideSystemUI();

        // --- MUDANÇA: Removemos o ControllerVisibilityListener ---
    }
    
    // NOVO MÉTODO: Controla a visibilidade da lista de canais
    private void toggleChannelOverlay() {
        if (rvChannelsOverlay.getVisibility() == View.VISIBLE) {
            rvChannelsOverlay.setVisibility(View.GONE);
            playerView.hideController(); // Opcional: esconde os controles junto
        } else {
            rvChannelsOverlay.setVisibility(View.VISIBLE);
            // Centraliza a lista no canal que está tocando
            rvChannelsOverlay.scrollToPosition(player.getCurrentMediaItemIndex());
        }
    }

    // NOVO MÉTODO: Captura os cliques do controle remoto
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Se o usuário apertar "OK" (DPAD_CENTER) ou "CIMA"/"BAIXO"
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    toggleChannelOverlay();
                    return true; // Evento consumido, não passa adiante
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    // Se a lista estiver visível, permite navegar nela
                    if (rvChannelsOverlay.getVisibility() == View.VISIBLE) {
                        return rvChannelsOverlay.dispatchKeyEvent(event);
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    // ... (O resto da sua classe onStart, onStop, etc., continua igual, com as devidas importações)
    @Override
    protected void onStart() {
        super.onStart();
        if (channelList != null && !channelList.isEmpty()) {
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

            List<MediaItem> mediaItems = new ArrayList<>();
            for (Channel channel : channelList) {
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
        rvChannelsOverlay.setLayoutManager(new LinearLayoutManager(this));
        PlaylistAdapter overlayAdapter = new PlaylistAdapter(channelList, clickedChannel -> {
            int clickedIndex = channelList.indexOf(clickedChannel);
            if (clickedIndex != -1) {
                player.seekTo(clickedIndex, 0);
                // Esconde a lista após selecionar um novo canal
                rvChannelsOverlay.setVisibility(View.GONE);
            }
        });
        rvChannelsOverlay.setAdapter(overlayAdapter);
        rvChannelsOverlay.scrollToPosition(startPosition);
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