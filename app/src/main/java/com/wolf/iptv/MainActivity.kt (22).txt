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

data class Canal(
    val nome: String,
    val url: String,
    val logo: String = "",
    val categoria: String = "Canais ao vivo"
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
    private val CANAIS_URL = BASE_URL + "canais.json"

    private val ARQUIVOS_SERIES =
        listOf("series.json", "serie.json", "s%C3%A9rie.json")

    private val ARQUIVOS_DORAMAS =
        listOf("doramas.json", "Doramas.json")

    private val ARQUIVOS_ANIMES =
        listOf("animes.json", "anime.json", "Animes.json", "Anime.json")

    private val ARQUIVOS_DESENHOS =
        listOf("desenho.json", "desenhos.json", "Desenho.json", "Desenhos.json")

    private val REQ_VOZ = 100
    private val TAMANHO_PAGINA = 60

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var campoPesquisaTopo: TextView

    private val ICONES = linkedMapOf(
        "Início" to "🏠",
        "Filmes" to "🎬",
        "Séries" to "📺",
        "Doramas" to "🌸",
        "Anime" to "🍥",
        "Desenhos" to "🧸",
        "Favoritos" to "★",
        "Continue assistindo" to "⏳",
        "Buscar" to "🔍",
        "Configurações" to "⚙"
    )
    private val itensMenu = LinkedHashMap<String, TextView>()
    private var menuAtual = "Início"
    private var compacto = false
    private var ehTv = false

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
    private var voltarListaAtual: () -> Unit = {}

    private val filmes = mutableListOf<Filme>()
    private val series = mutableListOf<Serie>()
    private val doramas = mutableListOf<Serie>()
    private val animes = mutableListOf<Serie>()
    private val desenhos = mutableListOf<Filme>()
    private val desenhosSeries = mutableListOf<Serie>()
    private val canais = mutableListOf<Canal>()

    private val cardsAtuais = mutableListOf<View>()
    private val linhasCards = mutableListOf<List<View>>()

    private var bannerView: View? = null
    private var btnAssistir: View? = null
    private var btnInfoBanner: View? = null

    private var colunas = 5
    private val favoritos = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val tv = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_LEANBACK
        )
        ehTv = tv
        val larguraDp = resources.configuration.screenWidthDp

        colunas = when {
            tv -> 5
            larguraDp < 600 -> 2
            larguraDp < 900 -> 3
            else -> 5
        }
        compacto = !tv && larguraDp < 600

        favoritos.addAll(prefsWolf.getStringSet("favoritos", emptySet()) ?: emptySet())

        criarInterface()
        verificarAssinatura()
        mostrarUltimoErro()

        activityScope.launch {
            var ciclo = 0
            while (true) {
                delay(120_000L)
                if (!noPrimeiroPlano) continue
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
                it.isNotBlank() && (!it.trimStart().startsWith("at ") || it.contains("com.wolf.iptv"))
            }.take(14).joinToString("\n")

            AlertDialog.Builder(this)
                .setTitle("O app fechou por este erro")
                .setMessage(resumo)
                .setPositiveButton("OK", null)
                .show()
        } catch (_: Throwable) {
        }
    }

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

    // ───────────────────────── FUNDO / LOGO ─────────────────────────

    private fun abridorFundoLocal(): (() -> java.io.InputStream)? {
        for (tipo in listOf("drawable", "raw", "mipmap")) {
            val id = resources.getIdentifier("fundo", tipo, packageName)
            if (id != 0) {
                val abrirRes: () -> java.io.InputStream = { resources.openRawResource(id) }
                return abrirRes
            }
        }
        for (nome in listOf("fundo.png", "fundo.jpg", "fundo.jpeg", "fundo.webp")) {
            try {
                assets.open(nome).close()
                val abrirAsset: () -> java.io.InputStream = { assets.open(nome) }
                return abrirAsset
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private fun decodificarFundoAmostrado(
        abrir: () -> java.io.InputStream,
        larguraAlvo: Int,
        alturaAlvo: Int
    ): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        abrir().use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var amostra = 1
        while (
            limites.outWidth / (amostra * 2) >= larguraAlvo &&
            limites.outHeight / (amostra * 2) >= alturaAlvo
        ) {
            amostra *= 2
        }

        val leitura = BitmapFactory.Options().apply {
            inSampleSize = amostra
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return abrir().use { BitmapFactory.decodeStream(it, null, leitura) }
    }

    private fun carregarFundoApp(fundo: ImageView) {
        val larg = resources.displayMetrics.widthPixels
        val alt = resources.displayMetrics.heightPixels

        executorImagens.execute {
            var bitmap: Bitmap? = null
            try {
                val abrir = abridorFundoLocal()
                if (abrir != null) bitmap = decodificarFundoAmostrado(abrir, larg, alt)
            } catch (_: Throwable) {
            }

            val pronto = bitmap
            runOnUiThread {
                if (pronto != null) {
                    fundo.setImageBitmap(pronto)
                }
            }
        }
    }

    private fun criarLogoCabecalho(): Bitmap? {
        return try {
            val bytes = android.util.Base64.decode(
                LOGO_PARTES.joinToString(""),
                android.util.Base64.DEFAULT
            )
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val lado = 360
            val reduzido = Bitmap.createScaledBitmap(original, lado, lado, true)
            val pixels = IntArray(lado * lado)
            reduzido.getPixels(pixels, 0, lado, 0, 0, lado, lado)

            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val brilho = maxOf(r, g, b)
                val alfa = ((brilho - 10) * 255 / 245).coerceIn(0, 255)

                pixels[i] = if (brilho == 0 || alfa == 0) {
                    0
                } else {
                    Color.argb(alfa, r * 255 / brilho, g * 255 / brilho, b * 255 / brilho)
                }
            }
            Bitmap.createBitmap(pixels, lado, lado, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            null
        }
    }

    // ───────────────────────── INTERFACE PRINCIPAL ─────────────────────────

    private fun textoMenu(nome: String): String {
        val icone = ICONES[nome] ?: ""
        val sino = if (nome == "Configurações" && novidades.isNotEmpty()) "  🔔" else ""
        return if (compacto) icone + sino else "$icone   $nome$sino"
    }

    private fun criarInterface() {
        raiz = FrameLayout(this)
        raiz.layoutDirection = View.LAYOUT_DIRECTION_LTR
        raiz.setBackgroundColor(Color.BLACK)

        val fundo = ImageView(this)
        fundo.scaleType = ImageView.ScaleType.CENTER_CROP
        fundo.alpha = 1f
        carregarFundoApp(fundo)

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val degrade = View(this)
        degrade.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(30, 5, 7, 18),
                Color.argb(120, 4, 6, 16),
                Color.argb(230, 2, 3, 10)
            )
        )
        raiz.addView(
            degrade,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val layoutPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(14), dp(10))
        }

        raiz.addView(
            layoutPrincipal,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ----- Menu lateral -----
        val larguraItem = if (compacto) dp(48) else dp(190)

        val painelMenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(2), dp(10), dp(8))
        }

        val logoTopo = criarLogoCabecalho()
        if (logoTopo != null) {
            val t = if (compacto) dp(44) else dp(76)
            painelMenu.addView(
                ImageView(this).apply {
                    setImageBitmap(logoTopo)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(t, t).apply { bottomMargin = dp(10) }
            )
        }

        ICONES.keys.forEach { nome ->
            val ativo0 = nome == menuAtual
            val item = TextView(this).apply {
                text = textoMenu(nome)
                setTextColor(Color.WHITE)
                textSize = if (compacto) 16f else 14f
                setTypeface(null, if (ativo0) Typeface.BOLD else Typeface.NORMAL)
                gravity = if (compacto) Gravity.CENTER else Gravity.CENTER_VERTICAL
                setPadding(if (compacto) 0 else dp(16), 0, if (compacto) 0 else dp(10), 0)
                isFocusable = true
                isClickable = true
                background = criarFundoMenuLateral(ativo0, false)
            }
            item.setOnFocusChangeListener { _, foco ->
                item.background = criarFundoMenuLateral(nome == menuAtual, foco)
            }
            item.setOnClickListener { abrirSecao(nome) }

            painelMenu.addView(
                item,
                LinearLayout.LayoutParams(larguraItem, dp(40)).apply { bottomMargin = dp(4) }
            )
            itensMenu[nome] = item
        }

        val rolagemMenu = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(painelMenu)
        }

        val colunaMenu = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        colunaMenu.addView(
            rolagemMenu,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        colunaMenu.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.argb(0, 255, 30, 40),
                        Color.rgb(230, 25, 35),
                        Color.argb(0, 255, 30, 40)
                    )
                )
            },
            LinearLayout.LayoutParams(dp(2), ViewGroup.LayoutParams.MATCH_PARENT)
        )

        layoutPrincipal.addView(
            colunaMenu,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ----- Painel direito -----
        val painelDireito = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        val barraTopo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        campoPesquisaTopo = TextView(this).apply {
            text = "🔍   Buscar filmes, séries, doramas..."
            setTextColor(Color.rgb(170, 175, 195))
            textSize = 14f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
            isFocusable = true
            isClickable = true
            background = GradientDrawable().apply {
                setColor(Color.argb(180, 14, 18, 38))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(44, 55, 100))
            }
            setOnClickListener { abrirPesquisa() }
        }
        campoPesquisaTopo.setOnFocusChangeListener { v, foco ->
            v.background = GradientDrawable().apply {
                setColor(Color.argb(180, 14, 18, 38))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(if (foco) 2 else 1), if (foco) Color.rgb(255, 40, 40) else Color.rgb(44, 55, 100))
            }
        }

        barraTopo.addView(
            campoPesquisaTopo,
            LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(14) }
        )

        fun addIcone(icone: String, acao: () -> Unit) {
            barraTopo.addView(
                botaoIcone(icone, acao),
                LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) }
            )
        }

        if (!compacto) addIcone("👑") { mostrarAssinatura() }
        if (!compacto) addIcone("♥") { abrirSecao("Favoritos") }
        if (!compacto) addIcone("🕘") { abrirSecao("Continue assistindo") }
        addIcone("⚙") { abrirConfiguracoes() }
        if (!compacto) addIcone("👤") { mostrarAssinatura() }

        barraTopo.addView(
            criarRelogio(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        painelDireito.addView(
            barraTopo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scroll = ScrollView(this).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
        }

        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(
            conteudo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        painelDireito.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        layoutPrincipal.addView(
            painelDireito,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        )

        setContentView(raiz)
        criarSplash()
    }

    private fun botaoIcone(icone: String, acao: () -> Unit): TextView {
        val b = TextView(this).apply {
            text = icone
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
        }
        fun estilo(foco: Boolean) {
            b.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(190, 18, 22, 45))
                setStroke(
                    dp(if (foco) 2 else 1),
                    if (foco) Color.rgb(255, 40, 40) else Color.rgb(70, 80, 120)
                )
            }
        }
        estilo(false)
        b.setOnFocusChangeListener { _, f -> estilo(f) }
        b.setOnClickListener { acao() }
        return b
    }

    private fun criarFundoMenuLateral(ativo: Boolean, foco: Boolean): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.cornerRadius = dp(10).toFloat()
        if (ativo) {
            fundo.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            fundo.colors = intArrayOf(Color.rgb(215, 20, 30), Color.rgb(120, 10, 20))
            fundo.setStroke(dp(if (foco) 2 else 1), if (foco) Color.WHITE else Color.argb(200, 255, 90, 90))
        } else if (foco) {
            fundo.setColor(Color.argb(60, 255, 255, 255))
            fundo.setStroke(dp(2), Color.rgb(255, 40, 40))
        } else {
            fundo.setColor(Color.TRANSPARENT)
        }
        return fundo
    }

    private fun marcarMenu(nome: String) {
        menuAtual = nome
        itensMenu.forEach { (n, v) ->
            val ativo = n == nome
            v.background = criarFundoMenuLateral(ativo, v.hasFocus())
            v.setTypeface(null, if (ativo) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun abrirSecao(nome: String) {
        if (nome != "Buscar" && nome != "Configurações") marcarMenu(nome)
        when (nome) {
            "Início" -> mostrarInicio()
            "Filmes" -> mostrarColecao(
                "Filmes", "🔥", filmes, emptyList(), "Nenfum filme encontrado".replace("Nenfum", "Nenhum")
            ) { abrirSecao("Filmes") }
            "Séries" -> mostrarColecao(
                "Séries", "🎬", emptyList(), series, "Nenhuma série encontrada"
            ) { abrirSecao("Séries") }
            "Doramas" -> mostrarColecao(
                "Doramas", "🌸", emptyList(), doramas, "Nenhum dorama encontrado"
            ) { abrirSecao("Doramas") }
            "Anime" -> mostrarColecao(
                "Anime", "🍥", emptyList(), animes, "Nenhum anime encontrado"
            ) { abrirSecao("Anime") }
            "Desenhos" -> mostrarColecao(
                "Desenhos", "🧸", desenhos, desenhosSeries, "Nenhum desenho encontrado"
            ) { abrirSecao("Desenhos") }
            "Favoritos" -> mostrarColecao(
                "Favoritos", "★",
                (filmes + desenhos).filter { favoritos.contains(it.titulo) },
                (series + doramas + animes + desenhosSeries).filter { favoritos.contains(it.titulo) },
                "Nenhum favorito ainda.\nSegure o dedo em um título (ou use o botão ☆ nos detalhes)."
            ) { abrirSecao("Favoritos") }
            "Continue assistindo" -> mostrarContinuarAssistindo()
            "Buscar" -> abrirPesquisa()
            "Configurações" -> abrirConfiguracoes()
        }
    }

    // ───────────────────────── SPLASH ─────────────────────────

    private val LOGO_PARTES = listOf(
        "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAJYAlgDASIAAhEBAxEB/8QAHQAAAAcBAQEAAAAAAAAAAAAAAAECAwQFBgcICf/EAFAQAAIBAwIDBgIHBQUGBAQEBwECAwAEEQUhEjFBBhMiUWFxgZEHFDJCobHBI1Ji0fAVM3KC4QgkQ1OS8RZjorI0RHPCJTWDhJPSVGRFlOL/xAAbAQABBQEBAAAAAAAAAAAAAAAAAQIDBAUGB//EAEQRAAEDAgMFBQcCAwYFBAMAAAEAAgMEERIhBRMxQVEGYXGBkaGxwdHwQuEVI/EWM1JikqJDcoKywiQ0U9JEY+L/2gAMAwEAAhEDEQA/AP"
    ).let { listOf(it[0]) }

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

        tela.postDelayed({ esconderSplash(true) }, 10000)
    }

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

    // ───────────────────────── COMPONENTES VISUAIS ─────────────────────────

    private fun tituloSecao(texto: String, icone: String = ""): View {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(12), dp(8), dp(8))
        }

        if (icone.isEmpty()) {
            linha.addView(
                View(this).apply { setBackgroundColor(Color.rgb(255, 40, 40)) },
                LinearLayout.LayoutParams(dp(4), dp(22)).apply { rightMargin = dp(10) }
            )
        }

        linha.addView(
            TextView(this).apply {
                this.text = if (icone.isEmpty()) texto else "$icone  $texto"
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            }
        )

        return linha
    }

    private fun criarCabecalho(titulo: String, icone: String, aoVerTodos: (() -> Unit)?): View {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(10), dp(8), dp(6))
        }
        linha.addView(
            TextView(this).apply {
                this.text = "$icone  $titulo"
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (aoVerTodos != null) {
            linha.addView(
                TextView(this).apply {
                    this.text = "Ver todos  ›"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    isClickable = true
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    setOnClickListener { aoVerTodos() }
                }
            )
        }
        return linha
    }

    private fun chip(texto: String, grande: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = if (grande) 12f else 10f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            val h = dp(if (grande) 12 else 8)
            val v = dp(if (grande) 4 else 2)
            setPadding(h, v, h, v)
            background = GradientDrawable().apply {
                setColor(Color.argb(100, 20, 26, 50))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.argb(140, 200, 205, 225))
            }
        }
    }

    private fun botaoPilula(texto: String, vermelho: Boolean, acao: () -> Unit): TextView {
        val b = TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = if (compacto) 12f else 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setPadding(dp(22), dp(10), dp(22), dp(10))
        }
        fun estilo(foco: Boolean) {
            b.background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(if (vermelho) Color.rgb(220, 20, 30) else Color.argb(150, 20, 26, 50))
                setStroke(
                    dp(if (foco) 3 else 1),
                    if (foco) Color.WHITE
                    else if (vermelho) Color.rgb(220, 20, 30)
                    else Color.argb(150, 200, 205, 225)
                )
            }
        }
        estilo(false)
        b.setOnFocusChangeListener { _, f -> estilo(f) }
        b.setOnClickListener { acao() }
        return b
    }

    private lateinit var relogioHora: TextView
    private lateinit var relogioData: TextView

    private fun criarRelogio(): View {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(dp(6), 0, dp(4), 0)
        }

        relogioHora = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }

        relogioData = TextView(this).apply {
            setTextColor(Color.rgb(180, 185, 200))
            textSize = 11f
            gravity = Gravity.END
        }

        caixa.addView(relogioHora)
        caixa.addView(relogioData)

        activityScope.launch {
            val formatoHora = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
            val formatoData = SimpleDateFormat("EEE, dd MMM", Locale("pt", "BR"))
            while (true) {
                val agora = Date()
                relogioHora.text = formatoHora.format(agora)
                relogioData.text = formatoData.format(agora)
                    .replace(".", "")
                    .split(" ")
                    .joinToString(" ") { p -> p.replaceFirstChar { it.uppercase() } }
                delay(30_000)
            }
        }

        return caixa
    }

    private fun criarFundoCard(foco: Boolean): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.setColor(
            if (foco) Color.argb(245, 14, 20, 42)
            else Color.argb(225, 8, 12, 28)
        )
        fundo.cornerRadius = dp(10).toFloat()
        if (foco) fundo.setStroke(dp(3), Color.rgb(255, 40, 40))
        else fundo.setStroke(dp(1), Color.rgb(36, 50, 100))
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(10).toFloat()
        borda.setStroke(dp(3), Color.rgb(255, 40, 40))
        return borda
    }

    // ───────────────────────── IMAGENS ─────────────────────────

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

    private val aguardandoImagem = HashMap<String, MutableList<ImageView>>()
    private val executorPrecarga = Executors.newFixedThreadPool(3)

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

    private fun precarregarCapas(urls: List<String>) {
        urls.filter { it.isNotBlank() }.distinct().forEach { url ->
            try {
                executorPrecarga.execute {
                    try {
                        if (!noPrimeiroPlano) return@execute
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

        val largAlvo = if (larguraAlvo > 0) larguraAlvo
            else (resources.displayMetrics.widthPixels / colunas.coerceAtLeast(1)) * 3 / 2
        val altAlvo = if (alturaAlvo > 0) alturaAlvo else dp(240) * 3 / 2

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

    // ───────────────────────── CATÁLOGO ─────────────────────────

    private fun anoDoItem(item: JSONObject): Int {
        val ano = item.optInt("ano", 0)
        if (ano in 1900..2099) return ano
        val texto = item.optString("titulo", "") + " " + Uri.decode(item.optString("video", ""))
        return Regex("(?<!\\d)(19|20)\\d{2}(?!\\d)").find(texto)?.value?.toInt() ?: 0
    }

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

    private fun lerSeries(array: JSONArray?): ArrayList<Serie> {
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
                                    } catch (_: Exception) {
                                    }
                                }
                            }

                            temporadas.add(Temporada(numeroTemporada, episodios))
                        } catch (_: Exception) {
                        }
                    }
                }

                if (titulo.isNotBlank()) {
                    resultado.add(
                        Serie(
                            titulo, categoria, capa, temporadas,
                            objeto.optString("sinopse", objeto.optString("descricao", ""))
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        return resultado
    }

    private fun lerDesenhos(texto: String?): ArrayList<Filme> {
        val resultado = ArrayList<Filme>()
        if (texto.isNullOrBlank()) return resultado

        val array = extrairArray(texto, "desenhos")
            ?: extrairArray(texto, "desenho")
            ?: return resultado

        for (i in 0 until array.length()) {
            try {
                val item = array.getJSONObject(i)
                if (item.has("temporadas")) continue
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

                val novosDesenhosSeries = lerSeries(
                    (extrairArray(textoDesenhos, "desenhos")
                        ?: extrairArray(textoDesenhos, "desenho"))?.let { todos ->
                        val soSeries = JSONArray()
                        for (i in 0 until todos.length()) {
                            val o = todos.optJSONObject(i)
                            if (o != null && o.has("temporadas")) soSeries.put(o)
                        }
                        soSeries
                    }
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

                    desenhos.clear()
                    desenhosSeries.clear()
                    desenhos.addAll(novosDesenhos)
                    desenhosSeries.addAll(novosDesenhosSeries)

                    assinaturasCarregadas = mapOf(
                        "filmes" to resposta.trim().hashCode(),
                        "séries" to (textoSeries?.trim()?.hashCode() ?: 0),
                        "doramas" to (textoDoramas?.trim()?.hashCode() ?: 0),
                        "animes" to (textoAnimes?.trim()?.hashCode() ?: 0),
                        "desenhos" to (textoDesenhos?.trim()?.hashCode() ?: 0)
                    )
                    novidades = emptyList()
                    aplicarNovidades()

                    marcarMenu("Início")
                    mostrarInicio()
                    esconderSplash()

                    precarregarCapas(
                        filmes.map { it.capa } +
                            desenhos.map { it.capa } +
                            desenhosSeries.map { it.capa } +
                            series.map { it.capa } +
                            doramas.map { it.capa } +
                            animes.map { it.capa }
                    )

                    if (cacheUsado) {
                        verificarNovidades()
                    }
                }
            } catch (erro: Exception) {
                withContext(Dispatchers.Main) {
                    filmes.clear()
                    series.clear()
                    doramas.clear()
                    animes.clear()
                    desenhos.clear()
                    desenhosSeries.clear()
                    limparConteudo()

                    val erroTexto = TextView(this@MainActivity).apply {
                        text = "ERRO NO CATÁLOGO\n\n" + (erro.message ?: "Erro desconhecido")
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
                conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        } finally {
            conexao?.disconnect()
        }
    }

    // ───────────────────────── ASSINATURA / CÓDIGO ─────────────────────────

    private val EXIGIR_CODIGO = true
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

    private fun conferirCodigo(codigo: String): ResultadoCodigo {
        if (normalizarCodigo(codigo).length < 4) return ResultadoCodigo.INVALIDO

        var conexao: HttpURLConnection? = null
        return try {
            val url = BASE_URL + "clientes/" + nomeArquivoCodigo(codigo) + ".json?t=" + System.currentTimeMillis()
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

            val texto = conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(texto.trim())

            if (!json.optBoolean("ativo", false)) return ResultadoCodigo.BLOQUEADO

            val ate = json.optString("ate", "")
            val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (ate.isNotBlank()) {
                if (ate.length > 10) {
                    val limite = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).parse(ate)
                    } catch (_: Exception) { null }
                    if (limite != null && Date().after(limite)) return ResultadoCodigo.VENCIDO
                } else if (hoje > ate) {
                    return ResultadoCodigo.VENCIDO
                }
            }
            ResultadoCodigo.OK
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
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(prefs.getString("codigo", "") ?: "")
        }

        AlertDialog.Builder(this)
            .setTitle("WOLF IPTV – Código de acesso")
            .setMessage(mensagem)
            .setView(campo)
            .setCancelable(false)
            .setPositiveButton("Entrar") { _, _ ->
                dialogoCodigoAberto = false
                prefs.edit().putString("codigo", campo.text.toString().trim()).apply()
                verificarAssinatura()
            }
            .setNegativeButton("Sair") { _, _ ->
                dialogoCodigoAberto = false
                finish()
            }
            .show()
    }

    private fun mostrarAssinatura() {
        val codigo = prefsWolf.getString("codigo", "") ?: ""
        AlertDialog.Builder(this)
            .setTitle("Minha conta")
            .setMessage(
                if (codigo.isBlank()) "Nenhum código cadastrado."
                else "Código de acesso: $codigo"
            )
            .setPositiveButton("Verificar agora") { _, _ ->
                Toast.makeText(this, "Verificando assinatura…", Toast.LENGTH_SHORT).show()
                verificarAssinatura()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    // ───────────────────────── NOVIDADES / CONFIGURAÇÕES ─────────────────────────

    private var assinaturasCarregadas: Map<String, Int> = emptyMap()
    private var novidades: List<String> = emptyList()
    private var verificandoNovidades = false

    private fun textoAtualizar(): String =
        if (novidades.isEmpty()) "🔄  Atualizar catálogo" else "🔔  Atualizar catálogo (novo!)"

    private fun aplicarNovidades() {
        itensMenu["Configurações"]?.text = textoMenu("Configurações")
    }

    private fun abrirConfiguracoes() {
        val opcoes = arrayOf(
            textoAtualizar(),
            "🔑  Trocar código de acesso",
            "🧹  Limpar cache de imagens"
        )
        AlertDialog.Builder(this)
            .setTitle("Configurações")
            .setItems(opcoes) { _, i ->
                when (i) {
                    0 -> {
                        Toast.makeText(this, "Atualizando catálogo…", Toast.LENGTH_LONG).show()
                        carregarCatalogo(true)
                    }
                    1 -> {
                        prefsWolf.edit().remove("codigo").remove("ultima_ok").apply()
                        verificarAssinatura()
                    }
                    2 -> {
                        cacheCapas.evictAll()
                        pastaCacheImagens.listFiles()?.forEach { it.delete() }
                        Toast.makeText(this, "Cache de imagens limpo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

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
                            "Novidades: ${lista.joinToString(", ")}.",
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

    private fun extrairArray(texto: String?, chave: String): JSONArray? {
        if (texto.isNullOrBlank()) return null
        val limpo = texto.trim()

        return try {
            if (limpo.startsWith("[")) {
                JSONArray(limpo)
            } else {
                val obj = JSONObject(limpo)
                obj.optJSONArray(chave) ?: run {
                    // Se a chave tiver outro nome, usa a primeira lista do arquivo
                    var achada: JSONArray? = null
                    val chaves = obj.keys()
                    while (chaves.hasNext() && achada == null) {
                        achada = obj.optJSONArray(chaves.next())
                    }
                    achada
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // ───────────────────────── TELAS ─────────────────────────

    private var telaInicial = false

    private fun limparConteudo() {
        conteudo.removeAllViews()
        cardsAtuais.clear()
        linhasCards.clear()
        bannerView = null
        btnAssistir = null
        btnInfoBanner = null
        viewsContinuar = emptyList()
        cardsContinuar = emptyList()
        telaInicial = false
    }

    private fun mostrarMensagemVazia(texto: String) {
        val vazio = TextView(this).apply {
            this.text = texto
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }
        conteudo.addView(vazio, LinearLayout.LayoutParams(-1, -2))
    }

    private fun mostrarInicio() {
        historicoConteudo.clear()
        voltarListaAtual = { mostrarInicio() }
        limparConteudo()

        if (filmes.isEmpty() && series.isEmpty() && doramas.isEmpty() &&
            animes.isEmpty() && desenhos.isEmpty() && desenhosSeries.isEmpty()
        ) {
            mostrarMensagemVazia("Carregando catálogo…")
            return
        }

        telaInicial = true

        if (filmes.isNotEmpty()) montarBannerDestaque(filmes.first())

        if (filmes.isNotEmpty()) {
            conteudo.addView(
                criarCabecalho("Filmes", "🔥") { abrirSecao("Filmes") },
                LinearLayout.LayoutParams(-1, -2)
            )
            montarFileira(filmes.take(colunas).map { cardFilmePaisagem(it) })
        }
        if (series.isNotEmpty()) {
            conteudo.addView(
                criarCabecalho("Séries", "🎬") { abrirSecao("Séries") },
                LinearLayout.LayoutParams(-1, -2)
            )
            montarFileira(series.take(colunas).map { cardSeriePaisagem(it) })
        }
        if (doramas.isNotEmpty()) {
            conteudo.addView(
                criarCabecalho("Doramas", "🌸") { abrirSecao("Doramas") },
                LinearLayout.LayoutParams(-1, -2)
            )
            montarFileira(doramas.take(colunas).map { cardSeriePaisagem(it) })
        }
        if (animes.isNotEmpty()) {
            conteudo.addView(
                criarCabecalho("Anime", "🍥") { abrirSecao("Anime") },
                LinearLayout.LayoutParams(-1, -2)
            )
            montarFileira(animes.take(colunas).map { cardSeriePaisagem(it) })
        }
        if (desenhos.isNotEmpty() || desenhosSeries.isNotEmpty()) {
            conteudo.addView(
                criarCabecalho("Desenhos", "🧸") { abrirSecao("Desenhos") },
                LinearLayout.LayoutParams(-1, -2)
            )
            if (desenhos.isNotEmpty()) {
                montarFileira(desenhos.take(colunas).map { cardFilmePaisagem(it) })
            } else {
                montarFileira(desenhosSeries.take(colunas).map { cardSeriePaisagem(it) })
            }
        }

        try {
            montarFileiraContinuar()
        } catch (_: Throwable) {
        }

        if (ehTv) btnAssistir?.requestFocus()
    }

    private fun montarBannerDestaque(filme: Filme) {
        val altura = if (compacto) dp(220) else dp(250)

        val banner = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 12, 25))
                cornerRadius = dp(14).toFloat()
            }
            clipToOutline = true
        }

        val imagemFundo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.8f
        }
        carregarImagemFundoPrioritaria(filme.capa, imagemFundo)
        banner.addView(imagemFundo, FrameLayout.LayoutParams(-1, -1))

        banner.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.argb(250, 5, 6, 14), Color.argb(190, 5, 6, 14), Color.argb(30, 5, 6, 14))
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(16), dp(16), dp(16))
            gravity = Gravity.CENTER_VERTICAL
        }

        val tagL = TextView(this).apply {
            val anoTag = if (filme.ano > 0) filme.ano else 2026
            text = "🔥  LANÇAMENTO $anoTag"
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(12), dp(4), dp(14), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.rgb(200, 25, 30))
                cornerRadius = dp(12).toFloat()
            }
        }
        infoLayout.addView(tagL, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(8) })

        infoLayout.addView(TextView(this).apply {
            text = filme.titulo.uppercase()
            setTextColor(Color.WHITE)
            textSize = if (compacto) 20f else 30f
            setTypeface(null, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setShadowLayer(6f, 0f, 2f, Color.BLACK)
        })

        val tagsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        listOfNotNull(
            if (filme.ano > 0) filme.ano.toString() else null,
            filme.categoria.takeIf { it.isNotBlank() },
            "HD"
        ).forEach { t ->
            tagsRow.addView(chip(t, true), LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        }
        infoLayout.addView(tagsRow)

        infoLayout.addView(TextView(this).apply {
            text = if (filme.sinopse.isNotBlank()) filme.sinopse
            else "Assista agora em HD no WOLF IPTV."
            setTextColor(Color.rgb(215, 220, 235))
            textSize = 13f
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END
        })

        val acoesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }

        val assistir = botaoPilula("▶  Assistir agora", true) {
            abrirVideo(filme.titulo, filme.video, filme.capa)
        }
        acoesRow.addView(assistir, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(12) })

        val info = botaoPilula("★  Mais info", false) { abrirFilme(filme) }
        acoesRow.addView(info, LinearLayout.LayoutParams(-2, -2))

        infoLayout.addView(acoesRow)

        val larguraInfo = (resources.displayMetrics.widthPixels * (if (compacto) 0.92 else 0.5)).toInt()
        banner.addView(infoLayout, FrameLayout.LayoutParams(larguraInfo, -1))

        conteudo.addView(
            banner,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, altura).apply {
                bottomMargin = dp(8)
            }
        )

        bannerView = banner
        btnAssistir = assistir
        btnInfoBanner = info
    }

    // ----- Fileiras horizontais (Início) -----

    private fun montarLinha(cards: List<View>): LinearLayout {
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun parametros() = LinearLayout.LayoutParams(0, dp(130), 1f).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
            bottomMargin = dp(6)
        }
        cards.forEach { linha.addView(it, parametros()) }
        repeat(colunas - cards.size) { linha.addView(View(this), parametros()) }
        return linha
    }

    private fun montarFileira(cards: List<View>) {
        val linha = montarLinha(cards)
        conteudo.addView(linha, LinearLayout.LayoutParams(-1, dp(142)))
        cardsAtuais.addAll(cards)
        linhasCards.add(cards)
    }

    private var viewsContinuar: List<View> = emptyList()
    private var cardsContinuar: List<View> = emptyList()

    private fun removerContinuar() {
        viewsContinuar.forEach { conteudo.removeView(it) }
        val antigos = cardsContinuar
        cardsAtuais.removeAll(antigos.toSet())
        linhasCards.removeAll { it === antigos }
        viewsContinuar = emptyList()
        cardsContinuar = emptyList()
    }

    private fun montarFileiraContinuar() {
        removerContinuar()
        val itens = try {
            WolfProgress.emAndamento(this).take(colunas)
        } catch (_: Throwable) {
            emptyList()
        }
        if (itens.isEmpty()) return

        val cabecalho = criarCabecalho("Continue assistindo", "⏳") { abrirSecao("Continue assistindo") }
        val cards = itens.map { item ->
            criarCardPaisagem(item.capa, item.titulo, listOf(item.resumo()), item).apply {
                setOnClickListener { abrirVideo(item.titulo, item.url, item.capa) }
            }
        }
        val linha = montarLinha(cards)

        val indice = if (bannerView != null) 1 else 0
        conteudo.addView(cabecalho, indice, LinearLayout.LayoutParams(-1, -2))
        conteudo.addView(linha, indice + 1, LinearLayout.LayoutParams(-1, dp(142)))
        cardsAtuais.addAll(cards)
        linhasCards.add(0, cards)

        viewsContinuar = listOf(cabecalho, linha)
        cardsContinuar = cards
    }

    private fun adicionarBarraProgresso(card: View, item: ProgressoItem) {
        val moldura = card as? FrameLayout ?: return
        if (item.duracao <= 0L) return

        val pct = ((item.posicao * 100) / item.duracao).toInt().coerceIn(0, 100)
        val trilho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 100f
            setBackgroundColor(Color.argb(170, 60, 66, 90))
        }
        trilho.addView(
            View(this).apply { setBackgroundColor(Color.rgb(255, 40, 40)) },
            LinearLayout.LayoutParams(0, -1, pct.toFloat())
        )
        moldura.addView(
            trilho,
            FrameLayout.LayoutParams(-1, dp(3)).apply { gravity = Gravity.BOTTOM }
        )
    }

    // ----- Cards -----

    private fun chipsFilme(f: Filme): List<String> = listOfNotNull(
        if (f.ano > 0) f.ano.toString() else null,
        f.categoria.takeIf { it.isNotBlank() },
        "HD"
    )

    private fun chipsSerie(s: Serie): List<String> {
        val n = s.temporadas.size
        return listOfNotNull(
            if (n > 1) "T1–T$n" else if (n == 1) "T1" else null,
            s.categoria.takeIf { it.isNotBlank() },
            "HD"
        )
    }

    private fun criarCardPaisagem(
        capa: String,
        titulo: String,
        chips: List<String>,
        progresso: ProgressoItem? = null
    ): FrameLayout {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
            clipToOutline = true
        }

        val imagem = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        carregarImagem(capa, imagem)
        card.addView(imagem, FrameLayout.LayoutParams(-1, -1))

        card.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.TRANSPARENT, Color.argb(110, 0, 0, 0), Color.argb(240, 4, 6, 14))
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(8))
        }
        info.addView(
            TextView(this).apply {
                text = titulo
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setShadowLayer(4f, 0f, 1f, Color.BLACK)
            },
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }
        )
        val chipsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        chips.take(if (compacto) 2 else 3).forEach {
            chipsRow.addView(chip(it), LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
        info.addView(chipsRow)
        card.addView(info, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.BOTTOM })

        val borda = View(this).apply {
            background = criarBordaVermelha()
            visibility = View.GONE
        }
        card.addView(borda, FrameLayout.LayoutParams(-1, -1))
        card.setOnFocusChangeListener { _, foco ->
            borda.visibility = if (foco) View.VISIBLE else View.GONE
            card.background = criarFundoCard(foco)
        }

        if (progresso != null) adicionarBarraProgresso(card, progresso)
        return card
    }

    private fun cardFilmePaisagem(f: Filme): FrameLayout =
        criarCardPaisagem(f.capa, f.titulo, chipsFilme(f)).apply {
            setOnClickListener { abrirFilme(f) }
            setOnLongClickListener { alternarFavorito(f.titulo); true }
        }

    private fun cardSeriePaisagem(s: Serie): FrameLayout =
        criarCardPaisagem(s.capa, s.titulo, chipsSerie(s)).apply {
            setOnClickListener { abrirSerie(s) }
            setOnLongClickListener { alternarFavorito(s.titulo); true }
        }

    private fun rodapeDuasLinhas(linha1: String, linha2: String): View {
        val informacoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(40, 6, 9, 22), Color.argb(240, 6, 9, 22))
            )
        }

        val titulo = TextView(this).apply {
            text = linha1
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        informacoes.addView(titulo, LinearLayout.LayoutParams(-1, dp(22)))

        val detalhes = TextView(this).apply {
            text = linha2
            setTextColor(Color.rgb(175, 180, 200))
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        informacoes.addView(detalhes, LinearLayout.LayoutParams(-1, dp(18)))

        return informacoes
    }

    private fun criarCardBase(capa: String, rodape: View): Pair<FrameLayout, ImageView> {
        val card = FrameLayout(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = criarFundoCard(false)
            clipToOutline = true
        }

        val imagem = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        carregarImagem(capa, imagem)

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(210)
            )
        )

        card.addView(
            rodape,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply { gravity = Gravity.BOTTOM }
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
        }

        return Pair(card, imagem)
    }

    // ----- Grades -----

    private fun montarGrade(cards: List<View>) {
        cards.chunked(colunas).forEach { grupo ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            fun parametros() = LinearLayout.LayoutParams(0, dp(260), 1f).apply {
                leftMargin = dp(6)
                rightMargin = dp(6)
                bottomMargin = dp(16)
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
                    dp(275)
                )
            )
            linhasCards.add(grupo)
        }
        if (ehTv) cardsAtuais.firstOrNull()?.requestFocus()
    }

    private fun <T> montarGradePaginada(itens: List<T>, inicio: Int = 0, criar: (T) -> View) {
        val fim = minOf(inicio + TAMANHO_PAGINA, itens.size)
        montarGrade(itens.subList(inicio, fim).map { criar(it) })

        if (fim < itens.size) {
            val restantes = itens.size - fim
            val btn = botaoPilula("Mostrar mais  ($restantes)", false) {}
            btn.setOnClickListener {
                conteudo.removeView(btn)
                linhasCards.removeAll { it.size == 1 && it[0] === btn }
                montarGradePaginada(itens, fim, criar)
            }
            conteudo.addView(
                btn,
                LinearLayout.LayoutParams(-2, -2).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(6)
                    bottomMargin = dp(20)
                }
            )
            linhasCards.add(listOf(btn))
        }
    }

    private fun mostrarColecao(
        titulo: String,
        icone: String,
        listaFilmes: List<Filme>,
        listaSeries: List<Serie>,
        vazio: String,
        voltar: () -> Unit
    ) {
        historicoConteudo.clear()
        voltarListaAtual = voltar
        limparConteudo()

        if (listaFilmes.isEmpty() && listaSeries.isEmpty()) {
            mostrarMensagemVazia(vazio)
            return
        }

        val dois = listaFilmes.isNotEmpty() && listaSeries.isNotEmpty()

        if (listaFilmes.isNotEmpty()) {
            conteudo.addView(
                tituloSecao(if (dois) "$titulo • Filmes" else "$titulo  (${listaFilmes.size})", icone),
                LinearLayout.LayoutParams(-1, -2)
            )
            if (dois) montarGrade(listaFilmes.map { criarCard(it) })
            else montarGradePaginada(listaFilmes) { criarCard(it) }
        }

        if (listaSeries.isNotEmpty()) {
            conteudo.addView(
                tituloSecao(if (dois) "$titulo • Séries" else "$titulo  (${listaSeries.size})", icone),
                LinearLayout.LayoutParams(-1, -2).apply { if (dois) topMargin = dp(10) }
            )
            if (dois) montarGrade(listaSeries.map { criarCardSerie(it) })
            else montarGradePaginada(listaSeries) { criarCardSerie(it) }
        }
    }

    // ----- Filmes -----

    private fun abrirFilme(filme: Filme) {
        mostrarDetalhes(
            filme.titulo,
            filme.capa,
            if (filme.ano > 0) "${filme.ano}  •  ${filme.categoria}" else filme.categoria,
            filme.sinopse,
            "▶  ASSISTIR",
            WolfProgress.posicao(this, filme.video),
            true
        ) { abrirVideo(filme.titulo, filme.video, filme.capa) }
    }

    private fun criarCard(filme: Filme, comDetalhes: Boolean = true): View {
        val (card, _) = criarCardBase(
            filme.capa,
            rodapeDuasLinhas(
                filme.titulo,
                if (filme.ano > 0) "${filme.ano} • ${filme.categoria}" else filme.categoria
            )
        )

        card.setOnClickListener {
            if (comDetalhes) abrirFilme(filme)
            else abrirVideo(filme.titulo, filme.video, filme.capa)
        }
        card.setOnLongClickListener { alternarFavorito(filme.titulo); true }
        return card
    }

    private fun alternarFavorito(titulo: String) {
        val agora = if (favoritos.contains(titulo)) {
            favoritos.remove(titulo)
            false
        } else {
            favoritos.add(titulo)
            true
        }
        prefsWolf.edit().putStringSet("favoritos", HashSet(favoritos)).apply()
        Toast.makeText(
            this,
            if (agora) "★ Adicionado aos favoritos" else "Removido dos favoritos",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun botaoDetalhe(texto: String, principal: Boolean): TextView {
        val b = TextView(this).apply {
            text = texto
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            setPadding(dp(24), dp(12), dp(24), dp(12))
        }
        fun estilo(foco: Boolean) {
            val g = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(if (principal) Color.rgb(220, 20, 30) else Color.argb(150, 60, 60, 60))
                if (foco) setStroke(dp(3), Color.WHITE)
            }
            b.background = g
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
        val dialogo = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val raizD = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        val fundo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0.30f
        }
        carregarImagem(capa, fundo, resources.displayMetrics.widthPixels / 2, resources.displayMetrics.heightPixels / 2, true)
        raizD.addView(fundo, FrameLayout.LayoutParams(-1, -1))

        val veu = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.argb(235, 0, 0, 0), Color.argb(120, 0, 0, 0))
            )
        }
        raizD.addView(veu, FrameLayout.LayoutParams(-1, -1))

        val paisagem = resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels

        val poster = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply {
                setColor(Color.argb(120, 20, 20, 20))
                cornerRadius = dp(12).toFloat()
            }
            clipToOutline = true
        }
        carregarImagem(capa, poster)

        val coluna = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        coluna.addView(TextView(this).apply {
            text = titulo
            setTextColor(Color.WHITE)
            textSize = if (paisagem) 30f else 24f
            typeface = Typeface.DEFAULT_BOLD
        })

        coluna.addView(TextView(this).apply {
            text = meta
            setTextColor(Color.rgb(255, 90, 90))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(12))
        })

        coluna.addView(TextView(this).apply {
            text = "SINOPSE"
            setTextColor(Color.rgb(170, 170, 170))
            textSize = 12f
            letterSpacing = 0.15f
            typeface = Typeface.DEFAULT_BOLD
        })

        coluna.addView(TextView(this).apply {
            text = if (sinopse.isNotBlank()) sinopse else "Sinopse ainda não disponível para este título."
            setTextColor(Color.rgb(220, 220, 220))
            textSize = 16f
            setLineSpacing(0f, 1.2f)
        })

        val botoes = LinearLayout(this).apply {
            orientation = if (paisagem) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(0, dp(24), 0, 0)
        }

        val assistir = botaoDetalhe(
            if (posicaoMs > 0L) "▶  CONTINUAR (${WolfProgress.formatar(posicaoMs)})" else textoAssistir,
            true
        )
        assistir.setOnClickListener {
            if (manterAberta) {
                aoAssistir()
            } else {
                dialogo.dismiss()
                aoAssistir()
            }
        }
        botoes.addView(assistir, LinearLayout.LayoutParams(-2, -2).apply {
            rightMargin = dp(12)
            bottomMargin = dp(10)
        })

        fun textoFav() = if (favoritos.contains(titulo)) "★  FAVORITO" else "☆  FAVORITAR"
        val favorito = botaoDetalhe(textoFav(), false)
        favorito.setOnClickListener {
            alternarFavorito(titulo)
            favorito.text = textoFav()
        }
        botoes.addView(favorito, LinearLayout.LayoutParams(-2, -2).apply {
            rightMargin = dp(12)
            bottomMargin = dp(10)
        })

        val voltar = botaoDetalhe("VOLTAR", false)
        voltar.setOnClickListener { dialogo.dismiss() }
        botoes.addView(voltar, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(10) })

        coluna.addView(botoes)

        val conteudoD = LinearLayout(this).apply {
            orientation = if (paisagem) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dp(40), dp(28), dp(40), dp(28))
        }

        if (paisagem) {
            conteudoD.addView(poster, LinearLayout.LayoutParams(dp(220), dp(330)).apply { rightMargin = dp(32) })
        } else {
            conteudoD.addView(poster, LinearLayout.LayoutParams(dp(160), dp(240)).apply { bottomMargin = dp(16); gravity = Gravity.CENTER_HORIZONTAL })
        }

        val rolagem = ScrollView(this).apply { isFillViewport = true }
        rolagem.addView(coluna)
        conteudoD.addView(rolagem, LinearLayout.LayoutParams(0, -1, 1f).also {
            if (!paisagem) { it.width = -1; it.height = 0 }
        })

        raizD.addView(conteudoD, FrameLayout.LayoutParams(-1, -1))
        dialogo.setContentView(raizD)
        dialogo.show()
        assistir.requestFocus()
    }

    // ----- Séries -----

    private fun abrirSerie(serie: Serie) {
        val total = serie.temporadas.sumOf { it.episodios.size }
        mostrarDetalhes(
            serie.titulo,
            serie.capa,
            "${serie.categoria}  •  ${serie.temporadas.size} temp  •  $total eps",
            serie.sinopse,
            "▶  VER EPISÓDIOS",
            0L
        ) {
            val voltar = voltarListaAtual
            historicoConteudo.add { voltar() }
            if (serie.temporadas.size == 1 && desenhosSeries.any { it === serie }) {
                mostrarEpisodios(serie, serie.temporadas[0])
            } else {
                mostrarTemporadas(serie)
            }
        }
    }

    private fun criarCardSerie(serie: Serie): View {
        val (card, _) = criarCardBase(
            serie.capa,
            rodapeDuasLinhas(serie.titulo, "${serie.categoria} • ${serie.temporadas.size} temp")
        )

        card.setOnClickListener { abrirSerie(serie) }
        card.setOnLongClickListener { alternarFavorito(serie.titulo); true }
        return card
    }

    private fun mostrarTemporadas(serie: Serie) {
        limparConteudo()
        montarGrade(serie.temporadas.map { criarCardTemporada(serie, it) })
    }

    private fun criarCardTemporada(serie: Serie, temporada: Temporada): View {
        val rodape = TextView(this).apply {
            text = "Temporada ${temporada.numero}"
            setTextColor(Color.WHITE)
            textSize = 15f
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

    private fun mostrarEpisodios(serie: Serie, temporada: Temporada) {
        limparConteudo()
        montarGrade(temporada.episodios.map { criarCardEpisodio(serie, temporada.numero, it) })
    }

    private fun chaveEpisodio(serie: Serie, numTemp: Int, numEp: Int): String {
        return "${serie.titulo}|T$numTemp|E$numEp"
    }

    private fun episodioAssistido(chave: String): Boolean {
        return prefsWolf.getStringSet("assistidos", emptySet())?.contains(chave) == true
    }

    private fun marcarEpisodio(chave: String, assistido: Boolean) {
        val novo = HashSet(prefsWolf.getStringSet("assistidos", emptySet()) ?: emptySet())
        if (assistido) novo.add(chave) else novo.remove(chave)
        prefsWolf.edit().putStringSet("assistidos", novo).apply()
    }

    private val prefsWolf by lazy { getSharedPreferences("wolf_prefs", MODE_PRIVATE) }

    private fun criarCardEpisodio(serie: Serie, numTemp: Int, episodio: Episodio): View {
        val chave = chaveEpisodio(serie, numTemp, episodio.numero)
        val textoBase = "EP ${episodio.numero} • ${episodio.titulo}"

        val informacoes = TextView(this).apply {
            text = textoBase
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setBackgroundColor(Color.argb(235, 10, 10, 10))
        }

        val (card, imagem) = criarCardBase(serie.capa, informacoes)
        val selo = TextView(this).apply {
            text = "✓ VISTO"
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                setColor(Color.rgb(0, 150, 60))
                cornerRadius = dp(6).toFloat()
            }
            visibility = View.GONE
        }

        card.addView(
            selo,
            FrameLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(6)
                rightMargin = dp(6)
            }
        )

        fun atualizarVisual() {
            val visto = episodioAssistido(chave)
            selo.visibility = if (visto) View.VISIBLE else View.GONE
            imagem.alpha = if (visto) 0.45f else 1f
        }
        atualizarVisual()

        card.setOnClickListener {
            if (episodio.video.isNotBlank()) {
                marcarEpisodio(chave, true)
                atualizarVisual()
            }
            abrirVideo("${serie.titulo} - EP ${episodio.numero}", episodio.video, serie.capa)
        }

        card.setOnLongClickListener {
            val agora = !episodioAssistido(chave)
            marcarEpisodio(chave, agora)
            atualizarVisual()
            true
        }

        return card
    }

    // ----- Player -----

    private fun abrirVideo(titulo: String, video: String, capa: String) {
        if (video.isBlank()) {
            Toast.makeText(this, "Vídeo indisponível", Toast.LENGTH_SHORT).show()
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
            .setMessage("Continuar de onde parou (${WolfProgress.formatar(pos)})?")
            .setPositiveButton("Continuar") { _, _ -> iniciarPlayer(titulo, video, capa, pos) }
            .setNegativeButton("Do início") { _, _ ->
                WolfProgress.apagar(this, video)
                iniciarPlayer(titulo, video, capa, 0L)
            }
            .show()
    }

    private fun iniciarPlayer(titulo: String, video: String, capa: String, posicaoMs: Long) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("VIDEO_URL", video)
            putExtra("VIDEO_TITLE", titulo)
            putExtra("VIDEO_COVER", capa)
            putExtra("START_POSITION", posicaoMs)
        }
        startActivity(intent)
    }

    // ----- Canais (mantido, ainda sem botão no menu) -----

    private fun carregarCanais() {
        limparConteudo()
        mostrarMensagemVazia("Carregando canais ao vivo...")
        activityScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val texto = baixarTexto(CANAIS_URL)
                    if (texto.isNullOrBlank()) emptyList()
                    else {
                        val raizJson = texto.trim()
                        val array = if (raizJson.startsWith("[")) {
                            JSONArray(raizJson)
                        } else {
                            val obj = JSONObject(raizJson)
                            obj.optJSONArray("canais") ?: obj.optJSONArray("channels") ?: JSONArray()
                        }
                        val lista = mutableListOf<Canal>()
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val nome = item.optString("nome", item.optString("name", "")).trim()
                            val url = item.optString("url", item.optString("link", "")).trim()
                            val logo = logoDoCanal(item.optString("logo", "").trim())
                            val categoria = item.optString("categoria", "Canais ao vivo")
                            if (nome.isNotBlank() && url.startsWith("http")) {
                                lista.add(Canal(nome, url, logo, categoria))
                            }
                        }
                        lista
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            canais.clear()
            canais.addAll(resultado)
            mostrarListaCanais()
        }
    }

    private fun mostrarListaCanais() {
        limparConteudo()
        if (canais.isEmpty()) {
            mostrarMensagemVazia("Nenhum canal encontrado em canais.json.")
            return
        }
        conteudo.addView(tituloSecao("CANAIS AO VIVO  •  ${canais.size}", "📡"))
        montarGrade(canais.map { criarCardCanal(it) })
    }

    private fun logoDoCanal(logo: String): String {
        if (logo.isBlank()) return ""
        val dominio = Regex("^https?://logo\\.clearbit\\.com/([^/?#]+)").find(logo)?.groupValues?.get(1)
        return if (dominio != null) "https://www.google.com/s2/favicons?sz=256&domain=$dominio" else logo
    }

    private fun criarCardCanal(canal: Canal): View {
        val (card, imagem) = criarCardBase(canal.logo, rodapeDuasLinhas(canal.nome, canal.categoria))
        imagem.setBackgroundColor(Color.rgb(12, 16, 34))
        card.setOnClickListener {
            abrirVideo(canal.nome, canal.url, canal.logo)
        }
        return card
    }

    // ----- Continue assistindo -----

    private fun mostrarContinuarAssistindo() {
        val itens = WolfProgress.emAndamento(this)
        historicoConteudo.clear()
        voltarListaAtual = { mostrarContinuarAssistindo() }
        limparConteudo()
        if (itens.isEmpty()) {
            mostrarMensagemVazia("Nada em andamento")
            return
        }
        conteudo.addView(tituloSecao("Continue assistindo", "⏳"), LinearLayout.LayoutParams(-1, -2))
        montarGrade(itens.map { criarCard(Filme(it.titulo, 0, it.resumo(), it.capa, it.url), false) })
    }

    // ----- Pesquisa -----

    private fun abrirPesquisa() {
        val campo = EditText(this).apply {
            hint = "Nome do filme ou série..."
            textSize = 18f
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Pesquisar")
            .setView(campo)
            .setPositiveButton("Buscar") { _, _ -> pesquisar(campo.text.toString()) }
            .setNeutralButton("🎤 Voz") { _, _ -> iniciarVoz() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pesquisar(consulta: String) {
        val termo = normalizarTexto(consulta)
        if (termo.isBlank()) return
        val achadosFilmes = (filmes + desenhos).filter { normalizarTexto(it.titulo).contains(termo) }
        val achadasSeries = (series + doramas + animes + desenhosSeries)
            .filter { normalizarTexto(it.titulo).contains(termo) }

        mostrarColecao(
            "Resultados", "🔍", achadosFilmes, achadasSeries,
            "Nada encontrado para \"$consulta\""
        ) { pesquisar(consulta) }
    }

    private fun iniciarVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }
        try {
            startActivityForResult(intent, REQ_VOZ)
        } catch (_: Exception) {
            Toast.makeText(this, "Voz indisponível", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VOZ && resultCode == RESULT_OK) {
            val texto = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!texto.isNullOrBlank()) pesquisar(texto)
        }
    }

    // ───────────────────────── CONTROLE REMOTO (TV) ─────────────────────────

    private fun focarConteudo() {
        (btnAssistir ?: linhasCards.firstOrNull()?.firstOrNull())?.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val foco = currentFocus
            val tecla = event.keyCode
            val linhaIdx = if (foco == null) -1 else linhasCards.indexOfFirst { l -> l.any { it === foco } }

            if (linhaIdx >= 0 &&
                (tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    tecla == KeyEvent.KEYCODE_DPAD_UP || tecla == KeyEvent.KEYCODE_DPAD_DOWN)
            ) {
                val linha = linhasCards[linhaIdx]
                val col = linha.indexOfFirst { it === foco }
                when (tecla) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (col > 0) linha[col - 1].requestFocus()
                        else itensMenu[menuAtual]?.requestFocus()
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (col < linha.lastIndex) linha[col + 1].requestFocus()
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (linhaIdx > 0) {
                            val acima = linhasCards[linhaIdx - 1]
                            acima[minOf(col, acima.lastIndex)].requestFocus()
                        } else {
                            (btnAssistir ?: campoPesquisaTopo).requestFocus()
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (linhaIdx < linhasCards.lastIndex) {
                            val abaixo = linhasCards[linhaIdx + 1]
                            abaixo[minOf(col, abaixo.lastIndex)].requestFocus()
                        }
                    }
                }
                return true
            }

            if (foco != null) {
                if (tecla == KeyEvent.KEYCODE_DPAD_RIGHT && itensMenu.values.any { it === foco }) {
                    focarConteudo()
                    return true
                }
                if (tecla == KeyEvent.KEYCODE_DPAD_DOWN &&
                    (foco === btnAssistir || foco === btnInfoBanner || foco === campoPesquisaTopo)
                ) {
                    val primeiro = linhasCards.firstOrNull()?.firstOrNull()
                    if (primeiro != null) {
                        primeiro.requestFocus()
                        return true
                    }
                }
            }

            if (tecla == KeyEvent.KEYCODE_BACK) {
                if (historicoConteudo.isNotEmpty()) {
                    historicoConteudo.removeAt(historicoConteudo.size - 1).invoke()
                    return true
                } else if (!telaInicial && filmes.isNotEmpty()) {
                    abrirSecao("Início")
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ───────────────────────── CICLO DE VIDA ─────────────────────────

    @Volatile
    private var noPrimeiroPlano = true

    override fun onStart() { super.onStart(); noPrimeiroPlano = true }
    override fun onStop() { noPrimeiroPlano = false; super.onStop() }

    override fun onResume() {
        super.onResume()
        if (telaInicial && catalogoIniciado && filmes.isNotEmpty()) {
            try {
                montarFileiraContinuar()
            } catch (_: Throwable) {
            }
        }
    }

    override fun onDestroy() {
        activityScope.coroutineContext[Job]?.cancel()
        executorImagens.shutdownNow()
        executorPrecarga.shutdownNow()
        super.onDestroy()
    }
}
