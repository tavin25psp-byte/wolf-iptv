
package com.wolf.iptv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.InputType
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    // Cache limitado por memória (antes crescia sem limite e derrubava o app).
    private val cacheCapas = object : android.util.LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 6).toInt().coerceAtLeast(8 * 1024 * 1024)
    ) {
        override fun sizeOf(chave: String, valor: Bitmap): Int = valor.byteCount
    }
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

        // Guarda o erro que fechou o app, para mostrar na próxima abertura.
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->
            try {
                val texto = android.util.Log.getStackTraceString(erro)
                getSharedPreferences("wolf_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("ultimo_erro", "[${thread.name}]\n$texto")
                    .commit()
            } catch (_: Throwable) {
            }
            anterior?.uncaughtException(thread, erro)
        }

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
        verificarAssinatura()   // só carrega o catálogo se o código estiver válido
        mostrarUltimoErro()

        // A cada 2 minutos confere novidades; a cada 10, a assinatura.
        activityScope.launch {
            var ciclo = 0
            while (true) {
                delay(120_000L)
                ciclo++
                if (catalogoIniciado) verificarNovidades()
                if (ciclo % 5 == 0) verificarAssinatura()
            }
        }
    }

    private fun mostrarUltimoErro() {
        try {
            val prefs = getSharedPreferences("wolf_prefs", MODE_PRIVATE)
            val erro = prefs.getString("ultimo_erro", null) ?: return
            prefs.edit().remove("ultimo_erro").apply()

            val resumo = erro.lines().filter {
                it.isNotBlank() &&
                    (!it.trimStart().startsWith("at ") ||
                        it.contains("com.wolf.iptv"))
            }.take(14).joinToString("\n")

            AlertDialog.Builder(this)
                .setTitle("O app fechou por este erro")
                .setMessage(resumo)
                .setPositiveButton("OK", null)
                .show()
        } catch (_: Throwable) {
        }
    }

    // Regex compilada uma vez só e resultados guardados: o menu chama isso
    // milhares de vezes e, sem isso, travava TVs mais fracas.
    private val REGEX_ACENTOS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val cacheNormalizado = ConcurrentHashMap<String, String>()

    private fun normalizarTexto(texto: String): String {
        cacheNormalizado[texto]?.let { return it }

        val resultado = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(REGEX_ACENTOS, "")
            .lowercase()
            .trim()

        if (cacheNormalizado.size < 20000) cacheNormalizado[texto] = resultado
        return resultado
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
        fundo.cornerRadius = dp(14).toFloat()
        if (foco) fundo.setStroke(dp(4), Color.RED)
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(14).toFloat()
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
                    resources.displayMetrics.heightPixels,
                    true
                )
                if (bitmap != null) {
                    cacheCapas.put(url, bitmap)
                    imagem.setImageBitmap(bitmap)
                    return
                }
            }
        } catch (_: Throwable) {
        }

        carregarImagem(
            url,
            imagem,
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels,
            true
        )
    }

    // Várias telas podem pedir a mesma capa ao mesmo tempo (capas repetidas).
    // Antes só a primeira recebia a imagem; agora todas são atendidas.
    private val aguardandoImagem = HashMap<String, MutableList<ImageView>>()
    private val executorPrecarga = Executors.newFixedThreadPool(3)

    // Baixa a capa para o cache em disco (com novas tentativas). Retorna o arquivo ou null.
    private fun baixarCapaParaCache(url: String): File? {
        val arquivoCache = arquivoCacheImagem(url)
        if (arquivoCache.exists() && arquivoCache.length() > 0L) return arquivoCache

        for (tentativa in 1..3) {
            var conexao: HttpURLConnection? = null
            val temporario = File(
                pastaCacheImagens,
                "${arquivoCache.name}.${Thread.currentThread().id}.tmp"
            )
            try {
                conexao = URL(url).openConnection() as HttpURLConnection
                conexao.connectTimeout = 10000
                conexao.readTimeout = 20000
                conexao.doInput = true
                conexao.instanceFollowRedirects = true
                conexao.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                conexao.setRequestProperty(
                    "Accept",
                    "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
                )
                conexao.connect()

                if (conexao.responseCode !in 200..299) {
                    throw Exception("HTTP ${conexao.responseCode}")
                }

                conexao.inputStream.buffered(64 * 1024).use { entrada ->
                    temporario.outputStream().buffered(64 * 1024).use { saida ->
                        entrada.copyTo(saida, 64 * 1024)
                    }
                }

                if (temporario.exists() && temporario.length() > 0L) {
                    if (arquivoCache.exists()) arquivoCache.delete()
                    if (temporario.renameTo(arquivoCache)) return arquivoCache
                }
            } catch (_: Throwable) {
            } finally {
                conexao?.disconnect()
                if (temporario.exists()) temporario.delete()
            }

            try {
                Thread.sleep(700L * tentativa)
            } catch (_: InterruptedException) {
                return null
            }
        }
        return null
    }

    // Baixa todas as capas em segundo plano, para abrirem rápido depois.
    private fun precarregarCapas(urls: List<String>) {
        urls.filter { it.isNotBlank() }.distinct().forEach { url ->
            try {
                executorPrecarga.execute {
                    try {
                        baixarCapaParaCache(url)
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun carregarImagem(
        url: String,
        imagem: ImageView,
        larguraAlvo: Int = 0,
        alturaAlvo: Int = 0,
        leve: Boolean = false
    ) {
        if (url.isBlank()) return
        imagem.tag = url

        // Tamanho do card (nítido, sem decodificar a imagem grande inteira).
        // 1,5x o tamanho do card: dá nitidez mesmo com a TV ampliando a imagem.
        val largAlvo = if (larguraAlvo > 0) larguraAlvo
            else (resources.displayMetrics.widthPixels / colunas.coerceAtLeast(1)) * 3 / 2
        val altAlvo = if (alturaAlvo > 0) alturaAlvo else dp(275) * 3 / 2

        cacheCapas[url]?.let { bitmap ->
            imagem.setImageBitmap(bitmap)
            return
        }

        synchronized(aguardandoImagem) {
            val fila = aguardandoImagem[url]
            if (fila != null) {
                fila.add(imagem)
                return
            }
            aguardandoImagem[url] = mutableListOf(imagem)
        }

        executorImagens.execute {
            var bitmap: Bitmap? = null

            try {
                val arquivo = baixarCapaParaCache(url)
                if (arquivo != null) {
                    bitmap = decodificarImagemOtimizada(arquivo, largAlvo, altAlvo, leve)
                    // Arquivo corrompido: apaga para baixar de novo na próxima vez.
                    if (bitmap == null) arquivo.delete()
                }
                bitmap?.let { cacheCapas.put(url, it) }
            } catch (_: Throwable) {
            } finally {
                val telas = synchronized(aguardandoImagem) {
                    aguardandoImagem.remove(url)
                } ?: mutableListOf()

                val pronta = bitmap
                if (pronta != null) {
                    runOnUiThread {
                        telas.forEach { if (it.tag == url) it.setImageBitmap(pronta) }
                    }
                }
            }
        }
    }

    private fun decodificarImagemOtimizada(
        arquivo: File,
        larguraAlvo: Int,
        alturaAlvo: Int,
        leve: Boolean = false
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
        leitura.inPreferredConfig =
            if (leve) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
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

    // Ano do JSON; se faltar, tenta achar um ano (1900-2099) no título ou no nome do vídeo
    private fun anoDoItem(item: JSONObject): Int {
        val ano = item.optInt("ano", 0)
        if (ano in 1900..2099) return ano
        val texto = item.optString("titulo", "") + " " + Uri.decode(item.optString("video", ""))
        return Regex("(?<!\\d)(19|20)\\d{2}(?!\\d)").find(texto)?.value?.toInt() ?: 0
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
                                        anoDoItem(item),
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
                                        anoDoItem(item),
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

                val novasColecoes = try {
                    calcularColecoes(novosFilmes)
                } catch (_: Throwable) {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    filmes.clear()
                    filmes.addAll(novosFilmes)
                    colecoesProntas = novasColecoes

                    series.clear()
                    series.addAll(novasSeries)

                    doramas.clear()
                    doramas.addAll(novosDoramas)

                    animes.clear()
                    animes.addAll(novosAnimes)

                    desenhos.clear()
                    desenhos.addAll(novosDesenhos)

                    assinaturasCarregadas = mapOf(
                        "filmes" to resposta.trim().hashCode(),
                        "séries" to (textoSeries?.trim()?.hashCode() ?: 0),
                        "doramas" to (textoDoramas?.trim()?.hashCode() ?: 0),
                        "animes" to (textoAnimes?.trim()?.hashCode() ?: 0),
                        "desenhos" to (textoDesenhos?.trim()?.hashCode() ?: 0)
                    )
                    novidades = emptyList()
                    aplicarNovidades()

                    mostrarListaCards(filmes)

                    precarregarCapas(
                        filmes.map { it.capa } +
                            desenhos.map { it.capa } +
                            series.map { it.capa } +
                            doramas.map { it.capa } +
                            animes.map { it.capa }
                    )

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

    // ===== ASSINATURA (CÓDIGO DO CLIENTE) =====
    // Cada cliente tem um código. O app confere em clientes/<hash do código>.json
    // no GitHub: {"ativo": true, "ate": "2026-12-31"}. Vencido, bloqueado ou
    // inexistente = o app não carrega o catálogo.

    // Mude para true quando quiser exigir o código de acesso dos clientes.
    private val EXIGIR_CODIGO = false

    private val DIAS_OFFLINE = 3
    private var dialogoCodigoAberto = false
    private var catalogoIniciado = false

    private enum class ResultadoCodigo { OK, VENCIDO, BLOQUEADO, INVALIDO, SEM_INTERNET }

    private fun normalizarCodigo(codigo: String): String =
        codigo.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    private fun nomeArquivoCodigo(codigo: String): String {
        val resumo = MessageDigest.getInstance("SHA-256")
            .digest(normalizarCodigo(codigo).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return resumo.take(16)
    }

    // Roda fora da tela principal (rede).
    private fun conferirCodigo(codigo: String): ResultadoCodigo {
        if (normalizarCodigo(codigo).length < 4) return ResultadoCodigo.INVALIDO

        var conexao: HttpURLConnection? = null
        return try {
            val url = BASE_URL + "clientes/" + nomeArquivoCodigo(codigo) +
                ".json?t=" + System.currentTimeMillis()

            conexao = URL(url).openConnection() as HttpURLConnection
            conexao.connectTimeout = 15000
            conexao.readTimeout = 15000
            conexao.requestMethod = "GET"
            conexao.setRequestProperty("User-Agent", "Mozilla/5.0")
            conexao.setRequestProperty("Cache-Control", "no-cache")
            conexao.connect()

            val status = conexao.responseCode
            if (status == 404) return ResultadoCodigo.INVALIDO
            if (status !in 200..299) return ResultadoCodigo.SEM_INTERNET

            val texto = conexao.inputStream
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(texto.trim())

            if (!json.optBoolean("ativo", false)) return ResultadoCodigo.BLOQUEADO

            val ate = json.optString("ate", "")
            val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (ate.isNotBlank() && hoje > ate) ResultadoCodigo.VENCIDO
            else ResultadoCodigo.OK
        } catch (_: Throwable) {
            ResultadoCodigo.SEM_INTERNET
        } finally {
            conexao?.disconnect()
        }
    }

    private fun liberarApp() {
        if (!catalogoIniciado) {
            catalogoIniciado = true
            carregarCatalogo()
        }
    }

    private fun bloquearApp(mensagem: String) {
        limparConteudo()
        mostrarMensagemVazia(mensagem)
        pedirCodigo(mensagem)
    }

    private fun verificarAssinatura() {
        if (!EXIGIR_CODIGO) {
            liberarApp()
            return
        }

        activityScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("wolf_prefs", MODE_PRIVATE)
            val codigo = prefs.getString("codigo", null)

            if (codigo.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    pedirCodigo("Digite o código de acesso que você recebeu.")
                }
                return@launch
            }

            var resultado = conferirCodigo(codigo)

            if (resultado == ResultadoCodigo.OK) {
                prefs.edit().putLong("ultima_ok", System.currentTimeMillis()).apply()
            } else if (resultado == ResultadoCodigo.SEM_INTERNET) {
                // Sem internet: deixa usar por alguns dias com a última validação.
                val ultima = prefs.getLong("ultima_ok", 0L)
                if (System.currentTimeMillis() - ultima <= DIAS_OFFLINE * 86400000L) {
                    resultado = ResultadoCodigo.OK
                }
            }

            withContext(Dispatchers.Main) {
                when (resultado) {
                    ResultadoCodigo.OK -> liberarApp()
                    ResultadoCodigo.VENCIDO ->
                        bloquearApp("Assinatura vencida. Fale com o suporte para renovar.")
                    ResultadoCodigo.BLOQUEADO ->
                        bloquearApp("Acesso bloqueado. Fale com o suporte.")
                    ResultadoCodigo.INVALIDO ->
                        bloquearApp("Código inválido. Confira e digite de novo.")
                    ResultadoCodigo.SEM_INTERNET ->
                        bloquearApp("Sem internet. Conecte a internet para validar seu acesso.")
                }
            }
        }
    }

    private fun pedirCodigo(mensagem: String) {
        if (dialogoCodigoAberto || isFinishing) return
        dialogoCodigoAberto = true

        val prefs = getSharedPreferences("wolf_prefs", MODE_PRIVATE)

        val campo = EditText(this).apply {
            hint = "XXXX-XXXX"
            textSize = 22f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(prefs.getString("codigo", "") ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("WOLF IPTV – Código de acesso")
            .setMessage(mensagem)
            .setView(campo)
            .setCancelable(false)
            .setPositiveButton("Entrar") { _, _ ->
                dialogoCodigoAberto = false
                prefs.edit()
                    .putString("codigo", campo.text.toString().trim())
                    .apply()
                verificarAssinatura()
            }
            .setNegativeButton("Sair") { _, _ ->
                dialogoCodigoAberto = false
                finish()
            }
            .show()
    }

    // ===== AVISO DE NOVIDADES NO "ATUALIZAR CATÁLOGO" =====

    private var assinaturasCarregadas: Map<String, Int> = emptyMap()
    private var novidades: List<String> = emptyList()
    private var itemAtualizar: TextView? = null
    private var verificandoNovidades = false

    private fun textoAtualizar(): String =
        if (novidades.isEmpty()) "🔄  Atualizar catálogo"
        else "🔔  Atualizar catálogo (novo!)"

    private fun aplicarNovidades() {
        botaoMenu.text =
            if (novidades.isEmpty()) "☰  WOLF MENU" else "☰  WOLF MENU  🔔"
        itemAtualizar?.text = textoAtualizar()
    }

    // Compara o catálogo da internet com o que já está carregado no app.
    private fun verificarNovidades() {
        if (verificandoNovidades) return
        verificandoNovidades = true

        activityScope.launch(Dispatchers.IO) {
            try {
                val textos = linkedMapOf(
                    "filmes" to baixarTexto(CATALOGO_URL),
                    "séries" to baixarPrimeiro(ARQUIVOS_SERIES),
                    "doramas" to baixarPrimeiro(ARQUIVOS_DORAMAS),
                    "animes" to baixarPrimeiro(ARQUIVOS_ANIMES),
                    "desenhos" to baixarPrimeiro(ARQUIVOS_DESENHOS)
                )

                // Sem internet: não avisa nada.
                if (textos["filmes"].isNullOrBlank()) return@launch

                val lista = ArrayList<String>()
                for ((nome, texto) in textos) {
                    if (texto.isNullOrBlank()) continue
                    val assinatura = texto.trim().hashCode()
                    if ((assinaturasCarregadas[nome] ?: 0) == assinatura) continue

                    if (nome == "filmes") {
                        val total = extrairArray(texto, "filmes")?.length() ?: 0
                        val diferenca = total - filmes.size
                        lista.add(if (diferenca > 0) "+$diferenca filmes" else "filmes")
                    } else {
                        lista.add(nome)
                    }
                }

                withContext(Dispatchers.Main) {
                    val eraVazio = novidades.isEmpty()
                    novidades = lista
                    aplicarNovidades()

                    if (lista.isNotEmpty() && eraVazio) {
                        Toast.makeText(
                            this@MainActivity,
                            "Novidades: ${lista.joinToString(", ")}. " +
                                "Abra o menu e toque em Atualizar catálogo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (_: Throwable) {
            } finally {
                verificandoNovidades = false
            }
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
            clipToOutline = true
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

    private val CATEGORIAS_MENU = listOf(
        "Ação" to "🔥",
        "Animação" to "🧸",
        "Aventura" to "🧭",
        "Comédia" to "😂",
        "Drama" to "🎭",
        "Fantasia" to "✨",
        "Ficção" to "🚀",
        "Suspense" to "🔪",
        "Terror" to "👻"
    )

    private class SecaoMenu(
        val titulo: String,
        val total: Int,
        val cabecalho: TextView,
        val corpo: LinearLayout
    )

    private val secoesMenu = mutableListOf<SecaoMenu>()

    private val COR_PAINEL = Color.argb(250, 12, 16, 38)
    private val COR_ITEM = Color.argb(255, 27, 32, 56)
    private val COR_SECAO = Color.argb(255, 44, 56, 108)

    private fun criarFundoMenu(
        cor: Int,
        foco: Boolean,
        raio: Int = 14
    ): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.setColor(cor)
        fundo.cornerRadius = dp(raio).toFloat()
        if (foco) fundo.setStroke(dp(3), Color.RED)
        return fundo
    }

    private fun mesmaCategoria(a: String, b: String): Boolean {
        return normalizarTexto(a) == normalizarTexto(b)
    }

    private fun adicionarItemMenu(
        texto: String,
        pai: LinearLayout = menuConteudo,
        cor: Int = COR_ITEM,
        acao: () -> Unit
    ): TextView {
        val item = TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(12), 0)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoMenu(cor, false)

            setOnFocusChangeListener { _, foco ->
                background = criarFundoMenu(cor, foco)
            }

            setOnClickListener { acao() }
        }

        pai.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(6)
            }
        )

        itensMenuFoco.add(item)
        return item
    }

    private fun textoSecao(titulo: String, total: Int, aberta: Boolean): String {
        return "$titulo ($total)   " + if (aberta) "▾" else "▸"
    }

    // Seção que abre e fecha ao clicar, com as mesmas categorias em todas.
    private fun adicionarSecaoCategorias(
        titulo: String,
        total: Int,
        aberta: Boolean,
        contar: (String) -> Int,
        mostrarTodos: () -> Unit,
        mostrarCategoria: (String) -> Unit
    ) {
        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (aberta) View.VISIBLE else View.GONE
        }

        val cabecalho = adicionarItemMenu(
            textoSecao(titulo, total, aberta),
            cor = COR_SECAO
        ) {
            alternarSecao(titulo)
        }

        cabecalho.setTypeface(null, Typeface.BOLD)

        menuConteudo.addView(
            corpo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        adicionarItemMenu("🎬  Todos ($total)", pai = corpo) {
            fecharMenu()
            mostrarTodos()
        }

        CATEGORIAS_MENU.forEach { (cat, icone) ->
            adicionarItemMenu("$icone  $cat (${contar(cat)})", pai = corpo) {
                fecharMenu()
                mostrarCategoria(cat)
            }
        }

        secoesMenu.add(SecaoMenu(titulo, total, cabecalho, corpo))
    }

    // ===== COLETÂNEAS (franquias) =====

    private class Colecao(
        val nome: String,
        val icone: String,
        val chaves: List<String>,
        val exclui: List<String> = emptyList()
    )

    // As chaves são comparadas com o título sem acento e em minúsculas.
    // Para incluir mais filmes numa coletânea, é só somar uma chave.
    private val COLECOES = listOf(
        Colecao("A Era do Gelo", "🧊", listOf("era do gelo")),
        Colecao("A Hora do Pesadelo", "😴", listOf("hora do pesadelo")),
        Colecao("Alien", "👽", listOf("alien:", "alien,", "aliens", "=alien", "alien vs", "alien x")),
        Colecao("Alvin e os Esquilos", "🐿️", listOf("alvin e os esquilos")),
        Colecao("American Pie", "🥧", listOf("american pie")),
        Colecao("Animais Fantásticos", "🐉", listOf("animais fantasticos")),
        Colecao("Anjos da Noite", "🐺", listOf("underworld", "anjos da noite")),
        Colecao("Aquaman", "🔱", listOf("aquaman")),
        Colecao("Aranhaverso", "🕷️", listOf("aranhaverso")),
        Colecao("As Crônicas de Nárnia", "🦁", listOf("narnia")),
        Colecao("Atividade Paranormal", "📹", listOf("atividade paranormal")),
        Colecao("Bad Boys", "🚔", listOf("bad boys")),
        Colecao("Barbie", "💖", listOf("barbie")),
        Colecao("Batman", "🦇", listOf("batman")),
        Colecao("Ben 10", "⌚", listOf("ben 10")),
        Colecao("Busca Implacável", "📞", listOf("busca implacavel")),
        Colecao("Caça-Fantasmas", "👻", listOf("caca-fantasmas", "ghostbusters")),
        Colecao("Capitão América", "🛡️", listOf("capitao america")),
        Colecao("Carros", "🚗", listOf("carros")),
        Colecao("Cemitério Maldito", "🪦", listOf("cemiterio maldito", "pet sematary")),
        Colecao("Chucky", "🪆", listOf("chucky", "brinquedo assassino")),
        Colecao("Cinquenta Tons", "🖤", listOf("cinquenta tons")),
        Colecao("Como Treinar o Seu Dragão", "🐲", listOf("como treinar o seu dragao")),
        Colecao("Coringa", "🃏", listOf("coringa")),
        Colecao("Crepúsculo", "🧛", listOf("crepusculo", "lua nova", "amanhecer")),
        Colecao("De Volta para o Futuro", "⏱️", listOf("de volta para o futuro")),
        Colecao("Deadpool", "🗡️", listOf("deadpool")),
        Colecao("Divergente", "🔷", listOf("divergente", "insurgente", "convergente")),
        Colecao("Doutor Estranho", "🔮", listOf("doutor estranho")),
        Colecao("Duna", "🏜️", listOf("duna")),
        Colecao("Duro de Matar", "💥", listOf("duro de matar")),
        Colecao("Esquadrão Suicida", "💣", listOf("esquadrao suicida")),
        Colecao("Esqueceram de Mim", "🏠", listOf("esqueceram de mim")),
        Colecao("Evil Dead", "🪓", listOf("evil dead", "morte do demonio", "uma noite alucinante")),
        Colecao("Exterminador do Futuro", "🦿", listOf("exterminador do futuro")),
        Colecao("Extermínio", "☣️", listOf("exterminio")),
        Colecao("Five Nights at Freddy's", "🐻", listOf("five nights")),
        Colecao("Frozen", "❄️", listOf("frozen")),
        Colecao("Gato de Botas", "🐱", listOf("gato de botas")),
        Colecao("Gladiador", "⚔️", listOf("gladiador")),
        Colecao("Godzilla e Kong", "🦍", listOf("godzilla", "king kong", "kong:")),
        Colecao("Gremlins", "🧌", listOf("gremlins")),
        Colecao("Guardiões da Galáxia", "🚀", listOf("guardioes da galaxia")),
        Colecao("Halloween", "🎃", listOf("halloween")),
        Colecao("Harry Potter", "⚡", listOf("harry potter")),
        Colecao("Hellraiser", "⛓️", listOf("hellraiser")),
        Colecao("Homem de Ferro", "🦾", listOf("homem de ferro")),
        Colecao("Homem-Aranha", "🕷️", listOf("homem-aranha")),
        Colecao("Homem-Formiga", "🐜", listOf("homem-formiga")),
        Colecao("Homens de Preto", "🕴️", listOf("homens de preto")),
        Colecao("Hotel Transilvânia", "🧛", listOf("hotel transilvania")),
        Colecao("Indiana Jones", "🤠", listOf("indiana jones")),
        Colecao("Invocação do Mal", "👻", listOf("invocacao do mal", "annabelle", "a freira")),
        Colecao("It: A Coisa", "🎈", listOf("it: a coisa")),
        Colecao("Jack Ryan", "🕵️", listOf("jack ryan", "operacao sombra")),
        Colecao("Jeepers Creepers", "🦇", listOf("jeepers creepers")),
        Colecao("Jogos Mortais", "🪚", listOf("jogos mortais")),
        Colecao("Jogos Vorazes", "🏹", listOf("jogos vorazes")),
        Colecao("John Wick", "🔫", listOf("john wick")),
        Colecao("Jumanji", "🎲", listOf("jumanji")),
        Colecao("Jurassic Park e World", "🦖", listOf("jurassic")),
        Colecao("Karate Kid", "🥋", listOf("karate kid")),
        Colecao("Kingsman", "🎩", listOf("kingsman")),
        Colecao("Kung Fu Panda", "🐼", listOf("kung fu panda")),
        Colecao("Lilo & Stitch", "🌺", listOf("lilo")),
        Colecao("M3GAN", "🤖", listOf("m3gan")),
        Colecao("Mad Max", "🔥", listOf("mad max")),
        Colecao("Madagascar", "🦁", listOf("madagascar")),
        Colecao("Massacre da Serra Elétrica", "🪚", listOf("massacre da serra")),
        Colecao("Matrix", "🕶️", listOf("matrix")),
        Colecao("Maze Runner", "🌀", listOf("maze runner", "correr ou morrer", "prova de fogo", "a cura mortal")),
        Colecao("Meu Malvado Favorito e Minions", "🍌", listOf("meu malvado favorito", "minions")),
        Colecao("Missão Impossível", "🎯", listOf("missao impossivel")),
        Colecao("Missão Resgate", "🚁", listOf("missao resgate", "tyler rake")),
        Colecao("Monstros S.A.", "👹", listOf("monstros s.a.", "universidade monstros")),
        Colecao("Mortal Kombat", "🐉", listOf("mortal kombat")),
        Colecao("Mulher-Maravilha", "⭐", listOf("mulher-maravilha")),
        Colecao("O Albergue", "🏨", listOf("o albergue", "hostel")),
        Colecao("O Chamado", "📼", listOf("o chamado")),
        Colecao("O Escorpião Rei", "🦂", listOf("escorpiao rei")),
        Colecao("O Exorcista", "✝️", listOf("exorcista")),
        Colecao("O Grito", "👁️", listOf("o grito")),
        Colecao("O Poderoso Chefão", "🌹", listOf("poderoso chefao")),
        Colecao("O Rei Leão", "🦁", listOf("rei leao")),
        Colecao("Os Incríveis", "🦸‍♂️", listOf("os incriveis")),
        Colecao("Os Mercenários", "💪", listOf("os mercenarios")),
        Colecao("Os Smurfs", "🔵", listOf("smurfs")),
        Colecao("Pânico", "🔪", listOf("panico"), listOf("todo mundo", "panico no lago", "panico na floresta")),
        Colecao("Pânico no Lago", "🐊", listOf("panico no lago", "lake placid")),
        Colecao("Pânico na Floresta", "🌲", listOf("panico na floresta", "wrong turn")),
        Colecao("Pantera Negra", "🐆", listOf("pantera negra")),
        Colecao("Para Todos os Garotos", "💌", listOf("para todos os garotos")),
        Colecao("Percy Jackson", "🔱", listOf("percy jackson")),
        Colecao("Piratas do Caribe", "🏴‍☠️", listOf("piratas do caribe")),
        Colecao("Planeta dos Macacos", "🦍", listOf("planeta dos macacos")),
        Colecao("Poltergeist", "📺", listOf("poltergeist")),
        Colecao("Predador", "🦎", listOf("predador", "predator")),
        Colecao("Premonição", "💀", listOf("premonicao")),
        Colecao("Procurando Nemo e Dory", "🐠", listOf("procurando")),
        Colecao("Rambo", "🪖", listOf("rambo")),
        Colecao("Resident Evil", "🧟", listOf("resident evil")),
        Colecao("Rocky e Creed", "🥊", listOf("rocky", "creed")),
        Colecao("Se Beber, Não Case", "🍻", listOf("se beber")),
        Colecao("Senhor dos Anéis e O Hobbit", "💍", listOf("senhor dos aneis", "o hobbit")),
        Colecao("Sexta-Feira 13", "🏒", listOf("sexta-feira 13", "jason")),
        Colecao("Sherlock Holmes", "🔍", listOf("sherlock holmes")),
        Colecao("Shrek", "🧅", listOf("shrek")),
        Colecao("Sobrenatural", "🌑", listOf("sobrenatural")),
        Colecao("Sonic", "💨", listOf("sonic")),
        Colecao("Sorria", "😁", listOf("sorria")),
        Colecao("Star Trek", "🖖", listOf("star trek")),
        Colecao("Star Wars", "✨", listOf("star wars")),
        Colecao("Super Mario", "🍄", listOf("super mario")),
        Colecao("Superman", "🦸", listOf("superman")),
        Colecao("Tartarugas Ninja", "🐢", listOf("tartarugas ninja")),
        Colecao("Teen Wolf", "🐺", listOf("teen wolf")),
        Colecao("Terrifier", "🤡", listOf("terrifier")),
        Colecao("Thor", "⚡", listOf("=thor", "thor:")),
        Colecao("Todo Mundo em Pânico", "😱", listOf("todo mundo em panico")),
        Colecao("Toy Story", "🤠", listOf("toy story")),
        Colecao("Transformers", "🤖", listOf("transformers")),
        Colecao("Um Lugar Silencioso", "🤫", listOf("um lugar silencioso")),
        Colecao("Uma Noite de Crime", "🎭", listOf("noite de crime")),
        Colecao("Velozes e Furiosos", "🏎️", listOf("velozes e furiosos")),
        Colecao("Venom", "🕸️", listOf("venom")),
        Colecao("Vingadores", "🛡️", listOf("vingadores")),
        Colecao("X-Men", "🧬", listOf("x-men"))
    )

    // Filmes da coletânea em ordem de lançamento.
    private fun filmesDaColecao(colecao: Colecao, base: List<Filme> = filmes): List<Filme> {
        return base
            .filter { f ->
                val t = normalizarTexto(f.titulo)
                colecao.chaves.any { k ->
                    if (k.startsWith("=")) t == k.substring(1) else t.contains(k)
                } &&
                    colecao.exclui.none { t.contains(it) }
            }
            .sortedWith(compareBy<Filme> { it.ano }.thenBy { it.titulo.lowercase() })
    }

    // Mínimo de filmes para a coletânea aparecer no menu (0 = mostra todas, até as vazias).
    private val COLECAO_MINIMO = 0

    // Calculado uma vez quando o catálogo carrega (e não a cada abertura do menu).
    private var colecoesProntas: List<Pair<Colecao, List<Filme>>> = emptyList()

    private fun calcularColecoes(base: List<Filme>): List<Pair<Colecao, List<Filme>>> {
        return COLECOES
            .map { it to filmesDaColecao(it, base) }
            .filter { it.second.size >= COLECAO_MINIMO }
            // As que já têm filmes vão primeiro (a ordem alfabética se mantém).
            .sortedByDescending { it.second.isNotEmpty() }
    }

    private fun secaoColetaneas(aberta: Boolean) {
        val validas = colecoesProntas

        if (validas.isEmpty()) return

        val titulo = "COLETÂNEAS"

        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (aberta) View.VISIBLE else View.GONE
        }

        val cabecalho = adicionarItemMenu(
            textoSecao(titulo, validas.size, aberta),
            cor = COR_SECAO
        ) {
            alternarSecao(titulo)
        }

        cabecalho.setTypeface(null, Typeface.BOLD)

        menuConteudo.addView(
            corpo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        validas.forEach { (colecao, lista) ->
            adicionarItemMenu(
                "${colecao.icone}  ${colecao.nome} (${lista.size})",
                pai = corpo
            ) {
                fecharMenu()
                mostrarListaCards(lista)
            }
        }

        secoesMenu.add(SecaoMenu(titulo, validas.size, cabecalho, corpo))
    }

    private fun secaoFilmes(titulo: String, base: List<Filme>, aberta: Boolean) {
        adicionarSecaoCategorias(
            titulo,
            base.size,
            aberta,
            { c -> base.count { mesmaCategoria(it.categoria, c) } },
            { mostrarListaCards(base) },
            { c -> mostrarListaCards(base.filter { mesmaCategoria(it.categoria, c) }) }
        )
    }

    private fun secaoSeries(titulo: String, base: List<Serie>, aberta: Boolean) {
        adicionarSecaoCategorias(
            titulo,
            base.size,
            aberta,
            { c -> base.count { mesmaCategoria(it.categoria, c) } },
            { mostrarListaSeries(base) },
            { c -> mostrarListaSeries(base.filter { mesmaCategoria(it.categoria, c) }) }
        )
    }

    // Abre a seção clicada e fecha as outras.
    private fun alternarSecao(titulo: String) {
        val alvo = secoesMenu.firstOrNull { it.titulo == titulo } ?: return
        val abrir = alvo.corpo.visibility != View.VISIBLE

        secoesMenu.forEach {
            val aberta = it === alvo && abrir
            it.corpo.visibility = if (aberta) View.VISIBLE else View.GONE
            it.cabecalho.text = textoSecao(it.titulo, it.total, aberta)
        }

        alvo.cabecalho.post { ajustarScrollMenu(alvo.cabecalho) }
    }

    private fun abrirMenu() {
        if (menuAberto) return

        menuAberto = true
        itensMenuFoco.clear()
        secoesMenu.clear()

        menuLateral?.let { raiz.removeView(it) }

        val larguraMenu = minOf(
            dp(360),
            (resources.displayMetrics.widthPixels * 0.85f).toInt()
        )

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = criarFundoMenu(COR_PAINEL, false, 22)
            clipToOutline = true
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
                setMargins(dp(10), dp(10), dp(10), dp(10))
            }
        )

        val cabecalho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(8), dp(14), dp(8))
        }

        menu.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(68)
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
            textSize = 22f
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoMenu(COR_ITEM, false, 24)

            setOnFocusChangeListener { _, foco ->
                background = criarFundoMenu(COR_ITEM, foco, 24)
            }

            setOnClickListener { fecharMenu() }
        }

        cabecalho.addView(
            botaoFecharMenu,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        itensMenuFoco.add(botaoFecharMenu)

        menuScroll = ScrollView(this).apply {
            isFocusable = false
            isVerticalScrollBarEnabled = false
        }

        menuConteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(20))
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

        itemAtualizar = adicionarItemMenu(textoAtualizar()) {
            fecharMenu()
            Toast.makeText(
                this,
                "Atualizando catálogo...",
                Toast.LENGTH_SHORT
            ).show()
            carregarCatalogo()
        }

        adicionarItemMenu("▶  Continuar assistindo") {
            fecharMenu()
            mostrarContinuarAssistindo()
        }

        adicionarItemMenu("★  Favoritos (${favoritos.size})") {
            fecharMenu()
            mostrarListaCards(
                filmes.filter { favoritos.contains(it.titulo) }
            )
        }

        adicionarItemMenu("🔍  Pesquisa 🎤") {
            abrirPesquisa()
        }

        verificarNovidades()

        secaoFilmes("FILMES", filmes, true)
        secaoColetaneas(false)
        secaoSeries("SÉRIES", series, false)
        secaoSeries("DORAMAS", doramas, false)
        secaoFilmes("DESENHOS", desenhos, false)
        secaoSeries("ANIMES", animes, false)

        botaoFecharMenu.requestFocus()
    }

    private fun fecharMenu() {
        if (!menuAberto) return

        menuAberto = false
        itemAtualizar = null
        menuLateral?.let { raiz.removeView(it) }
        menuLateral = null
        itensMenuFoco.clear()
        secoesMenu.clear()
        botaoMenu.requestFocus()
    }

    private fun moverMenu(direcao: Int) {
        // Só navega pelos itens visíveis (seções fechadas ficam de fora).
        val visiveis = itensMenuFoco.filter { it.isShown }
        if (visiveis.isEmpty()) return

        val atual = visiveis.indexOfFirst { it.hasFocus() }
        var indice = if (atual < 0) 0 else atual + direcao

        if (indice < 0) indice = visiveis.size - 1
        if (indice >= visiveis.size) indice = 0

        val proximo = visiveis[indice]
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
                view.requestRectangleOnScreen(
                    android.graphics.Rect(0, 0, view.width, view.height),
                    false
                )
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
        executorPrecarga.shutdownNow()
        super.onDestroy()
    }
}
