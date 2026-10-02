package com.wolf.iptv

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class Filme(
    val titulo: String,
    val ano: Int,
    val capa: String,
    val video: String,
    val categoria: String
)

class MainActivity : Activity() {

    private lateinit var conteudo: LinearLayout
    private lateinit var scrollVertical: ScrollView

    private var abaAtual = "FILMES"

    private val filmes = listOf(

        Filme(
            "Jumanji: Bem-Vindo à Selva",
            2017,
            "https://i.postimg.cc/k4ptjpsN/jumanji.jpg",
            "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4",
            "Aventura"
        ),

        Filme(
            "Garota Infernal",
            2009,
            "https://i.postimg.cc/yNfPHnn8/garota-infernal.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Garota%20Infernal.mp4",
            "Terror"
        ),

        Filme(
            "Como Mágica",
            2026,
            "",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/Como%20M%C3%A1gica.mp4",
            "Comédia"
        ),

        Filme(
            "17 Outra Vez",
            2009,
            "https://i.postimg.cc/FFwTzW46/17.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/17%20Outra%20Vez%20-%20Dublado%20(Series%20Zoiudo).mp4",
            "Comédia"
        ),

        Filme(
            "A Noiva Cadáver",
            2005,
            "https://i.postimg.cc/Z54FvXD1/noiva.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/A%20noiva%20Cad%C3%A1ver.mp4",
            "Animação"
        ),

        Filme(
            "Avatar",
            2009,
            "https://i.postimg.cc/7LQgchYy/avatar.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/avatar-1080p.mp4",
            "Ficção"
        ),

        Filme(
            "Deu a Louca nos Bichos",
            2010,
            "https://i.postimg.cc/0N7W1734/deu-a-louca-nos-bichos.jpg",
            "https://wolf-channel-cdn.b-cdn.net/BALA-CHANNEL/Deu%20A%20Louca%20Nos%20Bichos%20-2010-%20-%20Dublado%20(SeriesZoiudo).mp4",
            "Comédia"
        ),

        Filme(
            "Moana",
            2026,
            "https://i.postimg.cc/pdj7VwhR/moana.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/199a8bfd-7a6e-4e1a-9cbe-d5ae4881089b/playlist.m3u8",
            "Animação"
        ),

        Filme(
            "Pinóquio",
            2026,
            "https://i.postimg.cc/ryfYVWCk/IMG-20261002-044814.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/7e4da3e7-be60-47f7-ab66-bbc6c5c273d6/playlist.m3u8",
            "Animação"
        ),

        Filme(
            "Todo Mundo em Pânico",
            2026,
            "",
            "https://vz-c091a331-1f1.b-cdn.net/324210b1-aadf-4b2c-b1b5-a23d06716dcb/playlist.m3u8",
            "Comédia"
        ),

        Filme(
            "Como Treinar o Seu Dragão",
            2026,
            "https://i.postimg.cc/664hkrZ6/treinar.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/555db026-0cb8-4242-9bdd-dd8a0d165d53/playlist.m3u8",
            "Aventura"
        ),

        Filme(
            "Quarteto Fantástico: Primeiro Passo",
            2026,
            "https://i.postimg.cc/BZ2q7zms/capa-fantastico.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/37836617-2900-4725-9d3e-ed56afffbc45/playlist.m3u8",
            "Ação"
        ),

        Filme(
            "Homem-Aranha: Um Novo Dia",
            2026,
            "https://i.postimg.cc/QCctbsqF/aranha.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/145cd2d1-82bc-4e35-986c-9137d44cf91a/playlist.m3u8",
            "Ação"
        ),

        Filme(
            "Conexão Perigosa",
            2026,
            "https://i.postimg.cc/JhyRxMrH/conexao.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/78e46f67-4aec-4ba3-b4c4-7072cd6d921b/playlist.m3u8",
            "Ação"
        ),

        Filme(
            "A Odisseia",
            2026,
            "https://i.postimg.cc/K8WjhML7/odisseia.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/85bf08b5-0911-4dc8-9e28-926212aec3bd/playlist.m3u8",
            "Aventura"
        ),

        Filme(
            "Resident Evil",
            2026,
            "https://i.postimg.cc/3J7DtmC4/evil.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/5c3bb1d6-a493-4f7e-b6b2-0ec43d5fa911/playlist.m3u8",
            "Terror"
        ),

        Filme(
            "Vingança",
            2026,
            "https://i.postimg.cc/26M7q6P7/vinganca.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/10f5522b-3890-48cd-bfb7-89b2806e8269/playlist.m3u8",
            "Ação"
        ),

        Filme(
            "A Revolta",
            2026,
            "https://i.postimg.cc/7YyLx45D/revolta.jpg",
            "https://vz-c091a331-1f1.b-cdn.net/ae8d06f6-e3da-4705-9a83-313fae7114f9/playlist.m3u8",
            "Ação"
        ),

        Filme(
            "Kingsman: Agente Secreto",
            2014,
            "https://i.postimg.cc/0QDbfyzB/kings.jpg",
            "https://wolf-channel-cdn.b-cdn.net/New%20Folder/New%20Folder/BALA-CHANNEL/Kingsman%20-%20Servi%C3%A7o%20Secreto%20-%20Dublado%20(Series%20Zoiudo).mp4",
            "Ação"
        ),

        Filme(
            "Thunderbolts",
            2026,
            "https://i.postimg.cc/FzbPQdZJ/D-NQ-NP-848607-CBT107833899597-022026-O.webp",
            "https://wolf-channel-cdn.b-cdn.net/Bala%20zip/Thunderbolts.mp4",
            "Ação"
        )
    )

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

        val fundo = ImageView(this)

        fundo.scaleType = ImageView.ScaleType.CENTER_CROP

        fundo.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        carregarImagem(
            fundo,
            "https://i.postimg.cc/Ghk8PP7w/wolf.png"
        )

        val raiz = LinearLayout(this)

        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.TRANSPARENT)

        raiz.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val camada = FrameLayout(this)

        camada.addView(fundo)
        camada.addView(raiz)

        setContentView(camada)

        criarCabecalho(raiz)
        criarAbas(raiz)
        criarAreaConteudo(raiz)

        mostrarFilmes()
    }

    private fun criarCabecalho(raiz: LinearLayout) {

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(55, 25, 55, 10)

        header.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            95
        )

        val logo = TextView(this)

        logo.text = "WOLF CHANNEL"
        logo.textSize = 30f
        logo.setTextColor(Color.WHITE)
        logo.typeface = Typeface.DEFAULT_BOLD

        header.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val info = TextView(this)

        info.text = "FILMES • SÉRIES • DORAMAS"
        info.textSize = 13f
        info.setTextColor(Color.WHITE)
        info.alpha = 0.8f

        header.addView(info)

        raiz.addView(header)
    }

    private fun criarAbas(raiz: LinearLayout) {

        val scroll = HorizontalScrollView(this)

        scroll.isHorizontalScrollBarEnabled = false

        scroll.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            65
        )

        val abas = LinearLayout(this)

        abas.orientation = LinearLayout.HORIZONTAL
        abas.gravity = Gravity.CENTER_VERTICAL
        abas.setPadding(55, 0, 30, 0)

        val nomes = listOf(
            "FILMES",
            "SÉRIES",
            "DORAMAS"
        )

        for (nome in nomes) {

            val aba = TextView(this)

            aba.text = nome
            aba.textSize = 17f
            aba.setTextColor(Color.WHITE)
            aba.typeface = Typeface.DEFAULT_BOLD
            aba.gravity = Gravity.CENTER
            aba.setPadding(28, 8, 28, 8)
            aba.isFocusable = true
            aba.isClickable = true

            aba.alpha =
                if (nome == abaAtual) 1f else 0.65f

            aba.setOnFocusChangeListener { view, foco ->

                if (foco) {
                    view.scaleX = 1.08f
                    view.scaleY = 1.08f
                    view.alpha = 1f
                } else {
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.alpha =
                        if (nome == abaAtual) 1f else 0.65f
                }
            }

            aba.setOnClickListener {

                abaAtual = nome

                if (nome == "FILMES") {
                    mostrarFilmes()
                } else {
                    mostrarMensagemSemConteudo(nome)
                }

                for (i in 0 until abas.childCount) {
                    val item = abas.getChildAt(i) as TextView
                    item.alpha =
                        if (item.text.toString() == abaAtual) 1f
                        else 0.65f
                }
            }

            abas.addView(aba)
        }

        scroll.addView(abas)
        raiz.addView(scroll)
    }

    private fun criarAreaConteudo(raiz: LinearLayout) {

        scrollVertical = ScrollView(this)

        scrollVertical.isFillViewport = true
        scrollVertical.isVerticalScrollBarEnabled = false

        scrollVertical.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        conteudo = LinearLayout(this)

        conteudo.orientation = LinearLayout.VERTICAL
        conteudo.setPadding(45, 20, 45, 50)

        scrollVertical.addView(conteudo)

        raiz.addView(scrollVertical)
    }

    private fun mostrarFilmes() {

        conteudo.removeAllViews()

        criarTituloCategoria("Filmes em destaque")

        var linha = criarNovaLinha()
        var contador = 0

        for (filme in filmes) {

            linha.addView(criarCard(filme))

            contador++

            if (contador == 5) {

                adicionarLinha(linha)

                linha = criarNovaLinha()
                contador = 0
            }
        }

        if (contador > 0) {

            while (contador < 5) {

                val espaco = View(this)

                linha.addView(
                    espaco,
                    LinearLayout.LayoutParams(
                        0,
                        280,
                        1f
                    )
                )

                contador++
            }

            adicionarLinha(linha)
        }
    }

    private fun criarNovaLinha(): LinearLayout {

        val linha = LinearLayout(this)

        linha.orientation = LinearLayout.HORIZONTAL
        linha.gravity = Gravity.TOP

        return linha
    }

    private fun adicionarLinha(linha: LinearLayout) {

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300
            )
        )
    }

    private fun criarTituloCategoria(titulo: String) {

        val texto = TextView(this)

        texto.text = titulo
        texto.textSize = 22f
        texto.setTextColor(Color.WHITE)
        texto.typeface = Typeface.DEFAULT_BOLD
        texto.setPadding(8, 15, 8, 12)

        conteudo.addView(
            texto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )
    }

    private fun criarCard(filme: Filme): View {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.TOP
        card.isFocusable = true
        card.isClickable = true
        card.setPadding(6, 5, 6, 5)

        val parametros = LinearLayout.LayoutParams(
            0,
            280,
            1f
        )

        parametros.setMargins(6, 5, 6, 5)

        card.layoutParams = parametros

        val capa = ImageView(this)

        capa.scaleType = ImageView.ScaleType.CENTER_CROP
        capa.setBackgroundColor(
            Color.rgb(25, 25, 25)
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                220
            )
        )

        if (filme.capa.isNotBlank()) {

            carregarImagem(
                capa,
                filme.capa
            )
        }

        val titulo = TextView(this)

        titulo.text = filme.titulo
        titulo.textSize = 14f
        titulo.setTextColor(Color.WHITE)
        titulo.typeface = Typeface.DEFAULT_BOLD
        titulo.maxLines = 1
        titulo.ellipsize = TextUtils.TruncateAt.END

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                35
            )
        )

        val ano = TextView(this)

        ano.text = filme.ano.toString()
        ano.textSize = 12f
        ano.setTextColor(Color.LTGRAY)

        card.addView(
            ano,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                22
            )
        )

        card.setOnFocusChangeListener { view, foco ->

            if (foco) {

                view.scaleX = 1.08f
                view.scaleY = 1.08f
                view.alpha = 1f
                view.bringToFront()

            } else {

                view.scaleX = 1f
                view.scaleY = 1f
                view.alpha = 0.9f
            }
        }

        card.setOnClickListener {
            abrirFilme(filme)
        }

        return card
    }

    private fun mostrarMensagemSemConteudo(nome: String) {

        conteudo.removeAllViews()

        val caixa = LinearLayout(this)

        caixa.orientation = LinearLayout.VERTICAL
        caixa.gravity = Gravity.CENTER

        val titulo = TextView(this)

        titulo.text = nome
        titulo.textSize = 25f
        titulo.setTextColor(Color.WHITE)
        titulo.typeface = Typeface.DEFAULT_BOLD
        titulo.gravity = Gravity.CENTER

        val mensagem = TextView(this)

        mensagem.text =
            "Nenhum conteúdo cadastrado em $nome ainda."

        mensagem.textSize = 18f
        mensagem.setTextColor(Color.LTGRAY)
        mensagem.gravity = Gravity.CENTER
        mensagem.setPadding(0, 15, 0, 0)

        caixa.addView(titulo)
        caixa.addView(mensagem)

        conteudo.addView(
            caixa,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                500
            )
        )
    }

    private fun abrirFilme(filme: Filme) {

        if (filme.video.isBlank()) return

        val intent = Intent(
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

    private fun carregarImagem(
        imageView: ImageView,
        urlImagem: String
    ) {

        thread {

            try {

                val url = URL(urlImagem)

                val conexao =
                    url.openConnection() as HttpURLConnection

                conexao.connectTimeout = 10000
                conexao.readTimeout = 15000
                conexao.doInput = true

                conexao.connect()

                val bitmap: Bitmap? =
                    BitmapFactory.decodeStream(
                        conexao.inputStream
                    )

                conexao.inputStream.close()
                conexao.disconnect()

                if (bitmap != null) {

                    runOnUiThread {

                        imageView.setImageBitmap(bitmap)
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (event.action == KeyEvent.ACTION_DOWN) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco = currentFocus

                    if (foco != null && foco.isClickable) {

                        foco.performClick()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
