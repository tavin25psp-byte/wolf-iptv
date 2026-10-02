package com.wolf.iptv

import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var botaoMenu: TextView

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView
    private lateinit var menuConteudo: LinearLayout

    private var menuAberto = false

    private val itensMenuFoco = mutableListOf<View>()

    private val filmes = mutableListOf(

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
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4",
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
        mostrarFilmes()
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
            FrameLayout.LayoutParams(-1, -1)
        )

        val sombra = View(this)

        sombra.setBackgroundColor(
            Color.argb(145, 0, 0, 0)
        )

        raiz.addView(
            sombra,
            FrameLayout.LayoutParams(-1, -1)
        )

        val principal = LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        principal.setPadding(
            dp(18),
            dp(10),
            dp(18),
            dp(8)
        )

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(-1, -1)
        )

        val topo = LinearLayout(this)

        topo.gravity =
            Gravity.CENTER_VERTICAL

        botaoMenu = TextView(this)

        botaoMenu.text = "☰"
        botaoMenu.textSize = 30f
        botaoMenu.gravity = Gravity.CENTER
        botaoMenu.setTextColor(Color.WHITE)

        botaoMenu.isFocusable = true
        botaoMenu.isFocusableInTouchMode = true
        botaoMenu.isClickable = true

        botaoMenu.setOnClickListener {

            if (menuAberto) {
                fecharMenu()
            } else {
                abrirMenu()
            }
        }

        topo.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(60),
                dp(52)
            )
        )

        val titulo = TextView(this)

        titulo.text = "WOLF CHANNEL"
        titulo.textSize = 24f
        titulo.setTextColor(Color.WHITE)
        titulo.typeface =
            Typeface.DEFAULT_BOLD

        topo.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        principal.addView(topo)

        val scroll = ScrollView(this)

        scroll.isFocusable = false
        scroll.isFocusableInTouchMode = false

        conteudo = LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.isFocusable = false

        scroll.addView(conteudo)

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(raiz)
    }

    private fun mostrarFilmes(
        categoria: String? = null,
        busca: String? = null
    ) {

        val lista = filmes
            .filter {
                categoria == null ||
                it.categoria.equals(
                    categoria,
                    true
                )
            }
            .filter {
                busca.isNullOrBlank() ||
                it.titulo.contains(
                    busca,
                    true
                )
            }
            .sortedWith(
                compareByDescending<Filme> {
                    it.ano
                }.thenBy {
                    it.titulo
                }
            )

        mostrarListaCards(lista)
    }

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            val vazio = TextView(this)

            vazio.text =
                "Nenhum conteúdo encontrado."

            vazio.textSize = 20f
            vazio.setTextColor(Color.WHITE)
            vazio.gravity = Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    -1,
                    dp(250)
                )
            )

            return
        }

        val larguraDp =
            resources.displayMetrics.widthPixels /
            resources.displayMetrics.density

        val colunas =
            if (larguraDp >= 800) 5 else 2

        var linha: LinearLayout? = null

        lista.forEachIndexed { index, filme ->

            if (index % colunas == 0) {

                linha = LinearLayout(this)

                linha!!.gravity =
                    Gravity.CENTER

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(340)
                    )
                )
            }

            linha!!.addView(
                criarCard(filme),
                LinearLayout.LayoutParams(
                    0,
                    dp(320),
                    1f
                ).apply {
                    setMargins(
                        dp(5),
                        dp(7),
                        dp(5),
                        dp(7)
                    )
                }
            )
        }

        val resto =
            lista.size % colunas

        if (resto != 0) {

            repeat(colunas - resto) {

                linha!!.addView(
                    View(this),
                    LinearLayout.LayoutParams(
                        0,
                        dp(320),
                        1f
                    ).apply {
                        setMargins(
                            dp(5),
                            dp(7),
                            dp(5),
                            dp(7)
                        )
                    }
                )
            }
        }

        encontrarPrimeiroFocavel(
            conteudo
        )?.requestFocus()
    }

    private fun criarCard(
        filme: Filme
    ): LinearLayout {

        val card = LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.isFocusable = true
        card.isClickable = true
        card.isFocusableInTouchMode = true

        aplicarEstadoCard(
            card,
            false
        )

        card.setOnFocusChangeListener {
                view,
                foco ->

            aplicarEstadoCard(
                view as LinearLayout,
                foco
            )
        }

        val capa = ImageView(this)

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
                dp(255)
            )
        )

        val nome = TextView(this)

        nome.text =
            filme.titulo

        nome.textSize = 13f
        nome.setTextColor(Color.WHITE)
        nome.gravity = Gravity.CENTER
        nome.maxLines = 2

        card.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                dp(40)
            )
        )

        val info = TextView(this)

        info.text =
            "${filme.ano} • ${filme.categoria}"

        info.textSize = 10f
        info.setTextColor(Color.LTGRAY)
        info.gravity = Gravity.CENTER

        card.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                dp(18)
            )
        )

        card.setOnClickListener {
            abrirVideo(filme)
        }

        card.setOnKeyListener {
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

                abrirVideo(filme)

                true

            } else {
                false
            }
        }

        return card
    }

    private fun aplicarEstadoCard(
        card: LinearLayout,
        focado: Boolean
    ) {

        val fundo =
            GradientDrawable()

        fundo.cornerRadius =
            dp(12).toFloat()

        if (focado) {

            fundo.setColor(
                Color.rgb(
                    55,
                    0,
                    0
                )
            )

            fundo.setStroke(
                dp(3),
                Color.RED
            )

            card.scaleX = 1.03f
            card.scaleY = 1.03f

        } else {

            fundo.setColor(
                Color.argb(
                    210,
                    8,
                    8,
                    8
                )
            )

            fundo.setStroke(
                dp(1),
                Color.rgb(
                    70,
                    70,
                    70
                )
            )

            card.scaleX = 1f
            card.scaleY = 1f
        }

        card.background = fundo
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
    }    private fun abrirMenu() {

        if (menuAberto) return

        menuAberto = true
        itensMenuFoco.clear()

        menuLateral = LinearLayout(this)

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

        val larguraTela =
            resources.displayMetrics.widthPixels

        val larguraDp =
            larguraTela /
            resources.displayMetrics.density

        val larguraMenu =
            if (larguraDp >= 800)
                dp(350)
            else
                dp(310)

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

        // CABEÇALHO FIXO
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

        fecharX.text = "✕"
        fecharX.textSize = 32f
        fecharX.gravity =
            Gravity.CENTER

        fecharX.setTextColor(
            Color.WHITE
        )

        fecharX.isFocusable = true
        fecharX.isFocusableInTouchMode = true
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

            view.background = bg
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

        adicionarSeparadorPremium()

        // AQUI COMEÇA A ÁREA QUE ROLA
        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable = false
        menuScroll.isFocusableInTouchMode = false

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

        menuConteudo.isFocusable = false

        menuScroll.addView(
            menuConteudo,
            ScrollView.LayoutParams(
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

        adicionarSeparadorPremium()

        // FILMES
        adicionarCabecalho(
            "FILMES",
            filmes.size
        )

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
        }

        adicionarSeparadorPremium()

        // SÉRIES
        adicionarCabecalho(
            "SÉRIES",
            series.size
        )

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
                "   $categoria (${contar(
                    series,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarConteudoEspecial(
                    series,
                    categoria,
                    "Séries"
                )
            }
        }

        adicionarSeparadorPremium()

        // DORAMAS
        adicionarCabecalho(
            "DORAMAS",
            doramas.size
        )

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
                "   $categoria (${contar(
                    doramas,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarConteudoEspecial(
                    doramas,
                    categoria,
                    "Doramas"
                )
            }
        }

        adicionarSeparadorPremium()

        // ANIME
        adicionarCabecalho(
            "ANIME",
            animes.size
        )

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
                "   $categoria (${contar(
                    animes,
                    categoria
                )})"
            ) {

                fecharMenu()

                mostrarConteudoEspecial(
                    animes,
                    categoria,
                    "Anime"
                )
            }
        }

        adicionarSeparadorPremium()

        adicionarItemMenu(
            "✕  FECHAR MENU"
        ) {
            fecharMenu()
        }

        // Começa no primeiro item abaixo do cabeçalho
        if (itensMenuFoco.size > 1) {
            itensMenuFoco[1].requestFocus()
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

        item.text = texto
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
        item.isFocusableInTouchMode = true
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

                // Garante que o item focado fique visível
                view.post {

                    val y =
                        view.top

                    menuScroll.smoothScrollTo(
                        0,
                        y
                    )
                }

            } else {

                bg.setColor(
                    Color.TRANSPARENT
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }

            view.background = bg
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

        registrarFocoMenu(item)
    }

    private fun registrarFocoMenu(
        view: View
    ) {

        itensMenuFoco.add(view)
    }

    private fun adicionarSeparadorPremium() {

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

        if (::menuConteudo.isInitialized) {

            menuConteudo.addView(
                linha,
                params
            )

        } else {

            menuLateral.addView(
                linha,
                params
            )
        }
    }

    private fun fecharMenu() {

        if (!menuAberto) return

        if (::menuLateral.isInitialized) {

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

    private fun mostrarConteudoEspecial(
        listaOriginal: List<Filme>,
        categoria: String,
        tipo: String
    ) {

        val lista =
            listaOriginal
                .filter {
                    it.categoria.equals(
                        categoria,
                        true
                    )
                }
                .sortedWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo
                    }
                )

        mostrarListaCards(lista)

        if (lista.isEmpty()) {

            mostrarMensagem(
                "$tipo • $categoria está vazio."
            )
        }
    }

    private fun mostrarFavoritos() {

        val lista =
            filmes.filter {
                ehFavorito(it)
            }

        mostrarListaCards(
            lista
        )
    }

    private fun mostrarContinueAssistindo() {

        val prefs =
            getSharedPreferences(
                "wolf_progress",
                MODE_PRIVATE
            )

        val lista =
            filmes.filter { filme ->

                prefs.getLong(
                    filme.video,
                    0L
                ) > 0L

            }.sortedByDescending { filme ->

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
            android.app.AlertDialog.Builder(
                this
            )
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
    }    private fun contar(
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

        if (view is ViewGroup) {

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

        while (atual != null) {

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

        if (indice < 0) {
            indice = 0
        } else {

            indice += direcao

            if (indice < 0) {
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

                menuScroll.smoothScrollTo(
                    0,
                    proximo.top
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

            // BOTÃO MENU DO CONTROLE
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

            // MENU ABERTO
            if (menuAberto) {

                val foco =
                    currentFocus

                // Nunca deixa os cards de trás receberem D-pad
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

                    KeyEvent.KEYCODE_BACK -> {

                        fecharMenu()

                        return true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> {

                        foco?.performClick()

                        return true
                    }
                }
            }

            // MENU FECHADO
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

            // NAVEGAÇÃO DOS CARDS
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

                if (atual != null) {

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

            if (
                key ==
                KeyEvent.KEYCODE_BACK
            ) {

                if (menuAberto) {

                    fecharMenu()

                    return true
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

        thread {

            try {

                val conexao =
                    URL(url)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    15000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.disconnect()

                if (
                    bitmap != null
                ) {

                    runOnUiThread {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
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
