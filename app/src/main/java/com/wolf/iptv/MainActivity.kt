package com.wolf.iptv

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    data class Filme(
        val titulo: String,
        val ano: Int,
        val categoria: String,
        val capa: String,
        val video: String
    )

    private val filmes = listOf(

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
            "Jumanji: Bem-Vindo à Selva",
            2017,
            "Aventura",
            "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
        ),

        Filme(
            "Deu a Louca nos Bichos",
            2010,
            "Comédia",
            "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4"
        ),

        Filme(
            "A Noiva Cadáver",
            2005,
            "Animação",
            "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4"
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
            "Kingsman: Agente Secreto",
            2014,
            "Ação",
            "https://i.postimg.cc/0QDbfyzB/kings.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4"
        )
    )

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout

    private var menuAberto = false

    private val branco = Color.WHITE

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
    }

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

        val escuro = View(this)

        escuro.setBackgroundColor(
            Color.argb(115, 0, 0, 0)
        )

        raiz.addView(
            escuro,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val principal = LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        principal.setPadding(
            30,
            18,
            30,
            15
        )

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val topo = LinearLayout(this)

        topo.orientation =
            LinearLayout.HORIZONTAL

        topo.gravity =
            Gravity.CENTER_VERTICAL

        principal.addView(
            topo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            )
        )

        val titulo = TextView(this)

        titulo.text = "WOLF CHANNEL"
        titulo.textSize = 27f
        titulo.setTextColor(branco)
        titulo.typeface =
            Typeface.DEFAULT_BOLD

        topo.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val filmesBtn =
            criarBotaoTopo("FILMES")

        val seriesBtn =
            criarBotaoTopo("SÉRIES")

        val doramasBtn =
            criarBotaoTopo("DORAMAS")

        topo.addView(filmesBtn)
        topo.addView(seriesBtn)
        topo.addView(doramasBtn)

        val menuBtn = TextView(this)

        menuBtn.text = "☰"
        menuBtn.textSize = 30f
        menuBtn.gravity = Gravity.CENTER
        menuBtn.setTextColor(branco)
        menuBtn.isFocusable = true
        menuBtn.isClickable = true

        menuBtn.setOnClickListener {
            abrirMenu()
        }

        aplicarFoco(menuBtn)

        topo.addView(
            menuBtn,
            LinearLayout.LayoutParams(
                75,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        val scroll =
            ScrollView(this)

        scroll.isFillViewport = true

        scroll.addView(conteudo)

        principal.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)

        mostrarFilmes(filmes)

        filmesBtn.requestFocus()
    }

    private fun criarBotaoTopo(
        texto: String
    ): TextView {

        val botao = TextView(this)

        botao.text = texto
        botao.textSize = 15f
        botao.gravity = Gravity.CENTER
        botao.setTextColor(branco)
        botao.typeface =
            Typeface.DEFAULT_BOLD

        botao.isFocusable = true
        botao.isClickable = true

        botao.setPadding(
            12,
            0,
            12,
            0
        )

        botao.setOnClickListener {

            when (texto) {

                "FILMES" -> {
                    mostrarFilmes(filmes)
                }

                "SÉRIES" -> {
                    mostrarMensagem(
                        "SÉRIES",
                        "Nenhuma série adicionada ainda."
                    )
                }

                "DORAMAS" -> {
                    mostrarMensagem(
                        "DORAMAS",
                        "Nenhum dorama adicionado ainda."
                    )
                }
            }
        }

        aplicarFoco(botao)

        return botao
    }

    private fun mostrarFilmes(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        val titulo = TextView(this)

        titulo.text = "FILMES"
        titulo.textSize = 21f
        titulo.setTextColor(branco)
        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setPadding(
            5,
            10,
            5,
            10
        )

        conteudo.addView(titulo)

        val ordenados =
            lista.sortedByDescending {
                it.ano
            }

        var linha: LinearLayout? = null

        ordenados.forEachIndexed {
            index,
            filme ->

            if (index % 5 == 0) {

                linha =
                    LinearLayout(this)

                linha!!.orientation =
                    LinearLayout.HORIZONTAL

                linha!!.gravity =
                    Gravity.TOP

                conteudo.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        260
                    )
                )
            }

            val card =
                criarCard(filme)

            linha?.addView(card)
        }
    }

    private fun criarCard(
        filme: Filme
    ): FrameLayout {

        val card =
            FrameLayout(this)

        card.isFocusable = true
        card.isClickable = true

        val params =
            LinearLayout.LayoutParams(
                0,
                245,
                1f
            )

        params.setMargins(
            7,
            7,
            7,
            7
        )

        card.layoutParams = params

        val fundoCard =
            GradientDrawable()

        fundoCard.setColor(
            Color.argb(
                205,
                15,
                15,
                15
            )
        )

        fundoCard.cornerRadius = 12f

        card.background =
            fundoCard

        val capa =
            ImageView(this)

        /*
         * FIT_CENTER mantém a capa inteira.
         * Não corta no celular nem na TV.
         */

        capa.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            capa,
            filme.capa
        )

        card.addView(
            capa,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val gradiente =
            View(this)

        val overlay =
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(
                        225,
                        0,
                        0,
                        0
                    )
                )
            )

        gradiente.background =
            overlay

        card.addView(
            gradiente,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.gravity =
            Gravity.BOTTOM

        info.setPadding(
            8,
            8,
            8,
            8
        )

        val nome =
            TextView(this)

        nome.text =
            filme.titulo

        nome.textSize = 13f

        nome.setTextColor(
            Color.WHITE
        )

        nome.typeface =
            Typeface.DEFAULT_BOLD

        nome.maxLines = 2

        val ano =
            TextView(this)

        ano.text =
            "${filme.ano} • ${filme.categoria}"

        ano.textSize = 10f

        ano.setTextColor(
            Color.LTGRAY
        )

        info.addView(nome)
        info.addView(ano)

        card.addView(
            info,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        aplicarFoco(card)

        card.setOnClickListener {
            abrirVideo(filme)
        }

        return card
    }

    private fun abrirVideo(
        filme: Filme
    ) {

        val intent =
            android.content.Intent(
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

    private fun aplicarFoco(
        view: View
    ) {

        view.setOnFocusChangeListener {
            v,
            ganhouFoco ->

            if (ganhouFoco) {

                v.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(120)
                    .start()

                v.elevation = 20f

            } else {

                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()

                v.elevation = 0f
            }
        }
    }    private fun abrirMenu() {

        if (menuAberto) return

        menuAberto = true

        val fundoMenu = View(this)

        fundoMenu.setBackgroundColor(
            Color.argb(
                160,
                0,
                0,
                0
            )
        )

        fundoMenu.isFocusable = true

        raiz.addView(
            fundoMenu,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val painel =
            LinearLayout(this)

        painel.orientation =
            LinearLayout.VERTICAL

        painel.setPadding(
            25,
            25,
            25,
            25
        )

        val painelBg =
            GradientDrawable()

        painelBg.setColor(
            Color.rgb(
                12,
                12,
                12
            )
        )

        painelBg.cornerRadius = 18f

        painel.background =
            painelBg

        painel.isFocusable = true

        val painelParams =
            FrameLayout.LayoutParams(
                430,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        painelParams.gravity =
            Gravity.END

        painelParams.setMargins(
            0,
            25,
            25,
            25
        )

        raiz.addView(
            painel,
            painelParams
        )

        val titulo =
            TextView(this)

        titulo.text = "MENU"
        titulo.textSize = 24f
        titulo.setTextColor(
            Color.WHITE
        )

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            10,
            0,
            10,
            20
        )

        painel.addView(titulo)

        adicionarItemMenu(
            painel,
            "⭐  Favoritos"
        ) {

            fecharMenu()

            mostrarMensagem(
                "FAVORITOS",
                "Nenhum favorito adicionado ainda."
            )
        }

        adicionarItemMenu(
            painel,
            "▶  Continue assistindo"
        ) {

            fecharMenu()

            mostrarMensagem(
                "CONTINUE ASSISTINDO",
                "Nenhum vídeo em andamento."
            )
        }

        adicionarSeparador(
            painel
        )

        adicionarCategoria(
            painel,
            "🎬 FILMES",
            listOf(
                "Ação",
                "Aventura",
                "Comédia",
                "Terror",
                "Animação",
                "Ficção"
            )
        )

        adicionarCategoria(
            painel,
            "📺 SÉRIES",
            listOf(
                "Ação",
                "Comédia",
                "Drama",
                "Romance"
            )
        )

        adicionarCategoria(
            painel,
            "🇰🇷 DORAMAS",
            listOf(
                "Ação",
                "Romance",
                "Drama",
                "Comédia"
            )
        )

        adicionarCategoria(
            painel,
            "🍥 ANIME",
            listOf(
                "Ação",
                "Aventura",
                "Fantasia",
                "Comédia"
            )
        )

        val fechar =
            criarItemMenu(
                "✕  Fechar"
            )

        fechar.setOnClickListener {
            fecharMenu()
        }

        painel.addView(
            fechar
        )

        fechar.requestFocus()
    }

    private fun adicionarCategoria(
        painel: LinearLayout,
        titulo: String,
        categorias: List<String>
    ) {

        val cabecalho =
            TextView(this)

        cabecalho.text =
            titulo

        cabecalho.textSize = 17f

        cabecalho.setTextColor(
            Color.WHITE
        )

        cabecalho.typeface =
            Typeface.DEFAULT_BOLD

        cabecalho.setPadding(
            10,
            12,
            10,
            8
        )

        painel.addView(
            cabecalho
        )

        categorias.forEach { categoria ->

            adicionarItemMenu(
                painel,
                "    • $categoria"
            ) {

                fecharMenu()

                if (
                    titulo == "🎬 FILMES"
                ) {

                    val filtrados =
                        filmes.filter {

                            it.categoria.equals(
                                categoria,
                                ignoreCase = true
                            )
                        }

                    mostrarFilmes(
                        filtrados
                    )

                } else {

                    mostrarMensagem(
                        categoria,
                        "Nenhum conteúdo adicionado ainda."
                    )
                }
            }
        }
    }

    private fun adicionarItemMenu(
        painel: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            criarItemMenu(texto)

        item.setOnClickListener {
            acao()
        }

        painel.addView(item)
    }

    private fun criarItemMenu(
        texto: String
    ): TextView {

        val item =
            TextView(this)

        item.text = texto
        item.textSize = 16f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            15,
            0,
            15,
            0
        )

        item.isFocusable = true
        item.isClickable = true

        aplicarFoco(item)

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48
            )

        params.setMargins(
            0,
            3,
            0,
            3
        )

        item.layoutParams =
            params

        return item
    }

    private fun adicionarSeparador(
        painel: LinearLayout
    ) {

        val linha = View(this)

        linha.setBackgroundColor(
            Color.rgb(
                55,
                55,
                55
            )
        )

        painel.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        )
    }

    private fun fecharMenu() {

        if (!menuAberto) return

        menuAberto = false

        while (raiz.childCount > 2) {

            raiz.removeViewAt(
                raiz.childCount - 1
            )
        }
    }

    private fun mostrarMensagem(
        tituloTexto: String,
        mensagem: String
    ) {

        conteudo.removeAllViews()

        val titulo =
            TextView(this)

        titulo.text =
            tituloTexto

        titulo.textSize = 22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.typeface =
            Typeface.DEFAULT_BOLD

        titulo.setPadding(
            10,
            20,
            10,
            15
        )

        conteudo.addView(
            titulo
        )

        val texto =
            TextView(this)

        texto.text =
            mensagem

        texto.textSize = 16f

        texto.setTextColor(
            Color.LTGRAY
        )

        texto.gravity =
            Gravity.CENTER

        texto.setPadding(
            20,
            40,
            20,
            40
        )

        conteudo.addView(
            texto,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            )
        )
    }

    private fun carregarImagem(
        imageView: ImageView,
        url: String
    ) {

        thread {

            try {

                val conexao =
                    URL(url).openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    10000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeStream(
                            conexao.inputStream
                        )

                conexao.disconnect()

                runOnUiThread {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {

            }
        }
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            if (
                event.keyCode ==
                KeyEvent.KEYCODE_BACK
            ) {

                if (menuAberto) {

                    fecharMenu()

                    return true
                }
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
        }

        return super.dispatchKeyEvent(
            event
        )
    }
}
