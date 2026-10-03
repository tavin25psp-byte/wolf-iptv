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

    private val cardsFilmes =
        mutableListOf<View>()

    private var indiceCardAtual = 0
    private var indiceMenuAtual = 0

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

                runOnUiThread {

                    fundo.setImageBitmap(
                        bitmap
                    )
                }

                conexao.disconnect()

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

        botaoMenu.isFocusableInTouchMode =
            true

        botaoMenu.isClickable = true

        botaoMenu.isEnabled = true

        botaoMenu.background =
            criarFundoCard(false)

        botaoMenu.setOnFocusChangeListener {
                view,
                foco ->

            view.background =
                criarFundoCard(foco)

            view.invalidate()
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

        scroll.isVerticalScrollBarEnabled =
            false

        scroll.isHorizontalScrollBarEnabled =
            false

        scroll.overScrollMode =
            View.OVER_SCROLL_NEVER

        scroll.descendantFocusability =
            ViewGroup.FOCUS_AFTER_DESCENDANTS

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

    private fun criarFundoCard(
        foco: Boolean = false
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            if (foco) {
                Color.rgb(45, 45, 45)
            } else {
                Color.rgb(18, 18, 18)
            }
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        fundo.setStroke(
            if (foco) {
                dp(4)
            } else {
                dp(1)
            },
            if (foco) {
                Color.RED
            } else {
                Color.rgb(
                    55,
                    55,
                    55
                )
            }
        )

        return fundo
    }

    private fun aplicarFoco(
        view: View,
        foco: Boolean
    ) {

        view.background =
            criarFundoCard(foco)

        view.invalidate()
    }

    private fun carregarImagem(
        url: String,
        imageView: ImageView
    ) {

        if (
            cacheCapas.containsKey(url)
        ) {

            imageView.setImageBitmap(
                cacheCapas[url]
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

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
            }
        }
    }private fun carregarFilmes() {

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
                "https://i.postimg.cc/5t0bhHHr/images-(6).jpg",
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
}

private fun carregarSeries() {

    series.clear()

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

    doramas.clear()
    animes.clear()
}private fun mostrarListaCards(
    lista: List<Filme>
) {

    conteudo.removeAllViews()

    historicoConteudo.clear()

    cardsFilmes.clear()
    indiceCardAtual = 0

    if (lista.isEmpty()) {

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum filme encontrado."

        vazio.textSize = 20f

        vazio.setTextColor(
            Color.WHITE
        )

        vazio.gravity =
            Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(180)
            )
        )

        return
    }

    var linha =
        LinearLayout(this)

    linha.orientation =
        LinearLayout.HORIZONTAL

    conteudo.addView(
        linha,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(245)
        )
    )

    lista.forEachIndexed {
        indice,
        filme ->

        if (
            indice > 0 &&
            indice % 5 == 0
        ) {

            linha =
                LinearLayout(this)

            linha.orientation =
                LinearLayout.HORIZONTAL

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(245)
                )
            )
        }

        val card =
            criarCard(filme)

        cardsFilmes.add(card)

        linha.addView(
            card,
            LinearLayout.LayoutParams(
                0,
                dp(225),
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

    if (
        cardsFilmes.isNotEmpty()
    ) {

        indiceCardAtual = 0

        cardsFilmes[0].post {

            cardsFilmes[0].requestFocus()

            rolarParaCard(
                cardsFilmes[0]
            )
        }
    }
}

private fun criarCard(
    filme: Filme
): FrameLayout {

    val card =
        FrameLayout(this)

    card.isFocusable = true

    card.isFocusableInTouchMode =
        true

    card.isClickable = true

    card.isEnabled = true

    card.background =
        criarFundoCard(false)

    val imagem =
        ImageView(this)

    imagem.scaleType =
        ImageView.ScaleType.FIT_CENTER

    imagem.setBackgroundColor(
        Color.BLACK
    )

    imagem.isFocusable = false
    imagem.isClickable = false

    card.addView(
        imagem,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    carregarImagem(
        filme.capa,
        imagem
    )

    val ano =
        TextView(this)

    ano.text =
        filme.ano.toString()

    ano.textSize = 12f

    ano.setTextColor(
        Color.WHITE
    )

    ano.setTypeface(
        null,
        Typeface.BOLD
    )

    ano.gravity =
        Gravity.CENTER

    ano.setBackgroundColor(
        Color.RED
    )

    ano.isFocusable = false
    ano.isClickable = false

    val anoParams =
        FrameLayout.LayoutParams(
            dp(55),
            dp(28)
        )

    anoParams.gravity =
        Gravity.TOP or
        Gravity.END

    card.addView(
        ano,
        anoParams
    )

    val titulo =
        TextView(this)

    titulo.text =
        filme.titulo

    titulo.textSize = 14f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER

    titulo.setPadding(
        dp(6),
        dp(4),
        dp(6),
        dp(4)
    )

    titulo.setBackgroundColor(
        Color.argb(
            220,
            0,
            0,
            0
        )
    )

    titulo.isFocusable = false
    titulo.isClickable = false

    val tituloParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

    tituloParams.gravity =
        Gravity.BOTTOM

    card.addView(
        titulo,
        tituloParams
    )

    card.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    card.setOnClickListener {

        abrirVideo(
            filme.video,
            filme.titulo,
            filme.capa
        )
    }

    return card
}

private fun rolarParaCard(
    card: View
) {

    val parent =
        card.parent

    val linha =
        parent as? View ?: return

    val scroll =
        linha.parent?.parent as? ScrollView
            ?: return

    val topo =
        linha.top

    val baixo =
        linha.bottom

    val atual =
        scroll.scrollY

    val altura =
        scroll.height

    val margem =
        dp(20)

    if (
        topo <
        atual + margem
    ) {

        scroll.scrollTo(
            0,
            maxOf(
                0,
                topo - margem
            )
        )

    } else if (
        baixo >
        atual + altura - margem
    ) {

        scroll.scrollTo(
            0,
            maxOf(
                0,
                baixo -
                altura +
                margem
            )
        )
    }
}

private fun moverCard(
    movimento: Int
) {

    if (
        cardsFilmes.isEmpty()
    ) {
        return
    }

    val novoIndice =
        indiceCardAtual + movimento

    if (
        novoIndice < 0 ||
        novoIndice >= cardsFilmes.size
    ) {
        return
    }

    indiceCardAtual =
        novoIndice

    val card =
        cardsFilmes[
            indiceCardAtual
        ]

    card.requestFocus()

    card.post {
        rolarParaCard(card)
    }
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

    mostrarListaSeries(lista)
}

private fun mostrarListaSeries(
    lista: List<Serie>
) {

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    if (lista.isEmpty()) {

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum conteúdo encontrado."

        vazio.textSize = 20f

        vazio.setTextColor(
            Color.WHITE
        )

        vazio.gravity =
            Gravity.CENTER

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(180)
            )
        )

        return
    }

    var linha =
        criarLinhaEspecial()

    conteudo.addView(
        linha,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(245)
        )
    )

    lista.forEachIndexed {
        indice,
        serie ->

        if (
            indice > 0 &&
            indice % 5 == 0
        ) {

            linha =
                criarLinhaEspecial()

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(245)
                )
            )
        }

        val card =
            criarCardSerie(serie)

        cardsFilmes.add(card)

        linha.addView(
            card,
            LinearLayout.LayoutParams(
                0,
                dp(225),
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

    if (
        cardsFilmes.isNotEmpty()
    ) {

        indiceCardAtual = 0

        cardsFilmes[0].post {

            cardsFilmes[0].requestFocus()

            rolarParaCard(
                cardsFilmes[0]
            )
        }
    }
}

private fun criarLinhaEspecial():
    LinearLayout {

    val linha =
        LinearLayout(this)

    linha.orientation =
        LinearLayout.HORIZONTAL

    linha.gravity =
        Gravity.CENTER_VERTICAL

    return linha
}

private fun criarCardSerie(
    serie: Serie
): FrameLayout {

    val card =
        FrameLayout(this)

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true
    card.isEnabled = true

    card.background =
        criarFundoCard(false)

    val imagem =
        ImageView(this)

    imagem.scaleType =
        ImageView.ScaleType.FIT_CENTER

    imagem.setBackgroundColor(
        Color.BLACK
    )

    imagem.isFocusable = false
    imagem.isClickable = false

    card.addView(
        imagem,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    carregarImagem(
        serie.capa,
        imagem
    )

    val titulo =
        TextView(this)

    titulo.text =
        serie.titulo

    titulo.textSize = 14f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER

    titulo.setPadding(
        dp(6),
        dp(4),
        dp(6),
        dp(4)
    )

    titulo.setBackgroundColor(
        Color.argb(
            220,
            0,
            0,
            0
        )
    )

    titulo.isFocusable = false
    titulo.isClickable = false

    val tituloParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        )

    tituloParams.gravity =
        Gravity.BOTTOM

    card.addView(
        titulo,
        tituloParams
    )

    card.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    card.setOnClickListener {

        historicoConteudo.add {

            mostrarListaSeries(
                listaSeriesAtual()
            )
        }

        mostrarTemporadas(serie)
    }

    return card
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
    }private fun mostrarTemporadas(
    serie: Serie
) {

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    val titulo =
        TextView(this)

    titulo.text =
        "${serie.titulo}\n\nTEMPORADAS"

    titulo.textSize = 22f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER

    titulo.setPadding(
        dp(10),
        dp(10),
        dp(10),
        dp(10)
    )

    titulo.isFocusable = false

    conteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(90)
        )
    )

    val linha =
        LinearLayout(this)

    linha.orientation =
        LinearLayout.HORIZONTAL

    linha.gravity =
        Gravity.CENTER

    conteudo.addView(
        linha,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(245)
        )
    )

    serie.temporadas.forEach {
        temporada ->

        val card =
            criarCardTemporada(
                serie,
                temporada
            )

        cardsFilmes.add(card)

        linha.addView(
            card,
            LinearLayout.LayoutParams(
                dp(220),
                dp(220)
            ).apply {

                setMargins(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )
            }
        )
    }

    if (
        cardsFilmes.isNotEmpty()
    ) {

        cardsFilmes[0].post {
            cardsFilmes[0].requestFocus()
        }
    }

    adicionarBotaoVoltar {

        if (
            historicoConteudo.isNotEmpty()
        ) {

            historicoConteudo
                .removeLast()
                .invoke()

        } else {

            mostrarListaSeries(
                listaSeriesAtual()
            )
        }
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
    card.isClickable = true
    card.isEnabled = true

    card.background =
        criarFundoCard(false)

    val titulo =
        TextView(this)

    titulo.text =
        "TEMPORADA ${temporada.numero}\n\n${temporada.episodios.size} episódios"

    titulo.textSize = 18f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER

    titulo.setPadding(
        dp(10),
        dp(10),
        dp(10),
        dp(10)
    )

    titulo.isFocusable = false

    card.addView(
        titulo,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    card.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    card.setOnClickListener {

        historicoConteudo.add {
            mostrarTemporadas(serie)
        }

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

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    val titulo =
        TextView(this)

    titulo.text =
        "${serie.titulo}\nTEMPORADA ${temporada.numero}"

    titulo.textSize = 21f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER

    titulo.setPadding(
        dp(10),
        dp(10),
        dp(10),
        dp(10)
    )

    titulo.isFocusable = false

    conteudo.addView(
        titulo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(80)
        )
    )

    temporada.episodios.forEach {
        episodio ->

        val card =
            criarCardEpisodio(
                episodio
            )

        cardsFilmes.add(card)

        conteudo.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
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

    if (
        cardsFilmes.isNotEmpty()
    ) {

        cardsFilmes[0].post {
            cardsFilmes[0].requestFocus()
        }
    }

    adicionarBotaoVoltar {

        if (
            historicoConteudo.isNotEmpty()
        ) {

            historicoConteudo
                .removeLast()
                .invoke()

        } else {

            mostrarTemporadas(
                serie
            )
        }
    }
}

private fun criarCardEpisodio(
    episodio: Episodio
): LinearLayout {

    val card =
        LinearLayout(this)

    card.orientation =
        LinearLayout.HORIZONTAL

    card.gravity =
        Gravity.CENTER_VERTICAL

    card.setPadding(
        dp(18),
        0,
        dp(18),
        0
    )

    card.isFocusable = true
    card.isFocusableInTouchMode = true
    card.isClickable = true
    card.isEnabled = true

    card.background =
        criarFundoCard(false)

    val texto =
        TextView(this)

    texto.text =
        "EP ${episodio.numero}  •  ${episodio.titulo}"

    texto.textSize = 16f

    texto.setTextColor(
        Color.WHITE
    )

    texto.setTypeface(
        null,
        Typeface.BOLD
    )

    texto.gravity =
        Gravity.CENTER_VERTICAL

    texto.isFocusable = false

    card.addView(
        texto,
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        )
    )

    card.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    card.setOnClickListener {

        abrirEpisodio(
            episodio
        )
    }

    return card
}

private fun adicionarBotaoVoltar(
    acao: () -> Unit
) {

    val voltar =
        TextView(this)

    voltar.text =
        "← VOLTAR"

    voltar.textSize = 17f

    voltar.setTextColor(
        Color.WHITE
    )

    voltar.setTypeface(
        null,
        Typeface.BOLD
    )

    voltar.gravity =
        Gravity.CENTER

    voltar.isFocusable = true
    voltar.isFocusableInTouchMode = true
    voltar.isClickable = true
    voltar.isEnabled = true

    voltar.background =
        criarFundoCard(false)

    voltar.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    voltar.setOnClickListener {
        acao()
    }

    conteudo.addView(
        voltar,
        LinearLayout.LayoutParams(
            dp(180),
            dp(55)
        ).apply {

            setMargins(
                0,
                dp(15),
                0,
                dp(15)
            )
        }
    )
}

private fun abrirEpisodio(
    episodio: Episodio
) {

    abrirVideo(
        episodio.video,
        episodio.titulo,
        ""
    )
}

private fun abrirVideo(
    video: String,
    titulo: String,
    capa: String
) {

    if (
        video.isBlank()
    ) {

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
}private fun abrirMenu() {

    if (menuAberto) {
        return
    }

    menuAberto = true

    itensMenuFoco.clear()
    indiceMenuAtual = 0

    menuLateral =
        LinearLayout(this)

    menuLateral.orientation =
        LinearLayout.VERTICAL

    menuLateral.setBackgroundColor(
        Color.rgb(12, 12, 12)
    )

    menuLateral.setPadding(
        dp(12),
        dp(12),
        dp(12),
        dp(12)
    )

    val params =
        FrameLayout.LayoutParams(
            dp(330),
            ViewGroup.LayoutParams.MATCH_PARENT
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

    val titulo =
        TextView(this)

    titulo.text =
        "WOLF MENU"

    titulo.textSize = 20f

    titulo.setTextColor(
        Color.WHITE
    )

    titulo.setTypeface(
        null,
        Typeface.BOLD
    )

    titulo.gravity =
        Gravity.CENTER_VERTICAL

    titulo.isFocusable = false

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

    botaoFecharMenu.textSize = 24f

    botaoFecharMenu.setTextColor(
        Color.WHITE
    )

    botaoFecharMenu.gravity =
        Gravity.CENTER

    botaoFecharMenu.isFocusable = true

    botaoFecharMenu.isFocusableInTouchMode =
        true

    botaoFecharMenu.isClickable = true

    botaoFecharMenu.isEnabled = true

    botaoFecharMenu.background =
        criarFundoCard(false)

    botaoFecharMenu.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
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

    menuScroll =
        ScrollView(this)

    menuScroll.isFocusable = false
    menuScroll.isFocusableInTouchMode = false
    menuScroll.isVerticalScrollBarEnabled = false
    menuScroll.isHorizontalScrollBarEnabled = false
    menuScroll.overScrollMode =
        View.OVER_SCROLL_NEVER

    menuScroll.descendantFocusability =
        ViewGroup.FOCUS_AFTER_DESCENDANTS

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

    adicionarItemMenu(
        "▶  Continuar assistindo"
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
        "⌕  Pesquisa"
    ) {

        fecharMenu()
        abrirPesquisa()
    }

    adicionarSeparadorPremium(
        "FILMES"
    )

    adicionarItemMenu(
        "🎬  Todos os filmes (${quantidadeFilmes("Todos")})"
    ) {

        fecharMenu()
        mostrarListaCards(filmes)
    }

    listOf(
        "Ação",
        "Aventura",
        "Animação",
        "Comédia",
        "Drama",
        "Terror",
        "Ficção"
    ).forEach {

        adicionarCategoriaFilmes(it)
    }

    adicionarSeparadorPremium(
        "SÉRIES"
    )

    adicionarItemMenu(
        "📺  Todas as séries (${quantidadeSeries(series, "Todos")})"
    ) {

        fecharMenu()

        mostrarConteudoEspecial(
            series,
            "Todos",
            "series"
        )
    }

    listOf(
        "Ação",
        "Aventura",
        "Comédia",
        "Drama",
        "Terror"
    ).forEach {

        adicionarCategoriaSeries(it)
    }

    adicionarSeparadorPremium(
        "DORAMAS"
    )

    adicionarItemMenu(
        "🎭  Todos os doramas (${quantidadeSeries(doramas, "Todos")})"
    ) {

        fecharMenu()

        mostrarConteudoEspecial(
            doramas,
            "Todos",
            "doramas"
        )
    }

    listOf(
        "Ação",
        "Romance",
        "Drama",
        "Comédia"
    ).forEach {

        adicionarCategoriaDoramas(it)
    }

    adicionarSeparadorPremium(
        "ANIME"
    )

    adicionarItemMenu(
        "⚡  Todos os animes (${quantidadeSeries(animes, "Todos")})"
    ) {

        fecharMenu()

        mostrarConteudoEspecial(
            animes,
            "Todos",
            "anime"
        )
    }

    listOf(
        "Ação",
        "Aventura",
        "Comédia",
        "Fantasia"
    ).forEach {

        adicionarCategoriaAnimes(it)
    }

    if (
        itensMenuFoco.isNotEmpty()
    ) {

        indiceMenuAtual = 0

        itensMenuFoco[0].post {

            itensMenuFoco[0]
                .requestFocus()

            ajustarScrollMenu(
                itensMenuFoco[0]
            )
        }
    }
}

private fun fecharMenu() {

    if (!menuAberto) {
        return
    }

    menuAberto = false

    if (
        ::menuLateral.isInitialized &&
        menuLateral.parent != null
    ) {

        raiz.removeView(
            menuLateral
        )
    }

    itensMenuFoco.clear()

    indiceMenuAtual = 0

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

    item.textSize = 16f

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
        dp(14),
        0,
        dp(10),
        0
    )

    item.isFocusable = true
    item.isFocusableInTouchMode = true
    item.isClickable = true
    item.isEnabled = true

    item.background =
        criarFundoCard(false)

    item.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )

        if (foco) {

            val indice =
                itensMenuFoco.indexOf(
                    view
                )

            if (indice >= 0) {
                indiceMenuAtual = indice
            }
        }
    }

    item.setOnClickListener {
        acao()
    }

    menuConteudo.addView(
        item,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        )
    )

    itensMenuFoco.add(item)
}

private fun adicionarSeparadorPremium(
    texto: String
) {

    val separador =
        TextView(this)

    separador.text =
        texto

    separador.textSize = 14f

    separador.setTextColor(
        Color.RED
    )

    separador.setTypeface(
        null,
        Typeface.BOLD
    )

    separador.gravity =
        Gravity.CENTER_VERTICAL

    separador.setPadding(
        dp(8),
        dp(15),
        dp(8),
        dp(5)
    )

    separador.isFocusable = false

    menuConteudo.addView(
        separador,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(45)
        )
    )
}

private fun ajustarScrollMenu(
    item: View
) {

    if (
        !::menuScroll.isInitialized
    ) {
        return
    }

    menuScroll.post {

        val topo =
            item.top

        val baixo =
            item.bottom

        val atual =
            menuScroll.scrollY

        val altura =
            menuScroll.height

        val margem =
            dp(15)

        if (
            topo <
            atual + margem
        ) {

            menuScroll.scrollTo(
                0,
                maxOf(
                    0,
                    topo - margem
                )
            )

        } else if (
            baixo >
            atual + altura - margem
        ) {

            menuScroll.scrollTo(
                0,
                maxOf(
                    0,
                    baixo -
                    altura +
                    margem
                )
            )
        }
    }
}

private fun moverMenu(
    movimento: Int
) {

    if (
        itensMenuFoco.isEmpty()
    ) {
        return
    }

    val novoIndice =
        indiceMenuAtual + movimento

    if (
        novoIndice < 0 ||
        novoIndice >= itensMenuFoco.size
    ) {
        return
    }

    indiceMenuAtual =
        novoIndice

    val item =
        itensMenuFoco[
            indiceMenuAtual
        ]

    item.requestFocus()

    ajustarScrollMenu(item)
}

private fun adicionarCategoriaFilmes(
    categoria: String
) {

    adicionarItemMenu(
        "   • $categoria (${quantidadeFilmes(categoria)})"
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
        "   • $categoria (${quantidadeSeries(series, categoria)})"
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
        "   • $categoria (${quantidadeSeries(doramas, categoria)})"
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
        "   • $categoria (${quantidadeSeries(animes, categoria)})"
    ) {

        fecharMenu()

        mostrarConteudoEspecial(
            animes,
            categoria,
            "anime"
        )
    }
}

private fun quantidadeFilmes(
    categoria: String
): Int {

    if (
        categoria.equals(
            "Todos",
            ignoreCase = true
        )
    ) {

        return filmes.size
    }

    return filmes.count {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }
}

private fun quantidadeSeries(
    lista: List<Serie>,
    categoria: String
): Int {

    if (
        categoria.equals(
            "Todos",
            ignoreCase = true
        )
    ) {

        return lista.size
    }

    return lista.count {

        it.categoria.equals(
            categoria,
            ignoreCase = true
        )
    }
}private fun mostrarFavoritos() {

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    val texto =
        TextView(this)

    texto.text =
        "★ FAVORITOS\n\nNenhum conteúdo favoritado."

    texto.textSize = 20f

    texto.setTextColor(
        Color.WHITE
    )

    texto.gravity =
        Gravity.CENTER

    texto.setPadding(
        dp(20),
        dp(40),
        dp(20),
        dp(40)
    )

    texto.isFocusable = false

    conteudo.addView(
        texto,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(180)
        )
    )
}

private fun mostrarContinueAssistindo() {

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    val texto =
        TextView(this)

    texto.text =
        "▶ CONTINUE ASSISTINDO\n\nNenhum conteúdo para continuar."

    texto.textSize = 20f

    texto.setTextColor(
        Color.WHITE
    )

    texto.gravity =
        Gravity.CENTER

    texto.setPadding(
        dp(20),
        dp(40),
        dp(20),
        dp(40)
    )

    texto.isFocusable = false

    conteudo.addView(
        texto,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(180)
        )
    )
}

private fun abrirPesquisa() {

    conteudo.removeAllViews()

    cardsFilmes.clear()
    indiceCardAtual = 0

    val campo =
        EditText(this)

    campo.hint =
        "Digite o nome do filme..."

    campo.setHintTextColor(
        Color.GRAY
    )

    campo.setTextColor(
        Color.WHITE
    )

    campo.textSize = 18f

    campo.setSingleLine(true)

    campo.isFocusable = true
    campo.isFocusableInTouchMode = true

    campo.setPadding(
        dp(15),
        0,
        dp(15),
        0
    )

    campo.setBackgroundColor(
        Color.rgb(
            25,
            25,
            25
        )
    )

    conteudo.addView(
        campo,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(60)
        )
    )

    val botao =
        TextView(this)

    botao.text =
        "PESQUISAR"

    botao.textSize = 17f

    botao.setTextColor(
        Color.WHITE
    )

    botao.setTypeface(
        null,
        Typeface.BOLD
    )

    botao.gravity =
        Gravity.CENTER

    botao.isFocusable = true
    botao.isFocusableInTouchMode = true
    botao.isClickable = true

    botao.background =
        criarFundoCard(false)

    botao.setOnFocusChangeListener {
            view,
            foco ->

        aplicarFoco(
            view,
            foco
        )
    }

    botao.setOnClickListener {

        pesquisar(
            campo.text.toString()
        )
    }

    val botaoParams =
        LinearLayout.LayoutParams(
            dp(180),
            dp(55)
        )

    botaoParams.setMargins(
        0,
        dp(12),
        0,
        dp(12)
    )

    conteudo.addView(
        botao,
        botaoParams
    )

    campo.requestFocus()
}

private fun pesquisar(
    texto: String
) {

    val busca =
        texto.trim()

    if (
        busca.isEmpty()
    ) {

        mostrarListaCards(
            filmes
        )

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
    // Reservado para favoritos.
}

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

    /*
     * ============================
     * MENU ABERTO
     * ============================
     */

    if (menuAberto) {

        when (
            event.keyCode
        ) {

            KeyEvent.KEYCODE_DPAD_UP -> {

                if (
                    botaoFecharMenu.hasFocus()
                ) {

                    if (
                        itensMenuFoco.isNotEmpty()
                    ) {

                        indiceMenuAtual =
                            itensMenuFoco.lastIndex

                        val item =
                            itensMenuFoco[
                                indiceMenuAtual
                            ]

                        item.requestFocus()

                        ajustarScrollMenu(item)
                    }

                } else {

                    moverMenu(-1)
                }

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (
                    botaoFecharMenu.hasFocus()
                ) {

                    if (
                        itensMenuFoco.isNotEmpty()
                    ) {

                        indiceMenuAtual = 0

                        val item =
                            itensMenuFoco[0]

                        item.requestFocus()

                        ajustarScrollMenu(item)
                    }

                } else {

                    if (
                        indiceMenuAtual ==
                        itensMenuFoco.lastIndex
                    ) {

                        botaoFecharMenu.requestFocus()

                    } else {

                        moverMenu(1)
                    }
                }

                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (
                    botaoFecharMenu.hasFocus()
                ) {

                    fecharMenu()

                    return true
                }

                if (
                    indiceMenuAtual >= 0 &&
                    indiceMenuAtual <
                    itensMenuFoco.size
                ) {

                    itensMenuFoco[
                        indiceMenuAtual
                    ].performClick()
                }

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

    /*
     * ============================
     * MENU FECHADO
     * ============================
     */

    when (
        event.keyCode
    ) {

        KeyEvent.KEYCODE_DPAD_LEFT -> {

            if (
                botaoMenu.hasFocus()
            ) {

                return true
            }

            if (
                cardsFilmes.isNotEmpty()
            ) {

                val coluna =
                    indiceCardAtual % 5

                if (
                    coluna == 0
                ) {

                    botaoMenu.requestFocus()

                    return true
                }

                moverCard(-1)

                return true
            }

            return true
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {

            if (
                botaoMenu.hasFocus()
            ) {

                if (
                    cardsFilmes.isNotEmpty()
                ) {

                    indiceCardAtual = 0

                    val card =
                        cardsFilmes[0]

                    card.requestFocus()

                    card.post {
                        rolarParaCard(card)
                    }
                }

                return true
            }

            moverCard(1)

            return true
        }

        KeyEvent.KEYCODE_DPAD_UP -> {

            moverCard(-5)

            return true
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> {

            moverCard(5)

            return true
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER -> {

            currentFocus?.performClick()

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

                val ultima =
                    historicoConteudo
                        .removeLast()

                ultima()

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

        val ultima =
            historicoConteudo
                .removeLast()

        ultima()

        return
    }

    super.onBackPressed()
}
}
