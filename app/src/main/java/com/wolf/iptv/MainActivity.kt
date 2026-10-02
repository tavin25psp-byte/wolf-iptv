package com.wolf.iptv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.FocusFinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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

data class Filme(
    val titulo: String,
    val ano: Int,
    val categoria: String,
    val capa: String,
    val video: String
)

data class Episodio(
    val numero: Int,
    val titulo: String,
    val video: String
)

data class Temporada(
    val numero: Int,
    val episodios: List<Episodio>
)

data class Serie(
    val titulo: String,
    val categoria: String,
    val capa: String,
    val temporadas: List<Temporada>
)

class MainActivity : AppCompatActivity() {

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var botaoMenu: TextView

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView
    private lateinit var menuConteudo: LinearLayout

    private var menuAberto = false

    private val itensMenuFoco =
        mutableListOf<View>()

    private val cacheCapas =
        HashMap<String, Bitmap>()

    private val historicoConteudo =
        mutableListOf<() -> Unit>()

    private val filmes =
        mutableListOf<Filme>()

    private val series =
        mutableListOf<Serie>()

    private val doramas =
        mutableListOf<Serie>()

    private val animes =
        mutableListOf<Serie>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        criarInterface()

        carregarFilmes()

        carregarSeries()

        mostrarListaCards(
            filmes
        )
    }

    private fun criarInterface() {

        raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        setContentView(raiz)

        val fundo =
            ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        fundo.alpha = 0.55f

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        carregarImagem(
            fundo,
            "https://i.postimg.cc/Ghk8PP7w/wolf.png"
        )

        val camada =
            LinearLayout(this)

        camada.orientation =
            LinearLayout.VERTICAL

        camada.setPadding(
            dp(20),
            dp(12),
            dp(20),
            dp(12)
        )

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        botaoMenu =
            TextView(this)

        botaoMenu.text =
            "☰  WOLF MENU"

        botaoMenu.textSize = 18f

        botaoMenu.typeface =
            Typeface.DEFAULT_BOLD

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable = true
        botaoMenu.isFocusableInTouchMode =
            true

        botaoMenu.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        botaoMenu.setBackgroundColor(
            Color.TRANSPARENT
        )

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        camada.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(220),
                dp(55)
            )
        )

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.isFocusable = false

        val scroll =
            ScrollView(this)

        scroll.isFocusable = false

        scroll.addView(
            conteudo,
            ViewGroup.LayoutParams(
                -1,
                -2
            )
        )

        camada.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )
    }

    private fun carregarFilmes() {

        filmes.clear()

        filmes.addAll(
            listOf(

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
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Todo%20Mundo%20em%20P%C3%A2nico.mp4"
                ),

                Filme(
                    "Pânico 7",
                    2026,
                    "Terror",
                    "https://i.postimg.cc/FzcqGbHV/images-(3).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/P%C3%A2nico%207.mp4"
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
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Como%20Treinar%20o%20Seu%20Drag%C3%A3o.mp4"
                ),

                Filme(
                    "Quarteto Fantástico: Primeiro Passo",
                    2026,
                    "Ação",
                    "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Quarteto%20Fant%C3%A1stico%20Primeiros%20Passos.mp4"
                ),

                Filme(
                    "Homem-Aranha: Um Novo Dia",
                    2026,
                    "Ação",
                    "https://i.postimg.cc/QCctbsqF/aranha.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filme%20hospedagem%20/Homem-Aranha%20Um%20Novo%20Dia.mp4"
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
                    "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/BALA-CHANNEL/A%20Odisseia.mp4"
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
                    "Super Mario Galaxy: O Filme",
                    2026,
                    "Animação",
                    "https://i.postimg.cc/W3Y1mvHd/images-(2).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Super%20Mario%20Galaxy%20O%20Filme.mp4"
                ),

                Filme(
                    "Mufasa: O Rei Leão",
                    2024,
                    "Aventura",
                    "https://i.postimg.cc/mgw3xmpz/917q-7O0TJL.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Mufasa%20O%20Rei%20Le%C3%A3o.mp4"
                ),

                Filme(
                    "Coringa: Delírio a Dois",
                    2024,
                    "Drama",
                    "https://i.postimg.cc/LXN621K3/MV5BZTU0ZGI3Yz-Mt-ZTUw-MC00MGJj-LWFk-NDIt-MDUz-NTUx-Zjg5N2Y4Xk-Ey-Xk-Fqc-Gc-V1.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Coringa%20Del%C3%ADrio%20a%20Dois.mp4"
                ),

                Filme(
                    "Deadpool & Wolverine",
                    2024,
                    "Ação",
                    "https://i.postimg.cc/2SZ6hyv9/IMG-20261002-100134.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Deadpool%20%26amp%3B%20Wolverine.mp4"
                ),

                Filme(
                    "Bad Boys: Até o Fim",
                    2024,
                    "Ação",
                    "https://i.postimg.cc/sDv6PQmF/images-(4).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Bad%20Boys%20At%C3%A9%20o%20Fim.mp4"
                ),

                Filme(
                    "Furiosa: Uma Saga Mad Max",
                    2024,
                    "Ação",
                    "https://i.postimg.cc/FzZY7Hfn/71K2Mcc-CQ2L-AC-UF894-1000-QL80.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Furiosa%20Uma%20Saga%20Mad%20Max.mp4"
                ),

                Filme(
                    "Meu Malvado Favorito 4",
                    2024,
                    "Animação",
                    "",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Meu%20Malvado%20Favorito%204.mp4"
                ),

                Filme(
                    "Jumanji: Bem-Vindo à Selva",
                    2017,
                    "Aventura",
                    "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
                ),

                Filme(
                    "Kingsman: Agente Secreto",
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
                    "Garota Infernal",
                    2009,
                    "Terror",
                    "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4"
                ),

                Filme(
                    "A Noiva Cadáver",
                    2005,
                    "Animação",
                    "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4"
                )
            )
        )

        filmes.sortWith(
            compareByDescending<Filme> {
                it.ano
            }.thenBy {
                it.titulo
            }
        )
    }    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        historicoConteudo.clear()

        if (lista.isEmpty()) {
            mostrarMensagem(
                "Nenhum conteúdo encontrado."
            )
            return
        }

        val colunas = 5

        var linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.TOP

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                -1,
                dp(320)
            )
        )

        lista.forEachIndexed {
                indice,
                filme ->

            if (
                indice > 0 &&
                indice % colunas == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(320)
                    )
                )
            }

            val card =
                criarCard(filme)

            linha.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(310),
                    1f
                ).apply {
                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }
    }

    private fun criarCard(
        filme: Filme
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        val fundoNormal =
            GradientDrawable()

        fundoNormal.cornerRadius =
            dp(10).toFloat()

        fundoNormal.setColor(
            Color.argb(
                190,
                10,
                10,
                10
            )
        )

        card.background =
            fundoNormal

        val areaCapa =
            FrameLayout(this)

        val capa =
            ImageView(this)

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        capa.setBackgroundColor(
            Color.TRANSPARENT
        )

        areaCapa.addView(
            capa,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        carregarImagem(
            capa,
            filme.capa
        )

        val ano =
            TextView(this)

        ano.text =
            filme.ano.toString()

        ano.textSize = 14f

        ano.typeface =
            Typeface.DEFAULT_BOLD

        ano.setTextColor(
            Color.WHITE
        )

        ano.gravity =
            Gravity.CENTER

        val fundoAno =
            GradientDrawable()

        fundoAno.cornerRadius =
            dp(6).toFloat()

        fundoAno.setColor(
            Color.argb(
                220,
                0,
                0,
                0
            )
        )

        fundoAno.setStroke(
            dp(1),
            Color.RED
        )

        ano.background =
            fundoAno

        areaCapa.addView(
            ano,
            FrameLayout.LayoutParams(
                dp(58),
                dp(32)
            ).apply {
                gravity =
                    Gravity.TOP or
                    Gravity.END

                setMargins(
                    0,
                    dp(7),
                    dp(7),
                    0
                )
            }
        )

        card.addView(
            areaCapa,
            LinearLayout.LayoutParams(
                -1,
                dp(255)
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.textSize = 13f

        titulo.maxLines = 2

        titulo.gravity =
            Gravity.CENTER

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(45)
            )
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            val bg =
                GradientDrawable()

            bg.cornerRadius =
                dp(10).toFloat()

            if (foco) {

                bg.setColor(
                    Color.rgb(
                        70,
                        0,
                        0
                    )
                )

                bg.setStroke(
                    dp(4),
                    Color.RED
                )

                view.scaleX = 1.03f
                view.scaleY = 1.03f

            } else {

                bg.setColor(
                    Color.argb(
                        190,
                        10,
                        10,
                        10
                    )
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }

            view.background =
                bg
        }

        card.setOnClickListener {
            abrirVideo(filme)
        }

        return card
    }

    private fun mostrarFilmes(
        categoria: String? = null,
        busca: String? = null
    ) {

        var lista =
            filmes.toList()

        if (
            !categoria.isNullOrBlank()
        ) {

            lista =
                lista.filter {
                    it.categoria.equals(
                        categoria,
                        true
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
                        true
                    )
                }
        }

        lista =
            lista.sortedWith(
                compareByDescending<Filme> {
                    it.ano
                }.thenBy {
                    it.titulo
                }
            )

        mostrarListaCards(
            lista
        )

        if (lista.isEmpty()) {

            mostrarMensagem(
                "Nenhum filme encontrado."
            )
        }
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
            Intent(
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
            "VIDEO_COVER",
            filme.capa
        )

        startActivity(intent)
    }

    private fun abrirMenu() {

        if (menuAberto) return

        menuAberto = true
        itensMenuFoco.clear()

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.argb(
                248,
                5,
                5,
                5
            )
        )

        fundo.setStroke(
            dp(2),
            Color.rgb(
                100,
                0,
                0
            )
        )

        menuLateral.background =
            fundo

        val larguraDp =
            resources.displayMetrics.widthPixels /
            resources.displayMetrics.density

        val larguraMenu =
            if (larguraDp >= 800) {
                dp(350)
            } else {
                dp(310)
            }

        val parametros =
            FrameLayout.LayoutParams(
                larguraMenu,
                -1
            )

        parametros.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            parametros
        )

        val topoMenu =
            LinearLayout(this)

        topoMenu.orientation =
            LinearLayout.HORIZONTAL

        topoMenu.gravity =
            Gravity.CENTER_VERTICAL

        val tituloMenu =
            TextView(this)

        tituloMenu.text =
            "MENU"

        tituloMenu.textSize = 22f

        tituloMenu.typeface =
            Typeface.DEFAULT_BOLD

        tituloMenu.setTextColor(
            Color.WHITE
        )

        topoMenu.addView(
            tituloMenu,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        val fecharX =
            TextView(this)

        fecharX.text =
            "✕"

        fecharX.textSize = 32f

        fecharX.gravity =
            Gravity.CENTER

        fecharX.setTextColor(
            Color.WHITE
        )

        fecharX.isFocusable = true
        fecharX.isFocusableInTouchMode =
            true
        fecharX.isClickable = true

        fecharX.setOnClickListener {
            fecharMenu()
        }

        fecharX.setOnFocusChangeListener {
                view,
                foco ->

            val bg =
                GradientDrawable()

            bg.cornerRadius =
                dp(12).toFloat()

            if (foco) {

                bg.setColor(
                    Color.rgb(
                        65,
                        0,
                        0
                    )
                )

                bg.setStroke(
                    dp(3),
                    Color.RED
                )

            } else {

                bg.setColor(
                    Color.TRANSPARENT
                )
            }

            view.background =
                bg
        }

        topoMenu.addView(
            fecharX,
            LinearLayout.LayoutParams(
                dp(70),
                dp(55)
            )
        )

        menuLateral.addView(
            topoMenu,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        registrarFocoMenu(
            fecharX
        )

        adicionarSeparadorPremium(
            menuLateral
        )

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable =
            false

        menuScroll.isFocusableInTouchMode =
            false

        menuScroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

        menuConteudo.isFocusable =
            false

        menuConteudo.isFocusableInTouchMode =
            false

        menuScroll.addView(
            menuConteudo,
            ViewGroup.LayoutParams(
                -1,
                -2
            )
        )

        menuLateral.addView(
            menuScroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        adicionarItemMenu(
            "▶  Continue assistindo"
        ) {

            fecharMenu()
            mostrarContinueAssistindo()
        }

        adicionarItemMenu(
            "♥  Favoritos"
        ) {

            fecharMenu()
            mostrarFavoritos()
        }

        adicionarItemMenu(
            "🔎  Pesquisa"
        ) {

            fecharMenu()
            abrirPesquisa()
        }

        adicionarSeparadorPremium(
            menuConteudo
        )

        adicionarCabecalho(
            "FILMES",
            filmes.size
        )

        adicionarItemMenu(
            "   TODOS (${filmes.size})"
        ) {

            fecharMenu()

            mostrarFilmes()
        }

        val categoriasFilmes =
            listOf(
                "Ação",
                "Aventura",
                "Comédia",
                "Terror",
                "Animação",
                "Ficção"
            )

        categoriasFilmes.forEach {
            categoria ->

            adicionarItemMenu(
                "   $categoria (${contar(
                    filmes,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarFilmes(
                    categoria = categoria
                )
            }
    }        adicionarSeparadorPremium(
            menuConteudo
        )

        adicionarCabecalho(
            "SÉRIES",
            series.size
        )

        adicionarItemMenu(
            "   TODOS (${series.size})"
        ) {

            fecharMenu()

            mostrarSeries(
                series
            )
        }

        val categoriasSeries =
            listOf(
                "Ação",
                "Comédia",
                "Drama",
                "Romance"
            )

        categoriasSeries.forEach {
            categoria ->

            adicionarItemMenu(
                "   $categoria (${contarSeries(
                    series,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarSeries(
                    series.filter {
                        it.categoria.equals(
                            categoria,
                            true
                        )
                    }
                )
            }
        }

        adicionarSeparadorPremium(
            menuConteudo
        )

        adicionarCabecalho(
            "DORAMAS",
            doramas.size
        )

        adicionarItemMenu(
            "   TODOS (${doramas.size})"
        ) {

            fecharMenu()

            mostrarSeries(
                doramas
            )
        }

        val categoriasDoramas =
            listOf(
                "Ação",
                "Romance",
                "Drama",
                "Comédia"
            )

        categoriasDoramas.forEach {
            categoria ->

            adicionarItemMenu(
                "   $categoria (${contarSeries(
                    doramas,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarSeries(
                    doramas.filter {
                        it.categoria.equals(
                            categoria,
                            true
                        )
                    }
                )
            }
        }

        adicionarSeparadorPremium(
            menuConteudo
        )

        adicionarCabecalho(
            "ANIME",
            animes.size
        )

        adicionarItemMenu(
            "   TODOS (${animes.size})"
        ) {

            fecharMenu()

            mostrarSeries(
                animes
            )
        }

        val categoriasAnime =
            listOf(
                "Ação",
                "Aventura",
                "Fantasia",
                "Comédia"
            )

        categoriasAnime.forEach {
            categoria ->

            adicionarItemMenu(
                "   $categoria (${contarSeries(
                    animes,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarSeries(
                    animes.filter {
                        it.categoria.equals(
                            categoria,
                            true
                        )
                    }
                )
            }
        }

        adicionarSeparadorPremium(
            menuConteudo
        )

        adicionarItemMenu(
            "✕  FECHAR MENU"
        ) {

            fecharMenu()
        }

        if (
            itensMenuFoco.size > 1
        ) {

            itensMenuFoco[1]
                .requestFocus()

        } else {

            fecharX.requestFocus()
        }
    }

    private fun adicionarCabecalho(
        texto: String,
        quantidade: Int
    ) {

        val item =
            TextView(this)

        item.text =
            "$texto  ($quantidade)"

        item.textSize = 17f

        item.typeface =
            Typeface.DEFAULT_BOLD

        item.setTextColor(
            Color.rgb(
                255,
                70,
                70
            )
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8)
        )

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )
    }

    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize = 15f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(14),
            0,
            dp(8),
            0
        )

        item.isFocusable = true
        item.isFocusableInTouchMode =
            true

        item.isClickable = true

        item.setOnFocusChangeListener {
                view,
                foco ->

            val bg =
                GradientDrawable()

            bg.cornerRadius =
                dp(8).toFloat()

            if (foco) {

                bg.setColor(
                    Color.rgb(
                        65,
                        0,
                        0
                    )
                )

                bg.setStroke(
                    dp(2),
                    Color.RED
                )

                view.scaleX = 1.02f
                view.scaleY = 1.02f

                view.post {

                    menuScroll
                        .smoothScrollTo(
                            0,
                            view.top
                        )
                }

            } else {

                bg.setColor(
                    Color.TRANSPARENT
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }

            view.background =
                bg
        }

        item.setOnClickListener {
            acao()
        }

        item.setOnKeyListener {
                _,
                keyCode,
                event ->

            if (
                event.action ==
                KeyEvent.ACTION_DOWN &&
                (
                    keyCode ==
                    KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode ==
                    KeyEvent.KEYCODE_ENTER
                )
            ) {

                acao()

                true

            } else {

                false
            }
        }

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        registrarFocoMenu(
            item
        )
    }

    private fun registrarFocoMenu(
        view: View
    ) {

        itensMenuFoco.add(view)
    }

    private fun adicionarSeparadorPremium(
        destino: ViewGroup
    ) {

        val linha =
            View(this)

        linha.setBackgroundColor(
            Color.rgb(
                70,
                70,
                70
            )
        )

        val params =
            LinearLayout.LayoutParams(
                -1,
                dp(1)
            )

        params.setMargins(
            dp(5),
            dp(7),
            dp(5),
            dp(7)
        )

        destino.addView(
            linha,
            params
        )
    }

    private fun fecharMenu() {

        if (!menuAberto) return

        if (
            ::menuLateral.isInitialized
        ) {

            raiz.removeView(
                menuLateral
            )
        }

        menuAberto = false

        itensMenuFoco.clear()

        encontrarPrimeiroFocavel(
            conteudo
        )?.requestFocus()
    }

    private fun mostrarSeries(
        listaOriginal: List<Serie>
    ) {

        conteudo.removeAllViews()

        if (listaOriginal.isEmpty()) {

            mostrarMensagem(
                "Nenhuma série encontrada."
            )

            return
        }

        val lista =
            listaOriginal.sortedBy {
                it.titulo
            }

        val colunas = 5

        var linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.TOP

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                -1,
                dp(320)
            )
        )

        lista.forEachIndexed {
                indice,
                serie ->

            if (
                indice > 0 &&
                indice % colunas == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(320)
                    )
                )
            }

            val card =
                criarCardSerie(
                    serie
                )

            linha.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(310),
                    1f
                ).apply {

                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }
    }

    private fun criarCardSerie(
        serie: Serie
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        val fundo =
            GradientDrawable()

        fundo.cornerRadius =
            dp(10).toFloat()

        fundo.setColor(
            Color.argb(
                190,
                10,
                10,
                10
            )
        )

        card.background =
            fundo

        val capa =
            ImageView(this)

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            capa,
            serie.capa
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                -1,
                dp(255)
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize = 13f

        titulo.maxLines = 2

        titulo.gravity =
            Gravity.CENTER

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(45)
            )
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            val bg =
                GradientDrawable()

            bg.cornerRadius =
                dp(10).toFloat()

            if (foco) {

                bg.setColor(
                    Color.rgb(
                        70,
                        0,
                        0
                    )
                )

                bg.setStroke(
                    dp(4),
                    Color.RED
                )

                view.scaleX = 1.03f
                view.scaleY = 1.03f

            } else {

                bg.setColor(
                    Color.argb(
                        190,
                        10,
                        10,
                        10
                    )
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }

            view.background =
                bg
        }

        card.setOnClickListener {

            mostrarTemporadas(
                serie
            )
        }

        return card
    }

    private fun mostrarTemporadas(
        serie: Serie
    ) {

        conteudo.removeAllViews()

        if (serie.temporadas.isEmpty()) {

            mostrarMensagem(
                "Nenhuma temporada encontrada."
            )

            return
        }

        val titulo =
            TextView(this)

        titulo.text =
            "‹  ${serie.titulo}"

        titulo.textSize = 22f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.isFocusable = true
        titulo.isFocusableInTouchMode =
            true
        titulo.isClickable = true

        titulo.setOnClickListener {

            mostrarSeries(
                listOf(serie)
            )
        }

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        serie.temporadas.forEach {
            temporada ->

            val item =
                TextView(this)

            item.text =
                "Temporada ${temporada.numero}"

            item.textSize = 18f

            item.typeface =
                Typeface.DEFAULT_BOLD

            item.setTextColor(
                Color.WHITE
            )

            item.gravity =
                Gravity.CENTER_VERTICAL

            item.setPadding(
                dp(20),
                0,
                dp(10),
                0
            )

            item.isFocusable = true
            item.isFocusableInTouchMode =
                true
            item.isClickable = true

            item.setOnFocusChangeListener {
                    view,
                    foco ->

                val bg =
                    GradientDrawable()

                bg.cornerRadius =
                    dp(10).toFloat()

                if (foco) {

                    bg.setColor(
                        Color.rgb(
                            70,
                            0,
                            0
                        )
                    )

                    bg.setStroke(
                        dp(3),
                        Color.RED
                    )

                } else {

                    bg.setColor(
                        Color.argb(
                            180,
                            10,
                            10,
                            10
                        )
                    )
                }

                view.background =
                    bg
            }

            item.setOnClickListener {

                mostrarEpisodios(
                    serie,
                    temporada
                )
            }

            conteudo.addView(
                item,
                LinearLayout.LayoutParams(
                    -1,
                    dp(60)
                ).apply {
                    setMargins(
                        dp(10),
                        dp(5),
                        dp(10),
                        dp(5)
                    )
                }
            )
        }

        encontrarPrimeiroFocavel(
            conteudo
        )?.requestFocus()
    }    private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "‹  ${serie.titulo} • Temporada ${temporada.numero}"

        titulo.textSize = 20f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.isFocusable = true
        titulo.isFocusableInTouchMode =
            true
        titulo.isClickable = true

        titulo.setOnClickListener {

            mostrarTemporadas(
                serie
            )
        }

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        val colunas = 5

        var linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.TOP

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                -1,
                dp(220)
            )
        )

        temporada.episodios.forEachIndexed {
                indice,
                episodio ->

            if (
                indice > 0 &&
                indice % colunas == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(220)
                    )
                )
            }

            val card =
                criarCardEpisodio(
                    serie,
                    temporada,
                    episodio
                )

            linha.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(210),
                    1f
                ).apply {
                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }

        encontrarPrimeiroFocavel(
            conteudo
        )?.requestFocus()
    }

    private fun criarCardEpisodio(
        serie: Serie,
        temporada: Temporada,
        episodio: Episodio
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        val bgNormal =
            GradientDrawable()

        bgNormal.cornerRadius =
            dp(12).toFloat()

        bgNormal.setColor(
            Color.argb(
                200,
                15,
                15,
                15
            )
        )

        card.background =
            bgNormal

        val numero =
            TextView(this)

        numero.text =
            "EP ${episodio.numero}"

        numero.textSize = 25f

        numero.gravity =
            Gravity.CENTER

        numero.setTextColor(
            Color.RED
        )

        numero.typeface =
            Typeface.DEFAULT_BOLD

        card.addView(
            numero,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        val nome =
            TextView(this)

        nome.text =
            episodio.titulo

        nome.textSize = 14f

        nome.gravity =
            Gravity.CENTER

        nome.maxLines = 3

        nome.setTextColor(
            Color.WHITE
        )

        nome.typeface =
            Typeface.DEFAULT_BOLD

        card.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            val bg =
                GradientDrawable()

            bg.cornerRadius =
                dp(12).toFloat()

            if (foco) {

                bg.setColor(
                    Color.rgb(
                        70,
                        0,
                        0
                    )
                )

                bg.setStroke(
                    dp(4),
                    Color.RED
                )

                view.scaleX = 1.03f
                view.scaleY = 1.03f

            } else {

                bg.setColor(
                    Color.argb(
                        200,
                        15,
                        15,
                        15
                    )
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }

            view.background =
                bg
        }

        card.setOnClickListener {

            abrirEpisodio(
                serie,
                temporada,
                episodio
            )
        }

        return card
    }

    private fun abrirEpisodio(
        serie: Serie,
        temporada: Temporada,
        episodio: Episodio
    ) {

        if (
            episodio.video.isBlank()
        ) {

            mostrarMensagem(
                "O episódio ${episodio.numero} ainda não possui link."
            )

            return
        }

        val intent =
            Intent(
                this,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "VIDEO_URL",
            episodio.video
        )

        intent.putExtra(
            "VIDEO_TITLE",
            "${serie.titulo} • ${temporada.numero}ª Temporada • EP ${episodio.numero} - ${episodio.titulo}"
        )

        intent.putExtra(
            "VIDEO_COVER",
            serie.capa
        )

        startActivity(intent)
    }

    private fun mostrarFavoritos() {

        val lista =
            filmes.filter {
                ehFavorito(it)
            }

        mostrarListaCards(
            lista
        )

        if (lista.isEmpty()) {

            mostrarMensagem(
                "Você ainda não possui favoritos."
            )
        }
    }

    private fun mostrarContinueAssistindo() {

        val prefs =
            getSharedPreferences(
                "wolf_progress",
                MODE_PRIVATE
            )

        val lista =
            filmes
                .filter { filme ->

                    prefs.getLong(
                        filme.video,
                        0L
                    ) > 0L
                }
                .sortedByDescending { filme ->

                    prefs.getLong(
                        "${filme.video}_time",
                        0L
                    )
                }

        mostrarListaCards(
            lista
        )

        if (lista.isEmpty()) {

            mostrarMensagem(
                "Ainda não há vídeos para continuar assistindo."
            )
        }
    }

    private fun abrirPesquisa() {

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome do filme"

        campo.setTextColor(
            Color.WHITE
        )

        campo.setHintTextColor(
            Color.LTGRAY
        )

        campo.setSingleLine(true)

        val dialog =
            android.app.AlertDialog
                .Builder(this)
                .setTitle(
                    "Pesquisar"
                )
                .setView(
                    campo
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Pesquisar"
                ) { _, _ ->

                    mostrarFilmes(
                        busca =
                            campo.text
                                .toString()
                                .trim()
                    )
                }
                .create()

        dialog.show()
    }

    private fun contar(
        lista: List<Filme>,
        categoria: String
    ): Int {

        return lista.count {
            it.categoria.equals(
                categoria,
                true
            )
        }
    }

    private fun contarSeries(
        lista: List<Serie>,
        categoria: String
    ): Int {

        return lista.count {
            it.categoria.equals(
                categoria,
                true
            )
        }
    }

    private fun ehFavorito(
        filme: Filme
    ): Boolean {

        return getSharedPreferences(
            "wolf_favoritos",
            MODE_PRIVATE
        ).getBoolean(
            filme.video,
            false
        )
    }

    private fun alternarFavorito(
        filme: Filme
    ) {

        val prefs =
            getSharedPreferences(
                "wolf_favoritos",
                MODE_PRIVATE
            )

        val novoEstado =
            !prefs.getBoolean(
                filme.video,
                false
            )

        prefs.edit()
            .putBoolean(
                filme.video,
                novoEstado
            )
            .apply()
    }

    private fun voltarConteudoEspecial(): Boolean {

        if (
            historicoConteudo.isEmpty()
        ) {

            return false
        }

        val ultima =
            historicoConteudo
                .removeAt(
                    historicoConteudo.lastIndex
                )

        ultima()

        return true
    }

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

                val encontrado =
                    encontrarPrimeiroFocavel(
                        view.getChildAt(i)
                    )

                if (
                    encontrado != null
                ) {

                    return encontrado
                }
            }
        }

        return null
    }

    private fun estaDentroDoMenu(
        view: View?
    ): Boolean {

        if (
            view == null ||
            !::menuLateral.isInitialized
        ) {

            return false
        }

        var atual: View? = view

        while (
            atual != null
        ) {

            if (
                atual === menuLateral
            ) {

                return true
            }

            atual =
                atual.parent as? View
        }

        return false
    }

    private fun navegarMenu(
        direcao: Int
    ): Boolean {

        if (
            itensMenuFoco.isEmpty()
        ) {

            return false
        }

        val atual =
            currentFocus

        var indice =
            itensMenuFoco.indexOf(
                atual
            )

        if (
            indice < 0
        ) {

            indice = 0

        } else {

            indice += direcao

            if (
                indice < 0
            ) {

                indice = 0
            }

            if (
                indice >
                itensMenuFoco.lastIndex
            ) {

                indice =
                    itensMenuFoco.lastIndex
            }
        }

        val proximo =
            itensMenuFoco[indice]

        proximo.requestFocus()

        proximo.post {

            if (
                ::menuScroll.isInitialized
            ) {

                val posicao =
                    proximo.top -
                    dp(30)

                menuScroll.smoothScrollTo(
                    0,
                    posicao.coerceAtLeast(0)
                )
            }
        }

        return true
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            val key =
                event.keyCode

            if (
                key ==
                KeyEvent.KEYCODE_MENU
            ) {

                if (menuAberto) {

                    fecharMenu()

                } else {

                    abrirMenu()
                }

                return true
            }

            if (menuAberto) {

                val foco =
                    currentFocus

                if (
                    !estaDentroDoMenu(
                        foco
                    )
                ) {

                    if (
                        itensMenuFoco.isNotEmpty()
                    ) {

                        itensMenuFoco[0]
                            .requestFocus()
                    }

                    return true
                }

                when (key) {

                    KeyEvent.KEYCODE_DPAD_UP -> {

                        return navegarMenu(-1)
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {

                        return navegarMenu(1)
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {

                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {

                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {

                        foco?.performClick()

                        return true
                    }

                    KeyEvent.KEYCODE_BACK -> {

                        fecharMenu()

                        return true
                    }
                }
            }

            if (
                key ==
                KeyEvent.KEYCODE_BACK
            ) {

                if (
                    voltarConteudoEspecial()
                ) {

                    return true
                }
            }

            if (
                !menuAberto &&
                key ==
                KeyEvent.KEYCODE_DPAD_UP
            ) {

                val atual =
                    currentFocus

                if (
                    atual != null &&
                    atual !== botaoMenu
                ) {

                    val proximo =
                        FocusFinder
                            .getInstance()
                            .findNextFocus(
                                raiz,
                                atual,
                                View.FOCUS_UP
                            )

                    if (
                        proximo == null
                    ) {

                        botaoMenu.requestFocus()

                        return true
                    }
                }
            }

            if (
                !menuAberto &&
                key ==
                KeyEvent.KEYCODE_DPAD_DOWN
            ) {

                if (
                    currentFocus ===
                    botaoMenu
                ) {

                    encontrarPrimeiroFocavel(
                        conteudo
                    )?.requestFocus()

                    return true
                }
            }

            if (
                !menuAberto &&
                (
                    key ==
                    KeyEvent.KEYCODE_DPAD_LEFT ||
                    key ==
                    KeyEvent.KEYCODE_DPAD_RIGHT ||
                    key ==
                    KeyEvent.KEYCODE_DPAD_UP ||
                    key ==
                    KeyEvent.KEYCODE_DPAD_DOWN
                )
            ) {

                val atual =
                    currentFocus

                if (
                    atual != null
                ) {

                    val direcao =
                        when (key) {

                            KeyEvent.KEYCODE_DPAD_LEFT ->
                                View.FOCUS_LEFT

                            KeyEvent.KEYCODE_DPAD_RIGHT ->
                                View.FOCUS_RIGHT

                            KeyEvent.KEYCODE_DPAD_UP ->
                                View.FOCUS_UP

                            else ->
                                View.FOCUS_DOWN
                        }

                    val proximo =
                        FocusFinder
                            .getInstance()
                            .findNextFocus(
                                raiz,
                                atual,
                                direcao
                            )

                    if (
                        proximo != null
                    ) {

                        proximo.requestFocus()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

    private fun carregarImagem(
        imageView: ImageView,
        url: String
    ) {

        if (
            url.isBlank()
        ) return

        val imagemCache =
            cacheCapas[url]

        if (
            imagemCache != null
        ) {

            imageView.setImageBitmap(
                imagemCache
            )

            return
        }

        thread {

            var conexao:
                HttpURLConnection? = null

            try {

                conexao =
                    URL(url)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    5000

                conexao.readTimeout =
                    7000

                conexao.doInput =
                    true

                conexao.useCaches =
                    true

                conexao.setRequestProperty(
                    "Cache-Control",
                    "max-age=86400"
                )

                conexao.connect()

                val bitmap =
                    BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                if (
                    bitmap != null
                ) {

                    cacheCapas[url] =
                        bitmap

                    runOnUiThread {

                        if (
                            imageView.visibility ==
                            View.VISIBLE
                        ) {

                            imageView.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                }

            } catch (
                _: Exception
            ) {

            } finally {

                try {
                    conexao?.disconnect()
                } catch (
                    _: Exception
                ) {
                }
            }
        }
    }

    private fun mostrarMensagem(
        mensagem: String
    ) {

        Toast.makeText(
            this,
            mensagem,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
            resources
                .displayMetrics
                .density
        ).toInt()
    }
}
