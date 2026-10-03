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

        // ===============================
        // TEMA WOLF
        // ===============================

        val fundo =
            ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        fundo.alpha =
            0.55f

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

        // ===============================
        // CAMADA PRINCIPAL
        // ===============================

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

        // ===============================
        // BOTÃO WOLF MENU
        // ===============================

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

        botaoMenu.isFocusable =
            true

        botaoMenu.isFocusableInTouchMode =
            true

        botaoMenu.isClickable =
            true

        botaoMenu.background =
            criarFundoCard(false)

        botaoMenu.setOnFocusChangeListener {
                _,
                foco ->

            botaoMenu.background =
                criarFundoCard(foco)
        }

        // IMPORTANTE:
        // Só abre ao apertar OK.

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

        // ===============================
        // ÁREA DOS CARDS
        // ===============================

        val scroll =
            ScrollView(this)

        scroll.isFocusable =
            false

        scroll.isFocusableInTouchMode =
            false

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        scroll.addView(
            conteudo
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

    // ===============================
    // FUNDO
    // ===============================

    private fun criarFundoCard(
        foco: Boolean
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.argb(
                if (foco) 235 else 190,
                10,
                10,
                10
            )
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
    // IMAGENS
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
    // FILMES
    // ===============================

    private fun carregarFilmes() {

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

        filmes.sortWith(
            compareByDescending<Filme> {
                it.ano
            }.thenBy {
                it.titulo
            }
        )
    }    // ===============================
    // SÉRIES
    // ===============================

    private fun carregarSeries() {

        series.clear()
        doramas.clear()
        animes.clear()

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
    }

    // ===============================
    // LISTA DE FILMES
    // ===============================

    private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        var linha: LinearLayout? = null

        lista.forEachIndexed {
                indice,
                filme ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.TOP

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

            cardsAtuais.add(
                card
            )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(335),
                    1f
                ).apply {

                    leftMargin =
                        dp(4)

                    rightMargin =
                        dp(4)

                    bottomMargin =
                        dp(12)
                }
            )
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0].requestFocus()
        }
    }

    // ===============================
    // CARD DE FILME
    // ===============================

    private fun criarCard(
        filme: Filme
    ): FrameLayout {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        // CAPA

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setBackgroundColor(
            Color.TRANSPARENT
        )

        carregarImagem(
            filme.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            )
        )

        // INFORMAÇÕES

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informacoes.setBackgroundColor(
            Color.argb(
                235,
                10,
                10,
                10
            )
        )

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60),
                Gravity.BOTTOM
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.maxLines =
            1

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val detalhes =
            TextView(this)

        detalhes.text =
            "${filme.ano} • ${filme.categoria}"

        detalhes.textSize =
            12f

        detalhes.setTextColor(
            Color.LTGRAY
        )

        informacoes.addView(
            detalhes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        // BORDA

        val borda =
            View(this)

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

        card.setOnFocusChangeListener {
                _,
                foco ->

            borda.visibility =
                if (foco)
                    View.VISIBLE
                else
                    View.GONE
        }

        card.setOnClickListener {

            abrirVideo(
                filme.titulo,
                filme.video,
                filme.capa
            )
        }

        return card
    }    // ===============================
    // LISTA DE SÉRIES
    // ===============================

    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        var linha: LinearLayout? = null

        lista.forEachIndexed {
                indice,
                serie ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(300)
                    )
                )
            }

            val card =
                criarCardSerie(serie)

            cardsAtuais.add(
                card
            )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(285),
                    1f
                ).apply {

                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(10)
                }
            )
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

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

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
                dp(225)
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.setPadding(
            dp(8),
            dp(4),
            dp(8),
            dp(4)
        )

        info.setBackgroundColor(
            Color.argb(
                235,
                10,
                10,
                10
            )
        )

        card.addView(
            info,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60),
                Gravity.BOTTOM
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.maxLines = 1

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        info.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val categoria =
            TextView(this)

        categoria.text =
            serie.categoria

        categoria.textSize =
            12f

        categoria.setTextColor(
            Color.LTGRAY
        )

        info.addView(
            categoria
        )

        val borda =
            View(this)

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

        card.setOnFocusChangeListener {
                _,
                foco ->

            borda.visibility =
                if (foco)
                    View.VISIBLE
                else
                    View.GONE
        }

        card.setOnClickListener {
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

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo} • TEMPORADAS"

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
            dp(15),
            dp(10),
            dp(15)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        var linha: LinearLayout? = null

        serie.temporadas.forEachIndexed {
                indice,
                temporada ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

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

            cardsAtuais.add(
                card
            )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(150),
                    1f
                ).apply {

                    leftMargin = dp(5)
                    rightMargin = dp(5)
                }
            )
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCardTemporada(
        serie: Serie,
        temporada: Temporada
    ): FrameLayout {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true

        val texto =
            TextView(this)

        texto.text =
            "TEMPORADA ${temporada.numero}\n\n${temporada.episodios.size} episódios"

        texto.textSize =
            17f

        texto.gravity =
            Gravity.CENTER

        texto.setTextColor(
            Color.WHITE
        )

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        texto.background =
            criarFundoCard(false)

        card.addView(
            texto,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val borda =
            View(this)

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

        card.setOnFocusChangeListener {
                _,
                foco ->

            borda.visibility =
                if (foco)
                    View.VISIBLE
                else
                    View.GONE
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
            mostrarTemporadas(serie)
        }

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo} • TEMPORADA ${temporada.numero}"

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            dp(10),
            dp(15),
            dp(10),
            dp(15)
        )

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        var linha: LinearLayout? = null

        temporada.episodios.forEachIndexed {
                indice,
                episodio ->

            if (indice % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(170)
                    )
                )
            }

            val card =
                criarCardEpisodio(
                    serie,
                    episodio
                )

            cardsAtuais.add(
                card
            )

            linha!!.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(150),
                    1f
                ).apply {

                    leftMargin = dp(5)
                    rightMargin = dp(5)
                }
            )
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCardEpisodio(
        serie: Serie,
        episodio: Episodio
    ): FrameLayout {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isFocusableInTouchMode = true

        val texto =
            TextView(this)

        texto.text =
            "EP ${episodio.numero}\n${episodio.titulo}"

        texto.textSize =
            15f

        texto.gravity =
            Gravity.CENTER

        texto.setTextColor(
            Color.WHITE
        )

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        texto.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        texto.background =
            criarFundoCard(false)

        card.addView(
            texto,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val borda =
            View(this)

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

        card.setOnFocusChangeListener {
                _,
                foco ->

            borda.visibility =
                if (foco)
                    View.VISIBLE
                else
                    View.GONE
        }

        card.setOnClickListener {

            abrirVideo(
                "${serie.titulo} - EP ${episodio.numero}",
                episodio.video,
                serie.capa
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

        if (video.isBlank()) {

            Toast.makeText(
                this,
                "Vídeo indisponível",
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
    // MENU
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
            dp(15),
            dp(10)
        )

        val params =
            FrameLayout.LayoutParams(
                dp(360),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        params.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            params
        )

        // ===============================
        // CABEÇALHO
        // ===============================

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

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

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

        botaoFecharMenu.textSize =
            25f

        botaoFecharMenu.gravity =
            Gravity.CENTER

        botaoFecharMenu.setTextColor(
            Color.WHITE
        )

        botaoFecharMenu.isFocusable =
            true

        botaoFecharMenu.isFocusableInTouchMode =
            true

        botaoFecharMenu.setOnFocusChangeListener {
                _,
                foco ->

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
            cabecalho
        )

        // ===============================
        // SCROLL
        // ===============================

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable =
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
        // ITENS
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
            "★  Favoritos"
        ) {
            mostrarFavoritos()
            fecharMenu()
        }

        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {
            abrirPesquisa()
            fecharMenu()
        }

        adicionarSeparador(
            "FILMES"
        )

        adicionarItemMenu(
            "🎬  Todos os filmes (${filmes.size})"
        ) {
            mostrarListaCards(filmes)
            fecharMenu()
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

            adicionarItemMenu(
                "   $categoria"
            ) {

                val lista =
                    filmes.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaCards(
                    lista
                )

                fecharMenu()
            }
        }

        adicionarSeparador(
            "SÉRIES"
        )

        adicionarItemMenu(
            "📺  Todas as séries (${series.size})"
        ) {
            mostrarListaSeries(series)
            fecharMenu()
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

            adicionarItemMenu(
                "   $categoria"
            ) {

                val lista =
                    series.filter {
                        it.categoria.equals(
                            categoria,
                            ignoreCase = true
                        )
                    }

                mostrarListaSeries(
                    lista
                )

                fecharMenu()
            }
        }

        adicionarSeparador(
            "DORAMAS"
        )

        adicionarItemMenu(
            "🎭  Todos os doramas (${doramas.size})"
        ) {

            mostrarListaSeries(
                doramas
            )

            fecharMenu()
        }

        listOf(
            "Ação",
            "Romance",
            "Drama",
            "Comédia"
        ).forEach { categoria ->

            adicionarItemMenu(
                "   $categoria"
            ) {

                mostrarListaSeries(
                    doramas.filter {
                        it.categoria.equals(
                            categoria,
                            true
                        )
                    }
                )

                fecharMenu()
            }
        }

        adicionarSeparador(
            "ANIME"
        )

        adicionarItemMenu(
            "⚡  Todos os animes (${animes.size})"
        ) {

            mostrarListaSeries(
                animes
            )

            fecharMenu()
        }

        listOf(
            "Ação",
            "Aventura",
            "Comédia",
            "Fantasia"
        ).forEach { categoria ->

            adicionarItemMenu(
                "   $categoria"
            ) {

                mostrarListaSeries(
                    animes.filter {
                        it.categoria.equals(
                            categoria,
                            true
                        )
                    }
                )

                fecharMenu()
            }
        }

        botaoFecharMenu.requestFocus()
    }

    // ===============================
    // SEPARADOR DO MENU
    // ===============================

    private fun adicionarSeparador(
        texto: String
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            14f

        item.setTextColor(
            Color.RED
        )

        item.setTypeface(
            null,
            Typeface.BOLD
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
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
            16f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setTypeface(
            null,
            Typeface.BOLD
        )

        item.setPadding(
            dp(12),
            0,
            dp(12),
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

        item.setOnFocusChangeListener {
                _,
                foco ->

            item.background =
                criarFundoCard(
                    foco
                )
        }

        item.setOnClickListener {
            acao()
        }

        itensMenuFoco.add(
            item
        )

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )
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

        if (::menuLateral.isInitialized) {
            raiz.removeView(
                menuLateral
            )
        }

        itensMenuFoco.clear()

        if (cardsAtuais.isNotEmpty()) {

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

    // ===============================
    // MOVER CARD
    // ===============================

    private fun moverCard(
        deslocamento: Int
    ) {

        if (cardsAtuais.isEmpty()) {
            return
        }

        val atual =
            cardsAtuais.indexOfFirst {
                it.hasFocus()
            }

        if (atual < 0) {
            return
        }

        val novo =
            atual + deslocamento

        if (
            novo < 0 ||
            novo >= cardsAtuais.size
        ) {
            return
        }

        indiceCardAtual =
            novo

        cardsAtuais[novo]
            .requestFocus()

        rolarParaCard(
            cardsAtuais[novo]
        )
    }

    private fun rolarParaCard(
        view: View
    ) {

        view.post {

            view.parent?.let {
                (it as? ViewGroup)
            }

            view.requestRectangleOnScreen(
                android.graphics.Rect(
                    0,
                    0,
                    view.width,
                    view.height
                ),
                true
            )
        }
    }

    // ===============================
    // FAVORITOS
    // ===============================

    private fun mostrarFavoritos() {

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

        conteudo.removeAllViews()

        cardsAtuais.clear()

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

        campo.textSize =
            20f

        campo.isFocusable =
            true

        campo.isFocusableInTouchMode =
            true

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        campo.requestFocus()

        campo.setOnEditorActionListener {
                _,
                _,
                _ ->

            pesquisar(
                campo.text.toString()
            )

            true
        }
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

        val resultado =
            filmes.filter {
                it.titulo.contains(
                    busca,
                    ignoreCase = true
                )
            }

        mostrarListaCards(
            resultado
        )
    }

    // ===============================
    // CONTROLE REMOTO
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

        // ===========================
        // MENU ABERTO
        // ===========================

        if (menuAberto) {

            when (
                event.keyCode
            ) {

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

        // ===========================
        // MENU FECHADO
        // ===========================

        when (
            event.keyCode
        ) {

            KeyEvent.KEYCODE_DPAD_UP -> {

                // Se está no botão,
                // não faz nada.

                if (
                    botaoMenu.hasFocus()
                ) {
                    return true
                }

                val atual =
                    cardsAtuais.indexOfFirst {
                        it.hasFocus()
                    }

                // PRIMEIRO CARD:
                // sobe para WOLF MENU.

                if (atual == 0) {

                    botaoMenu.requestFocus()

                    return true
                }

                // Uma fileira para cima.

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

                // WOLF MENU:
                // só abre com OK.

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
                    historicoConteudo.isNotEmpty()
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

    // ===============================
    // MOVIMENTO DO MENU
    // ===============================

    private fun moverMenu(
        deslocamento: Int
    ) {

        if (
            itensMenuFoco.isEmpty()
        ) {
            return
        }

        val atual =
            itensMenuFoco.indexOfFirst {
                it.hasFocus()
            }

        if (atual < 0) {

            itensMenuFoco[0]
                .requestFocus()

            return
        }

        val novo =
            atual + deslocamento

        if (
            novo < 0 ||
            novo >= itensMenuFoco.size
        ) {
            return
        }

        itensMenuFoco[novo]
            .requestFocus()

        ajustarScrollMenu(
            itensMenuFoco[novo]
        )
    }

    private fun ajustarScrollMenu(
        view: View
    ) {

        view.post {

            menuScroll.scrollTo(
                0,
                view.top -
                    dp(80)
            )
        }
    }

    // ===============================
    // BACK
    // ===============================

    @Suppress(
        "DEPRECATION"
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
                    .removeLast()

            voltar()

            return
        }

        super.onBackPressed()
    }
}
