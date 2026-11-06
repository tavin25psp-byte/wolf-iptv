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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null

    // Tentativas automáticas quando a conexão cai ou dá timeout.
    private var tentativasRede = 0
    private val maxTentativasRede = 5

    // ---- AO VIVO ----
    // true quando o link é canal ao vivo (não é /movie/ nem /series/).
    private var ehAoVivo = false
    // Link original da lista (pode ser .ts).
    private var urlOriginal = ""
    // Link realmente tocado (.ts ou .m3u8, conforme o botão de formato).
    private var urlFinal = ""
    // Já voltou pro link original depois que o .m3u8 falhou?
    private var jaTentouOriginal = false
    // Formato escolhido para canais ao vivo (lembrado entre aberturas).
    private var usarM3u8 = true

    private lateinit var container: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var erroTexto: TextView
    private lateinit var menuZoom: LinearLayout

    private var menuAberto = false
    private var zoomAtual = 0

    private var engrenagemNativa: View? = null

    // Botão "Transmitir para a TV" (aparece junto com os controles do player).
    private lateinit var botaoEspelhar: TextView
    // Botão que alterna entre TS e M3U8 nos canais ao vivo.
    private lateinit var botaoFormato: TextView
    // true enquanto o usuário está na tela de espelhamento do Android:
    // evita pausar o vídeo quando o app sai de cena por um instante.
    private var abrindoEspelhamento = false

    private val zooms = floatArrayOf(1.0f, 1.1f, 1.25f, 1.5f, 2.0f)
    private val nomesZoom = arrayOf("1.0×   Normal", "1.1×", "1.25×", "1.5×", "2.0×")

    private val prefs by lazy { getSharedPreferences("wolf_prefs", MODE_PRIVATE) }

    // Salva a posição do vídeo a cada 5 segundos (continuar assistindo).
    private val salvarLoop = object : Runnable {
        override fun run() {
            salvarProgresso()
            playerView.postDelayed(this, 5000)
        }
    }

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

        usarM3u8 = prefs.getBoolean("ao_vivo_m3u8", true)

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
        playerView.setShutterBackgroundColor(Color.BLACK)
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        playerView.setKeepContentOnPlayerReset(true)
        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        container.addView(
            playerView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ERRO
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

        // MENU ZOOM
        menuZoom = LinearLayout(this)
        menuZoom.orientation = LinearLayout.VERTICAL
        menuZoom.setPadding(20, 20, 20, 20)
        menuZoom.background = fundoArredondado(Color.rgb(10, 10, 10), 24f)
        menuZoom.elevation = 30f
        menuZoom.visibility = View.GONE
        menuZoom.isFocusable = true

        val menuParams = FrameLayout.LayoutParams(390, FrameLayout.LayoutParams.WRAP_CONTENT)
        menuParams.gravity = Gravity.BOTTOM or Gravity.END
        menuParams.setMargins(0, 0, 28, 120)

        container.addView(menuZoom, menuParams)

        criarBotoesZoom()

        // BOTÃO TRANSMITIR PARA A TV
        botaoEspelhar = TextView(this)
        botaoEspelhar.text = "📺  Transmitir para a TV"
        botaoEspelhar.textSize = 16f
        botaoEspelhar.setTextColor(Color.WHITE)
        botaoEspelhar.setPadding(34, 20, 34, 20)
        botaoEspelhar.background = fundoArredondado(Color.argb(190, 10, 10, 10), 40f)
        botaoEspelhar.elevation = 20f
        botaoEspelhar.isFocusable = true
        botaoEspelhar.isClickable = true
        botaoEspelhar.setOnClickListener { abrirEspelhamento() }
        botaoEspelhar.setOnFocusChangeListener { view, temFoco ->
            view.background = if (temFoco) {
                fundoArredondado(Color.rgb(190, 0, 0), 40f)
            } else {
                fundoArredondado(Color.argb(190, 10, 10, 10), 40f)
            }
        }

        val espelharParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        espelharParams.gravity = Gravity.TOP or Gravity.END
        espelharParams.setMargins(0, 36, 36, 0)
        container.addView(botaoEspelhar, espelharParams)

        // BOTÃO FORMATO (TS / M3U8) — só aparece em canal ao vivo
        botaoFormato = TextView(this)
        botaoFormato.textSize = 16f
        botaoFormato.setTextColor(Color.WHITE)
        botaoFormato.setPadding(34, 20, 34, 20)
        botaoFormato.background = fundoArredondado(Color.argb(190, 10, 10, 10), 40f)
        botaoFormato.elevation = 20f
        botaoFormato.isFocusable = true
        botaoFormato.isClickable = true
        botaoFormato.visibility = View.GONE
        botaoFormato.setOnClickListener { alternarFormato() }
        botaoFormato.setOnFocusChangeListener { view, temFoco ->
            view.background = if (temFoco) {
                fundoArredondado(Color.rgb(190, 0, 0), 40f)
            } else {
                fundoArredondado(Color.argb(190, 10, 10, 10), 40f)
            }
        }

        val formatoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        formatoParams.gravity = Gravity.TOP or Gravity.START
        formatoParams.setMargins(36, 36, 0, 0)
        container.addView(botaoFormato, formatoParams)

        // Os botões aparecem e somem junto com os controles do player.
        playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibilidade ->
                botaoEspelhar.visibility = visibilidade
                botaoFormato.visibility = if (ehAoVivo) visibilidade else View.GONE
            }
        )

        setContentView(container)

        playerView.postDelayed({ conectarEngrenagemNativa() }, 300)
    }

    // =========================================================
    // FORMATO TS / M3U8
    // =========================================================

    private fun atualizarTextoFormato() {
        val m3u8 = urlFinal.contains(".m3u8", ignoreCase = true)
        botaoFormato.text = if (m3u8) "🔁  Formato: M3U8" else "🔁  Formato: TS"
    }

    // Troca entre TS e M3U8 na hora, sem precisar mexer no código.
    private fun alternarFormato() {
        if (!ehAoVivo || !urlOriginal.endsWith(".ts", ignoreCase = true)) {
            Toast.makeText(this, "Só vale para canais ao vivo.", Toast.LENGTH_SHORT).show()
            return
        }

        usarM3u8 = !urlFinal.contains(".m3u8", ignoreCase = true)
        prefs.edit().putBoolean("ao_vivo_m3u8", usarM3u8).apply()

        jaTentouOriginal = false
        tentativasRede = 0
        urlFinal = calcularUrlFinal(urlOriginal)
        atualizarTextoFormato()
        erroTexto.visibility = View.GONE

        carregarMidia(0L)

        Toast.makeText(
            this,
            if (usarM3u8) "Formato: M3U8" else "Formato: TS",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun calcularUrlFinal(url: String): String {
        return if (ehAoVivo && usarM3u8 && url.endsWith(".ts", ignoreCase = true)) {
            url.dropLast(3) + ".m3u8"
        } else {
            url
        }
    }

    private fun carregarMidia(inicio: Long) {
        val builder = MediaItem.Builder().setUri(Uri.parse(urlFinal))

        if (urlFinal.contains(".m3u8", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        if (inicio > 0L) {
            player?.setMediaItem(builder.build(), inicio)
        } else {
            player?.setMediaItem(builder.build())
        }

        player?.prepare()
        player?.playWhenReady = true
    }

    // =========================================================
    // ENGRENAGEM NATIVA
    // =========================================================

    private fun conectarEngrenagemNativa() {
        val id = resources.getIdentifier("exo_settings", "id", packageName)
        if (id == 0) return

        val engrenagem = playerView.findViewById<View>(id)

        engrenagem?.let { botao ->
            engrenagemNativa = botao

            botao.isFocusable = true
            botao.isClickable = true

            botao.setOnClickListener {
                if (menuAberto) fecharMenuZoom() else abrirMenuZoom()
            }

            botao.setOnFocusChangeListener { view, temFoco ->
                if (temFoco && !menuAberto) {
                    view.background = fundoArredondado(Color.rgb(190, 0, 0), 14f)
                } else {
                    view.background = null
                }
            }
        }
    }

    // =========================================================
    // FUNDO
    // =========================================================

    private fun fundoArredondado(cor: Int, raio: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(cor)
            cornerRadius = raio
            setStroke(2, Color.argb(100, 255, 255, 255))
        }
    }

    // =========================================================
    // BOTÕES ZOOM
    // =========================================================

    private fun criarBotoesZoom() {
        menuZoom.removeAllViews()

        val titulo = TextView(this)
        titulo.text = "⚙  Zoom da imagem"
        titulo.textSize = 20f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER_VERTICAL
        titulo.setPadding(12, 0, 12, 14)

        menuZoom.addView(
            titulo,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 65)
        )

        for (i in nomesZoom.indices) {
            val botao = Button(this)
            botao.isFocusable = true
            botao.isClickable = true

            atualizarBotao(botao, i)

            botao.setOnFocusChangeListener { view, temFoco ->
                if (temFoco) {
                    view.background = fundoArredondado(Color.rgb(190, 0, 0), 16f)
                } else {
                    atualizarBotao(botao, i)
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
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 62).apply {
                    setMargins(0, 4, 0, 4)
                }
            )
        }

        atualizarBotoes()
    }

    private fun atualizarBotoes() {
        for (i in nomesZoom.indices) {
            val view = menuZoom.getChildAt(i + 1)

            if (view is Button) {
                if (view.hasFocus()) {
                    view.background = fundoArredondado(Color.rgb(190, 0, 0), 16f)
                } else {
                    atualizarBotao(view, i)
                }
            }
        }
    }

    private fun atualizarBotao(botao: Button, indice: Int) {
        if (indice == zoomAtual) {
            botao.text = "✓   ${nomesZoom[indice]}"
            botao.setTextColor(Color.WHITE)
            botao.textSize = 17f
            botao.background = fundoArredondado(Color.rgb(55, 55, 55), 16f)
        } else {
            botao.text = "     ${nomesZoom[indice]}"
            botao.setTextColor(Color.LTGRAY)
            botao.textSize = 17f
            botao.background = fundoArredondado(Color.rgb(28, 28, 28), 16f)
        }
    }

    // =========================================================
    // TRANSMITIR PARA A TV
    // =========================================================

    // Abre o menu com as TVs encontradas (Google Cast e DLNA) e o
    // botão "Espelhar tela" (Smart View / Transmitir do sistema).
    private fun abrirEspelhamento() {
        val p = player
        Transmitir.mostrar(
            activity = this,
            titulo = intent.getStringExtra("VIDEO_TITLE") ?: "WOLF IPTV",
            url = urlFinal,
            capa = intent.getStringExtra("VIDEO_COVER") ?: "",
            posicaoMs = p?.currentPosition ?: 0L,
            aoVivo = ehAoVivo,
            aoEspelhar = { abrindoEspelhamento = true }
        ) {
            // Transmissão começou: para o vídeo no celular.
            salvarProgresso()
            if (ehAoVivo) p?.stop() else p?.pause()
        }
    }

    // =========================================================
    // ABRIR / FECHAR MENU
    // =========================================================

    private fun abrirMenuZoom() {
        menuAberto = true

        engrenagemNativa?.background = null
        menuZoom.visibility = View.VISIBLE

        atualizarBotoes()

        if (menuZoom.childCount > 1) {
            menuZoom.getChildAt(zoomAtual + 1).post {
                menuZoom.getChildAt(zoomAtual + 1).requestFocus()
                atualizarBotoes()
            }
        }
    }

    private fun fecharMenuZoom() {
        menuAberto = false
        menuZoom.visibility = View.GONE

        engrenagemNativa?.post { engrenagemNativa?.requestFocus() }
    }

    // =========================================================
    // APLICAR ZOOM
    // =========================================================

    private fun aplicarZoom() {
        val escala = zooms[zoomAtual]
        val superficie = playerView.videoSurfaceView

        superficie?.let {
            it.pivotX = it.width / 2f
            it.pivotY = it.height / 2f

            it.scaleX = 1.0f
            it.scaleY = escala

            it.translationX = 0f
            it.translationY = 0f
        }
    }

    // =========================================================
    // CONTINUAR ASSISTINDO
    // =========================================================

    private fun salvarProgresso() {
        // Canal ao vivo não tem posição pra salvar.
        if (ehAoVivo) return

        try {
            val p = player ?: return
            val url = intent.getStringExtra("VIDEO_URL") ?: return
            val duracao = p.duration
            WolfProgress.salvar(this, url, p.currentPosition, if (duracao > 0) duracao else 0L)
        } catch (_: Throwable) {
        }
    }

    // =========================================================
    // RECONECTAR AO VIVO
    // =========================================================

    private fun reconectarAoVivo(atrasoMs: Long = 500L) {
        playerView.postDelayed({
            player?.seekToDefaultPosition()
            player?.prepare()
            player?.playWhenReady = true
        }, atrasoMs)
    }

    // =========================================================
    // INICIAR PLAYER
    // =========================================================

    private fun iniciarPlayer() {
        val url = intent.getStringExtra("VIDEO_URL")

        if (url.isNullOrBlank()) {
            mostrarErro("Nenhuma URL de vídeo foi recebida.")
            return
        }

        // Canal ao vivo = link que não é filme nem série sob demanda.
        urlOriginal = url
        ehAoVivo = !url.contains("/movie/") && !url.contains("/series/")

        // Nos canais ao vivo, o formato (TS ou M3U8) vem do botão e fica salvo.
        urlFinal = calcularUrlFinal(url)

        if (ehAoVivo && urlOriginal.endsWith(".ts", ignoreCase = true)) {
            botaoFormato.visibility = View.VISIBLE
            atualizarTextoFormato()
        }

        // Posição pra retomar (0 = começa do início).
        var inicio = intent.getLongExtra("START_POSITION", 0L)

        // Canal ao vivo / .m3u8: ignora posição salva anterior para evitar OUT_OF_RANGE
        if (ehAoVivo || urlFinal.contains(".m3u8", ignoreCase = true)) {
            inicio = 0L
        }

        try {
            // Leitura do TS mais tolerante: ajuda com áudio fora de sincronia
            // em streams de IPTV com marcação de tempo bagunçada.
            val extratores = DefaultExtractorsFactory().setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                    DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )

            player =
                ExoPlayer.Builder(
                    this,
                    DefaultRenderersFactory(this).setEnableDecoderFallback(true)
                )
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(this, extratores)
                            .setDataSourceFactory(
                                DefaultHttpDataSource.Factory()
                                    .setUserAgent("VLC/3.0.20 LibVLC/3.0.20")
                                    .setConnectTimeoutMs(30_000)
                                    .setReadTimeoutMs(30_000)
                                    .setAllowCrossProtocolRedirects(true)
                            )
                    )
                    .setLoadControl(
                        DefaultLoadControl.Builder()
                            .setBufferDurationsMs(15_000, 30_000, 2_500, 5_000)
                            .setTargetBufferBytes(12 * 1024 * 1024)
                            .setPrioritizeTimeOverSizeThresholds(false)
                            .build()
                    )
                    .build()

            playerView.player = player

            player?.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                erroTexto.visibility = View.GONE
                            }

                            Player.STATE_READY -> {
                                tentativasRede = 0
                                erroTexto.visibility = View.GONE

                                playerView.post {
                                    aplicarZoom()
                                    conectarEngrenagemNativa()
                                }
                            }

                            Player.STATE_ENDED -> {
                                if (ehAoVivo) {
                                    // Canal ao vivo nunca "termina": o servidor cortou
                                    // a conexão. Reconecta sozinho.
                                    reconectarAoVivo(500L)
                                } else {
                                    // Filme/série terminou: atualiza "continuar assistindo".
                                    salvarProgresso()
                                }
                            }

                            Player.STATE_IDLE -> {
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        try {
                            tratarErroPlayer(error)
                        } catch (e: Throwable) {
                            mostrarErro("Erro ao reproduzir:\n\n" + error.errorCodeName)
                        }
                    }

                    private fun tratarErroPlayer(error: PlaybackException) {
                        // Servidor não aceitou .m3u8: volta pro link original (.ts)
                        // e deixa o STATE_ENDED reconectar quando cortar.
                        if (ehAoVivo && !jaTentouOriginal && urlFinal != urlOriginal) {
                            jaTentouOriginal = true
                            urlFinal = urlOriginal
                            atualizarTextoFormato()
                            player?.setMediaItem(MediaItem.fromUri(Uri.parse(urlOriginal)))
                            player?.prepare()
                            player?.playWhenReady = true
                            return
                        }

                        // Canal ao vivo ficou para trás da janela: volta pro ao vivo e recarrega
                        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                            player?.seekToDefaultPosition()
                            player?.prepare()
                            player?.playWhenReady = true
                            return
                        }

                        val erroDeRede =
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT

                        // Deteta se o erro é de posição fora do limite (OUT_OF_RANGE)
                        val erroForaDoLimite =
                            error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
                            (error.message?.contains("OUT_OF_RANGE", ignoreCase = true) == true)

                        if (erroForaDoLimite) {
                            mostrarErro("Canal instável ou posição inválida.\n\nA reiniciar do início...")

                            // Limpa o progresso salvo para evitar loop do erro
                            if (!ehAoVivo) {
                                WolfProgress.salvar(this@PlayerActivity, urlOriginal, 0L, 0L)
                            }

                            // Reinicia o player do zero após 1 segundo
                            playerView.postDelayed({
                                player?.seekTo(0)
                                player?.prepare()
                                player?.playWhenReady = true
                            }, 1000)

                            return
                        }

                        // Se foi problema de rede, tenta reconectar sozinho
                        if (erroDeRede && tentativasRede < maxTentativasRede) {
                            tentativasRede++

                            mostrarErro(
                                "Conexão instável.\n\n" +
                                "Reconectando... ($tentativasRede/$maxTentativasRede)"
                            )

                            playerView.postDelayed({
                                player?.prepare()
                                player?.playWhenReady = true
                            }, 2000)

                            return
                        }

                        mostrarErro("Erro ao reproduzir o vídeo:\n\n" + error.errorCodeName + "\n" + (error.cause?.message ?: ""))
                    }
                }
            )

            carregarMidia(inicio)

            playerView.postDelayed(salvarLoop, 5000)

        } catch (e: Exception) {
            mostrarErro("Erro ao abrir o player:\n\n" + e.message)
        }
    }

    // =========================================================
    // ERRO
    // =========================================================

    private fun mostrarErro(mensagem: String) {
        erroTexto.text = mensagem
        erroTexto.visibility = View.VISIBLE
    }

    // =========================================================
    // D-PAD
    // =========================================================

    private var zoomTeclaPressionada = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        // MENU DE ZOOM
        if (menuAberto) {
            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    // Bloqueia o foco automático do Android enquanto o menu está aberto.
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount > 0 || zoomTeclaPressionada) {
                            return true
                        }
                        zoomTeclaPressionada = true
                        return true
                    }

                    if (event.action == KeyEvent.ACTION_UP) {
                        if (!zoomTeclaPressionada) {
                            return true
                        }

                        zoomTeclaPressionada = false

                        if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
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
                    if (event.action == KeyEvent.ACTION_UP) {
                        val foco = currentFocus
                        if (foco is Button) {
                            foco.performClick()
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        fecharMenuZoom()
                    }
                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        fecharMenuZoom()
                    }
                    return true
                }

                else -> {
                    // Enquanto o menu estiver aberto, não deixa o sistema navegar sozinho.
                    return true
                }
            }
        }

        // PLAYER NORMAL
        if (event.action != KeyEvent.ACTION_UP) {
            return super.dispatchKeyEvent(event)
        }

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                val foco = currentFocus
                val engrenagem = engrenagemNativa

                if (foco != null && foco == botaoEspelhar) {
                    abrirEspelhamento()
                    return true
                }

                if (foco != null && foco == botaoFormato) {
                    alternarFormato()
                    return true
                }

                if (foco != null && foco == engrenagem) {
                    abrirMenuZoom()
                    return true
                }

                player?.let {
                    if (it.playbackState == Player.STATE_IDLE) {
                        // Depois de transmitir um canal ao vivo, o player ficou parado.
                        it.prepare()
                        it.play()
                    } else if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.play()
                    }
                }

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Em canal ao vivo não faz sentido avançar/voltar.
                if (!ehAoVivo) {
                    player?.let { it.seekTo(it.currentPosition + 10_000) }
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (!ehAoVivo) {
                    player?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
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

    private fun moverFocoZoom(direcao: Int) {
        val foco = currentFocus

        var indiceAtual = menuZoom.indexOfChild(foco)

        // O título ocupa a posição 0. Os zooms começam na posição 1.
        if (indiceAtual < 1) {
            indiceAtual = zoomAtual + 1
        }

        var novoIndice = indiceAtual + direcao

        if (novoIndice < 1) {
            novoIndice = menuZoom.childCount - 1
        }

        if (novoIndice >= menuZoom.childCount) {
            novoIndice = 1
        }

        val proximo = menuZoom.getChildAt(novoIndice)

        // Desativa temporariamente o foco dos outros botões
        // para impedir que o Android pule automaticamente.
        for (i in 1 until menuZoom.childCount) {
            menuZoom.getChildAt(i).isFocusable = false
        }

        proximo.isFocusable = true
        proximo.requestFocus()

        atualizarBotoes()

        // Reativa os outros botões depois que o foco terminou de mudar.
        menuZoom.post {
            for (i in 1 until menuZoom.childCount) {
                menuZoom.getChildAt(i).isFocusable = true
            }

            proximo.requestFocus()
            atualizarBotoes()
        }
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onResume() {
        super.onResume()
        abrindoEspelhamento = false
        if (player != null) {
            playerView.removeCallbacks(salvarLoop)
            playerView.postDelayed(salvarLoop, 5000)
        }
    }

    override fun onPause() {
        playerView.removeCallbacks(salvarLoop)
        salvarProgresso()
        super.onPause()
    }

    override fun onStop() {
        playerView.removeCallbacks(salvarLoop)
        salvarProgresso()
        if (!abrindoEspelhamento) player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        salvarProgresso()
        playerView.removeCallbacks(salvarLoop)
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
