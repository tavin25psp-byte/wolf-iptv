package com.wolf.iptv

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

        container.addView(
            erroTexto,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // =====================================================
        // MENU DE ZOOM
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

        menuZoom.elevation = 40f

        menuZoom.visibility =
            View.GONE

        menuZoom.isFocusable = false

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

        // Espera os controles do Media3 aparecerem.
        playerView.postDelayed(
            {
                conectarEngrenagemNativa()
            },
            400
        )
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

        if (id == 0) {
            return
        }

        val botao =
            playerView.findViewById<View>(id)

        if (botao == null) {
            return
        }

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

        botao.setOnFocusChangeListener {
                view,
                temFoco ->

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

    // =========================================================
    // FUNDO ARREDONDADO
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
    }

    // =========================================================
    // CRIAR BOTÕES DE ZOOM
    // =========================================================

    private fun criarBotoesZoom() {

        menuZoom.removeAllViews()

        // =====================================================
        // TÍTULO
        // =====================================================

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

        titulo.isFocusable = false

        menuZoom.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // =====================================================
        // ZOOMS
        // =====================================================

        for (i in nomesZoom.indices) {

            val indice = i

            val botao =
                Button(this)

            botao.isFocusable = true
            botao.isClickable = true

            atualizarBotao(
                botao,
                indice
            )

            // =================================================
            // FOCO
            // =================================================

            botao.setOnFocusChangeListener {
                    view,
                    temFoco ->

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

                    if (view is Button) {

                        view.setTextColor(
                            Color.WHITE
                        )
                    }

                } else {

                    atualizarBotao(
                        view as Button,
                        indice
                    )
                }
            }

            // =================================================
            // CLIQUE
            // =================================================

            botao.setOnClickListener {

                zoomAtual = indice

                aplicarZoom()

                atualizarBotoes()

                // Mantém o foco no botão escolhido.
                botao.post {
                    botao.requestFocus()
                }
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
    // ATUALIZAR TODOS OS BOTÕES
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

                    view.setTextColor(
                        Color.WHITE
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

    // =========================================================
    // ATUALIZAR UM BOTÃO
    // =========================================================

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

        // Remove destaque da engrenagem enquanto o menu está aberto.
        engrenagemNativa?.background = null

        menuZoom.visibility =
            View.VISIBLE

        atualizarBotoes()

        // Foco direto no zoom atualmente selecionado.
        val botaoZoom =
            menuZoom.getChildAt(
                zoomAtual + 1
            )

        botaoZoom.isFocusable = true

        botaoZoom.requestFocus()

        botaoZoom.post {
            botaoZoom.requestFocus()
        }
    }

    // =========================================================
    // FECHAR MENU
    // =========================================================

    private fun fecharMenuZoom() {

        menuAberto = false

        menuZoom.visibility =
            View.GONE

        // Devolve o foco para a engrenagem.
        engrenagemNativa?.let {

            it.isFocusable = true

            it.requestFocus()

            it.post {
                it.requestFocus()
            }
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

            /*
             * O zoom é aplicado somente
             * na superfície do vídeo.
             *
             * Os controles do PlayerView
             * não são ampliados.
             */
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
                                // Vídeo terminou.
                            }

                            Player.STATE_IDLE -> {
                                // Player parado.
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

            // =================================================
            // M3U8
            // =================================================

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
    }

    // =========================================================
    // MOSTRAR ERRO
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

        /*
         * Só processa quando a tecla foi solta.
         */
        if (
            event.action !=
            KeyEvent.ACTION_UP
        ) {

            return super.dispatchKeyEvent(
                event
            )
        }

        // =====================================================
        // MENU DE ZOOM ABERTO
        // =====================================================

        if (menuAberto) {

            when (event.keyCode) {

                // -------------------------------------------------
                // BACK FECHA
                // -------------------------------------------------

                KeyEvent.KEYCODE_BACK -> {

                    fecharMenuZoom()

                    return true
                }

                // -------------------------------------------------
                // SETA PARA CIMA
                // -------------------------------------------------

                KeyEvent.KEYCODE_DPAD_UP -> {

                    moverFocoZoom(-1)

                    return true
                }

                // -------------------------------------------------
                // SETA PARA BAIXO
                // -------------------------------------------------

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    moverFocoZoom(1)

                    return true
                }

                // -------------------------------------------------
                // OK
                // -------------------------------------------------

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco =
                        currentFocus

                    if (foco is Button) {

                        foco.performClick()

                        foco.post {
                            foco.requestFocus()
                        }
                    }

                    return true
                }

                // -------------------------------------------------
                // ESQUERDA FECHA
                // -------------------------------------------------

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    fecharMenuZoom()

                    return true
                }
            }

            /*
             * Enquanto o menu está aberto,
             * nenhuma outra tecla pode entregar
             * o foco aos controles do player.
             */
            return t
