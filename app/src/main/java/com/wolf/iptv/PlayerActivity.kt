package com.wolf.iptv

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    private lateinit var container: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var erroTexto: TextView
    private lateinit var engrenagem: TextView
    private lateinit var menuZoom: LinearLayout

    private var menuAberto = false
    private var zoomAtual = 0

    private val zooms = floatArrayOf(
        1.0f,
        1.1f,
        1.25f,
        1.5f,
        2.0f
    )

    private val nomesZoom = arrayOf(
        "Normal",
        "1.1×",
        "1.25×",
        "1.5×",
        "2.0×"
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

        criarInterface()

        iniciarPlayer()
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private fun criarInterface() {

        container = FrameLayout(this)

        // =====================================================
        // PLAYER
        // =====================================================

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 5000

        val playerParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        container.addView(
            playerView,
            playerParams
        )

        // =====================================================
        // TEXTO DE ERRO
        // =====================================================

        erroTexto = TextView(this)

        erroTexto.textSize = 18f
        erroTexto.setTextColor(Color.WHITE)
        erroTexto.setBackgroundColor(Color.BLACK)
        erroTexto.gravity = Gravity.CENTER

        erroTexto.setPadding(
            40,
            40,
            40,
            40
        )

        erroTexto.visibility = View.GONE

        val erroParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        container.addView(
            erroTexto,
            erroParams
        )

        // =====================================================
        // ENGRENAGEM
        // =====================================================

        engrenagem = TextView(this)

        engrenagem.text = "⚙"
        engrenagem.textSize = 28f
        engrenagem.setTextColor(Color.WHITE)

        engrenagem.gravity = Gravity.CENTER

        engrenagem.setBackgroundColor(
            Color.argb(
                170,
                0,
                0,
                0
            )
        )

        engrenagem.setPadding(
            12,
            8,
            12,
            8
        )

        engrenagem.isFocusable = true
        engrenagem.isClickable = true

        val engrenagemParams =
            FrameLayout.LayoutParams(
                70,
                70
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

        // TOQUE NO CELULAR
        engrenagem.setOnClickListener {
            abrirMenuZoom()
        }

        // =====================================================
        // MENU
        // =====================================================

        menuZoom = LinearLayout(this)

        menuZoom.orientation =
            LinearLayout.VERTICAL

        menuZoom.setPadding(
            25,
            25,
            25,
            25
        )

        menuZoom.setBackgroundColor(
            Color.argb(
                235,
                20,
                20,
                20
            )
        )

        menuZoom.visibility =
            View.GONE

        menuZoom.isFocusable = true

        val menuParams =
            FrameLayout.LayoutParams(
                360,
                FrameLayout.LayoutParams.WRAP_CONTENT
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

        criarBotoesZoom()

        setContentView(container)
    }

    // =========================================================
    // BOTÕES DE ZOOM
    // =========================================================

    private fun criarBotoesZoom() {

        menuZoom.removeAllViews()

        val titulo = TextView(this)

        titulo.text = "⚙  ZOOM"
        titulo.textSize = 20f
        titulo.setTextColor(Color.WHITE)

        titulo.gravity =
            Gravity.CENTER

        titulo.setPadding(
            10,
            10,
            10,
            20
        )

        menuZoom.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            )
        )

        for (i in nomesZoom.indices) {

            val botao = Button(this)

            botao.text =
                nomesZoom[i]

            botao.textSize = 17f

            botao.isFocusable = true
            botao.isClickable = true

            botao.setOnClickListener {

                zoomAtual = i

                aplicarZoom()

                fecharMenuZoom()
            }

            menuZoom.addView(
                botao,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    65
                )
            )
        }
    }

    // =========================================================
    // ABRIR MENU
    // =========================================================

    private fun abrirMenuZoom() {

        menuAberto = true

        menuZoom.visibility =
            View.VISIBLE

        // Primeiro botão recebe foco na TV
        if (menuZoom.childCount > 1) {
            menuZoom.getChildAt(1).requestFocus()
        }
    }

    // =========================================================
    // FECHAR MENU
    // =========================================================

    private fun fecharMenuZoom() {

        menuAberto = false

        menuZoom.visibility =
            View.GONE

        engrenagem.requestFocus()
    }

    // =========================================================
    // APLICAR ZOOM
    // =========================================================

    private fun aplicarZoom() {

        val escala =
            zooms[zoomAtual]

        playerView.scaleX =
            escala

        playerView.scaleY =
            escala
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private fun iniciarPlayer() {

        val url =
            intent.getStringExtra(
                "VIDEO_URL"
            )

        if (url.isNullOrBlank()) {

            mostrarErro(
                "Nenhuma URL de vídeo foi recebida."
            )

            return
        }

        try {

            player =
                ExoPlayer.Builder(this)
                    .build()

            playerView.player =
                player

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

            val uri =
                Uri.parse(url)

            val builder =
                MediaItem.Builder()
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

            val mediaItem =
                builder.build()

            player?.setMediaItem(
                mediaItem
            )

            player?.prepare()

            player?.playWhenReady =
                true

        } catch (e: Exception) {

            mostrarErro(
                "Erro ao abrir o player:\n\n" +
                e.message
            )
        }
    }

    // =========================================================
    // ERRO
    // =========================================================

    private fun mostrarErro(
        mensagem: String
    ) {

        erroTexto.text =
            mensagem

        erroTexto.visibility =
            View.VISIBLE
    }

    // =========================================================
    // D-PAD
    // =========================================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event)
        }

        // =====================================================
        // MENU ABERTO
        // =====================================================

        if (menuAberto) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    fecharMenuZoom()

                    return true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {

                    moverFocoZoom(-1)

                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    moverFocoZoom(1)

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco =
                        currentFocus

                    if (
                        foco is Button
                    ) {
                        foco.performClick()
                    }

                    return true
                }
            }

            return true
        }

        // =====================================================
        // PLAYER NORMAL
        // =====================================================

        when (event.keyCode) {

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

        return super.dispatchKeyEvent(event)
    }

    // =========================================================
    // MOVER PELO MENU COM D-PAD
    // =========================================================

    private fun moverFocoZoom(
        direcao: Int
    ) {

        val foco =
            currentFocus ?: return

        var indice =
            menuZoom.indexOfChild(foco)

        if (indice < 1) {
            indice = 1
        }

        indice += direcao

        if (indice < 1) {
            indice =
                menuZoom.childCount - 1
        }

        if (
            indice >=
            menuZoom.childCount
        ) {
            indice = 1
        }

        menuZoom
            .getChildAt(indice)
            .requestFocus()
    }

    // =========================================================
    // PAUSE
    // =========================================================

    override fun onPause() {

        super.onPause()

        // Não pausar o player aqui.
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        playerView.player =
            null

        player?.release()

        player = null

        super.onDestroy()
    }
}
