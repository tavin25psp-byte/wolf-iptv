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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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

    private val BASE_URL =
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/"

    private val CATALOGO_URL = BASE_URL + "catalogo.json"

    // Nomes possíveis dos arquivos no GitHub (ele tenta um por um).
    // Se você renomear o arquivo, é só colocar o nome certo aqui.
    private val ARQUIVOS_SERIES = listOf("series.json", "serie.json", "s%C3%A9rie.json")
    private val ARQUIVOS_DORAMAS = listOf("doramas.json", "Doramas.json")
    private val ARQUIVOS_ANIMES = listOf("animes.json", "anime.json", "Animes.json", "Anime.json")

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var botaoMenu: TextView

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView
    private lateinit var menuConteudo: LinearLayout
    private lateinit var botaoFecharMenu: TextView

    private var menuAberto = false

    private val itensMenuFoco = mutableListOf<View>()

    private val cacheCapas = ConcurrentHashMap<String, Bitmap>()
    private val imagensEmCarregamento = ConcurrentHashMap.newKeySet<String>()
    private val executorImagens = Executors.newFixedThreadPool(8)

    private val pastaCacheImagens: File by lazy {
        File(cacheDir, "wolf_imagens").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val historicoConteudo = mutableListOf<() -> Unit>()

    private val filmes = mutableListOf<Filme>()
    private val series = mutableListOf<Serie>()
    private val doramas = mutableListOf<Serie>()
    private val animes = mutableListOf<Serie>()

    // Guarda de qual lista (séries, doramas ou animes) o usuário veio,
    // pra o botão voltar retornar pra lista certa.
    private var listaSeriesAtual: List<Serie> = emptyList()

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
        carregarCatalogo()
    }

    private fun normalizarTexto(texto: String): String {
        val temp = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return temp.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase().trim()
    }

    private fun criarInterface() {
        raiz = FrameLayout(this)
        raiz.setBackgroundColor(Color.BLACK)

        val fundo = ImageView(this)
        fundo.scaleType = ImageView.ScaleType.CENTER_CROP
        fundo.alpha = 0.55f

        carregarImagemFundoPrioritaria(
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

    private fun carregarImagemFundoPrioritaria(url: String, imagem: ImageView) {
        if (url.isBlank()) return
        imagem.tag = url

        cacheCapas[url]?.let { bitmap ->
            imagem.setImageBitmap(bitmap)
            return
        }

        try {
            val arquivo = arquivoCacheImagem(url)
            if (arquivo.exists() && arquivo.length() > 0L) {
                val bitmap = decodificarImagemOtimizada(
                    arquivo,
                    resources.displayMetrics.widthPixels,
                    resources.displayMetrics.heightPixels
                )
                if (bitmap != null) {
                    cacheCapas[url] = bitmap
                    imagem.setImageBitmap(bitmap)
                    return
                }
            }
        } catch (_: Exception) {}

        carregarImagem(
            url,
            imagem,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
    }

    private fun carregarImagem(
        url: String,
        imagem: ImageView,
        larguraAlvo: Int = 500,
        alturaAlvo: Int = 700
    ) {
        if (url.isBlank()) return
        imagem.tag = url

        cacheCapas[url]?.let { bitmap ->
            imagem.setImageBitmap(bitmap)
            return
        }

        if (!imagensEmCarregamento.add(url)) return

        executorImagens.execute {
            var bitmap: Bitmap? = null
            var arquivoCache: File? = null

            try {
                arquivoCache = arquivoCacheImagem(url)

                if (arquivoCache.exists() && arquivoCache.length() > 0L) {
                    bitmap = decodificarImagemOtimizada(arquivoCache, larguraAlvo, alturaAlvo)
                }

                if (bitmap == null) {
                    var conexao: HttpURLConnection? = null
                    try {
                        conexao = URL(url).openConnection() as HttpURLConnection
                        conexao.connectTimeout = 5000
                        conexao.readTimeout = 8000
                        conexao.doInput = true
                        conexao.instanceFollowRedirects = true
                        conexao.useCaches = true

                        conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                        conexao.setRequestProperty(
                            "Accept",
                            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                        )
                        conexao.setRequestProperty("Connection", "keep-alive")
                        conexao.connect()

                        if (conexao.responseCode !in 200..299) throw Exception("HTTP ${conexao.responseCode}")

                        val arquivoTemporario = File(pastaCacheImagens, "${arquivoCache.name}.tmp")

                        try {
                            conexao.inputStream.buffered(128 * 1024).use { input ->
                                arquivoTemporario.outputStream().buffered(128 * 1024).use { output ->
                                    input.copyTo(output, 128 * 1024)
                                }
                            }

                            if (arquivoTemporario.exists() && arquivoTemporario.length() > 0L) {
                                if (arquivoCache.exists()) arquivoCache.delete()
                                arquivoTemporario.renameTo(arquivoCache)
                                bitmap = decodificarImagemOtimizada(arquivoCache, larguraAlvo, alturaAlvo)
                            }
                        } finally {
                            if (arquivoTemporario.exists()) arquivoTemporario.delete()
                        }
                    } finally {
                        conexao?.disconnect()
                    }
                }

                bitmap?.let { carregada ->
                    cacheCapas[url] = carregada
                    runOnUiThread {
                        if (imagem.tag == url) {
                            imagem.setImageBitmap(carregada)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                imagensEmCarregamento.remove(url)
            }
        }
    }

    private fun decodificarImagemOtimizada(
        arquivo: File,
        larguraAlvo: Int,
        alturaAlvo: Int
    ): Bitmap? {
        val opcoes = BitmapFactory.Options()
        opcoes.inJustDecodeBounds = true
        BitmapFactory.decodeFile(arquivo.absolutePath, opcoes)

        if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) return null

        val largura = larguraAlvo.coerceAtLeast(1)
        val altura = alturaAlvo.coerceAtLeast(1)
        var amostra = 1

        while (
            opcoes.outWidth / (amostra * 2) >= largura &&
            opcoes.outHeight / (amostra * 2) >= altura
        ) {
            amostra *= 2
        }

        val leitura = BitmapFactory.Options()
        leitura.inSampleSize = amostra
        leitura.inPreferredConfig = Bitmap.Config.RGB_565
        leitura.inScaled = true

        return BitmapFactory.decodeFile(arquivo.absolutePath, leitura)
    }

    private fun arquivoCacheImagem(url: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        return File(pastaCacheImagens, "$hash.img")
    }
        private fun carregarCatalogo() {
        activityScope.launch(Dispatchers.IO) {
            var conexao: HttpURLConnection? = null
            try {
                conexao = URL(CATALOGO_URL).openConnection() as HttpURLConnection
                conexao.connectTimeout = 20000
                conexao.readTimeout = 20000
                conexao.requestMethod = "GET"
                conexao.doInput = true
                conexao.setRequestProperty("User-Agent", "Mozilla/5.0")
                conexao.connect()

                if (conexao.responseCode !in 200..299) throw Exception("HTTP ${conexao.responseCode}")

                val resposta = conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (resposta.isBlank()) throw Exception("Catálogo vazio")

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
                                                    episodios.add(
                                                        Episodio(
                                                            numero = objetoEpisodio.optInt("numero", e + 1),
                                                            titulo = objetoEpisodio.optString("titulo", ""),
                                                            video = objetoEpisodio.optString("video", "")
                                                        )
                                                    )
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

                // Séries, doramas e animes vêm de arquivos separados.
                // Se algum arquivo falhar, o resto do catálogo continua funcionando.
                // Se ainda existir "series"/"doramas"/"animes" dentro do catalogo.json,
                // também é lido (compatível com o formato antigo).
                val textoSeries = baixarPrimeiro(ARQUIVOS_SERIES)
                val textoDoramas = baixarPrimeiro(ARQUIVOS_DORAMAS)
                val textoAnimes = baixarPrimeiro(ARQUIVOS_ANIMES)

                val novasSeries = lerSeries(
                    extrairArray(textoSeries, "series") ?: raizJson.optJSONArray("series")
                )
                val novosDoramas = lerSeries(
                    extrairArray(textoDoramas, "doramas") ?: raizJson.optJSONArray("doramas")
                )
                val novosAnimes = lerSeries(
                    extrairArray(textoAnimes, "animes") ?: raizJson.optJSONArray("animes")
                )

                withContext(Dispatchers.Main) {
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
                withContext(Dispatchers.Main) {
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
                }
            } finally {
                conexao?.disconnect()
            }
        }
        }
            private fun baixarTexto(url: String): String? {
        var conexao: HttpURLConnection? = null
        return try {
            conexao = URL(url).openConnection() as HttpURLConnection
            conexao.connectTimeout = 20000
            conexao.readTimeout = 20000
            conexao.requestMethod = "GET"
            conexao.doInput = true
            conexao.setRequestProperty("User-Agent", "Mozilla/5.0")
            conexao.connect()

            if (conexao.responseCode !in 200..299) {
                null
            } else {
                conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            conexao?.disconnect()
        }
    }

    private fun baixarPrimeiro(nomes: List<String>): String? {
        for (nome in nomes) {
            val texto = baixarTexto(BASE_URL + nome)
            if (!texto.isNullOrBlank()) return texto
        }
        return null
    }

    // Aceita o arquivo tanto como { "series": [ ... ] } quanto como [ ... ].
    private fun extrairArray(texto: String?, chave: String): JSONArray? {
        if (texto.isNullOrBlank()) return null
        val limpo = texto.trim()
        return try {
            if (limpo.startsWith("[")) JSONArray(limpo)
            else JSONObject(limpo).optJSONArray(chave)
        } catch (_: Exception) {
            null
        }
    }

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
                linha.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                        leftMargin = dp(4)
                        rightMargin = dp(4)
                        bottomMargin = dp(12)
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

        card.addView(
            imagem,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275)).apply {
                gravity = Gravity.TOP
            }
        )

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

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(
            borda,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) indiceCardAtual = cardsAtuais.indexOf(card)
        }

        card.setOnClickListener {
            abrirVideo(filme.titulo, filme.video, filme.capa)
        }

        return card
    }

    private fun mostrarListaSeries(lista: List<Serie>) {
        listaSeriesAtual = lista
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
                linha.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                        leftMargin = dp(4)
                        rightMargin = dp(4)
                        bottomMargin = dp(12)
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

    private fun criarCardSerie(serie: Serie): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
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

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(
            borda,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) indiceCardAtual = cardsAtuais.indexOf(card)
        }

        card.setOnClickListener {
            val origem = listaSeriesAtual
            historicoConteudo.add { mostrarListaSeries(origem) }
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
                linha.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                        leftMargin = dp(4)
                        rightMargin = dp(4)
                        bottomMargin = dp(12)
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

    private fun criarCardTemporada(serie: Serie, temporada: Temporada): View {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
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

        card.addView(
            titulo,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(
            borda,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) indiceCardAtual = cardsAtuais.indexOf(card)
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
                val card = criarCardEpisodio(serie, temporada.numero, episodio)
                linha.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(335), 1f).apply {
                        leftMargin = dp(4)
                        rightMargin = dp(4)
                        bottomMargin = dp(12)
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

    // ===== EPISÓDIOS ASSISTIDOS (salvos no aparelho) =====

    private val prefsWolf by lazy {
        getSharedPreferences("wolf_prefs", MODE_PRIVATE)
    }

    private fun chaveEpisodio(serie: Serie, numeroTemporada: Int, numeroEpisodio: Int): String {
        return "${serie.titulo}|T$numeroTemporada|E$numeroEpisodio"
    }

    private fun episodioAssistido(chave: String): Boolean {
        return prefsWolf.getStringSet("assistidos", emptySet())?.contains(chave) == true
    }

    private fun marcarEpisodio(chave: String, assistido: Boolean) {
        val novo = HashSet(prefsWolf.getStringSet("assistidos", emptySet()) ?: emptySet())
        if (assistido) novo.add(chave) else novo.remove(chave)
        prefsWolf.edit().putStringSet("assistidos", novo).apply()
    }

    private fun criarCardEpisodio(serie: Serie, numeroTemporada: Int, episodio: Episodio): View {
        val chave = chaveEpisodio(serie, numeroTemporada, episodio.numero)
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
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

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(
            borda,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
            if (foco) indiceCardAtual = cardsAtuais.indexOf(card)
        }

        val textoBase = informacoes.text.toString()

        val selo = TextView(this).apply {
            text = "✓ ASSISTIDO"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setBackgroundColor(Color.argb(230, 0, 140, 60))
            visibility = View.GONE
        }
        card.addView(
            selo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(8)
                rightMargin = dp(8)
            }
        )

        fun atualizarVisual() {
            val visto = episodioAssistido(chave)
            selo.visibility = if (visto) View.VISIBLE else View.GONE
            imagem.alpha = if (visto) 0.45f else 1f
            informacoes.text = if (visto) "✓ $textoBase" else textoBase
        }
        atualizarVisual()

        card.setOnClickListener {
            // Marca como assistido quando o episódio é aberto.
            if (episodio.video.isNotBlank()) {
                marcarEpisodio(chave, true)
                atualizarVisual()
            }
            abrirVideo("${serie.titulo} - EP ${episodio.numero}", episodio.video, serie.capa)
        }

        // Segurar o OK do controle (ou o dedo) marca/desmarca na mão.
        card.setOnLongClickListener {
            val agora = !episodioAssistido(chave)
            marcarEpisodio(chave, agora)
            atualizarVisual()
            Toast.makeText(
                this,
                if (agora) "Marcado como assistido" else "Marcado como não assistido",
                Toast.LENGTH_SHORT
            ).show()
            true
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
    }
        private fun adicionarItemMenu(texto: String, acao: () -> Unit) {
        val item = TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(12), 0)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)

            setOnFocusChangeListener { _, foco -> background = criarFundoCard(foco) }
            setOnClickListener { acao() }
        }

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                bottomMargin = dp(4)
            }
        )
        itensMenuFoco.add(item)
    }

    private fun abrirMenu() {
        if (menuAberto) return
        menuAberto = true
        itensMenuFoco.clear()

        menuLateral = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(245, 5, 5, 5))
        }

        val params = FrameLayout.LayoutParams(dp(360), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START or Gravity.TOP
        }
        raiz.addView(menuLateral, params)

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(8), dp(10), dp(8))
        }
        menuLateral.addView(cabecalho, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(65)))

        val tituloMenu = TextView(this).apply {
            text = "WOLF MENU"
            setTextColor(Color.WHITE)
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }
        cabecalho.addView(tituloMenu, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        botaoFecharMenu = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
            setOnFocusChangeListener { _, foco -> background = criarFundoCard(foco) }
            setOnClickListener { fecharMenu() }
        }
        cabecalho.addView(botaoFecharMenu, LinearLayout.LayoutParams(dp(55), dp(48)))

        menuScroll = ScrollView(this).apply { isFocusable = false }
        menuConteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(5), dp(10), dp(20))
        }

        menuScroll.addView(
            menuConteudo,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        menuLateral.addView(
            menuScroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        adicionarItemMenu("🔄  Atualizar catálogo") {
            fecharMenu()
            Toast.makeText(this, "Atualizando catálogo...", Toast.LENGTH_SHORT).show()
            carregarCatalogo()
        }

        adicionarItemMenu("▶  Continuar assistindo") {
            fecharMenu()
            Toast.makeText(this, "Continue assistindo", Toast.LENGTH_SHORT).show()
        }

        adicionarItemMenu("★  Favoritos (${favoritos.size})") {
            fecharMenu()
            val lista = filmes.filter { favoritos.contains(it.titulo) }
            mostrarListaCards(lista)
        }

        adicionarItemMenu("⌕  Pesquisa") { abrirPesquisa() }

        adicionarTituloMenu("FILMES")
        adicionarItemMenu("🎬  Todos os filmes (${filmes.size})") {
            fecharMenu()
            mostrarListaCards(filmes)
        }

        val categoriasFilmes = listOf("Ação", "Aventura", "Animação", "Comédia", "Drama", "Terror", "Ficção")
        val iconesFilmes = listOf("🔥", "🏹", "🧸", "😂", "🎭", "👻", "🚀")

        categoriasFilmes.forEachIndexed { i, cat ->
            val conta = filmes.count { normalizarTexto(it.categoria) == normalizarTexto(cat) }
            adicionarItemMenu("${iconesFilmes[i]}  $cat ($conta)") {
                fecharMenu()
                mostrarListaCards(filmes.filter { normalizarTexto(it.categoria) == normalizarTexto(cat) })
            }
        }

        adicionarTituloMenu("SÉRIES")
        adicionarItemMenu("📺  Todas as séries (${series.size})") {
            fecharMenu()
            mostrarListaSeries(series)
        }

        val categoriasSeries = listOf("Ação", "Aventura", "Comédia", "Drama", "Terror")
        val iconesSeries = listOf("🔥", "🏹", "😂", "🎭", "👻")

        categoriasSeries.forEachIndexed { i, cat ->
            val conta = series.count { normalizarTexto(it.categoria) == normalizarTexto(cat) }
            adicionarItemMenu("${iconesSeries[i]}  $cat ($conta)") {
                fecharMenu()
                mostrarListaSeries(series.filter { normalizarTexto(it.categoria) == normalizarTexto(cat) })
            }
        }

        adicionarTituloMenu("DORAMAS")
        adicionarItemMenu("📺  Todos os Doramas (${doramas.size})") {
            fecharMenu()
            mostrarListaSeries(doramas)
        }

        val categoriasDoramas = listOf("Romance", "Ação", "Comédia", "Terror")
        val iconesDoramas = listOf("💖", "🔥", "😂", "👻")

        categoriasDoramas.forEachIndexed { i, cat ->
            val conta = doramas.count { normalizarTexto(it.categoria) == normalizarTexto(cat) }
            adicionarItemMenu("${iconesDoramas[i]}  $cat ($conta)") {
                fecharMenu()
                mostrarListaSeries(doramas.filter { normalizarTexto(it.categoria) == normalizarTexto(cat) })
            }
        }

        adicionarTituloMenu("ANIME")
        adicionarItemMenu("🍥  Todos os Animes (${animes.size})") {
            fecharMenu()
            mostrarListaSeries(animes)
        }

        val categoriasAnimes = listOf("Ação", "Comédia", "Terror")
        val iconesAnimes = listOf("🔥", "😂", "👻")

        categoriasAnimes.forEachIndexed { i, cat ->
            val conta = animes.count { normalizarTexto(it.categoria) == normalizarTexto(cat) }
            adicionarItemMenu("${iconesAnimes[i]}  $cat ($conta)") {
                fecharMenu()
                mostrarListaSeries(animes.filter { normalizarTexto(it.categoria) == normalizarTexto(cat) })
            }
        }

        botaoFecharMenu.requestFocus()
    }

    private fun adicionarTituloMenu(texto: String) {
        val titulo = TextView(this).apply {
            this.text = texto
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(dp(18), dp(18), dp(12), dp(8))
            isFocusable = false
        }
        menuConteudo.addView(titulo)
    }

    private fun fecharMenu() {
        menuAberto = false
        menuLateral.visibility = View.GONE
        botaoMenu.requestFocus()
    }

    private fun moverMenu(direcao: Int) {
        if (itensMenuFoco.isEmpty()) return

        var indice = itensMenuFoco.indexOfFirst { it.hasFocus() }
        if (indice < 0) indice = 0

        indice += direcao

        if (indice < 0) indice = itensMenuFoco.size - 1
        if (indice >= itensMenuFoco.size) indice = 0

        val proximo = itensMenuFoco[indice]
        proximo.requestFocus()
        ajustarScrollMenu(proximo)
    }

    private fun ajustarScrollMenu(view: View) {
        view.post { menuScroll.smoothScrollTo(0, view.top) }
    }

    private fun moverCard(direcao: Int) {
        if (cardsAtuais.isEmpty()) return

        var indice = cardsAtuais.indexOfFirst { it.hasFocus() }
        if (indice < 0) indice = indiceCardAtual

        indice += direcao

        if (indice < 0) indice = 0
        if (indice >= cardsAtuais.size) indice = cardsAtuais.size - 1

        indiceCardAtual = indice
        cardsAtuais[indice].requestFocus()
    }

    private fun abrirPesquisa() {
        val campo = EditText(this).apply {
            hint = "Digite o nome..."
            textSize = 20f
            setSingleLine(true)
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        Toast.makeText(this, "Use a busca pelo menu", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MENU -> {
                    if (menuAberto) fecharMenu() else abrirMenu()
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
                    } else if (cardsAtuais.any { it.hasFocus() }) {
                        val indiceAtual = cardsAtuais.indexOfFirst { it.hasFocus() }
                        if (indiceAtual >= 5) {
                            moverCard(-5)
                        } else {
                            botaoMenu.requestFocus()
                        }
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (menuAberto) {
                        moverMenu(1)
                        return true
                    } else if (botaoMenu.hasFocus() && cardsAtuais.isNotEmpty()) {
                        cardsAtuais[0].requestFocus()
                        return true
                    } else if (cardsAtuais.any { it.hasFocus() }) {
                        moverCard(5)
                        return true
                    }
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (menuAberto) {
                        fecharMenu()
                        return true
                    } else if (historicoConteudo.isNotEmpty()) {
                        val acaoVoltar = historicoConteudo.removeAt(historicoConteudo.size - 1)
                        acaoVoltar.invoke()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        executorImagens.shutdownNow()
        super.onDestroy()
    }
}
