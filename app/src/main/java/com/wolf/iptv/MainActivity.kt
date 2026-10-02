package com.wolf.iptv

import android.content.Intent
import android.graphics.Bitmap
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
    private lateinit var botaoMenu: TextView

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView
    private lateinit var menuConteudo: LinearLayout

    private var menuAberto = false

    private val itensMenuFoco =
        mutableListOf<View>()

    private val cacheCapas =
        HashMap<String, Bitmap>()

    private val filmes =
        mutableListOf<Filme>()

    private val series =
        mutableListOf<Filme>()

    private val doramas =
        mutableListOf<Filme>()

    private val animes =
        mutableListOf<Filme>()

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

        mostrarListaCards(filmes)
    }

    private fun criarInterface() {

        raiz = FrameLayout(this)

        raiz.setBackgroundColor(Color.BLACK)

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

        botaoMenu.textSize =
            18f

        botaoMenu.typeface =
            Typeface.DEFAULT_BOLD

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable =
            true

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

        aplicarFocoVermelho(
            botaoMenu
        )

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

        conteudo.isFocusable =
            false

        val scroll =
            ScrollView(this)

        scroll.isFocusable =
            false

        scroll.isFocusableInTouchMode =
            false

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
                    "Deadpool & Wolverine",
                    2024,
                    "Ação",
                    "https://i.postimg.cc/2SZ6hyv9/IMG-20261002-100134.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Deadpool%20%26amp%3B%20Wolverine.mp4"
                ),

                Filme(
                    "Coringa: Delírio a Dois",
                    2024,
                    "Drama",
                    "https://i.postimg.cc/LXN621K3/MV5BZTU0ZGI3Yz-Mt-ZTUw-MC00MGJj-LWFk-NDIt-MDUz-NTUx-Zjg5N2Y4Xk-Ey-Xk-Fqc-Gc-V1.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Coringa%20Del%C3%ADrio%20a%20Dois.mp4"
                ),

                Filme(
                    "Mufasa: O Rei Leão",
                    2024,
                    "Aventura",
                    "https://i.postimg.cc/mgw3xmpz/917q-7O0TJL.jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Mufasa%20O%20Rei%20Le%C3%A3o.mp4"
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
    }// ===============================
// PARTE 2/4
// ===============================

private fun mostrarListaCards(lista: List<Filme>) {

    conteudo.removeAllViews()

    val titulo = TextView(this)
    titulo.text = "FILMES"
    titulo.textSize = 28f
    titulo.setTextColor(Color.WHITE)
    titulo.setTypeface(null, Typeface.BOLD)
    titulo.setPadding(20, 10, 20, 20)

    conteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )

    val scroll = HorizontalScrollView(this)
    scroll.isHorizontalScrollBarEnabled = false

    val grade = GridLayout(this)

    grade.columnCount = 5
    grade.rowCount = maxOf(1, (lista.size + 4) / 5)

    val gradeParams = HorizontalScrollView.LayoutParams(
        HorizontalScrollView.LayoutParams.WRAP_CONTENT,
        HorizontalScrollView.LayoutParams.WRAP_CONTENT
    )

    scroll.addView(grade, gradeParams)

    lista.forEachIndexed { index, filme ->

        val card = criarCard(filme)

        val params = GridLayout.LayoutParams()

        params.width = dp(220)
        params.height = dp(320)

        params.setMargins(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        card.layoutParams = params

        grade.addView(card)

        card.setOnFocusChangeListener { view, focado ->

            if (focado) {

                view.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(120)
                    .start()

                aplicarFocoVermelho(view)

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()

                view.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        if (index == 0) {
            card.post {
                card.requestFocus()
            }
        }
    }

    conteudo.addView(
        scroll,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    )
}


// ===============================
// CRIAR CARD
// ===============================

private fun criarCard(filme: Filme): LinearLayout {

    val card = LinearLayout(this)

    card.orientation = LinearLayout.VERTICAL
    card.gravity = Gravity.CENTER
    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.setPadding(
        dp(5),
        dp(5),
        dp(5),
        dp(5)
    )

    val imagem = ImageView(this)

    imagem.scaleType = ImageView.ScaleType.FIT_CENTER

    imagem.setBackgroundColor(Color.TRANSPARENT)

    card.addView(
        imagem,
        LinearLayout.LayoutParams(
            dp(210),
            dp(255)
        )
    )

    val nome = TextView(this)

    nome.text = filme.titulo
    nome.textSize = 15f
    nome.setTextColor(Color.WHITE)
    nome.gravity = Gravity.CENTER
    nome.maxLines = 2
    nome.ellipsize = TextUtils.TruncateAt.END
    nome.setTypeface(null, Typeface.BOLD)

    val nomeParams = LinearLayout.LayoutParams(
        dp(210),
        dp(45)
    )

    nomeParams.topMargin = dp(5)

    card.addView(nome, nomeParams)

    val ano = TextView(this)

    ano.text = filme.ano.toString()
    ano.textSize = 12f
    ano.setTextColor(Color.LTGRAY)
    ano.gravity = Gravity.CENTER

    card.addView(
        ano,
        LinearLayout.LayoutParams(
            dp(210),
            dp(25)
        )
    )

    carregarImagem(imagem, filme.capa)

    card.setOnClickListener {

        abrirVideo(filme)
    }

    card.setOnKeyListener { _, keyCode, event ->

        if (
            event.action == KeyEvent.ACTION_DOWN &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER
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


// ===============================
// CARREGAR IMAGEM
// ===============================

private fun carregarImagem(
    imageView: ImageView,
    url: String
) {

    Thread {

        try {

            val connection =
                URL(url).openConnection()

            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            connection.doInput = true

            connection.connect()

            val input =
                connection.getInputStream()

            val bitmap =
                BitmapFactory.decodeStream(input)

            input.close()

            runOnUiThread {

                if (bitmap != null) {

                    imageView.setImageBitmap(bitmap)

                }
            }

        } catch (_: Exception) {

            runOnUiThread {

                imageView.setImageDrawable(null)
            }
        }

    }.start()
}


// ===============================
// ABRIR VÍDEO
// ===============================

private fun abrirVideo(filme: Filme) {

    if (filme.video.isBlank()) {

        Toast.makeText(
            this,
            "Vídeo ainda não disponível.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val intent =
        Intent(this, PlayerActivity::class.java)

    intent.putExtra(
        "VIDEO_URL",
        filme.video
    )

    intent.putExtra(
        "VIDEO_TITULO",
        filme.titulo
    )

    startActivity(intent)
}


// ===============================
// MENU LATERAL
// ===============================

private fun abrirMenu() {

    if (menuAberto) {

        fecharMenu()

        return
    }

    menuAberto = true

    menuLateral.visibility = View.VISIBLE

    menuLateral.translationX =
        -dp(400).toFloat()

    menuLateral.animate()
        .translationX(0f)
        .setDuration(220)
        .start()

    construirMenuDinamico()
}


// ===============================
// FECHAR MENU
// ===============================

private fun fecharMenu() {

    menuAberto = false

    menuLateral.animate()
        .translationX(-dp(400).toFloat())
        .setDuration(180)
        .withEndAction {

            menuLateral.visibility =
                View.GONE
        }
        .start()

    botaoMenu.postDelayed({

        botaoMenu.requestFocus()

    }, 200)
}


// ===============================
// CONSTRUIR MENU
// ===============================

private fun construirMenuDinamico() {

    menuConteudo.removeAllViews()

    itensMenuFoco.clear()

    adicionarItemMenu(
        "⌂  INÍCIO",
        true
    ) {

        fecharMenu()

        mostrarTodos()
    }

    adicionarItemMenu(
        "★  FAVORITOS",
        false
    ) {

        mostrarFavoritos()
    }

    adicionarItemMenu(
        "▶  CONTINUE ASSISTINDO",
        false
    ) {

        mostrarContinueAssistindo()
    }

    adicionarItemMenu(
        "⌕  PESQUISA",
        false
    ) {

        abrirPesquisa()
    }

    adicionarSeparadorPremium(
        "FILMES"
    )

    adicionarCategoria(
        "Ação",
        filmes.count {
            it.categoria.equals(
                "Ação",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Ação"
        )
    }

    adicionarCategoria(
        "Aventura",
        filmes.count {
            it.categoria.equals(
                "Aventura",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Aventura"
        )
    }

    adicionarCategoria(
        "Comédia",
        filmes.count {
            it.categoria.equals(
                "Comédia",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Comédia"
        )
    }

    adicionarCategoria(
        "Terror",
        filmes.count {
            it.categoria.equals(
                "Terror",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Terror"
        )
    }

    adicionarCategoria(
        "Animação",
        filmes.count {
            it.categoria.equals(
                "Animação",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Animação"
        )
    }

    adicionarCategoria(
        "Ficção",
        filmes.count {
            it.categoria.equals(
                "Ficção",
                true
            )
        }
    ) {

        mostrarCategoria(
            "Ficção"
        )
    }

    adicionarSeparadorPremium(
        "SÉRIES"
    )

    adicionarCategoria(
        "Ação",
        series.count {
            it.categoria.equals(
                "Ação",
                true
            )
        }
    ) {

        mostrarCategoriaSerie(
            "Ação"
        )
    }

    adicionarCategoria(
        "Comédia",
        series.count {
            it.categoria.equals(
                "Comédia",
                true
            )
        }
    ) {

        mostrarCategoriaSerie(
            "Comédia"
        )
    }

    adicionarCategoria(
        "Drama",
        series.count {
            it.categoria.equals(
                "Drama",
                true
            )
        }
    ) {

        mostrarCategoriaSerie(
            "Drama"
        )
    }

    adicionarCategoria(
        "Romance",
        series.count {
            it.categoria.equals(
                "Romance",
                true
            )
        }
    ) {

        mostrarCategoriaSerie(
            "Romance"
        )
    }

    adicionarSeparadorPremium(
        "DORAMAS"
    )

    adicionarCategoria(
        "Ação",
        doramas.count {
            it.categoria.equals(
                "Ação",
                true
            )
        }
    ) {

        mostrarCategoriaDorama(
            "Ação"
        )
    }

    adicionarCategoria(
        "Romance",
        doramas.count {
            it.categoria.equals(
                "Romance",
                true
            )
        }
    ) {

        mostrarCategoriaDorama(
            "Romance"
        )
    }

    adicionarCategoria(
        "Drama",
        doramas.count {
            it.categoria.equals(
                "Drama",
                true
            )
        }
    ) {

        mostrarCategoriaDorama(
            "Drama"
        )
    }

    adicionarCategoria(
        "Comédia",
        doramas.count {
            it.categoria.equals(
                "Comédia",
                true
            )
        }
    ) {

        mostrarCategoriaDorama(
            "Comédia"
        )
    }

    adicionarSeparadorPremium(
        "ANIME"
    )

    adicionarCategoria(
        "Ação",
        animes.count {
            it.categoria.equals(
                "Ação",
                true
            )
        }
    ) {

        mostrarCategoriaAnime(
            "Ação"
        )
    }

    adicionarCategoria(
        "Aventura",
        animes.count {
            it.categoria.equals(
                "Aventura",
                true
            )
        }
    ) {

        mostrarCategoriaAnime(
            "Aventura"
        )
    }

    adicionarCategoria(
        "Fantasia",
        animes.count {
            it.categoria.equals(
                "Fantasia",
                true
            )
        }
    ) {

        mostrarCategoriaAnime(
            "Fantasia"
        )
    }

    adicionarCategoria(
        "Comédia",
        animes.count {
            it.categoria.equals(
                "Comédia",
                true
            )
        }
    ) {

        mostrarCategoriaAnime(
            "Comédia"
        )
    }

    adicionarSeparadorPremium(
        "OUTROS"
    )

    adicionarItemMenu(
        "✕  FECHAR",
        false
    ) {

        fecharMenu()
    }

    if (itensMenuFoco.isNotEmpty()) {

        itensMenuFoco.first().post {

            itensMenuFoco.first()
                .requestFocus()
        }
    }
}


// ===============================
// SEPARADOR
// ===============================

private fun adicionarSeparadorPremium(
    titulo: String
) {

    val texto = TextView(this)

    texto.text = titulo
    texto.textSize = 14f
    texto.setTextColor(Color.RED)
    texto.setTypeface(null, Typeface.BOLD)
    texto.setPadding(
        dp(18),
        dp(18),
        dp(10),
        dp(8)
    )

    menuConteudo.addView(
        texto,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )
}


// ===============================
// ITEM DO MENU
// ===============================

private fun adicionarItemMenu(
    texto: String,
    principal: Boolean,
    acao: () -> Unit
) {

    val item = TextView(this)

    item.text = texto
    item.textSize =
        if (principal) 18f else 16f

    item.setTextColor(Color.WHITE)

    item.gravity =
        Gravity.CENTER_VERTICAL

    item.setPadding(
        dp(18),
        dp(12),
        dp(12),
        dp(12)
    )

    item.isFocusable = true
    item.isFocusableInTouchMode = true

    item.setOnClickListener {

        acao()
    }

    item.setOnKeyListener { _, keyCode, event ->

        if (
            event.action == KeyEvent.ACTION_DOWN &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER
            )
        ) {

            acao()

            true

        } else {

            false
        }
    }

    item.setOnFocusChangeListener { view, focado ->

        if (focado) {

            aplicarFocoVermelho(view)

        } else {

            view.setBackgroundColor(
                Color.TRANSPARENT
            )
        }
    }

    itensMenuFoco.add(item)

    menuConteudo.addView(
        item,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(52)
        )
    )
}


// ===============================
// CATEGORIA
// ===============================

private fun adicionarCategoria(
    nome: String,
    quantidade: Int,
    acao: () -> Unit
) {

    val texto = TextView(this)

    texto.text =
        "   $nome                         $quantidade"

    texto.textSize = 15f
    texto.setTextColor(Color.LTGRAY)

    texto.gravity =
        Gravity.CENTER_VERTICAL

    texto.setPadding(
        dp(18),
        0,
        dp(10),
        0
    )

    texto.isFocusable = true
    texto.isFocusableInTouchMode = true

    texto.setOnClickListener {

        acao()
    }

    texto.setOnKeyListener { _, keyCode, event ->

        if (
            event.action == KeyEvent.ACTION_DOWN &&
            (
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER
            )
        ) {

            acao()

            true

        } else {

            false
        }
    }

    texto.setOnFocusChangeListener { view, focado ->

        if (focado) {

            aplicarFocoVermelho(view)

        } else {

            view.setBackgroundColor(
                Color.TRANSPARENT
            )
        }
    }

    itensMenuFoco.add(texto)

    menuConteudo.addView(
        texto,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(44)
        )
    )
}// ===============================
// PARTE 3/4
// ===============================

private fun mostrarTodos() {

    mostrarListaCards(
        filmes
    )
}


// ===============================
// MOSTRAR CATEGORIA DE FILMES
// ===============================

private fun mostrarCategoria(
    categoria: String
) {

    val lista = filmes.filter {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }

    mostrarListaCards(
        lista
    )
}


// ===============================
// SÉRIES
// ===============================

private fun mostrarCategoriaSerie(
    categoria: String
) {

    val lista = series.filter {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }

    mostrarListaCards(
        lista
    )
}


// ===============================
// DORAMAS
// ===============================

private fun mostrarCategoriaDorama(
    categoria: String
) {

    val lista = doramas.filter {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }

    mostrarListaCards(
        lista
    )
}


// ===============================
// ANIMES
// ===============================

private fun mostrarCategoriaAnime(
    categoria: String
) {

    val lista = animes.filter {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }

    mostrarListaCards(
        lista
    )
}


// ===============================
// FAVORITOS
// ===============================

private fun mostrarFavoritos() {

    fecharMenu()

    val favoritos =
        filmes.filter {

            favoritosSalvos.contains(
                it.titulo
            )
        }

    if (favoritos.isEmpty()) {

        mostrarMensagem(
            "★\n\nNenhum favorito ainda."
        )

        return
    }

    mostrarListaCards(
        favoritos
    )
}


// ===============================
// CONTINUE ASSISTINDO
// ===============================

private fun mostrarContinueAssistindo() {

    fecharMenu()

    val continuar =
        filmes.filter {

            progressoVideos.containsKey(
                it.titulo
            )
        }

    if (continuar.isEmpty()) {

        mostrarMensagem(
            "▶\n\nNenhum vídeo para continuar."
        )

        return
    }

    mostrarListaCards(
        continuar
    )
}


// ===============================
// PESQUISA
// ===============================

private fun abrirPesquisa() {

    fecharMenu()

    val entrada = EditText(this)

    entrada.hint =
        "Digite o nome do filme..."

    entrada.setHintTextColor(
        Color.LTGRAY
    )

    entrada.setTextColor(
        Color.WHITE
    )

    entrada.textSize = 18f

    entrada.setSingleLine(true)

    entrada.setPadding(
        dp(20),
        dp(10),
        dp(20),
        dp(10)
    )

    val caixa =
        LinearLayout(this)

    caixa.orientation =
        LinearLayout.VERTICAL

    caixa.setPadding(
        dp(30),
        dp(30),
        dp(30),
        dp(20)
    )

    caixa.setBackgroundColor(
        Color.argb(
            235,
            15,
            15,
            15
        )
    )

    val titulo =
        TextView(this)

    titulo.text =
        "PESQUISAR"

    titulo.textSize = 25f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.setPadding(
        0,
        0,
        0,
        dp(20)
    )

    caixa.addView(
        titulo
    )

    caixa.addView(
        entrada,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    val pesquisar =
        TextView(this)

    pesquisar.text =
        "🔎  PESQUISAR"

    pesquisar.textSize = 17f

    pesquisar.setTextColor(
        Color.WHITE
    )

    pesquisar.gravity =
        Gravity.CENTER

    pesquisar.isFocusable = true
    pesquisar.isFocusableInTouchMode = true

    pesquisar.setOnFocusChangeListener {
            view,
            focado ->

        if (focado) {

            aplicarFocoVermelho(
                view
            )

        } else {

            view.setBackgroundColor(
                Color.TRANSPARENT
            )
        }
    }

    pesquisar.setOnClickListener {

        executarPesquisa(
            entrada.text.toString()
        )
    }

    pesquisar.setOnKeyListener {
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

            executarPesquisa(
                entrada.text.toString()
            )

            true

        } else {

            false
        }
    }

    caixa.addView(
        pesquisar,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(55)
        )
    )

    val fechar =
        TextView(this)

    fechar.text =
        "✕  FECHAR"

    fechar.textSize = 16f

    fechar.setTextColor(
        Color.WHITE
    )

    fechar.gravity =
        Gravity.CENTER

    fechar.isFocusable = true
    fechar.isFocusableInTouchMode = true

    fechar.setOnClickListener {

        conteudo.removeAllViews()
        mostrarTodos()
    }

    fechar.setOnKeyListener {
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

            conteudo.removeAllViews()
            mostrarTodos()

            true

        } else {

            false
        }
    }

    caixa.addView(
        fechar,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(55)
        )
    )

    conteudo.removeAllViews()

    conteudo.addView(
        caixa,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )

    entrada.requestFocus()
}


// ===============================
// EXECUTAR PESQUISA
// ===============================

private fun executarPesquisa(
    termoOriginal: String
) {

    val termo =
        termoOriginal.trim()

    if (termo.isEmpty()) {

        Toast.makeText(
            this,
            "Digite algo para pesquisar.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val resultado =
        filmes.filter {

            it.titulo.contains(
                termo,
                ignoreCase = true
            )
        }

    if (resultado.isEmpty()) {

        mostrarMensagem(
            "🔎\n\nNenhum resultado encontrado."
        )

        return
    }

    mostrarListaCards(
        resultado
    )
}


// ===============================
// MENSAGEM
// ===============================

private fun mostrarMensagem(
    mensagem: String
) {

    conteudo.removeAllViews()

    val texto =
        TextView(this)

    texto.text = mensagem

    texto.textSize = 24f

    texto.setTextColor(
        Color.WHITE
    )

    texto.gravity =
        Gravity.CENTER

    texto.setPadding(
        dp(30),
        dp(30),
        dp(30),
        dp(30)
    )

    conteudo.addView(
        texto,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
    )
}


// ===============================
// FOCO VERMELHO
// ===============================

private fun aplicarFocoVermelho(
    view: View
) {

    val fundo =
        GradientDrawable()

    fundo.setColor(
        Color.rgb(
            35,
            0,
            0
        )
    )

    fundo.setStroke(
        dp(3),
        Color.RED
    )

    fundo.cornerRadius =
        dp(8).toFloat()

    view.background = fundo
}


// ===============================
// DP
// ===============================

private fun dp(
    valor: Int
): Int {

    return (
        valor *
        resources.displayMetrics.density
    ).toInt()
}


// ===============================
// BACK
// ===============================

override fun onBackPressed() {

    if (menuAberto) {

        fecharMenu()

        return
    }

    super.onBackPressed()
}// ===============================
// PARTE 4/4
// ===============================

private fun marcarFavorito(filme: Filme) {

    if (favoritosSalvos.contains(filme.titulo)) {

        favoritosSalvos.remove(filme.titulo)

    } else {

        favoritosSalvos.add(filme.titulo)
    }

    salvarDados()
}


// ===============================
// SALVAR DADOS
// ===============================

private fun salvarDados() {

    val preferencias =
        getSharedPreferences(
            "WOLF_DADOS",
            MODE_PRIVATE
        )

    preferencias.edit()
        .putStringSet(
            "favoritos",
            favoritosSalvos
        )
        .apply()
}


// ===============================
// CARREGAR DADOS
// ===============================

private fun carregarDados() {

    val preferencias =
        getSharedPreferences(
            "WOLF_DADOS",
            MODE_PRIVATE
        )

    favoritosSalvos.clear()

    favoritosSalvos.addAll(
        preferencias.getStringSet(
            "favoritos",
            emptySet()
        ) ?: emptySet()
    )
}


// ===============================
// INICIALIZAR DADOS
// ===============================

private fun prepararDados() {

    carregarDados()

    if (!::botaoMenu.isInitialized) {
        return
    }

    botaoMenu.setOnClickListener {

        abrirMenu()
    }

    botaoMenu.setOnKeyListener {
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

            abrirMenu()

            true

        } else {

            false
        }
    }

    botaoMenu.setOnFocusChangeListener {
            view,
            focado ->

        if (focado) {

            aplicarFocoVermelho(
                view
            )

        } else {

            view.setBackgroundColor(
                Color.TRANSPARENT
            )
        }
    }
}


// ===============================
// NAVEGAÇÃO DO CONTROLE
// ===============================

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


// ===============================
// SALVAR ÚLTIMO VÍDEO
// ===============================

private fun salvarProgresso(
    filme: Filme,
    posicao: Long
) {

    if (posicao <= 0) {
        return
    }

    progressoVideos[
        filme.titulo
    ] = posicao

    val preferencias =
        getSharedPreferences(
            "WOLF_PROGRESSO",
            MODE_PRIVATE
        )

    preferencias.edit()
        .putLong(
            filme.titulo,
            posicao
        )
        .apply()
}


// ===============================
// CARREGAR PROGRESSOS
// ===============================

private fun carregarProgressos() {

    val preferencias =
        getSharedPreferences(
            "WOLF_PROGRESSO",
            MODE_PRIVATE
        )

    progressoVideos.clear()

    for (filme in filmes) {

        val valor =
            preferencias.getLong(
                filme.titulo,
                0L
            )

        if (valor > 0L) {

            progressoVideos[
                filme.titulo
            ] = valor
        }
    }
}


// ===============================
// LIMPAR PROGRESSO
// ===============================

private fun limparProgresso(
    filme: Filme
) {

    progressoVideos.remove(
        filme.titulo
    )

    val preferencias =
        getSharedPreferences(
            "WOLF_PROGRESSO",
            MODE_PRIVATE
        )

    preferencias.edit()
        .remove(filme.titulo)
        .apply()
}


// ===============================
// FINAL DA ACTIVITY
// ===============================

override fun onResume() {

    super.onResume()

    if (::botaoMenu.isInitialized) {

        botaoMenu.post {

            botaoMenu.clearFocus()
        }
    }
}


// ===============================
// FIM DA MAIN ACTIVITY
// ===============================

}
