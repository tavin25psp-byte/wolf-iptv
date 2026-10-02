package com.wolf.iptv

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerView = PlayerView(this)
        playerView.useController = true

        setContentView(playerView)

        iniciarPlayer()
    }

    private fun iniciarPlayer() {

        val url = intent.getStringExtra("VIDEO_URL")

        if (url.isNullOrBlank()) {
            finish()
            return
        }

        player = ExoPlayer.Builder(this).build()

        playerView.player = player

        val mediaItem = MediaItem.fromUri(Uri.parse(url))

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onStop() {
        super.onStop()

        player?.release()
        player = null
    }
}
