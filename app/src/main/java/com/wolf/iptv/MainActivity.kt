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

    private val ARQUIVOS_NOVELAS =
        listOf("novelas.json", "novela.json", "Novelas.json", "Novela.json")

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
        "Coletâneas" to "📚",
        "Canais ao vivo" to "📡",
        "Séries" to "📺",
        "Doramas" to "🌸",
        "Novelas" to "💕",
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
    private val executorImagens = Executors.newFixedThreadPool(4)

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
    private val novelas = mutableListOf<Serie>()
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
        scrollPrincipal = scroll
        scroll.viewTreeObserver.addOnScrollChangedListener {
            val filho = scroll.getChildAt(0) ?: return@addOnScrollChangedListener
            val faltando = filho.height - (scroll.scrollY + scroll.height)
            if (faltando < dp(1200)) carregarMaisLinhas()
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
            "Filmes" -> mostrarCategoriasColecao(
                "Filmes", "🔥", filmes, emptyList(), "Nenhum filme encontrado"
            ) { abrirSecao("Filmes") }
            "Coletâneas" -> mostrarAbaColetaneas()
            "Canais ao vivo" -> carregarCanais()
            "Séries" -> mostrarCategoriasColecao(
                "Séries", "🎬", emptyList(), series, "Nenhuma série encontrada"
            ) { abrirSecao("Séries") }
            "Doramas" -> mostrarCategoriasColecao(
                "Doramas", "🌸", emptyList(), doramas, "Nenhum dorama encontrado"
            ) { abrirSecao("Doramas") }
            "Novelas" -> mostrarCategoriasColecao(
                "Novelas", "💕", emptyList(), novelas, "Nenhuma novela encontrada",
                { abrirSecao("Novelas") },
                { categoriasNovela(it) },
                listOf("novelas mexicanas", "novelas brasileiras", "novelas turcas")
            )
            "Anime" -> mostrarCategoriasColecao(
                "Anime", "🍥", emptyList(), animes, "Nenhum anime encontrado"
            ) { abrirSecao("Anime") }
            "Desenhos" -> mostrarCategoriasColecao(
                "Desenhos", "🧸", desenhos, desenhosSeries, "Nenhum desenho encontrado"
            ) { abrirSecao("Desenhos") }
            "Favoritos" -> mostrarColecao(
                "Favoritos", "★",
                (filmes + desenhos).filter { favoritos.contains(it.titulo) },
                (series + doramas + novelas + animes + desenhosSeries).filter { favoritos.contains(it.titulo) },
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
            val botao = TextView(this).apply {
                this.text = "Ver todos  ›"
                setTextColor(Color.WHITE)
                textSize = 13f
                isClickable = true
                isFocusable = true
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener { aoVerTodos() }
            }
            fun estiloVerTodos(foco: Boolean) {
                botao.background = if (foco) GradientDrawable().apply {
                    setColor(Color.argb(90, 255, 255, 255))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(2), Color.rgb(255, 40, 40))
                } else null
            }
            estiloVerTodos(false)
            botao.setOnFocusChangeListener { _, f -> estiloVerTodos(f) }
            linha.tag = botao
            linha.addView(botao)
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

        if (!imagem.isAttachedToWindow) {
            imagem.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    if (v.tag == url) carregarImagem(url, imagem, larguraAlvo, alturaAlvo, leve)
                }
                override fun onViewDetachedFromWindow(v: View) {}
            })
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

    // Descobre o ano certo do filme sozinho (vale para filmes novos também).
    // Prioridade: (2019) no título > campo ano/year/lancamento do JSON > ano no nome do arquivo de vídeo
    // > ano solto no fim do título. Anos absurdos (ex.: "Blade Runner 2049") são ignorados.
    private fun anoDoItem(item: JSONObject): Int {
        val maximo = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 1
        fun valido(a: Int) = a in 1900..maximo

        val titulo = item.optString("titulo", "")
        val video = Uri.decode(item.optString("video", ""))
        val nomeVideo = video.substringBefore('?').substringAfterLast('/')

        // 1) ano entre parênteses/colchetes no título
        Regex("[(\\[]\\s*((?:19|20)\\d{2})\\s*[)\\]]").findAll(titulo).lastOrNull()
            ?.groupValues?.get(1)?.toIntOrNull()?.let { if (valido(it)) return it }

        // 2) campos do JSON (aceita número ou texto como "2019-05-10")
        for (campo in listOf("ano", "year", "lancamento", "lançamento", "data", "release_date")) {
            val bruto = item.opt(campo)?.toString().orEmpty()
            if (bruto.isBlank()) continue
            Regex("(?<!\\d)((?:19|20)\\d{2})(?!\\d)").find(bruto)
                ?.groupValues?.get(1)?.toIntOrNull()?.let { if (valido(it)) return it }
        }

        // 3) ano no nome do arquivo de vídeo, cercado por separadores
        Regex("[ ._\\-(\\[]((?:19|20)\\d{2})[ ._\\-)\\]]").find(" $nomeVideo ")
            ?.groupValues?.get(1)?.toIntOrNull()?.let { if (valido(it)) return it }

        // 4) ano solto no fim do título (só se o título tiver mais palavras, ex.: "Titulo 2019")
        val m = Regex("^(.+\\S)\\s+((?:19|20)\\d{2})$").find(titulo.trim())
        if (m != null) {
            val a = m.groupValues[2].toIntOrNull() ?: 0
            if (valido(a)) return a
        }
        return 0
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
                val textoNovelas = textoDe("novelas", ARQUIVOS_NOVELAS)
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

                val novasNovelas = lerSeries(
                    extrairArray(textoNovelas, "novelas")
                        ?: raizJson.optJSONArray("novelas")
                ).map { organizarNovela(it) }

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

                    novelas.clear()
                    novelas.addAll(novasNovelas)

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
                        "novelas" to (textoNovelas?.trim()?.hashCode() ?: 0),
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
                            novelas.map { it.capa } +
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
                    novelas.clear()
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

    // ───────────────────────── TELA DE LOGIN (CÓDIGO) ─────────────────────────

    private val MSG_PADRAO_CODIGO = "Digite o código de acesso"
    private var telaLogin: FrameLayout? = null
    private var campoLogin: EditText? = null
    private var botaoLogin: TextView? = null
    private var erroLogin: TextView? = null
    private var verificandoLogin = false
    private val focoAnterior = HashMap<ViewGroup, Int>()

    private val FUNDO_LOGIN_PARTES = listOf(
        "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"
    )

    private fun bitmapFundoLogin(): Bitmap? = try {
        val bytes = android.util.Base64.decode(FUNDO_LOGIN_PARTES.joinToString(""), android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) {
        null
    }

    private fun bitmapLogoLogin(): Bitmap? = try {
        val bytes = android.util.Base64.decode(LOGO_PARTES.joinToString(""), android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) {
        null
    }

    private fun bloquearFocoAtras(bloquear: Boolean, tela: View?) {
        if (bloquear) {
            for (i in 0 until raiz.childCount) {
                val filho = raiz.getChildAt(i)
                if (filho === tela || filho === splash || filho !is ViewGroup) continue
                focoAnterior[filho] = filho.descendantFocusability
                filho.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            }
        } else {
            focoAnterior.forEach { (v, antigo) -> v.descendantFocusability = antigo }
            focoAnterior.clear()
        }
    }

    private fun pedirCodigo(mensagem: String) {
        esconderSplash(true)
        if (isFinishing) return

        val ehErro = !mensagem.startsWith("Digite o código")

        // Já está aberta: só atualiza a mensagem
        if (telaLogin != null) {
            verificandoLogin = false
            botaoLogin?.apply { text = "→   Entrar"; alpha = 1f; isEnabled = true }
            erroLogin?.apply {
                text = if (ehErro) mensagem else ""
                visibility = if (ehErro) View.VISIBLE else View.INVISIBLE
            }
            return
        }

        val prefs = getSharedPreferences("wolf_prefs", MODE_PRIVATE)
        val vermelho = Color.rgb(235, 28, 40)
        val larg = resources.displayMetrics.widthPixels
        val alt = resources.displayMetrics.heightPixels

        val tela = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(6, 6, 10))
            isClickable = true
            isFocusable = false
        }

        // Fundo: imagem do app bem escurecida + brilho vermelho embaixo
        val fundo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val bmpFundo = bitmapFundoLogin()
        if (bmpFundo != null) fundo.setImageBitmap(bmpFundo) else carregarFundoApp(fundo)
        tela.addView(fundo, FrameLayout.LayoutParams(-1, -1))

        // Em telas estreitas (celular em pé) escurece mais para o texto ficar legível
        val retrato = alt > larg
        tela.addView(
            View(this).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.argb(if (retrato) 190 else 40, 4, 4, 8),
                        Color.argb(if (retrato) 170 else 20, 8, 4, 8),
                        Color.argb(if (retrato) 215 else 60, 60, 4, 10)
                    )
                )
            },
            FrameLayout.LayoutParams(-1, -1)
        )

        val larguraColuna = minOf(larg - dp(32), dp(560))

        val coluna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        // Logo
        bitmapLogoLogin()?.let { bmp ->
            val tamanho = minOf((larg * 0.7f).toInt(), (alt * 0.30f).toInt(), dp(300))
            coluna.addView(
                ImageView(this).apply {
                    setImageBitmap(bmp)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(tamanho, tamanho).apply { bottomMargin = dp(6) }
            )
        }

        // Título
        coluna.addView(TextView(this).apply {
            text = MSG_PADRAO_CODIGO
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(larguraColuna, -2))
        coluna.addView(TextView(this).apply {
            text = "para começar a assistir"
            setTextColor(Color.rgb(190, 192, 205))
            textSize = 16f
            letterSpacing = 0.05f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(larguraColuna, -2).apply { bottomMargin = dp(18) })

        // Campo do código
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
        }
        fun estiloCaixa(foco: Boolean) {
            caixa.background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.argb(235, 8, 8, 12))
                setStroke(dp(if (foco) 3 else 2), if (foco) Color.rgb(255, 80, 90) else vermelho)
            }
        }
        estiloCaixa(false)

        caixa.addView(TextView(this).apply {
            text = "⌨"
            textSize = 26f
            setTextColor(Color.rgb(170, 172, 185))
        }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(14) })

        val campo = EditText(this).apply {
            hint = "Ex: 123456"
            setHintTextColor(Color.rgb(120, 122, 135))
            setTextColor(Color.WHITE)
            textSize = 20f
            letterSpacing = 0.08f
            setSingleLine(true)
            background = null
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setText(prefs.getString("codigo", "") ?: "")
            setPadding(0, 0, 0, 0)
            isFocusable = true
            isFocusableInTouchMode = true
            setOnFocusChangeListener { _, f -> estiloCaixa(f) }
        }
        caixa.addView(campo, LinearLayout.LayoutParams(0, -1, 1f))
        coluna.addView(caixa, LinearLayout.LayoutParams(larguraColuna, dp(62)))
        campoLogin = campo

        // Mensagem de erro
        val erro = TextView(this).apply {
            text = if (ehErro) mensagem else ""
            visibility = if (ehErro) View.VISIBLE else View.INVISIBLE
            setTextColor(Color.rgb(255, 105, 105))
            textSize = 14f
            gravity = Gravity.CENTER
            minHeight = dp(22)
        }
        coluna.addView(erro, LinearLayout.LayoutParams(larguraColuna, -2).apply {
            topMargin = dp(8); bottomMargin = dp(8)
        })
        erroLogin = erro

        // Botão Entrar
        val botao = TextView(this).apply {
            text = "→   Entrar"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }
        fun estiloBotao(foco: Boolean) {
            botao.background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(255, 45, 55), Color.rgb(190, 10, 22))
            ).apply {
                cornerRadius = dp(40).toFloat()
                if (foco) setStroke(dp(3), Color.WHITE)
            }
            botao.animate().scaleX(if (foco) 1.04f else 1f).scaleY(if (foco) 1.04f else 1f).setDuration(120).start()
        }
        estiloBotao(false)
        botao.setOnFocusChangeListener { _, f -> estiloBotao(f) }
        coluna.addView(botao, LinearLayout.LayoutParams((larguraColuna * 0.78f).toInt(), dp(58)))
        botaoLogin = botao

        fun entrar() {
            if (verificandoLogin) return
            val codigo = campo.text.toString().trim()
            if (normalizarCodigo(codigo).length < 4) {
                erro.text = "Digite um código válido."
                erro.visibility = View.VISIBLE
                return
            }
            verificandoLogin = true
            erro.visibility = View.INVISIBLE
            botao.text = "Verificando…"
            botao.alpha = 0.6f
            botao.isEnabled = false
            (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
                ?.hideSoftInputFromWindow(campo.windowToken, 0)

            activityScope.launch {
                val resultado = withContext(Dispatchers.IO) { conferirCodigo(codigo) }
                verificandoLogin = false
                if (resultado == ResultadoCodigo.OK) {
                    prefs.edit()
                        .putString("codigo", codigo)
                        .putLong("ultima_ok", System.currentTimeMillis())
                        .apply()
                    fecharTelaLogin()
                    val jaIniciado = catalogoIniciado
                    liberarApp()
                    if (jaIniciado && filmes.isNotEmpty()) {
                        marcarMenu("Início")
                        mostrarInicio()
                    }
                } else {
                    botao.text = "→   Entrar"
                    botao.alpha = 1f
                    botao.isEnabled = true
                    erro.text = when (resultado) {
                        ResultadoCodigo.VENCIDO -> "Assinatura vencida. Fale com o suporte para renovar."
                        ResultadoCodigo.BLOQUEADO -> "Acesso bloqueado. Fale com o suporte."
                        ResultadoCodigo.INVALIDO -> "Código inválido. Confira e digite de novo."
                        else -> "Sem internet. Conecte a internet para validar seu acesso."
                    }
                    erro.visibility = View.VISIBLE
                    botao.requestFocus()
                }
            }
        }
        botao.setOnClickListener { entrar() }
        campo.setOnEditorActionListener { _, acao, _ ->
            if (acao == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { entrar(); true } else false
        }

        // Ícones das seções
        val linhaIcones = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val itens = listOf(
            "📺" to "Filmes", "🎬" to "Séries", "🌸" to "Doramas", "🍥" to "Animes", "📡" to "Canais ao vivo"
        )
        itens.forEachIndexed { i, (ic, nome) ->
            if (i > 0) {
                linhaIcones.addView(
                    View(this).apply { setBackgroundColor(Color.argb(90, 255, 255, 255)) },
                    LinearLayout.LayoutParams(dp(1), dp(46))
                )
            }
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(2), 0, dp(2), 0)
            }
            item.addView(TextView(this).apply { text = ic; textSize = 24f; gravity = Gravity.CENTER })
            item.addView(TextView(this).apply {
                text = nome
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 2
            })
            linhaIcones.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        coluna.addView(linhaIcones, LinearLayout.LayoutParams(larguraColuna, -2).apply { topMargin = dp(26) })

        // Rodapé
        val rodape = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        rodape.addView(View(this).apply { setBackgroundColor(vermelho) }, LinearLayout.LayoutParams(0, dp(2), 1f))
        rodape.addView(TextView(this).apply {
            text = "SEU ENTRETENIMENTO EM UM SÓ LUGAR"
            setTextColor(Color.rgb(170, 172, 185))
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
        }, LinearLayout.LayoutParams(-2, -2))
        rodape.addView(View(this).apply { setBackgroundColor(vermelho) }, LinearLayout.LayoutParams(0, dp(2), 1f))
        coluna.addView(rodape, LinearLayout.LayoutParams(larguraColuna, -2).apply { topMargin = dp(22) })

        val rolagem = ScrollView(this).apply {
            isFillViewport = true
            isFocusable = false
            isVerticalScrollBarEnabled = false
        }
        rolagem.addView(coluna, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL))
        tela.addView(rolagem, FrameLayout.LayoutParams(-1, -1))

        tela.alpha = 0f
        raiz.addView(tela, FrameLayout.LayoutParams(-1, -1))
        tela.animate().alpha(1f).setDuration(300).start()
        telaLogin = tela
        bloquearFocoAtras(true, tela)

        campo.requestFocus()
        if (!ehTv) {
            campo.post {
                (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
                    ?.showSoftInput(campo, 0)
            }
        }
    }

    private fun fecharTelaLogin() {
        val tela = telaLogin ?: return
        telaLogin = null
        campoLogin = null
        botaoLogin = null
        erroLogin = null
        bloquearFocoAtras(false, null)
        tela.animate().alpha(0f).setDuration(300).withEndAction { raiz.removeView(tela) }.start()
        focarConteudo()
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
                    "novelas" to baixarPrimeiro(ARQUIVOS_NOVELAS),
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
        gradesPendentes.clear()
        cardsAtuais.clear()
        linhasCards.clear()
        verTodosDaLinha.clear()
        bannerView = null
        btnAssistir = null
        btnInfoBanner = null
        campoBuscaCanais = null
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
            novelas.isEmpty() && animes.isEmpty() && desenhos.isEmpty() && desenhosSeries.isEmpty()
        ) {
            mostrarMensagemVazia("Carregando catálogo…")
            return
        }

        telaInicial = true

        if (filmes.isNotEmpty()) montarBannerDestaque(filmes.first())

        fun secao(titulo: String, icone: String, cards: () -> List<View>) {
            val cab = criarCabecalho(titulo, icone) { abrirSecao(titulo) }
            conteudo.addView(cab, LinearLayout.LayoutParams(-1, -2))
            montarFileira(cards(), cab)
        }

        if (filmes.isNotEmpty()) {
            secao("Filmes", "🔥") { filmes.take(colunas).map { cardFilmePaisagem(it) } }
        }
        if (series.isNotEmpty()) {
            secao("Séries", "🎬") { series.take(colunas).map { cardSeriePaisagem(it) } }
        }
        if (doramas.isNotEmpty()) {
            secao("Doramas", "🌸") { doramas.take(colunas).map { cardSeriePaisagem(it) } }
        }
        if (novelas.isNotEmpty()) {
            secao("Novelas", "💕") { novelas.take(colunas).map { cardSeriePaisagem(it) } }
        }
        if (animes.isNotEmpty()) {
            secao("Anime", "🍥") { animes.take(colunas).map { cardSeriePaisagem(it) } }
        }
        if (desenhos.isNotEmpty() || desenhosSeries.isNotEmpty()) {
            secao("Desenhos", "🧸") {
                if (desenhos.isNotEmpty()) desenhos.take(colunas).map { cardFilmePaisagem(it) }
                else desenhosSeries.take(colunas).map { cardSeriePaisagem(it) }
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

    // Liga cada fileira de cards ao seu botão "Ver todos" (para navegar com o controle)
    private val verTodosDaLinha = java.util.IdentityHashMap<List<View>, View>()

    private fun montarFileira(cards: List<View>, cabecalho: View? = null) {
        val linha = montarLinha(cards)
        conteudo.addView(linha, LinearLayout.LayoutParams(-1, dp(142)))
        cardsAtuais.addAll(cards)
        linhasCards.add(cards)
        (cabecalho?.tag as? View)?.let { verTodosDaLinha[cards] = it }
    }

    private var viewsContinuar: List<View> = emptyList()
    private var cardsContinuar: List<View> = emptyList()

    private fun removerContinuar() {
        viewsContinuar.forEach { conteudo.removeView(it) }
        val antigos = cardsContinuar
        cardsAtuais.removeAll(antigos.toSet())
        linhasCards.removeAll { it === antigos }
        verTodosDaLinha.remove(antigos)
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
        (cabecalho.tag as? View)?.let { verTodosDaLinha[cards] = it }

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

    private var scrollPrincipal: ScrollView? = null

    // Grade que vai adicionando linhas aos poucos (evita travar/fechar com milhares de itens)
    private class GradePendente(
        val container: LinearLayout,
        val restantes: ArrayDeque<List<View>>,
        var ultima: List<View>?
    )

    private val gradesPendentes = mutableListOf<GradePendente>()
    private val LINHAS_INICIAIS = 6
    private val LINHAS_POR_LOTE = 3

    private fun criarLinhaGrade(grupo: List<View>): View {
        val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

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
        return linha
    }

    private fun adicionarLinhasGrade(g: GradePendente, quantidade: Int) {
        var n = 0
        while (n < quantidade && g.restantes.isNotEmpty()) {
            val grupo = g.restantes.removeFirst()
            g.container.addView(
                criarLinhaGrade(grupo),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(275))
            )
            val ref = g.ultima
            val idx = if (ref == null) linhasCards.size
                else linhasCards.indexOfFirst { it === ref } + 1
            linhasCards.add(idx.coerceIn(0, linhasCards.size), grupo)
            g.ultima = grupo
            n++
        }
    }

    private fun carregarMaisLinhas() {
        val g = gradesPendentes.firstOrNull { it.restantes.isNotEmpty() } ?: return
        adicionarLinhasGrade(g, LINHAS_POR_LOTE)
        if (g.restantes.isEmpty()) gradesPendentes.remove(g)
    }

    private fun montarGrade(cards: List<View>, focar: Boolean = true) {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        conteudo.addView(
            container,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        val g = GradePendente(container, ArrayDeque(cards.chunked(colunas)), linhasCards.lastOrNull())
        adicionarLinhasGrade(g, LINHAS_INICIAIS)
        if (g.restantes.isNotEmpty()) gradesPendentes.add(g)

        if (ehTv && focar) cardsAtuais.firstOrNull()?.requestFocus()
    }

    // Mostra tudo de uma vez (sem botão "Mostrar mais")
    private fun <T> montarGradePaginada(itens: List<T>, inicio: Int = 0, criar: (T) -> View) {
        montarGrade(itens.subList(inicio, itens.size).map { criar(it) })
    }

    // ----- Coletâneas (sagas / franquias) -----

    private class Coletanea(
        val nome: String,
        val filmes: List<Filme>,
        val series: List<Serie>
    ) {
        val total get() = filmes.size + series.size
    }

    private fun chaveColetanea(titulo: String): String {
        var t = normalizarTexto(titulo)
        t = t.replace(Regex("\\(.*?\\)|\\[.*?]"), " ")
        t = t.split(":", " - ", " – ")[0]
        for (sep in listOf(" e o ", " e a ", " e os ", " e as ")) {
            val i = t.indexOf(sep)
            if (i > 0 && t.substring(0, i).trim().contains(" ")) {
                t = t.substring(0, i)
            }
        }
        t = t.replace(Regex("[^a-z0-9 ]"), " ")
        t = t.replace(Regex("\\s+(\\d+|ii|iii|iv|v|vi|vii|viii|ix|x)$"), "")
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    private fun montarColetaneas(listaFilmes: List<Filme>, listaSeries: List<Serie>): List<Coletanea> {
        val grupos = LinkedHashMap<String, Pair<MutableList<Filme>, MutableList<Serie>>>()
        val nomes = HashMap<String, String>()

        fun grupo(titulo: String, categoria: String): Pair<MutableList<Filme>, MutableList<Serie>>? {
            val chave = chaveColetanea(titulo)
            if (chave.length < 3) return null
            nomes.getOrPut(chave) { titulo.split(":", " - ", " – ")[0].replace(Regex("\\s*\\(.*?\\)"), "").trim() }
            return grupos.getOrPut(chave) { Pair(mutableListOf(), mutableListOf()) }
        }

        listaFilmes.forEach { grupo(it.titulo, it.categoria)?.first?.add(it) }
        listaSeries.forEach { grupo(it.titulo, it.categoria)?.second?.add(it) }

        return grupos.entries
            .filter { it.value.first.size + it.value.second.size >= 2 }
            .map { Coletanea(nomes[it.key] ?: it.key, it.value.first, it.value.second) }
            .sortedBy { normalizarTexto(it.nome) }
    }

    private fun cardColetanea(c: Coletanea): FrameLayout {
        val capa = c.filmes.firstOrNull()?.capa ?: c.series.firstOrNull()?.capa ?: ""
        val (card, _) = criarCardBase(capa, rodapeDuasLinhas(c.nome, "Coletânea • ${c.total} títulos"))
        card.setOnClickListener { mostrarColetanea(c) }
        return card
    }

    private fun mostrarColetanea(c: Coletanea) {
        val voltarPara = voltarListaAtual
        historicoConteudo.add { voltarPara() }
        limparConteudo()

        conteudo.addView(
            tituloSecao("${c.nome}  (${c.total})", "📚"),
            LinearLayout.LayoutParams(-1, -2)
        )
        montarGrade(c.filmes.map { criarCard(it) } + c.series.map { criarCardSerie(it) })
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

    // ----- Categorias dentro de Filmes / Séries / Doramas / Anime / Desenhos -----

    private fun categoriasDoItem(cat: String): List<String> {
        val partes = cat.split(",", "/", ";", "|").map { it.trim() }.filter { it.isNotBlank() }
        return if (partes.isEmpty()) listOf("Outros") else partes
    }

    private val PAISES_NOVELA = setOf(
        "novelas brasileiras", "novelas mexicanas", "novelas turcas", "novelas coreanas",
        "novelas colombianas", "novelas argentinas", "novelas indianas"
    )

    // Novela nova: se a categoria não diz o país, descobre pelo título/sinopse
    private fun organizarNovela(sr: Serie): Serie {
        val atuais = categoriasNovela(sr.categoria).map { normalizarTexto(it) }
        if (atuais.any { it in PAISES_NOVELA }) return sr

        val t = normalizarTexto(sr.titulo + " " + sr.sinopse + " " + sr.categoria)
        val palavras = linkedMapOf(
            "Novelas turcas" to arrayOf("turc", "turqu", "turk", "istambul", "istanbul", "ankara", "otoman",
                "bosforo", "capadocia", "izmir", "bodrum", "anatolia", "dizi"),
            "Novelas mexicanas" to arrayOf("mexic", "televisa", "azteca", "guadalajara", "monterrey",
                "acapulco", "cancun", "tijuana", "puebla", "las estrellas"),
            "Novelas brasileiras" to arrayOf("brasil", "rede globo", "globo", "sbt", "record", "rio de janeiro",
                "sao paulo", "salvador", "recife", "fortaleza", "belo horizonte", "pantanal",
                "minas gerais", "bahia", "nordeste", "sertao", "favela", "curitiba"),
            "Novelas coreanas" to arrayOf("corean", "korea", "seul"),
            "Novelas colombianas" to arrayOf("colomb", "bogota", "medellin"),
            "Novelas argentinas" to arrayOf("argentin", "buenos aires")
        )
        var melhor: String? = null
        var melhorPontos = 0
        palavras.forEach { (pais, chaves) ->
            val pontos = chaves.count { temPalavra(t, it) }
            if (pontos > melhorPontos) { melhorPontos = pontos; melhor = pais }
        }
        return melhor?.let { sr.copy(categoria = it) } ?: sr
    }

    // Separa novelas por país de origem (Brasileiras, Mexicanas, Turcas...)
    private fun categoriasNovela(cat: String): List<String> {
        val t = normalizarTexto(cat)
        val r = mutableListOf<String>()
        if (t.contains("brasil") || t.contains("nacional") || t.contains("globo")) r.add("Novelas brasileiras")
        if (t.contains("mexic") || t.contains("televisa")) r.add("Novelas mexicanas")
        if (t.contains("turc") || t.contains("turqu")) r.add("Novelas turcas")
        if (t.contains("corean") || t.contains("korea")) r.add("Novelas coreanas")
        if (t.contains("colomb")) r.add("Novelas colombianas")
        if (t.contains("argentin")) r.add("Novelas argentinas")
        if (t.contains("indian") || t.contains("india")) r.add("Novelas indianas")
        if (r.isNotEmpty()) return r
        val partes = cat.split(",", "/", ";", "|").map { it.trim() }.filter { it.isNotBlank() }
        return if (partes.isEmpty()) listOf("Outras") else partes
    }

    private fun mostrarCategoriasColecao(
        titulo: String,
        icone: String,
        listaFilmes: List<Filme>,
        listaSeries: List<Serie>,
        vazio: String,
        voltar: () -> Unit,
        extrator: (String) -> List<String> = { categoriasDoItem(it) },
        ordem: List<String> = emptyList()
    ) {
        historicoConteudo.clear()
        voltarListaAtual = voltar
        limparConteudo()

        if (listaFilmes.isEmpty() && listaSeries.isEmpty()) {
            mostrarMensagemVazia(vazio)
            return
        }

        val total = listaFilmes.size + listaSeries.size
        conteudo.addView(tituloSecao("$titulo  ($total)", icone), LinearLayout.LayoutParams(-1, -2))

        val botao = criarBotaoBuscaColecao(titulo, icone, listaFilmes, listaSeries)
        campoBuscaCanais = botao
        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                leftMargin = dp(6); rightMargin = dp(6); bottomMargin = dp(12)
            }
        )

        val nomes = LinkedHashMap<String, String>()
        val filmesPor = HashMap<String, MutableList<Filme>>()
        val seriesPor = HashMap<String, MutableList<Serie>>()
        listaFilmes.forEach { f ->
            extrator(f.categoria).forEach { c ->
                val k = normalizarTexto(c)
                nomes.getOrPut(k) { c }
                filmesPor.getOrPut(k) { mutableListOf() }.add(f)
            }
        }
        listaSeries.forEach { sr ->
            extrator(sr.categoria).forEach { c ->
                val k = normalizarTexto(c)
                nomes.getOrPut(k) { c }
                seriesPor.getOrPut(k) { mutableListOf() }.add(sr)
            }
        }

        val voltarCategorias = { mostrarCategoriasColecao(titulo, icone, listaFilmes, listaSeries, vazio, voltar, extrator, ordem) }
        val cards = mutableListOf<View>()

        val capaTodos = listaFilmes.firstOrNull()?.capa ?: listaSeries.firstOrNull()?.capa ?: ""
        cards.add(
            criarCardCategoria("Todos", capaTodos, total) {
                abrirItensCategoria(titulo, icone, "Todos", listaFilmes, listaSeries, voltarCategorias)
            }
        )

        nomes.keys
            .sortedWith(
                compareBy(
                    { it == "outros" || it == "outras" },
                    { val i = ordem.indexOf(it); if (i < 0) 99 else i },
                    { it }
                )
            )
            .forEach { k ->
                val f = filmesPor[k].orEmpty()
                val sr = seriesPor[k].orEmpty()
                val nome = nomes[k] ?: k
                val capa = f.firstOrNull()?.capa ?: sr.firstOrNull()?.capa ?: ""
                cards.add(
                    criarCardCategoria(nome, capa, f.size + sr.size) {
                        abrirItensCategoria(titulo, icone, nome, f, sr, voltarCategorias)
                    }
                )
            }
        montarGrade(cards, focar = ehTv)
    }

    private fun criarCardCategoria(nome: String, capa: String, qtd: Int, aoClicar: () -> Unit): View {
        val (card, imagem) = criarCardBase(capa, rodapeDuasLinhas(nome, if (qtd == 1) "1 título" else "$qtd títulos"))
        imagem.setBackgroundColor(Color.rgb(12, 16, 34))
        card.setOnClickListener { aoClicar() }
        return card
    }

    private fun abrirItensCategoria(
        titulo: String, icone: String, nomeCat: String,
        f: List<Filme>, sr: List<Serie>, voltarCategorias: () -> Unit
    ) {
        historicoConteudo.add { voltarCategorias() }
        mostrarItensCategoria(titulo, icone, nomeCat, f, sr)
    }

    private fun mostrarItensCategoria(
        titulo: String, icone: String, nomeCat: String, f: List<Filme>, sr: List<Serie>
    ) {
        voltarListaAtual = { mostrarItensCategoria(titulo, icone, nomeCat, f, sr) }
        limparConteudo()
        val dois = f.isNotEmpty() && sr.isNotEmpty()
        val rotulo = if (nomeCat == "Todos") titulo else "$titulo • $nomeCat"

        if (f.isNotEmpty()) {
            conteudo.addView(
                tituloSecao(if (dois) "$rotulo • Filmes  (${f.size})" else "$rotulo  (${f.size})", icone),
                LinearLayout.LayoutParams(-1, -2)
            )
            montarGrade(f.map { criarCard(it) })
        }
        if (sr.isNotEmpty()) {
            conteudo.addView(
                tituloSecao(if (dois) "$rotulo • Séries  (${sr.size})" else "$rotulo  (${sr.size})", icone),
                LinearLayout.LayoutParams(-1, -2).apply { if (dois) topMargin = dp(10) }
            )
            montarGrade(sr.map { criarCardSerie(it) })
        }
    }

    private fun criarBotaoBuscaColecao(titulo: String, icone: String, f: List<Filme>, sr: List<Serie>): View {
        val b = TextView(this).apply {
            text = "🔍   Buscar em $titulo..."
            setTextColor(Color.rgb(190, 195, 215))
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }
        fun estilo(foco: Boolean) {
            b.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.rgb(18, 22, 44))
                setStroke(dp(if (foco) 3 else 1), if (foco) Color.WHITE else Color.rgb(60, 66, 100))
            }
        }
        estilo(false)
        b.setOnFocusChangeListener { _, fo -> estilo(fo) }
        b.setOnClickListener {
            val campo = EditText(this).apply {
                hint = "Nome..."
                textSize = 18f
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT
            }
            AlertDialog.Builder(this)
                .setTitle("Buscar em $titulo")
                .setView(campo)
                .setPositiveButton("Buscar") { _, _ ->
                    val consulta = campo.text.toString()
                    if (normalizarTexto(consulta).isNotBlank()) {
                        val voltarPara = voltarListaAtual
                        historicoConteudo.add { voltarPara() }
                        mostrarResultadosColecao(consulta, titulo, icone, f, sr)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        return b
    }

    private fun mostrarResultadosColecao(
        consulta: String, titulo: String, icone: String, f: List<Filme>, sr: List<Serie>
    ) {
        voltarListaAtual = { mostrarResultadosColecao(consulta, titulo, icone, f, sr) }
        limparConteudo()
        val palavras = normalizarTexto(consulta).split(" ").filter { it.isNotBlank() }
        fun casa(t: String): Boolean {
            val n = normalizarTexto(t)
            return palavras.all { n.contains(it) }
        }
        val af = f.filter { casa(it.titulo) }
        val asr = sr.filter { casa(it.titulo) }
        if (af.isEmpty() && asr.isEmpty()) {
            conteudo.addView(
                tituloSecao("Resultados para \"$consulta\"", "🔍"),
                LinearLayout.LayoutParams(-1, -2)
            )
            mostrarMensagemVazia("Nada encontrado")
            return
        }
        val dois = af.isNotEmpty() && asr.isNotEmpty()
        if (af.isNotEmpty()) {
            conteudo.addView(
                tituloSecao("Resultados • ${if (dois) "Filmes " else ""}(${af.size})", "🔍"),
                LinearLayout.LayoutParams(-1, -2)
            )
            montarGrade(af.map { criarCard(it) })
        }
        if (asr.isNotEmpty()) {
            conteudo.addView(
                tituloSecao("Resultados • ${if (dois) "Séries " else ""}(${asr.size})", "🔍"),
                LinearLayout.LayoutParams(-1, -2).apply { if (dois) topMargin = dp(10) }
            )
            montarGrade(asr.map { criarCardSerie(it) })
        }
    }

    private fun mostrarAbaColetaneas() {
        historicoConteudo.clear()
        voltarListaAtual = { abrirSecao("Coletâneas") }
        limparConteudo()

        val coletaneas = montarColetaneas(filmes, emptyList())
        if (coletaneas.isEmpty()) {
            mostrarMensagemVazia("Nenhuma coletânea encontrada")
            return
        }

        coletaneas.forEach { c ->
            conteudo.addView(
                tituloSecao("${c.nome}  (${c.total})", "📚"),
                LinearLayout.LayoutParams(-1, -2)
            )
            montarGrade(c.filmes.map { criarCard(it) })
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

    // ----- Canais ao vivo (agrupados por nome + busca) -----

    private var canaisCarregados = false
    private var canaisTentados = false
    private var campoBuscaCanais: View? = null
    private var jobBuscaCanais: Job? = null

    private suspend fun baixarCanais(): List<Canal> = withContext(Dispatchers.IO) {
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

    private fun carregarCanais() {
        if (canaisCarregados && canais.isNotEmpty()) {
            mostrarListaCanais()
            return
        }
        limparConteudo()
        mostrarMensagemVazia("Carregando canais ao vivo...")
        activityScope.launch {
            val resultado = baixarCanais()
            canaisTentados = true
            canais.clear()
            canais.addAll(resultado)
            canaisCarregados = resultado.isNotEmpty()
            if (menuAtual == "Canais ao vivo") mostrarListaCanais()
        }
    }

    // Ordena "HBO 2" antes de "HBO 10"
    private fun chaveOrdemCanal(nome: String): String =
        normalizarTexto(nome).replace(Regex("\\d+")) { it.value.padStart(5, '0') }

    private fun canalCasaBusca(canal: Canal, termo: String): Boolean {
        val alvo = normalizarTexto(canal.nome) + " " + normalizarTexto(canal.categoria)
        return termo.split(" ").filter { it.isNotBlank() }.all { alvo.contains(it) }
    }

    // ----- Categorias de canais -----

    private val ICONES_CATEGORIAS_CANAIS = linkedMapOf(
        "Novelas mexicanas" to "🇲🇽",
        "Novelas brasileiras" to "🇧🇷",
        "Novelas turcas" to "🇹🇷",
        "Filmes" to "🎬",
        "Séries" to "📺",
        "Doramas" to "🌸",
        "Novelas" to "💕",
        "Animes" to "🍥",
        "Desenhos" to "🧸",
        "Esportes" to "⚽",
        "Notícias" to "📰",
        "Abertos" to "📡",
        "Documentários" to "🌎",
        "Música" to "🎵",
        "Outros" to "📁"
    )

    private fun temPalavra(texto: String, vararg chaves: String): Boolean =
        Regex("\\b(" + chaves.joinToString("|") { Regex.escape(it) } + ")").containsMatchIn(texto)

    // Usa o campo "categoria" do canais.json e também o nome do canal
    private fun categoriaDoCanal(c: Canal): String {
        val t = normalizarTexto(c.categoria + " " + c.nome)
        return when {
            temPalavra(t, "dorama", "kdrama", "k-drama", "asian") -> "Doramas"
            temPalavra(t, "novela", "telenovela", "novelas", "dizi", "turk", "televisa", "las estrellas") -> {
                when {
                    temPalavra(t, "mexic", "televisa", "las estrellas", "azteca") -> "Novelas mexicanas"
                    temPalavra(t, "brasil", "globo", "record", "sbt") -> "Novelas brasileiras"
                    temPalavra(t, "turc", "turk", "turqu", "dizi") -> "Novelas turcas"
                    else -> "Novelas"
                }
            }
            temPalavra(t, "anime", "crunchyroll", "animax") -> "Animes"
            temPalavra(t, "desenho", "cartoon", "infantil", "kids", "disney", "nick", "gloob",
                "boomerang", "tooncast", "toon", "baby", "discovery k") -> "Desenhos"
            temPalavra(t, "esporte", "sport", "espn", "combate", "premiere", "nba", "ufc",
                "futebol", "dazn", "fox sports", "bandsports", "sportv", "tnt sports") -> "Esportes"
            temPalavra(t, "noticia", "news", "cnn", "globonews", "jovem pan", "bandnews") -> "Notícias"
            temPalavra(t, "serie", "series", "sitcom", "sony", "lifetime", "comedy central",
                "warner", "fx", "amc", "universal tv", "tnt series", "space") -> "Séries"
            temPalavra(t, "filme", "movie", "cine", "telecine", "hbo", "megapix", "paramount",
                "studio universal", "tnt", "axn", "max", "tcm", "film") -> "Filmes"
            temPalavra(t, "document", "discovery", "history", "nat geo", "national geographic",
                "animal planet", "a&e", "tlc", "curta") -> "Documentários"
            temPalavra(t, "musica", "music", "mtv", "multishow", "vh1", "radio", "play tv") -> "Música"
            temPalavra(t, "globo", "sbt", "record", "band", "redetv", "rede tv", "tv brasil",
                "cultura", "gazeta", "aberto") -> "Abertos"
            else -> "Outros"
        }
    }

    private fun criarCardCategoriaCanais(nome: String, icone: String, lista: List<Canal>, aoClicar: () -> Unit): View {
        val (card, imagem) = criarCardBase("", rodapeDuasLinhas(nome, "${lista.size} canais"))
        imagem.setImageDrawable(null)
        imagem.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(30, 36, 80), Color.rgb(12, 16, 34))
        )
        card.addView(
            TextView(this).apply {
                text = icone
                textSize = 52f
                gravity = Gravity.CENTER
            },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(210))
        )
        // garante que a borda de foco continue por cima
        card.setOnClickListener { aoClicar() }
        return card
    }

    private fun criarBotaoBuscaCanais(titulo: String, lista: List<Canal>): View {
        val b = TextView(this).apply {
            text = "🔍   Buscar canal pelo nome..."
            setTextColor(Color.rgb(190, 195, 215))
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }
        fun estilo(foco: Boolean) {
            b.background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.rgb(18, 22, 44))
                setStroke(dp(if (foco) 3 else 1), if (foco) Color.WHITE else Color.rgb(60, 66, 100))
            }
        }
        estilo(false)
        b.setOnFocusChangeListener { _, f -> estilo(f) }
        b.setOnClickListener { abrirBuscaCanais(titulo, lista) }
        return b
    }

    private fun abrirBuscaCanais(titulo: String, lista: List<Canal>) {
        val campo = EditText(this).apply {
            hint = "Nome do canal..."
            textSize = 18f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(this)
            .setTitle("Buscar em $titulo")
            .setView(campo)
            .setPositiveButton("Buscar") { _, _ ->
                mostrarResultadosCanais(campo.text.toString(), lista)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarResultadosCanais(consulta: String, lista: List<Canal>) {
        val termo = normalizarTexto(consulta)
        if (termo.isBlank()) return
        val voltarPara = voltarListaAtual
        historicoConteudo.add { voltarPara() }
        limparConteudo()
        val achados = lista.filter { canalCasaBusca(it, termo) }.sortedBy { chaveOrdemCanal(it.nome) }
        conteudo.addView(
            tituloSecao("Resultados para \"$consulta\"  (${achados.size})", "🔍"),
            LinearLayout.LayoutParams(-1, -2)
        )
        if (achados.isEmpty()) {
            mostrarMensagemVazia("Nenhum canal encontrado")
            return
        }
        montarGrade(achados.map { criarCardCanal(it) })
    }

    private fun mostrarListaCanais() {
        historicoConteudo.clear()
        voltarListaAtual = { mostrarListaCanais() }
        limparConteudo()
        if (canais.isEmpty()) {
            mostrarMensagemVazia("Nenhum canal encontrado em canais.json.")
            return
        }
        conteudo.addView(tituloSecao("CANAIS AO VIVO  •  ${canais.size}", "📡"))

        val botao = criarBotaoBuscaCanais("todos os canais", canais)
        campoBuscaCanais = botao
        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                leftMargin = dp(6); rightMargin = dp(6); bottomMargin = dp(12)
            }
        )

        val porCategoria = canais.groupBy { categoriaDoCanal(it) }
        val cards = mutableListOf<View>()
        cards.add(
            criarCardCategoriaCanais("Todos os canais", "📡", canais) { abrirCategoriaCanais("Todos os canais") }
        )
        ICONES_CATEGORIAS_CANAIS.forEach { (nome, icone) ->
            val lista = porCategoria[nome] ?: return@forEach
            cards.add(criarCardCategoriaCanais(nome, icone, lista) { abrirCategoriaCanais(nome) })
        }
        montarGrade(cards, focar = ehTv)
    }

    private fun abrirCategoriaCanais(nome: String) {
        historicoConteudo.add { mostrarListaCanais() }
        mostrarCategoriaCanais(nome)
    }

    private fun mostrarCategoriaCanais(nome: String) {
        voltarListaAtual = { mostrarCategoriaCanais(nome) }
        limparConteudo()
        val lista = (if (nome == "Todos os canais") canais else canais.filter { categoriaDoCanal(it) == nome })
            .sortedBy { chaveOrdemCanal(it.nome) }
        val icone = ICONES_CATEGORIAS_CANAIS[nome] ?: "📡"
        conteudo.addView(tituloSecao("$nome  (${lista.size})", icone), LinearLayout.LayoutParams(-1, -2))
        if (lista.isEmpty()) {
            mostrarMensagemVazia("Nenhum canal nesta categoria")
            return
        }
        val botao = criarBotaoBuscaCanais(nome, lista)
        campoBuscaCanais = botao
        conteudo.addView(
            botao,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                leftMargin = dp(6); rightMargin = dp(6); bottomMargin = dp(12)
            }
        )
        montarGrade(lista.map { criarCardCanal(it) }, focar = ehTv)
    }

    private fun logoDoCanal(logo: String): String {
        if (logo.isBlank()) return ""
        val dominio = Regex("^https?://logo\\.clearbit\\.com/([^/?#]+)").find(logo)?.groupValues?.get(1)
        return if (dominio != null) "https://www.google.com/s2/favicons?sz=256&domain=$dominio" else logo
    }

    private fun criarCardCanal(canal: Canal): View {
        val (card, imagem) = criarCardBase(canal.logo, rodapeDuasLinhas(canal.nome, categoriaDoCanal(canal)))
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
            hint = "Nome do filme, série ou canal..."
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
        if (!canaisCarregados && !canaisTentados) {
            activityScope.launch {
                val r = baixarCanais()
                canaisTentados = true
                if (r.isNotEmpty()) {
                    canais.clear()
                    canais.addAll(r)
                    canaisCarregados = true
                }
                pesquisarAgora(consulta)
            }
            return
        }
        pesquisarAgora(consulta)
    }

    private fun pesquisarAgora(consulta: String) {
        val termo = normalizarTexto(consulta)
        val achadosFilmes = (filmes + desenhos).filter { normalizarTexto(it.titulo).contains(termo) }
        val achadasSeries = (series + doramas + novelas + animes + desenhosSeries)
            .filter { normalizarTexto(it.titulo).contains(termo) }
        val achadosCanais = canais.filter { canalCasaBusca(it, termo) }
            .sortedBy { chaveOrdemCanal(it.nome) }

        if (achadosCanais.isEmpty()) {
            mostrarColecao(
                "Resultados", "🔍", achadosFilmes, achadasSeries,
                "Nada encontrado para \"$consulta\""
            ) { pesquisar(consulta) }
            return
        }

        if (achadosFilmes.isNotEmpty() || achadasSeries.isNotEmpty()) {
            mostrarColecao(
                "Resultados", "🔍", achadosFilmes, achadasSeries, ""
            ) { pesquisar(consulta) }
        } else {
            historicoConteudo.clear()
            voltarListaAtual = { pesquisar(consulta) }
            limparConteudo()
        }
        conteudo.addView(
            tituloSecao("Resultados • Canais ao vivo  (${achadosCanais.size})", "📡"),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) }
        )
        montarGrade(achadosCanais.map { criarCardCanal(it) })
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
        // Tela de login aberta: voltar sai do app, o resto segue o comportamento normal dos campos
        if (telaLogin != null) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                finish()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            val foco = currentFocus
            val tecla = event.keyCode
            val linhaIdx = if (foco == null) -1 else linhasCards.indexOfFirst { l -> l.any { it === foco } }

            // Foco no botão "Ver todos" de uma fileira
            val linhaVerTodos = if (foco == null) -1
                else linhasCards.indexOfFirst { l -> verTodosDaLinha[l] === foco }
            if (linhaVerTodos >= 0 &&
                (tecla == KeyEvent.KEYCODE_DPAD_LEFT || tecla == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    tecla == KeyEvent.KEYCODE_DPAD_UP || tecla == KeyEvent.KEYCODE_DPAD_DOWN)
            ) {
                when (tecla) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> linhasCards[linhaVerTodos].firstOrNull()?.requestFocus()
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (linhaVerTodos > 0) {
                            val acima = linhasCards[linhaVerTodos - 1]
                            (acima.lastOrNull() ?: acima.firstOrNull())?.requestFocus()
                        } else {
                            (btnAssistir ?: campoBuscaCanais ?: campoPesquisaTopo).requestFocus()
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> itensMenu[menuAtual]?.requestFocus()
                    else -> {}
                }
                return true
            }

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
                        val verTodos = verTodosDaLinha[linha]
                        if (verTodos != null) {
                            verTodos.requestFocus()
                        } else if (linhaIdx > 0) {
                            val acima = linhasCards[linhaIdx - 1]
                            acima[minOf(col, acima.lastIndex)].requestFocus()
                        } else {
                            (btnAssistir ?: campoBuscaCanais ?: campoPesquisaTopo).requestFocus()
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (linhaIdx >= linhasCards.lastIndex - 2) carregarMaisLinhas()
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
                    (foco === btnAssistir || foco === btnInfoBanner || foco === campoPesquisaTopo || foco === campoBuscaCanais)
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
