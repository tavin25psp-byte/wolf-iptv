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
    }    private fun carregarSeries() {

        series.clear()

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
                                "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%205%20-%20O%20Mundo%20Espiritual.mp4"
                            ),

                            Episodio(
                                6,
                                "Máscaras",
                                "https://wolf-channel-cdn.b-cdn.net/S%C3%A9ries%20/S%C3%A9ries%20/Temporada%201%20Epis%C3%B3dio%206%20-%20M%C3%A1scaras.mp4"
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

        doramas.clear()
        animes.clear()
    }

    private fun dp(valor: Int): Int {
        return (
            valor *
                resources.displayMetrics.density
        ).toInt()
    }

    private fun criarFundoCard(
        foco: Boolean = false
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.rgb(18, 18, 18)
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        fundo.setStroke(
            dp(
                if (foco) 3 else 1
            ),
            if (foco)
                Color.RED
            else
                Color.DKGRAY
        )

        return fundo
    }

    private fun carregarImagem(
        imagem: ImageView,
        url: String
    ) {

        if (url.isBlank()) {
            return
        }

        val cache =
            cacheCapas[url]

        if (cache != null) {

            imagem.post {
                imagem.setImageBitmap(
                    cache
                )
            }

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
                    10000

                conexao.doInput =
                    true

                conexao.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        conexao.inputStream
                    )

                conexao.disconnect()

                if (bitmap != null) {

                    cacheCapas[url] =
                        bitmap

                    imagem.post {
                        imagem.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (
                e: Exception
            ) {

                e.printStackTrace()
            }
        }
    }    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        historicoConteudo.clear()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum filme encontrado."

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
                    dp(120)
                )
            )

            return
        }

        var linha: LinearLayout? =
            null

        lista.forEachIndexed {
                indice,
                filme ->

            if (
                indice % 5 == 0
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
                        -1,
                        dp(245)
                    )
                )
            }

            linha!!.addView(
                criarCard(filme),
                LinearLayout.LayoutParams(
                    0,
                    dp(225),
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

    private fun criarCard(
        filme: Filme
    ): View {

        val caixa =
            FrameLayout(this)

        caixa.isFocusable =
            true

        caixa.isFocusableInTouchMode =
            true

        caixa.background =
            criarFundoCard()

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        caixa.addView(
            imagem,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        carregarImagem(
            imagem,
            filme.capa
        )

        val ano =
            TextView(this)

        ano.text =
            filme.ano.toString()

        ano.textSize =
            13f

        ano.typeface =
            Typeface.DEFAULT_BOLD

        ano.setTextColor(
            Color.WHITE
        )

        ano.gravity =
            Gravity.CENTER

        val fundoAno =
            GradientDrawable()

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

        fundoAno.cornerRadius =
            dp(5).toFloat()

        ano.background =
            fundoAno

        ano.setPadding(
            dp(5),
            dp(2),
            dp(5),
            dp(2)
        )

        val paramsAno =
            FrameLayout.LayoutParams(
                dp(55),
                dp(30)
            )

        paramsAno.gravity =
            Gravity.TOP or
            Gravity.END

        paramsAno.setMargins(
            0,
            dp(7),
            dp(7),
            0
        )

        caixa.addView(
            ano,
            paramsAno
        )

        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.textSize =
            12f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setPadding(
            dp(5),
            dp(4),
            dp(5),
            dp(4)
        )

        titulo.setBackgroundColor(
            Color.argb(
                210,
                0,
                0,
                0
            )
        )

        val paramsTitulo =
            FrameLayout.LayoutParams(
                -1,
                dp(42)
            )

        paramsTitulo.gravity =
            Gravity.BOTTOM

        caixa.addView(
            titulo,
            paramsTitulo
        )

        caixa.setOnFocusChangeListener {
                view,
                temFoco ->

            view.background =
                criarFundoCard(
                    temFoco
                )
        }

        caixa.setOnClickListener {

            abrirVideo(
                filme.video,
                filme.titulo,
                filme.capa
            )
        }

        return caixa
    }

    private fun mostrarConteudoEspecial(
        listaOriginal: List<Serie>,
        categoria: String,
        tipo: String
    ) {

        val lista =
            if (
                categoria.equals(
                    "Todos",
                    ignoreCase = true
                )
            ) {
                listaOriginal
            } else {
                listaOriginal.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }
            }

        mostrarListaSeries(
            lista.sortedBy {
                it.titulo
            }
        )
    }

    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma série encontrada."

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
                    dp(120)
                )
            )

            return
        }

        var linha: LinearLayout? =
            null

        lista.forEachIndexed {
                indice,
                serie ->

            if (
                indice % 5 == 0
            ) {

                linha =
                    criarLinhaEspecial()

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(245)
                    )
                )
            }

            linha!!.addView(
                criarCardSerie(
                    serie
                ),
                LinearLayout.LayoutParams(
                    0,
                    dp(225),
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

    private fun criarLinhaEspecial():
        LinearLayout {

        val linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.CENTER

        return linha
    }

    private fun criarCardSerie(
        serie: Serie
    ): View {

        val caixa =
            FrameLayout(this)

        caixa.isFocusable =
            true

        caixa.isFocusableInTouchMode =
            true

        caixa.background =
            criarFundoCard()

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        caixa.addView(
            imagem,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        carregarImagem(
            imagem,
            serie.capa
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize =
            12f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setPadding(
            dp(5),
            dp(4),
            dp(5),
            dp(4)
        )

        titulo.setBackgroundColor(
            Color.argb(
                210,
                0,
                0,
                0
            )
        )

        val params =
            FrameLayout.LayoutParams(
                -1,
                dp(42)
            )

        params.gravity =
            Gravity.BOTTOM

        caixa.addView(
            titulo,
            params
        )

        caixa.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(
                    foco
                )
        }

        caixa.setOnClickListener {

            historicoConteudo.add {
                mostrarListaSeries(
                    listaSeriesAtual()
                )
            }

            mostrarTemporadas(
                serie
            )
        }

        return caixa
    }

    private fun listaSeriesAtual():
        List<Serie> {

        return when {
            series.isNotEmpty() &&
                series.any {
                    it.titulo ==
                        "Spaide Noir"
                } -> series

            doramas.isNotEmpty() ->
                doramas

            animes.isNotEmpty() ->
                animes

            else ->
                series
        }
        }    private fun mostrarTemporadas(
        serie: Serie
    ) {

        conteudo.removeAllViews()

        adicionarBotaoVoltar {
            mostrarListaSeries(
                listaSeriesAtual()
            )
        }

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize =
            24f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setPadding(
            0,
            dp(10),
            0,
            dp(15)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        var linha: LinearLayout? =
            null

        serie.temporadas.forEachIndexed {
                indice,
                temporada ->

            if (
                indice % 5 == 0
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
                        -1,
                        dp(180)
                    )
                )
            }

            linha!!.addView(
                criarCardTemporada(
                    temporada,
                    serie
                ),
                LinearLayout.LayoutParams(
                    0,
                    dp(155),
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

    private fun criarCardTemporada(
        temporada: Temporada,
        serie: Serie
    ): View {

        val caixa =
            FrameLayout(this)

        caixa.isFocusable =
            true

        caixa.isFocusableInTouchMode =
            true

        caixa.background =
            criarFundoCard()

        val texto =
            TextView(this)

        texto.text =
            "TEMPORADA\n${temporada.numero}ª"

        texto.textSize =
            18f

        texto.typeface =
            Typeface.DEFAULT_BOLD

        texto.setTextColor(
            Color.WHITE
        )

        texto.gravity =
            Gravity.CENTER

        caixa.addView(
            texto,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        val episodios =
            TextView(this)

        episodios.text =
            "${temporada.episodios.size} episódios"

        episodios.textSize =
            11f

        episodios.setTextColor(
            Color.LTGRAY
        )

        episodios.gravity =
            Gravity.CENTER

        episodios.setBackgroundColor(
            Color.argb(
                180,
                0,
                0,
                0
            )
        )

        val epParams =
            FrameLayout.LayoutParams(
                -1,
                dp(30)
            )

        epParams.gravity =
            Gravity.BOTTOM

        caixa.addView(
            episodios,
            epParams
        )

        caixa.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(
                    foco
                )
        }

        caixa.setOnClickListener {

            mostrarEpisodios(
                serie,
                temporada
            )
        }

        return caixa
    }

    private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {

        conteudo.removeAllViews()

        adicionarBotaoVoltar {

            mostrarTemporadas(
                serie
            )
        }

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo} - Temporada ${temporada.numero}"

        titulo.textSize =
            22f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setPadding(
            0,
            dp(10),
            0,
            dp(15)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        temporada.episodios.forEach {
                episodio ->

            val card =
                criarCardEpisodio(
                    episodio,
                    serie
                )

            val params =
                LinearLayout.LayoutParams(
                    -1,
                    dp(70)
                )

            params.setMargins(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
            )

            conteudo.addView(
                card,
                params
            )
        }
    }

    private fun criarCardEpisodio(
        episodio: Episodio,
        serie: Serie
    ): View {

        val caixa =
            LinearLayout(this)

        caixa.orientation =
            LinearLayout.HORIZONTAL

        caixa.gravity =
            Gravity.CENTER_VERTICAL

        caixa.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        caixa.isFocusable =
            true

        caixa.isFocusableInTouchMode =
            true

        caixa.background =
            criarFundoCard()

        val numero =
            TextView(this)

        numero.text =
            "EP ${episodio.numero}"

        numero.textSize =
            16f

        numero.typeface =
            Typeface.DEFAULT_BOLD

        numero.setTextColor(
            Color.RED
        )

        numero.gravity =
            Gravity.CENTER

        caixa.addView(
            numero,
            LinearLayout.LayoutParams(
                dp(75),
                -1
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            episodio.titulo

        titulo.textSize =
            16f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        caixa.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        caixa.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(
                    foco
                )
        }

        caixa.setOnClickListener {

            abrirEpisodio(
                episodio,
                serie
            )
        }

        return caixa
    }

    private fun adicionarBotaoVoltar(
        acao: () -> Unit
    ) {

        val voltar =
            TextView(this)

        voltar.text =
            "←  VOLTAR"

        voltar.textSize =
            16f

        voltar.typeface =
            Typeface.DEFAULT_BOLD

        voltar.setTextColor(
            Color.WHITE
        )

        voltar.gravity =
            Gravity.CENTER

        voltar.isFocusable =
            true

        voltar.isFocusableInTouchMode =
            true

        voltar.background =
            criarFundoCard()

        voltar.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(
                    foco
                )
        }

        voltar.setOnClickListener {
            acao()
        }

        val params =
            LinearLayout.LayoutParams(
                dp(180),
                dp(50)
            )

        params.setMargins(
            dp(10),
            dp(5),
            dp(10),
            dp(8)
        )

        conteudo.addView(
            voltar,
            params
        )
    }

    private fun abrirEpisodio(
        episodio: Episodio,
        serie: Serie
    ) {

        abrirVideo(
            episodio.video,
            "${serie.titulo} - EP ${episodio.numero}: ${episodio.titulo}",
            serie.capa
        )
    }

    private fun abrirVideo(
        url: String,
        titulo: String,
        capa: String
    ) {

        if (url.isBlank()) {

            Toast.makeText(
                this,
                "Vídeo ainda não disponível.",
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
            url
        )

        intent.putExtra(
            "VIDEO_TITLE",
            titulo
        )

        intent.putExtra(
            "VIDEO_COVER",
            capa
        )

        startActivity(
            intent
        )
    }    private fun abrirMenu() {

        if (menuAberto) {
            fecharMenu()
            return
        }

        menuAberto = true

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setBackgroundColor(
            Color.rgb(10, 10, 10)
        )

        menuLateral.setPadding(
            dp(15),
            dp(15),
            dp(10),
            dp(15)
        )

        val params =
            FrameLayout.LayoutParams(
                dp(330),
                -1
            )

        params.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            params
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
                -1,
                dp(60)
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF MENU"

        titulo.textSize =
            22f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        cabecalho.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        val fechar =
            TextView(this)

        fechar.text =
            "✕"

        fechar.textSize =
            25f

        fechar.typeface =
            Typeface.DEFAULT_BOLD

        fechar.setTextColor(
            Color.WHITE
        )

        fechar.gravity =
            Gravity.CENTER

        fechar.isFocusable =
            true

        fechar.isFocusableInTouchMode =
            true

        fechar.setOnClickListener {
            fecharMenu()
        }

        cabecalho.addView(
            fechar,
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable =
            false

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

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

        itensMenuFoco.clear()

        adicionarItemMenu(
            "▶  Continue assistindo"
        ) {
            fecharMenu()
            mostrarContinueAssistindo()
        }

        adicionarItemMenu(
            "★  Favoritos"
        ) {
            fecharMenu()
            mostrarFavoritos()
        }

        adicionarItemMenu(
            "🔍  Pesquisa"
        ) {
            fecharMenu()
            abrirPesquisa()
        }

        adicionarSeparadorPremium(
            "FILMES"
        )

        adicionarItemMenu(
            "🎬  Todos os filmes"
        ) {
            fecharMenu()
            mostrarListaCards(filmes)
        }

        adicionarCategoriaFilmes(
            "Ação"
        )

        adicionarCategoriaFilmes(
            "Aventura"
        )

        adicionarCategoriaFilmes(
            "Animação"
        )

        adicionarCategoriaFilmes(
            "Comédia"
        )

        adicionarCategoriaFilmes(
            "Drama"
        )

        adicionarCategoriaFilmes(
            "Terror"
        )

        adicionarCategoriaFilmes(
            "Ficção"
        )

        adicionarSeparadorPremium(
            "SÉRIES"
        )

        adicionarItemMenu(
            "📺  Todas as séries"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                series,
                "Todos",
                "series"
            )
        }

        adicionarCategoriaSeries(
            "Ação"
        )

        adicionarCategoriaSeries(
            "Aventura"
        )

        adicionarCategoriaSeries(
            "Comédia"
        )

        adicionarCategoriaSeries(
            "Drama"
        )

        adicionarCategoriaSeries(
            "Terror"
        )

        adicionarSeparadorPremium(
            "DORAMAS"
        )

        adicionarItemMenu(
            "🎎  Todos os doramas"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                doramas,
                "Todos",
                "doramas"
            )
        }

        adicionarCategoriaDoramas(
            "Ação"
        )

        adicionarCategoriaDoramas(
            "Romance"
        )

        adicionarCategoriaDoramas(
            "Drama"
        )

        adicionarCategoriaDoramas(
            "Comédia"
        )

        adicionarSeparadorPremium(
            "ANIME"
        )

        adicionarItemMenu(
            "⚡  Todos os animes"
        ) {
            fecharMenu()

            mostrarConteudoEspecial(
                animes,
                "Todos",
                "anime"
            )
        }

        adicionarCategoriaAnimes(
            "Ação"
        )

        adicionarCategoriaAnimes(
            "Aventura"
        )

        adicionarCategoriaAnimes(
            "Comédia"
        )

        adicionarCategoriaAnimes(
            "Fantasia"
        )

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        if (
            ::menuLateral.isInitialized
        ) {

            raiz.removeView(
                menuLateral
            )
        }

        itensMenuFoco.clear()

        botaoMenu.requestFocus()
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
            16f

        item.typeface =
            Typeface.DEFAULT_BOLD

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

        item.background =
            criarFundoCard()

        item.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(
                    foco
                )
        }

        item.setOnClickListener {
            acao()
        }

        menuConteudo.addView(
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

        itensMenuFoco.add(
            item
        )
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
            Color.RED
        )

        separador.gravity =
            Gravity.CENTER_VERTICAL

        separador.setPadding(
            dp(10),
            0,
            0,
            0
        )

        menuConteudo.addView(
            separador,
            LinearLayout.LayoutParams(
                -1,
                dp(42)
            )
        )
    }

    private fun adicionarCategoriaFilmes(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria"
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

    private fun adicionarCategoriaSeries(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                series,
                categoria,
                "series"
            )
        }
    }

    private fun adicionarCategoriaDoramas(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                doramas,
                categoria,
                "doramas"
            )
        }
    }

    private fun adicionarCategoriaAnimes(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                animes,
                categoria,
                "anime"
            )
        }
    }

    private fun mostrarFavoritos() {

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "★  FAVORITOS"

        titulo.textSize =
            24f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum favorito ainda."

        vazio.textSize =
            18f

        vazio.setTextColor(
            Color.LTGRAY
        )

        vazio.gravity =
            Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                -1,
                dp(100)
            )
        )
    }

    private fun mostrarContinueAssistindo() {

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "▶  CONTINUE ASSISTINDO"

        titulo.textSize =
            24f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum vídeo em andamento."

        vazio.textSize =
            18f

        vazio.setTextColor(
            Color.LTGRAY
        )

        vazio.gravity =
            Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                -1,
                dp(100)
            )
        )
    }

    private fun abrirPesquisa() {

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            "🔍  PESQUISA"

        titulo.textSize =
            24f

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome..."

        campo.textSize =
            18f

        campo.setTextColor(
            Color.WHITE
        )

        campo.setHintTextColor(
            Color.GRAY
        )

        campo.setSingleLine(
            true
        )

        campo.isFocusable =
            true

        campo.isFocusableInTouchMode =
            true

        campo.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        campo.background =
            criarFundoCard()

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            ).apply {
                setMargins(
                    dp(20),
                    dp(10),
                    dp(20),
                    dp(15)
                )
            }
        )

        campo.setOnEditorActionListener {
                v,
                actionId,
                event ->

            pesquisar(
                campo.text.toString()
            )

            true
        }

        campo.requestFocus()
    }

    private fun pesquisar(
        texto: String
    ) {

        val busca =
            texto.trim()

        if (busca.isEmpty()) {
            mostrarListaCards(filmes)
            return
        }

        val encontrados =
            filmes.filter {
                it.titulo.contains(
                    busca,
                    ignoreCase = true
                ) ||
                it.categoria.contains(
                    busca,
                    ignoreCase = true
                )
            }

        mostrarListaCards(
            encontrados
        )
    }

    private fun alternarFavorito(
        filme: Filme
    ) {
        // Reservado para o sistema
        // de favoritos.
    }

    private fun tratarDpad(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {
            return false
        }

        if (
            event.keyCode ==
            KeyEvent.KEYCODE_MENU
        ) {

            if (menuAberto) {
                fecharMenu()
            } else {
                abrirMenu()
            }

            return true
        }

        if (
            event.keyCode ==
            KeyEvent.KEYCODE_BACK
        ) {

            if (menuAberto) {

                fecharMenu()

                return true
            }

            if (
                historicoConteudo.isNotEmpty()
            ) {

                val voltar =
                    historicoConteudo
                        .removeAt(
                            historicoConteudo.lastIndex
                        )

                voltar()

                return true
            }
        }

        return false
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            tratarDpad(event)
        ) {
            return true
        }

        return super.dispatchKeyEvent(
            event
        )
    }

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
                        historicoConteudo.lastIndex
                    )

            voltar()

            return
        }

        super.onBackPressed()
    }
}
