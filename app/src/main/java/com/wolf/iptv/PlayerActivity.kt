package com.wolf.iptv

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var erroTexto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val container = FrameLayout(this)

        playerView = PlayerView(this)
        playerView.useController = true
        playerView.controllerShowTimeoutMs = 5000

        container.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        erroTexto = TextView(this)
        erroTexto.textSize = 18f
        erroTexto.setTextColor(Color.WHITE)
        erroTexto.setBackgroundColor(Color.BLACK)
        erroTexto.gravity = Gravity.CENTER
        erroTexto.setPadding(40, 40, 40, 40)
        erroTexto.visibility = View.GONE

        container.addView(
            erroTexto,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(container)

        iniciarPlayer()
    }

    private fun iniciarPlayer() {

        val url = intent.getStringExtra("VIDEO_URL")

        if (url.isNullOrBlank()) {
            mostrarErro("Nenhuma URL de vídeo foi recebida.")
            return
        }

        try {

            player = ExoPlayer.Builder(this).build()

            playerView.player = player

            player?.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(state: Int) {

                    when (state) {

                        Player.STATE_BUFFERING -> {
                            erroTexto.visibility = View.GONE
                        }

                        Player.STATE_READY -> {
                            erroTexto.visibility = View.GONE
                        }

                        Player.STATE_ENDED -> {
                            // Vídeo terminou
                        }

                        Player.STATE_IDLE -> {
                            // Player parado
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {

                    mostrarErro(
                        "Erro ao reproduzir o vídeo:\n\n" +
                        error.errorCodeName
                    )
                }
            })

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()

            player?.setMediaItem(mediaItem)

            player?.prepare()

            player?.playWhenReady = true

        } catch (e: Exception) {

            mostrarErro(
                "Erro ao abrir o player:\n\n${e.message}"
            )
        }
    }

    private fun mostrarErro(mensagem: String) {

        erroTexto.text = mensagem
        erroTexto.visibility = View.VISIBLE
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
