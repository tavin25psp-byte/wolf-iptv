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

        menuZoom.elevation = 40f

        menuZoom.visibility =
            View.GONE

        /*
         * O menu inteiro não recebe foco.
         * Somente os botões recebem foco.
         */
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

        playerView.postDelayed({

            conectarEngrenagemNativa()

        }, 400)
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

        val botao =
            playerView.findViewById<View>(id)

        if (botao == null) return

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
                        Color.rgb(190, 0, 0),
                        14f
                    )

            } else {

                view.background = null
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
    }

    // =========================================================
    // CRIAR ZOOMS
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

        titulo.isFocusable = false

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

            botao.setOnFocusChangeListener { view, temFoco ->

                if (temFoco) {

                    /*
                     * FOCO VERMELHO
                     */
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

                    /*
                     * Quando perde o foco,
                     * volta para o visual normal.
                     */
                    val indice =
                        menuZoom.indexOfChild(
                            view
                        ) - 1

                    if (indice >= 0) {

                        atualizarBotao(
                            view as Button,
                            indice
                        )
                    }
                }
            }

            botao.setOnClickListener {

                zoomAtual = i

                aplicarZoom()

                atualizarBotoes()

                /*
                 * Mantém o foco no zoom escolhido.
                 */
                botao.post {

                    botao.requestFocus()
                }
            }

            atualizarBotao(
                botao,
                i
            )

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
    // ATUALIZAR VISUAL DOS ZOOMS
    // =========================================================

    private fun atualizarBotoes() {

        for (i in nomesZoom.indices) {

            val view =
                menuZoom.getChildAt(
                    i + 1
                )

            if (view is Button) {

                /*
                 * Se esse botão está focado,
                 * o vermelho permanece.
                 */
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
    // ABRIR ZOOM
    // =========================================================

    private fun abrirMenuZoom() {

        menuAberto = true

        /*
         * Retira o foco visual da engrenagem.
         */
        engrenagemNativa?.background = null

        menuZoom.visibility =
            View.VISIBLE

        atualizarBotoes()

        /*
         * FOCO VAI DIRETO PARA O ZOOM ATUAL.
         */
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
    // FECHAR ZOOM
    // =========================================================

    private fun fecharMenuZoom() {

        menuAberto = false

        menuZoom.visibility =
            View.GONE

        /*
         * O foco volta para a engrenagem.
         */
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

        if (
            event.action !=
            KeyEvent.ACTION_UP
        ) {

            return super.dispatchKeyEvent(
                event
            )
        }

        // =====================================================
        // MENU ZOOM
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

                    if (foco is Button) {

                        foco.performClick()

                        /*
                         * Garante que o foco
                         * continue no botão.
                         */
                        foco.post {

                            foco.requestFocus()
                        }
                    }

                    return true
                }

                /*
                 * ESQUERDA também fecha o menu.
                 * Útil no controle remoto.
                 */
                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    fecharMenuZoom()

                    return true
                }
            }

            /*
             * Impede que qualquer outra tecla
             * entregue o foco aos controles
             * do player.
             */
            return true
        }

        // =====================================================
        // PLAYER NORMAL
        // =====================================================

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (
                    currentFocus ==
                    engrenagemNativa
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

        return super.dispatchKeyEvent(
            event
        )
    }

    // =
