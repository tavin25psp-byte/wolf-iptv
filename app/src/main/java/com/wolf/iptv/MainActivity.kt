package com.wolf.iptv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
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
    private lateinit var botaoFecharMenu: TextView

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

    private val cardsAtuais =
        mutableListOf<View>()

    private var indiceCardAtual = 0

    private val favoritos =
        mutableSetOf<String>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        criarInterface()

        carregarFilmes()
        carregarSeries()

        mostrarListaCards(filmes)
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

        fundo.alpha = 0.55f

        carregarImagem(
            "https://i.postimg.cc/nzZBfj9r/file-00000000df30820ea4ab1e688774fb21.png",
            fundo
        )

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        botaoMenu = TextView(this)

        botaoMenu.text =
            "☰  WOLF MENU"

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.textSize = 20f

        botaoMenu.setTypeface(
            null,
            Typeface.BOLD
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable = true
        botaoMenu.isFocusableInTouchMode = true
        botaoMenu.isClickable = true

        botaoMenu.background =
            criarFundoCard(false)

        botaoMenu.setOnFocusChangeListener {
                _, foco ->

            botaoMenu.background =
                criarFundoCard(foco)
        }

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        camada.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(235),
                dp(58)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        val scroll =
            ScrollView(this)

        scroll.isFocusable = false
        scroll.isFocusableInTouchMode = false

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        scroll.addView(
            conteudo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        camada.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun criarFundoCard(
        foco: Boolean
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            if (foco) {
                Color.argb(
                    235,
                    10,
                    10,
                    10
                )
            } else {
                Color.argb(
                    190,
                    10,
                    10,
                    10
                )
            }
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        if (foco) {

            fundo.setStroke(
                dp(4),
                Color.RED
            )
        }

        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {

        val borda =
            GradientDrawable()

        borda.setColor(
            Color.TRANSPARENT
        )

        borda.cornerRadius =
            dp(8).toFloat()

        borda.setStroke(
            dp(4),
            Color.RED
        )

        return borda
    }

    private fun carregarImagem(
        url: String,
        imagem: ImageView
    ) {

        if (url.isBlank()) {
            return
        }

        val existente =
            cacheCapas[url]

        if (existente != null) {

            imagem.setImageBitmap(
                existente
            )

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
                    15000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        conexao.inputStream
                    )

                conexao.disconnect()

                if (bitmap != null) {

                    cacheCapas[url] =
                        bitmap

                    runOnUiThread {

                        imagem.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (
                _: Exception
            ) {
            }
        }
    }// ===============================
// FILMES - PARTE 1
// ===============================

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
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Moana.mp4"
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
                "Golpe Explosivo",
                2025,
                "Ação",
                "https://i.postimg.cc/HnD0xZNY/images-(19).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Golpe%20Explosivo.mp4"
            ),

            Filme(
                "Operação Sombra",
                2025,
                "Ação",
                "https://i.postimg.cc/k5SB4JPc/images-(22).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Opera%C3%A7%C3%A3o%20Sombra.mp4"
            ),

            Filme(
                "A Morte de Robin Hood",
                2025,
                "Aventura",
                "https://i.postimg.cc/J7fYDKXH/images-(17).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/A%20Morte%20de%20Robin%20Hood.mp4"
            ),

            Filme(
                "Mortal Kombat 2",
                2025,
                "Ação",
                "https://i.postimg.cc/ZRxyK7Fh/images-(13).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Mortal%20Kombat%202.mp4"
            ),

            Filme(
                "Destruição Iminente",
                2024,
                "Ação",
                "https://i.postimg.cc/HxPwwvRd/20260730-destruicao-iminente-papo-de-cinema-cartaz-(1).webp",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Destrui%C3%A7%C3%A3o%20Iminente.mp4"
            ),

            Filme(
                "Planeta dos Macacos: O Reinado",
                2024,
                "Ação",
                "https://i.postimg.cc/fL1HwP8X/MV5BYTkw-Mm-Iy-NWQt-NWEy-ZS00M2M3LTgx-ZGEt-MDlm-MGIz-ZGM4NWNh-Xk-Ey-Xk-Fqc-Gc-V1-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Planeta%20dos%20Macacos%20O%20Reinado.mp4"
            ),

            Filme(
                "Divertida Mente 2",
                2024,
                "Animação",
                "https://i.postimg.cc/0jgfgwN6/aa61ae8fb015160d802c4d5cb4fe6858058ea76485c3498ed9ff431eee4fc83f-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Divertida%20Mente%202.mp4"
            ),

            Filme(
                "Kung Fu Panda 4",
                2024,
                "Animação",
                "https://i.postimg.cc/XYyYqBNV/kung-fu-panda-4-cartaz-1zso1c-717x1200-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Kung%20Fu%20Panda%204.mp4"
            ),

            Filme(
                "Ghostbusters: Apocalipse de Gelo",
                2024,
                "Ação",
                "https://i.postimg.cc/1zh5qR4V/2523057-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Ghostbusters%20Apocalipse%20de%20Gelo.mp4"
            )
        )
    )        Filme(
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
            "https://i.postimg.cc/5t0bhHHr/images-(6).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Meu%20Malvado%20Favorito%204.mp4"
        ),

        Filme(
            "Gran Turismo – De Jogador a Corredor",
            2023,
            "Ação",
            "https://i.postimg.cc/YSVWsL2p/images-(7).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Gran%20Turismo%20%E2%80%93%20De%20Jogador%20a%20Corredor.mp4"
        ),

        Filme(
            "Besouro Azul",
            2023,
            "Ação",
            "https://i.postimg.cc/fTjYRbCr/images-(9).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Besouro%20Azul.mp4"
        ),

        Filme(
            "A Pequena Sereia",
            2023,
            "Aventura",
            "https://i.postimg.cc/vmDPnnSb/images-(10).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/A%20Pequena%20Sereia.mp4"
        ),

        Filme(
            "Sonic 2: O Filme",
            2022,
            "Aventura",
            "https://i.postimg.cc/VkMCSkzP/images-(16).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Sonic%202%20O%20Filme.mp4"
        ),

        Filme(
            "A Fera",
            2022,
            "Ação",
            "https://i.postimg.cc/GhQzSKW1/images-(11).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/A%20Fera.mp4"
        ),

        Filme(
            "O Projeto Adam",
            2022,
            "Ficção",
            "https://i.postimg.cc/rp8T2fm3/images-(15).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/O%20Projeto%20Adam.mp4"
        ),

        Filme(
            "Uncharted: Fora do Mapa",
            2022,
            "Aventura",
            "https://i.postimg.cc/wvNYhZbd/Uncharted-Official-Poster-(1).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Uncharted%20Fora%20do%20Mapa.mp4"
        ),

        Filme(
            "Morbius",
            2022,
            "Ação",
            "https://i.postimg.cc/YCTX10c0/Morbius-cartaz-(1).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Morbius.mp4"
        ),

        Filme(
            "Coringa",
            2019,
            "Drama",
            "https://i.postimg.cc/yWC72Btj/Joker-(2019)-(1).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Coringa.mp4"
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
        ),

        Filme(
            "Fogo Contra Fogo",
            1995,
            "Ação",
            "https://i.postimg.cc/P5PCgb6Q/images-(20).jpg",
            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Fogo%20Contra%20Fogo.mp4"
        )
    )

    filmes.sortWith(
        compareByDescending<Filme> {
            it.ano
        }.thenBy {
            it.titulo
        }
    )
}


// ===============================
// LISTA DE FILMES
// ===============================

private fun mostrarListaCards(lista: List<Filme>) {

    historicoConteudo.add {
        mostrarListaCards(filmes)
    }

    conteudo.removeAllViews()
    cardsAtuais.clear()
    indiceCardAtual = 0

    if (lista.isEmpty()) {

        val vazio = TextView(this)

        vazio.text = "Nenhum filme encontrado."
        vazio.setTextColor(Color.WHITE)
        vazio.textSize = 20f
        vazio.gravity = Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        return
    }

    val quantidadePorLinha = 5
    var linha: LinearLayout? = null

    lista.forEachIndexed { indice, filme ->

        if (indice % quantidadePorLinha == 0) {

            linha = LinearLayout(this)

            linha!!.orientation =
                LinearLayout.HORIZONTAL

            linha!!.gravity =
                Gravity.CENTER

            linha!!.setPadding(
                0,
                dp(5),
                0,
                dp(5)
            )

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )
            )
        }

        val card = criarCard(filme)

        linha!!.addView(
            card,
            LinearLayout.LayoutParams(
                dp(335),
                dp(330)
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        cardsAtuais.add(card)
    }

    if (cardsAtuais.isNotEmpty()) {
        cardsAtuais[0].requestFocus()
    }
}// ===============================
// CARD DE FILME
// ===============================

private fun criarCard(filme: Filme): FrameLayout {

    val card = FrameLayout(this)

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true

    card.background = criarFundoCard(false)

    val imagem = ImageView(this)

    imagem.scaleType =
        ImageView.ScaleType.FIT_CENTER

    card.addView(
        imagem,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(275)
        )
    )

    carregarImagem(
        filme.capa,
        imagem
    )

    val informacoes = LinearLayout(this)

    informacoes.orientation =
        LinearLayout.VERTICAL

    informacoes.gravity =
        Gravity.CENTER_VERTICAL

    informacoes.setPadding(
        dp(8),
        dp(3),
        dp(8),
        dp(3)
    )

    val parametrosInfo =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )

    parametrosInfo.gravity =
        Gravity.BOTTOM

    card.addView(
        informacoes,
        parametrosInfo
    )

    val titulo = TextView(this)

    titulo.text = filme.titulo
    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 14f

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.maxLines = 2

    titulo.ellipsize =
        TextUtils.TruncateAt.END

    informacoes.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val detalhes = TextView(this)

    detalhes.text =
        "${filme.ano} • ${filme.categoria}"

    detalhes.setTextColor(
        Color.LTGRAY
    )

    detalhes.textSize = 12f

    informacoes.addView(
        detalhes,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val borda = View(this)

    borda.background =
        criarBordaVermelha()

    borda.visibility =
        View.GONE

    card.addView(
        borda,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    card.setOnFocusChangeListener { _, temFoco ->

        borda.visibility =
            if (temFoco) {
                View.VISIBLE
            } else {
                View.GONE
            }

        card.background =
            criarFundoCard(temFoco)
    }

    card.setOnClickListener {

        indiceCardAtual =
            cardsAtuais.indexOf(card)
                .coerceAtLeast(0)

        abrirVideo(filme)
    }

    return card
}


// ===============================
// SÉRIES
// ===============================

private fun carregarSeries() {

    series.clear()

    series.add(

        Serie(
            titulo = "Spider Noir",
            categoria = "Ação",
            capa = "https://i.postimg.cc/R0dfWdj7/alright-people-whove-seen-spider-noir-should-i-watch-it-in-v0-73e3ew89v94h1-(1).jpg",

            temporadas = listOf(

                Temporada(
                    numero = 1,

                    episodios = listOf(

                        Episodio(
                            1,
                            "Entre no meu Escritório",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%201%20-%20Entre%20no%20meu%20Escrit%C3%B3rio.mp4"
                        ),

                        Episodio(
                            2,
                            "Pisando em Ovos",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%202%20-%20Pisando%20em%20Ovos.mp4"
                        ),

                        Episodio(
                            3,
                            "Falsidade",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%203%20-%20Falsidade.mp4"
                        ),

                        Episodio(
                            4,
                            "Nunca Repita o Mesmo Erro",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%204%20-%20Nunca%20Repita%20o%20Mesmo%20Erro.mp4"
                        ),

                        Episodio(
                            5,
                            "Traição",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%205%20-%20Trai%C3%A7%C3%A3o.mp4"
                        ),

                        Episodio(
                            6,
                            "Pesadelo na Maca",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%206%20-%20Pesadelo%20na%20Maca.mp4"
                        ),

                        Episodio(
                            7,
                            "Herói de Ninguém",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%207%20-%20Her%C3%B3i%20de%20Ningu%C3%A9m.mp4"
                        ),

                        Episodio(
                            8,
                            "O Homem Mascarado",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%208%20-%20O%20Homem%20Mascarado.mp4"
                        )
                    )
                )
            )
        )
    )

    series.add(

        Serie(
            titulo = "Avatar: A Lenda de Aang",
            categoria = "Aventura",
            capa = "https://i.postimg.cc/BvWjsB3G/Avatar-The-Last-Airbender-2024-series-poster-(1).jpg",

            temporadas = listOf(

                Temporada(
                    numero = 1,

                    episodios = listOf(

                        Episodio(
                            1,
                            "Aang",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%201%20-%20Aang.mp4"
                        ),

                        Episodio(
                            2,
                            "Guerreiros",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%202%20-%20Guerreiros.mp4"
                        ),

                        Episodio(
                            3,
                            "Omashu",
                            "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%203%20-%20Omashu.mp4"
                        ),

                        Episodio(
                            4,
                            "No Escuro",
                            "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%204%20-%20No%20escuro.mp4"
                        ),

                        Episodio(
                            5,
                            "O Mundo Espiritual",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%205%20-%20O%20Mundo%20Espiritual.mp4"
                        ),

                        Episodio(
                            6,
                            "Máscaras",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%206%20-%20M%C3%A1scaras.mp4"
                        ),

                        Episodio(
                            7,
                            "O Norte",
                            "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%207%20-%20O%20Norte.mp4"
                        ),

                        Episodio(
                            8,
                            "Episódio 8",
                            "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%208%20-%20Epis%C3%B3dio%208.mp4"
                        )
                    )
                )
            )
        )
    )
}// ===============================
// LISTA DE SÉRIES
// ===============================

private fun mostrarListaSeries(lista: List<Serie>) {

    historicoConteudo.add {
        mostrarListaSeries(series)
    }

    conteudo.removeAllViews()
    cardsAtuais.clear()
    indiceCardAtual = 0

    if (lista.isEmpty()) {

        val vazio = TextView(this)

        vazio.text = "Nenhuma série encontrada."
        vazio.setTextColor(Color.WHITE)
        vazio.textSize = 20f
        vazio.gravity = Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        return
    }

    val quantidadePorLinha = 5
    var linha: LinearLayout? = null

    lista.forEachIndexed { indice, serie ->

        if (indice % quantidadePorLinha == 0) {

            linha = LinearLayout(this)

            linha!!.orientation =
                LinearLayout.HORIZONTAL

            linha!!.gravity =
                Gravity.CENTER

            linha!!.setPadding(
                0,
                dp(5),
                0,
                dp(5)
            )

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )
            )
        }

        val card = criarCardSerie(serie)

        linha!!.addView(
            card,
            LinearLayout.LayoutParams(
                dp(335),
                dp(330)
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        cardsAtuais.add(card)
    }

    if (cardsAtuais.isNotEmpty()) {
        cardsAtuais[0].requestFocus()
    }
}


// ===============================
// CARD DE SÉRIE
// ===============================

private fun criarCardSerie(
    serie: Serie
): FrameLayout {

    val card = FrameLayout(this)

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true

    card.background =
        criarFundoCard(false)

    val imagem = ImageView(this)

    imagem.scaleType =
        ImageView.ScaleType.FIT_CENTER

    card.addView(
        imagem,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(275)
        )
    )

    carregarImagem(
        serie.capa,
        imagem
    )

    val informacoes = LinearLayout(this)

    informacoes.orientation =
        LinearLayout.VERTICAL

    informacoes.gravity =
        Gravity.CENTER_VERTICAL

    informacoes.setPadding(
        dp(8),
        dp(3),
        dp(8),
        dp(3)
    )

    val parametros =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )

    parametros.gravity =
        Gravity.BOTTOM

    card.addView(
        informacoes,
        parametros
    )

    val titulo = TextView(this)

    titulo.text = serie.titulo
    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 14f

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.maxLines = 2

    titulo.ellipsize =
        TextUtils.TruncateAt.END

    informacoes.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val quantidade = serie.temporadas.sumOf {
        it.episodios.size
    }

    val detalhes = TextView(this)

    detalhes.text =
        "${serie.categoria} • $quantidade episódios"

    detalhes.setTextColor(
        Color.LTGRAY
    )

    detalhes.textSize = 12f

    informacoes.addView(
        detalhes,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val borda = View(this)

    borda.background =
        criarBordaVermelha()

    borda.visibility =
        View.GONE

    card.addView(
        borda,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    card.setOnFocusChangeListener { _, foco ->

        borda.visibility =
            if (foco) {
                View.VISIBLE
            } else {
                View.GONE
            }

        card.background =
            criarFundoCard(foco)
    }

    card.setOnClickListener {

        indiceCardAtual =
            cardsAtuais.indexOf(card)
                .coerceAtLeast(0)

        mostrarTemporadas(serie)
    }

    return card
}


// ===============================
// TEMPORADAS
// ===============================

private fun mostrarTemporadas(
    serie: Serie
) {

    historicoConteudo.add {
        mostrarListaSeries(series)
    }

    conteudo.removeAllViews()
    cardsAtuais.clear()
    indiceCardAtual = 0

    val titulo = TextView(this)

    titulo.text =
        "${serie.titulo} - TEMPORADAS"

    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 22f

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER_VERTICAL

    titulo.setPadding(
        dp(20),
        0,
        0,
        0
    )

    conteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    val quantidadePorLinha = 5
    var linha: LinearLayout? = null

    serie.temporadas.forEachIndexed {
        indice,
        temporada ->

        if (indice % quantidadePorLinha == 0) {

            linha = LinearLayout(this)

            linha!!.orientation =
                LinearLayout.HORIZONTAL

            linha!!.gravity =
                Gravity.CENTER

            linha!!.setPadding(
                0,
                dp(5),
                0,
                dp(5)
            )

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(240)
                )
            )
        }

        val card =
            criarCardTemporada(
                temporada,
                serie
            )

        linha!!.addView(
            card,
            LinearLayout.LayoutParams(
                dp(335),
                dp(220)
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        cardsAtuais.add(card)
    }

    if (cardsAtuais.isNotEmpty()) {
        cardsAtuais[0].requestFocus()
    }
}


// ===============================
// CARD TEMPORADA
// ===============================

private fun criarCardTemporada(
    temporada: Temporada,
    serie: Serie
): FrameLayout {

    val card = FrameLayout(this)

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true

    card.background =
        criarFundoCard(false)

    val texto = TextView(this)

    texto.text =
        "TEMPORADA ${temporada.numero}\n\n" +
        "${temporada.episodios.size} episódios"

    texto.setTextColor(Color.WHITE)
    texto.textSize = 18f

    texto.setTypeface(
        null,
        Typeface.BOLD
    )

    texto.gravity =
        Gravity.CENTER

    card.addView(
        texto,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val borda = View(this)

    borda.background =
        criarBordaVermelha()

    borda.visibility =
        View.GONE

    card.addView(
        borda,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    card.setOnFocusChangeListener { _, foco ->

        borda.visibility =
            if (foco) {
                View.VISIBLE
            } else {
                View.GONE
            }

        card.background =
            criarFundoCard(foco)
    }

    card.setOnClickListener {

        indiceCardAtual =
            cardsAtuais.indexOf(card)
                .coerceAtLeast(0)

        mostrarEpisodios(
            serie,
            temporada
        )
    }

    return card
}// ===============================
// EPISÓDIOS
// ===============================

private fun mostrarEpisodios(
    serie: Serie,
    temporada: Temporada
) {

    historicoConteudo.add {
        mostrarTemporadas(serie)
    }

    conteudo.removeAllViews()
    cardsAtuais.clear()
    indiceCardAtual = 0

    val titulo = TextView(this)

    titulo.text =
        "${serie.titulo} - TEMPORADA ${temporada.numero}"

    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 22f

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER_VERTICAL

    titulo.setPadding(
        dp(20),
        0,
        0,
        0
    )

    conteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    val quantidadePorLinha = 5
    var linha: LinearLayout? = null

    temporada.episodios.forEachIndexed {
        indice,
        episodio ->

        if (indice % quantidadePorLinha == 0) {

            linha = LinearLayout(this)

            linha!!.orientation =
                LinearLayout.HORIZONTAL

            linha!!.gravity =
                Gravity.CENTER

            linha!!.setPadding(
                0,
                dp(5),
                0,
                dp(5)
            )

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(240)
                )
            )
        }

        val card =
            criarCardEpisodio(
                episodio,
                serie
            )

        linha!!.addView(
            card,
            LinearLayout.LayoutParams(
                dp(335),
                dp(220)
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        cardsAtuais.add(card)
    }

    if (cardsAtuais.isNotEmpty()) {
        cardsAtuais[0].requestFocus()
    }
}


// ===============================
// CARD DE EPISÓDIO
// ===============================

private fun criarCardEpisodio(
    episodio: Episodio,
    serie: Serie
): FrameLayout {

    val card = FrameLayout(this)

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true

    card.background =
        criarFundoCard(false)

    val texto = TextView(this)

    texto.text =
        "EPISÓDIO ${episodio.numero}\n\n" +
        episodio.titulo

    texto.setTextColor(Color.WHITE)
    texto.textSize = 17f

    texto.setTypeface(
        null,
        Typeface.BOLD
    )

    texto.gravity =
        Gravity.CENTER

    texto.maxLines = 4

    texto.ellipsize =
        TextUtils.TruncateAt.END

    card.addView(
        texto,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val borda = View(this)

    borda.background =
        criarBordaVermelha()

    borda.visibility =
        View.GONE

    card.addView(
        borda,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    card.setOnFocusChangeListener { _, foco ->

        borda.visibility =
            if (foco) {
                View.VISIBLE
            } else {
                View.GONE
            }

        card.background =
            criarFundoCard(foco)
    }

    card.setOnClickListener {

        indiceCardAtual =
            cardsAtuais.indexOf(card)
                .coerceAtLeast(0)

        abrirVideo(
            episodio.titulo,
            episodio.video,
            serie.capa
        )
    }

    return card
}


// ===============================
// ABRIR VÍDEO - FILME
// ===============================

private fun abrirVideo(
    filme: Filme
) {

    if (filme.video.isBlank()) {

        Toast.makeText(
            this,
            "Vídeo indisponível.",
            Toast.LENGTH_SHORT
        ).show()

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


// ===============================
// ABRIR VÍDEO - EPISÓDIO
// ===============================

private fun abrirVideo(
    titulo: String,
    video: String,
    capa: String
) {

    if (video.isBlank()) {

        Toast.makeText(
            this,
            "Vídeo indisponível.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    val intent =
        Intent(
            this,
            PlayerActivity::class.java
        )

    intent.putExtra(
        "VIDEO_URL",
        video
    )

    intent.putExtra(
        "VIDEO_TITLE",
        titulo
    )

    intent.putExtra(
        "VIDEO_COVER",
        capa
    )

    startActivity(intent)
}// ===============================
// TÍTULO DO MENU
// ===============================

private fun adicionarTituloMenu(
    texto: String
) {
    val titulo = TextView(this)

    titulo.text = texto
    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 16f
    titulo.setTypeface(null, Typeface.BOLD)

    titulo.setPadding(
        dp(15),
        dp(12),
        dp(10),
        dp(8)
    )

    menuConteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(45)
        )
    )
}


// ===============================
// ITEM DO MENU
// ===============================

private fun adicionarItemMenu(
    texto: String,
    acao: () -> Unit
) {
    val item = TextView(this)

    item.text = texto
    item.setTextColor(Color.WHITE)
    item.textSize = 15f
    item.gravity = Gravity.CENTER_VERTICAL

    item.setPadding(
        dp(15),
        0,
        dp(10),
        0
    )

    item.isFocusable = true
    item.isFocusableInTouchMode = true
    item.isClickable = true

    item.setOnFocusChangeListener { _, foco ->

        if (foco) {
            item.setTextColor(Color.RED)
            item.background =
                criarBordaVermelha()
        } else {
            item.setTextColor(Color.WHITE)
            item.background = null
        }
    }

    item.setOnClickListener {
        acao()
    }

    menuConteudo.addView(
        item,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )
    )

    itensMenuFoco.add(item)
}


// ===============================
// ABRIR MENU
// ===============================

private fun abrirMenu() {

    if (menuAberto) return

    menuAberto = true

    itensMenuFoco.clear()

    menuLateral = LinearLayout(this)

    menuLateral.orientation =
        LinearLayout.VERTICAL

    menuLateral.setBackgroundColor(
        Color.rgb(12, 12, 12)
    )

    val parametros =
        FrameLayout.LayoutParams(
            dp(420),
            ViewGroup.LayoutParams.MATCH_PARENT
        )

    parametros.gravity =
        Gravity.TOP or Gravity.END

    parametros.setMargins(
        0,
        dp(10),
        dp(10),
        dp(10)
    )

    raiz.addView(
        menuLateral,
        parametros
    )

    val topo = LinearLayout(this)

    topo.orientation =
        LinearLayout.HORIZONTAL

    topo.gravity =
        Gravity.CENTER_VERTICAL

    menuLateral.addView(
        topo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    val titulo = TextView(this)

    titulo.text = "WOLF MENU"
    titulo.setTextColor(Color.WHITE)
    titulo.textSize = 20f
    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER_VERTICAL

    titulo.setPadding(
        dp(15),
        0,
        0,
        0
    )

    topo.addView(
        titulo,
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
    )

    botaoFecharMenu = TextView(this)

    botaoFecharMenu.text = "✕"
    botaoFecharMenu.textSize = 24f
    botaoFecharMenu.setTextColor(Color.WHITE)
    botaoFecharMenu.gravity = Gravity.CENTER

    botaoFecharMenu.isFocusable = true
    botaoFecharMenu.isFocusableInTouchMode = true
    botaoFecharMenu.isClickable = true

    botaoFecharMenu.setOnFocusChangeListener {
        _, foco ->

        if (foco) {
            botaoFecharMenu.setTextColor(
                Color.RED
            )

            botaoFecharMenu.background =
                criarBordaVermelha()

        } else {
            botaoFecharMenu.setTextColor(
                Color.WHITE
            )

            botaoFecharMenu.background = null
        }
    }

    botaoFecharMenu.setOnClickListener {
        fecharMenu()
    }

    topo.addView(
        botaoFecharMenu,
        LinearLayout.LayoutParams(
            dp(60),
            dp(55)
        )
    )

    menuScroll = ScrollView(this)

    menuConteudo = LinearLayout(this)

    menuConteudo.orientation =
        LinearLayout.VERTICAL

    menuScroll.addView(
        menuConteudo,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    menuLateral.addView(
        menuScroll,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    )

    adicionarItemMenu(
        "▶ Continuar assistindo"
    ) {
        Toast.makeText(
            this,
            "Continue assistindo",
            Toast.LENGTH_SHORT
        ).show()
    }

    adicionarItemMenu(
        "★ Favoritos (${favoritos.size})"
    ) {
        mostrarFavoritos()
    }

    adicionarItemMenu(
        "⌕ Pesquisa"
    ) {
        abrirPesquisa()
    }

    adicionarTituloMenu(
        "FILMES"
    )

    adicionarItemMenu(
        "🎬 Todos os filmes (${filmes.size})"
    ) {
        fecharMenu()
        mostrarListaCards(filmes)
    }

    val categoriasFilmes = listOf(
        "Ação",
        "Aventura",
        "Animação",
        "Comédia",
        "Drama",
        "Terror",
        "Ficção"
    )

    categoriasFilmes.forEach { categoria ->

        val quantidade =
            filmes.count {
                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "$categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            )
        }
    }

    adicionarTituloMenu(
        "SÉRIES"
    )

    adicionarItemMenu(
        "📺 Todas as séries (${series.size})"
    ) {
        fecharMenu()
        mostrarListaSeries(series)
    }

    val categoriasSeries = listOf(
        "Ação",
        "Aventura",
        "Comédia",
        "Drama",
        "Terror"
    )

    categoriasSeries.forEach { categoria ->

        val quantidade =
            series.count {
                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "$categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            )
        }
    }

    adicionarTituloMenu(
        "DORAMAS"
    )

    adicionarItemMenu(
        "🎭 Todos os doramas (${doramas.size})"
    ) {
        fecharMenu()
        mostrarListaSeries(doramas)
    }

    listOf(
        "Ação",
        "Romance",
        "Drama",
        "Comédia"
    ).forEach { categoria ->

        val quantidade =
            doramas.count {
                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "$categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            )
        }
    }

    adicionarTituloMenu(
        "ANIME"
    )

    adicionarItemMenu(
        "⚡ Todos os animes (${animes.size})"
    ) {
        fecharMenu()
        mostrarListaSeries(animes)
    }

    listOf(
        "Ação",
        "Aventura",
        "Comédia",
        "Fantasia"
    ).forEach { categoria ->

        val quantidade =
            animes.count {
                it.categoria.equals(
                    categoria,
                    ignoreCase = true
                )
            }

        adicionarItemMenu(
            "$categoria ($quantidade)"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            )
        }
    }

    if (itensMenuFoco.isNotEmpty()) {
        itensMenuFoco[0].requestFocus()
    }
}


// ===============================
// FECHAR MENU
// ===============================

private fun fecharMenu() {

    if (!menuAberto) return

    menuAberto = false

    raiz.removeView(menuLateral)

    itensMenuFoco.clear()

    if (cardsAtuais.isNotEmpty()) {

        val indice =
            indiceCardAtual.coerceIn(
                0,
                cardsAtuais.lastIndex
            )

        cardsAtuais[indice].requestFocus()

    } else {

        botaoMenu.requestFocus()
    }
}


// ===============================
// MOVER MENU
// ===============================

private fun moverMenu(
    passo: Int
) {

    if (itensMenuFoco.isEmpty()) return

    val atual =
        itensMenuFoco.indexOfFirst {
            it.hasFocus()
        }

    val indiceAtual =
        if (atual >= 0) atual else 0

    val novo =
        (indiceAtual + passo)
            .coerceIn(
                0,
                itensMenuFoco.lastIndex
            )

    itensMenuFoco[novo].requestFocus()

    ajustarScrollMenu(
        itensMenuFoco[novo]
    )
}


// ===============================
// SCROLL DO MENU
// ===============================

private fun ajustarScrollMenu(
    view: View
) {

    view.post {
        menuScroll.smoothScrollTo(
            0,
            view.top
        )
    }
}


// ===============================
// FAVORITOS
// ===============================

private fun mostrarFavoritos() {

    fecharMenu()

    val lista =
        filmes.filter {
            favoritos.contains(
                it.titulo
            )
        }

    mostrarListaCards(lista)
}


// ===============================
// PESQUISA
// ===============================

private fun abrirPesquisa() {

    fecharMenu()

    conteudo.removeAllViews()
    cardsAtuais.clear()
    indiceCardAtual = 0

    val campo = EditText(this)

    campo.hint =
        "Digite o nome do filme..."

    campo.setTextColor(Color.WHITE)
    campo.setHintTextColor(Color.LTGRAY)
    campo.textSize = 18f

    campo.setSingleLine(true)

    campo.isFocusable = true
    campo.isFocusableInTouchMode = true

    conteudo.addView(
        campo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    campo.requestFocus()

    campo.addTextChangedListener(
        object : android.text.TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                val texto =
                    s?.toString()
                        ?.trim()
                        ?.lowercase()
                        ?: ""

                val resultado =
                    if (texto.isEmpty()) {
                        filmes
                    } else {
                        filmes.filter {
                            it.titulo
                                .lowercase()
                                .contains(texto)
                        }
                    }

                mostrarResultadoPesquisa(
                    resultado
                )
            }

            override fun afterTextChanged(
                s: android.text.Editable?
            ) {
            }
        }
    )
}


// ===============================
// RESULTADO DA PESQUISA
// ===============================

private fun mostrarResultadoPesquisa(
    lista: List<Filme>
) {

    if (conteudo.childCount > 1) {

        conteudo.removeViews(
            1,
            conteudo.childCount - 1
        )
    }

    cardsAtuais.clear()
    indiceCardAtual = 0

    val quantidadePorLinha = 5
    var linha: LinearLayout? = null

    lista.forEachIndexed { indice, filme ->

        if (indice % quantidadePorLinha == 0) {

            linha = LinearLayout(this)

            linha!!.orientation =
                LinearLayout.HORIZONTAL

            linha!!.gravity =
                Gravity.CENTER

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )
            )
        }

        val card = criarCard(filme)

        linha!!.addView(
            card,
            LinearLayout.LayoutParams(
                dp(335),
                dp(330)
            ).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        )

        cardsAtuais.add(card)
    }

    if (cardsAtuais.isNotEmpty()) {
        cardsAtuais[0].requestFocus()
    }
}// ===============================
// MOVER CARD
// ===============================

private fun moverCard(
    passo: Int
) {

    if (cardsAtuais.isEmpty()) return

    val atual =
        cardsAtuais.indexOfFirst {
            it.hasFocus()
        }

    val indiceAtual =
        if (atual >= 0) {
            atual
        } else {
            indiceCardAtual
        }

    val novo =
        (indiceAtual + passo)
            .coerceIn(
                0,
                cardsAtuais.lastIndex
            )

    indiceCardAtual = novo

    cardsAtuais[novo].requestFocus()
}


// ===============================
// D-PAD
// ===============================

override fun dispatchKeyEvent(
    event: KeyEvent
): Boolean {

    if (event.action != KeyEvent.ACTION_DOWN) {
        return super.dispatchKeyEvent(event)
    }

    // ===========================
    // MENU ABERTO
    // ===========================

    if (menuAberto) {

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_UP -> {
                moverMenu(-1)
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moverMenu(1)
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                val foco =
                    if (botaoFecharMenu.hasFocus()) {

                        botaoFecharMenu

                    } else {

                        itensMenuFoco
                            .firstOrNull {
                                it.hasFocus()
                            }
                    }

                foco?.performClick()

                return true
            }

            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_MENU -> {

                fecharMenu()

                return true
            }
        }

        return true
    }


    // ===========================
    // MENU FECHADO
    // ===========================

    when (event.keyCode) {

        // =========================
        // CIMA
        // =========================

        KeyEvent.KEYCODE_DPAD_UP -> {

            // Se estiver no botão WOLF,
            // não sobe mais.

            if (botaoMenu.hasFocus()) {
                return true
            }

            val atual =
                cardsAtuais.indexOfFirst {
                    it.hasFocus()
                }

            // Primeiro card -> botão WOLF

            if (atual == 0) {

                botaoMenu.requestFocus()

                return true
            }

            // Sobe exatamente uma fileira

            moverCard(-5)

            return true
        }


        // =========================
        // BAIXO
        // =========================

        KeyEvent.KEYCODE_DPAD_DOWN -> {

            // Botão WOLF -> primeiro card

            if (botaoMenu.hasFocus()) {

                if (cardsAtuais.isNotEmpty()) {

                    indiceCardAtual = 0

                    cardsAtuais[0].requestFocus()
                }

                return true
            }

            // Desce exatamente uma fileira

            moverCard(5)

            return true
        }


        // =========================
        // ESQUERDA
        // =========================

        KeyEvent.KEYCODE_DPAD_LEFT -> {

            if (botaoMenu.hasFocus()) {
                return true
            }

            moverCard(-1)

            return true
        }


        // =========================
        // DIREITA
        // =========================

        KeyEvent.KEYCODE_DPAD_RIGHT -> {

            if (botaoMenu.hasFocus()) {
                return true
            }

            moverCard(1)

            return true
        }


        // =========================
        // OK / ENTER
        // =========================

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER -> {

            // OK no botão WOLF
            // abre o menu.

            if (botaoMenu.hasFocus()) {

                abrirMenu()

                return true
            }

            // OK em um card
            // abre o filme/série.

            val foco =
                cardsAtuais.firstOrNull {
                    it.hasFocus()
                }

            foco?.performClick()

            return true
        }


        // =========================
        // BOTÃO MENU DO CONTROLE
        // =========================

        KeyEvent.KEYCODE_MENU -> {

            abrirMenu()

            return true
        }


        // =========================
        // VOLTAR
        // =========================

        KeyEvent.KEYCODE_BACK -> {

            // Se houver histórico,
            // volta para a tela anterior.

            if (historicoConteudo.isNotEmpty()) {

                val voltar =
                    historicoConteudo.removeLast()

                voltar()

                return true
            }

            // Sem histórico,
            // fecha a Activity.

            finish()

            return true
        }
    }

    return super.dispatchKeyEvent(event)
}

}
