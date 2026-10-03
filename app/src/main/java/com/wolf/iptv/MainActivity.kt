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

    // ===============================
    // INTERFACE PRINCIPAL
    // ===============================

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

        botaoMenu =
            TextView(this)

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
    // FUNDO DOS CARDS
    // ===============================

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

    // ===============================
    // BORDA VERMELHA
    // ===============================

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()

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

    // ===============================
    // CARREGAR CAPAS
    // ===============================

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
    }    private fun carregarFilmes() {

        filmes.clear()

        filmes.addAll(

            listOf(

                Filme(
                    titulo = "Como Mágica",
                    ano = 2026,
                    categoria = "Comédia",
                    capa = "https://i.postimg.cc/9z5YzWLn/D-NQ-NP-674318-MLB111141123921-052026-O.webp",
                    video = "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Como%20M%C3%A1gica.mp4"
                ),

                Filme(
                    titulo = "Todo Mundo em Pânico 4",
                    ano = 2026,
                    categoria = "Comédia",
                    capa = "https://i.postimg.cc/sgfwW2VH/dfdb52dae07d0b0950bb9dfc98ab09c08e44a3f704815cc5a5a6af72d156f913.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Todo%20Mundo%20em%20P%C3%A2nico.mp4"
                ),

                Filme(
                    titulo = "Pânico 7",
                    ano = 2026,
                    categoria = "Terror",
                    capa = "https://i.postimg.cc/FzcqGbHV/images-(3).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/P%C3%A2nico%207.mp4"
                ),

                Filme(
                    titulo = "Pinóquio",
                    ano = 2026,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/ryfYVWCk/IMG-20261002-044814.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Pin%C3%B3quio.mp4"
                ),

                Filme(
                    titulo = "Moana",
                    ano = 2026,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/pdj7VwhR/moana.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Moana.mp4"
                ),

                Filme(
                    titulo = "Como Treinar o Seu Dragão",
                    ano = 2026,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/664hkrZ6/treinar.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Como%20Treinar%20o%20Seu%20Drag%C3%A3o.mp4"
                ),

                Filme(
                    titulo = "Quarteto Fantástico: Primeiro Passo",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Quarteto%20Fant%C3%A1stico%20Primeiros%20Passos.mp4"
                ),

                Filme(
                    titulo = "Homem-Aranha: Um Novo Dia",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/QCctbsqF/aranha.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filme%20hospedagem%20/Homem-Aranha%20Um%20Novo%20Dia.mp4"
                ),

                Filme(
                    titulo = "Conexão Perigosa",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/JhyRxMrH/conexao.jpg",
                    video = "https://vz-c091a331-1f1.b-cdn.net/78e46f67-4aec-4ba3-b4c4-7072cd6d921b/playlist.m3u8"
                ),

                Filme(
                    titulo = "A Odisseia",
                    ano = 2026,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/K8WjhML7/odisseia.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/BALA-CHANNEL/A%20Odisseia.mp4"
                ),

                Filme(
                    titulo = "Resident Evil",
                    ano = 2026,
                    categoria = "Terror",
                    capa = "https://i.postimg.cc/3J7DtmC4/evil.jpg",
                    video = "https://vz-c091a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8"
                ),

                Filme(
                    titulo = "Vingança",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/26M7q6P7/vinganca.jpg",
                    video = "https://vz-c091a331-1f1.b-cdn.net/10f5522b-3890-48cd-bfb7-89b2806e8269/playlist.m3u8"
                ),

                Filme(
                    titulo = "A Revolta",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/7YyLx45D/revolta.jpg",
                    video = "https://vz-c091a331-1f1.b-cdn.net/ae8d06f6-e3da-4705-9a83-313fae7114f9/playlist.m3u8"
                ),

                Filme(
                    titulo = "Thunderbolts",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/FzbPQdZJ/D-NQ-NP-848607-CBT107833899597-022026-O.webp",
                    video = "https://wolf-channel-cdn.b-cdn.net/Bala%20zip/Thunderbolts.mp4"
                ),

                Filme(
                    titulo = "Céu em Fúria",
                    ano = 2026,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/PJMnXB7d/ceu-em-furia.jpg",
                    video = ""
                ),

                Filme(
                    titulo = "Super Mario Galaxy: O Filme",
                    ano = 2026,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/W3Y1mvHd/images-(2).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Super%20Mario%20Galaxy%20O%20Filme.mp4"
                ),

                Filme(
                    titulo = "Golpe Explosivo",
                    ano = 2025,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/HnD0xZNY/images-(19).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Golpe%20Explosivo.mp4"
                ),

                Filme(
                    titulo = "Operação Sombra",
                    ano = 2025,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/k5SB4JPc/images-(22).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Opera%C3%A7%C3%A3o%20Sombra.mp4"
                ),

                Filme(
                    titulo = "A Morte de Robin Hood",
                    ano = 2025,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/J7fYDKXH/images-(17).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/A%20Morte%20de%20Robin%20Hood.mp4"
                ),

                Filme(
                    titulo = "Mortal Kombat 2",
                    ano = 2025,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/ZRxyK7Fh/images-(13).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Mortal%20Kombat%202.mp4"
                ),

                Filme(
                    titulo = "Destruição Iminente",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/HxPwwvRd/20260730-destruicao-iminente-papo-de-cinema-cartaz-(1).webp",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Destrui%C3%A7%C3%A3o%20Iminente.mp4"
                ),

                Filme(
                    titulo = "Planeta dos Macacos: O Reinado",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/fL1HwP8X/MV5BYTkw-Mm-Iy-NWQt-NWEy-ZS00M2M3LTgx-ZGEt-MDlm-MGIz-ZGM4NWNh-Xk-Ey-Xk-Fqc-Gc-V1-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Planeta%20dos%20Macacos%20O%20Reinado.mp4"
                ),

                Filme(
                    titulo = "Divertida Mente 2",
                    ano = 2024,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/0jgfgwN6/aa61ae8fb015160d802c4d5cb4fe6858058ea76485c3498ed9ff431eee4fc83f-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Divertida%20Mente%202.mp4"
                ),

                Filme(
                    titulo = "Kung Fu Panda 4",
                    ano = 2024,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/XYyYqBNV/kung-fu-panda-4-cartaz-1zso1c-717x1200-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Kung%20Fu%20Panda%204.mp4"
                ),

                Filme(
                    titulo = "Ghostbusters: Apocalipse de Gelo",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/1zh5qR4V/2523057-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Ghostbusters%20Apocalipse%20de%20Gelo.mp4"
                ),

                Filme(
                    titulo = "Mufasa: O Rei Leão",
                    ano = 2024,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/mgw3xmpz/917q-7O0TJL.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Mufasa%20O%20Rei%20Le%C3%A3o.mp4"
                ),

                Filme(
                    titulo = "Coringa: Delírio a Dois",
                    ano = 2024,
                    categoria = "Drama",
                    capa = "https://i.postimg.cc/LXN621K3/MV5BZTU0ZGI3Yz-Mt-ZTUw-MC00MGJj-LWFk-NDIt-MDUz-NTUx-Zjg5N2Y4Xk-Ey-Xk-Fqc-Gc-V1.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Coringa%20Del%C3%ADrio%20a%20Dois.mp4"
                ),

                Filme(
                    titulo = "Deadpool & Wolverine",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/2SZ6hyv9/IMG-20261002-100134.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Deadpool%20%26amp%3B%20Wolverine.mp4"
                ),

                Filme(
                    titulo = "Bad Boys: Até o Fim",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/sDv6PQmF/images-(4).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Bad%20Boys%20At%C3%A9%20o%20Fim.mp4"
                ),

                Filme(
                    titulo = "Furiosa: Uma Saga Mad Max",
                    ano = 2024,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/FzZY7Hfn/71K2Mcc-CQ2L-AC-UF894-1000-QL80.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Furiosa%20Uma%20Saga%20Mad%20Max.mp4"
                ),

                Filme(
                    titulo = "Meu Malvado Favorito 4",
                    ano = 2024,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/5t0bhHHr/images-(6).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Meu%20Malvado%20Favorito%204.mp4"
                ),

                Filme(
                    titulo = "Gran Turismo – De Jogador a Corredor",
                    ano = 2023,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/YSVWsL2p/images-(7).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Gran%20Turismo%20%E2%80%93%20De%20Jogador%20a%20Corredor.mp4"
                ),

                Filme(
                    titulo = "Besouro Azul",
                    ano = 2023,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/fTjYRbCr/images-(9).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Besouro%20Azul.mp4"
                ),

                Filme(
                    titulo = "A Pequena Sereia",
                    ano = 2023,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/vmDPnnSb/images-(10).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/A%20Pequena%20Sereia.mp4"
                ),

                Filme(
                    titulo = "Sonic 2: O Filme",
                    ano = 2022,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/VkMCSkzP/images-(16).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Sonic%202%20O%20Filme.mp4"
                ),

                Filme(
                    titulo = "A Fera",
                    ano = 2022,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/GhQzSKW1/images-(11).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/A%20Fera.mp4"
                ),

                Filme(
                    titulo = "O Projeto Adam",
                    ano = 2022,
                    categoria = "Ficção",
                    capa = "https://i.postimg.cc/rp8T2fm3/images-(15).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/O%20Projeto%20Adam.mp4"
                ),

                Filme(
                    titulo = "Uncharted: Fora do Mapa",
                    ano = 2022,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/wvNYhZbd/Uncharted-Official-Poster-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Uncharted%20Fora%20do%20Mapa.mp4"
                ),

                Filme(
                    titulo = "Morbius",
                    ano = 2022,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/YCTX10c0/Morbius-cartaz-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Morbius.mp4"
                ),

                Filme(
                    titulo = "Coringa",
                    ano = 2019,
                    categoria = "Drama",
                    capa = "https://i.postimg.cc/yWC72Btj/Joker-(2019)-(1).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Coringa.mp4"
                ),

                Filme(
                    titulo = "Jumanji: Bem-Vindo à Selva",
                    ano = 2017,
                    categoria = "Aventura",
                    capa = "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
                ),

                Filme(
                    titulo = "Kingsman: Agente Secreto",
                    ano = 2014,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/0QDbfyzB/kings.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4"
                ),

                Filme(
                    titulo = "Deu a Louca nos Bichos",
                    ano = 2010,
                    categoria = "Comédia",
                    capa = "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4"
                ),

                Filme(
                    titulo = "Avatar",
                    ano = 2009,
                    categoria = "Ficção",
                    capa = "https://i.postimg.cc/7LQgchYy/avatar.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/avatar-1080p.mp4"
                ),

                Filme(
                    titulo = "17 Outra Vez",
                    ano = 2009,
                    categoria = "Comédia",
                    capa = "https://i.postimg.cc/FFwTzW46/17.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4"
                ),

                Filme(
                    titulo = "Garota Infernal",
                    ano = 2009,
                    categoria = "Terror",
                    capa = "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4"
                ),

                Filme(
                    titulo = "A Noiva Cadáver",
                    ano = 2005,
                    categoria = "Animação",
                    capa = "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4"
                ),

                Filme(
                    titulo = "Fogo Contra Fogo",
                    ano = 1995,
                    categoria = "Ação",
                    capa = "https://i.postimg.cc/P5PCgb6Q/images-(20).jpg",
                    video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Fogo%20Contra%20Fogo.mp4"
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
    }    private fun carregarSeries() {

        series.clear()
        doramas.clear()
        animes.clear()

        series.add(

            Serie(
                titulo = "Spaide Noir",
                categoria = "Ação",
                capa = "https://i.postimg.cc/R0dfWdj7/alright-people-whove-seen-spider-noir-should-i-watch-it-in-v0-73e3ew89v94h1-(1).jpg",

                temporadas = listOf(

                    Temporada(
                        numero = 1,

                        episodios = listOf(

                            Episodio(
                                numero = 1,
                                titulo = "Entre no meu Escritório",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%201%20-%20Entre%20no%20meu%20Escrit%C3%B3rio.mp4"
                            ),

                            Episodio(
                                numero = 2,
                                titulo = "Pisando em Ovos",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%202%20-%20Pisando%20em%20Ovos.mp4"
                            ),

                            Episodio(
                                numero = 3,
                                titulo = "Falsidade",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%203%20-%20Falsidade.mp4"
                            ),

                            Episodio(
                                numero = 4,
                                titulo = "Nunca Repita o Mesmo Erro",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%204%20-%20Nunca%20Repita%20o%20Mesmo%20Erro.mp4"
                            ),

                            Episodio(
                                numero = 5,
                                titulo = "Traição",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%205%20-%20Trai%C3%A7%C3%A3o.mp4"
                            ),

                            Episodio(
                                numero = 6,
                                titulo = "Pesadelo na Maca",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%206%20-%20Pesadelo%20na%20Maca.mp4"
                            ),

                            Episodio(
                                numero = 7,
                                titulo = "Herói de Ninguém",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%207%20-%20Her%C3%B3i%20de%20Ningu%C3%A9m.mp4"
                            ),

                            Episodio(
                                numero = 8,
                                titulo = "O Homem Mascarado",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%208%20-%20O%20Homem%20Mascarado.mp4"
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
                                numero = 1,
                                titulo = "Aang",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%201%20-%20Aang.mp4"
                            ),

                            Episodio(
                                numero = 2,
                                titulo = "Guerreiros",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%202%20-%20Guerreiros.mp4"
                            ),

                            Episodio(
                                numero = 3,
                                titulo = "Omashu",
                                video = "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%203%20-%20Omashu.mp4"
                            ),

                            Episodio(
                                numero = 4,
                                titulo = "No Escuro",
                                video = "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%204%20-%20No%20escuro.mp4"
                            ),

                            Episodio(
                                numero = 5,
                                titulo = "O Mundo Espiritual",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%205%20-%20O%20Mundo%20Espiritual.mp4"
                            ),

                            Episodio(
                                numero = 6,
                                titulo = "Máscaras",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%206%20-%20M%C3%A1scaras.mp4"
                            ),

                            Episodio(
                                numero = 7,
                                titulo = "O Norte",
                                video = "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%207%20-%20O%20Norte.mp4"
                            ),

                            Episodio(
                                numero = 8,
                                titulo = "Episódio 8",
                                video = "https://wolf-channel-cdn.b-cdn.net/Filmes%20/S%C3%A9ries%20/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%208%20-%20Epis%C3%B3dio%208.mp4"
                            )
                        )
                    )
                )
            )
        )
    }

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        historicoConteudo.add {
            mostrarListaCards(filmes)
        }

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum filme encontrado."

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.textSize = 20f

            vazio.gravity =
                Gravity.CENTER

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

        lista.forEachIndexed {
                indice, filme ->

            if (
                indice % quantidadePorLinha == 0
            ) {

                linha =
                    LinearLayout(this)

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

            val card =
                criarCard(filme)

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

    private fun criarCard(
        filme: Filme
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(false)

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            filme.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            ).apply {
                gravity = Gravity.TOP
            }
        )

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.gravity =
            Gravity.CENTER

        informacoes.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 14f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines = 2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        val ano =
            TextView(this)

        ano.text =
            "${filme.ano} • ${filme.categoria}"

        ano.setTextColor(
            Color.LTGRAY
        )

        ano.textSize = 12f

        ano.gravity =
            Gravity.CENTER

        informacoes.addView(
            ano,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda =
            View(this)

        borda.setBackground(
            criarBordaVermelha()
        )

        borda.visibility =
            View.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
                _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(false)
        }

        card.setOnClickListener {

            indiceCardAtual =
                cardsAtuais.indexOf(card)

            abrirVideo(filme)
        }

        return card
    }    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        historicoConteudo.add {
            mostrarListaSeries(series)
        }

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma série encontrada."

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.textSize = 20f

            vazio.gravity =
                Gravity.CENTER

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

        lista.forEachIndexed {
                indice, serie ->

            if (
                indice % quantidadePorLinha == 0
            ) {

                linha =
                    LinearLayout(this)

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

            val card =
                criarCardSerie(serie)

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

    private fun criarCardSerie(
        serie: Serie
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(false)

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            serie.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            ).apply {
                gravity = Gravity.TOP
            }
        )

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.gravity =
            Gravity.CENTER

        informacoes.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 14f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines = 2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        val quantidade =
            serie.temporadas.sumOf {
                it.episodios.size
            }

        val info =
            TextView(this)

        info.text =
            "${serie.categoria} • $quantidade episódios"

        info.setTextColor(
            Color.LTGRAY
        )

        info.textSize = 12f

        info.gravity =
            Gravity.CENTER

        informacoes.addView(
            info,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda =
            View(this)

        borda.setBackground(
            criarBordaVermelha()
        )

        borda.visibility =
            View.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
                _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        card.setOnClickListener {

            indiceCardAtual =
                cardsAtuais.indexOf(card)

            mostrarTemporadas(serie)
        }

        return card
    }

    private fun mostrarTemporadas(
        serie: Serie
    ) {

        historicoConteudo.add {
            mostrarListaSeries(series)
        }

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 24f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        val quantidadePorLinha = 5

        var linha: LinearLayout? = null

        serie.temporadas.forEachIndexed {
                indice, temporada ->

            if (
                indice % quantidadePorLinha == 0
            ) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(250)
                    )
                )
            }

            val card =
                criarCardTemporada(
                    serie,
                    temporada
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

    private fun criarCardTemporada(
        serie: Serie,
        temporada: Temporada
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(false)

        val texto =
            TextView(this)

        texto.text =
            "TEMPORADA ${temporada.numero}\n\n" +
            "${temporada.episodios.size} episódios"

        texto.setTextColor(
            Color.WHITE
        )

        texto.textSize = 20f

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

        val borda =
            View(this)

        borda.setBackground(
            criarBordaVermelha()
        )

        borda.visibility =
            View.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
                _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        card.setOnClickListener {

            indiceCardAtual =
                cardsAtuais.indexOf(card)

            mostrarEpisodios(
                serie,
                temporada
            )
        }

        return card
    }

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

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo} • " +
            "Temporada ${temporada.numero}"

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 22f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        val quantidadePorLinha = 5

        var linha: LinearLayout? = null

        temporada.episodios.forEachIndexed {
                indice, episodio ->

            if (
                indice % quantidadePorLinha == 0
            ) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(220)
                    )
                )
            }

            val card =
                criarCardEpisodio(
                    serie,
                    episodio
                )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(335),
                    dp(190)
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

    private fun criarCardEpisodio(
        serie: Serie,
        episodio: Episodio
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(false)

        val texto =
            TextView(this)

        texto.text =
            "EPISÓDIO ${episodio.numero}\n\n" +
            episodio.titulo

        texto.setTextColor(
            Color.WHITE
        )

        texto.textSize = 16f

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        texto.gravity =
            Gravity.CENTER

        texto.maxLines = 3

        texto.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            texto,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val borda =
            View(this)

        borda.setBackground(
            criarBordaVermelha()
        )

        borda.visibility =
            View.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
                _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        card.setOnClickListener {

            indiceCardAtual =
                cardsAtuais.indexOf(card)

            abrirVideo(
                episodio.titulo,
                episodio.video,
                serie.capa
            )
        }

        return card
    }

    private fun abrirVideo(
        filme: Filme
    ) {

        if (filme.video.isBlank()) {

            Toast.makeText(
                this,
                "Vídeo ainda não disponível",
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

    private fun abrirVideo(
        titulo: String,
        video: String,
        capa: String
    ) {

        if (video.isBlank()) {

            Toast.makeText(
                this,
                "Vídeo ainda não disponível",
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
    }    // ===============================
    // MENU LATERAL
    // ===============================

    private fun adicionarTituloMenu(
        texto: String
    ) {

        val titulo =
            TextView(this)

        titulo.text = texto

        titulo.setTextColor(
            Color.RED
        )

        titulo.textSize = 14f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            dp(15),
            0,
            dp(8),
            0
        )

        menuConteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
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

        item.setTextColor(
            Color.WHITE
        )

        item.textSize = 15f

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(8),
            0
        )

        item.isFocusable = true
        item.isFocusableInTouchMode = true
        item.isClickable = true

        item.background =
            criarFundoCard(false)

        item.setOnFocusChangeListener {
                _, foco ->

            item.background =
                criarFundoCard(foco)
        }

        item.setOnClickListener {
            acao()
        }

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(3)
            }
        )

        itensMenuFoco.add(item)
    }

    private fun abrirMenu() {

        if (menuAberto) {
            return
        }

        menuAberto = true

        itensMenuFoco.clear()

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setBackgroundColor(
            Color.argb(
                245,
                5,
                5,
                5
            )
        )

        menuLateral.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        val cabecalho =
            LinearLayout(this)

        cabecalho.orientation =
            LinearLayout.HORIZONTAL

        cabecalho.gravity =
            Gravity.CENTER_VERTICAL

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF MENU"

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 21f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        cabecalho.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        botaoFecharMenu =
            TextView(this)

        botaoFecharMenu.text =
            "✕"

        botaoFecharMenu.setTextColor(
            Color.WHITE
        )

        botaoFecharMenu.textSize = 26f

        botaoFecharMenu.gravity =
            Gravity.CENTER

        botaoFecharMenu.isFocusable = true
        botaoFecharMenu.isFocusableInTouchMode = true
        botaoFecharMenu.isClickable = true

        botaoFecharMenu.background =
            criarFundoCard(false)

        botaoFecharMenu.setOnFocusChangeListener {
                _, foco ->

            botaoFecharMenu.background =
                criarFundoCard(foco)
        }

        botaoFecharMenu.setOnClickListener {
            fecharMenu()
        }

        cabecalho.addView(
            botaoFecharMenu,
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        menuLateral.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable = false
        menuScroll.isFocusableInTouchMode = false

        menuConteudo =
            LinearLayout(this)

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

        raiz.addView(
            menuLateral,
            FrameLayout.LayoutParams(
                dp(390),
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START
            }
        )

        // ===============================
        // PRINCIPAIS
        // ===============================

        adicionarItemMenu(
            "▶  Continuar assistindo"
        ) {
            Toast.makeText(
                this,
                "Continue assistindo",
                Toast.LENGTH_SHORT
            ).show()
        }

        adicionarItemMenu(
            "★  Favoritos (${favoritos.size})"
        ) {
            mostrarFavoritos()
        }

        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {
            abrirPesquisa()
        }

        // ===============================
        // FILMES
        // ===============================

        adicionarTituloMenu(
            "FILMES"
        )

        adicionarItemMenu(
            "🎬  Todos os filmes (${filmes.size})"
        ) {
            fecharMenu()
            mostrarListaCards(filmes)
        }

        val categoriasFilmes =
            listOf(
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
                "   $categoria ($quantidade)"
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

        // ===============================
        // SÉRIES
        // ===============================

        adicionarTituloMenu(
            "SÉRIES"
        )

        adicionarItemMenu(
            "📺  Todas as séries (${series.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(series)
        }

        val categoriasSeries =
            listOf(
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
                "   $categoria ($quantidade)"
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

        // ===============================
        // DORAMAS
        // ===============================

        adicionarTituloMenu(
            "DORAMAS"
        )

        adicionarItemMenu(
            "🎭  Todos os doramas (${doramas.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(doramas)
        }

        val categoriasDoramas =
            listOf(
                "Ação",
                "Romance",
                "Drama",
                "Comédia"
            )

        categoriasDoramas.forEach { categoria ->

            val quantidade =
                doramas.count {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }

            adicionarItemMenu(
                "   $categoria ($quantidade)"
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

        // ===============================
        // ANIME
        // ===============================

        adicionarTituloMenu(
            "ANIME"
        )

        adicionarItemMenu(
            "⚡  Todos os animes (${animes.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(animes)
        }

        val categoriasAnime =
            listOf(
                "Ação",
                "Aventura",
                "Comédia",
                "Fantasia"
            )

        categoriasAnime.forEach { categoria ->

            val quantidade =
                animes.count {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }

            adicionarItemMenu(
                "   $categoria ($quantidade)"
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

        botaoFecharMenu.requestFocus()
    }

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        raiz.removeView(
            menuLateral
        )

        itensMenuFoco.clear()

        if (
            cardsAtuais.isNotEmpty()
        ) {

            val indice =
                indiceCardAtual.coerceIn(
                    0,
                    cardsAtuais.lastIndex
                )

            cardsAtuais[indice]
                .requestFocus()
        } else {

            botaoMenu.requestFocus()
        }
    }

    private fun moverMenu(
        direcao: Int
    ) {

        if (!menuAberto) {
            return
        }

        if (
            itensMenuFoco.isEmpty()
        ) {
            return
        }

        var atual =
            itensMenuFoco.indexOfFirst {
                it.hasFocus()
            }

        if (atual < 0) {
            atual = 0
        }

        val novo =
            (atual + direcao)
                .coerceIn(
                    0,
                    itensMenuFoco.lastIndex
                )

        itensMenuFoco[novo]
            .requestFocus()

        ajustarScrollMenu(
            itensMenuFoco[novo]
        )
    }

    private fun ajustarScrollMenu(
        view: View
    ) {

        menuScroll.post {

            menuScroll.smoothScrollTo(
                0,
                view.top -
                    dp(30)
            )
        }
    }    // ===============================
    // FAVORITOS
    // ===============================

    private fun mostrarFavoritos() {

        fecharMenu()

        val lista =
            filmes.filter {
                favoritos.contains(it.titulo)
            }

        mostrarListaCards(
            lista,
            adicionarHistorico = true
        )
    }

    // ===============================
    // PESQUISA
    // ===============================

    private fun abrirPesquisa() {

        fecharMenu()

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome do filme..."

        campo.setHintTextColor(
            Color.LTGRAY
        )

        campo.setTextColor(
            Color.WHITE
        )

        campo.textSize = 18f

        campo.singleLine = true

        campo.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        campo.background =
            criarFundoCard(false)

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                bottomMargin = dp(15)
            }
        )

        campo.requestFocus()

        campo.setOnEditorActionListener {
                _, _, _ ->

            true
        }

        campo.addTextChangedListener(
            object :
                android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val busca =
                        s?.toString()
                            ?.trim()
                            ?.lowercase()
                            ?: ""

                    val resultado =
                        if (busca.isEmpty()) {
                            filmes
                        } else {
                            filmes.filter {
                                it.titulo
                                    .lowercase()
                                    .contains(busca)
                            }
                        }

                    mostrarResultadoPesquisa(
                        resultado
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )
    }

    private fun mostrarResultadoPesquisa(
        lista: List<Filme>
    ) {

        val antigaBusca =
            conteudo.getChildAt(0)

        conteudo.removeViews(
            1,
            conteudo.childCount - 1
        )

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum resultado encontrado."

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.textSize = 18f

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(120)
                )
            )

            return
        }

        val quantidadePorLinha = 5

        var linha: LinearLayout? = null

        lista.forEachIndexed {
                indice, filme ->

            if (
                indice % quantidadePorLinha == 0
            ) {

                linha =
                    LinearLayout(this)

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

            val card =
                criarCard(filme)

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
        } else {
            antigaBusca.requestFocus()
        }
    }

    // ===============================
    // MOVIMENTAÇÃO DOS CARDS
    // ===============================

    private fun moverCard(
        passo: Int
    ) {

        if (cardsAtuais.isEmpty()) {
            return
        }

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

        cardsAtuais[novo]
            .requestFocus()

        cardsAtuais[novo]
            .post {
                cardsAtuais[novo]
                    .parent
                    ?.let {
                        conteudo.smoothScrollTo(
                            0,
                            cardsAtuais[novo]
                                .top
                        )
                    }
            }
    }

    // ===============================
    // CONTROLE DO D-PAD
    // ===============================

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {
            return super.dispatchKeyEvent(
                event
            )
        }

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
                        if (
                            botaoFecharMenu.hasFocus()
                        ) {
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

        when (event.keyCode) {

            KeyEvent.KEYCODE_DPAD_UP -> {

                if (
                    botaoMenu.hasFocus()
                ) {
                    return true
                }

                val atual =
                    cardsAtuais
                        .indexOfFirst {
                            it.hasFocus()
                        }

                if (atual == 0) {

                    botaoMenu
                        .requestFocus()

                    return true
                }

                moverCard(-5)

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    if (
                        cardsAtuais.isNotEmpty()
                    ) {
                        cardsAtuais[0]
                            .requestFocus()
                    }

                    return true
                }

                moverCard(5)

                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (
                    botaoMenu.hasFocus()
                ) {
                    return true
                }

                moverCard(-1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (
                    botaoMenu.hasFocus()
                ) {
                    return true
                }

                moverCard(1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    abrirMenu()

                    return true
                }

                val foco =
                    cardsAtuais
                        .firstOrNull {
                            it.hasFocus()
                        }

                foco?.performClick()

                return true
            }

            KeyEvent.KEYCODE_MENU -> {

                abrirMenu()

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                if (
                    historicoConteudo
                        .isNotEmpty()
                ) {

                    val voltar =
                        historicoConteudo
                            .removeLast()

                    voltar()

                    return true
                }

                finish()

                return true
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }
}
