package com.wolf.iptv

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    private lateinit var container: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var erroTexto: TextView
    private lateinit var menuZoom: LinearLayout

    private var menuAberto = false
    private var zoomAtual = 0

    private var engrenagemNativa: View? = null

    private val zooms = floatArrayOf(
        1.0f,
        1.1f,
        1.25f,
        1.5f,
        2.0f
    )

    private val nomesZoom = arrayOf(
        "1.0×   Normal",
        "1.1×",
        "1.25×",
        "1.5×",
        "2.0×"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém a tela ligada (evita protetor de tela / tela apagando).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Fundo da janela preto: se a superfície do vídeo recriar,
        // não aparece a imagem de fundo do tema do app.
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

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
        container.setBackgroundColor(Color.BLACK)

        playerView = PlayerView(this)

        playerView.useController = true
        playerView.controllerShowTimeoutMs = 5000

        // Evita a tela de fundo aparecer quando o player
        // estiver carregando ou recriando a superfície.
        playerView.setShutterBackgroundColor(Color.BLACK)

        // Mostra o círculo de carregando quando está buffering,
        // pra diferenciar de pausa.
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)

        // Mantém o conteúdo do vídeo quando o player é resetado.
        playerView.setKeepContentOnPlayerReset(true)

        playerView.resizeMode =
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        container.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // =====================================================
        // ERRO
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

        container.addView(
            erroTexto,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // =====================================================
        // MENU ZOOM
        // =====================================================

        menuZoom = LinearLayout(this)

        menuZoom.orientation =
            LinearLayout.VERTICAL

        menuZoom.setPadding(
            20,
            20,
            20,
            20
        )

        menuZoom.background =
            fundoArredondado(
                Color.rgb(10, 10, 10),
                24f
            )

        menuZoom.elevation = 30f

        menuZoom.visibility =
            View.GONE

        menuZoom.isFocusable = true

        val menuParams =
            FrameLayout.LayoutParams(
                390,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

        menuParams.gravity =
            Gravity.BOTTOM or Gravity.END

        menuParams.setMargins(
            0,
            0,
            28,
            120
        )

        container.addView(
            menuZoom,
            menuParams
        )

        criarBotoesZoom()

        setContentView(container)

        playerView.postDelayed({

            conectarEngrenagemNativa()

        }, 300)
    }

    // =========================================================
    // ENGRENAGEM NATIVA
    // =========================================================

    private fun conectarEngrenagemNativa() {

        val id = resources.getIdentifier(
            "exo_settings",
            "id",
            packageName
        )

        if (id == 0) return

        val engrenagem =
            playerView.findViewById<View>(id)

        engrenagem?.let { botao ->

            engrenagemNativa = botao

            botao.isFocusable = true
            botao.isClickable = true

            botao.setOnClickListener {

                if (menuAberto) {

                    fecharMenuZoom()

                } else {

                    abrirMenuZoom()
                }
            }

            botao.setOnFocusChangeListener { view, temFoco ->

                if (temFoco && !menuAberto) {

                    view.background =
                        fundoArredondado(
                            Color.rgb(
                                190,
                                0,
                                0
                            ),
                            14f
                        )

                } else {

                    view.background = null
                }
            }
        }
    }

    // =========================================================
    // FUNDO
    // =========================================================

    private fun fundoArredondado(
        cor: Int,
        raio: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(cor)

            cornerRadius = raio

            setStroke(
                2,
                Color.argb(
                    100,
                    255,
                    255,
                    255
                )
            )
        }
    }    // =========================================================
    // BOTÕES ZOOM
    // =========================================================

    private fun criarBotoesZoom() {

        menuZoom.removeAllViews()

        val titulo = TextView(this)

        titulo.text =
            "⚙  Zoom da imagem"

        titulo.textSize = 20f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            12,
            0,
            12,
            14
        )

        menuZoom.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        for (i in nomesZoom.indices) {

            val botao =
                Button(this)

            botao.isFocusable = true
            botao.isClickable = true

            atualizarBotao(
                botao,
                i
            )

            botao.setOnFocusChangeListener { view, temFoco ->

                if (temFoco) {

                    view.background =
                        fundoArredondado(
                            Color.rgb(
                                190,
                                0,
                                0
                            ),
                            16f
                        )

                } else {

                    atualizarBotao(
                        botao,
                        i
                    )
                }
            }

            botao.setOnClickListener {

                zoomAtual = i

                aplicarZoom()

                atualizarBotoes()

                botao.requestFocus()
            }

            menuZoom.addView(
                botao,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    62
                ).apply {

                    setMargins(
                        0,
                        4,
                        0,
                        4
                    )
                }
            )
        }

        atualizarBotoes()
    }

    // =========================================================
    // ATUALIZAR BOTÕES
    // =========================================================

    private fun atualizarBotoes() {

        for (i in nomesZoom.indices) {

            val view =
                menuZoom.getChildAt(
                    i + 1
                )

            if (view is Button) {

                if (view.hasFocus()) {

                    view.background =
                        fundoArredondado(
                            Color.rgb(
                                190,
                                0,
                                0
                            ),
                            16f
                        )

                } else {

                    atualizarBotao(
                        view,
                        i
                    )
                }
            }
        }
    }

    private fun atualizarBotao(
        botao: Button,
        indice: Int
    ) {

        if (indice == zoomAtual) {

            botao.text =
                "✓   ${nomesZoom[indice]}"

            botao.setTextColor(
                Color.WHITE
            )

            botao.textSize = 17f

            botao.background =
                fundoArredondado(
                    Color.rgb(
                        55,
                        55,
                        55
                    ),
                    16f
                )

        } else {

            botao.text =
                "     ${nomesZoom[indice]}"

            botao.setTextColor(
                Color.LTGRAY
            )

            botao.textSize = 17f

            botao.background =
                fundoArredondado(
                    Color.rgb(
                        28,
                        28,
                        28
                    ),
                    16f
                )
        }
    }

    // =========================================================
    // ABRIR MENU
    // =========================================================

    private fun abrirMenuZoom() {

        menuAberto = true

        engrenagemNativa?.background = null

        menuZoom.visibility =
            View.VISIBLE

        atualizarBotoes()

        if (menuZoom.childCount > 1) {

            menuZoom
                .getChildAt(
                    zoomAtual + 1
                )
                .post {

                    menuZoom
                        .getChildAt(
                            zoomAtual + 1
                        )
                        .requestFocus()

                    atualizarBotoes()
                }
        }
    }

    // =========================================================
    // FECHAR MENU
    // =========================================================

    private fun fecharMenuZoom() {

        menuAberto = false

        menuZoom.visibility =
            View.GONE

        engrenagemNativa?.post {

            engrenagemNativa?.requestFocus()
        }
    }

    // =========================================================
    // APLICAR ZOOM
    // =========================================================

    private fun aplicarZoom() {

        val escala =
            zooms[zoomAtual]

        val superficie =
            playerView.videoSurfaceView

        superficie?.let {

            it.pivotX =
                it.width / 2f

            it.pivotY =
                it.height / 2f

            it.scaleX = 1.0f
            it.scaleY = escala

            it.translationX = 0f
            it.translationY = 0f
        }
    }

    // =========================================================
    // INICIAR PLAYER
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
                    .setLoadControl(
                        DefaultLoadControl.Builder()
                            .setBufferDurationsMs(
                                30_000,
                                120_000,
                                2_500,
                                5_000
                            )
                            .build()
                    )
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

                                playerView.post {

                                    aplicarZoom()

                                    conectarEngrenagemNativa()
                                }
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

            player?.setMediaItem(
                builder.build()
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
    }    // =========================================================
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

    private var zoomTeclaPressionada = false

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        // =====================================================
        // MENU DE ZOOM
        // =====================================================

        if (menuAberto) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    // Bloqueia completamente o foco automático
                    // do Android enquanto o menu está aberto.

                    if (event.action ==
                        KeyEvent.ACTION_DOWN
                    ) {

                        if (
                            event.repeatCount > 0 ||
                            zoomTeclaPressionada
                        ) {
                            return true
                        }

                        zoomTeclaPressionada = true

                        return true
                    }

                    if (event.action ==
                        KeyEvent.ACTION_UP
                    ) {

                        if (!zoomTeclaPressionada) {
                            return true
                        }

                        zoomTeclaPressionada = false

                        if (
                            event.keyCode ==
                            KeyEvent.KEYCODE_DPAD_UP
                        ) {

                            moverFocoZoom(-1)

                        } else {

                            moverFocoZoom(1)
                        }

                        return true
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    if (
                        event.action ==
                        KeyEvent.ACTION_UP
                    ) {

                        val foco =
                            currentFocus

                        if (foco is Button) {

                            foco.performClick()
                        }
                    }

                    return true
                }

                KeyEvent.KEYCODE_BACK -> {

                    if (
                        event.action ==
                        KeyEvent.ACTION_UP
                    ) {

                        fecharMenuZoom()
                    }

                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (
                        event.action ==
                        KeyEvent.ACTION_UP
                    ) {

                        fecharMenuZoom()
                    }

                    return true
                }

                else -> {

                    // Enquanto o menu estiver aberto,
                    // não deixa o sistema navegar sozinho.
                    return true
                }
            }
        }

        // =====================================================
        // PLAYER NORMAL
        // =====================================================

        if (
            event.action !=
            KeyEvent.ACTION_UP
        ) {

            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                val foco =
                    currentFocus

                val engrenagem =
                    engrenagemNativa

                if (
                    foco != null &&
                    foco == engrenagem
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
    // MOVIMENTAR ZOOM
    // =========================================================

    private fun moverFocoZoom(
        direcao: Int
    ) {

        val foco =
            currentFocus

        var indiceAtual =
            menuZoom.indexOfChild(foco)

        // O título ocupa a posição 0.
        // Os zooms começam na posição 1.

        if (indiceAtual < 1) {

            indiceAtual =
                zoomAtual + 1
        }

        var novoIndice =
            indiceAtual + direcao

        // Primeiro zoom
        if (novoIndice < 1) {

            novoIndice =
                menuZoom.childCount - 1
        }

        // Último zoom
        if (
            novoIndice >=
            menuZoom.childCount
        ) {

            novoIndice = 1
        }

        val proximo =
            menuZoom.getChildAt(
                novoIndice
            )

        // Desativa temporariamente o foco
        // dos outros botões para impedir que
        // o Android pule automaticamente.

        for (
            i in 1 until menuZoom.childCount
        ) {

            menuZoom
                .getChildAt(i)
                .isFocusable = false
        }

        proximo.isFocusable = true

        proximo.requestFocus()

        atualizarBotoes()

        // Reativa os outros botões depois
        // que o foco terminou de mudar.

        menuZoom.post {

            for (
                i in 1 until menuZoom.childCount
            ) {

                menuZoom
                    .getChildAt(i)
                    .isFocusable = true
            }

            proximo.requestFocus()

            atualizarBotoes()
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    override fun onPause() {

        super.onPause()
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        playerView.player = null

        player?.release()

        player = null

        super.onDestroy()
    }
}
