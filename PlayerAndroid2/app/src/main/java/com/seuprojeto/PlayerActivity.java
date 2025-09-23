package com.seuprojeto;

import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private String channelUrl;
    private String channelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressBar);

        // Recebe os dados passados pela Intent
        channelUrl = getIntent().getStringExtra("channel_url");
        channelName = getIntent().getStringExtra("channel_name");

        if (channelUrl == null || channelUrl.isEmpty()) {
            Toast.makeText(this, "URL do canal não fornecida.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setTitle(channelName != null ? channelName : "Reproduzindo Canal");
        Log.d(TAG, "Reproduzindo: " + channelName + " - URL: " + channelUrl);

        // (Opcional) Ativar fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
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

            try {
                Uri uri = Uri.parse(channelUrl);
                MediaItem mediaItem = MediaItem.fromUri(uri);
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
            } catch (Exception e) {
                Log.e(TAG, "URL inválida: " + channelUrl, e);
                Toast.makeText(this, "URL inválida.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    switch (state) {
                        case Player.STATE_BUFFERING:
                            progressBar.setVisibility(View.VISIBLE);
                            break;
                        case Player.STATE_READY:
                        case Player.STATE_ENDED:
                        case Player.STATE_IDLE:
                            progressBar.setVisibility(View.GONE);
                            break;
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Erro do player: " + error.getMessage(), error);
                    Toast.makeText(PlayerActivity.this,
                            "Erro ao reproduzir o canal: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
