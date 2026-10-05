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

    private val itensMenuFoco = mutableListOf<View>()
    private val cacheCapas = HashMap<String, Bitmap>()
    private val historicoConteudo = mutableListOf<() -> Unit>()

    private val filmes = mutableListOf<Filme>()
    private val series = mutableListOf<Serie>()
    private val doramas = mutableListOf<Serie>()
    private val animes = mutableListOf<Serie>()

    private val cardsAtuais = mutableListOf<View>()
    private var indiceCardAtual = 0

    private val favoritos = mutableSetOf<String>()

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

        carregarFilmes()
        carregarSeries()
    }

    private fun criarInterface() {
        raiz = FrameLayout(this)

        // Fundo Gradiente Escuro (Garante exibição instantânea)
        val fundoGradiente = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#121212"), Color.parseColor("#050505"))
        )
        raiz.background = fundoGradiente

        val fundo = ImageView(this)
        fundo.scaleType = ImageView.ScaleType.CENTER_CROP
        fundo.alpha = 0.40f

        // Tenta carregar o fundo do lobo em background sem travar o app
        carregarImagem("https://i.postimg.cc/Ghk8PP7w/wolf.png", fundo, otimizarTamanho = false)

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val camada = LinearLayout(this)
        camada.orientation = LinearLayout.VERTICAL
        camada.setPadding(dp(20), dp(12), dp(20), dp(12))

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        botaoMenu = TextView(this)
        botaoMenu.text = "☰  WOLF MENU"
        botaoMenu.setTextColor(Color.WHITE)
        botaoMenu.textSize = 20f
        botaoMenu.setTypeface(null, Typeface.BOLD)
        botaoMenu.gravity = Gravity.CENTER
        botaoMenu.isFocusable = true
        botaoMenu.isClickable = true
        botaoMenu.background = criarFundoCard(false)

        botaoMenu.setOnFocusChangeListener { _, foco ->
            botaoMenu.background = criarFundoCard(foco)
        }

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        camada.addView(
            botaoMenu,
            LinearLayout.LayoutParams(dp(235), dp(58))
        )

        val scroll = ScrollView(this)
        scroll.isFocusable = false
        scroll.isFocusableInTouchMode = false

        conteudo = LinearLayout(this)
        conteudo.orientation = LinearLayout.VERTICAL

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

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    private fun criarFundoCard(foco: Boolean): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.setColor(if (foco) Color.argb(235, 10, 10, 10) else Color.argb(190, 10, 10, 10))
        fundo.cornerRadius = dp(8).toFloat()
        if (foco) {
            fundo.setStroke(dp(4), Color.RED)
        }
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(8).toFloat()
        borda.setStroke(dp(4), Color.RED)
        return borda
    }

    private fun carregarImagem(url: String, imagem: ImageView, otimizarTamanho: Boolean = true) {
        if (url.isBlank()) return

        val salva = cacheCapas[url]
        if (salva != null) {
            imagem.setImageBitmap(salva)
            return
        }

        thread {
            try {
                val conexao = URL(url).openConnection() as HttpURLConnection
                conexao.connectTimeout = 6000
                conexao.readTimeout = 6000
                conexao.doInput = true
                conexao.connect()

                val inputStream = conexao.inputStream

                val bitmap = if (otimizarTamanho) {
                    // Otimização para acelerar e poupar memória RAM na TV
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    BitmapFactory.decodeStream(inputStream, null, options)
                } else {
                    BitmapFactory.decodeStream(inputStream)
                }

                conexao.disconnect()

                if (bitmap != null) {
                    cacheCapas[url] = bitmap
                    runOnUiThread {
                        imagem.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {}
        }
    }private fun carregarFilmes() {
        thread {
            var conexao: HttpURLConnection? = null
            try {
                conexao = URL(CATALOGO_URL).openConnection() as HttpURLConnection
                conexao.connectTimeout = 12000
                conexao.readTimeout = 12000
                conexao.requestMethod = "GET"
                conexao.doInput = true
                conexao.setRequestProperty("User-Agent", "Mozilla/5.0")
                conexao.connect()

                val codigo = conexao.responseCode
                if (codigo !in 200..299) {
                    throw Exception("HTTP $codigo")
                }

                val resposta = conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (resposta.isBlank()) {
                    throw Exception("Catálogo vazio")
                }

                val raizJson = JSONObject(resposta.trim())
                val novosFilmes = ArrayList<Filme>()
                val listaFilmes = raizJson.optJSONArray("filmes")

                if (listaFilmes != null) {
                    for (i in 0 until listaFilmes.length()) {
                        try {
                            val item = listaFilmes.getJSONObject(i)
                            val titulo = item.optString("titulo", "")
                            val ano = item.optInt("ano", 0)
                            val categoria = item.optString("categoria", "")
                            val capa = item.optString("capa", "")
                            val video = item.optString("video", "")

                            if (titulo.isNotBlank()) {
                                novosFilmes.add(Filme(titulo, ano, categoria, capa, video))
                            }
                        } catch (_: Exception) {}
                    }
                }

                novosFilmes.sortWith(compareByDescending<Filme> { it.ano }.thenBy { it.titulo.lowercase() })

                fun lerSeries(array: JSONArray?): ArrayList<Serie> {
                    val resultado = ArrayList<Serie>()
                    if (array == null) return resultado

                    for (i in 0 until array.length()) {
                        try {
                            val objeto = array.getJSONObject(i)
                            val titulo = objeto.optString("titulo", "")
                            val categoria = objeto.optString("categoria", "")
                            val capa = objeto.optString("capa", "")

                            val temporadas = ArrayList<Temporada>()
                            val arrayTemporadas = objeto.optJSONArray("temporadas")

                            if (arrayTemporadas != null) {
                                for (t in 0 until arrayTemporadas.length()) {
                                    try {
                                        val objetoTemporada = arrayTemporadas.getJSONObject(t)
                                        val numeroTemporada = objetoTemporada.optInt("numero", t + 1)
                                        val episodios = ArrayList<Episodio>()
                                        val arrayEpisodios = objetoTemporada.optJSONArray("episodios")

                                        if (arrayEpisodios != null) {
                                            for (e in 0 until arrayEpisodios.length()) {
                                                try {
                                                    val objetoEpisodio = arrayEpisodios.getJSONObject(e)
                                                    val numeroEpisodio = objetoEpisodio.optInt("numero", e + 1)
                                                    val tituloEpisodio = objetoEpisodio.optString("titulo", "")
                                                    val videoEpisodio = objetoEpisodio.optString("video", "")

                                                    episodios.add(Episodio(numeroEpisodio, tituloEpisodio, videoEpisodio))
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        temporadas.add(Temporada(numeroTemporada, episodios))
                                    } catch (_: Exception) {}
                                }
                            }

                            if (titulo.isNotBlank()) {
                                resultado.add(Serie(titulo, categoria, capa, temporadas))
                            }
                        } catch (_: Exception) {}
                    }
                    return resultado
                }

                val novasSeries = lerSeries(raizJson.optJSONArray("series"))
                val novosDoramas = lerSeries(raizJson.optJSONArray("doramas"))
                val novosAnimes = lerSeries(raizJson.optJSONArray("animes"))

                runOnUiThread {
                    filmes.clear()
                    filmes.addAll(novosFilmes)

                    series.clear()
                    series.addAll(novasSeries)

                    doramas.clear()
                    doramas.addAll(novosDoramas)

                    animes.clear()
                    animes.addAll(novosAnimes)

                    mostrarListaCards(filmes)

                    Toast.makeText(
                        this@MainActivity,
                        "Catálogo carregado: ${filmes.size} filmes, ${series.size} séries, ${doramas.size} doramas e ${animes.size} animes",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (erro: Exception) {
                runOnUiThread {
                    filmes.clear()
                    series.clear()
                    doramas.clear()
                    animes.clear()

                    conteudo.removeAllViews()
                    val erroTexto = TextView(this@MainActivity).apply {
                        text = "ERRO NO CATÁLOGO\n\n${erro.message ?: "Erro desconhecido"}"
                        textSize = 20f
                        setTextColor(Color.WHITE)
                        gravity = Gravity.CENTER
                        setPadding(dp(30), dp(30), dp(30), dp(30))
                    }
                    conteudo.addView(erroTexto)

                    Toast.makeText(this@MainActivity, "Erro ao carregar catálogo", Toast.LENGTH_LONG).show()
                }
            } finally {
                conexao?.disconnect()
            }
        }
    }

    private fun carregarSeries() {}

    private fun mostrarListaCards(lista: List<Filme>) {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        indiceCardAtual = 0

        if (lista.isEmpty()) {
            val vazio = TextView(this).apply {
                text = "Nenhum filme encontrado"
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            conteudo.addView(vazio, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100)))
            return
        }

        lista.chunked(5).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350))
            }

            grupo.forEach { filme ->
                val card = criarCard(filme)
                linha.addView(card, LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(12)
                })
                cardsAtuais.add(card)
            }
            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCard(filme: Filme): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        carregarImagem(filme.capa, imagem)

        card.addView(imagem, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275)).apply {
            gravity = Gravity.TOP
        })

        val informacoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val titulo = TextView(this).apply {
            text = filme.titulo
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        informacoes.addView(titulo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        val detalhes = TextView(this).apply {
            text = "${filme.ano} • ${filme.categoria}"
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }
        informacoes.addView(detalhes, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)))

        card.addView(informacoes, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
            gravity = Gravity.BOTTOM
        })

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(borda, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) {
                indiceCardAtual = cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {
            abrirVideo(filme.titulo, filme.video, filme.capa)
        }

        return card
    }

    private fun mostrarListaSeries(lista: List<Serie>) {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        indiceCardAtual = 0

        if (lista.isEmpty()) {
            val vazio = TextView(this).apply {
                text = "Nenhuma série encontrada"
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
            }
            conteudo.addView(vazio, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100)))
            return
        }

        lista.chunked(5).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350))
            }

            grupo.forEach { serie ->
                val card = criarCardSerie(serie)
                linha.addView(card, LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(12)
                })
                cardsAtuais.add(card)
            }
            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCardSerie(serie: Serie): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        carregarImagem(serie.capa, imagem)

        card.addView(imagem, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275)))

        val informacoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val titulo = TextView(this).apply {
            text = serie.titulo
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        informacoes.addView(titulo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)))

        val detalhes = TextView(this).apply {
            text = "${serie.categoria} • ${serie.temporadas.size} temporada(s)"
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }
        informacoes.addView(detalhes, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)))

        card.addView(informacoes, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
            gravity = Gravity.BOTTOM
        })

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(borda, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) {
                indiceCardAtual = cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {
            historicoConteudo.add { mostrarListaSeries(series) }
            mostrarTemporadas(serie)
        }

        return card
    }

    private fun mostrarTemporadas(serie: Serie) {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        indiceCardAtual = 0

        serie.temporadas.chunked(5).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350))
            }

            grupo.forEach { temporada ->
                val card = criarCardTemporada(serie, temporada)
                linha.addView(card, LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(12)
                })
                cardsAtuais.add(card)
            }
            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCardTemporada(serie: Serie, temporada: Temporada): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        carregarImagem(serie.capa, imagem)

        card.addView(imagem, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275)))

        val titulo = TextView(this).apply {
            text = "Temporada ${temporada.numero}"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        card.addView(titulo, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
            gravity = Gravity.BOTTOM
        })

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(borda, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) {
                indiceCardAtual = cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {
            historicoConteudo.add { mostrarTemporadas(serie) }
            mostrarEpisodios(serie, temporada)
        }

        return card
    }

    private fun mostrarEpisodios(serie: Serie, temporada: Temporada) {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        indiceCardAtual = 0

        temporada.episodios.chunked(5).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(350))
            }

            grupo.forEach { episodio ->
                val card = criarCardEpisodio(serie, episodio)
                linha.addView(card, LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                    leftMargin = dp(4)
                    rightMargin = dp(4)
                    bottomMargin = dp(12)
                })
                cardsAtuais.add(card)
            }
            conteudo.addView(linha)
        }

        if (cardsAtuais.isNotEmpty()) {
            cardsAtuais[0].requestFocus()
        }
    }

    private fun criarCardEpisodio(serie: Serie, episodio: Episodio): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        carregarImagem(serie.capa, imagem)

        card.addView(imagem, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275)))

        val informacoes = TextView(this).apply {
            text = "EP ${episodio.numero} • ${episodio.titulo}"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(8), dp(3))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        card.addView(informacoes, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
            gravity = Gravity.BOTTOM
        })

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(borda, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) {
                indiceCardAtual = cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {
            abrirVideo("${serie.titulo} - EP ${episodio.numero}", episodio.video, serie.capa)
        }

        return card
    }

    private fun abrirVideo(titulo: String, video: String, capa: String) {
        if (video.isBlank()) {
            Toast.makeText(this, "Vídeo ainda não disponível", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URL", video)
            putExtra("VIDEO_TITLE", titulo)
            putExtra("VIDEO_COVER", capa)
        }
        startActivity(intent)
    }private fun abrirMenu() {
        if (menuAberto) return
        menuAberto = true

        menuLateral = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(245, 12, 12, 12))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val topo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tituloMenu = TextView(this).apply {
            text = "MENU WOLF"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
        }
        topo.addView(tituloMenu, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        botaoFecharMenu = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = criarFundoCard(false)

            setOnFocusChangeListener { _, foco ->
                background = criarFundoCard(foco)
            }

            setOnClickListener {
                fecharMenu()
            }
        }
        topo.addView(botaoFecharMenu, LinearLayout.LayoutParams(dp(45), dp(45)))

        menuLateral.addView(topo, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val campoBusca = EditText(this).apply {
            hint = "Buscar filme ou série..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = criarFundoCard(false)

            setOnFocusChangeListener { _, foco ->
                background = criarFundoCard(foco)
            }
        }
        menuLateral.addView(campoBusca, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply {
            topMargin = dp(15)
            bottomMargin = dp(15)
        })

        menuScroll = ScrollView(this)
        menuConteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        menuScroll.addView(menuConteudo)

        menuLateral.addView(menuScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        itensMenuFoco.clear()

        val criarOpcaoMenu = { texto: String, acao: () -> Unit ->
            val item = TextView(this).apply {
                text = texto
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(dp(16), dp(14), dp(16), dp(14))
                isFocusable = true
                isClickable = true
                background = criarFundoCard(false)

                setOnFocusChangeListener { _, foco ->
                    background = criarFundoCard(foco)
                }

                setOnClickListener {
                    fecharMenu()
                    acao()
                }
            }
            menuConteudo.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            })
            itensMenuFoco.add(item)
        }

        criarOpcaoMenu("🎬 Filmes") {
            historicoConteudo.clear()
            mostrarListaCards(filmes)
        }

        criarOpcaoMenu("📺 Séries") {
            historicoConteudo.clear()
            mostrarListaSeries(series)
        }

        criarOpcaoMenu("⛩️ Animes") {
            historicoConteudo.clear()
            mostrarListaSeries(animes)
        }

        criarOpcaoMenu("🌸 Doramas") {
            historicoConteudo.clear()
            mostrarListaSeries(doramas)
        }

        criarOpcaoMenu("🔍 Executar Busca") {
            val termo = campoBusca.text.toString().trim().lowercase()
            if (termo.isNotBlank()) {
                historicoConteudo.clear()
                val resultado = filmes.filter { it.titulo.lowercase().contains(termo) }
                mostrarListaCards(resultado)
            }
        }

        raiz.addView(menuLateral, FrameLayout.LayoutParams(dp(320), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START))
        botaoFecharMenu.requestFocus()
    }

    private fun fecharMenu() {
        if (!menuAberto) return
        raiz.removeView(menuLateral)
        menuAberto = false
        botaoMenu.requestFocus()
    }

    override fun onBackPressed() {
        if (menuAberto) {
            fecharMenu()
            return
        }

        if (historicoConteudo.isNotEmpty()) {
            val acaoAnterior = historicoConteudo.removeAt(historicoConteudo.size - 1)
            acaoAnterior()
            return
        }

        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (menuAberto) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                fecharMenu()
                return true
            }
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                val focoAtual = currentFocus
                focoAtual?.performClick()
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (botaoMenu.hasFocus() && cardsAtuais.isNotEmpty()) {
                    cardsAtuais[0].requestFocus()
                    return true
                }
                
                val novoIndice = indiceCardAtual + 5
                if (novoIndice < cardsAtuais.size) {
                    cardsAtuais[novoIndice].requestFocus()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                val novoIndice = indiceCardAtual - 5
                if (novoIndice >= 0 && novoIndice < cardsAtuais.size) {
                    cardsAtuais[novoIndice].requestFocus()
                    return true
                } else if (indiceCardAtual in 0..4) {
                    botaoMenu.requestFocus()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (indiceCardAtual % 5 != 0 && indiceCardAtual - 1 >= 0) {
                    cardsAtuais[indiceCardAtual - 1].requestFocus()
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if ((indiceCardAtual + 1) % 5 != 0 && indiceCardAtual + 1 < cardsAtuais.size) {
                    cardsAtuais[indiceCardAtual + 1].requestFocus()
                    return true
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}
