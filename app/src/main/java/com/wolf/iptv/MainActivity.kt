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
import java.io.BufferedReader
import java.io.InputStreamReader
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

    companion object {
        private const val CATALOGO_URL =
            "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/refs/heads/main/catalogo.json"
    }

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

        raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        val fundo =
            ImageView(this)

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

        botaoMenu.textSize =
            20f

        botaoMenu.setTypeface(
            null,
            Typeface.BOLD
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable =
            true

        botaoMenu.isClickable =
            true

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

        scroll.isFocusable =
            false

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

        if (url.isBlank()) {
            return
        }

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

                    runOnUiThread {

                        imagem.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    private fun carregarFilmes() {

        filmes.clear()

        mostrarListaCards(
            filmes
        )

        atualizarCatalogo(
            mostrarMensagem = false
        )
    }

    private fun atualizarCatalogo(
        mostrarMensagem: Boolean = true
    ) {

        thread {

            var conexao:
                HttpURLConnection? = null

            try {

                val urlComCacheBust =
                    "$CATALOGO_URL?ts=${System.currentTimeMillis()}"

                conexao =
                    URL(
                        urlComCacheBust
                    ).openConnection()
                        as HttpURLConnection

                conexao.connectTimeout =
                    15000

                conexao.readTimeout =
                    15000

                conexao.requestMethod =
                    "GET"

                conexao.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
                )

                conexao.setRequestProperty(
                    "Pragma",
                    "no-cache"
                )

                conexao.connect()

                val codigo =
                    conexao.responseCode

                if (
                    codigo !in 200..299
                ) {
                    throw Exception(
                        "HTTP $codigo"
                    )
                }

                val texto =
                    BufferedReader(
                        InputStreamReader(
                            conexao.inputStream
                        )
                    ).use {
                        it.readText()
                    }

                val json =
                    JSONArray(texto)

                val novaLista =
                    mutableListOf<Filme>()

                for (
                    i in 0 until json.length()
                ) {

                    val item =
                        json.getJSONObject(i)

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

                        novaLista.add(
                            Filme(
                                titulo,
                                ano,
                                categoria,
                                capa,
                                video
                            )
                        )
                    }
                }

                novaLista.sortWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo
                    }
                )

                runOnUiThread {

                    filmes.clear()

                    filmes.addAll(
                        novaLista
                    )

                    historicoConteudo.clear()

                    mostrarListaCards(
                        filmes
                    )

                    if (
                        mostrarMensagem
                    ) {

                        Toast.makeText(
                            this,
                            "Catálogo atualizado: ${filmes.size} filmes",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (_: Exception) {

                runOnUiThread {

                    if (
                        mostrarMensagem
                    ) {

                        Toast.makeText(
                            this,
                            "Não foi possível atualizar o catálogo",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    mostrarListaCards(
                        filmes
                    )
                }

            } finally {

                conexao?.disconnect()
            }
        }
    }    private fun mostrarTemporadas(
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

                linha.gravity =
                    Gravity.CENTER_VERTICAL

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
            cardsAtuais[0].requestFocus()
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
            ImageView.ScaleType.CENTER_CROP

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
            "Temporada ${temporada.numero}\n${temporada.episodios.size} episódios"

        informacoes.setTextColor(
            Color.WHITE
        )

        informacoes.textSize =
            15f

        informacoes.setTypeface(
            null,
            Typeface.BOLD
        )

        informacoes.gravity =
            Gravity.CENTER

        informacoes.setPadding(
            dp(8),
            dp(4),
            dp(8),
            dp(4)
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
                dp(60)
            ).apply {
                gravity =
                    Gravity.BOTTOM
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

                linha.gravity =
                    Gravity.CENTER_VERTICAL

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
            cardsAtuais[0].requestFocus()
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
            ImageView.ScaleType.CENTER_CROP

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
            "EP ${episodio.numero} • ${episodio.titulo}"

        informacoes.setTextColor(
            Color.WHITE
        )

        informacoes.textSize =
            14f

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
                gravity =
                    Gravity.BOTTOM
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
                "${serie.titulo} - EP ${episodio.numero}",
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
    }    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item = TextView(this)

        item.text = texto
        item.setTextColor(Color.WHITE)
        item.textSize = 16f
        item.gravity = Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isFocusableInTouchMode = true
        item.isClickable = true
        item.background = criarFundoCard(false)

        item.setOnFocusChangeListener { _, foco ->
            item.background = criarFundoCard(foco)
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

    private fun adicionarTituloMenu(
        texto: String
    ) {

        val titulo = TextView(this)

        titulo.text = texto
        titulo.setTextColor(Color.RED)
        titulo.textSize = 14f
        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            dp(15),
            dp(10),
            dp(10),
            dp(4)
        )

        menuConteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )
    }

    private fun abrirMenu() {

        if (menuAberto) {
            return
        }

        menuAberto = true
        itensMenuFoco.clear()

        menuLateral = LinearLayout(this)

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

        val params = FrameLayout.LayoutParams(
            dp(360),
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        params.gravity =
            Gravity.START or Gravity.TOP

        raiz.addView(
            menuLateral,
            params
        )

        val cabecalho = LinearLayout(this)

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

        val tituloMenu = TextView(this)

        tituloMenu.text = "WOLF MENU"
        tituloMenu.setTextColor(Color.WHITE)
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

        botaoFecharMenu = TextView(this)

        botaoFecharMenu.text = "✕"
        botaoFecharMenu.setTextColor(Color.WHITE)
        botaoFecharMenu.textSize = 24f
        botaoFecharMenu.gravity = Gravity.CENTER

        botaoFecharMenu.isFocusable = true
        botaoFecharMenu.isFocusableInTouchMode = true
        botaoFecharMenu.isClickable = true

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

        menuScroll = ScrollView(this)

        menuScroll.isFocusable = false
        menuScroll.isFocusableInTouchMode = false

        menuConteudo = LinearLayout(this)

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

            val lista = filmes.filter {
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

        adicionarItemMenu(
            "🔄  Atualizar catálogo"
        ) {

            fecharMenu()

            atualizarCatalogo()
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
            "🔥  Ação (${filmes.count { it.categoria == "Ação" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Ação"
                }
            )
        }

        adicionarItemMenu(
            "🏹  Aventura (${filmes.count { it.categoria == "Aventura" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Aventura"
                }
            )
        }

        adicionarItemMenu(
            "🧸  Animação (${filmes.count { it.categoria == "Animação" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Animação"
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${filmes.count { it.categoria == "Comédia" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Comédia"
                }
            )
        }

        adicionarItemMenu(
            "🎭  Drama (${filmes.count { it.categoria == "Drama" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Drama"
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${filmes.count { it.categoria == "Terror" }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria == "Terror"
                }
            )
        }

        adicionarItemMenu(
            "🚀  Ficção (${filmes.count { it.categoria == "Ficção" }})"
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
            "🔥  Ação (${series.count { it.categoria == "Ação" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria == "Ação"
                }
            )
        }

        adicionarItemMenu(
            "🏹  Aventura (${series.count { it.categoria == "Aventura" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria == "Aventura"
                }
            )
        }

        adicionarItemMenu(
            "😂  Comédia (${series.count { it.categoria == "Comédia" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria == "Comédia"
                }
            )
        }

        adicionarItemMenu(
            "🎭  Drama (${series.count { it.categoria == "Drama" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria == "Drama"
                }
            )
        }

        adicionarItemMenu(
            "👻  Terror (${series.count { it.categoria == "Terror" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                series.filter {
                    it.categoria == "Terror"
                }
            )
        }        adicionarTituloMenu(
            "DORAMAS"
        )

        adicionarItemMenu(
            "🎎  Todos os doramas (${doramas.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                doramas
            )
        }

        adicionarItemMenu(
            "🔥  Ação (${doramas.count { it.categoria == "Ação" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                doramas.filter {
                    it.categoria == "Ação"
                }
            )
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
            "🎭  Drama (${doramas.count { it.categoria == "Drama" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                doramas.filter {
                    it.categoria == "Drama"
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

        adicionarTituloMenu(
            "ANIME"
        )

        adicionarItemMenu(
            "🎌  Todos os animes (${animes.size})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes
            )
        }

        adicionarItemMenu(
            "🔥  Ação (${animes.count { it.categoria == "Ação" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes.filter {
                    it.categoria == "Ação"
                }
            )
        }

        adicionarItemMenu(
            "🏹  Aventura (${animes.count { it.categoria == "Aventura" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes.filter {
                    it.categoria == "Aventura"
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
            "✨  Fantasia (${animes.count { it.categoria == "Fantasia" }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                animes.filter {
                    it.categoria == "Fantasia"
                }
            )
        }

        botaoFecharMenu.requestFocus()

        ajustarScrollMenu()
    }

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

        botaoMenu.requestFocus()
    }

    private fun moverMenu(
        direcao: Int
    ): Boolean {

        if (!menuAberto) {
            return false
        }

        if (itensMenuFoco.isEmpty()) {
            return false
        }

        val atual =
            itensMenuFoco.indexOfFirst {
                it.hasFocus()
            }

        if (atual < 0) {

            itensMenuFoco[0].requestFocus()

            ajustarScrollMenu()

            return true
        }

        val novo =
            (atual + direcao).coerceIn(
                0,
                itensMenuFoco.lastIndex
            )

        if (novo != atual) {

            itensMenuFoco[novo].requestFocus()

            ajustarScrollMenu()
        }

        return true
    }

    private fun ajustarScrollMenu() {

        if (!::menuScroll.isInitialized) {
            return
        }

        val foco =
            itensMenuFoco.firstOrNull {
                it.hasFocus()
            } ?: return

        menuScroll.post {

            val topo = foco.top
            val altura = menuScroll.height
            val margem = dp(60)

            if (
                topo <
                menuScroll.scrollY + margem
            ) {

                menuScroll.smoothScrollTo(
                    0,
                    (topo - margem)
                        .coerceAtLeast(0)
                )

            } else if (
                topo + foco.height >
                menuScroll.scrollY +
                altura -
                margem
            ) {

                menuScroll.smoothScrollTo(
                    0,
                    topo +
                    foco.height -
                    altura +
                    margem
                )
            }
        }
    }

    private fun moverCard(
        tecla: Int
    ): Boolean {

        if (cardsAtuais.isEmpty()) {
            return false
        }

        val atual =
            cardsAtuais.indexOfFirst {
                it.hasFocus()
            }

        if (atual < 0) {

            cardsAtuais[0].requestFocus()

            return true
        }

        val colunas = 5

        val novo =
            when (tecla) {

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (atual % colunas > 0) {
                        atual - 1
                    } else {
                        atual
                    }
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    if (
                        atual % colunas <
                        colunas - 1 &&
                        atual + 1 <
                        cardsAtuais.size
                    ) {
                        atual + 1
                    } else {
                        atual
                    }
                }

                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (atual - colunas >= 0) {
                        atual - colunas
                    } else {
                        atual
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (
                        atual + colunas <
                        cardsAtuais.size
                    ) {
                        atual + colunas
                    } else {
                        atual
                    }
                }

                else -> atual
            }

        if (novo != atual) {
            cardsAtuais[novo].requestFocus()
        }

        return true
    }

    private fun abrirPesquisa() {

        fecharMenu()

        val caixa = EditText(this)

        caixa.hint =
            "Digite o nome do filme"

        caixa.setTextColor(
            Color.WHITE
        )

        caixa.setHintTextColor(
            Color.LTGRAY
        )

        caixa.textSize = 18f

        caixa.setSingleLine(true)

        caixa.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        caixa.background =
            criarFundoCard(true)

        val container =
            FrameLayout(this)

        container.setBackgroundColor(
            Color.argb(
                235,
                0,
                0,
                0
            )
        )

        container.setPadding(
            dp(30),
            dp(30),
            dp(30),
            dp(30)
        )

        container.addView(
            caixa,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        raiz.addView(
            container,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        caixa.requestFocus()

        caixa.setOnEditorActionListener {
                _, _, _ ->

            val texto =
                caixa.text
                    .toString()
                    .trim()

            if (texto.isNotBlank()) {

                val resultado =
                    filmes.filter {
                        it.titulo.contains(
                            texto,
                            ignoreCase = true
                        )
                    }

                raiz.removeView(
                    container
                )

                mostrarListaCards(
                    resultado
                )
            }

            true
        }

        caixa.setOnKeyListener {
                _, tecla, evento ->

            if (
                tecla ==
                KeyEvent.KEYCODE_BACK &&
                evento.action ==
                KeyEvent.ACTION_DOWN
            ) {

                raiz.removeView(
                    container
                )

                botaoMenu.requestFocus()

                true

            } else {
                false
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

            when (
                event.keyCode
            ) {

                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_UP -> {

                    if (menuAberto) {
                        return moverMenu(-1)
                    }

                    return moverCard(
                        KeyEvent.KEYCODE_DPAD_UP
                    )
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    if (menuAberto) {
                        return moverMenu(1)
                    }

                    return moverCard(
                        KeyEvent.KEYCODE_DPAD_DOWN
                    )
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (!menuAberto) {

                        return moverCard(
                            KeyEvent.KEYCODE_DPAD_LEFT
                        )
                    }
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    if (!menuAberto) {

                        return moverCard(
                            KeyEvent.KEYCODE_DPAD_RIGHT
                        )
                    }
                }

                KeyEvent.KEYCODE_MENU -> {

                    if (!menuAberto) {

                        abrirMenu()

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
