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
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

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

        val fundo =
            ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        fundo.alpha = 0.55f

        thread {

            try {

                val conexao =
                    URL(
                        "https://i.postimg.cc/Ghk8PP7w/wolf.png"
                    ).openConnection()
                        as HttpURLConnection

                conexao.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        conexao.inputStream
                    )

                conexao.disconnect()

                runOnUiThread {
                    fundo.setImageBitmap(bitmap)
                }

            } catch (_: Exception) {
            }
        }

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

        botaoMenu.textSize =
            20f

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.setTypeface(
            null,
            Typeface.BOLD
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        botaoMenu.isFocusable = true
        botaoMenu.isFocusableInTouchMode = true
        botaoMenu.isClickable = true

        botaoMenu.background =
            criarFundoCard(false)

        botaoMenu.setOnFocusChangeListener { _, foco ->

            if (foco) {

                botaoMenu.background =
                    criarFundoCard(true)

                botaoMenu.foreground =
                    criarBordaVermelha()

            } else {

                botaoMenu.background =
                    criarFundoCard(false)

                botaoMenu.foreground = null
            }
        }

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        camada.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(235),
                dp(58)
            )
        )

        val scroll =
            ScrollView(this)

        scroll.isFocusable = false
        scroll.isFocusableInTouchMode = false

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        scroll.addView(conteudo)

        camada.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun dp(valor: Int): Int {
        return (
            valor *
            resources.displayMetrics.density
        ).toInt()
    }    private fun carregarFilmes() {

        filmes.clear()

        filmes.add(
            Filme(
                "Como Mágica",
                2026,
                "Comédia",
                "https://i.postimg.cc/9z5YzWLn/D-NQ-NP-674318-MLB111141123921-052026-O.webp",
                "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Como%20M%C3%A1gica.mp4"
            )
        )

        filmes.add(
            Filme(
                "Todo Mundo em Pânico 4",
                2026,
                "Comédia",
                "https://i.postimg.cc/sgfwW2VH/dfdb52dae07d0b0950bb9dfc98ab09c08e44a3f704815cc5a5a6af72d156f913.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Todo%20Mundo%20em%20P%C3%A2nico.mp4"
            )
        )

        filmes.add(
            Filme(
                "Pânico 7",
                2026,
                "Terror",
                "https://i.postimg.cc/FzcqGbHV/images-(3).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/P%C3%A2nico%207.mp4"
            )
        )

        filmes.add(
            Filme(
                "Pinóquio",
                2026,
                "Animação",
                "https://i.postimg.cc/ryfYVWCk/IMG-20261002-044814.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Pin%C3%B3quio.mp4"
            )
        )

        filmes.add(
            Filme(
                "Moana",
                2026,
                "Animação",
                "https://i.postimg.cc/pdj7VwhR/moana.jpg",
                "https://vz-c091a331-1f1.b-cdn.net/199a8bfd-7a6e-4e1a-9cbe-d5ae4881089b/playlist.m3u8"
            )
        )

        filmes.add(
            Filme(
                "Como Treinar o Seu Dragão",
                2026,
                "Aventura",
                "https://i.postimg.cc/664hkrZ6/treinar.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Como%20Treinar%20o%20Seu%20Drag%C3%A3o.mp4"
            )
        )

        filmes.add(
            Filme(
                "Quarteto Fantástico: Primeiro Passo",
                2026,
                "Ação",
                "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Quarteto%20Fant%C3%A1stico%20Primeiros%20Passos.mp4"
            )
        )

        filmes.add(
            Filme(
                "Homem-Aranha: Um Novo Dia",
                2026,
                "Ação",
                "https://i.postimg.cc/QCctbsqF/aranha.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filme%20hospedagem%20/Homem-Aranha%20Um%20Novo%20Dia.mp4"
            )
        )

        filmes.add(
            Filme(
                "Conexão Perigosa",
                2026,
                "Ação",
                "https://i.postimg.cc/JhyRxMrH/conexao.jpg",
                "https://vz-c091a331-1f1.b-cdn.net/78e46f67-4aec-4ba3-b4c4-7072cd6d921b/playlist.m3u8"
            )
        )

        filmes.add(
            Filme(
                "A Odisseia",
                2026,
                "Aventura",
                "https://i.postimg.cc/K8WjhML7/odisseia.jpg",
                "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/BALA-CHANNEL/A%20Odisseia.mp4"
            )
        )

        filmes.add(
            Filme(
                "Resident Evil",
                2026,
                "Terror",
                "https://i.postimg.cc/3J7DtmC4/evil.jpg",
                "https://vz-c091a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8"
            )
        )

        filmes.add(
            Filme(
                "Vingança",
                2026,
                "Ação",
                "https://i.postimg.cc/26M7q6P7/vinganca.jpg",
                "https://vz-c091a331-1f1.b-cdn.net/10f5522b-3890-48cd-bfb7-89b2806e8269/playlist.m3u8"
            )
        )

        filmes.add(
            Filme(
                "A Revolta",
                2026,
                "Ação",
                "https://i.postimg.cc/7YyLx45D/revolta.jpg",
                "https://vz-c091a331-1f1.b-cdn.net/ae8d06f6-e3da-4705-9a83-313fae7114f9/playlist.m3u8"
            )
        )

        filmes.add(
            Filme(
                "Thunderbolts",
                2026,
                "Ação",
                "https://i.postimg.cc/FzbPQdZJ/D-NQ-NP-848607-CBT107833899597-022026-O.webp",
                "https://wolf-channel-cdn.b-cdn.net/Bala%20zip/Thunderbolts.mp4"
            )
        )

        filmes.add(
            Filme(
                "Céu em Fúria",
                2026,
                "Ação",
                "https://i.postimg.cc/PJMnXB7d/ceu-em-furia.jpg",
                ""
            )
        )

        filmes.add(
            Filme(
                "Super Mario Galaxy: O Filme",
                2026,
                "Animação",
                "https://i.postimg.cc/W3Y1mvHd/images-(2).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Super%20Mario%20Galaxy%20O%20Filme.mp4"
            )
        )

        // NOVOS FILMES

        filmes.add(
            Filme(
                "Mortal Kombat 2",
                2025,
                "Ação",
                "https://i.postimg.cc/ZRxyK7Fh/images-(13).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Mortal%20Kombat%202.mp4"
            )
        )

        filmes.add(
            Filme(
                "Planeta dos Macacos: O Reinado",
                2024,
                "Ação",
                "https://i.postimg.cc/fL1HwP8X/MV5BYTkw-Mm-Iy-NWQt-NWEy-ZS00M2M3LTgx-ZGEt-MDlm-MGIz-ZGM4NWNh-Xk-Ey-Xk-Fqc-Gc-V1-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Planeta%20dos%20Macacos%20O%20Reinado.mp4"
            )
        )

        filmes.add(
            Filme(
                "Divertida Mente 2",
                2024,
                "Animação",
                "https://i.postimg.cc/0jgfgwN6/aa61ae8fb015160d802c4d5cb4fe6858058ea76485c3498ed9ff431eee4fc83f-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Divertida%20Mente%202.mp4"
            )
        )

        filmes.add(
            Filme(
                "Kung Fu Panda 4",
                2024,
                "Animação",
                "https://i.postimg.cc/XYyYqBNV/kung-fu-panda-4-cartaz-1zso1c-717x1200-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Kung%20Fu%20Panda%204.mp4"
            )
        )

        filmes.add(
            Filme(
                "Ghostbusters: Apocalipse de Gelo",
                2024,
                "Ação",
                "https://i.postimg.cc/1zh5qR4V/2523057-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Ghostbusters%20Apocalipse%20de%20Gelo.mp4"
            )
        )

        filmes.add(
            Filme(
                "Mufasa: O Rei Leão",
                2024,
                "Aventura",
                "https://i.postimg.cc/mgw3xmpz/917q-7O0TJL.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Mufasa%20O%20Rei%20Le%C3%A3o.mp4"
            )
        )

        filmes.add(
            Filme(
                "Coringa: Delírio a Dois",
                2024,
                "Drama",
                "https://i.postimg.cc/LXN621K3/MV5BZTU0ZGI3Yz-Mt-ZTUw-MC00MGJj-LWFk-NDIt-MDUz-NTUx-Zjg5N2Y4Xk-Ey-Xk-Fqc-Gc-V1.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Coringa%20Del%C3%ADrio%20a%20Dois.mp4"
            )
        )

        filmes.add(
            Filme(
                "Deadpool & Wolverine",
                2024,
                "Ação",
                "https://i.postimg.cc/2SZ6hyv9/IMG-20261002-100134.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Deadpool%20%26amp%3B%20Wolverine.mp4"
            )
        )

        filmes.add(
            Filme(
                "Bad Boys: Até o Fim",
                2024,
                "Ação",
                "https://i.postimg.cc/sDv6PQmF/images-(4).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Bad%20Boys%20At%C3%A9%20o%20Fim.mp4"
            )
        )

        filmes.add(
            Filme(
                "Furiosa: Uma Saga Mad Max",
                2024,
                "Ação",
                "https://i.postimg.cc/FzZY7Hfn/71K2Mcc-CQ2L-AC-UF894-1000-QL80.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Furiosa%20Uma%20Saga%20Mad%20Max.mp4"
            )
        )

        filmes.add(
            Filme(
                "Meu Malvado Favorito 4",
                2024,
                "Animação",
                "https://i.postimg.cc/5t0bhHHr/images-(6).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Meu%20Malvado%20Favorito%204.mp4"
            )
        )

        filmes.add(
            Filme(
                "Gran Turismo – De Jogador a Corredor",
                2023,
                "Ação",
                "https://i.postimg.cc/YSVWsL2p/images-(7).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Gran%20Turismo%20%E2%80%93%20De%20Jogador%20a%20Corredor.mp4"
            )
        )

        filmes.add(
            Filme(
                "Besouro Azul",
                2023,
                "Ação",
                "https://i.postimg.cc/fTjYRbCr/images-(9).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Besouro%20Azul.mp4"
            )
        )

        filmes.add(
            Filme(
                "A Pequena Sereia",
                2023,
                "Aventura",
                "https://i.postimg.cc/vmDPnnSb/images-(10).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/A%20Pequena%20Sereia.mp4"
            )
        )

        filmes.add(
            Filme(
                "A Fera",
                2022,
                "Ação",
                "https://i.postimg.cc/GhQzSKW1/images-(11).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/A%20Fera.mp4"
            )
        )

        filmes.add(
            Filme(
                "O Projeto Adam",
                2022,
                "Ficção",
                "https://i.postimg.cc/rp8T2fm3/images-(15).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/O%20Projeto%20Adam.mp4"
            )
        )

        filmes.add(
            Filme(
                "Uncharted: Fora do Mapa",
                2022,
                "Aventura",
                "https://i.postimg.cc/wvNYhZbd/Uncharted-Official-Poster-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Uncharted%20Fora%20do%20Mapa.mp4"
            )
        )

        filmes.add(
            Filme(
                "Morbius",
                2022,
                "Ação",
                "https://i.postimg.cc/YCTX10c0/Morbius-cartaz-(1).jpg",
                "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Filmes%20/Morbius.mp4"
            )
        )

        filmes.add(
            Filme(
                "Jumanji: Bem-Vindo à Selva",
                2017,
                "Aventura",
                "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
                "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
            )
        )

        filmes.add(
            Filme(
                "Kingsman: Agente Secreto",
                2014,
                "Ação",
                "https://i.postimg.cc/0QDbfyzB/kings.jpg",
                "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4"
            )
        )

        filmes.add(
            Filme(
                "Deu a Louca nos Bichos",
                2010,
                "Comédia",
                "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
                "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4"
            )
        )

        filmes.add(
            Filme(
                "Avatar",
                2009,
                "Ficção",
                "https://i.postimg.cc/7LQgchYy/avatar.jpg",
                "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/avatar-1080p.mp4"
            )
        )

        filmes.add(
            Filme(
                "17 Outra Vez",
                2009,
                "Comédia",
                "https://i.postimg.cc/FFwTzW46/17.jpg",
                "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4"
            )
        )

        filmes.add(
            Filme(
                "Garota Infernal",
                2009,
                "Terror",
                "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
                "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4"
            )
        )

        filmes.add(
            Filme(
                "A Noiva Cadáver",
                2005,
                "Animação",
                "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
                "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4"
            )
        )

        // ORGANIZA:
        // filmes de ano mais novo primeiro
        // e, dentro do mesmo ano, por título.

        filmes.sortWith(
            compareByDescending<Filme> { it.ano }
                .thenBy { it.titulo }
        )
    }    private fun carregarSeries() {

        series.clear()
        doramas.clear()
        animes.clear()

        // =========================
        // SPAIDE NOIR
        // =========================

        series.add(
            Serie(
                "Spaide Noir",
                "Ação",
                "https://i.postimg.cc/R0dfWdj7/alright-people-whove-seen-spider-noir-should-i-watch-it-in-v0-73e3ew89v94h1-(1).jpg",
                listOf(
                    Temporada(
                        1,
                        listOf(

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

        // =========================
        // AVATAR: A LENDA DE AANG
        // =========================

        series.add(
            Serie(
                "Avatar: A Lenda de Aang",
                "Aventura",
                "https://i.postimg.cc/BvWjsB3G/Avatar-The-Last-Airbender-2024-series-poster-(1).jpg",
                listOf(
                    Temporada(
                        1,
                        listOf(

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

        // =========================
        // DORAMAS
        // =========================

        // Nenhum dorama cadastrado ainda.

        // =========================
        // ANIMES
        // =========================

        // Nenhum anime cadastrado ainda.
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
            Color.argb(
                if (foco) 230 else 190,
                20,
                20,
                20
            )
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        return fundo
    }

    // ===============================
    // BORDA VERMELHA
    // ===============================

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

    // ===============================
    // CARREGAR IMAGEM
    // ===============================

    private fun carregarImagem(
        url: String,
        imagem: ImageView
    ) {

        if (url.isEmpty()) {
            return
        }

        val cache =
            cacheCapas[url]

        if (cache != null) {

            imagem.setImageBitmap(
                cache
            )

            return
        }

        thread {

            try {

                val conexao =
                    URL(url)
                        .openConnection()
                        as HttpURLConnection

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

            } catch (_: Exception) {
            }
        }
    }    // ===============================
    // MOSTRAR FILMES
    // ===============================

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        historicoConteudo.clear()

        cardsAtuais.clear()

        indiceCardAtual = 0

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum filme encontrado"

            vazio.textSize =
                22f

            vazio.setTextColor(
                Color.WHITE
            )

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

        var linha:
            LinearLayout? = null

        lista.forEachIndexed { indice, filme ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER_VERTICAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(265)
                    )
                )
            }

            val card =
                criarCard(filme)

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(245),
                    dp(245)
                ).apply {

                    leftMargin =
                        dp(5)

                    rightMargin =
                        dp(5)
                }
            )

            cardsAtuais.add(card)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0].requestFocus()

            indiceCardAtual = 0

            rolarParaCard(
                cardsAtuais[0]
            )
        }
    }

    // ===============================
    // CRIAR CARD DE FILME
    // ===============================

    private fun criarCard(
        filme: Filme
    ): FrameLayout {

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
            ImageView.ScaleType.CENTER_CROP

        carregarImagem(
            filme.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val sombra =
            TextView(this)

        sombra.setBackgroundColor(
            Color.argb(
                210,
                0,
                0,
                0
            )
        )

        sombra.gravity =
            Gravity.CENTER_VERTICAL

        sombra.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        sombra.text =
            filme.titulo

        sombra.textSize =
            15f

        sombra.setTextColor(
            Color.WHITE
        )

        sombra.setTypeface(
            null,
            Typeface.BOLD
        )

        val params =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55),
                Gravity.BOTTOM
            )

        card.addView(
            sombra,
            params
        )

        card.setOnFocusChangeListener { _, foco ->

            if (foco) {

                card.background =
                    criarFundoCard(true)

                card.foreground =
                    criarBordaVermelha()

            } else {

                card.background =
                    criarFundoCard(false)

                card.foreground =
                    null
            }
        }

        card.setOnClickListener {

            abrirVideo(
                filme.titulo,
                filme.video,
                filme.capa
            )
        }

        return card
    }

    // ===============================
    // ABRIR VÍDEO
    // ===============================

    private fun abrirVideo(
        titulo: String,
        video: String,
        capa: String
    ) {

        if (video.isEmpty()) {

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
    }

    // ===============================
    // MOSTRAR SÉRIES
    // ===============================

    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        historicoConteudo.clear()

        cardsAtuais.clear()

        indiceCardAtual = 0

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma série encontrada"

            vazio.textSize =
                22f

            vazio.setTextColor(
                Color.WHITE
            )

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

        var linha:
            LinearLayout? = null

        lista.forEachIndexed { indice, serie ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER_VERTICAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(265)
                    )
                )
            }

            val card =
                criarCardSerie(
                    serie
                )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(245),
                    dp(245)
                ).apply {

                    leftMargin =
                        dp(5)

                    rightMargin =
                        dp(5)
                }
            )

            cardsAtuais.add(card)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0].requestFocus()

            indiceCardAtual = 0

            rolarParaCard(
                cardsAtuais[0]
            )
        }
    }

    // ===============================
    // CARD DE SÉRIE
    // ===============================

    private fun criarCardSerie(
        serie: Serie
    ): FrameLayout {

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
            ImageView.ScaleType.CENTER_CROP

        carregarImagem(
            serie.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize =
            15f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(5)
        )

        titulo.setBackgroundColor(
            Color.argb(
                210,
                0,
                0,
                0
            )
        )

        card.addView(
            titulo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55),
                Gravity.BOTTOM
            )
        )

        card.setOnFocusChangeListener { _, foco ->

            if (foco) {

                card.background =
                    criarFundoCard(true)

                card.foreground =
                    criarBordaVermelha()

            } else {

                card.background =
                    criarFundoCard(false)

                card.foreground =
                    null
            }
        }

        card.setOnClickListener {

            mostrarTemporadas(
                serie
            )
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
            mostrarListaSeries(
                series
            )
        }

        cardsAtuais.clear()

        indiceCardAtual = 0

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo}  •  TEMPORADAS"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        var linha:
            LinearLayout? = null

        serie.temporadas.forEachIndexed { indice, temporada ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER_VERTICAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(170)
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
                    dp(140),
                    dp(140)
                ).apply {

                    leftMargin =
                        dp(5)

                    rightMargin =
                        dp(5)
                }
            )

            cardsAtuais.add(card)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0].requestFocus()

            indiceCardAtual = 0

            rolarParaCard(
                cardsAtuais[0]
            )
        }
    }

    // ===============================
    // CARD DE TEMPORADA
    // ===============================

    private fun criarCardTemporada(
        serie: Serie,
        temporada: Temporada
    ): TextView {

        val card =
            TextView(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.gravity =
            Gravity.CENTER

        card.text =
            "TEMPORADA ${temporada.numero}\n\n${temporada.episodios.size} episódios"

        card.textSize =
            18f

        card.setTextColor(
            Color.WHITE
        )

        card.setTypeface(
            null,
            Typeface.BOLD
        )

        card.background =
            criarFundoCard(false)

        card.setOnFocusChangeListener { _, foco ->

            if (foco) {

                card.background =
                    criarFundoCard(true)

                card.foreground =
                    criarBordaVermelha()

            } else {

                card.background =
                    criarFundoCard(false)

                card.foreground =
                    null
            }
        }

        card.setOnClickListener {

            mostrarEpisodios(
                serie,
                temporada
            )
        }

        return card
    }

    // ===============================
    // EPISÓDIOS
    // ===============================

    private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {

        historicoConteudo.add {
            mostrarTemporadas(
                serie
            )
        }

        cardsAtuais.clear()

        indiceCardAtual = 0

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo}  •  TEMPORADA ${temporada.numero}"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        var linha:
            LinearLayout? = null

        temporada.episodios.forEachIndexed { indice, episodio ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.CENTER_VERTICAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(165)
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
                    dp(135),
                    dp(135)
                ).apply {

                    leftMargin =
                        dp(5)

                    rightMargin =
                        dp(5)
                }
            )

            cardsAtuais.add(card)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0].requestFocus()

            indiceCardAtual = 0

            rolarParaCard(
                cardsAtuais[0]
            )
        }
    }

    // ===============================
    // CARD DE EPISÓDIO
    // ===============================

    private fun criarCardEpisodio(
        serie: Serie,
        episodio: Episodio
    ): TextView {

        val card =
            TextView(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.gravity =
            Gravity.CENTER

        card.text =
            "EP ${episodio.numero}\n\n${episodio.titulo}"

        card.textSize =
            15f

        card.setTextColor(
            Color.WHITE
        )

        card.setTypeface(
            null,
            Typeface.BOLD
        )

        card.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        card.background =
            criarFundoCard(false)

        card.setOnFocusChangeListener { _, foco ->

            if (foco) {

                card.background =
                    criarFundoCard(true)

                card.foreground =
                    criarBordaVermelha()

            } else {

                card.background =
                    criarFundoCard(false)

                card.foreground =
                    null
            }
        }

        card.setOnClickListener {

            abrirVideo(
                "${serie.titulo} - EP ${episodio.numero} - ${episodio.titulo}",
                episodio.video,
                serie.capa
            )
        }

        return card
    }    // ===============================
    // ABRIR MENU
    // ===============================

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
                8,
                8,
                8
            )
        )

        menuLateral.setPadding(
            dp(15),
            dp(10),
            dp(10),
            dp(10)
        )

        val parametros =
            FrameLayout.LayoutParams(
                dp(340),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        parametros.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            parametros
        )

        val cabecalho =
            LinearLayout(this)

        cabecalho.orientation =
            LinearLayout.HORIZONTAL

        cabecalho.gravity =
            Gravity.CENTER_VERTICAL

        menuLateral.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF MENU"

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

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
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        botaoFecharMenu =
            TextView(this)

        botaoFecharMenu.text =
            "✕"

        botaoFecharMenu.textSize =
            28f

        botaoFecharMenu.setTextColor(
            Color.WHITE
        )

        botaoFecharMenu.gravity =
            Gravity.CENTER

        botaoFecharMenu.isFocusable =
            true

        botaoFecharMenu.isFocusableInTouchMode =
            true

        botaoFecharMenu.isClickable =
            true

        botaoFecharMenu.background =
            criarFundoCard(false)

        botaoFecharMenu.setOnFocusChangeListener { _, foco ->

            if (foco) {

                botaoFecharMenu.background =
                    criarFundoCard(true)

                botaoFecharMenu.foreground =
                    criarBordaVermelha()

            } else {

                botaoFecharMenu.background =
                    criarFundoCard(false)

                botaoFecharMenu.foreground =
                    null
            }
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

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable =
            false

        menuScroll.isFocusableInTouchMode =
            false

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

        menuScroll.addView(
            menuConteudo
        )

        menuLateral.addView(
            menuScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ===============================
        // MENU PRINCIPAL
        // ===============================

        adicionarItemMenu(
            "▶  Continuar assistindo"
        ) {

            Toast.makeText(
                this,
                "Nenhum conteúdo para continuar",
                Toast.LENGTH_SHORT
            ).show()
        }

        adicionarItemMenu(
            "★  Favoritos"
        ) {

            mostrarFavoritos()
        }

        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {

            abrirPesquisa()
        }

        adicionarSeparador(
            "FILMES"
        )

        adicionarItemMenu(
            "🎬  Todos os filmes (${filmes.size})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes
            )
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
                "$categoria ($quantidade)"
            ) {

                fecharMenu()

                val filtrados =
                    filmes.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaCards(
                    filtrados
                )
            }
        }

        // ===============================
        // SÉRIES
        // ===============================

        adicionarSeparador(
            "SÉRIES"
        )

        adicionarItemMenu(
            "📺  Todas as séries (${series.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series
            )
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
                "$categoria ($quantidade)"
            ) {

                fecharMenu()

                val filtradas =
                    series.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaSeries(
                    filtradas
                )
            }
        }

        // ===============================
        // DORAMAS
        // ===============================

        adicionarSeparador(
            "DORAMAS"
        )

        adicionarItemMenu(
            "🎭  Todos os doramas (${doramas.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(
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

        categoriasDoramas.forEach { categoria ->

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

                val filtrados =
                    doramas.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaSeries(
                    filtrados
                )
            }
        }

        // ===============================
        // ANIME
        // ===============================

        adicionarSeparador(
            "ANIME"
        )

        adicionarItemMenu(
            "⚡  Todos os animes (${animes.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes
            )
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
                "$categoria ($quantidade)"
            ) {

                fecharMenu()

                val filtrados =
                    animes.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaSeries(
                    filtrados
                )
            }
        }

        if (itensMenuFoco.isNotEmpty()) {

            itensMenuFoco[0].requestFocus()

            ajustarScrollMenu(
                itensMenuFoco[0]
            )

        } else {

            botaoFecharMenu.requestFocus()
        }
    }

    // ===============================
    // SEPARADOR DO MENU
    // ===============================

    private fun adicionarSeparador(
        texto: String
    ) {

        val separador =
            TextView(this)

        separador.text =
            texto

        separador.textSize =
            14f

        separador.setTextColor(
            Color.LTGRAY
        )

        separador.setTypeface(
            null,
            Typeface.BOLD
        )

        separador.gravity =
            Gravity.CENTER_VERTICAL

        separador.setPadding(
            dp(12),
            dp(12),
            dp(5),
            dp(5)
        )

        menuConteudo.addView(
            separador,
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
            dp(15),
            0,
            dp(10),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true

        item.isClickable =
            true

        item.background =
            criarFundoCard(false)

        item.setOnFocusChangeListener { _, foco ->

            if (foco) {

                item.background =
                    criarFundoCard(true)

                item.foreground =
                    criarBordaVermelha()

            } else {

                item.background =
                    criarFundoCard(false)

                item.foreground =
                    null
            }
        }

        item.setOnClickListener {

            acao()
        }

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {

                bottomMargin =
                    dp(3)
            }
        )

        itensMenuFoco.add(
            item
        )
    }

    // ===============================
    // FECHAR MENU
    // ===============================

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        itensMenuFoco.clear()

        if (::menuLateral.isInitialized) {

            raiz.removeView(
                menuLateral
            )
        }

        botaoMenu.requestFocus()
    }

    // ===============================
    // MOVER MENU
    // ===============================

    private fun moverMenu(
        deslocamento: Int
    ) {

        if (itensMenuFoco.isEmpty()) {
            return
        }

        val atual =
            itensMenuFoco.indexOfFirst {
                it.hasFocus()
            }

        val indiceAtual =
            if (atual >= 0) {
                atual
            } else {
                0
            }

        var novoIndice =
            indiceAtual + deslocamento

        if (novoIndice < 0) {
            novoIndice = 0
        }

        if (novoIndice >= itensMenuFoco.size) {
            novoIndice =
                itensMenuFoco.size - 1
        }

        itensMenuFoco[
            novoIndice
        ].requestFocus()

        ajustarScrollMenu(
            itensMenuFoco[
                novoIndice
            ]
        )
    }

    // ===============================
    // AJUSTAR SCROLL DO MENU
    // ===============================

    private fun ajustarScrollMenu(
        view: View
    ) {

        if (!::menuScroll.isInitialized) {
            return
        }

        menuScroll.post {

            menuScroll.smoothScrollTo(
                0,
                view.top - dp(30)
            )
        }
    }    // ===============================
    // MOVER CARD
    // ===============================

    private fun moverCard(
        deslocamento: Int
    ) {

        if (cardsAtuais.isEmpty()) {
            return
        }

        var novoIndice =
            indiceCardAtual + deslocamento

        if (novoIndice < 0) {
            novoIndice = 0
        }

        if (novoIndice >= cardsAtuais.size) {
            novoIndice =
                cardsAtuais.size - 1
        }

        indiceCardAtual =
            novoIndice

        cardsAtuais[
            indiceCardAtual
        ].requestFocus()

        rolarParaCard(
            cardsAtuais[
                indiceCardAtual
            ]
        )
    }

    // ===============================
    // ROLAR ATÉ O CARD
    // ===============================

    private fun rolarParaCard(
        view: View
    ) {

        view.post {

            var pai =
                view.parent

            while (
                pai != null &&
                pai !is ScrollView
            ) {

                pai =
                    pai.parent
            }

            if (pai is ScrollView) {

                pai.smoothScrollTo(
                    0,
                    view.top - dp(20)
                )
            }
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

        mostrarListaCards(
            lista
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

        val titulo =
            TextView(this)

        titulo.text =
            "PESQUISAR"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

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

        campo.textSize =
            18f

        campo.setSingleLine(
            true
        )

        campo.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        campo.background =
            criarFundoCard(false)

        campo.isFocusable =
            true

        campo.isFocusableInTouchMode =
            true

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {

                leftMargin =
                    dp(10)

                rightMargin =
                    dp(10)

                bottomMargin =
                    dp(15)
            }
        )

        val botao =
            TextView(this)

        botao.text =
            "PESQUISAR"

        botao.textSize =
            18f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.setTypeface(
            null,
            Typeface.BOLD
        )

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.isClickable =
            true

        botao.background =
            criarFundoCard(false)

        botao.setOnFocusChangeListener { _, foco ->

            if (foco) {

                botao.background =
                    criarFundoCard(true)

                botao.foreground =
                    criarBordaVermelha()

            } else {

                botao.background =
                    criarFundoCard(false)

                botao.foreground =
                    null
            }
        }

        botao.setOnClickListener {

            pesquisar(
                campo.text.toString()
            )
        }

        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(
                dp(220),
                dp(55)
            ).apply {

                leftMargin =
                    dp(10)
            }
        )

        campo.setOnEditorActionListener { _, _, _ ->

            pesquisar(
                campo.text.toString()
            )

            true
        }

        campo.requestFocus()
    }

    // ===============================
    // EXECUTAR PESQUISA
    // ===============================

    private fun pesquisar(
        texto: String
    ) {

        val busca =
            texto.trim()

        if (busca.isEmpty()) {

            Toast.makeText(
                this,
                "Digite algo para pesquisar",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val resultados =
            filmes.filter {

                it.titulo.contains(
                    busca,
                    ignoreCase = true
                )
            }

        if (resultados.isEmpty()) {

            Toast.makeText(
                this,
                "Nenhum resultado encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        mostrarListaCards(
            resultados
        )
    }

    // ===============================
    // CONTROLE D-PAD
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

        // ===============================
        // MENU ABERTO
        // ===============================

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

                    // Não deixa o foco sair
                    // horizontalmente do menu.

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco =
                        currentFocus

                    if (
                        foco != null &&
                        foco.isClickable
                    ) {

                        foco.performClick()
                    }

                    return true
                }

                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_MENU -> {

                    fecharMenu()

                    return true
                }
            }

            return super.dispatchKeyEvent(
                event
            )
        }

        // ===============================
        // MENU FECHADO
        // ===============================

        when (event.keyCode) {

            KeyEvent.KEYCODE_MENU -> {

                abrirMenu()

                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {

                if (cardsAtuais.isEmpty()) {

                    botaoMenu.requestFocus()

                    return true
                }

                val atual =
                    cardsAtuais.indexOfFirst {
                        it.hasFocus()
                    }

                if (atual < 0) {

                    botaoMenu.requestFocus()

                    return true
                }

                // Primeira fileira:
                // sobe para o WOLF MENU.

                if (atual < 5) {

                    botaoMenu.requestFocus()

                    return true
                }

                moverCard(-5)

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                // Se o WOLF MENU estiver focado,
                // desce para o primeiro card.

                if (
                    botaoMenu.hasFocus()
                ) {

                    if (
                        cardsAtuais.isNotEmpty()
                    ) {

                        indiceCardAtual = 0

                        cardsAtuais[0]
                            .requestFocus()

                        rolarParaCard(
                            cardsAtuais[0]
                        )
                    }

                    return true
                }

                if (cardsAtuais.isEmpty()) {
                    return true
                }

                val atual =
                    cardsAtuais.indexOfFirst {
                        it.hasFocus()
                    }

                if (atual < 0) {
                    return true
                }

                // Desce exatamente uma fileira.

                if (
                    atual + 5 <
                    cardsAtuais.size
                ) {

                    moverCard(5)
                }

                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (botaoMenu.hasFocus()) {
                    return true
                }

                if (cardsAtuais.isEmpty()) {
                    return true
                }

                val atual =
                    cardsAtuais.indexOfFirst {
                        it.hasFocus()
                    }

                if (atual <= 0) {
                    return true
                }

                // Não deixa passar para a
                // coluna anterior quando já
                // está na primeira coluna.

                if (
                    atual % 5 == 0
                ) {

                    return true
                }

                moverCard(-1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (botaoMenu.hasFocus()) {
                    return true
                }

                if (cardsAtuais.isEmpty()) {
                    return true
                }

                val atual =
                    cardsAtuais.indexOfFirst {
                        it.hasFocus()
                    }

                if (atual < 0) {
                    return true
                }

                // Não deixa passar para uma
                // nova linha pela direita.

                if (
                    atual % 5 == 4 ||
                    atual + 1 >=
                    cardsAtuais.size
                ) {

                    return true
                }

                moverCard(1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                val foco =
                    currentFocus

                if (
                    foco != null &&
                    foco.isClickable
                ) {

                    foco.performClick()
                }

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                if (
                    historicoConteudo.isNotEmpty()
                ) {

                    val voltar =
                        historicoConteudo
                            .removeAt(
                                historicoConteudo.size - 1
                            )

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

    // ===============================
    // BOTÃO VOLTAR
    // ===============================

    @Deprecated(
        "Deprecated in Java"
    )
    override fun onBackPressed() {

        if (menuAberto) {

            fecharMenu()

            return
        }

        if (
            historicoConteudo.isNotEmpty()
        ) {

            val voltar =
                historicoConteudo
                    .removeAt(
                        historicoConteudo.size - 1
                    )

            voltar()

            return
        }

        super.onBackPressed()
    }
}
