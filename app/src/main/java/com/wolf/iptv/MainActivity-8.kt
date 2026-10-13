
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
    val video: String,
    val sinopse: String = ""
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
    val temporadas: List<Temporada>,
    val sinopse: String = ""
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
        criarSplash()
    }

    // ===== TELA DE ABERTURA (splash) =====

    // Logo do WOLF IPTV (JPEG 600px em base64, dividido para caber no limite do Kotlin).
    private val LOGO_PARTES = listOf(
        "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"
    )


    private var splash: FrameLayout? = null
    private var pulsoSplash: android.animation.ValueAnimator? = null
    private val inicioSplash = System.currentTimeMillis()

    private fun criarSplash() {
        val tela = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
        }

        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logoBitmap = try {
            val bytes = android.util.Base64.decode(
                LOGO_PARTES.joinToString(""),
                android.util.Base64.DEFAULT
            )
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Throwable) {
            null
        }

        if (logoBitmap != null) {
            val tamanho = minOf(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels
            ) * 3 / 4

            coluna.addView(
                ImageView(this).apply {
                    setImageBitmap(logoBitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(tamanho, tamanho)
            )
        } else {
            coluna.addView(TextView(this).apply {
                text = "WOLF IPTV"
                setTextColor(Color.rgb(230, 25, 35))
                textSize = 48f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
        }

        val carregando = TextView(this).apply {
            text = "Carregando…"
            setTextColor(Color.rgb(150, 155, 175))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        coluna.addView(carregando)

        tela.addView(
            coluna,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        pulsoSplash = android.animation.ValueAnimator.ofFloat(0.35f, 1f).apply {
            duration = 800
            repeatMode = android.animation.ValueAnimator.REVERSE
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener { carregando.alpha = it.animatedValue as Float }
            start()
        }

        coluna.alpha = 0f
        coluna.animate().alpha(1f).setDuration(500).start()

        raiz.addView(
            tela,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        splash = tela

        // Segurança: nunca fica presa mais de 10 segundos.
        tela.postDelayed({ esconderSplash(true) }, 10000)
    }

    // Some com um esmaecer; fica no mínimo ~1,8 s para não piscar.
    private fun esconderSplash(forcar: Boolean = false) {
        val tela = splash ?: return
        val falta = 1800 - (System.currentTimeMillis() - inicioSplash)

        if (falta > 0 && !forcar) {
            tela.postDelayed({ esconderSplash(true) }, falta)
            return
        }

        splash = null
        pulsoSplash?.cancel()
        pulsoSplash = null
        tela.animate()
            .alpha(0f)
            .setDuration(350)
            .withEndAction { raiz.removeView(tela) }
            .start()
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

    // ===== CACHE DO CATÁLOGO (abre rápido, sem baixar tudo toda vez) =====

    private fun arquivoCacheCatalogo(nome: String): File =
        File(filesDir, "cat_" + nome + ".json")

    private fun lerCacheCatalogo(nome: String): String? = try {
        val f = arquivoCacheCatalogo(nome)
        if (f.exists() && f.length() > 0) f.readText(Charsets.UTF_8) else null
    } catch (_: Throwable) {
        null
    }

    private fun salvarCacheCatalogo(nome: String, texto: String?) {
        if (texto.isNullOrBlank()) return
        try {
            arquivoCacheCatalogo(nome).writeText(texto, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }

    // forcarRede = true: baixa tudo da internet (botão "Atualizar catálogo").
    // false: usa o que ficou salvo e só vai à internet se não houver nada salvo.
    private fun carregarCatalogo(forcarRede: Boolean = false) {
        activityScope.launch(Dispatchers.IO) {
            try {
                var vindoDoCache = false
                var respostaTmp: String? = if (forcarRede) null else lerCacheCatalogo("filmes")

                if (respostaTmp != null) {
                    vindoDoCache = true
                } else {
                    respostaTmp = baixarTexto(CATALOGO_URL)
                    if (respostaTmp.isNullOrBlank()) {
                        respostaTmp = lerCacheCatalogo("filmes")
                        if (respostaTmp != null) vindoDoCache = true
                    } else {
                        salvarCacheCatalogo("filmes", respostaTmp)
                    }
                }

                if (respostaTmp.isNullOrBlank()) {
                    throw Exception("Sem internet e sem catálogo salvo")
                }
                val resposta: String = respostaTmp
                val cacheUsado = vindoDoCache

                fun textoDe(nome: String, arquivos: List<String>): String? {
                    if (cacheUsado) {
                        val salvo = lerCacheCatalogo(nome)
                        if (salvo != null) return salvo
                    }
                    val baixado = baixarPrimeiro(arquivos)
                    if (!baixado.isNullOrBlank()) {
                        salvarCacheCatalogo(nome, baixado)
                        return baixado
                    }
                    return lerCacheCatalogo(nome)
                }

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
                                        item.optString("video", ""),
                                        item.optString("sinopse", item.optString("descricao", ""))
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
                                    Serie(titulo, categoria, capa, temporadas, objeto.optString("sinopse", objeto.optString("descricao", "")))
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
                                        item.optString("video", ""),
                                        item.optString("sinopse", item.optString("descricao", ""))
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

                val textoSeries = textoDe("series", ARQUIVOS_SERIES)
                val textoDoramas = textoDe("doramas", ARQUIVOS_DORAMAS)
                val textoAnimes = textoDe("animes", ARQUIVOS_ANIMES)
                val textoDesenhos = textoDe("desenhos", ARQUIVOS_DESENHOS)

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
                    esconderSplash()

                    precarregarCapas(
                        filmes.map { it.capa } +
                            desenhos.map { it.capa } +
                            series.map { it.capa } +
                            doramas.map { it.capa } +
                            animes.map { it.capa }
                    )

                    if (cacheUsado) {
                        // Abriu na hora com o que estava salvo; confere novidades em segundo plano.
                        verificarNovidades()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Catálogo carregado: ${filmes.size} filmes, " +
                                "${series.size} séries, ${doramas.size} doramas, " +
                                "${animes.size} animes e ${desenhos.size} desenhos",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
                    esconderSplash()
                }
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
        esconderSplash(true)
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
        esconderSplash(true)
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

    private fun criarCard(filme: Filme, comDetalhes: Boolean = true): View {
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
            if (comDetalhes) {
                mostrarDetalhes(
                    filme.titulo,
                    filme.capa,
                    if (filme.ano > 0) "${filme.ano}  •  ${filme.categoria}" else filme.categoria,
                    filme.sinopse,
                    "▶  ASSISTIR",
                    WolfProgress.posicao(this, filme.video),
                    true
                ) { abrirVideo(filme.titulo, filme.video, filme.capa) }
            } else {
                abrirVideo(filme.titulo, filme.video, filme.capa)
            }
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

    // ===== TELA DE SINOPSE (detalhes) =====

    private fun botaoDetalhe(texto: String, principal: Boolean): TextView {
        val b = TextView(this).apply {
            text = texto
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            setPadding(dp(28), dp(14), dp(28), dp(14))
        }
        fun estilo(foco: Boolean) {
            val g = GradientDrawable()
            g.cornerRadius = dp(10).toFloat()
            g.setColor(
                if (principal) Color.rgb(220, 20, 30)
                else Color.argb(150, 60, 60, 60)
            )
            if (foco) g.setStroke(dp(3), Color.WHITE)
            b.background = g
            b.scaleX = if (foco) 1.06f else 1f
            b.scaleY = if (foco) 1.06f else 1f
        }
        estilo(false)
        b.setOnFocusChangeListener { _, f -> estilo(f) }
        return b
    }

    private fun mostrarDetalhes(
        titulo: String,
        capa: String,
        meta: String,
        sinopse: String,
        textoAssistir: String,
        posicaoMs: Long,
        manterAberta: Boolean = false,
        aoAssistir: () -> Unit
    ) {
        val dialogo = android.app.Dialog(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )

        val raizD = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        // Fundo: a própria capa, escurecida
        val fundo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.30f
        }
        carregarImagem(
            capa, fundo,
            resources.displayMetrics.widthPixels / 2,
            resources.displayMetrics.heightPixels / 2,
            true
        )
        raizD.addView(fundo, FrameLayout.LayoutParams(-1, -1))

        val veu = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(235, 0, 0, 0), Color.argb(120, 0, 0, 0))
            )
        }
        raizD.addView(veu, FrameLayout.LayoutParams(-1, -1))

        val paisagem = resources.displayMetrics.widthPixels >
            resources.displayMetrics.heightPixels

        val poster = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val g = GradientDrawable()
            g.setColor(Color.argb(120, 20, 20, 20))
            g.cornerRadius = dp(12).toFloat()
            background = g
            clipToOutline = true
        }
        carregarImagem(capa, poster)

        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        coluna.addView(TextView(this).apply {
            text = titulo
            setTextColor(Color.WHITE)
            textSize = if (paisagem) 32f else 26f
            typeface = Typeface.DEFAULT_BOLD
        })

        coluna.addView(TextView(this).apply {
            text = meta
            setTextColor(Color.rgb(255, 90, 90))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(14))
        })

        coluna.addView(TextView(this).apply {
            text = "SINOPSE"
            setTextColor(Color.rgb(170, 170, 170))
            textSize = 13f
            letterSpacing = 0.15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(6))
        })

        coluna.addView(TextView(this).apply {
            text = if (sinopse.isNotBlank()) sinopse
                else "Sinopse ainda não disponível para este título."
            setTextColor(
                if (sinopse.isNotBlank()) Color.rgb(225, 225, 225)
                else Color.rgb(140, 140, 140)
            )
            textSize = 17f
            setLineSpacing(0f, 1.2f)
        })

        val botoes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(26), 0, 0)
        }

        val assistir = botaoDetalhe(
            if (posicaoMs > 0L) "▶  CONTINUAR (${WolfProgress.formatar(posicaoMs)})"
            else textoAssistir,
            true
        )
        assistir.setOnClickListener {
            if (manterAberta) {
                // Filme: a tela fica aberta por baixo enquanto o player abre,
                // sem piscar a lista de filmes.
                aoAssistir()
            } else {
                dialogo.dismiss()
                aoAssistir()
            }
        }
        botoes.addView(
            assistir,
            LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(14) }
        )

        val voltar = botaoDetalhe("VOLTAR", false)
        voltar.setOnClickListener { dialogo.dismiss() }
        botoes.addView(voltar, LinearLayout.LayoutParams(-2, -2))

        coluna.addView(botoes)

        val conteudo = LinearLayout(this).apply {
            orientation = if (paisagem) LinearLayout.HORIZONTAL
                else LinearLayout.VERTICAL
            setPadding(dp(40), dp(28), dp(40), dp(28))
        }

        if (paisagem) {
            conteudo.addView(
                poster,
                LinearLayout.LayoutParams(dp(230), dp(345)).apply {
                    rightMargin = dp(36)
                }
            )
        } else {
            conteudo.addView(
                poster,
                LinearLayout.LayoutParams(dp(180), dp(270)).apply {
                    bottomMargin = dp(20)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
        }

        val rolagem = ScrollView(this).apply { isFillViewport = true }
        rolagem.addView(coluna)
        conteudo.addView(rolagem, LinearLayout.LayoutParams(0, -1, 1f).also {
            if (!paisagem) { it.width = -1; it.height = 0 }
        })

        raizD.addView(conteudo, FrameLayout.LayoutParams(-1, -1))

        dialogo.setContentView(raizD)
        dialogo.show()
        assistir.requestFocus()
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
            val total = serie.temporadas.sumOf { it.episodios.size }
            mostrarDetalhes(
                serie.titulo,
                serie.capa,
                "${serie.categoria}  •  ${serie.temporadas.size} temporada(s)  •  $total episódios",
                serie.sinopse,
                "▶  VER EPISÓDIOS",
                0L
            ) {
                val origem = listaSeriesAtual
                historicoConteudo.add { mostrarListaSeries(origem) }
                mostrarTemporadas(serie)
            }
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

        montarGrade(filmesEmAndamento.map { criarCard(it, false) })
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
        Colecao("As Crônicas de Nárnia", "🦁", listOf("narnia")),
        Colecao("Atividade Paranormal", "📹", listOf("atividade paranormal")),
        Colecao("Bad Boys", "🚔", listOf("bad boys")),
        Colecao("Barbie", "💖", listOf("barbie")),
        Colecao("Ben 10", "⌚", listOf("ben 10")),
        Colecao("Busca Implacável", "📞", listOf("busca implacavel")),
        Colecao("Caça-Fantasmas", "👻", listOf("caca-fantasmas", "ghostbusters")),
        Colecao("Carros", "🚗", listOf("carros")),
        Colecao("Cemitério Maldito", "🪦", listOf("cemiterio maldito", "pet sematary")),
        Colecao("Chucky", "🪆", listOf("chucky", "brinquedo assassino")),
        Colecao("Cinquenta Tons", "🖤", listOf("cinquenta tons")),
        Colecao("Como Treinar o Seu Dragão", "🐲", listOf("como treinar o seu dragao")),
        Colecao("Crepúsculo", "🧛", listOf("crepusculo", "lua nova", "amanhecer")),
        Colecao("DC", "🔵", listOf("batman", "superman", "coringa", "aquaman", "mulher-maravilha", "esquadrao suicida", "shazam", "the flash", "besouro azul", "supergirl", "liga da justica", "aves de rapina", "adao negro", "lanterna verde", "pacificador")),
        Colecao("De Volta para o Futuro", "⏱️", listOf("de volta para o futuro")),
        Colecao("Divergente", "🔷", listOf("divergente", "insurgente", "convergente")),
        Colecao("Duna", "🏜️", listOf("duna")),
        Colecao("Duro de Matar", "💥", listOf("duro de matar")),
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
        Colecao("Halloween", "🎃", listOf("halloween")),
        Colecao("Harry Potter", "⚡", listOf("harry potter")),
        Colecao("Hellraiser", "⛓️", listOf("hellraiser")),
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
        Colecao("Marvel", "🔴", listOf("vingadores", "homem de ferro", "capitao america", "=thor", "thor:", "homem-aranha", "aranhaverso", "homem-formiga", "doutor estranho", "pantera negra", "guardioes da galaxia", "deadpool", "x-men", "venom", "quarteto fantastico", "thunderbolts", "morbius", "o justiceiro", "capita marvel", "viuva negra", "shang-chi", "os eternos", "hulk", "madame web")),
        Colecao("Massacre da Serra Elétrica", "🪚", listOf("massacre da serra")),
        Colecao("Matrix", "🕶️", listOf("matrix")),
        Colecao("Maze Runner", "🌀", listOf("maze runner", "correr ou morrer", "prova de fogo", "a cura mortal")),
        Colecao("Meu Malvado Favorito e Minions", "🍌", listOf("meu malvado favorito", "minions")),
        Colecao("Missão Impossível", "🎯", listOf("missao impossivel")),
        Colecao("Missão Resgate", "🚁", listOf("missao resgate", "tyler rake")),
        Colecao("Monstros S.A.", "👹", listOf("monstros s.a.", "universidade monstros")),
        Colecao("Mortal Kombat", "🐉", listOf("mortal kombat")),
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
        Colecao("Pânico na Floresta", "🌲", listOf("panico na floresta", "wrong turn")),
        Colecao("Pânico no Lago", "🐊", listOf("panico no lago", "lake placid")),
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
        Colecao("Tartarugas Ninja", "🐢", listOf("tartarugas ninja")),
        Colecao("Teen Wolf", "🐺", listOf("teen wolf")),
        Colecao("Terrifier", "🤡", listOf("terrifier")),
        Colecao("Todo Mundo em Pânico", "😱", listOf("todo mundo em panico")),
        Colecao("Toy Story", "🤠", listOf("toy story")),
        Colecao("Transformers", "🤖", listOf("transformers")),
        Colecao("Um Lugar Silencioso", "🤫", listOf("um lugar silencioso")),
        Colecao("Uma Noite de Crime", "🎭", listOf("noite de crime")),
        Colecao("Velozes e Furiosos", "🏎️", listOf("velozes e furiosos"))
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
            carregarCatalogo(true)
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
