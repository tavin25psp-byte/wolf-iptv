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
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/refs/heads/main/catalogo.json"

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

        try {

            val conexao =
                URL(CATALOGO_URL)
                    .openConnection() as HttpURLConnection

            conexao.connectTimeout = 15000
            conexao.readTimeout = 15000
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

            conexao.disconnect()

            val texto =
                resposta.trim()

            if (texto.isBlank()) {
                throw Exception(
                    "Catálogo vazio"
                )
            }

            val raizJson =
                JSONObject(texto)

            val novaListaFilmes =
                mutableListOf<Filme>()

            val jsonFilmes =
                raizJson.optJSONArray(
                    "filmes"
                ) ?: JSONArray()

            for (
                i in 0 until
                jsonFilmes.length()
            ) {

                val item =
                    jsonFilmes
                        .getJSONObject(i)

                val titulo =
                    item.optString(
                        "titulo"
                    ).trim()

                val ano =
                    item.optInt(
                        "ano",
                        0
                    )

                val categoria =
                    item.optString(
                        "categoria"
                    ).trim()

                val capa =
                    item.optString(
                        "capa"
                    ).trim()

                val video =
                    item.optString(
                        "video"
                    ).trim()

                if (
                    titulo.isNotBlank() &&
                    capa.isNotBlank()
                ) {

                    novaListaFilmes.add(
                        Filme(
                            titulo = titulo,
                            ano = ano,
                            categoria = categoria,
                            capa = capa,
                            video = video
                        )
                    )
                }
            }

            novaListaFilmes.sortWith(
                compareByDescending<Filme> {
                    it.ano
                }.thenBy {
                    it.titulo
                }
            )

            fun lerSeries(
                array: JSONArray
            ): MutableList<Serie> {

                val resultado =
                    mutableListOf<Serie>()

                for (
                    i in 0 until
                    array.length()
                ) {

                    val item =
                        array.getJSONObject(i)

                    val titulo =
                        item.optString(
                            "titulo"
                        ).trim()

                    val categoria =
                        item.optString(
                            "categoria"
                        ).trim()

                    val capa =
                        item.optString(
                            "capa"
                        ).trim()

                    val temporadas =
                        mutableListOf<Temporada>()

                    val jsonTemporadas =
                        item.optJSONArray(
                            "temporadas"
                        ) ?: JSONArray()

                    for (
                        t in 0 until
                        jsonTemporadas.length()
                    ) {

                        val temporadaJson =
                            jsonTemporadas
                                .getJSONObject(t)

                        val numeroTemporada =
                            temporadaJson.optInt(
                                "numero",
                                t + 1
                            )

                        val episodios =
                            mutableListOf<Episodio>()

                        val jsonEpisodios =
                            temporadaJson
                                .optJSONArray(
                                    "episodios"
                                ) ?: JSONArray()

                        for (
                            e in 0 until
                            jsonEpisodios.length()
                        ) {

                            val episodioJson =
                                jsonEpisodios
                                    .getJSONObject(e)

                            val numeroEpisodio =
                                episodioJson.optInt(
                                    "numero",
                                    e + 1
                                )

                            val tituloEpisodio =
                                episodioJson
                                    .optString(
                                        "titulo"
                                    )
                                    .trim()

                            val videoEpisodio =
                                episodioJson
                                    .optString(
                                        "video"
                                    )
                                    .trim()

                            if (
                                tituloEpisodio
                                    .isNotBlank()
                            ) {

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
                    }

                    if (
                        titulo.isNotBlank() &&
                        capa.isNotBlank()
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
                }

                return resultado
            }

            val novasSeries =
                lerSeries(
                    raizJson.optJSONArray(
                        "series"
                    ) ?: JSONArray()
                )

            val novosDoramas =
                lerSeries(
                    raizJson.optJSONArray(
                        "doramas"
                    ) ?: JSONArray()
                )

            val novosAnimes =
                lerSeries(
                    raizJson.optJSONArray(
                        "animes"
                    ) ?: JSONArray()
                )

            runOnUiThread {

                filmes.clear()
                filmes.addAll(
                    novaListaFilmes
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
                    this,
                    "${filmes.size} filmes • " +
                        "${series.size} séries carregados",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {

            runOnUiThread {

                filmes.clear()
                series.clear()
                doramas.clear()
                animes.clear()

                mostrarListaCards(
                    filmes
                )

                Toast.makeText(
                    this,
                    "Erro ao carregar catálogo: " +
                        e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

private fun carregarSeries() {

    // Séries, doramas e animes
    // são carregados junto com o
    // catalogo.json.
}private fun mostrarListaCards(
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
}private fun criarCardTemporada(
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
}

private fun mostrarEpisodios(
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
            it.categoria == "Ação"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Ação"
            }
        )
    }

    adicionarItemMenu(
        "🏹  Aventura (${filmes.count {
            it.categoria == "Aventura"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Aventura"
            }
        )
    }

    adicionarItemMenu(
        "🧸  Animação (${filmes.count {
            it.categoria == "Animação"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Animação"
            }
        )
    }

    adicionarItemMenu(
        "😂  Comédia (${filmes.count {
            it.categoria == "Comédia"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Comédia"
            }
        )
    }

    adicionarItemMenu(
        "🎭  Drama (${filmes.count {
            it.categoria == "Drama"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Drama"
            }
        )
    }

    adicionarItemMenu(
        "👻  Terror (${filmes.count {
            it.categoria == "Terror"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Terror"
            }
        )
    }

    adicionarItemMenu(
        "🚀  Ficção (${filmes.count {
            it.categoria == "Ficção"
        }})"
    ) {

        fecharMenu()

        mostrarListaCards(
            filmes.filter {
                it.categoria == "Ficção"
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
            it.categoria == "Ação"
        }})"
    ) {

        fecharMenu()

        mostrarListaSeries(
            series.filter {
                it.categoria == "Ação"
            }
        )
    }

    adicionarItemMenu(
        "🏹  Aventura (${series.count {
            it.categoria == "Aventura"
        }})"
    ) {

        fecharMenu()

        mostrarListaSeries(
            series.filter {
                it.categoria == "Aventura"
            }
        )
    }

    adicionarItemMenu(
        "😂  Comédia (${series.count {
            it.categoria == "Comédia"
        }})"
    ) {

        fecharMenu()

        mostrarListaSeries(
            series.filter {
                it.categoria == "Comédia"
            }
        )
    }

    adicionarItemMenu(
        "🎭  Drama (${series.count {
            it.categoria == "Drama"
        }})"
    ) {

        fecharMenu()

        mostrarListaSeries(
            series.filter {
                it.categoria == "Drama"
            }
        )
    }

    adicionarItemMenu(
        "👻  Terror (${series.count {
            it.categoria == "Terror"
        }})"
    ) {

        fecharMenu()

        mostrarListaSeries(
            series.filter {
                it.categoria == "Terror"
            }
        )
    }// ===============================
// DORAMAS
// ===============================

adicionarTituloMenu("DORAMAS")

adicionarItemMenu(
    "💗  Todos os Doramas (${doramas.size})"
) {
    fecharMenu()
    mostrarListaSeries(doramas)
}

adicionarItemMenu(
    "❤️  Romance (${doramas.count { it.categoria == "Romance" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        doramas.filter {
            it.categoria == "Romance"
        }
    )
}

adicionarItemMenu(
    "⚔️  Ação (${doramas.count { it.categoria == "Ação" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        doramas.filter {
            it.categoria == "Ação"
        }
    )
}

adicionarItemMenu(
    "😂  Comédia (${doramas.count { it.categoria == "Comédia" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        doramas.filter {
            it.categoria == "Comédia"
        }
    )
}

adicionarItemMenu(
    "👻  Terror (${doramas.count { it.categoria == "Terror" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        doramas.filter {
            it.categoria == "Terror"
        }
    )
}


// ===============================
// ANIME
// ===============================

adicionarTituloMenu("ANIME")

adicionarItemMenu(
    "🎌  Todos os Animes (${animes.size})"
) {
    fecharMenu()
    mostrarListaSeries(animes)
}

adicionarItemMenu(
    "⚔️  Ação (${animes.count { it.categoria == "Ação" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        animes.filter {
            it.categoria == "Ação"
        }
    )
}

adicionarItemMenu(
    "😂  Comédia (${animes.count { it.categoria == "Comédia" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        animes.filter {
            it.categoria == "Comédia"
        }
    )
}

adicionarItemMenu(
    "👻  Terror (${animes.count { it.categoria == "Terror" }})"
) {
    fecharMenu()
    mostrarListaSeries(
        animes.filter {
            it.categoria == "Terror"
        }
    )
}

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
    titulo.setTextColor(Color.LTGRAY)

    titulo.setPadding(
        dp(18),
        dp(18),
        dp(12),
        dp(8)
    )

    titulo.isFocusable = false

    menuConteudo.addView(titulo)
}


// ===============================
// FECHAR MENU
// ===============================

private fun fecharMenu() {

    menuAberto = false

    menuLateral.visibility = View.GONE

    botaoMenu.requestFocus()
}


// ===============================
// MOVER MENU
// ===============================

private fun moverMenu(
    direcao: Int
) {

    if (itensMenuFoco.isEmpty()) return

    var atual = itensMenuFoco.indexOfFirst {
        it.hasFocus()
    }

    if (atual < 0) {
        atual = 0
    }

    val novo = atual + direcao

    if (novo in itensMenuFoco.indices) {

        itensMenuFoco[novo].requestFocus()

        ajustarScrollMenu(
            itensMenuFoco[novo]
        )
    }
}


// ===============================
// SCROLL DO MENU
// ===============================

private fun ajustarScrollMenu(
    view: View
) {

    menuScroll.post {

        val top = view.top
        val bottom = view.bottom

        val scrollTop = menuScroll.scrollY
        val scrollBottom =
            scrollTop + menuScroll.height

        if (top < scrollTop) {

            menuScroll.scrollTo(
                0,
                maxOf(0, top - dp(20))
            )

        } else if (bottom > scrollBottom) {

            menuScroll.scrollTo(
                0,
                bottom - menuScroll.height + dp(20)
            )
        }
    }
}


// ===============================
// MOVER CARD
// ===============================

private fun moverCard(
    direcao: Int
) {

    if (cardsAtuais.isEmpty()) return

    val novoIndice =
        indiceCardAtual + direcao

    if (novoIndice !in cardsAtuais.indices) {
        return
    }

    indiceCardAtual = novoIndice

    cardsAtuais[indiceCardAtual].requestFocus()
}


// ===============================
// PESQUISA
// ===============================

private fun abrirPesquisa() {

    val campo = EditText(this)

    campo.hint = "Digite o nome..."
    campo.textSize = 18f
    campo.setSingleLine(true)

    val resultado = filmes.filter {
        it.titulo.lowercase().contains(
            campo.text.toString()
                .trim()
                .lowercase()
        )
    }

    campo.requestFocus()

    Toast.makeText(
        this,
        "Use a busca pelo menu",
        Toast.LENGTH_SHORT
    ).show()
}


// ===============================
// CONTROLE D-PAD
// ===============================

override fun dispatchKeyEvent(
    event: KeyEvent
): Boolean {

    if (event.action != KeyEvent.ACTION_DOWN) {
        return super.dispatchKeyEvent(event)
    }

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

    return super.dispatchKeyEvent(event)
}

}
