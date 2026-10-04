package com.wolf.iptv

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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
    private lateinit var container: FrameLayout
    private lateinit var erroTexto: TextView
    private lateinit var engrenagem: TextView
    private lateinit var menuZoom: TextView

    private var menuAberto = false
    private var zoomAtual = 0

    private val zooms = floatArrayOf(
        1.0f,
        1.1f,
        1.25f,
        1.5f,
        2.0f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        container = FrameLayout(this)

        // ==============================
        // PLAYER
        // ==============================

        playerView = PlayerView(this)
        playerView.useController = true
        playerView.controllerShowTimeoutMs = 5000

        val playerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        container.addView(playerView, playerParams)

        // ==============================
        // ERRO
        // ==============================

        erroTexto = TextView(this)
        erroTexto.textSize = 18f
        erroTexto.setTextColor(Color.WHITE)
        erroTexto.setBackgroundColor(Color.BLACK)
        erroTexto.gravity = Gravity.CENTER
        erroTexto.setPadding(40, 40, 40, 40)
        erroTexto.visibility = View.GONE

        val erroParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        container.addView(erroTexto, erroParams)

        // ==============================
        // ENGRENAGEM
        // ==============================

        engrenagem = TextView(this)

        engrenagem.text = "⚙"
        engrenagem.textSize = 28f
        engrenagem.setTextColor(Color.WHITE)
        engrenagem.gravity = Gravity.CENTER
        engrenagem.setBackgroundColor(Color.argb(150, 0, 0, 0))

        engrenagem.isFocusable = true
        engrenagem.isClickable = true

        engrenagem.setPadding(12, 8, 12, 8)

        val engrenagemParams = FrameLayout.LayoutParams(
            65,
            65
        )

        engrenagemParams.gravity =
            Gravity.TOP or Gravity.END

        engrenagemParams.setMargins(
            0,
            25,
            25,
            0
        )

        container.addView(
            engrenagem,
            engrenagemParams
        )

        engrenagem.setOnClickListener {
            abrirMenuZoom()
        }

        // ==============================
        // MENU DE ZOOM
        // ==============================

        menuZoom = TextView(this)

        menuZoom.textSize = 18f
        menuZoom.setTextColor(Color.WHITE)
        menuZoom.setBackgroundColor(
            Color.argb(235, 20, 20, 20)
        )

        menuZoom.setPadding(
            30,
            25,
            30,
            25
        )

        menuZoom.gravity = Gravity.CENTER_VERTICAL

        menuZoom.visibility = View.GONE

        menuZoom.isFocusable = true

        val menuParams = FrameLayout.LayoutParams(
            330,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        menuParams.gravity =
            Gravity.TOP or Gravity.END

        menuParams.setMargins(
            0,
            105,
            25,
            0
        )

        container.addView(
            menuZoom,
            menuParams
        )

        setContentView(container)

        iniciarPlayer()
    }

    // ==============================
    // PLAYER
    // ==============================

    private fun iniciarPlayer() {

        val url = intent.getStringExtra("VIDEO_URL")

        if (url.isNullOrBlank()) {
            mostrarErro(
                "Nenhuma URL de vídeo foi recebida."
            )
            return
        }

        try {

            player = ExoPlayer.Builder(this).build()

            playerView.player = player

            player?.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        state: Int
                    ) {

                        when (state) {

                            Player.STATE_BUFFERING -> {
                                erroTexto.visibility =
                                    View.GONE
                            }

                            Player.STATE_READY -> {
                                erroTexto.visibility =
                                    View.GONE
                            }

                            Player.STATE_ENDED -> {
                            }

                            Player.STATE_IDLE -> {
                            }
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        mostrarErro(
                            "Erro ao reproduzir o vídeo:\n\n" +
                            error.errorCodeName
                        )
                    }
                }
            )

            val uri = Uri.parse(url)

            val builder = MediaItem.Builder()
                .setUri(uri)

            if (
                url.contains(
                    ".m3u8",
                    ignoreCase = true
                )
            ) {

                builder.setMimeType(
                    MimeTypes.APPLICATION_M3U8
                )
            }

            val mediaItem = builder.build()

            player?.setMediaItem(mediaItem)

            player?.prepare()

            player?.playWhenReady = true

        } catch (e: Exception) {

            mostrarErro(
                "Erro ao abrir o player:\n\n${e.message}"
            )
        }
    }

    // ==============================
    // ABRIR MENU
    // ==============================

    private fun abrirMenuZoom() {

        menuAberto = true

        atualizarMenuZoom()

        menuZoom.visibility =
            View.VISIBLE

        menuZoom.requestFocus()
    }

    // ==============================
    // FECHAR MENU
    // ==============================

    private fun fecharMenuZoom() {

        menuAberto = false

        menuZoom.visibility =
            View.GONE

        engrenagem.requestFocus()
    }

    // ==============================
    // MENU
    // ==============================

    private fun atualizarMenuZoom() {

        val nomes = arrayOf(
            "Normal",
            "1.1×",
            "1.25×",
            "1.5×",
            "2.0×"
        )

        val texto = StringBuilder()

        texto.append("⚙  ZOOM\n\n")

        for (i in nomes.indices) {

            if (i == zoomAtual) {
                texto.append("▶ ")
            } else {
                texto.append("   ")
            }

            texto.append(nomes[i])
            texto.append("\n")
        }

        texto.append("\n")
        texto.append("▲▼ Escolher   OK Aplicar")

        menuZoom.text =
            texto.toString()
    }

    // ==============================
    // APLICAR ZOOM
    // ==============================

    private fun aplicarZoom() {

        val valor =
            zooms[zoomAtual]

        val videoView =
            playerView.videoSurfaceView

        if (videoView != null) {

            videoView.pivotX =
                videoView.width / 2f

            videoView.pivotY =
                videoView.height / 2f

            videoView.scaleX =
                valor

            videoView.scaleY =
                valor
        }
    }

    // ==============================
    // ERRO
    // ==============================

    private fun mostrarErro(
        mensagem: String
    ) {

        erroTexto.text =
            mensagem

        erroTexto.visibility =
            View.VISIBLE
    }

    // ==============================
    // D-PAD
    // ==============================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action == KeyEvent.ACTION_UP) {

            // ==========================
            // MENU DE ZOOM ABERTO
            // ==========================

            if (menuAberto) {

                when (event.keyCode) {

                    KeyEvent.KEYCODE_DPAD_UP -> {

                        zoomAtual--

                        if (zoomAtual < 0) {
                            zoomAtual =
                                zooms.size - 1
                        }

                        atualizarMenuZoom()

                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {

                        zoomAtual++

                        if (
                            zoomAtual >=
                            zooms.size
                        ) {
                            zoomAtual = 0
                        }

                        atualizarMenuZoom()

                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {

                        aplicarZoom()

                        fecharMenuZoom()

                        return true
                    }

                    KeyEvent.KEYCODE_BACK -> {

                        fecharMenuZoom()

                        return true
                    }
                }

                return true
            }

            // ==========================
            // D-PAD NORMAL
            // ==========================

            when (event.keyCode) {

                // Abrir engrenagem
                KeyEvent.KEYCODE_DPAD_UP -> {

                    engrenagem.requestFocus()

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (
                        engrenagem.hasFocus()
                    ) {

                        abrirMenuZoom()

                        return true
                    }

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

                        it.seekTo(
                            it.currentPosition +
                                    10_000
                        )
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    player?.let {

                        it.seekTo(
                            (
                                it.currentPosition -
                                        10_000
                            ).coerceAtLeast(0)
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

    // ==============================
    // PAUSE
    // ==============================

    override fun onPause() {

        super.onPause()

        // Não pausar o player aqui.
    }

    // ==============================
    // DESTROY
    // ==============================

    override fun onDestroy() {

        playerView.player = null

        player?.release()

        player = null

        super.onDestroy()
    }
}
