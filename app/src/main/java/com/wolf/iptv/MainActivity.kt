package com.wolf.iptv

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
        raiz.setBackgroundColor(Color.BLACK)

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
            Color.argb(130, 0, 0, 0)
        )

        raiz.addView(
            sombra,
            FrameLayout.LayoutParams(-1, -1)
        )

        val principal = LinearLayout(this)

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
            FrameLayout.LayoutParams(-1, -1)
        )

        val topo = LinearLayout(this)

        topo.gravity =
            Gravity.CENTER_VERTICAL

        val logo = TextView(this)

        logo.text = "WOLF CHANNEL"
        logo.textSize = 25f
        logo.setTextColor(Color.WHITE)
        logo.typeface = Typeface.DEFAULT_BOLD

        topo.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        val botaoMenu = TextView(this)

        botaoMenu.text = "☰"
        botaoMenu.textSize = 32f
        botaoMenu.gravity = Gravity.CENTER
        botaoMenu.setTextColor(Color.WHITE)
        botaoMenu.isFocusable = true
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
                dp(65),
                dp(55)
            )
        )

        principal.addView(topo)

        val scroll = ScrollView(this)

        conteudo = LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

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

        val filtrados =
            filmes
                .filter {

                    categoria.isNullOrBlank() ||
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

        mostrarListaCards(filtrados)
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

            repeat(colunas - resto) {

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

        val card = LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setBackgroundColor(
            Color.argb(
                215,
                12,
                12,
                12
            )
        )

        card.isFocusable = true
        card.isClickable = true

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
                dp(280)
            )
        )

        val titulo = TextView(this)

        titulo.text = filme.titulo
        titulo.textSize = 14f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER
        titulo.maxLines = 2
        titulo.setPadding(
            dp(5),
            dp(3),
            dp(5),
            0
        )

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )

        val info = TextView(this)

        info.text =
            "${filme.ano} • ${filme.categoria}"

        info.textSize = 11f
        info.setTextColor(Color.LTGRAY)
        info.gravity = Gravity.CENTER

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

        val fundoMenu = View(this)

        fundoMenu.setBackgroundColor(
            Color.argb(
                150,
                0,
                0,
                0
            )
        )

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
            dp(20),
            dp(18),
            dp(20)
        )

        menuLateral.setBackgroundColor(
            Color.rgb(
                12,
                12,
                12
            )
        )

        menuScroll =
            ScrollView(this)

        menuScroll.addView(
            menuLateral
        )

        val largura =
            if (
                resources.displayMetrics.widthPixels /
                resources.displayMetrics.density >= 800
            ) {
                dp(380)
            } else {
                dp(310)
            }

        val params =
            FrameLayout.LayoutParams(
                largura,
                -1
            )

        params.gravity =
            Gravity.END

        raiz.addView(
            menuScroll,
            params
        )

        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {
            abrirPesquisa()
        }

        adicionarItemMenu(
            "♥  Favoritos"
        ) {
            abrirFavoritos()
        }

        adicionarItemMenu(
            "▶  Continue assistindo"
        ) {
            abrirContinueAssistindo()
        }

        adicionarSeparadorPremium(
            "FILMES"
        )

        adicionarCategoriaFilmes("Ação")
        adicionarCategoriaFilmes("Aventura")
        adicionarCategoriaFilmes("Comédia")
        adicionarCategoriaFilmes("Terror")
        adicionarCategoriaFilmes("Animação")
        adicionarCategoriaFilmes("Ficção")

        adicionarSeparadorPremium(
            "SÉRIES"
        )

        adicionarCategoriaSerie("Ação")
        adicionarCategoriaSerie("Comédia")
        adicionarCategoriaSerie("Drama")
        adicionarCategoriaSerie("Romance")

        adicionarSeparadorPremium(
            "DORAMAS"
        )

        adicionarCategoriaDorama("Ação")
        adicionarCategoriaDorama("Romance")
        adicionarCategoriaDorama("Drama")
        adicionarCategoriaDorama("Comédia")

        adicionarSeparadorPremium(
            "ANIME"
        )

        adicionarCategoriaAnime("Ação")
        adicionarCategoriaAnime("Aventura")
        adicionarCategoriaAnime("Fantasia")
        adicionarCategoriaAnime("Comédia")

        encontrarPrimeiroFocavel(
            menuLateral
        )?.requestFocus()
    }

    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item = TextView(this)

        item.text = texto
        item.textSize = 17f
        item.setTextColor(Color.WHITE)

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(12),
            0,
            dp(10),
            0
        )

        item.isFocusable = true
        item.isClickable = true

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
                dp(52)
            ).apply {

                setMargins(
                    0,
                    dp(3),
                    0,
                    dp(3)
                )
            }
        )
    }

    private fun adicionarCategoriaFilmes(
        categoria: String
    ) {

        val quantidade =
            filmes.count {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        adicionarItemMenu(
            "$categoria  ($quantidade)"
        ) {

            fecharMenu()

            mostrarFilmes(
                categoria
            )
        }
    }

    private fun adicionarCategoriaSerie(
        categoria: String
    ) {

        val quantidade =
            series.count {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        adicionarItemMenu(
            "$categoria  ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                series,
                categoria,
                "Séries"
            )
        }
    }

    private fun adicionarCategoriaDorama(
        categoria: String
    ) {

        val quantidade =
            doramas.count {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        adicionarItemMenu(
            "$categoria  ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                doramas,
                categoria,
                "Doramas"
            )
        }
    }

    private fun adicionarCategoriaAnime(
        categoria: String
    ) {

        val quantidade =
            animes.count {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        adicionarItemMenu(
            "$categoria  ($quantidade)"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                animes,
                categoria,
                "Anime"
            )
        }
    }

    private fun mostrarConteudoEspecial(
        lista: List<Filme>,
        categoria: String,
        nome: String
    ) {

        val resultado =
            lista.filter {

                it.categoria.equals(
                    categoria,
                    true
                )
            }

        if (resultado.isEmpty()) {

            conteudo.removeAllViews()

            val texto = TextView(this)

            texto.text =
                "$nome • $categoria\n\nNenhum conteúdo cadastrado ainda."

            texto.textSize = 20f
            texto.setTextColor(Color.WHITE)
            texto.gravity = Gravity.CENTER

            conteudo.addView(
                texto,
                LinearLayout.LayoutParams(
                    -1,
                    dp(250)
                )
            )

            return
        }

        mostrarListaCards(
            resultado
        )
    }

    private fun abrirPesquisa() {

        fecharMenu()

        conteudo.removeAllViews()

        val campo =
            EditText(this)

        campo.hint =
            "Pesquisar..."

        campo.setTextColor(
            Color.WHITE
        )

        campo.setHintTextColor(
            Color.LTGRAY
        )

        campo.setSingleLine(true)

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            ).apply {

                setMargins(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )
            }
        )

        val botao =
            Button(this)

        botao.text =
            "PESQUISAR"

        botao.setOnClickListener {

            val busca =
                campo.text
                    .toString()
                    .trim()

            if (busca.isEmpty()) {

                mostrarMensagem(
                    "Digite o nome do conteúdo."
                )

                return@setOnClickListener
            }

            mostrarFilmes(
                busca = busca
            )
        }

        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            ).apply {

                setMargins(
                    dp(10),
                    0,
                    dp(10),
                    dp(15)
                )
            }
        )

        campo.requestFocus()
    }

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
    }    private fun abrirContinueAssistindo() {

        fecharMenu()

        val prefs =
            getSharedPreferences(
                "wolf_continue",
                MODE_PRIVATE
            )

        val url =
            prefs.getString(
                "url",
                null
            )

        val titulo =
            prefs.getString(
                "titulo",
                null
            )

        val posicao =
            prefs.getLong(
                "posicao",
                0L
            )

        if (
            url.isNullOrBlank() ||
            titulo.isNullOrBlank() ||
            posicao <= 0L
        ) {

            mostrarMensagem(
                "Você ainda não tem vídeos para continuar."
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
                "Nenhum favorito ainda."
            )

            return
        }

        mostrarListaCards(
            lista
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

        mostrarMensagem(
            if (atual) {
                "Removido dos favoritos."
            } else {
                "Adicionado aos favoritos."
            }
        )
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
        mensagem: String
    ) {

        Toast.makeText(
            this,
            mensagem,
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

            when (event.keyCode) {

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
