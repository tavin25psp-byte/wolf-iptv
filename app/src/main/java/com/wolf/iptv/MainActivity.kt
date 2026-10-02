package com.wolf.iptv

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    data class Filme(
        val titulo: String,
        val ano: Int,
        val categoria: String,
        val capa: String,
        val video: String
    )

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView

    private var menuAberto = false

    private val filmes = listOf(

        Filme(
            "Como Mágica",
            2026,
            "Comédia",
            "https://i.postimg.cc/9z5YzWLn/D-NQ-NP-674318-MLB111141123921-052026-O.webp",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Como%20M%C3%A1gica.mp4"
        ),

        Filme(
            "Todo Mundo em Pânico 4",
            2026,
            "Comédia",
            "https://i.postimg.cc/sgfwW2VH/dfdb52dae07d0b0950bb9dfc98ab09c08e44a3f704815cc5a5a6af72d156f913.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/324210b1-aadf-4b2c-b1b5-a23d06716dcb/playlist.m3u8"
        ),

        Filme(
            "Pinóquio",
            2026,
            "Animação",
            "https://i.postimg.cc/ryfYVWCk/IMG-20261002-044814.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Pin%C3%B3quio.mp4"
        ),

        Filme(
            "Moana",
            2026,
            "Animação",
            "https://i.postimg.cc/pdj7VwhR/moana.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/199a8bfd-7a6e-4e1a-9cbe-d5ae4881089b/playlist.m3u8"
        ),

        Filme(
            "Como Treinar o Seu Dragão",
            2026,
            "Aventura",
            "https://i.postimg.cc/664hkrZ6/treinar.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/555db026-0cb8-4242-9bdd-dd8a0d165d53/playlist.m3u8"
        ),

        Filme(
            "Quarteto Fantástico: Primeiro Passo",
            2026,
            "Ação",
            "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/37836617-2900-4725-9d3e-ed56afffbc45/playlist.m3u8"
        ),

        Filme(
            "Homem-Aranha: Um Novo Dia",
            2026,
            "Ação",
            "https://i.postimg.cc/QCctbsqF/aranha.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/145cd2d1-82bc-4e35-986c-9137d44cf91a/playlist.m3u8"
        ),

        Filme(
            "Conexão Perigosa",
            2026,
            "Ação",
            "https://i.postimg.cc/JhyRxMrH/conexao.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/78e46f67-4aec-4ba3-b4c4-7072cd6d921b/playlist.m3u8"
        ),

        Filme(
            "A Odisseia",
            2026,
            "Aventura",
            "https://i.postimg.cc/K8WjhML7/odisseia.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/85bf08b5-0911-4dc8-9e28-926212aec3bd/playlist.m3u8"
        ),

        Filme(
            "Resident Evil",
            2026,
            "Terror",
            "https://i.postimg.cc/3J7DtmC4/evil.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8"
        ),

        Filme(
            "Vingança",
            2026,
            "Ação",
            "https://i.postimg.cc/26M7q6P7/vinganca.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/10f5522b-3890-48cd-bfb7-89b2806e8269/playlist.m3u8"
        ),

        Filme(
            "A Revolta",
            2026,
            "Ação",
            "https://i.postimg.cc/7YyLx45D/revolta.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/ae8d06f6-e3da-4705-9a83-313fae7114f9/playlist.m3u8"
        ),

        Filme(
            "Thunderbolts",
            2026,
            "Ação",
            "https://i.postimg.cc/FzbPQdZJ/D-NQ-NP-848607-CBT107833899597-022026-O.webp",
            "https://wolf-channel-cdn.b-cdn.net/Bala%20zip/Thunderbolts.mp4"
        ),

        Filme(
            "Céu em Fúria",
            2026,
            "Ação",
            "https://i.postimg.cc/PJMnXB7d/ceu-em-furia.jpg",
            ""
        ),

        Filme(
            "Jumanji: Bem-Vindo à Selva",
            2017,
            "Aventura",
            "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
        ),

        Filme(
            "Kingsman - Serviço Secreto Dublado",
            2014,
            "Ação",
            "https://i.postimg.cc/0QDbfyzB/kings.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4"
        ),

        Filme(
            "Deu a Louca nos Bichos",
            2010,
            "Comédia",
            "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4"
        ),

        Filme(
            "Avatar",
            2009,
            "Ficção Científica",
            "https://i.postimg.cc/7LQgchYy/avatar.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/avatar-1080p.mp4"
        ),

        Filme(
            "17 Outra Vez",
            2009,
            "Comédia",
            "https://i.postimg.cc/FFwTzW46/17.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4"
        ),

        Filme(
            "Garota Infernal Dublado",
            2009,
            "Terror",
            "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4"
        ),

        Filme(
            "A Noiva Cadáver Dublado",
            2005,
            "Animação",
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
        raiz.setBackgroundColor(Color.BLACK)

        setContentView(raiz)

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

        val sombra = View(this)

        sombra.setBackgroundColor(
            Color.argb(95, 0, 0, 0)
        )

        raiz.addView(
            sombra,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val principal = LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        principal.setBackgroundColor(
            Color.TRANSPARENT
        )

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val topo = LinearLayout(this)

        topo.orientation =
            LinearLayout.HORIZONTAL

        topo.gravity =
            Gravity.CENTER_VERTICAL

        topo.setPadding(
            dp(18),
            dp(10),
            dp(18),
            dp(10)
        )

        topo.setBackgroundColor(
            Color.argb(235, 0, 0, 0)
        )

        principal.addView(
            topo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(75)
            )
        )

        val botaoMenu = TextView(this)

        botaoMenu.text = "☰"
        botaoMenu.textSize = 30f
        botaoMenu.gravity = Gravity.CENTER

        botaoMenu.setTextColor(Color.WHITE)

        botaoMenu.setBackgroundColor(
            Color.rgb(20, 20, 20)
        )

        botaoMenu.isFocusable = true
        botaoMenu.isClickable = true

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        botaoMenu.setOnFocusChangeListener {
                view,
                foco ->

            if (foco) {
                view.setBackgroundColor(
                    Color.rgb(120, 0, 0)
                )
            } else {
                view.setBackgroundColor(
                    Color.rgb(20, 20, 20)
                )
            }
        }

        topo.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(58),
                dp(58)
            )
        )

        val logo = TextView(this)

        logo.text = "WOLF CHANNEL"
        logo.textSize = 25f
        logo.typeface =
            Typeface.DEFAULT_BOLD

        logo.setTextColor(
            Color.rgb(255, 20, 30)
        )

        logo.gravity = Gravity.CENTER

        topo.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val scroll = ScrollView(this)

        scroll.isFillViewport = true

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        conteudo = LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(35)
        )

        scroll.addView(
            conteudo,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        mostrarFilmes()
    }

    private fun mostrarFilmes(
        categoria: String? = null
    ) {

        conteudo.removeAllViews()

        val lista =
            if (
                categoria == null ||
                categoria == "Todos"
            ) {
                filmes
            } else {
                filmes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            }

        if (lista.isEmpty()) {

            val vazio = TextView(this)

            vazio.text =
                "Nenhum filme encontrado nesta categoria."

            vazio.textSize = 20f
            vazio.setTextColor(Color.WHITE)
            vazio.gravity = Gravity.CENTER

            vazio.setPadding(
                dp(20),
                dp(50),
                dp(20),
                dp(50)
            )

            conteudo.addView(vazio)

            return
        }

        val larguraDp =
            resources.displayMetrics.widthPixels /
                    resources.displayMetrics.density

        val colunas =
            if (larguraDp >= 800) 5 else 2

        var linha: LinearLayout? = null
        var quantidadeNaLinha = 0

        lista.forEach { filme ->

            if (quantidadeNaLinha == 0) {

                linha = LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(410)
                    )
                )
            }

            val card =
                criarCard(filme)

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(390),
                    1f
                ).apply {
                    setMargins(
                        dp(7),
                        dp(7),
                        dp(7),
                        dp(7)
                    )
                }
            )

            quantidadeNaLinha++

            if (quantidadeNaLinha == colunas) {

                quantidadeNaLinha = 0
                linha = null
            }
        }

        if (
            quantidadeNaLinha > 0 &&
            linha != null
        ) {

            val faltam =
                colunas - quantidadeNaLinha

            repeat(faltam) {

                val espaco = View(this)

                espaco.visibility =
                    View.INVISIBLE

                espaco.isFocusable = false

                linha!!.addView(
                    espaco,
                    LinearLayout.LayoutParams(
                        0,
                        dp(390),
                        1f
                    ).apply {
                        setMargins(
                            dp(7),
                            dp(7),
                            dp(7),
                            dp(7)
                        )
                    }
                )
            }
        }
    }

    private fun criarCard(
        filme: Filme
    ): LinearLayout {

        val card = LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER_HORIZONTAL

        card.setBackgroundColor(
            Color.rgb(15, 15, 15)
        )

        card.isFocusable = true
        card.isClickable = true

        val capa = ImageView(this)

        capa.setBackgroundColor(
            Color.rgb(8, 8, 8)
        )

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            capa,
            filme.capa
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(315)
            )
        )

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.setPadding(
            dp(10),
            dp(6),
            dp(10),
            dp(5)
        )

        informacoes.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        card.addView(
            informacoes,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        )

        val titulo = TextView(this)

        titulo.text = filme.titulo
        titulo.textSize = 16f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(Color.WHITE)

        titulo.maxLines = 1

        titulo.ellipsize =
            android.text.TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(29)
            )
        )

        val detalhes = TextView(this)

        detalhes.text =
            "${filme.ano} • ${filme.categoria.lowercase()}"

        detalhes.textSize = 13f

        detalhes.setTextColor(
            Color.rgb(145, 145, 145)
        )

        informacoes.addView(
            detalhes,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            if (foco) {

                view.scaleX = 1.035f
                view.scaleY = 1.035f

                view.setBackgroundColor(
                    Color.rgb(65, 0, 0)
                )

                view.elevation =
                    dp(12).toFloat()

            } else {

                view.scaleX = 1f
                view.scaleY = 1f

                view.setBackgroundColor(
                    Color.rgb(15, 15, 15)
                )

                view.elevation = 0f
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
                "${filme.titulo}\n\nVídeo ainda não disponível."
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
    }    // ==========================================
    // MENU LATERAL
    // ==========================================

    private fun abrirMenu() {

        if (menuAberto) return

        menuAberto = true

        // Fundo escuro atrás do menu
        val fundoMenu = View(this)

        fundoMenu.setBackgroundColor(
            Color.argb(145, 0, 0, 0)
        )

        fundoMenu.isClickable = true

        fundoMenu.setOnClickListener {
            fecharMenu()
        }

        raiz.addView(
            fundoMenu,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // ==========================================
        // MENU PEQUENO NO LADO ESQUERDO
        // ==========================================

        menuLateral = LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setBackgroundColor(
            Color.rgb(10, 10, 10)
        )

        menuLateral.setPadding(
            dp(16),
            dp(18),
            dp(16),
            dp(20)
        )

        val larguraTela =
            resources.displayMetrics.widthPixels

        val larguraMenu =
            (larguraTela * 0.42f)
                .toInt()
                .coerceAtMost(dp(390))

        val menuParams =
            FrameLayout.LayoutParams(
                larguraMenu,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        menuParams.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            menuParams
        )

        // ==========================================
        // TÍTULO
        // ==========================================

        val tituloMenu = TextView(this)

        tituloMenu.text =
            "WOLF CHANNEL"

        tituloMenu.textSize = 23f

        tituloMenu.typeface =
            Typeface.DEFAULT_BOLD

        tituloMenu.setTextColor(
            Color.rgb(255, 30, 40)
        )

        tituloMenu.gravity =
            Gravity.CENTER

        menuLateral.addView(
            tituloMenu,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        adicionarSeparadorPremium()

        // ==========================================
        // FAVORITOS
        // ==========================================

        adicionarItemPremium(
            "❤️  Meus Favoritos (0)"
        ) {
            mostrarMensagem(
                "Meus Favoritos\n\nNenhum favorito adicionado."
            )
        }

        // ==========================================
        // INÍCIO
        // ==========================================

        adicionarItemPremium(
            "🏠  Início / Todos (${filmes.size})"
        ) {
            mostrarFilmes()
            fecharMenu()
        }

        adicionarSeparadorPremium()

        // ==========================================
        // FILMES
        // ==========================================

        adicionarTituloPremium(
            "🎬  Filmes (${filmes.size})"
        )

        adicionarSubtituloPremium(
            "CATEGORIAS"
        )

        adicionarCategoriaPremium(
            "🔥  Ação",
            "Ação"
        )

        adicionarCategoriaPremium(
            "🗺️  Aventura",
            "Aventura"
        )

        adicionarCategoriaPremium(
            "💀  Terror",
            "Terror"
        )

        adicionarCategoriaPremium(
            "😂  Comédia",
            "Comédia"
        )

        adicionarCategoriaPremium(
            "🎨  Animação",
            "Animação"
        )

        adicionarCategoriaPremium(
            "🚀  Ficção Científica",
            "Ficção Científica"
        )

        adicionarSeparadorPremium()

        // ==========================================
        // SÉRIES
        // ==========================================

        adicionarTituloPremium(
            "📺  Séries (0)"
        )

        adicionarSubtituloPremium(
            "CATEGORIAS"
        )

        adicionarCategoriaSerie(
            "🔥  Ação"
        )

        adicionarCategoriaSerie(
            "😂  Comédia"
        )

        adicionarCategoriaSerie(
            "🎭  Drama"
        )

        adicionarCategoriaSerie(
            "❤️  Romance"
        )

        adicionarSeparadorPremium()

        // ==========================================
        // DORAMAS
        // ==========================================

        adicionarTituloPremium(
            "🌸  Doramas (2)"
        )

        adicionarSubtituloPremium(
            "CATEGORIAS"
        )

        adicionarCategoriaDorama(
            "🔥  Ação"
        )

        adicionarCategoriaDorama(
            "❤️  Romance"
        )

        adicionarCategoriaDorama(
            "🎭  Drama"
        )

        adicionarCategoriaDorama(
            "😂  Comédia"
        )

        adicionarSeparadorPremium()

        // ==========================================
        // ANIME
        // ==========================================

        adicionarTituloPremium(
            "🍥  Anime (0)"
        )

        adicionarSubtituloPremium(
            "CATEGORIAS"
        )

        adicionarCategoriaAnime(
            "🔥  Ação"
        )

        adicionarCategoriaAnime(
            "🗺️  Aventura"
        )

        adicionarCategoriaAnime(
            "✨  Fantasia"
        )

        adicionarCategoriaAnime(
            "😂  Comédia"
        )

        // ==========================================
        // SCROLL DO MENU
        // ==========================================

        menuScroll = ScrollView(this)

        menuScroll.isFillViewport = true

        raiz.removeView(menuLateral)

        val containerMenu =
            LinearLayout(this)

        containerMenu.orientation =
            LinearLayout.VERTICAL

        containerMenu.setBackgroundColor(
            Color.rgb(10, 10, 10)
        )

        containerMenu.addView(
            menuLateral,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        menuScroll.addView(
            containerMenu,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        raiz.addView(
            menuScroll,
            menuParams
        )

        menuLateral = containerMenu

        menuScroll.post {

            val primeiro =
                encontrarPrimeiroFocavel(
                    menuScroll
                )

            primeiro?.requestFocus()
        }
    }

    // ==========================================
    // TÍTULO DO MENU
    // ==========================================

    private fun adicionarTituloPremium(
        texto: String
    ) {

        val item = TextView(this)

        item.text = texto

        item.textSize = 19f

        item.typeface =
            Typeface.DEFAULT_BOLD

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
            ).apply {
                setMargins(
                    0,
                    dp(7),
                    0,
                    dp(7)
                )
            }
        )
    }

    // ==========================================
    // SUBTÍTULO CATEGORIAS
    // ==========================================

    private fun adicionarSubtituloPremium(
        texto: String
    ) {

        val item = TextView(this)

        item.text = texto

        item.textSize = 14f

        item.typeface =
            Typeface.DEFAULT_BOLD

        item.setTextColor(
            Color.rgb(135, 135, 135)
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(35),
            0,
            0,
            0
        )

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )
    }

    // ==========================================
    // ITEM PRINCIPAL
    // ==========================================

    private fun adicionarItemPremium(
        texto: String,
        acao: () -> Unit
    ) {

        val item = TextView(this)

        item.text = texto

        item.textSize = 17f

        item.typeface =
            Typeface.DEFAULT_BOLD

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isClickable = true

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        item.setOnClickListener {
            acao()
        }

        aplicarEstiloFocoPremium(item)

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
            ).apply {
                setMargins(
                    0,
                    dp(5),
                    0,
                    dp(5)
                )
            }
        )
    }

    // ==========================================
    // CATEGORIAS DE FILMES
    // ==========================================

    private fun adicionarCategoriaPremium(
        texto: String,
        categoria: String
    ) {

        val item = TextView(this)

        item.text = texto

        item.textSize = 17f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(35),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isClickable = true

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        item.setOnClickListener {

            mostrarFilmes(categoria)

            fecharMenu()
        }

        aplicarEstiloFocoPremium(item)

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(4),
                    0,
                    dp(4)
                )
            }
        )
    }

    // ==========================================
    // CATEGORIAS DE SÉRIES
    // ==========================================

    private fun adicionarCategoriaSerie(
        texto: String
    ) {

        val item = TextView(this)

        item.text = texto
        item.textSize = 17f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(35),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isClickable = true

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        item.setOnClickListener {

            mostrarMensagem(
                "Séries\n\nNenhum conteúdo disponível nesta categoria."
            )
        }

        aplicarEstiloFocoPremium(item)

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(4),
                    0,
                    dp(4)
                )
            }
        )
    }

    // ==========================================
    // CATEGORIAS DE DORAMAS
    // ==========================================

    private fun adicionarCategoriaDorama(
        texto: String
    ) {

        val item = TextView(this)

        item.text = texto
        item.textSize = 17f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(35),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isClickable = true

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        item.setOnClickListener {

            mostrarMensagem(
                "Doramas\n\nNenhum conteúdo disponível nesta categoria."
            )
        }

        aplicarEstiloFocoPremium(item)

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(4),
                    0,
                    dp(4)
                )
            }
        )
    }

    // ==========================================
    // CATEGORIAS DE ANIME
    // ==========================================

    private fun adicionarCategoriaAnime(
        texto: String
    ) {

        val item = TextView(this)

        item.text = texto
        item.textSize = 17f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(35),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isClickable = true

        item.setBackgroundColor(
            Color.rgb(18, 18, 18)
        )

        item.setOnClickListener {

            mostrarMensagem(
                "Anime\n\nNenhum conteúdo disponível nesta categoria."
            )
        }

        aplicarEstiloFocoPremium(item)

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                setMargins(
                    0,
                    dp(4),
                    0,
                    dp(4)
                )
            }
        )
    }    // ==========================================
    // ESTILO DE FOCO DO MENU
    // ==========================================

    private fun aplicarEstiloFocoPremium(
        view: View
    ) {

        view.setOnFocusChangeListener {
                v,
                foco ->

            if (foco) {

                v.setBackgroundColor(
                    Color.rgb(65, 0, 0)
                )

                v.scaleX = 1.015f
                v.scaleY = 1.015f

                v.elevation =
                    dp(5).toFloat()

            } else {

                v.setBackgroundColor(
                    Color.rgb(18, 18, 18)
                )

                v.scaleX = 1f
                v.scaleY = 1f

                v.elevation = 0f
            }
        }
    }

    // ==========================================
    // SEPARADOR
    // ==========================================

    private fun adicionarSeparadorPremium() {

        val linha = View(this)

        linha.setBackgroundColor(
            Color.rgb(55, 55, 55)
        )

        menuLateral.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {

                setMargins(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )
            }
        )
    }

    // ==========================================
    // FECHAR MENU
    // ==========================================

    private fun fecharMenu() {

        if (!menuAberto) return

        menuAberto = false

        // A raiz possui inicialmente:
        // 1 - fundo
        // 2 - sombra
        // 3 - conteúdo principal
        //
        // Quando o menu abre:
        // 4 - fundo escuro do menu
        // 5 - menu lateral

        while (raiz.childCount > 3) {

            raiz.removeViewAt(
                raiz.childCount - 1
            )
        }

        menuLateral =
            LinearLayout(this)

        menuScroll =
            ScrollView(this)
    }

    // ==========================================
    // ENCONTRAR PRIMEIRO ITEM FOCÁVEL
    // ==========================================

    private fun encontrarPrimeiroFocavel(
        parent: ViewGroup
    ): View? {

        for (i in 0 until parent.childCount) {

            val filho =
                parent.getChildAt(i)

            if (filho.isFocusable) {
                return filho
            }

            if (filho is ViewGroup) {

                val encontrado =
                    encontrarPrimeiroFocavel(
                        filho
                    )

                if (encontrado != null) {
                    return encontrado
                }
            }
        }

        return null
    }

    // ==========================================
    // CONTROLE REMOTO / DPAD
    // ==========================================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (event.keyCode) {

                // ----------------------------------
                // BOTÃO MENU
                // ----------------------------------

                KeyEvent.KEYCODE_MENU -> {

                    if (menuAberto) {
                        fecharMenu()
                    } else {
                        abrirMenu()
                    }

                    return true
                }

                // ----------------------------------
                // BOTÃO VOLTAR
                // ----------------------------------

                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }

                // ----------------------------------
                // ENTER
                // ----------------------------------

                KeyEvent.KEYCODE_ENTER -> {

                    if (menuAberto) {

                        val foco =
                            currentFocus

                        if (
                            foco != null &&
                            foco.isClickable
                        ) {
                            foco.performClick()
                            return true
                        }
                    }
                }

                // ----------------------------------
                // OK / DPAD CENTER
                // ----------------------------------

                KeyEvent.KEYCODE_DPAD_CENTER -> {

                    if (menuAberto) {

                        val foco =
                            currentFocus

                        if (
                            foco != null &&
                            foco.isClickable
                        ) {
                            foco.performClick()
                            return true
                        }
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    // ==========================================
    // CARREGAR CAPAS
    // ==========================================

    private fun carregarImagem(
        imageView: ImageView,
        url: String
    ) {

        thread {

            try {

                val conexao =
                    URL(url).openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    15000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.inputStream.close()

                conexao.disconnect()

                if (bitmap != null) {

                    runOnUiThread {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {

                // Se a imagem não carregar,
                // o espaço continua reservado
                // para o card.
            }
        }
    }

    // ==========================================
    // MENSAGEM
    // ==========================================

    private fun mostrarMensagem(
        mensagem: String
    ) {

        Toast.makeText(
            this,
            mensagem,
            Toast.LENGTH_LONG
        ).show()
    }

    // ==========================================
    // CONVERTER DP
    // ==========================================

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }
}
