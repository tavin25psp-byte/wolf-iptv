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
import org.json.JSONArray
import org.json.JSONObject
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

    private val CATALOGO_URL =
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/catalogo.json"

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
    }
        private fun criarInterface() {

        raiz = FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        val fundo = ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        fundo.alpha = 0.55f

        carregarImagem(
            "https://i.postimg.cc/Ghk8PP7w/wolf.png",
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
            )
        )

        val scroll =
            ScrollView(this)

        scroll.isFocusable = false

        scroll.isFocusableInTouchMode =
            false

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

        setContentView(raiz)
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

    private fun criarBordaVermelha():
        GradientDrawable {

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

        if (url.isBlank()) return

        val salva =
            cacheCapas[url]

        if (salva != null) {

            imagem.setImageBitmap(
                salva
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
                    15000

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

            } catch (_: Exception) {
            }
        }
    }private fun carregarFilmes() {

        thread {

            var conexao: HttpURLConnection? = null

            try {

                conexao =
                    URL(CATALOGO_URL)
                        .openConnection() as HttpURLConnection

                conexao.connectTimeout = 20000
                conexao.readTimeout = 20000
                conexao.requestMethod = "GET"
                conexao.doInput = true

                conexao.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
                )

                conexao.connect()

                val codigo =
                    conexao.responseCode

                if (codigo !in 200..299) {
                    throw Exception(
                        "HTTP $codigo"
                    )
                }

                val resposta =
                    conexao.inputStream
                        .bufferedReader(
                            Charsets.UTF_8
                        )
                        .use {
                            it.readText()
                        }

                if (resposta.isBlank()) {
                    throw Exception(
                        "Catálogo vazio"
                    )
                }

                val texto =
                    resposta.trim()

                val raizJson =
                    JSONObject(texto)

                val novosFilmes =
                    ArrayList<Filme>()

                val listaFilmes =
                    raizJson.optJSONArray(
                        "filmes"
                    )

                if (listaFilmes != null) {

                    for (i in 0 until listaFilmes.length()) {

                        try {

                            val item =
                                listaFilmes.getJSONObject(i)

                            val titulo =
                                item.optString(
                                    "titulo",
                                    ""
                                )

                            val ano =
                                item.optInt(
                                    "ano",
                                    0
                                )

                            val categoria =
                                item.optString(
                                    "categoria",
                                    ""
                                )

                            val capa =
                                item.optString(
                                    "capa",
                                    ""
                                )

                            val video =
                                item.optString(
                                    "video",
                                    ""
                                )

                            if (
                                titulo.isNotBlank()
                            ) {

                                novosFilmes.add(
                                    Filme(
                                        titulo = titulo,
                                        ano = ano,
                                        categoria = categoria,
                                        capa = capa,
                                        video = video
                                    )
                                )
                            }

                        } catch (_: Exception) {}
                    }
                }

                novosFilmes.sortWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo.lowercase()
                    }
                )

                fun lerSeries(
                    array: JSONArray?
                ): ArrayList<Serie> {

                    val resultado =
                        ArrayList<Serie>()

                    if (array == null) {
                        return resultado
                    }

                    for (
                        i in 0 until array.length()
                    ) {

                        try {

                            val objeto =
                                array.getJSONObject(i)

                            val titulo =
                                objeto.optString(
                                    "titulo",
                                    ""
                                )

                            val categoria =
                                objeto.optString(
                                    "categoria",
                                    ""
                                )

                            val capa =
                                objeto.optString(
                                    "capa",
                                    ""
                                )

                            val temporadas =
                                ArrayList<Temporada>()

                            val arrayTemporadas =
                                objeto.optJSONArray(
                                    "temporadas"
                                )

                            if (
                                arrayTemporadas != null
                            ) {

                                for (
                                    t in 0 until
                                        arrayTemporadas.length()
                                ) {

                                    try {

                                        val objetoTemporada =
                                            arrayTemporadas
                                                .getJSONObject(t)

                                        val numeroTemporada =
                                            objetoTemporada
                                                .optInt(
                                                    "numero",
                                                    t + 1
                                                )

                                        val episodios =
                                            ArrayList<Episodio>()

                                        val arrayEpisodios =
                                            objetoTemporada
                                                .optJSONArray(
                                                    "episodios"
                                                )

                                        if (
                                            arrayEpisodios != null
                                        ) {

                                            for (
                                                e in 0 until
                                                    arrayEpisodios.length()
                                            ) {

                                                try {

                                                    val objetoEpisodio =
                                                        arrayEpisodios
                                                            .getJSONObject(e)

                                                    val numeroEpisodio =
                                                        objetoEpisodio
                                                            .optInt(
                                                                "numero",
                                                                e + 1
                                                            )

                                                    val tituloEpisodio =
                                                        objetoEpisodio
                                                            .optString(
                                                                "titulo",
                                                                ""
                                                            )

                                                    val videoEpisodio =
                                                        objetoEpisodio
                                                            .optString(
                                                                "video",
                                                                ""
                                                            )

                                                    episodios.add(
                                                        Episodio(
                                                            numero =
                                                                numeroEpisodio,
                                                            titulo =
                                                                tituloEpisodio,
                                                            video =
                                                                videoEpisodio
                                                        )
                                                    )

                                                } catch (_: Exception) {}
                                            }
                                        }

                                        temporadas.add(
                                            Temporada(
                                                numero =
                                                    numeroTemporada,
                                                episodios =
                                                    episodios
                                            )
                                        )

                                    } catch (_: Exception) {}
                                }
                            }

                            if (
                                titulo.isNotBlank()
                            ) {

                                resultado.add(
                                    Serie(
                                        titulo =
                                            titulo,
                                        categoria =
                                            categoria,
                                        capa =
                                            capa,
                                        temporadas =
                                            temporadas
                                    )
                                )
                            }

                        } catch (_: Exception) {}
                    }

                    return resultado
                }

                val novasSeries =
                    lerSeries(
                        raizJson.optJSONArray(
                            "series"
                        )
                    )

                val novosDoramas =
                    lerSeries(
                        raizJson.optJSONArray(
                            "doramas"
                        )
                    )

                val novosAnimes =
                    lerSeries(
                        raizJson.optJSONArray(
                            "animes"
                        )
                    )

                runOnUiThread {

                    filmes.clear()
                    filmes.addAll(
                        novosFilmes
                    )

                    series.clear()
                    series.addAll(
                        novasSeries
                    )

                    doramas.clear()
                    doramas.addAll(
                        novosDoramas
                    )

                    animes.clear()
                    animes.addAll(
                        novosAnimes
                    )

                    mostrarListaCards(
                        filmes
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Catálogo carregado: " +
                            "${filmes.size} filmes, " +
                            "${series.size} séries, " +
                            "${doramas.size} doramas e " +
                            "${animes.size} animes",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (
                erro: Exception
            ) {

                runOnUiThread {

                    filmes.clear()
                    series.clear()
                    doramas.clear()
                    animes.clear()

                    conteudo.removeAllViews()

                    val erroTexto =
                        TextView(this@MainActivity)

                    erroTexto.text =
                        "ERRO NO CATÁLOGO\n\n" +
                        "${erro.message ?: "Erro desconhecido"}"

                    erroTexto.textSize =
                        20f

                    erroTexto.setTextColor(
                        Color.WHITE
                    )

                    erroTexto.gravity =
                        Gravity.CENTER

                    erroTexto.setPadding(
                        dp(30),
                        dp(30),
                        dp(30),
                        dp(30)
                    )

                    conteudo.addView(
                        erroTexto
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao carregar catálogo",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                conexao?.disconnect()
            }
        }
    }

    private fun carregarSeries() {}private fun mostrarListaCards(
        lista: List<Filme>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum filme encontrado"

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.textSize = 20f

            vazio.gravity =
                Gravity.CENTER

            vazio.setPadding(
                dp(20),
                dp(40),
                dp(20),
                dp(40)
            )

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(100)
                )
            )

            return
        }

        lista.chunked(5).forEach { grupo ->

            val linha =
                LinearLayout(this)

            linha.orientation =
                LinearLayout.HORIZONTAL

            linha.layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )

            grupo.forEach { filme ->

                val card =
                    criarCard(filme)

                linha.addView(
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

                cardsAtuais.add(card)
            }

            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0]
                .requestFocus()
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
            Gravity.CENTER_VERTICAL

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

        titulo.maxLines = 1

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

        detalhes.setTextColor(
            Color.LTGRAY
        )

        detalhes.textSize = 12f

        informacoes.addView(
            detalhes,
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
            _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(foco)

            if (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
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

    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma série encontrada"

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
                    dp(100)
                )
            )

            return
        }

        lista.chunked(5).forEach { grupo ->

            val linha =
                LinearLayout(this)

            linha.orientation =
                LinearLayout.HORIZONTAL

            linha.layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )

            grupo.forEach { serie ->

                val card =
                    criarCardSerie(serie)

                linha.addView(
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

                cardsAtuais.add(card)
            }

            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0]
                .requestFocus()
        }
    }private fun criarCardSerie(
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
            )
        )

        val informacoes =
            LinearLayout(this)

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

        informacoes.setBackgroundColor(
            Color.argb(
                235,
                10,
                10,
                10
            )
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

        titulo.maxLines = 1

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
            "${serie.categoria} • " +
            "${serie.temporadas.size} temporada(s)"

        detalhes.setTextColor(
            Color.LTGRAY
        )

        detalhes.textSize = 12f

        informacoes.addView(
            detalhes,
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
            _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(foco)

            if (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            historicoConteudo.add {
                mostrarListaSeries(series)
            }

            mostrarTemporadas(serie)
        }

        return card
    }

    private fun mostrarTemporadas(
        serie: Serie
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        serie.temporadas
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { temporada ->

                    val card =
                        criarCardTemporada(
                            serie,
                            temporada
                        )

                    linha.addView(
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

                    cardsAtuais.add(card)
                }

                conteudo.addView(linha)
            }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0]
                .requestFocus()
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
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "Temporada ${temporada.numero}"

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 16f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.setBackgroundColor(
            Color.argb(
                235,
                10,
                10,
                10
            )
        )

        card.addView(
            titulo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                gravity = Gravity.BOTTOM
            }
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
            _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(foco)

            if (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
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
    }private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        temporada.episodios
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { episodio ->

                    val card =
                        criarCardEpisodio(
                            serie,
                            episodio
                        )

                    linha.addView(
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

                    cardsAtuais.add(card)
                }

                conteudo.addView(linha)
            }

        if (cardsAtuais.isNotEmpty()) {

            cardsAtuais[0]
                .requestFocus()
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
            )
        )

        val informacoes =
            TextView(this)

        informacoes.text =
            "EP ${episodio.numero} • " +
            episodio.titulo

        informacoes.setTextColor(
            Color.WHITE
        )

        informacoes.textSize = 14f

        informacoes.setTypeface(
            null,
            Typeface.BOLD
        )

        informacoes.gravity =
            Gravity.CENTER_VERTICAL

        informacoes.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informacoes.maxLines = 2

        informacoes.ellipsize =
            TextUtils.TruncateAt.END

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
                dp(60)
            ).apply {
                gravity = Gravity.BOTTOM
            }
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
            _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(foco)

            if (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            abrirVideo(
                "${serie.titulo} - " +
                    "EP ${episodio.numero}",
                episodio.video,
                serie.capa
            )
        }

        return card
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
    }
        private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.setTextColor(
            Color.WHITE
        )

        item.textSize = 16f

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
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
                bottomMargin = dp(4)
            }
        )

        itensMenuFoco.add(item)
    }

    private fun abrirMenu() {

        if (menuAberto) return

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

        val params =
            FrameLayout.LayoutParams(
                dp(360),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        params.gravity =
            Gravity.START or Gravity.TOP

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

        cabecalho.setPadding(
            dp(15),
            dp(8),
            dp(10),
            dp(8)
        )

        menuLateral.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val tituloMenu =
            TextView(this)

        tituloMenu.text =
            "WOLF MENU"

        tituloMenu.setTextColor(
            Color.WHITE
        )

        tituloMenu.textSize = 21f

        tituloMenu.setTypeface(
            null,
            Typeface.BOLD
        )

        tituloMenu.gravity =
            Gravity.CENTER_VERTICAL

        cabecalho.addView(
            tituloMenu,
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

        botaoFecharMenu.setTextColor(
            Color.WHITE
        )

        botaoFecharMenu.textSize = 24f

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
                dp(48)
            )
        )

        menuScroll =
            ScrollView(this)

        menuScroll.isFocusable = false

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

        menuConteudo.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(20)
        )

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
            "🔄  Atualizar catálogo"
        ) {
            fecharMenu()
            Toast.makeText(
                this,
                "Atualizando catálogo...",
                Toast.LENGTH_SHORT
            ).show()
            carregarFilmes()
        }

        adicionarItemMenu(
            "▶  Continuar assistindo"
        ) {

            fecharMenu()

            Toast.makeText(
                this,
                "Continue assistindo",
                Toast.LENGTH_SHORT
            ).show()
        }

        adicionarItemMenu(
            "★  Favoritos (${favoritos.size})"
        ) {

            fecharMenu()

            val lista =
                filmes.filter {
                    favoritos.contains(
                        it.titulo
                    )
                }

            mostrarListaCards(lista)
        }

        adicionarItemMenu(
            "⌕  Pesquisa"
        ) {
            abrirPesquisa()
        }

        adicionarTituloMenu(
            "FILMES"
        )

        adicionarItemMenu(
            "🎬  Todos os filmes (${filmes.size})"
        ) {

            fecharMenu()
            mostrarListaCards(filmes)
        }

        adicionarItemMenu(
            "🔥  Ação (${filmes.count {
                it.categoria.equals("Ação", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Ação", true)
                }
            )
        }

        adicionarItemMenu(
            "🏹  Aventura (${filmes.count {
                it.categoria.equals("Aventura", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Aventura", true)
                }
            )
        }

        adicionarItemMenu(
            "🧸  Animação (${filmes.count {
                it.categoria.equals("Animação", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Animação", true)
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${filmes.count {
                it.categoria.equals("Comédia", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Comédia", true)
                }
            )
        }

        adicionarItemMenu(
            "🎭  Drama (${filmes.count {
                it.categoria.equals("Drama", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Drama", true)
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${filmes.count {
                it.categoria.equals("Terror", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Terror", true)
                }
            )
        }

        adicionarItemMenu(
            "🚀  Ficção (${filmes.count {
                it.categoria.equals("Ficção", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Ficção", true)
                }
            )
        }

        adicionarTituloMenu(
            "SÉRIES"
        )

        adicionarItemMenu(
            "📺  Todas as séries (${series.size})"
        ) {

            fecharMenu()
            mostrarListaSeries(series)
        }

        adicionarItemMenu(
            "🔥  Ação (${series.count {
                it.categoria.equals("Ação", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals("Ação", true)
                }
            )
        }

        adicionarItemMenu(
            "🏹  Aventura (${series.count {
                it.categoria.equals("Aventura", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals("Aventura", true)
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${series.count {
                it.categoria.equals("Comédia", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals("Comédia", true)
                }
            )
        }

        adicionarItemMenu(
            "🎭  Drama (${series.count {
                it.categoria.equals("Drama", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals("Drama", true)
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${series.count {
                it.categoria.equals("Terror", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria.equals("Terror", true)
                }
            )
        }// ===============================
        // DORAMAS
        // ===============================

        adicionarTituloMenu("DORAMAS")

        adicionarItemMenu(
            "📺  Todos os Doramas (${doramas.size})"
        ) {
            fecharMenu()
            mostrarListaSeries(doramas)
        }

        adicionarItemMenu(
            "💖  Romance (${doramas.count {
                it.categoria.equals("romance", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("romance", true)
                }
            )
        }

        adicionarItemMenu(
            "🔥  Ação (${doramas.count {
                it.categoria.equals("acao", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("acao", true)
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${doramas.count {
                it.categoria.equals("comedia", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("comedia", true)
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${doramas.count {
                it.categoria.equals("terror", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("terror", true)
                }
            )
        }


        // ===============================
        // ANIME
        // ===============================

        adicionarTituloMenu("ANIME")

        adicionarItemMenu(
            "🍥  Todos os Animes (${animes.size})"
        ) {
            fecharMenu()
            mostrarListaSeries(animes)
        }

        adicionarItemMenu(
            "🔥  Ação (${animes.count {
                it.categoria.equals("acao", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("acao", true)
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${animes.count {
                it.categoria.equals("comedia", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("comedia", true)
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${animes.count {
                it.categoria.equals("terror", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("terror", true)
                }
            )
        }


        // ===============================
        // FOCO INICIAL DO MENU
        // ===============================

        botaoFecharMenu.requestFocus()

    }


    // ===============================
    // TÍTULO DO MENU
    // ===============================

    private fun adicionarTituloMenu(
        texto: String
    ) {

        val titulo = TextView(this)

        titulo.text = texto

        titulo.textSize = 16f

        titulo.setTextColor(
            Color.LTGRAY
        )

        titulo.setPadding(
            dp(18),
            dp(18),
            dp(12),
            dp(8)
        )

        titulo.isFocusable = false

        menuConteudo.addView(
            titulo
        )
    }


    // ===============================
    // FECHAR MENU
    // ===============================

    private fun fecharMenu() {

        menuAberto = false

        menuLateral.visibility =
            View.GONE

        botaoMenu.requestFocus()
    }


    // ===============================
    // MOVIMENTAÇÃO DO MENU
    // ===============================

    private fun moverMenu(
        direcao: Int
    ) {

        if (itensMenuFoco.isEmpty()) {
            return
        }

        var indice =
            itensMenuFoco.indexOfFirst {
                it.hasFocus()
            }

        if (indice < 0) {
            indice = 0
        }

        indice += direcao

        if (indice < 0) {
            indice =
                itensMenuFoco.size - 1
        }

        if (indice >= itensMenuFoco.size) {
            indice = 0
        }

        val proximo =
            itensMenuFoco[indice]

        proximo.requestFocus()

        ajustarScrollMenu(
            proximo
        )
    }


    // ===============================
    // AJUSTAR SCROLL DO MENU
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
    // MOVIMENTAÇÃO DOS CARDS
    // ===============================

    private fun moverCard(
        direcao: Int
    ) {

        if (cardsAtuais.isEmpty()) {
            return
        }

        var indice =
            cardsAtuais.indexOfFirst {
                it.hasFocus()
            }

        if (indice < 0) {
            indice = indiceCardAtual
        }

        indice += direcao

        if (indice < 0) {
            indice = 0
        }

        if (indice >= cardsAtuais.size) {
            indice =
                cardsAtuais.size - 1
        }

        indiceCardAtual = indice

        cardsAtuais[indice].requestFocus()
    }


    // ===============================
    // PESQUISA
    // ===============================

    private fun abrirPesquisa() {

        val campo =
            EditText(this)

        campo.hint =
            "Digite o nome..."

        campo.textSize =
            20f

        campo.setSingleLine(true)

        campo.isFocusable = true
        campo.isFocusableInTouchMode = true

        campo.requestFocus()

        Toast.makeText(
            this,
            "Use a busca pelo menu",
            Toast.LENGTH_SHORT
        ).show()
    }


    // ===============================
    // CONTROLE DO D-PAD
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

                    if (!menuAberto) {

                        moverCard(-1)

                        return true
                    }
                }


                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    if (!menuAberto) {

                        moverCard(1)

                        return true
                    }
                }


                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (menuAberto) {

                        moverMenu(-1)

                        return true
                    }
                }


                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (menuAberto) {

                        moverMenu(1)

                        return true
                    }
                }


                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }

}
