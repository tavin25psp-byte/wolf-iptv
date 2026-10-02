package com.wolf.iptv

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
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

    /*
     * FILMES
     *
     * A ordem já está por ano:
     * 2026 primeiro
     * depois 2017
     * 2014
     * 2010
     * 2009
     * 2005
     */

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
            "Ficção",
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

    /*
     * QUANDO VOCÊ ADICIONAR SÉRIES, DORAMAS OU ANIMES,
     * AS LISTAS ABAIXO RECEBERÃO OS CONTEÚDOS.
     *
     * Por enquanto a quantidade aparece como 0.
     */

    private val series = mutableListOf<Filme>()

    private val doramas = mutableListOf<Filme>()

    private val animes = mutableListOf<Filme>()

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

        raiz.setBackgroundColor(
            Color.BLACK
        )

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
                -1,
                -1
            )
        )

        val sombra = View(this)

        sombra.setBackgroundColor(
            Color.argb(
                95,
                0,
                0,
                0
            )
        )

        raiz.addView(
            sombra,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val principal =
            LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val topo =
            LinearLayout(this)

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
            Color.argb(
                235,
                0,
                0,
                0
            )
        )

        principal.addView(
            topo,
            LinearLayout.LayoutParams(
                -1,
                dp(75)
            )
        )

        val botaoMenu =
            TextView(this)

        botaoMenu.text = "☰"
        botaoMenu.textSize = 30f

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.setBackgroundColor(
            Color.rgb(
                20,
                20,
                20
            )
        )

        botaoMenu.isFocusable = true

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        botaoMenu.setOnFocusChangeListener {
                view,
                foco ->

            view.setBackgroundColor(
                if (foco)
                    Color.rgb(
                        120,
                        0,
                        0
                    )
                else
                    Color.rgb(
                        20,
                        20,
                        20
                    )
            )
        }

        topo.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(58),
                dp(58)
            )
        )

        val logo =
            TextView(this)

        logo.text =
            "WOLF CHANNEL"

        logo.textSize = 25f

        logo.typeface =
            Typeface.DEFAULT_BOLD

        logo.setTextColor(
            Color.rgb(
                255,
                20,
                30
            )
        )

        logo.gravity =
            Gravity.CENTER

        topo.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        val scroll =
            ScrollView(this)

        scroll.isFillViewport =
            true

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        conteudo =
            LinearLayout(this)

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
                -1,
                -2
            )
        )

        mostrarFilmes()
    }

    private fun mostrarFilmes(
        categoria: String? = null,
        busca: String? = null
    ) {

        conteudo.removeAllViews()

        var lista =
            filmes

        if (
            categoria != null &&
            categoria != "Todos"
        ) {

            lista =
                lista.filter {

                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
        }

        if (
            !busca.isNullOrBlank()
        ) {

            lista =
                lista.filter {

                    it.titulo.contains(
                        busca,
                        ignoreCase = true
                    )
                }
        }

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                if (
                    !busca.isNullOrBlank()
                ) {
                    "Nenhum resultado para:\n$busca"
                } else {
                    "Nenhum filme nesta categoria."
                }

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    -1,
                    dp(220)
                )
            )

            return
        }

        val larguraDp =
            resources.displayMetrics.widthPixels /
                resources.displayMetrics.density

        val colunas =
            if (larguraDp >= 800) {
                5
            } else {
                2
            }

        var linha:
            LinearLayout? = null

        var quantidade =
            0

        lista.forEach { filme ->

            if (quantidade == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(370)
                    )
                )
            }

            val card =
                criarCard(filme)

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(350),
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

            quantidade++

            if (
                quantidade ==
                colunas
            ) {

                quantidade = 0

                linha = null
            }
        }

        if (
            quantidade > 0 &&
            linha != null
        ) {

            repeat(
                colunas - quantidade
            ) {

                val espaco =
                    View(this)

                espaco.visibility =
                    View.INVISIBLE

                linha!!.addView(
                    espaco,
                    LinearLayout.LayoutParams(
                        0,
                        dp(350),
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

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER_HORIZONTAL

        card.setBackgroundColor(
            Color.rgb(
                15,
                15,
                15
            )
        )

        card.isFocusable =
            true

        card.isClickable =
            true

        val capa =
            ImageView(this)

        capa.setBackgroundColor(
            Color.rgb(
                8,
                8,
                8
            )
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
                -1,
                dp(280)
            )
        )

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(4)
        )

        informacoes.setBackgroundColor(
            Color.rgb(
                18,
                18,
                18
            )
        )

        card.addView(
            informacoes,
            LinearLayout.LayoutParams(
                -1,
                dp(62)
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.textSize =
            15f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.maxLines = 1

        titulo.ellipsize =
            android.text.TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(27)
            )
        )

        val detalhes =
            TextView(this)

        detalhes.text =
            "${filme.ano} • ${filme.categoria}"

        detalhes.textSize =
            12f

        detalhes.setTextColor(
            Color.rgb(
                145,
                145,
                145
            )
        )

        informacoes.addView(
            detalhes,
            LinearLayout.LayoutParams(
                -1,
                dp(24)
            )
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            if (foco) {

                view.scaleX =
                    1.035f

                view.scaleY =
                    1.035f

                view.setBackgroundColor(
                    Color.rgb(
                        65,
                        0,
                        0
                    )
                )

                view.elevation =
                    dp(10).toFloat()

            } else {

                view.scaleX =
                    1f

                view.scaleY =
                    1f

                view.setBackgroundColor(
                    Color.rgb(
                        15,
                        15,
                        15
                    )
                )

                view.elevation =
                    0f
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

        if (
            filme.video.isBlank()
        ) {

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
    }    private fun abrirMenu() {

        if (menuAberto) return

        menuAberto = true

        val fundoMenu =
            View(this)

        fundoMenu.setBackgroundColor(
            Color.argb(
                150,
                0,
                0,
                0
            )
        )

        fundoMenu.isFocusable =
            true

        fundoMenu.setOnClickListener {
            fecharMenu()
        }

        raiz.addView(
            fundoMenu,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(25)
        )

        menuLateral.setBackgroundColor(
            Color.rgb(
                10,
                10,
                10
            )
        )

        val larguraTela =
            resources.displayMetrics.widthPixels

        val larguraMenu =
            minOf(
                (larguraTela * 0.42f).toInt(),
                dp(390)
            )

        val containerMenu =
            FrameLayout(this)

        raiz.addView(
            containerMenu,
            FrameLayout.LayoutParams(
                larguraMenu,
                -1
            ).apply {
                gravity = Gravity.START
            }
        )

        menuScroll =
            ScrollView(this)

        menuScroll.isFillViewport =
            true

        containerMenu.addView(
            menuScroll,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        menuScroll.addView(
            menuLateral,
            ScrollView.LayoutParams(
                -1,
                -2
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF CHANNEL"

        titulo.textSize =
            25f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.rgb(
                255,
                25,
                35
            )
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        menuLateral.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        adicionarItemMenu(
            "⌂  Início"
        ) {

            fecharMenu()

            mostrarFilmes()
        }

        /*
         * PESQUISA
         */

        adicionarItemMenu(
            "🔎  Pesquisar"
        ) {

            abrirPesquisa()
        }

        /*
         * CONTINUE ASSISTINDO
         */

        adicionarItemMenu(
            "▶  Continue assistindo"
        ) {

            abrirContinueAssistindo()
        }

        /*
         * FAVORITOS
         */

        adicionarItemMenu(
            "♥  Favoritos"
        ) {

            abrirFavoritos()
        }

        /*
         * FILMES
         */

        adicionarSeparadorPremium(
            "FILMES"
        )

        adicionarCategoriaFilmes(
            "Ação"
        )

        adicionarCategoriaFilmes(
            "Aventura"
        )

        adicionarCategoriaFilmes(
            "Comédia"
        )

        adicionarCategoriaFilmes(
            "Terror"
        )

        adicionarCategoriaFilmes(
            "Animação"
        )

        adicionarCategoriaFilmes(
            "Ficção"
        )

        /*
         * SÉRIES
         */

        adicionarSeparadorPremium(
            "SÉRIES"
        )

        adicionarCategoriaSerie(
            "Ação"
        )

        adicionarCategoriaSerie(
            "Comédia"
        )

        adicionarCategoriaSerie(
            "Drama"
        )

        adicionarCategoriaSerie(
            "Romance"
        )

        /*
         * DORAMAS
         */

        adicionarSeparadorPremium(
            "DORAMAS"
        )

        adicionarCategoriaDorama(
            "Ação"
        )

        adicionarCategoriaDorama(
            "Romance"
        )

        adicionarCategoriaDorama(
            "Drama"
        )

        adicionarCategoriaDorama(
            "Comédia"
        )

        /*
         * ANIME
         */

        adicionarSeparadorPremium(
            "ANIME"
        )

        adicionarCategoriaAnime(
            "Ação"
        )

        adicionarCategoriaAnime(
            "Aventura"
        )

        adicionarCategoriaAnime(
            "Fantasia"
        )

        adicionarCategoriaAnime(
            "Comédia"
        )

        /*
         * PRIMEIRO FOCO
         */

        encontrarPrimeiroFocavel(
            menuLateral
        )?.requestFocus()
    }

    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            17f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(14),
            0,
            dp(10),
            0
        )

        item.isFocusable =
            true

        item.isClickable =
            true

        item.setBackgroundColor(
            Color.TRANSPARENT
        )

        item.setOnFocusChangeListener {
                view,
                foco ->

            view.setBackgroundColor(
                if (foco)
                    Color.rgb(
                        80,
                        0,
                        0
                    )
                else
                    Color.TRANSPARENT
            )

            if (foco) {

                view.scaleX =
                    1.02f

                view.scaleY =
                    1.02f

            } else {

                view.scaleX =
                    1f

                view.scaleY =
                    1f
            }
        }

        item.setOnClickListener {
            acao()
        }

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {

                setMargins(
                    0,
                    dp(2),
                    0,
                    dp(2)
                )
            }
        )
    }

    /*
     * CATEGORIAS DE FILMES
     * A quantidade é calculada automaticamente.
     */

    private fun adicionarCategoriaFilmes(
        categoria: String
    ) {

        val quantidade =
            filmes.count {

                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "    •  $categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarFilmes(
                categoria = categoria
            )
        }
    }

    /*
     * CATEGORIAS DE SÉRIES
     */

    private fun adicionarCategoriaSerie(
        categoria: String
    ) {

        val quantidade =
            series.count {

                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "    •  $categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                series,
                categoria,
                "Séries"
            )
        }
    }

    /*
     * CATEGORIAS DE DORAMAS
     */

    private fun adicionarCategoriaDorama(
        categoria: String
    ) {

        val quantidade =
            doramas.count {

                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "    •  $categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                doramas,
                categoria,
                "Doramas"
            )
        }
    }

    /*
     * CATEGORIAS DE ANIME
     */

    private fun adicionarCategoriaAnime(
        categoria: String
    ) {

        val quantidade =
            animes.count {

                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "    •  $categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                animes,
                categoria,
                "Anime"
            )
        }
    }

    /*
     * PESQUISA
     */

    private fun abrirPesquisa() {

        fecharMenu()

        val caixa =
            LinearLayout(this)

        caixa.orientation =
            LinearLayout.VERTICAL

        caixa.setPadding(
            dp(25),
            dp(25),
            dp(25),
            dp(25)
        )

        caixa.setBackgroundColor(
            Color.rgb(
                12,
                12,
                12
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "🔎 Pesquisar"

        titulo.textSize =
            24f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        caixa.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome do filme..."

        campo.setHintTextColor(
            Color.GRAY
        )

        campo.setTextColor(
            Color.WHITE
        )

        campo.textSize =
            18f

        campo.isFocusable =
            true

        campo.isSingleLine =
            true

        caixa.addView(
            campo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        val pesquisar =
            TextView(this)

        pesquisar.text =
            "PESQUISAR"

        pesquisar.textSize =
            17f

        pesquisar.typeface =
            Typeface.DEFAULT_BOLD

        pesquisar.gravity =
            Gravity.CENTER

        pesquisar.setTextColor(
            Color.WHITE
        )

        pesquisar.setBackgroundColor(
            Color.rgb(
                100,
                0,
                0
            )
        )

        pesquisar.isFocusable =
            true

        pesquisar.setOnClickListener {

            val texto =
                campo.text
                    .toString()
                    .trim()

            if (texto.isBlank()) {

                mostrarMensagem(
                    "Digite o nome que deseja pesquisar."
                )

                return@setOnClickListener
            }

            mostrarFilmes(
                busca = texto
            )
        }

        caixa.addView(
            pesquisar,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            ).apply {

                setMargins(
                    0,
                    dp(15),
                    0,
                    0
                )
            }
        )

        val voltar =
            TextView(this)

        voltar.text =
            "VOLTAR"

        voltar.textSize =
            16f

        voltar.gravity =
            Gravity.CENTER

        voltar.setTextColor(
            Color.WHITE
        )

        voltar.isFocusable =
            true

        voltar.setOnClickListener {
            mostrarFilmes()
        }

        caixa.addView(
            voltar,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {

                setMargins(
                    0,
                    dp(8),
                    0,
                    0
                )
            }
        )

        conteudo.removeAllViews()

        conteudo.addView(
            caixa,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        campo.requestFocus()
    }

    /*
     * MOSTRA SÉRIES / DORAMAS / ANIME
     * QUANDO EXISTIREM NA LISTA.
     */

    private fun mostrarConteudoEspecial(
        lista: List<Filme>,
        categoria: String,
        nomeTipo: String
    ) {

        conteudo.removeAllViews()

        val filtrados =
            lista.filter {

                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        if (filtrados.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "$nomeTipo • $categoria\n\nNenhum conteúdo cadastrado ainda."

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    -1,
                    dp(250)
                )
            )

            return
        }

        mostrarListaCards(
            filtrados
        )
    }

    /*
     * LISTA DE CARDS PARA QUALQUER TIPO DE CONTEÚDO
     */

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        val larguraDp =
            resources.displayMetrics.widthPixels /
                resources.displayMetrics.density

        val colunas =
            if (larguraDp >= 800) {
                5
            } else {
                2
            }

        var linha:
            LinearLayout? = null

        var quantidade =
            0

        lista.forEach { item ->

            if (quantidade == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(370)
                    )
                )
            }

            linha!!.addView(
                criarCard(item),
                LinearLayout.LayoutParams(
                    0,
                    dp(350),
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

            quantidade++

            if (
                quantidade ==
                colunas
            ) {

                quantidade = 0
                linha = null
            }
        }

        if (
            quantidade > 0 &&
            linha != null
        ) {

            repeat(
                colunas - quantidade
            ) {

                val espaco =
                    View(this)

                espaco.visibility =
                    View.INVISIBLE

                linha!!.addView(
                    espaco,
                    LinearLayout.LayoutParams(
                        0,
                        dp(350),
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
    }    /*
     * CONTINUE ASSISTINDO
     *
     * O PlayerActivity salva:
     * - título
     * - URL
     * - posição
     *
     * Aqui recuperamos esses dados.
     */

    private fun abrirContinueAssistindo() {

        fecharMenu()

        val prefs =
            getSharedPreferences(
                "wolf_continue",
                MODE_PRIVATE
            )

        val titulo =
            prefs.getString(
                "titulo",
                null
            )

        val url =
            prefs.getString(
                "url",
                null
            )

        val posicao =
            prefs.getLong(
                "posicao",
                0L
            )

        if (
            titulo.isNullOrBlank() ||
            url.isNullOrBlank() ||
            posicao <= 0L
        ) {

            mostrarMensagem(
                "Você ainda não tem vídeos para continuar."
            )

            return
        }

        val filme =
            filmes.firstOrNull {

                it.video == url
            }

        if (filme != null) {

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

            intent.putExtra(
                "VIDEO_POSITION",
                posicao
            )

            startActivity(intent)

        } else {

            val intent =
                android.content.Intent(
                    this,
                    PlayerActivity::class.java
                )

            intent.putExtra(
                "VIDEO_URL",
                url
            )

            intent.putExtra(
                "VIDEO_TITLE",
                titulo
            )

            intent.putExtra(
                "VIDEO_POSITION",
                posicao
            )

            startActivity(intent)
        }
    }

    /*
     * FAVORITOS
     *
     * Mantido preparado para os filmes
     * serem salvos individualmente.
     */

    private fun abrirFavoritos() {

        fecharMenu()

        val prefs =
            getSharedPreferences(
                "wolf_favoritos",
                MODE_PRIVATE
            )

        val favoritos =
            filmes.filter {

                prefs.getBoolean(
                    it.video,
                    false
                )
            }

        if (favoritos.isEmpty()) {

            mostrarMensagem(
                "Você ainda não adicionou filmes aos favoritos."
            )

            return
        }

        mostrarListaCards(
            favoritos
        )
    }

    /*
     * SEPARADORES DO MENU
     */

    private fun adicionarSeparadorPremium(
        texto: String
    ) {

        val separador =
            TextView(this)

        separador.text =
            texto

        separador.textSize =
            13f

        separador.typeface =
            Typeface.DEFAULT_BOLD

        separador.setTextColor(
            Color.rgb(
                255,
                30,
                40
            )
        )

        separador.gravity =
            Gravity.CENTER_VERTICAL

        separador.setPadding(
            dp(10),
            0,
            0,
            0
        )

        menuLateral.addView(
            separador,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            ).apply {

                setMargins(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )
    }

    /*
     * FECHAR MENU
     */

    private fun fecharMenu() {

        if (!menuAberto) return

        menuAberto = false

        while (
            raiz.childCount > 3
        ) {

            raiz.removeViewAt(
                raiz.childCount - 1
            )
        }

        menuLateral =
            LinearLayout(this)

        menuScroll =
            ScrollView(this)
    }

    /*
     * ENCONTRA O PRIMEIRO ITEM FOCÁVEL
     */

    private fun encontrarPrimeiroFocavel(
        view: View
    ): View? {

        if (
            view.isFocusable &&
            view.visibility ==
            View.VISIBLE
        ) {
            return view
        }

        if (
            view is ViewGroup
        ) {

            for (
                i in 0 until view.childCount
            ) {

                val resultado =
                    encontrarPrimeiroFocavel(
                        view.getChildAt(i)
                    )

                if (resultado != null) {
                    return resultado
                }
            }
        }

        return null
    }

    /*
     * CONTROLE DO D-PAD
     */

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (
                event.keyCode
            ) {

                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }

                KeyEvent.KEYCODE_MENU -> {

                    if (!menuAberto) {
                        abrirMenu()
                    } else {
                        fecharMenu()
                    }

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    /*
     * CARREGAMENTO DAS CAPAS
     */

    private fun carregarImagem(
        imageView: ImageView,
        urlString: String
    ) {

        thread {

            try {

                val url =
                    URL(urlString)

                val conexao =
                    url.openConnection()
                        as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    15000

                conexao.doInput =
                    true

                conexao.connect()

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.disconnect()

                runOnUiThread {

                    if (
                        bitmap != null
                    ) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (
                e: Exception
            ) {

                runOnUiThread {

                    imageView.setBackgroundColor(
                        Color.rgb(
                            25,
                            25,
                            25
                        )
                    )
                }
            }
        }
    }

    /*
     * MENSAGEM
     */

    private fun mostrarMensagem(
        texto: String
    ) {

        Toast.makeText(
            this,
            texto,
            Toast.LENGTH_LONG
        ).show()
    }

    /*
     * DP
     */

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
            resources.displayMetrics.density
        ).toInt()
    }
}
