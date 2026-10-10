
package com.wolf.iptv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.appcompat.app.AlertDialog
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

    private val ARQUIVOS_SERIES =
        listOf("series.json", "serie.json", "s%C3%A9rie.json")

    private val ARQUIVOS_DORAMAS =
        listOf("doramas.json", "Doramas.json")

    private val ARQUIVOS_ANIMES =
        listOf("animes.json", "anime.json", "Animes.json", "Anime.json")

    private val ARQUIVOS_DESENHOS =
        listOf("desenho.json", "desenhos.json", "Desenho.json", "Desenhos.json")

    private val REQ_VOZ = 100

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var botaoMenu: TextView

    private var menuLateral: LinearLayout? = null
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
            if (!exists()) mkdirs()
        }
    }

    private val historicoConteudo = mutableListOf<() -> Unit>()

    private val filmes = mutableListOf<Filme>()
    private val series = mutableListOf<Serie>()
    private val doramas = mutableListOf<Serie>()
    private val animes = mutableListOf<Serie>()
    private val desenhos = mutableListOf<Filme>()

    private var listaSeriesAtual: List<Serie> = emptyList()

    private val cardsAtuais = mutableListOf<View>()
    private var indiceCardAtual = 0

    // 2 colunas no celular, 3 em telas médias e 5 na TV.
    private var colunas = 5

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

        // CORREÇÃO: detecta TVs e mantém 5 colunas nelas.
        val tv = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_LEANBACK
        )

        val larguraDp = resources.configuration.screenWidthDp

        colunas = when {
            tv -> 5
            larguraDp < 600 -> 2
            larguraDp < 900 -> 3
            else -> 5
        }

        criarInterface()
        carregarCatalogo()
    }

    private fun normalizarTexto(texto: String): String {
        val temp = Normalizer.normalize(texto, Normalizer.Form.NFD)
        return temp.replace(
            "\\p{InCombiningDiacriticalMarks}+".toRegex(),
            ""
        ).lowercase().trim()
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
        botaoMenu.setOnClickListener { abrirMenu() }

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
        fundo.setColor(
            if (foco) Color.argb(235, 10, 10, 10)
            else Color.argb(190, 10, 10, 10)
        )
        fundo.cornerRadius = dp(8).toFloat()
        if (foco) fundo.setStroke(dp(4), Color.RED)
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(8).toFloat()
        borda.setStroke(dp(4), Color.RED)
        return borda
    }

    // ===== IMAGENS =====

    private fun carregarImagemFundoPrioritaria(
        url: String,
        imagem: ImageView
    ) {
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
        } catch (_: Exception) {
        }

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

            try {
                val arquivoCache = arquivoCacheImagem(url)

                if (arquivoCache.exists() && arquivoCache.length() > 0L) {
                    bitmap = decodificarImagemOtimizada(
                        arquivoCache,
                        larguraAlvo,
                        alturaAlvo
                    )
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

                        conexao.setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Android)"
                        )
                        conexao.setRequestProperty(
                            "Accept",
                            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                        )
                        conexao.setRequestProperty(
                            "Connection",
                            "keep-alive"
                        )
                        conexao.connect()

                        if (conexao.responseCode !in 200..299) {
                            throw Exception("HTTP ${conexao.responseCode}")
                        }

                        val arquivoTemporario = File(
                            pastaCacheImagens,
                            "${arquivoCache.name}.tmp"
                        )

                        try {
                            conexao.inputStream.buffered(128 * 1024).use { input ->
                                arquivoTemporario.outputStream()
                                    .buffered(128 * 1024).use { output ->
                                        input.copyTo(output, 128 * 1024)
                                    }
                            }

                            if (
                                arquivoTemporario.exists() &&
                                arquivoTemporario.length() > 0L
                            ) {
                                if (arquivoCache.exists()) arquivoCache.delete()
                                arquivoTemporario.renameTo(arquivoCache)

                                bitmap = decodificarImagemOtimizada(
                                    arquivoCache,
                                    larguraAlvo,
                                    alturaAlvo
                                )
                            }
                        } finally {
                            if (arquivoTemporario.exists()) {
                                arquivoTemporario.delete()
                            }
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

    // ===== CATÁLOGO =====

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

                if (conexao.responseCode !in 200..299) {
                    throw Exception("HTTP ${conexao.responseCode}")
                }

                val resposta = conexao.inputStream
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }

                if (resposta.isBlank()) throw Exception("Catálogo vazio")

                val raizJson = JSONObject(resposta.trim())

                val novosFilmes = ArrayList<Filme>()
                val listaFilmes = raizJson.optJSONArray("filmes")

                if (listaFilmes != null) {
                    for (i in 0 until listaFilmes.length()) {
                        try {
                            val item = listaFilmes.getJSONObject(i)
                            val titulo = item.optString("titulo", "")

                            if (titulo.isNotBlank()) {
                                novosFilmes.add(
                                    Filme(
                                        titulo,
                                        item.optInt("ano", 0),
                                        item.optString("categoria", ""),
                                        item.optString("capa", ""),
                                        item.optString("video", "")
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                novosFilmes.sortWith(
                    compareByDescending<Filme> { it.ano }
                        .thenBy { it.titulo.lowercase() }
                )

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
                                        val objetoTemporada =
                                            arrayTemporadas.getJSONObject(t)

                                        val numeroTemporada =
                                            objetoTemporada.optInt("numero", t + 1)

                                        val episodios = ArrayList<Episodio>()
                                        val arrayEpisodios =
                                            objetoTemporada.optJSONArray("episodios")

                                        if (arrayEpisodios != null) {
                                            for (e in 0 until arrayEpisodios.length()) {
                                                try {
                                                    val objetoEpisodio =
                                                        arrayEpisodios.getJSONObject(e)

                                                    episodios.add(
                                                        Episodio(
                                                            numero = objetoEpisodio.optInt(
                                                                "numero",
                                                                e + 1
                                                            ),
                                                            titulo = objetoEpisodio.optString(
                                                                "titulo",
                                                                ""
                                                            ),
                                                            video = objetoEpisodio.optString(
                                                                "video",
                                                                ""
                                                            )
                                                        )
                                                    )
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }

                                        temporadas.add(
                                            Temporada(
                                                numeroTemporada,
                                                episodios
                                            )
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }

                            if (titulo.isNotBlank()) {
                                resultado.add(
                                    Serie(titulo, categoria, capa, temporadas)
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }

                    return resultado
                }

                fun lerDesenhos(texto: String?): ArrayList<Filme> {
                    val resultado = ArrayList<Filme>()
                    if (texto.isNullOrBlank()) return resultado

                    val array = extrairArray(texto, "desenhos")
                        ?: extrairArray(texto, "desenho")
                        ?: return resultado

                    for (i in 0 until array.length()) {
                        try {
                            val item = array.getJSONObject(i)
                            val titulo = item.optString("titulo", "")

                            if (titulo.isNotBlank()) {
                                resultado.add(
                                    Filme(
                                        titulo,
                                        item.optInt("ano", 0),
                                        item.optString("categoria", ""),
                                        item.optString("capa", ""),
                                        item.optString("video", "")
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }

                    resultado.sortWith(
                        compareByDescending<Filme> { it.ano }
                            .thenBy { it.titulo.lowercase() }
                    )

                    return resultado
                }

                val textoSeries = baixarPrimeiro(ARQUIVOS_SERIES)
                val textoDoramas = baixarPrimeiro(ARQUIVOS_DORAMAS)
                val textoAnimes = baixarPrimeiro(ARQUIVOS_ANIMES)
                val textoDesenhos = baixarPrimeiro(ARQUIVOS_DESENHOS)

                val novasSeries = lerSeries(
                    extrairArray(textoSeries, "series")
                        ?: raizJson.optJSONArray("series")
                )

                val novosDoramas = lerSeries(
                    extrairArray(textoDoramas, "doramas")
                        ?: raizJson.optJSONArray("doramas")
                )

                val novosAnimes = lerSeries(
                    extrairArray(textoAnimes, "animes")
                        ?: raizJson.optJSONArray("animes")
                )

                val novosDesenhos = lerDesenhos(textoDesenhos)

                withContext(Dispatchers.Main) {
                    filmes.clear()
                    filmes.addAll(novosFilmes)

                    series.clear()
                    series.addAll(novasSeries)

                    doramas.clear()
                    doramas.addAll(novosDoramas)

                    animes.clear()
                    animes.addAll(novosAnimes)

                    desenhos.clear()
                    desenhos.addAll(novosDesenhos)

                    mostrarListaCards(filmes)

                    Toast.makeText(
                        this@MainActivity,
                        "Catálogo carregado: ${filmes.size} filmes, " +
                            "${series.size} séries, ${doramas.size} doramas, " +
                            "${animes.size} animes e ${desenhos.size} desenhos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (erro: Exception) {
                withContext(Dispatchers.Main) {
                    filmes.clear()
                    series.clear()
                    doramas.clear()
                    animes.clear()
                    desenhos.clear()
                    conteudo.removeAllViews()

                    val erroTexto = TextView(this@MainActivity).apply {
                        text = "ERRO NO CATÁLOGO\n\n" +
                            (erro.message ?: "Erro desconhecido")
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
                conexao.inputStream.bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
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

    // Aceita { "series": [ ... ] } ou [ ... ].
    private fun extrairArray(texto: String?, chave: String): JSONArray? {
        if (texto.isNullOrBlank()) return null
        val limpo = texto.trim()

        return try {
            if (limpo.startsWith("[")) {
                JSONArray(limpo)
            } else {
                JSONObject(limpo).optJSONArray(chave)
            }
        } catch (_: Exception) {
            null
        }
    }

    // ===== GRADE / CARDS =====

    private fun limparConteudo() {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        indiceCardAtual = 0
    }

    private fun mostrarMensagemVazia(texto: String) {
        val vazio = TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }

        conteudo.addView(
            vazio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(100)
            )
        )
    }

    private fun montarGrade(cards: List<View>) {
        cards.chunked(colunas).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            fun parametros() = LinearLayout.LayoutParams(
                0,
                dp(335),
                1f
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
                bottomMargin = dp(12)
            }

            grupo.forEach { card ->
                linha.addView(card, parametros())
                cardsAtuais.add(card)
            }

            repeat(colunas - grupo.size) {
                linha.addView(View(this), parametros())
            }

            conteudo.addView(
                linha,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )
            )
        }

        cardsAtuais.firstOrNull()?.requestFocus()
    }

    private fun rodapeDuasLinhas(
        linha1: String,
        linha2: String
    ): View {
        val informacoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(8), dp(3))
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val titulo = TextView(this).apply {
            text = linha1
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val detalhes = TextView(this).apply {
            text = linha2
            setTextColor(Color.LTGRAY)
            textSize = 12f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        informacoes.addView(
            detalhes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        return informacoes
    }

    private fun criarCardBase(
        capa: String,
        rodape: View
    ): Pair<FrameLayout, ImageView> {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        carregarImagem(capa, imagem)

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            )
        )

        card.addView(
            rodape,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {
                gravity = Gravity.BOTTOM
            }
        )

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)

            if (foco) {
                val indice = cardsAtuais.indexOf(card)
                if (indice >= 0) indiceCardAtual = indice
            }
        }

        return Pair(card, imagem)
    }

    private fun mostrarListaCards(lista: List<Filme>) {
        limparConteudo()

        if (lista.isEmpty()) {
            mostrarMensagemVazia("Nenhum filme encontrado")
            return
        }

        montarGrade(lista.map { criarCard(it) })
    }

    private fun criarCard(filme: Filme): View {
        val (card, _) = criarCardBase(
            filme.capa,
            rodapeDuasLinhas(
                filme.titulo,
                if (filme.ano > 0) {
                    "${filme.ano} • ${filme.categoria}"
                } else {
                    filme.categoria
                }
            )
        )

        card.setOnClickListener {
            abrirVideo(filme.titulo, filme.video, filme.capa)
        }

        return card
    }

    private fun mostrarListaSeries(lista: List<Serie>) {
        listaSeriesAtual = lista
        limparConteudo()

        if (lista.isEmpty()) {
            mostrarMensagemVazia("Nenhuma série encontrada")
            return
        }

        montarGrade(lista.map { criarCardSerie(it) })
    }

    private fun criarCardSerie(serie: Serie): View {
        val (card, _) = criarCardBase(
            serie.capa,
            rodapeDuasLinhas(
                serie.titulo,
                "${serie.categoria} • ${serie.temporadas.size} temporada(s)"
            )
        )

        card.setOnClickListener {
            val origem = listaSeriesAtual
            historicoConteudo.add { mostrarListaSeries(origem) }
            mostrarTemporadas(serie)
        }

        return card
    }

    private fun mostrarTemporadas(serie: Serie) {
        limparConteudo()
        montarGrade(serie.temporadas.map { criarCardTemporada(serie, it) })
    }

    private fun criarCardTemporada(
        serie: Serie,
        temporada: Temporada
    ): View {
        val rodape = TextView(this).apply {
            text = "Temporada ${temporada.numero}"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val (card, _) = criarCardBase(serie.capa, rodape)

        card.setOnClickListener {
            historicoConteudo.add { mostrarTemporadas(serie) }
            mostrarEpisodios(serie, temporada)
        }

        return card
    }

    private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {
        limparConteudo()

        montarGrade(
            temporada.episodios.map {
                criarCardEpisodio(serie, temporada.numero, it)
            }
        )
    }

    // ===== EPISÓDIOS ASSISTIDOS =====

    private val prefsWolf by lazy {
        getSharedPreferences("wolf_prefs", MODE_PRIVATE)
    }

    private fun chaveEpisodio(
        serie: Serie,
        numeroTemporada: Int,
        numeroEpisodio: Int
    ): String {
        return "${serie.titulo}|T$numeroTemporada|E$numeroEpisodio"
    }

    private fun episodioAssistido(chave: String): Boolean {
        return prefsWolf.getStringSet("assistidos", emptySet())
            ?.contains(chave) == true
    }

    private fun marcarEpisodio(
        chave: String,
        assistido: Boolean
    ) {
        val novo = HashSet(
            prefsWolf.getStringSet("assistidos", emptySet()) ?: emptySet()
        )

        if (assistido) novo.add(chave) else novo.remove(chave)

        prefsWolf.edit().putStringSet("assistidos", novo).apply()
    }

    private fun criarCardEpisodio(
        serie: Serie,
        numeroTemporada: Int,
        episodio: Episodio
    ): View {
        val chave = chaveEpisodio(
            serie,
            numeroTemporada,
            episodio.numero
        )

        val textoBase = "EP ${episodio.numero} • ${episodio.titulo}"

        val informacoes = TextView(this).apply {
            text = textoBase
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(3), dp(8), dp(3))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val (card, imagem) = criarCardBase(serie.capa, informacoes)

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
            if (episodio.video.isNotBlank()) {
                marcarEpisodio(chave, true)
                atualizarVisual()
            }

            abrirVideo(
                "${serie.titulo} - EP ${episodio.numero}",
                episodio.video,
                serie.capa
            )
        }

        card.setOnLongClickListener {
            val agora = !episodioAssistido(chave)
            marcarEpisodio(chave, agora)
            atualizarVisual()

            Toast.makeText(
                this,
                if (agora) "Marcado como assistido"
                else "Marcado como não assistido",
                Toast.LENGTH_SHORT
            ).show()

            true
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

        WolfProgress.registrar(this, video, titulo, capa)
        val pos = WolfProgress.posicao(this, video)

        if (pos <= 0L) {
            iniciarPlayer(titulo, video, capa, 0L)
            return
        }

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage("Você parou em ${WolfProgress.formatar(pos)}.")
            .setPositiveButton("Continuar") { _, _ ->
                iniciarPlayer(titulo, video, capa, pos)
            }
            .setNegativeButton("Voltar do início") { _, _ ->
                WolfProgress.apagar(this, video)
                WolfProgress.registrar(this, video, titulo, capa)
                iniciarPlayer(titulo, video, capa, 0L)
            }
            .show()
    }

    private fun iniciarPlayer(
        titulo: String,
        video: String,
        capa: String,
        posicaoMs: Long
    ) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URL", video)
            putExtra("VIDEO_TITLE", titulo)
            putExtra("VIDEO_COVER", capa)
            putExtra("START_POSITION", posicaoMs)
        }

        startActivity(intent)
    }

    private fun mostrarContinuarAssistindo() {
        val itens = WolfProgress.emAndamento(this)
        limparConteudo()

        if (itens.isEmpty()) {
            mostrarMensagemVazia("Nada em andamento ainda")
            return
        }

        val filmesEmAndamento = itens.map {
            Filme(it.titulo, 0, it.resumo(), it.capa, it.url)
        }

        montarGrade(filmesEmAndamento.map { criarCard(it) })
    }

    // ===== MENU LATERAL =====

    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {
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

            setOnFocusChangeListener { _, foco ->
                background = criarFundoCard(foco)
            }

            setOnClickListener { acao() }
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

    private fun adicionarCategoriasFilmes(
        categorias: List<String>,
        icones: List<String>,
        base: List<Filme>
    ) {
        categorias.forEachIndexed { i, cat ->
            val filtrados = base.filter {
                normalizarTexto(it.categoria) == normalizarTexto(cat)
            }

            adicionarItemMenu("${icones[i]}  $cat (${filtrados.size})") {
                fecharMenu()
                mostrarListaCards(filtrados)
            }
        }
    }

    private fun adicionarCategoriasSeries(
        categorias: List<String>,
        icones: List<String>,
        base: List<Serie>
    ) {
        categorias.forEachIndexed { i, cat ->
            val filtradas = base.filter {
                normalizarTexto(it.categoria) == normalizarTexto(cat)
            }

            adicionarItemMenu("${icones[i]}  $cat (${filtradas.size})") {
                fecharMenu()
                mostrarListaSeries(filtradas)
            }
        }
    }

    private fun abrirMenu() {
        if (menuAberto) return

        menuAberto = true
        itensMenuFoco.clear()

        menuLateral?.let { raiz.removeView(it) }

        val larguraMenu = minOf(
            dp(360),
            (resources.displayMetrics.widthPixels * 0.85f).toInt()
        )

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(250, 5, 5, 5))
            isClickable = true
            elevation = dp(16).toFloat()
        }

        menuLateral = menu

        raiz.addView(
            menu,
            FrameLayout.LayoutParams(
                larguraMenu,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START or Gravity.TOP
            }
        )

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(8), dp(10), dp(8))
        }

        menu.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val tituloMenu = TextView(this).apply {
            text = "WOLF MENU"
            setTextColor(Color.WHITE)
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

        cabecalho.addView(
            tituloMenu,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        botaoFecharMenu = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)

            setOnFocusChangeListener { _, foco ->
                background = criarFundoCard(foco)
            }

            setOnClickListener { fecharMenu() }
        }

        cabecalho.addView(
            botaoFecharMenu,
            LinearLayout.LayoutParams(dp(55), dp(48))
        )

        itensMenuFoco.add(botaoFecharMenu)

        menuScroll = ScrollView(this).apply {
            isFocusable = false
        }

        menuConteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(5), dp(10), dp(20))
        }

        menuScroll.addView(
            menuConteudo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        menu.addView(
            menuScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        adicionarItemMenu("🔄  Atualizar catálogo") {
            fecharMenu()
            Toast.makeText(
                this,
                "Atualizando catálogo...",
                Toast.LENGTH_SHORT
            ).show()
            carregarCatalogo()
        }

        adicionarItemMenu(
            "▶  Continuar assistindo (${WolfProgress.emAndamento(this).size})"
        ) {
            fecharMenu()
            mostrarContinuarAssistindo()
        }

        adicionarItemMenu("★  Favoritos (${favoritos.size})") {
            fecharMenu()
            mostrarListaCards(
                filmes.filter { favoritos.contains(it.titulo) }
            )
        }

        adicionarItemMenu("⌕  Pesquisa") {
            abrirPesquisa()
        }

        adicionarTituloMenu("FILMES")

        adicionarItemMenu("🎬  Todos os filmes (${filmes.size})") {
            fecharMenu()
            mostrarListaCards(filmes)
        }

        adicionarCategoriasFilmes(
            listOf(
                "Ação",
                "Aventura",
                "Animação",
                "Comédia",
                "Drama",
                "Terror",
                "Ficção"
            ),
            listOf("🔥", "🏹", "🧸", "😂", "🎭", "👻", "🚀"),
            filmes
        )

        adicionarTituloMenu("SÉRIES")

        adicionarItemMenu("📺  Todas as séries (${series.size})") {
            fecharMenu()
            mostrarListaSeries(series)
        }

        adicionarCategoriasSeries(
            listOf("Ação", "Aventura", "Comédia", "Drama", "Terror"),
            listOf("🔥", "🏹", "😂", "🎭", "👻"),
            series
        )

        adicionarTituloMenu("DORAMAS")

        adicionarItemMenu("📺  Todos os Doramas (${doramas.size})") {
            fecharMenu()
            mostrarListaSeries(doramas)
        }

        adicionarCategoriasSeries(
            listOf("Romance", "Ação", "Comédia", "Terror"),
            listOf("💖", "🔥", "😂", "👻"),
            doramas
        )

        adicionarTituloMenu("DESENHOS")

        adicionarItemMenu("🧸  Todos os desenhos (${desenhos.size})") {
            fecharMenu()
            mostrarListaCards(desenhos)
        }

        adicionarCategoriasFilmes(
            listOf(
                "Ação",
                "Aventura",
                "Animação",
                "Comédia",
                "Drama",
                "Terror",
                "Fantasia"
            ),
            listOf("🔥", "🏹", "🧸", "😂", "🎭", "👻", "✨"),
            desenhos
        )

        adicionarTituloMenu("ANIME")

        adicionarItemMenu("🍥  Todos os Animes (${animes.size})") {
            fecharMenu()
            mostrarListaSeries(animes)
        }

        adicionarCategoriasSeries(
            listOf("Ação", "Comédia", "Terror"),
            listOf("🔥", "😂", "👻"),
            animes
        )

        botaoFecharMenu.requestFocus()
    }

    private fun fecharMenu() {
        if (!menuAberto) return

        menuAberto = false
        menuLateral?.let { raiz.removeView(it) }
        menuLateral = null
        itensMenuFoco.clear()
        botaoMenu.requestFocus()
    }

    private fun moverMenu(direcao: Int) {
        if (itensMenuFoco.isEmpty()) return

        val atual = itensMenuFoco.indexOfFirst { it.hasFocus() }
        var indice = if (atual < 0) 0 else atual + direcao

        if (indice < 0) indice = itensMenuFoco.size - 1
        if (indice >= itensMenuFoco.size) indice = 0

        val proximo = itensMenuFoco[indice]
        proximo.requestFocus()
        ajustarScrollMenu(proximo)
    }

    private fun ajustarScrollMenu(view: View) {
        if (view === botaoFecharMenu) {
            menuScroll.post {
                menuScroll.smoothScrollTo(0, 0)
            }
        } else {
            view.post {
                menuScroll.smoothScrollTo(0, view.top)
            }
        }
    }

    // CORREÇÃO: navegação respeita linhas e colunas da grade.
    private fun moverCard(direcao: Int) {
        if (cardsAtuais.isEmpty()) return

        var atual = cardsAtuais.indexOfFirst { it.hasFocus() }

        if (atual < 0) {
            atual = indiceCardAtual.coerceIn(0, cardsAtuais.lastIndex)
        }

        val total = cardsAtuais.size
        val colunaAtual = atual % colunas

        val proximo = when (direcao) {
            -1 -> {
                if (colunaAtual > 0) atual - 1 else atual
            }

            1 -> {
                if (colunaAtual < colunas - 1 && atual + 1 < total) {
                    atual + 1
                } else {
                    atual
                }
            }

            -colunas -> {
                if (atual >= colunas) atual - colunas else atual
            }

            colunas -> {
                if (atual + colunas < total) atual + colunas else atual
            }

            else -> atual
        }

        indiceCardAtual = proximo

        val card = cardsAtuais[proximo]
        card.requestFocus()

        // Mantém o card focado visível dentro da área de rolagem.
        card.post {
            if (card.isAttachedToWindow) {
                card.requestRectangleOnScreen(
                    android.graphics.Rect(
                        0,
                        0,
                        card.width,
                        card.height
                    ),
                    true
                )
            }
        }
    }

    // ===== PESQUISA (TEXTO E VOZ) =====

    private fun abrirPesquisa() {
        fecharMenu()

        val campo = EditText(this).apply {
            hint = "Digite o nome..."
            textSize = 20f
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Pesquisar")
            .setView(campo)
            .setPositiveButton("Buscar") { _, _ ->
                pesquisar(campo.text.toString())
            }
            .setNeutralButton("🎤 Voz") { _, _ ->
                iniciarVoz()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pesquisar(consulta: String) {
        val termo = normalizarTexto(consulta)
        if (termo.isBlank()) return

        val achadosFilmes = (filmes + desenhos).filter {
            normalizarTexto(it.titulo).contains(termo)
        }

        val achadasSeries = (series + doramas + animes).filter {
            normalizarTexto(it.titulo).contains(termo)
        }

        Toast.makeText(
            this,
            "Busca: \"$consulta\" — ${achadosFilmes.size} filme(s), " +
                "${achadasSeries.size} série(s)",
            Toast.LENGTH_SHORT
        ).show()

        if (achadosFilmes.isNotEmpty()) {
            mostrarListaCards(achadosFilmes)
        } else {
            mostrarListaSeries(achadasSeries)
        }
    }

    private fun iniciarVoz() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "pt-BR"
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Diga o nome do filme ou série"
            )
        }

        try {
            startActivityForResult(intent, REQ_VOZ)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Voz indisponível neste aparelho, digite o nome",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_VOZ && resultCode == RESULT_OK) {
            val texto = data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!texto.isNullOrBlank()) pesquisar(texto)
        }
    }

    // ===== CONTROLE REMOTO =====

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
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
                    if (!menuAberto && cardsAtuais.any { it.hasFocus() }) {
                        moverCard(-1)
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!menuAberto && cardsAtuais.any { it.hasFocus() }) {
                        moverCard(1)
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (menuAberto) {
                        moverMenu(-1)
                        return true
                    } else if (cardsAtuais.any { it.hasFocus() }) {
                        val indiceAtual =
                            cardsAtuais.indexOfFirst { it.hasFocus() }

                        if (indiceAtual >= colunas) {
                            moverCard(-colunas)
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
                    } else if (
                        botaoMenu.hasFocus() &&
                        cardsAtuais.isNotEmpty()
                    ) {
                        cardsAtuais[0].requestFocus()
                        return true
                    } else if (cardsAtuais.any { it.hasFocus() }) {
                        moverCard(colunas)
                        return true
                    }
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (menuAberto) {
                        fecharMenu()
                        return true
                    } else if (historicoConteudo.isNotEmpty()) {
                        val acaoVoltar =
                            historicoConteudo.removeAt(
                                historicoConteudo.size - 1
                            )

                        acaoVoltar.invoke()
                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        activityScope.coroutineContext[Job]?.cancel()
        executorImagens.shutdownNow()
        super.onDestroy()
    }
}
