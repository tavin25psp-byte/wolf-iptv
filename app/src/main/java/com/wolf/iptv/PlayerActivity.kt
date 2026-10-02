package com.wolf.iptv

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 4000

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

        player?.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {

                when (playbackState) {

                    Player.STATE_BUFFERING -> {
                        // Carregando
                    }

                    Player.STATE_READY -> {
                        // Pronto para reproduzir
                    }

                    Player.STATE_ENDED -> {
                        // Terminou
                    }

                    Player.STATE_IDLE -> {
                        // Parado
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Erro de reprodução
            }
        })

        val mediaItem = MediaItem.fromUri(Uri.parse(url))

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (event.action == KeyEvent.ACTION_DOWN) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    player?.let {
                        if (it.isPlaying) {
                            it.pause()
                        } else {
                            it.play()
                        }
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    player?.let {
                        it.seekTo(it.currentPosition + 10_000)
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    player?.let {
                        it.seekTo(
                            (it.currentPosition - 10_000)
                                .coerceAtLeast(0)
                        )
                    }

                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    finish()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null

        player?.release()
        player = null

        super.onDestroy()
    }
}
