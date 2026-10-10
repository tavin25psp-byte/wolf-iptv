
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

    // Canais ao vivo: "canais ao vivo.json" (tenta vários nomes possíveis).
    private val ARQUIVOS_CANAIS = listOf(
        "canais%20ao%20vivo.json",
        "canais ao vivo.json",
        "canais_ao_vivo.json",
        "canais-ao-vivo.json",
        "canaisaovivo.json",
        "Canais%20ao%20vivo.json",
        "Canais%20Ao%20Vivo.json",
        "canais.json"
    )

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
    private val canais = mutableListOf<Filme>()

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
        fundo.cornerRadius = dp(16).toFloat()
        if (foco) fundo.setStroke(dp(4), Color.RED)
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(16).toFloat()
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
