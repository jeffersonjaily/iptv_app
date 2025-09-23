// src/main/java/com/seuprojeto/playeriptv/PlayerActivity.kt
package com.seuprojeto.playeriptv

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: StyledPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        // Obter a URL do canal passada da MainActivity
        val url = intent.getStringExtra("url_do_canal")

        if (url != null) {
            inicializarPlayer(url)
        }
    }

    private fun inicializarPlayer(url: String) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
    }

    override fun onStop() {
        super.onStop()
        // Liberar o player quando a atividade não estiver mais visível
        player.release()
    }
}