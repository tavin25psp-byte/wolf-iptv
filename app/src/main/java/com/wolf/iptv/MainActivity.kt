package com.wolf.iptv

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class Filme(
    val titulo: String,
    val ano: Int,
    val categoria: String,
    val capa: String,
    val video: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout

    private var menuAberto = false

    private val filmes = listOf(

        Filme(
            "Como Mágica",
            2026,
            "comedia",
            "https://i.postimg.cc/9z5YzWLn/D-NQ-NP-674318-MLB111141123921-052026-O.webp",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Como%20M%C3%A1gica.mp4"
        ),

        Filme(
            "Todo Mundo em Pânico 4",
            2026,
            "comedia",
            "https://i.postimg.cc/sgfwW2VH/dfdb52dae07d0b0950bb9dfc98ab09c08e44a3f704815cc5a5a6af72d156f913.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/324210b1-aadf-4b2c-b1b5-a23d06716dcb/playlist.m3u8"
        ),

        Filme(
            "Pinóquio",
            2026,
            "animacao",
            "https://i.postimg.cc/ryfYVWCk/IMG-20261002-044814.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Pin%C3%B3quio.mp4"
        ),

        Filme(
            "Moana",
            2026,
            "animacao",
            "https://i.postimg.cc/pdj7VwhR/moana.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/199a8bfd-7a6e-4e1a-9cbe-d5ae4881089b/playlist.m3u8"
        ),

        Filme(
            "Como Treinar o Seu Dragão",
            2026,
            "aventura",
            "https://i.postimg.cc/664hkrZ6/treinar.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/555db026-0cb8-4242-9bdd-dd8a0d165d53/playlist.m3u8"
        ),

        Filme(
            "Quarteto Fantástico: Primeiro Passo",
            2026,
            "acao",
            "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/37836617-2900-4725-9d3e-ed56afffbc45/playlist.m3u8"
        ),

        Filme(
            "Homem-Aranha: Um Novo Dia",
            2026,
            "acao",
            "https://i.postimg.cc/QCctbsqF/aranha.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/145cd2d1-82bc-4e35-986c-9137d44cf91a/playlist.m3u8"
        ),

        Filme(
            "Conexão Perigosa",
            2026,
            "acao",
            "https://i.postimg.cc/JhyRxMrH/conexao.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/78e46f67-4aec-4ba3-b4c4-7072cd6d921b/playlist.m3u8"
        ),

        Filme(
            "A Odisseia",
            2026,
            "aventura",
            "https://i.postimg.cc/K8WjhML7/odisseia.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/85bf08b5-0911-4dc8-9e28-926212aec3bd/playlist.m3u8"
        ),

        Filme(
            "Resident Evil",
            2026,
            "terror",
            "https://i.postimg.cc/3J7DtmC4/evil.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8"
        ),

        Filme(
            "Vingança",
            2026,
            "acao",
            "https://i.postimg.cc/26M7q6P7/vinganca.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/10f5522b-3890-48cd-bfb7-89b2806e8269/playlist.m3u8"
        ),

        Filme(
            "A Revolta",
            2026,
            "acao",
            "https://i.postimg.cc/7YyLx45D/revolta.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/ae8d06f6-e3da-4705-9a83-313fae7114f9/playlist.m3u8"
        ),

        Filme(
            "Thunderbolts",
            2026,
            "acao",
            "https://i.postimg.cc/FzbPQdZJ/D-NQ-NP-848607-CBT107833899597-022026-O.webp",
            "https://wolf-channel-cdn.b-cdn.net/Bala%20zip/Thunderbolts.mp4"
        ),

        Filme(
            "Céu em Fúria",
            2026,
            "acao",
            "https://i.postimg.cc/PJMnXB7d/ceu-em-furia.jpg",
            ""
        ),

        Filme(
            "Jumanji: Bem-Vindo à Selva",
            2017,
            "aventura",
            "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
        ),

        Filme(
            "Kingsman: Agente Secreto",
            2014,
            "acao",
            "https://i.postimg.cc/0QDbfyzB/kings.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4"
        ),

        Filme(
            "Deu a Louca nos Bichos",
            2010,
            "comedia",
            "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4"
        ),

        Filme(
            "Avatar",
            2009,
            "ficcao",
            "https://i.postimg.cc/7LQgchYy/avatar.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/avatar-1080p.mp4"
        ),

        Filme(
            "17 Outra Vez",
            2009,
            "comedia",
            "https://i.postimg.cc/FFwTzW46/17.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4"
        ),

        Filme(
            "Garota Infernal",
            2009,
            "terror",
            "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4"
        ),

        Filme(
            "A Noiva Cadáver",
            2005,
            "animacao",
            "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4"
        )
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
    }

    private fun criarInterface() {

        raiz = FrameLayout(this)

        val fundo = ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        carregarImagem(
            fundo,
            "https://i.postimg.cc/Ghk8PP7w/wolf.png"
        )

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val escuro = View(this)

        escuro.setBackgroundColor(
            Color.argb(115, 0, 0, 0)
        )

        raiz.addView(
            escuro,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val camada = LinearLayout(this)

        camada.orientation =
            LinearLayout.VERTICAL

        val topo = FrameLayout(this)

        val logo = TextView(this)

        logo.text = "WOLF CHANNEL"
        logo.textSize = 26f
        logo.setTextColor(Color.WHITE)
        logo.setTypeface(null, Typeface.BOLD)
        logo.gravity = Gravity.CENTER_VERTICAL

        val logoParams =
            FrameLayout.LayoutParams(
                400,
                75
            )

        logoParams.gravity =
            Gravity.START

        logoParams.setMargins(
            30,
            15,
            0,
            0
        )

        topo.addView(
            logo,
            logoParams
        )

        val botaoMenu =
            criarBotaoTopo()

        val menuParams =
            FrameLayout.LayoutParams(
                75,
                65
            )

        menuParams.gravity =
            Gravity.END

        menuParams.setMargins(
            0,
            20,
            25,
            0
        )

        topo.addView(
            botaoMenu,
            menuParams
        )

        camada.addView(
            topo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
            )
        )

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.setPadding(
            25,
            10,
            25,
            40
        )

        mostrarFilmes()

        val scroll =
            ScrollView(this)

        scroll.isFillViewport = true
        scroll.overScrollMode =
            View.OVER_SCROLL_NEVER

        /*
         * CORREÇÃO DO ERRO:
         * Usamos FrameLayout.LayoutParams
         * em vez de ScrollView.LayoutParams.
         */
        scroll.addView(
            conteudo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        camada.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(raiz)

        botaoMenu.requestFocus()
    }

    private fun criarBotaoTopo(): TextView {

        val botao = TextView(this)

        botao.text = "☰"
        botao.textSize = 30f
        botao.gravity = Gravity.CENTER
        botao.setTextColor(Color.WHITE)

        val normal =
            GradientDrawable()

        normal.setColor(
            Color.argb(190, 15, 15, 18)
        )

        normal.cornerRadius = 18f

        val foco =
            GradientDrawable()

        foco.setColor(
            Color.rgb(40, 40, 45)
        )

        foco.cornerRadius = 18f

        foco.setStroke(
            2,
            Color.WHITE
        )

        botao.background = normal

        botao.isFocusable = true
        botao.isClickable = true

        botao.setOnFocusChangeListener {
                view,
                temFoco ->

            if (temFoco) {
                view.background = foco
            } else {
                view.background = normal
            }
        }

        botao.setOnClickListener {
            abrirMenu()
        }

        return botao
    }

    private fun mostrarFilmes(
        categoria: String? = null
    ) {

        conteudo.removeAllViews()

        val titulo = TextView(this)

        titulo.text =
            if (categoria == null) {
                "FILMES"
            } else {
                categoria.uppercase()
            }

        titulo.textSize = 21f
        titulo.setTextColor(Color.WHITE)
        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            8,
            10,
            8,
            18
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        val lista =
            if (categoria == null) {
                filmes
            } else {
                filmes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            }

        var linha:
                LinearLayout? = null

        var quantidade = 0

        lista.forEach { filme ->

            if (quantidade == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER_VERTICAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        285
                    )
                )
            }

            linha?.addView(
                criarCard(filme),
                LinearLayout.LayoutParams(
                    0,
                    260,
                    1f
                ).apply {
                    setMargins(
                        8,
                        8,
                        8,
                        8
                    )
                }
            )

            quantidade++

            if (quantidade == 5) {
                quantidade = 0
            }
        }
    }

    private fun criarCard(
        filme: Filme
    ): FrameLayout {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isClickable = true
        card.isFocusableInTouchMode = true

        val fundoNormal =
            GradientDrawable()

        fundoNormal.setColor(
            Color.argb(
                190,
                10,
                10,
                12
            )
        )

        fundoNormal.cornerRadius = 14f

        fundoNormal.setStroke(
            1,
            Color.argb(
                100,
                255,
                255,
                255
            )
        )

        val fundoFoco =
            GradientDrawable()

        fundoFoco.setColor(
            Color.argb(
                235,
                25,
                25,
                30
            )
        )

        fundoFoco.cornerRadius = 14f

        fundoFoco.setStroke(
            3,
            Color.WHITE
        )

        card.background =
            fundoNormal

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setPadding(
            8,
            8,
            8,
            48
        )

        carregarImagem(
            imagem,
            filme.capa
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val faixa =
            TextView(this)

        faixa.text =
            "${filme.titulo}\n${filme.ano}"

        faixa.textSize = 13f
        faixa.setTextColor(Color.WHITE)

        faixa.setTypeface(
            null,
            Typeface.BOLD
        )

        faixa.gravity =
            Gravity.CENTER_VERTICAL

        faixa.setPadding(
            10,
            4,
            10,
            4
        )

        val faixaFundo =
            GradientDrawable()

        faixaFundo.setColor(
            Color.argb(
                220,
                0,
                0,
                0
            )
        )

        faixaFundo.cornerRadius = 10f

        faixa.background =
            faixaFundo

        val faixaParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                48
            )

        faixaParams.gravity =
            Gravity.BOTTOM

        faixaParams.setMargins(
            6,
            0,
            6,
            6
        )

        card.addView(
            faixa,
            faixaParams
        )

        card.setOnFocusChangeListener {
                view,
                temFoco ->

            if (temFoco) {

                view.background =
                    fundoFoco

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(120)
                    .start()

            } else {

                view.background =
                    fundoNormal

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }

        card.setOnClickListener {
            abrirVideo(filme)
        }

        return card
    }

    private fun abrirVideo(
        filme: Filme
    ) {

        if (filme.video.isBlank()) {

            mostrarMensagem(
                "O vídeo de ${filme.titulo} ainda não possui link."
            )

            return
        }

        val intent =
            android.content.Intent(
                this,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "VIDEO_URL",
            filme.video
        )

        intent.putExtra(
            "VIDEO_TITLE",
            filme.titulo
        )

        startActivity(intent)
    }

    private fun aplicarFoco(
        view: View
    ) {

        view.setOnFocusChangeListener {
                v,
                foco ->

            if (foco) {

                v.setBackgroundColor(
                    Color.rgb(
                        40,
                        40,
                        45
                    )
                )

            } else {

                v.setBackgroundColor(
                    Color.TRANSPARENT
                )
            }
        }
    }    private fun abrirMenu() {

        if (menuAberto) {
            fecharMenu()
            return
        }

        menuAberto = true

        val painel =
            LinearLayout(this)

        painel.orientation =
            LinearLayout.VERTICAL

        painel.setPadding(
            28,
            30,
            20,
            25
        )

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.rgb(
                10,
                10,
                13
            )
        )

        fundo.cornerRadius = 28f

        fundo.setStroke(
            2,
            Color.rgb(
                55,
                55,
                60
            )
        )

        painel.background = fundo

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF CHANNEL"

        titulo.textSize = 23f
        titulo.setTextColor(Color.WHITE)

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            5,
            0,
            0,
            3
        )

        painel.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        )

        val subtitulo =
            TextView(this)

        subtitulo.text =
            "NAVEGAÇÃO"

        subtitulo.textSize = 11f

        subtitulo.setTextColor(
            Color.rgb(
                145,
                145,
                150
            )
        )

        subtitulo.setTypeface(
            null,
            Typeface.BOLD
        )

        subtitulo.letterSpacing =
            0.15f

        subtitulo.setPadding(
            5,
            0,
            0,
            10
        )

        painel.addView(
            subtitulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                35
            )
        )

        val scroll =
            ScrollView(this)

        scroll.isFillViewport = true

        scroll.overScrollMode =
            View.OVER_SCROLL_NEVER

        val lista =
            LinearLayout(this)

        lista.orientation =
            LinearLayout.VERTICAL

        scroll.addView(
            lista,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        painel.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        adicionarItemPremium(
            lista,
            "★",
            "Favoritos"
        )

        adicionarItemPremium(
            lista,
            "▶",
            "Continue assistindo"
        )

        adicionarSeparadorPremium(
            lista
        )

        adicionarTituloPremium(
            lista,
            "FILMES"
        )

        adicionarCategoriaPremium(
            lista,
            "Ação",
            "FILMES",
            "acao"
        )

        adicionarCategoriaPremium(
            lista,
            "Aventura",
            "FILMES",
            "aventura"
        )

        adicionarCategoriaPremium(
            lista,
            "Comédia",
            "FILMES",
            "comedia"
        )

        adicionarCategoriaPremium(
            lista,
            "Terror",
            "FILMES",
            "terror"
        )

        adicionarCategoriaPremium(
            lista,
            "Animação",
            "FILMES",
            "animacao"
        )

        adicionarCategoriaPremium(
            lista,
            "Ficção",
            "FILMES",
            "ficcao"
        )

        adicionarSeparadorPremium(
            lista
        )

        adicionarTituloPremium(
            lista,
            "SÉRIES"
        )

        adicionarCategoriaPremium(
            lista,
            "Ação",
            "SERIES",
            "acao"
        )

        adicionarCategoriaPremium(
            lista,
            "Comédia",
            "SERIES",
            "comedia"
        )

        adicionarCategoriaPremium(
            lista,
            "Drama",
            "SERIES",
            "drama"
        )

        adicionarCategoriaPremium(
            lista,
            "Romance",
            "SERIES",
            "romance"
        )

        adicionarSeparadorPremium(
            lista
        )

        adicionarTituloPremium(
            lista,
            "DORAMAS"
        )

        adicionarCategoriaPremium(
            lista,
            "Ação",
            "DORAMAS",
            "acao"
        )

        adicionarCategoriaPremium(
            lista,
            "Romance",
            "DORAMAS",
            "romance"
        )

        adicionarCategoriaPremium(
            lista,
            "Drama",
            "DORAMAS",
            "drama"
        )

        adicionarCategoriaPremium(
            lista,
            "Comédia",
            "DORAMAS",
            "comedia"
        )

        adicionarSeparadorPremium(
            lista
        )

        adicionarTituloPremium(
            lista,
            "ANIME"
        )

        adicionarCategoriaPremium(
            lista,
            "Ação",
            "ANIME",
            "acao"
        )

        adicionarCategoriaPremium(
            lista,
            "Aventura",
            "ANIME",
            "aventura"
        )

        adicionarCategoriaPremium(
            lista,
            "Fantasia",
            "ANIME",
            "fantasia"
        )

        adicionarCategoriaPremium(
            lista,
            "Comédia",
            "ANIME",
            "comedia"
        )

        val parametros =
            FrameLayout.LayoutParams(
                480,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        parametros.gravity =
            Gravity.END

        parametros.setMargins(
            0,
            25,
            25,
            25
        )

        raiz.addView(
            painel,
            parametros
        )

        painel.alpha = 0f
        painel.translationX = 80f

        painel.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(220)
            .start()

        painel.isFocusableInTouchMode =
            true

        painel.post {

            if (lista.childCount > 0) {

                var primeiroFoco: View? = null

                for (
                    i in 0 until lista.childCount
                ) {

                    val item =
                        lista.getChildAt(i)

                    if (item.isFocusable) {

                        primeiroFoco = item
                        break
                    }
                }

                primeiroFoco?.requestFocus()
            }
        }
    }

    private fun adicionarTituloPremium(
        lista: LinearLayout,
        texto: String
    ) {

        val titulo =
            TextView(this)

        titulo.text = texto
        titulo.textSize = 11f

        titulo.setTextColor(
            Color.rgb(
                125,
                125,
                130
            )
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.letterSpacing =
            0.18f

        titulo.setPadding(
            12,
            18,
            8,
            8
        )

        lista.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                45
            )
        )
    }

    private fun adicionarItemPremium(
        lista: LinearLayout,
        icone: String,
        texto: String
    ) {

        val item =
            TextView(this)

        item.text =
            "$icone    $texto"

        item.textSize = 16f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            18,
            0,
            12,
            0
        )

        item.isFocusable = true
        item.isClickable = true

        aplicarEstiloFocoPremium(
            item
        )

        item.setOnClickListener {

            if (texto == "Favoritos") {

                mostrarMensagem(
                    "Favoritos"
                )

            } else {

                mostrarMensagem(
                    "Continue assistindo"
                )
            }
        }

        lista.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
        )
    }

    private fun adicionarCategoriaPremium(
        lista: LinearLayout,
        texto: String,
        tipo: String,
        categoria: String
    ) {

        val item =
            TextView(this)

        item.text =
            "›   $texto"

        item.textSize = 15f

        item.setTextColor(
            Color.rgb(
                215,
                215,
                220
            )
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            22,
            0,
            12,
            0
        )

        item.isFocusable = true
        item.isClickable = true

        aplicarEstiloFocoPremium(
            item
        )

        item.setOnClickListener {

            when (tipo) {

                "FILMES" -> {

                    mostrarFilmes(
                        categoria
                    )

                    fecharMenu()
                }

                "SERIES" -> {

                    mostrarMensagem(
                        "Séries • $texto"
                    )

                    fecharMenu()
                }

                "DORAMAS" -> {

                    mostrarMensagem(
                        "Doramas • $texto"
                    )

                    fecharMenu()
                }

                "ANIME" -> {

                    mostrarMensagem(
                        "Anime • $texto"
                    )

                    fecharMenu()
                }
            }
        }

        lista.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52
            )
        )
    }

    private fun aplicarEstiloFocoPremium(
        item: TextView
    ) {

        val normal =
            GradientDrawable()

        normal.setColor(
            Color.TRANSPARENT
        )

        normal.cornerRadius = 16f

        val foco =
            GradientDrawable()

        foco.setColor(
            Color.rgb(
                35,
                35,
                40
            )
        )

        foco.cornerRadius = 16f

        foco.setStroke(
            2,
            Color.WHITE
        )

        item.background =
            normal

        item.setOnFocusChangeListener {
                view,
                ganhouFoco ->

            if (ganhouFoco) {

                view.background =
                    foco

                view.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(100)
                    .start()

            } else {

                view.background =
                    normal

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
        }
    }

    private fun adicionarSeparadorPremium(
        lista: LinearLayout
    ) {

        val linha =
            View(this)

        linha.setBackgroundColor(
            Color.rgb(
                45,
                45,
                50
            )
        )

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )

        parametros.setMargins(
            8,
            14,
            8,
            8
        )

        lista.addView(
            linha,
            parametros
        )
    }

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        if (raiz.childCount > 2) {

            val painel =
                raiz.getChildAt(
                    raiz.childCount - 1
                )

            painel.animate()
                .alpha(0f)
                .translationX(80f)
                .setDuration(180)
                .withEndAction {

                    if (painel.parent != null) {

                        raiz.removeView(
                            painel
                        )
                    }

                    conteudo.requestFocus()
                }
                .start()

        } else {

            conteudo.requestFocus()
        }
    }

    private fun mostrarMensagem(
        mensagem: String
    ) {

        val aviso =
            TextView(this)

        aviso.text = mensagem
        aviso.textSize = 17f

        aviso.setTextColor(
            Color.WHITE
        )

        aviso.gravity =
            Gravity.CENTER

        aviso.setTypeface(
            null,
            Typeface.BOLD
        )

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.rgb(
                20,
                20,
                23
            )
        )

        fundo.cornerRadius = 18f

        fundo.setStroke(
            2,
            Color.rgb(
                70,
                70,
                75
            )
        )

        aviso.background =
            fundo

        val parametros =
            FrameLayout.LayoutParams(
                420,
                80
            )

        parametros.gravity =
            Gravity.CENTER

        raiz.addView(
            aviso,
            parametros
        )

        aviso.alpha = 0f

        aviso.animate()
            .alpha(1f)
            .setDuration(180)
            .withEndAction {

                aviso.postDelayed({

                    aviso.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction {

                            if (
                                aviso.parent != null
                            ) {

                                raiz.removeView(
                                    aviso
                                )
                            }
                        }
                        .start()

                }, 1300)
            }
            .start()
    }

    private fun carregarImagem(
        imagem: ImageView,
        url: String
    ) {

        if (url.isBlank()) {
            return
        }

        thread {

            try {

                val conexao =
                    URL(url)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    10000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.disconnect()

                runOnUiThread {

                    if (bitmap != null) {

                        imagem.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_MENU -> {

                    abrirMenu()

                    return true
                }

                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    override fun onBackPressed() {

        if (menuAberto) {

            fecharMenu()

        } else {

            super.onBackPressed()
        }
    }
}
