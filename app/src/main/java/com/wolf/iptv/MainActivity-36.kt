
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
    private val desenhosSeries = mutableListOf<Serie>()
    private val canais = mutableListOf<Canal>()

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
                if (!noPrimeiroPlano) continue   // filme aberto: não baixa nada
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

    // ===== FUNDO (fundo.png) E LOGO DO TOPO =====

    // Procura o fundo.png dentro do app: res/drawable, res/raw, res/mipmap ou assets.
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

    // Decodifica já reduzida para o tamanho da tela (não estoura a memória).
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
                } else {
                    // Não achou o fundo.png: fica preto (a imagem antiga foi removida).
                    Toast.makeText(
                        this,
                        "fundo.png não encontrado no app (coloque em res/drawable ou assets)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // O logo é um JPEG com fundo preto. Aqui o preto vira transparente (brilho vira
    // transparência), assim o lobo e o "WOLF IPTV" ficam flutuando sobre o fundo do app.
    private fun criarLogoCabecalho(): Bitmap? {
        return try {
            val bytes = android.util.Base64.decode(
                LOGO_PARTES.joinToString(""),
                android.util.Base64.DEFAULT
            )
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null

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

    private fun criarInterface() {
        raiz = FrameLayout(this)
        raiz.layoutDirection = View.LAYOUT_DIRECTION_LTR
        raiz.setBackgroundColor(Color.BLACK)

        val fundo = ImageView(this)
        fundo.scaleType = ImageView.ScaleType.CENTER_CROP
        fundo.alpha = 1f

        // Lê o fundo.png que está dentro do app (se não achar, usa a imagem antiga da internet).
        carregarFundoApp(fundo)

        raiz.addView(
            fundo,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Degradê: o lobo aparece em cima e o fundo vai ficando azul-marinho escuro embaixo.
        val degrade = View(this)
        degrade.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(0, 5, 7, 18),
                Color.argb(70, 5, 7, 18),
                Color.argb(190, 4, 6, 16)
            )
        )
        raiz.addView(
            degrade,
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

        val barraTopo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        // Celular usa o topo menor; TV e tablet usam o tamanho grande.
        val compacto = resources.configuration.screenWidthDp < 600
        botaoMenu.textSize = if (compacto) 15f else 20f

        // Coluna da esquerda: logo WOLF IPTV em cima e o botão do menu embaixo dele.
        val colunaEsquerda = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }

        val logoTopo = criarLogoCabecalho()
        val tamanhoLogo = if (compacto) dp(76) else dp(104)

        if (logoTopo != null) {
            colunaEsquerda.addView(
                ImageView(this).apply {
                    setImageBitmap(logoTopo)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(tamanhoLogo, tamanhoLogo)
            )
        } else {
            colunaEsquerda.addView(
                TextView(this).apply {
                    text = "WOLF IPTV"
                    setTextColor(Color.rgb(230, 25, 35))
                    textSize = if (compacto) 20f else 30f
                    setTypeface(null, Typeface.BOLD_ITALIC)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        colunaEsquerda.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                if (compacto) dp(150) else dp(235),
                if (compacto) dp(46) else dp(58)
            ).apply { topMargin = dp(6) }
        )

        barraTopo.addView(
            colunaEsquerda,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        barraTopo.addView(
            View(this),
            LinearLayout.LayoutParams(0, 1, 1f)
        )

        barraTopo.addView(
            criarRelogio(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        )
        camada.addView(
            barraTopo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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

    // ===== ESTILO WOLF (visual da tela inicial) =====

    // Seta "›" desenhada à direita dos itens do menu.
    private fun criarSeta(): android.graphics.drawable.Drawable {
        val tamanho = dp(22)
        return object : android.graphics.drawable.Drawable() {
            private val pincel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = dp(26).toFloat()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            override fun draw(canvas: android.graphics.Canvas) {
                val b = bounds
                canvas.drawText("›", b.exactCenterX(), b.centerY() + dp(9).toFloat(), pincel)
            }

            override fun setAlpha(alpha: Int) { pincel.alpha = alpha }
            override fun setColorFilter(filtro: android.graphics.ColorFilter?) { pincel.colorFilter = filtro }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
            override fun getIntrinsicWidth(): Int = tamanho
            override fun getIntrinsicHeight(): Int = tamanho
        }
    }

    // Título de seção com a barrinha vermelha na frente ("| FILMES EM DESTAQUE").
    private fun tituloSecao(texto: String, cinza: Boolean = false): View {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        linha.addView(
            View(this).apply { setBackgroundColor(Color.rgb(255, 40, 40)) },
            LinearLayout.LayoutParams(dp(4), dp(22)).apply { rightMargin = dp(10) }
        )

        linha.addView(
            TextView(this).apply {
                this.text = texto
                setTextColor(if (cinza) Color.rgb(170, 170, 170) else Color.WHITE)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                letterSpacing = 0.04f
            }
        )

        return linha
    }

    private lateinit var relogioHora: TextView
    private lateinit var relogioData: TextView

    private fun criarRelogio(): View {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(dp(14), 0, dp(6), 0)
        }

        relogioHora = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }

        relogioData = TextView(this).apply {
            setTextColor(Color.rgb(200, 200, 210))
            textSize = 13f
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
                    .replaceFirstChar { it.uppercase() }
                delay(30_000)
            }
        }

        return caixa
    }

    private fun criarFundoCard(foco: Boolean): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.setColor(
            if (foco) Color.argb(245, 10, 14, 32)
            else Color.argb(225, 8, 12, 28)
        )
        fundo.cornerRadius = dp(14).toFloat()
        if (foco) fundo.setStroke(dp(3), Color.rgb(255, 40, 40))
        else fundo.setStroke(dp(1), Color.rgb(44, 62, 120))
        return fundo
    }

    private fun criarBordaVermelha(): GradientDrawable {
        val borda = GradientDrawable()
        borda.setColor(Color.TRANSPARENT)
        borda.cornerRadius = dp(14).toFloat()
        borda.setStroke(dp(3), Color.rgb(255, 40, 40))
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

                    mostrarListaCards(filmes, true)
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
                        // Abriu na hora com o que estava salvo; confere novidades em segundo plano.
                        verificarNovidades()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Catálogo carregado: ${filmes.size} filmes, " +
                                "${series.size} séries, ${doramas.size} doramas, " +
                                "${animes.size} animes e ${desenhos.size + desenhosSeries.size} desenhos",
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
                    desenhosSeries.clear()
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

            if (ate.isNotBlank()) {
                if (ate.length > 10) {
                    // Teste com hora: "2026-10-14T04:30" (horário do aparelho)
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
        telaInicial = false
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
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(3), dp(8), dp(3))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(60, 6, 9, 22), Color.argb(245, 6, 9, 22))
            )
        }

        val titulo = TextView(this).apply {
            text = linha1
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
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
            setTextColor(Color.rgb(190, 195, 210))
            textSize = 12f
            gravity = Gravity.CENTER
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

    // true enquanto a tela inicial (com a fileira "Continuar assistindo") está aberta.
    private var telaInicial = false

    private fun mostrarListaCards(lista: List<Filme>, comContinuar: Boolean = false) {
        limparConteudo()
        telaInicial = comContinuar

        if (lista.isEmpty()) {
            mostrarMensagemVazia("Nenhum filme encontrado")
            return
        }

        if (comContinuar) {
            montarFileiraContinuar()
            // Fileira "Lançamentos recentes" removida; fica só o título da grade.
            conteudo.addView(
                tituloSecao("TODOS OS FILMES", cinza = true),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        montarGrade(lista.map { criarCard(it) })
    }

    // Primeira fileira: o que o cliente começou a assistir (filmes, séries, doramas, animes).
    private fun montarFileiraContinuar() {
        val itens = try {
            WolfProgress.emAndamento(this).take(colunas)
        } catch (_: Throwable) {
            emptyList()
        }
        if (itens.isEmpty()) return

        conteudo.addView(
            tituloSecao("CONTINUAR ASSISTINDO"),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun parametros() = LinearLayout.LayoutParams(0, dp(335), 1f).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
            bottomMargin = dp(12)
        }

        itens.forEach {
            val card = criarCard(
                Filme(it.titulo, 0, it.resumo(), it.capa, it.url),
                false
            )
            adicionarBarraProgresso(card, it)
            linha.addView(card, parametros())
            cardsAtuais.add(card)
        }

        // Espaços vazios entram na contagem para a navegação da grade não desalinhar.
        repeat(colunas - itens.size) {
            val vazio = View(this).apply {
                tag = "vazio"
                isFocusable = false
            }
            linha.addView(vazio, parametros())
            cardsAtuais.add(vazio)
        }

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(350)
            )
        )

    }

    // Fileira com os filmes mais novos (maior ano primeiro).
    private fun montarFileiraLancamentos(lista: List<Filme>): Boolean {
        val novos = lista
            .filter { it.ano > 0 }
            .sortedByDescending { it.ano }
            .take(colunas)
        if (novos.isEmpty()) return false

        conteudo.addView(
            tituloSecao("LANÇAMENTOS RECENTES"),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun parametros() = LinearLayout.LayoutParams(0, dp(335), 1f).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
            bottomMargin = dp(12)
        }

        novos.forEach {
            val card = criarCard(it)
            linha.addView(card, parametros())
            cardsAtuais.add(card)
        }

        // Espaços vazios mantêm a navegação da grade alinhada.
        repeat(colunas - novos.size) {
            val vazio = View(this).apply {
                tag = "vazio"
                isFocusable = false
            }
            linha.addView(vazio, parametros())
            cardsAtuais.add(vazio)
        }

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(350)
            )
        )

        conteudo.addView(
            tituloSecao("TODOS OS FILMES", cinza = true),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return true
    }

    // Barra vermelha de progresso com a porcentagem, em cima do rodapé do card.
    private fun adicionarBarraProgresso(card: View, item: ProgressoItem) {
        val moldura = card as? FrameLayout ?: return
        if (item.duracao <= 0L) return

        val pct = ((item.posicao * 100) / item.duracao).toInt().coerceIn(0, 100)

        val trilho = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 100f
            background = GradientDrawable().apply {
                setColor(Color.argb(170, 60, 66, 90))
                cornerRadius = dp(2).toFloat()
            }
        }

        trilho.addView(
            View(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.rgb(255, 40, 40))
                    cornerRadius = dp(2).toFloat()
                }
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, pct.toFloat())
        )

        moldura.addView(
            trilho,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(4)
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(dp(10), 0, dp(10), dp(66))
            }
        )

        moldura.addView(
            TextView(this).apply {
                text = "$pct%"
                setTextColor(Color.WHITE)
                textSize = 11f
                setShadowLayer(4f, 0f, 0f, Color.BLACK)
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(10), dp(72))
            }
        )
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

    // ===== CANAIS AO VIVO (canais.json) =====

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
                            obj.optJSONArray("canais")
                                ?: obj.optJSONArray("channels")
                                ?: obj.optJSONArray("items")
                                ?: JSONArray()
                        }
                        val lista = mutableListOf<Canal>()
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val nome = item.optString("nome",
                                item.optString("name",
                                    item.optString("titulo",
                                        item.optString("title", "")))).trim()
                            val url = item.optString("url",
                                item.optString("link",
                                    item.optString("stream",
                                        item.optString("video", "")))).trim()
                            val logo = logoDoCanal(item.optString("logo",
                                item.optString("capa",
                                    item.optString("tvg-logo", ""))).trim())
                            val categoria = item.optString("categoria",
                                item.optString("group-title", "Canais ao vivo"))
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
            mostrarMensagemVazia("Não foi possível carregar canais.json. Confira se o arquivo existe no GitHub.")
            return
        }

        conteudo.addView(TextView(this).apply {
            text = "📡  CANAIS AO VIVO  •  ${canais.size} canais"
            setTextColor(Color.WHITE)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(14))
        })

        montarGrade(canais.map { criarCardCanal(it) })
    }

    // O logo.clearbit.com saiu do ar em dez/2025. Troca pelo ícone do Google,
    // que não precisa de chave (usa o mesmo domínio do link antigo).
    private fun logoDoCanal(logo: String): String {
        if (logo.isBlank()) return ""
        val dominio = Regex("^https?://logo\\.clearbit\\.com/([^/?#]+)")
            .find(logo)?.groupValues?.get(1)
        return if (dominio != null) {
            "https://www.google.com/s2/favicons?sz=256&domain=$dominio"
        } else {
            logo
        }
    }

    // Logo provisório com as iniciais do canal (sem logo ou enquanto baixa).
    private fun criarLogoInicial(nome: String): Bitmap {
        val lado = 300
        val bmp = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val tela = android.graphics.Canvas(bmp)
        val tinta = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        tinta.color = Color.rgb(26, 36, 78)
        tela.drawCircle(lado / 2f, lado / 2f, lado / 2f - 6f, tinta)
        tinta.style = android.graphics.Paint.Style.STROKE
        tinta.strokeWidth = 6f
        tinta.color = Color.rgb(220, 30, 40)
        tela.drawCircle(lado / 2f, lado / 2f, lado / 2f - 6f, tinta)

        val palavras = nome.split(" ", ":", "|")
            .map { it.filter { c -> c.isLetterOrDigit() } }
            .filter { it.isNotBlank() && it.uppercase() != "HD" && it.uppercase() != "4K" }
        val iniciais = palavras.take(2)
            .joinToString("") { it.first().toString() }
            .uppercase()
            .ifBlank { "TV" }

        tinta.style = android.graphics.Paint.Style.FILL
        tinta.color = Color.WHITE
        tinta.textSize = 120f
        tinta.typeface = Typeface.DEFAULT_BOLD
        tinta.textAlign = android.graphics.Paint.Align.CENTER
        val ajuste = (tinta.descent() + tinta.ascent()) / 2f
        tela.drawText(iniciais, lado / 2f, lado / 2f - ajuste, tinta)
        return bmp
    }

    private fun criarCardCanal(canal: Canal): View {
        val (card, imagem) = criarCardBase(
            canal.logo,
            rodapeDuasLinhas(canal.nome, canal.categoria)
        )

        // Logo centralizado, sem esticar, sobre fundo escuro.
        imagem.setBackgroundColor(Color.rgb(12, 16, 34))
        imagem.setPadding(dp(40), dp(40), dp(40), dp(100))
        imagem.scaleType = ImageView.ScaleType.FIT_CENTER

        if (canal.logo.isBlank() || cacheCapas[canal.logo] == null) {
            imagem.setImageBitmap(criarLogoInicial(canal.nome))
        }

        card.setOnClickListener {
            mostrarDetalhes(
                canal.nome,
                canal.logo,
                canal.categoria,
                "Canal de televisão ao vivo",
                "▶  ASSISTIR",
                0L,
                true
            ) { abrirVideo(canal.nome, canal.url, canal.logo) }
        }

        return card
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

    private val COR_PAINEL = Color.argb(250, 6, 9, 22)
    private val COR_ITEM = Color.argb(255, 12, 17, 38)
    private val COR_SECAO = Color.argb(255, 44, 56, 108)

    private fun criarFundoMenu(
        cor: Int,
        foco: Boolean,
        raio: Int = 14
    ): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.setColor(cor)
        fundo.cornerRadius = dp(raio).toFloat()
        if (foco) fundo.setStroke(dp(3), Color.rgb(255, 40, 40))
        else fundo.setStroke(dp(1), Color.rgb(36, 52, 104))
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
            setPadding(dp(18), 0, dp(16), 0)
            setTypeface(null, Typeface.BOLD)
            compoundDrawablePadding = dp(8)
            setCompoundDrawablesWithIntrinsicBounds(null, null, criarSeta(), null)
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

    // Traço vermelho na frente do título da seção (igual ao dos títulos da tela inicial).
    private fun marcarCabecalhoSecao(cabecalho: TextView) {
        val barra = GradientDrawable().apply {
            setColor(Color.rgb(255, 40, 40))
            setSize(dp(4), dp(22))
        }
        cabecalho.compoundDrawablePadding = dp(12)
        cabecalho.setCompoundDrawablesWithIntrinsicBounds(barra, null, criarSeta(), null)
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
        marcarCabecalhoSecao(cabecalho)

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
        marcarCabecalhoSecao(cabecalho)

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
            { mostrarListaCards(base, base === filmes) },
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
                gravity = Gravity.LEFT or Gravity.TOP
                setMargins(dp(10), dp(10), dp(10), dp(10))
            }
        )

        // Desliza entrando pelo lado esquerdo.
        menu.translationX = -(larguraMenu + dp(20)).toFloat()
        menu.animate().translationX(0f).setDuration(220).start()

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

        adicionarItemMenu("📡  Canais ao vivo") {
            fecharMenu()
            carregarCanais()
        }

        adicionarItemMenu("▶  Continuar assistindo") {
            fecharMenu()
            mostrarContinuarAssistindo()
        }

        adicionarItemMenu("🆕  Lançamentos recentes") {
            fecharMenu()
            mostrarListaCards(
                filmes
                    .filter { it.ano > 0 }
                    .sortedByDescending { it.ano }
                    .take(60)
            )
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
        if (desenhos.isNotEmpty() || desenhosSeries.isEmpty()) {
            secaoFilmes("DESENHOS", desenhos, false)
        }
        if (desenhosSeries.isNotEmpty()) {
            secaoSeries(
                if (desenhos.isEmpty()) "DESENHOS" else "DESENHOS (SÉRIES)",
                desenhosSeries,
                false
            )
        }
        secaoSeries("ANIMES", animes, false)

        botaoFecharMenu.requestFocus()
    }

    private fun fecharMenu() {
        if (!menuAberto) return

        menuAberto = false
        itemAtualizar = null
        val menuSaindo = menuLateral
        menuLateral = null
        if (menuSaindo != null) {
            menuSaindo.animate()
                .translationX(-(menuSaindo.width + dp(20)).toFloat())
                .setDuration(180)
                .withEndAction { raiz.removeView(menuSaindo) }
                .start()
        }
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

        var destino = proximo

        // Espaço vazio da fileira "Continuar assistindo": vai para o último card real da linha.
        if (cardsAtuais[destino].tag == "vazio") {
            if (direcao == colunas || direcao == -colunas) {
                val inicioLinha = destino - destino % colunas
                var achou = -1
                for (j in inicioLinha until minOf(inicioLinha + colunas, total)) {
                    if (cardsAtuais[j].tag != "vazio") achou = j
                }
                destino = if (achou >= 0) achou else atual
            } else {
                destino = atual
            }
        }

        indiceCardAtual = destino

        val card = cardsAtuais[destino]
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

        val achadasSeries = (series + doramas + animes + desenhosSeries).filter {
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

    // Controla se a lista está na tela (false enquanto o player está aberto).
    private var noPrimeiroPlano = true

    override fun onStart() {
        super.onStart()
        noPrimeiroPlano = true
    }

    override fun onStop() {
        noPrimeiroPlano = false
        super.onStop()
    }

    // Libera memória das capas quando o app sai da tela (ex.: abriu o filme).
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) cacheCapas.evictAll()
    }

    override fun onResume() {
        super.onResume()
        // Voltou do player: atualiza a fileira "Continuar assistindo".
        if (telaInicial && catalogoIniciado && filmes.isNotEmpty()) {
            try {
                mostrarListaCards(filmes, true)
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
