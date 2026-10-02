package com.wolf.iptv

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

    private var menuAberto = false

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView

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
            "https://vz-c0911a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8"
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
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val sombra = View(this)

        sombra.setBackgroundColor(
            Color.argb(
                145,
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

        principal.setPadding(
            dp(20),
            dp(15),
            dp(20),
            dp(10)
        )

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val topo =
            LinearLayout(this)

        topo.gravity =
            Gravity.CENTER_VERTICAL

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF CHANNEL"

        titulo.textSize =
            25f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        topo.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        val menuBotao =
            TextView(this)

        menuBotao.text =
            "☰"

        menuBotao.textSize =
            30f

        menuBotao.gravity =
            Gravity.CENTER

        menuBotao.setTextColor(
            Color.WHITE
        )

        menuBotao.isFocusable =
            true

        menuBotao.setOnClickListener {

            if (menuAberto) {
                fecharMenu()
            } else {
                abrirMenu()
            }
        }

        topo.addView(
            menuBotao,
            LinearLayout.LayoutParams(
                dp(65),
                dp(55)
            )
        )

        principal.addView(
            topo
        )

        val scroll =
            ScrollView(this)

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        scroll.addView(
            conteudo
        )

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

        val lista =
            filmes
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

        mostrarListaCards(
            lista
        )
    }

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum conteúdo encontrado."

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

        lista.forEachIndexed {
            index,
            filme ->

            if (index % colunas == 0) {

                linha =
                    LinearLayout(this)

                linha!!.gravity =
                    Gravity.CENTER

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(370)
                    )
                )
            }

            linha!!.addView(
                criarCard(filme),
                LinearLayout.LayoutParams(
                    0,
                    dp(350),
                    1f
                ).apply {

                    setMargins(
                        dp(6),
                        dp(8),
                        dp(6),
                        dp(8)
                    )
                }
            )
        }

        val resto =
            lista.size % colunas

        if (resto != 0) {

            repeat(
                colunas - resto
            ) {

                linha!!.addView(
                    View(this),
                    LinearLayout.LayoutParams(
                        0,
                        dp(350),
                        1f
                    ).apply {

                        setMargins(
                            dp(6),
                            dp(8),
                            dp(6),
                            dp(8)
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

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setBackgroundColor(
            Color.argb(
                220,
                12,
                12,
                12
            )
        )

        card.isFocusable =
            true

        card.isClickable =
            true

        val capa =
            ImageView(this)

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

        val nome =
            TextView(this)

        nome.text =
            filme.titulo

        nome.textSize =
            14f

        nome.setTextColor(
            Color.WHITE
        )

        nome.gravity =
            Gravity.CENTER

        nome.maxLines =
            2

        nome.setPadding(
            dp(5),
            dp(3),
            dp(5),
            0
        )

        card.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        val info =
            TextView(this)

        info.text =
            "${filme.ano} • ${filme.categoria}"

        info.textSize =
            11f

        info.setTextColor(
            Color.LTGRAY
        )

        info.gravity =
            Gravity.CENTER

        card.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                dp(20)
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

        val escuro =
            View(this)

        escuro.setBackgroundColor(
            Color.argb(
                150,
                0,
                0,
                0
            )
        )

        raiz.addView(
            escuro,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val painel =
            FrameLayout(this)

        painel.setBackgroundColor(
            Color.rgb(
                8,
                8,
                8
            )
        )

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setPadding(
            dp(20),
            dp(18),
            dp(20),
            dp(25)
        )

        menuScroll =
            ScrollView(this)

        menuScroll.addView(
            menuLateral
        )

        painel.addView(
            menuScroll,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val linhaVermelha =
            View(this)

        linhaVermelha.setBackgroundColor(
            Color.rgb(
                255,
                0,
                0
            )
        )

        val linhaParams =
            FrameLayout.LayoutParams(
                dp(3),
                -1
            )

        linhaParams.gravity =
            Gravity.END

        painel.addView(
            linhaVermelha,
            linhaParams
        )

        val largura =
            (
                resources.displayMetrics.widthPixels
                * 0.78f
            ).toInt()

        val painelParams =
            FrameLayout.LayoutParams(
                largura,
                -1
            )

        painelParams.gravity =
            Gravity.START

        raiz.addView(
            painel,
            painelParams
        )

        val topo =
            LinearLayout(this)

        topo.gravity =
            Gravity.CENTER_VERTICAL

        val navegacao =
            TextView(this)

        navegacao.text =
            "NAVEGAÇÃO"

        navegacao.textSize =
            24f

        navegacao.setTextColor(
            Color.rgb(
                255,
                25,
                25
            )
        )

        navegacao.typeface =
            Typeface.DEFAULT_BOLD

        topo.addView(
            navegacao,
            LinearLayout.LayoutParams(
                0,
                dp(70),
                1f
            )
        )

        val fechar =
            TextView(this)

        fechar.text =
            "×"

        fechar.textSize =
            42f

        fechar.gravity =
            Gravity.CENTER

        fechar.setTextColor(
            Color.WHITE
        )

        fechar.setBackgroundColor(
            Color.TRANSPARENT
        )

        fechar.isFocusable =
            true

        fechar.setOnClickListener {
            fecharMenu()
        }

        topo.addView(
            fechar,
            LinearLayout.LayoutParams(
                dp(60),
                dp(65)
            )
        )

        menuLateral.addView(
            topo
        )

        val divisor =
            View(this)

        divisor.setBackgroundColor(
            Color.rgb(
                45,
                45,
                45
            )
        )

        menuLateral.addView(
            divisor,
            LinearLayout.LayoutParams(
                -1,
                dp(1)
            )
        )

        adicionarSeparadorMenu(
            "CONTINUE A ASSISTIR"
        )

        adicionarContinueCard()

        adicionarDivisor()

        val favoritos =
            quantidadeFavoritos()

        adicionarItemPrincipal(
            "♥  Meus Favoritos ($favoritos)"
        ) {
            abrirFavoritos()
        }

        adicionarItemPrincipal(
            "🏠  Início / Todos (${filmes.size})"
        ) {
            fecharMenu()
            mostrarFilmes()
        }

        adicionarDivisor()

        val totalFilmes =
            filmes.size

        adicionarItemPrincipal(
            "🎬  Filmes ($totalFilmes)"
        ) {
            fecharMenu()
            mostrarFilmes()
        }

        adicionarCategoria(
            "Ação",
            filmes
        )

        adicionarCategoria(
            "Terror",
            filmes
        )

        adicionarCategoria(
            "Aventura",
            filmes
        )

        adicionarCategoria(
            "Animação",
            filmes
        )

        adicionarCategoria(
            "Comédia",
            filmes
        )

        adicionarCategoria(
            "Ficção",
            filmes
        )

        adicionarDivisor()

        adicionarItemPrincipal(
            "📺  Séries (${series.size})"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                series,
                null,
                "Séries"
            )
        }

        adicionarCategoria(
            "Ação",
            series
        )

        adicionarCategoria(
            "Comédia",
            series
        )

        adicionarCategoria(
            "Drama",
            series
        )

        adicionarCategoria(
            "Romance",
            series
        )

        adicionarDivisor()

        adicionarItemPrincipal(
            "🌸  Doramas (${doramas.size})"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                doramas,
                null,
                "Doramas"
            )
        }

        adicionarCategoria(
            "Ação",
            doramas
        )

        adicionarCategoria(
            "Romance",
            doramas
        )

        adicionarCategoria(
            "Drama",
            doramas
        )

        adicionarCategoria(
            "Comédia",
            doramas
        )

        adicionarDivisor()

        adicionarItemPrincipal(
            "🍥  Anime (${animes.size})"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                animes,
                null,
                "Anime"
            )
        }

        adicionarCategoria(
            "Ação",
            animes
        )

        adicionarCategoria(
            "Aventura",
            animes
        )

        adicionarCategoria(
            "Fantasia",
            animes
        )

        adicionarCategoria(
            "Comédia",
            animes
        )

        adicionarDivisor()

        adicionarItemPrincipal(
            "⌕  Pesquisa"
        ) {
            abrirPesquisa()
        }

        encontrarPrimeiroFocavel(
            menuLateral
        )?.requestFocus()
    }

    private fun adicionarSeparadorMenu(
        texto: String
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            13f

        item.setTextColor(
            Color.rgb(
                255,
                20,
                20
            )
        )

        item.typeface =
            Typeface.DEFAULT_BOLD

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
                -1,
                dp(48)
            )
        )
    }

    private fun adicionarItemPrincipal(
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            18f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(10),
            0
        )

        item.typeface =
            Typeface.DEFAULT_BOLD

        item.isFocusable =
            true

        item.setBackground(
            criarFundoItem()
        )

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

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
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

    private fun adicionarCategoria(
        categoria: String,
        lista: List<Filme>
    ) {

        val quantidade =
            lista.count {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        val item =
            TextView(this)

        item.text =
            "↳  $categoria ($quantidade)"

        item.textSize =
            17f

        item.setTextColor(
            Color.LTGRAY
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(45),
            0,
            dp(10),
            0
        )

        item.isFocusable =
            true

        item.setBackground(
            criarFundoCategoria()
        )

        item.setOnClickListener {

            fecharMenu()

            if (lista === filmes) {

                mostrarFilmes(
                    categoria
                )

            } else {

                mostrarConteudoEspecial(
                    lista,
                    categoria,
                    "Conteúdo"
                )
            }
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

                fecharMenu()

                if (lista === filmes) {

                    mostrarFilmes(
                        categoria
                    )

                } else {

                    mostrarConteudoEspecial(
                        lista,
                        categoria,
                        "Conteúdo"
                    )
                }

                true

            } else {

                false
            }
        }

        menuLateral.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
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

    private fun adicionarDivisor() {

        val divisor =
            View(this)

        divisor.setBackgroundColor(
            Color.rgb(
                45,
                45,
                45
            )
        )

        menuLateral.addView(
            divisor,
            LinearLayout.LayoutParams(
                -1,
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
    }    private fun adicionarContinueCard() {

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

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum vídeo para continuar"

            vazio.textSize =
                14f

            vazio.setTextColor(
                Color.LTGRAY
            )

            vazio.gravity =
                Gravity.CENTER_VERTICAL

            vazio.setPadding(
                dp(35),
                0,
                0,
                0
            )

            menuLateral.addView(
                vazio,
                LinearLayout.LayoutParams(
                    -1,
                    dp(65)
                )
            )

            return
        }

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(12),
            dp(8),
            dp(12),
            dp(8)
        )

        card.isFocusable =
            true

        card.setBackground(
            criarFundoVermelho()
        )

        val capa =
            ImageView(this)

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        val filme =
            filmes.firstOrNull {
                it.video == url
            }

        if (filme != null) {

            carregarImagem(
                capa,
                filme.capa
            )
        }

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                dp(65),
                dp(75)
            )
        )

        val textos =
            LinearLayout(this)

        textos.orientation =
            LinearLayout.VERTICAL

        textos.gravity =
            Gravity.CENTER_VERTICAL

        val nome =
            TextView(this)

        nome.text =
            titulo

        nome.textSize =
            16f

        nome.setTextColor(
            Color.WHITE
        )

        nome.typeface =
            Typeface.DEFAULT_BOLD

        nome.maxLines =
            1

        val continuar =
            TextView(this)

        continuar.text =
            "▶ Retomar reprodução"

        continuar.textSize =
            14f

        continuar.setTextColor(
            Color.LTGRAY
        )

        textos.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                dp(35)
            )
        )

        textos.addView(
            continuar,
            LinearLayout.LayoutParams(
                -1,
                dp(30)
            )
        )

        card.addView(
            textos,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            ).apply {

                setMargins(
                    dp(12),
                    0,
                    0,
                    0
                )
            }
        )

        val acao = {

            val intent =
                Intent(
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

            startActivity(
                intent
            )
        }

        card.setOnClickListener {
            acao()
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

                acao()

                true

            } else {

                false
            }
        }

        menuLateral.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                dp(110)
            ).apply {

                setMargins(
                    dp(25),
                    dp(5),
                    dp(25),
                    dp(8)
                )
            }
        )
    }

    private fun abrirFavoritos() {

        fecharMenu()

        val prefs =
            getSharedPreferences(
                "wolf_favoritos",
                MODE_PRIVATE
            )

        val lista =
            filmes.filter {

                prefs.getBoolean(
                    it.video,
                    false
                )
            }

        if (lista.isEmpty()) {

            mostrarMensagem(
                "Você ainda não possui favoritos."
            )

            return
        }

        mostrarListaCards(
            lista
        )
    }

    private fun quantidadeFavoritos(): Int {

        val prefs =
            getSharedPreferences(
                "wolf_favoritos",
                MODE_PRIVATE
            )

        return filmes.count {

            prefs.getBoolean(
                it.video,
                false
            )
        }
    }

    private fun alternarFavorito(
        filme: Filme
    ) {

        val prefs =
            getSharedPreferences(
                "wolf_favoritos",
                MODE_PRIVATE
            )

        val atual =
            prefs.getBoolean(
                filme.video,
                false
            )

        prefs.edit()
            .putBoolean(
                filme.video,
                !atual
            )
            .apply()
    }

    private fun abrirPesquisa() {

        fecharMenu()

        conteudo.removeAllViews()

        val campo =
            EditText(this)

        campo.hint =
            "Pesquisar filmes..."

        campo.textSize =
            18f

        campo.setTextColor(
            Color.WHITE
        )

        campo.setHintTextColor(
            Color.LTGRAY
        )

        campo.setSingleLine(
            true
        )

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {

                setMargins(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(10)
                )
            }
        )

        val pesquisar =
            TextView(this)

        pesquisar.text =
            "🔎  PESQUISAR"

        pesquisar.textSize =
            18f

        pesquisar.setTextColor(
            Color.WHITE
        )

        pesquisar.gravity =
            Gravity.CENTER

        pesquisar.isFocusable =
            true

        pesquisar.setBackground(
            criarFundoVermelho()
        )

        pesquisar.setOnClickListener {

            val texto =
                campo.text
                    .toString()
                    .trim()

            mostrarFilmes(
                busca = texto
            )
        }

        conteudo.addView(
            pesquisar,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            ).apply {

                setMargins(
                    dp(15),
                    0,
                    dp(15),
                    dp(15)
                )
            }
        )

        campo.requestFocus()
    }

    private fun mostrarConteudoEspecial(
        lista: List<Filme>,
        categoria: String?,
        tipo: String
    ) {

        val resultado =
            if (categoria == null) {

                lista

            } else {

                lista.filter {

                    it.categoria.equals(
                        categoria,
                        true
                    )
                }
            }

        if (resultado.isEmpty()) {

            conteudo.removeAllViews()

            val texto =
                TextView(this)

            texto.text =
                "$tipo\n\nNenhum conteúdo cadastrado ainda."

            texto.textSize =
                20f

            texto.setTextColor(
                Color.WHITE
            )

            texto.gravity =
                Gravity.CENTER

            conteudo.addView(
                texto,
                LinearLayout.LayoutParams(
                    -1,
                    dp(300)
                )
            )

            return
        }

        mostrarListaCards(
            resultado
        )
    }

    private fun criarFundoItem():
        GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.rgb(
                    18,
                    18,
                    18
                )
            )

            cornerRadius =
                dp(14).toFloat()
        }
    }

    private fun criarFundoCategoria():
        GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.rgb(
                    20,
                    16,
                    16
                )
            )

            cornerRadius =
                dp(14).toFloat()
        }
    }

    private fun criarFundoVermelho():
        GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.rgb(
                    30,
                    5,
                    5
                )
            )

            setStroke(
                dp(2),
                Color.rgb(
                    220,
                    20,
                    30
                )
            )

            cornerRadius =
                dp(14).toFloat()
        }
    }

    private fun fecharMenu() {

        if (!menuAberto) return

        menuAberto =
            false

        while (
            raiz.childCount > 3
        ) {

            raiz.removeViewAt(
                raiz.childCount - 1
            )
        }
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

    private fun carregarImagem(
        imageView: ImageView,
        endereco: String
    ) {

        thread {

            try {

                val conexao =
                    URL(endereco)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    15000

                conexao.doInput =
                    true

                conexao.connect()

                val bitmap =
                    BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.inputStream.close()

                conexao.disconnect()

                runOnUiThread {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {

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

    private fun mostrarMensagem(
        texto: String
    ) {

        Toast.makeText(
            this,
            texto,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
            resources.displayMetrics.density
        ).toInt()
    }

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

                    if (menuAberto) {

                        fecharMenu()

                    } else {

                        abrirMenu()
                    }

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }
}
