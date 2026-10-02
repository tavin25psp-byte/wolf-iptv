package com.wolf.iptv

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import java.net.URL

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
    private lateinit var menuConteudo: LinearLayout

    private var menuAberto = false

    private val itensMenuFoco =
        mutableListOf<View>()

    private val filmes =
        mutableListOf<Filme>()

    private val series =
        mutableListOf<Filme>()

    private val doramas =
        mutableListOf<Filme>()

    private val animes =
        mutableListOf<Filme>()

    private val favoritosSalvos =
        mutableSetOf<String>()

    private val progressoVideos =
        mutableMapOf<String, Long>()


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        criarInterface()
        carregarFilmes()
        carregarDados()
        carregarProgressos()
        prepararDados()
        mostrarTodos()
    }


    // ===============================
    // INTERFACE
    // ===============================

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
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )


        val camada = LinearLayout(this)

        camada.orientation =
            LinearLayout.VERTICAL

        camada.setBackgroundColor(
            Color.argb(
                130,
                0,
                0,
                0
            )
        )

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )


        val topo = LinearLayout(this)

        topo.gravity =
            Gravity.CENTER_VERTICAL

        topo.setPadding(
            dp(15),
            dp(8),
            dp(15),
            dp(8)
        )


        botaoMenu = TextView(this)

        botaoMenu.text =
            "☰  MENU"

        botaoMenu.textSize =
            18f

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable = true
        botaoMenu.isFocusableInTouchMode = true


        topo.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(150),
                dp(55)
            )
        )


        val logo = TextView(this)

        logo.text =
            "WOLF CHANNEL"

        logo.textSize =
            25f

        logo.setTextColor(
            Color.WHITE
        )

        logo.setTypeface(
            null,
            Typeface.BOLD
        )

        logo.gravity =
            Gravity.CENTER


        topo.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )


        camada.addView(
            topo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )


        val scrollPrincipal =
            NestedScrollView(this)

        scrollPrincipal.isFillViewport =
            true


        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.setPadding(
            dp(15),
            dp(5),
            dp(15),
            dp(30)
        )


        scrollPrincipal.addView(
            conteudo,
            NestedScrollView.LayoutParams(
                NestedScrollView.LayoutParams.MATCH_PARENT,
                NestedScrollView.LayoutParams.WRAP_CONTENT
            )
        )


        camada.addView(
            scrollPrincipal,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )


        // ===============================
        // MENU LATERAL
        // ===============================

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setPadding(
            dp(10),
            dp(15),
            dp(10),
            dp(15)
        )

        menuLateral.setBackgroundColor(
            Color.argb(
                245,
                8,
                8,
                8
            )
        )

        menuLateral.visibility =
            View.GONE


        val menuTitulo =
            TextView(this)

        menuTitulo.text =
            "WOLF CHANNEL"

        menuTitulo.textSize =
            23f

        menuTitulo.setTextColor(
            Color.WHITE
        )

        menuTitulo.setTypeface(
            null,
            Typeface.BOLD
        )

        menuTitulo.gravity =
            Gravity.CENTER


        menuLateral.addView(
            menuTitulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )


        val menuScroll =
            NestedScrollView(this)


        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL


        menuScroll.addView(
            menuConteudo,
            NestedScrollView.LayoutParams(
                NestedScrollView.LayoutParams.MATCH_PARENT,
                NestedScrollView.LayoutParams.WRAP_CONTENT
            )
        )


        menuLateral.addView(
            menuScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )


        val menuParams =
            FrameLayout.LayoutParams(
                dp(390),
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        menuParams.gravity =
            Gravity.START


        raiz.addView(
            menuLateral,
            menuParams
        )


        setContentView(raiz)
    }


    // ===============================
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
    // MOSTRAR CARDS
    // ===============================

    private fun mostrarListaCards(
        titulo: String,
        lista: List<Filme>
    ) {

        val tituloView =
            TextView(this)

        tituloView.text =
            titulo

        tituloView.textSize =
            24f

        tituloView.setTextColor(
            Color.WHITE
        )

        tituloView.setTypeface(
            null,
            Typeface.BOLD
        )

        tituloView.setPadding(
            dp(5),
            dp(15),
            dp(5),
            dp(10)
        )

        conteudo.addView(
            tituloView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )


        val horizontal =
            HorizontalScrollView(this)

        horizontal.isHorizontalScrollBarEnabled =
            false


        val grade =
            GridLayout(this)

        grade.columnCount =
            5

        grade.rowCount =
            GridLayout.UNDEFINED


        lista.forEach { filme ->

            val card =
                criarCard(filme)

            val params =
                GridLayout.LayoutParams()

            params.width =
                dp(240)

            params.height =
                dp(320)

            params.setMargins(
                dp(8),
                dp(8),
                dp(8),
                dp(15)
            )

            grade.addView(
                card,
                params
            )
        }


        horizontal.addView(
            grade,
            HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
            )
        )


        conteudo.addView(
            horizontal,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }


    // ===============================
    // CARD
    // ===============================

    private fun criarCard(
        filme: Filme
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.setPadding(
            dp(5),
            dp(5),
            dp(5),
            dp(5)
        )

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true


        val fundoNormal =
            GradientDrawable()

        fundoNormal.setColor(
            Color.argb(
                220,
                15,
                15,
                15
            )
        )

        fundoNormal.cornerRadius =
            dp(10).toFloat()


        card.background =
            fundoNormal


        val capa =
            ImageView(this)

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        capa.setBackgroundColor(
            Color.TRANSPARENT
        )

        carregarImagem(
            capa,
            filme.capa
        )


        card.addView(
            capa,
            LinearLayout.LayoutParams(
                dp(230),
                dp(255)
            )
        )


        val titulo =
            TextView(this)

        titulo.text =
            filme.titulo

        titulo.textSize =
            15f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.maxLines =
            2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        titulo.setPadding(
            dp(5),
            dp(4),
            dp(5),
            dp(0)
        )


        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )


        val ano =
            TextView(this)

        ano.text =
            filme.ano.toString()

        ano.textSize =
            13f

        ano.setTextColor(
            Color.LTGRAY
        )

        ano.gravity =
            Gravity.CENTER


        card.addView(
            ano,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )


        card.setOnClickListener {

            if (filme.video.isBlank()) {

                Toast.makeText(
                    this,
                    "Vídeo ainda não disponível.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                abrirVideo(filme)
            }
        }


        card.setOnFocusChangeListener {
                view,
                ganhouFoco ->

            aplicarFocoVermelho(
                view,
                ganhouFoco
            )
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

                val conexao =
                    URL(url).openConnection()

                conexao.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        conexao.getInputStream()
                    )


                runOnUiThread {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {

                runOnUiThread {

                    imageView.setImageResource(
                        android.R.drawable.ic_menu_report_image
                    )
                }
            }

        }.start()
    }


    // ===============================
    // ABRIR VÍDEO
    // ===============================

    private fun abrirVideo(
        filme: Filme
    ) {

        if (filme.video.isBlank()) {
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

        startActivity(intent)
    }


    // ===============================
    // MENU
    // ===============================

    private fun abrirMenu() {

        menuAberto = true

        menuLateral.visibility =
            View.VISIBLE

        construirMenuDinamico()

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }


    private fun fecharMenu() {

        menuAberto = false

        menuLateral.visibility =
            View.GONE

        botaoMenu.requestFocus()
    }


    private fun construirMenuDinamico() {

        menuConteudo.removeAllViews()

        itensMenuFoco.clear()


        // ===============================
        // FECHAR
        // ===============================

        adicionarItemMenu(
            "✕  Fechar"
        ) {

            fecharMenu()
        }


        adicionarSeparadorPremium()


        // ===============================
        // PRINCIPAL
        // ===============================

        adicionarItemMenu(
            "⌂  Início"
        ) {

            mostrarTodos()
            fecharMenu()
        }


        adicionarItemMenu(
            "★  Favoritos (${favoritosSalvos.size})"
        ) {

            mostrarFavoritos()
            fecharMenu()
        }


        adicionarItemMenu(
            "▶  Continue assistindo"
        ) {

            mostrarContinuar()
            fecharMenu()
        }


        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {

            abrirPesquisa()
            fecharMenu()
        }


        adicionarSeparadorPremium()


        // ===============================
        // FILMES
        // ===============================

        adicionarItemMenu(
            "🎬  Filmes (${filmes.size})"
        ) {

            mostrarTodos()
            fecharMenu()
        }


        adicionarCategoria(
            "Ação",
            filmes.count {
                it.categoria == "Ação"
            }
        ) {

            mostrarCategoria(
                "Filmes • Ação",
                "Ação"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Aventura",
            filmes.count {
                it.categoria == "Aventura"
            }
        ) {

            mostrarCategoria(
                "Filmes • Aventura",
                "Aventura"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Comédia",
            filmes.count {
                it.categoria == "Comédia"
            }
        ) {

            mostrarCategoria(
                "Filmes • Comédia",
                "Comédia"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Terror",
            filmes.count {
                it.categoria == "Terror"
            }
        ) {

            mostrarCategoria(
                "Filmes • Terror",
                "Terror"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Animação",
            filmes.count {
                it.categoria == "Animação"
            }
        ) {

            mostrarCategoria(
                "Filmes • Animação",
                "Animação"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Ficção",
            filmes.count {
                it.categoria == "Ficção"
            }
        ) {

            mostrarCategoria(
                "Filmes • Ficção",
                "Ficção"
            )

            fecharMenu()
        }


        adicionarSeparadorPremium()


        // ===============================
        // SÉRIES
        // ===============================

        adicionarItemMenu(
            "📺  Séries (${series.size})"
        ) {

            mostrarListaGenerica(
                "Séries",
                series
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Ação",
            series.count {
                it.categoria == "Ação"
            }
        ) {

            mostrarCategoriaLista(
                "Séries • Ação",
                series,
                "Ação"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Comédia",
            series.count {
                it.categoria == "Comédia"
            }
        ) {

            mostrarCategoriaLista(
                "Séries • Comédia",
                series,
                "Comédia"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Drama",
            series.count {
                it.categoria == "Drama"
            }
        ) {

            mostrarCategoriaLista(
                "Séries • Drama",
                series,
                "Drama"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Romance",
            series.count {
                it.categoria == "Romance"
            }
        ) {

            mostrarCategoriaLista(
                "Séries • Romance",
                series,
                "Romance"
            )

            fecharMenu()
        }


        adicionarSeparadorPremium()


        // ===============================
        // DORAMAS
        // ===============================

        adicionarItemMenu(
            "🎎  Doramas (${doramas.size})"
        ) {

            mostrarListaGenerica(
                "Doramas",
                doramas
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Ação",
            doramas.count {
                it.categoria == "Ação"
            }
        ) {

            mostrarCategoriaLista(
                "Doramas • Ação",
                doramas,
                "Ação"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Romance",
            doramas.count {
                it.categoria == "Romance"
            }
        ) {

            mostrarCategoriaLista(
                "Doramas • Romance",
                doramas,
                "Romance"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Drama",
            doramas.count {
                it.categoria == "Drama"
            }
        ) {

            mostrarCategoriaLista(
                "Doramas • Drama",
                doramas,
                "Drama"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Comédia",
            doramas.count {
                it.categoria == "Comédia"
            }
        ) {

            mostrarCategoriaLista(
                "Doramas • Comédia",
                doramas,
                "Comédia"
            )

            fecharMenu()
        }


        adicionarSeparadorPremium()


        // ===============================
        // ANIME
        // ===============================

        adicionarItemMenu(
            "⚔  Anime (${animes.size})"
        ) {

            mostrarListaGenerica(
                "Anime",
                animes
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Ação",
            animes.count {
                it.categoria == "Ação"
            }
        ) {

            mostrarCategoriaLista(
                "Anime • Ação",
                animes,
                "Ação"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Aventura",
            animes.count {
                it.categoria == "Aventura"
            }
        ) {

            mostrarCategoriaLista(
                "Anime • Aventura",
                animes,
                "Aventura"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Fantasia",
            animes.count {
                it.categoria == "Fantasia"
            }
        ) {

            mostrarCategoriaLista(
                "Anime • Fantasia",
                animes,
                "Fantasia"
            )

            fecharMenu()
        }


        adicionarCategoria(
            "Comédia",
            animes.count {
                it.categoria == "Comédia"
            }
        ) {

            mostrarCategoriaLista(
                "Anime • Comédia",
                animes,
                "Comédia"
            )

            fecharMenu()
        }
    }


    // ===============================
    // SEPARADOR
    // ===============================

    private fun adicionarSeparadorPremium() {

        val linha =
            View(this)

        linha.setBackgroundColor(
            Color.DKGRAY
        )


        menuConteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {

                setMargins(
                    dp(5),
                    dp(8),
                    dp(5),
                    dp(8)
                )
            }
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
            dp(18),
            0,
            dp(10),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true


        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.TRANSPARENT
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        item.background =
            fundo


        item.setOnClickListener {
            acao()
        }


        item.setOnFocusChangeListener {
                view,
                ganhouFoco ->

            aplicarFocoVermelho(
                view,
                ganhouFoco
            )
        }


        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {

                setMargins(
                    dp(5),
                    dp(2),
                    dp(5),
                    dp(2)
                )
            }
        )


        itensMenuFoco.add(item)
    }


    // ===============================
    // CATEGORIA DO MENU
    // ===============================

    private fun adicionarCategoria(
        nome: String,
        quantidade: Int,
        acao: () -> Unit
    ) {

        adicionarItemMenu(
            "   └ $nome ($quantidade)",
            acao
        )
    }    // ===============================
    // LISTAS
    // ===============================

    private fun mostrarTodos() {

        conteudo.removeAllViews()

        mostrarListaCards(
            "🎬 Filmes",
            filmes
        )

        if (series.isNotEmpty()) {
            mostrarListaCards(
                "📺 Séries",
                series
            )
        }

        if (doramas.isNotEmpty()) {
            mostrarListaCards(
                "🎎 Doramas",
                doramas
            )
        }

        if (animes.isNotEmpty()) {
            mostrarListaCards(
                "⚔ Anime",
                animes
            )
        }

        botaoMenu.requestFocus()
    }


    private fun mostrarCategoria(
        titulo: String,
        categoria: String
    ) {

        val lista =
            filmes.filter {
                it.categoria == categoria
            }

        mostrarListaGenerica(
            titulo,
            lista
        )
    }


    private fun mostrarListaGenerica(
        titulo: String,
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        if (lista.isEmpty()) {

            mostrarMensagem(
                "$titulo\n\nNenhum conteúdo disponível."
            )

            return
        }


        mostrarListaCards(
            titulo,
            lista
        )
    }


    private fun mostrarCategoriaLista(
        titulo: String,
        lista: List<Filme>,
        categoria: String
    ) {

        val filtrada =
            lista.filter {
                it.categoria == categoria
            }

        mostrarListaGenerica(
            titulo,
            filtrada
        )
    }


    // ===============================
    // FAVORITOS
    // ===============================

    private fun mostrarFavoritos() {

        conteudo.removeAllViews()


        val lista =
            filmes.filter {
                favoritosSalvos.contains(
                    it.titulo
                )
            }


        if (lista.isEmpty()) {

            mostrarMensagem(
                "★ Favoritos\n\n" +
                "Você ainda não adicionou filmes aos favoritos."
            )

            return
        }


        mostrarListaCards(
            "★ Favoritos",
            lista
        )
    }


    // ===============================
    // CONTINUE ASSISTINDO
    // ===============================

    private fun mostrarContinuar() {

        conteudo.removeAllViews()


        val lista =
            filmes.filter {
                progressoVideos.containsKey(
                    it.titulo
                )
            }


        if (lista.isEmpty()) {

            mostrarMensagem(
                "▶ Continue assistindo\n\n" +
                "Nenhum vídeo em andamento."
            )

            return
        }


        mostrarListaCards(
            "▶ Continue assistindo",
            lista
        )
    }


    // ===============================
    // PESQUISA
    // ===============================

    private fun abrirPesquisa() {

        conteudo.removeAllViews()


        val titulo =
            TextView(this)

        titulo.text =
            "⌕ Pesquisa"

        titulo.textSize =
            26f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.setPadding(
            dp(10),
            dp(20),
            dp(10),
            dp(10)
        )


        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(65)
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

        campo.singleLine =
            true

        campo.isFocusable =
            true

        campo.isFocusableInTouchMode =
            true


        campo.setBackgroundColor(
            Color.argb(
                180,
                30,
                30,
                30
            )
        )


        conteudo.addView(
            campo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {

                setMargins(
                    dp(10),
                    dp(5),
                    dp(10),
                    dp(15)
                )
            }
        )


        val botao =
            TextView(this)

        botao.text =
            "PESQUISAR"

        botao.textSize =
            17f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true


        val fundo =
            GradientDrawable()

        fundo.setColor(
            Color.rgb(
                150,
                0,
                0
            )
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        botao.background =
            fundo


        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(
                dp(180),
                dp(50)
            ).apply {

                gravity =
                    Gravity.CENTER_HORIZONTAL
            }
        )


        botao.setOnClickListener {

            pesquisar(
                campo.text.toString()
            )
        }


        campo.setOnEditorActionListener {
                _,
                _,
                _ ->

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

        val termo =
            texto.trim()


        if (termo.isEmpty()) {

            Toast.makeText(
                this,
                "Digite algo para pesquisar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val resultados =
            filmes.filter {

                it.titulo.contains(
                    termo,
                    ignoreCase = true
                )
            }


        conteudo.removeAllViews()


        if (resultados.isEmpty()) {

            mostrarMensagem(
                "Pesquisa\n\n" +
                "Nenhum resultado encontrado para:\n" +
                "\"$termo\""
            )

            return
        }


        mostrarListaCards(
            "Resultados para \"$termo\"",
            resultados
        )
    }


    // ===============================
    // MENSAGEM
    // ===============================

    private fun mostrarMensagem(
        mensagem: String
    ) {

        val texto =
            TextView(this)

        texto.text =
            mensagem

        texto.textSize =
            20f

        texto.setTextColor(
            Color.WHITE
        )

        texto.gravity =
            Gravity.CENTER

        texto.setPadding(
            dp(30),
            dp(50),
            dp(30),
            dp(50)
        )


        conteudo.addView(
            texto,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            )
        )
    }


    // ===============================
    // FOCO VERMELHO
    // ===============================

    private fun aplicarFocoVermelho(
        view: View,
        ganhouFoco: Boolean
    ) {

        val fundo =
            GradientDrawable()


        if (ganhouFoco) {

            fundo.setColor(
                Color.argb(
                    235,
                    30,
                    30,
                    30
                )
            )

            fundo.setStroke(
                dp(3),
                Color.RED
            )

            fundo.cornerRadius =
                dp(10).toFloat()

            view.scaleX =
                1.03f

            view.scaleY =
                1.03f

        } else {

            fundo.setColor(
                Color.argb(
                    220,
                    15,
                    15,
                    15
                )
            )

            fundo.cornerRadius =
                dp(10).toFloat()

            view.scaleX =
                1f

            view.scaleY =
                1f
        }


        view.background =
            fundo
    }


    // ===============================
    // D-PAD
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

                    if (menuAberto) {
                        fecharMenu()
                    } else {
                        abrirMenu()
                    }

                    return true
                }


                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (
                        !menuAberto &&
                        event.repeatCount == 0
                    ) {

                        val foco =
                            currentFocus

                        if (
                            foco == botaoMenu
                        ) {

                            abrirMenu()

                            return true
                        }
                    }
                }


                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco =
                        currentFocus

                    if (
                        menuAberto &&
                        foco is TextView
                    ) {

                        foco.performClick()

                        return true
                    }
                }


                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                    } else {

                        finish()
                    }

                    return true
                }
            }
        }


        return super.dispatchKeyEvent(
            event
        )
    }


    // ===============================
    // DADOS
    // ===============================

    private fun prepararDados() {

        filmes.sortWith(
            compareByDescending<Filme> {
                it.ano
            }.thenBy {
                it.titulo
            }
        )
    }


    private fun carregarDados() {

        val preferencias =
            getSharedPreferences(
                "wolf_dados",
                MODE_PRIVATE
            )


        val favoritos =
            preferencias.getStringSet(
                "favoritos",
                emptySet()
            )


        favoritosSalvos.clear()

        if (favoritos != null) {

            favoritosSalvos.addAll(
                favoritos
            )
        }
    }


    private fun salvarDados() {

        getSharedPreferences(
            "wolf_dados",
            MODE_PRIVATE
        )
            .edit()
            .putStringSet(
                "favoritos",
                favoritosSalvos
            )
            .apply()
    }


    // ===============================
    // PROGRESSO
    // ===============================

    private fun carregarProgressos() {

        val preferencias =
            getSharedPreferences(
                "wolf_progressos",
                MODE_PRIVATE
            )


        progressoVideos.clear()


        filmes.forEach { filme ->

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


    private fun salvarProgresso(
        titulo: String,
        posicao: Long
    ) {

        progressoVideos[
            titulo
        ] = posicao


        getSharedPreferences(
            "wolf_progressos",
            MODE_PRIVATE
        )
            .edit()
            .putLong(
                titulo,
                posicao
            )
            .apply()
    }


    // ===============================
    // UTILITÁRIO
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
    // VOLTANDO PARA A TELA
    // ===============================

    override fun onResume() {

        super.onResume()

        if (
            ::botaoMenu.isInitialized &&
            !menuAberto
        ) {

            botaoMenu.requestFocus()
        }
    }


    override fun onDestroy() {

        salvarDados()

        super.onDestroy()
    }
}
