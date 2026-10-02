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
                    "https://i.postimg.cc/5t0bhHHr/images-(6).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Meu%20Malvado%20Favorito%204.mp4"
                ),

                Filme(
                    "Divertida Mente 2",
                    2024,
                    "Animação",
                    "https://i.postimg.cc/0jgfgwN6/aa61ae8fb015160d802c4d5cb4fe6858058ea76485c3498ed9ff431eee4fc83f-(1).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Divertida%20Mente%202.mp4"
                ),

                Filme(
                    "Planeta dos Macacos: O Reinado",
                    2024,
                    "Ação",
                    "https://i.postimg.cc/fL1HwP8X/MV5BYTkw-Mm-Iy-NWQt-NWEy-ZS00M2M3LTgx-ZGEt-MDlm-MGIz-ZGM4NWNh-Xk-Ey-Xk-Fqc-Gc-V1-(1).jpg",
                    "https://wolf-channel-cdn.b-cdn.net/Filmes%20/Filmes%20/Planeta%20dos%20Macacos%20O%20Reinado.mp4"
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
    }    private fun dp(valor: Int): Int {
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
            Color.rgb(
                18,
                18,
                18
            )
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
    }

    private fun mostrarListaCards(
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

        var linha:
            LinearLayout? =
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
                criarCard(
                    filme
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

        var linha:
            LinearLayout? =
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

        var linha:
            LinearLayout? =
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

        if (menuLateral != null) {
            fecharMenu()
            return
        }

        menuLateral =
            LinearLayout(this)

        menuLateral!!.orientation =
            LinearLayout.VERTICAL

        menuLateral!!.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        menuLateral!!.setBackgroundColor(
            Color.rgb(10, 10, 10)
        )

        val scroll =
            ScrollView(this)

        scroll.addView(
            menuLateral
        )

        val params =
            FrameLayout.LayoutParams(
                dp(360),
                -1
            )

        params.gravity =
            Gravity.START

        root.addView(
            scroll,
            params
        )

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
            "🔍  Buscar"
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
            mostrarListaCards(
                filmes.sortedWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo
                    }
                )
            )
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
            "📺  Todas as séries (${quantidadeSeries(series, "Todos")})"
        ) {
            fecharMenu()
            mostrarConteudoEspecial(
                "Todos",
                series
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
            "🎎  Todos os doramas (${quantidadeSeries(doramas, "Todos")})"
        ) {
            fecharMenu()
            mostrarConteudoEspecial(
                "Todos",
                doramas
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
            "⚡  Todos os animes (${quantidadeSeries(animes, "Todos")})"
        ) {
            fecharMenu()
            mostrarConteudoEspecial(
                "Todos",
                animes
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
    }

    private fun fecharMenu() {

        menuLateral?.let {

            val parent =
                it.parent

            if (parent is ViewGroup) {
                parent.parent?.let { p ->
                    if (p is ViewGroup) {
                        p.removeView(parent)
                    }
                }
            }
        }

        menuLateral =
            null
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
            17f

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

        menuLateral?.addView(
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

    private fun adicionarSeparadorPremium(
        texto: String
    ) {

        val separador =
            TextView(this)

        separador.text =
            texto

        separador.textSize =
            14f

        separador.typeface =
            Typeface.DEFAULT_BOLD

        separador.setTextColor(
            Color.RED
        )

        separador.gravity =
            Gravity.CENTER_VERTICAL

        separador.setPadding(
            dp(10),
            dp(12),
            0,
            dp(5)
        )

        menuLateral?.addView(
            separador,
            LinearLayout.LayoutParams(
                -1,
                dp(45)
            )
        )
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
    }

    private fun adicionarCategoriaFilmes(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria (${quantidadeFilmes(categoria)})"
        ) {

            fecharMenu()

            val lista =
                filmes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }.sortedWith(
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
    }

    private fun adicionarCategoriaSeries(
        categoria: String
    ) {

        adicionarItemMenu(
            "   • $categoria (${quantidadeSeries(series, categoria)})"
        ) {

            fecharMenu()

            mostrarConteudoEspecial(
                categoria,
                series
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
                categoria,
                doramas
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
                categoria,
                animes
            )
        }
    }    private fun mostrarFavoritos() {

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
                dp(70)
            )
        )

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum favorito adicionado."

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
                dp(70)
            )
        )

        val vazio =
            TextView(this)

        vazio.text =
            "Nenhum conteúdo para continuar."

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

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome do filme ou série..."

        campo.setHintTextColor(
            Color.GRAY
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
            dp(20),
            0,
            dp(20),
            0
        )

        campo.setBackgroundColor(
            Color.rgb(
                25,
                25,
                25
            )
        )

        conteudo.removeAllViews()

        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {

                setMargins(
                    dp(20),
                    dp(20),
                    dp(20),
                    dp(20)
                )
            }
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

        val termo =
            texto.trim()

        if (termo.isBlank()) {
            return
        }

        val resultado =
            filmes.filter {

                it.titulo.contains(
                    termo,
                    ignoreCase = true
                ) ||

                it.categoria.contains(
                    termo,
                    ignoreCase = true
                )
            }.sortedWith(
                compareByDescending<Filme> {
                    it.ano
                }.thenBy {
                    it.titulo
                }
            )

        mostrarListaCards(
            resultado
        )
    }

    private fun alternarFavorito(
        filme: Filme
    ) {

        // Sistema de favoritos
        // reservado para implementação futura
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

        when (
            event.keyCode
        ) {

            KeyEvent.KEYCODE_MENU -> {

                if (
                    menuLateral == null
                ) {
                    abrirMenu()
                } else {
                    fecharMenu()
                }

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                if (
                    menuLateral != null
                ) {

                    fecharMenu()

                    return true
                }

                if (
                    historico.isNotEmpty()
                ) {

                    historico.removeLast()

                    val anterior =
                        historico.lastOrNull()

                    if (
                        anterior != null
                    ) {

                        mostrarListaSeries(
                            anterior
                        )

                        return true
                    }
                }
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

        if (
            menuLateral != null
        ) {

            fecharMenu()

            return
        }

        if (
            historico.isNotEmpty()
        ) {

            historico.removeLast()

            val anterior =
                historico.lastOrNull()

            if (
                anterior != null
            ) {

                mostrarListaSeries(
                    anterior
                )

                return
            }
        }

        super.onBackPressed()
    }
}
