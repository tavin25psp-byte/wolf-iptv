package com.wolf.iptv

import android.app.Activity
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
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
    val episodios: ArrayList<Episodio>
)

data class Serie(
    val titulo: String,
    val ano: Int,
    val categoria: String,
    val capa: String,
    val temporadas: ArrayList<Temporada>
)

data class Dorama(
    val titulo: String,
    val ano: Int,
    val categoria: String,
    val capa: String,
    val temporadas: ArrayList<Temporada>
)

data class Anime(
    val titulo: String,
    val ano: Int,
    val categoria: String,
    val capa: String,
    val temporadas: ArrayList<Temporada>
)

class MainActivity : Activity() {

    companion object {

        private const val CATALOGO_URL =
            "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/refs/heads/main/catalogo.json"

        private const val PREFS =
            "WOLF_IPTV"

        private const val FAVORITOS =
            "FAVORITOS"
    }

    private lateinit var raiz:
        FrameLayout

    private lateinit var conteudo:
        LinearLayout

    private lateinit var scroll:
        ScrollView

    private lateinit var botaoMenu:
        TextView

    private lateinit var menuLateral:
        LinearLayout

    private lateinit var menuConteudo:
        LinearLayout

    private lateinit var botaoFecharMenu:
        TextView

    private val cacheCapas =
        ConcurrentHashMap<String, Bitmap>()

    private val capasEmCarregamento =
        ConcurrentHashMap.newKeySet<String>()

    private val cacheDiscoCapas by lazy {
        File(
            cacheDir,
            "capas"
        ).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val filmes =
        ArrayList<Filme>()

    private val series =
        ArrayList<Serie>()

    private val doramas =
        ArrayList<Dorama>()

    private val animes =
        ArrayList<Anime>()

    private val favoritos =
        HashSet<String>()

    private val historicoConteudo =
        ArrayList<() -> Unit>()

    private val cardsAtuais =
        ArrayList<View>()

    private val itensMenuFoco =
        ArrayList<View>()

    private var indiceCardAtual =
        0

    private var menuAberto =
        false

    private var categoriaAtual =
        "Todos"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        carregarFavoritos()

        criarInterface()

        carregarCatalogo()
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
            ImageView.ScaleType.FIT_CENTER

        fundo.alpha =
            0.55f

        fundo.setImageResource(
            R.drawable.wolf_background
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
            dp(15),
            dp(20),
            dp(10)
        )

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        criarCabecalho(
            camada
        )

        criarAbas(
            camada
        )

        scroll =
            ScrollView(this)

        scroll.isFillViewport =
            false

        scroll.overScrollMode =
            View.OVER_SCROLL_NEVER

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.setPadding(
            0,
            dp(8),
            0,
            dp(40)
        )

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

        criarMenuLateral()

        setContentView(
            raiz
        )
    }

    private fun criarCabecalho(
        camada: LinearLayout
    ) {

        val cabecalho =
            LinearLayout(this)

        cabecalho.orientation =
            LinearLayout.HORIZONTAL

        cabecalho.gravity =
            Gravity.CENTER_VERTICAL

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF IPTV"

        titulo.textSize =
            26f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        cabecalho.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )

        botaoMenu =
            TextView(this)

        botaoMenu.text =
            "☰"

        botaoMenu.textSize =
            30f

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable =
            true

        botaoMenu.isFocusableInTouchMode =
            true

        botaoMenu.background =
            criarFundoBotao()

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        cabecalho.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(75),
                dp(55)
            )
        )

        camada.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )
    }

    private fun criarAbas(
        camada: LinearLayout
    ) {

        val abas =
            LinearLayout(this)

        abas.orientation =
            LinearLayout.HORIZONTAL

        abas.gravity =
            Gravity.CENTER

        val abaFilmes =
            criarBotaoAba(
                "FILMES"
            )

        val abaSeries =
            criarBotaoAba(
                "SÉRIES"
            )

        val abaDoramas =
            criarBotaoAba(
                "DORAMAS"
            )

        abas.addView(
            abaFilmes,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
                )
            }
        )

        abas.addView(
            abaSeries,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
                )
            }
        )

        abas.addView(
            abaDoramas,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            ).apply {
                setMargins(
                    dp(4),
                    0,
                    dp(4),
                    0
                )
            }
        )

        abaFilmes.setOnClickListener {

            historicoConteudo.clear()

            mostrarListaFilmes(
                "Todos"
            )
        }

        abaSeries.setOnClickListener {

            historicoConteudo.clear()

            mostrarListaSeries(
                series
            )
        }

        abaDoramas.setOnClickListener {

            historicoConteudo.clear()

            mostrarListaDoramas(
                doramas
            )
        }

        camada.addView(
            abas,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )
    }

    private fun criarBotaoAba(
        texto: String
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            texto

        botao.textSize =
            16f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoBotao()

        botao.setOnFocusChangeListener {
                view,
                focado ->

            if (focado) {

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(100)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
        }

        return botao
    }

    private fun criarFundoBotao():
        GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(9).toFloat()

            setColor(
                Color.argb(
                    175,
                    25,
                    25,
                    25
                )
            )

            setStroke(
                dp(1),
                Color.argb(
                    100,
                    255,
                    255,
                    255
                )
            )
        }
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }    private fun carregarCatalogo() {

        thread {

            try {

                val conexao =
                    URL(CATALOGO_URL)
                        .openConnection() as HttpURLConnection

                conexao.connectTimeout =
                    15000

                conexao.readTimeout =
                    20000

                conexao.requestMethod =
                    "GET"

                conexao.setRequestProperty(
                    "User-Agent",
                    "WOLF-IPTV"
                )

                conexao.connect()

                if (
                    conexao.responseCode !in
                    200..299
                ) {

                    throw Exception(
                        "HTTP ${conexao.responseCode}"
                    )
                }

                val texto =
                    conexao.inputStream
                        .bufferedReader(
                            Charsets.UTF_8
                        )
                        .use {
                            it.readText()
                        }

                conexao.disconnect()

                val json =
                    JSONObject(texto)

                processarCatalogo(
                    json
                )

                runOnUiThread {

                    mostrarListaFilmes(
                        "Todos"
                    )
                }

            } catch (
                erro: Exception
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao carregar catálogo: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun processarCatalogo(
        json: JSONObject
    ) {

        filmes.clear()

        series.clear()

        doramas.clear()

        animes.clear()

        lerFilmes(
            json.optJSONArray(
                "filmes"
            )
        )

        lerSeries(
            json.optJSONArray(
                "series"
            )
        )

        lerDoramas(
            json.optJSONArray(
                "doramas"
            )
        )

        lerAnimes(
            json.optJSONArray(
                "animes"
            )
        )
    }

    private fun lerFilmes(
        lista: JSONArray?
    ) {

        if (lista == null) {
            return
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                val titulo =
                    item.optString(
                        "titulo",
                        "Sem título"
                    )

                val ano =
                    item.optInt(
                        "ano",
                        0
                    )

                val categoria =
                    item.optString(
                        "categoria",
                        "Outros"
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

                filmes.add(
                    Filme(
                        titulo = titulo,
                        ano = ano,
                        categoria = categoria,
                        capa = capa,
                        video = video
                    )
                )

            } catch (
                _: Exception
            ) {
                // Ignora somente o item inválido.
            }
        }
    }

    private fun lerSeries(
        lista: JSONArray?
    ) {

        if (lista == null) {
            return
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                val temporadas =
                    lerTemporadas(
                        item.optJSONArray(
                            "temporadas"
                        )
                    )

                series.add(
                    Serie(
                        titulo =
                            item.optString(
                                "titulo",
                                "Sem título"
                            ),

                        ano =
                            item.optInt(
                                "ano",
                                0
                            ),

                        categoria =
                            item.optString(
                                "categoria",
                                "Outros"
                            ),

                        capa =
                            item.optString(
                                "capa",
                                ""
                            ),

                        temporadas =
                            temporadas
                    )
                )

            } catch (
                _: Exception
            ) {
                // Mantém o restante do catálogo funcionando.
            }
        }
    }

    private fun lerDoramas(
        lista: JSONArray?
    ) {

        if (lista == null) {
            return
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                val temporadas =
                    lerTemporadas(
                        item.optJSONArray(
                            "temporadas"
                        )
                    )

                doramas.add(
                    Dorama(
                        titulo =
                            item.optString(
                                "titulo",
                                "Sem título"
                            ),

                        ano =
                            item.optInt(
                                "ano",
                                0
                            ),

                        categoria =
                            item.optString(
                                "categoria",
                                "Dorama"
                            ),

                        capa =
                            item.optString(
                                "capa",
                                ""
                            ),

                        temporadas =
                            temporadas
                    )
                )

            } catch (
                _: Exception
            ) {
                // Ignora apenas o item problemático.
            }
        }
    }

    private fun lerAnimes(
        lista: JSONArray?
    ) {

        if (lista == null) {
            return
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                val temporadas =
                    lerTemporadas(
                        item.optJSONArray(
                            "temporadas"
                        )
                    )

                animes.add(
                    Anime(
                        titulo =
                            item.optString(
                                "titulo",
                                "Sem título"
                            ),

                        ano =
                            item.optInt(
                                "ano",
                                0
                            ),

                        categoria =
                            item.optString(
                                "categoria",
                                "Anime"
                            ),

                        capa =
                            item.optString(
                                "capa",
                                ""
                            ),

                        temporadas =
                            temporadas
                    )
                )

            } catch (
                _: Exception
            ) {
                // Continua lendo os próximos animes.
            }
        }
    }

    private fun lerTemporadas(
        lista: JSONArray?
    ): ArrayList<Temporada> {

        val resultado =
            ArrayList<Temporada>()

        if (lista == null) {
            return resultado
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                val episodios =
                    lerEpisodios(
                        item.optJSONArray(
                            "episodios"
                        )
                    )

                resultado.add(
                    Temporada(
                        numero =
                            item.optInt(
                                "numero",
                                i + 1
                            ),

                        episodios =
                            episodios
                    )
                )

            } catch (
                _: Exception
            ) {
                // Não interrompe o catálogo inteiro.
            }
        }

        resultado.sortBy {
            it.numero
        }

        return resultado
    }

    private fun lerEpisodios(
        lista: JSONArray?
    ): ArrayList<Episodio> {

        val resultado =
            ArrayList<Episodio>()

        if (lista == null) {
            return resultado
        }

        for (
            i in 0 until lista.length()
        ) {

            try {

                val item =
                    lista.optJSONObject(i)
                        ?: continue

                resultado.add(
                    Episodio(
                        numero =
                            item.optInt(
                                "numero",
                                i + 1
                            ),

                        titulo =
                            item.optString(
                                "titulo",
                                "Episódio ${i + 1}"
                            ),

                        video =
                            item.optString(
                                "video",
                                ""
                            )
                    )
                )

            } catch (
                _: Exception
            ) {
                // Continua nos próximos episódios.
            }
        }

        resultado.sortBy {
            it.numero
        }

        return resultado
    }

    private fun carregarFavoritos() {

        val preferencias =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        favoritos.clear()

        favoritos.addAll(
            preferencias
                .getStringSet(
                    FAVORITOS,
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()
        )
    }

    private fun salvarFavoritos() {

        val preferencias =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        preferencias
            .edit()
            .putStringSet(
                FAVORITOS,
                favoritos.toSet()
            )
            .apply()
    }

    private fun chaveFavorito(
        tipo: String,
        titulo: String
    ): String {

        return "$tipo::$titulo"
    }

    private fun ehFavorito(
        tipo: String,
        titulo: String
    ): Boolean {

        return favoritos.contains(
            chaveFavorito(
                tipo,
                titulo
            )
        )
    }

    private fun alternarFavorito(
        tipo: String,
        titulo: String
    ) {

        val chave =
            chaveFavorito(
                tipo,
                titulo
            )

        if (
            favoritos.contains(
                chave
            )
        ) {

            favoritos.remove(
                chave
            )

        } else {

            favoritos.add(
                chave
            )
        }

        salvarFavoritos()
    }    private fun carregarCapa(
        imageView: ImageView,
        url: String
    ) {

        if (url.isBlank()) {

            imageView.setImageResource(
                R.drawable.wolf_background
            )

            return
        }

        imageView.tag =
            url

        val memoria =
            cacheCapas[url]

        if (memoria != null) {

            imageView.setImageBitmap(
                memoria
            )

            return
        }

        val arquivo =
            arquivoCacheCapa(
                url
            )

        if (arquivo.exists()) {

            thread {

                try {

                    val bitmap =
                        decodificarCapa(
                            arquivo
                        )

                    if (bitmap != null) {

                        cacheCapas[url] =
                            bitmap

                        runOnUiThread {

                            if (
                                imageView.tag ==
                                url
                            ) {

                                imageView.setImageBitmap(
                                    bitmap
                                )
                            }
                        }

                        return@thread
                    }

                } catch (
                    _: Exception
                ) {
                    // Se o cache estiver corrompido,
                    // tenta baixar novamente.
                }

                baixarCapa(
                    imageView,
                    url
                )
            }

            return
        }

        baixarCapa(
            imageView,
            url
        )
    }

    private fun baixarCapa(
        imageView: ImageView,
        url: String
    ) {

        if (
            !capasEmCarregamento.add(
                url
            )
        ) {
            return
        }

        thread {

            var bitmap:
                Bitmap? = null

            try {

                val conexao =
                    URL(url)
                        .openConnection() as HttpURLConnection

                conexao.connectTimeout =
                    10000

                conexao.readTimeout =
                    15000

                conexao.requestMethod =
                    "GET"

                conexao.setRequestProperty(
                    "User-Agent",
                    "WOLF-IPTV"
                )

                conexao.connect()

                if (
                    conexao.responseCode !in
                    200..299
                ) {

                    throw Exception(
                        "HTTP ${conexao.responseCode}"
                    )
                }

                val bytes =
                    conexao.inputStream
                        .use {
                            it.readBytes()
                        }

                conexao.disconnect()

                bitmap =
                    decodificarBytes(
                        bytes
                    )

                if (bitmap != null) {

                    cacheCapas[url] =
                        bitmap

                    salvarCapaNoDisco(
                        url,
                        bytes
                    )
                }

            } catch (
                _: Exception
            ) {

                bitmap =
                    null

            } finally {

                capasEmCarregamento.remove(
                    url
                )
            }

            runOnUiThread {

                if (
                    imageView.tag != url
                ) {
                    return@runOnUiThread
                }

                if (bitmap != null) {

                    imageView.setImageBitmap(
                        bitmap
                    )

                } else {

                    imageView.setImageResource(
                        R.drawable.wolf_background
                    )
                }
            }
        }
    }

    private fun decodificarBytes(
        bytes: ByteArray
    ): Bitmap? {

        if (bytes.isEmpty()) {
            return null
        }

        val opcoes =
            BitmapFactory.Options()

        opcoes.inJustDecodeBounds =
            true

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            opcoes
        )

        if (
            opcoes.outWidth <= 0 ||
            opcoes.outHeight <= 0
        ) {
            return null
        }

        opcoes.inSampleSize =
            calcularSampleSize(
                opcoes.outWidth,
                opcoes.outHeight
            )

        opcoes.inJustDecodeBounds =
            false

        opcoes.inPreferredConfig =
            Bitmap.Config.RGB_565

        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            opcoes
        )
    }

    private fun decodificarCapa(
        arquivo: File
    ): Bitmap? {

        val opcoes =
            BitmapFactory.Options()

        opcoes.inJustDecodeBounds =
            true

        BitmapFactory.decodeFile(
            arquivo.absolutePath,
            opcoes
        )

        if (
            opcoes.outWidth <= 0 ||
            opcoes.outHeight <= 0
        ) {
            return null
        }

        opcoes.inSampleSize =
            calcularSampleSize(
                opcoes.outWidth,
                opcoes.outHeight
            )

        opcoes.inJustDecodeBounds =
            false

        opcoes.inPreferredConfig =
            Bitmap.Config.RGB_565

        return BitmapFactory.decodeFile(
            arquivo.absolutePath,
            opcoes
        )
    }

    private fun calcularSampleSize(
        largura: Int,
        altura: Int
    ): Int {

        val larguraAlvo =
            420

        val alturaAlvo =
            560

        var sample =
            1

        var larguraAtual =
            largura

        var alturaAtual =
            altura

        while (
            larguraAtual / 2 >= larguraAlvo &&
            alturaAtual / 2 >= alturaAlvo
        ) {

            larguraAtual /= 2

            alturaAtual /= 2

            sample *= 2
        }

        return sample
    }

    private fun arquivoCacheCapa(
        url: String
    ): File {

        val digest =
            MessageDigest
                .getInstance("MD5")
                .digest(
                    url.toByteArray(
                        Charsets.UTF_8
                    )
                )

        val nome =
            digest.joinToString("") {
                "%02x".format(it)
            }

        return File(
            cacheDiscoCapas,
            "$nome.jpg"
        )
    }

    private fun salvarCapaNoDisco(
        url: String,
        bytes: ByteArray
    ) {

        try {

            val destino =
                arquivoCacheCapa(
                    url
                )

            val temporario =
                File(
                    destino.parentFile,
                    "${destino.name}.tmp"
                )

            FileOutputStream(
                temporario
            ).use {
                it.write(bytes)
                it.flush()
            }

            if (
                destino.exists()
            ) {
                destino.delete()
            }

            temporario.renameTo(
                destino
            )

        } catch (
            _: Exception
        ) {
            // Cache em disco é opcional.
            // Se falhar, a imagem continua funcionando
            // pelo cache de memória.
        }
    }

    private fun limparCacheMemoriaCapas() {

        cacheCapas.clear()
    }

    private fun limparCacheDiscoCapas() {

        try {

            cacheDiscoCapas
                .listFiles()
                ?.forEach {
                    it.delete()
                }

        } catch (
            _: Exception
        ) {
            // Não interrompe o aplicativo.
        }
    }

    private fun prepararImageView(
        imageView: ImageView
    ) {

        imageView.scaleType =
            ImageView.ScaleType.CENTER_CROP

        imageView.setImageResource(
            R.drawable.wolf_background
        )

        imageView.setBackgroundColor(
            Color.rgb(
                18,
                18,
                18
            )
        )
    }

    private fun estilizarFoco(
        view: View,
        focado: Boolean
    ) {

        if (focado) {

            view.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(110)
                .start()

            view.alpha =
                1f

        } else {

            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(110)
                .start()

            view.alpha =
                0.92f
        }
    }

    private fun textoSeguro(
        texto: String
    ): String {

        return texto
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    private fun adicionarEspaco(
        pai: LinearLayout,
        altura: Int
    ) {

        val espaco =
            View(this)

        pai.addView(
            espaco,
            LinearLayout.LayoutParams(
                1,
                dp(altura)
            )
        )
    }    private fun mostrarListaFilmes(
        categoria: String
    ) {

        categoriaAtual =
            categoria

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        val filmesFiltrados =
            if (
                categoria == "Todos"
            ) {

                filmes.sortedWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo.lowercase()
                    }
                )

            } else {

                filmes.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }.sortedWith(
                    compareByDescending<Filme> {
                        it.ano
                    }.thenBy {
                        it.titulo.lowercase()
                    }
                )
            }

        criarTituloSecao(
            conteudo,
            "FILMES"
        )

        criarCategoriasFilmes(
            conteudo,
            categoria
        )

        if (
            filmesFiltrados.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum filme encontrado."
            )

            return
        }

        criarGradeFilmes(
            conteudo,
            filmesFiltrados
        )
    }

    private fun criarTituloSecao(
        pai: LinearLayout,
        titulo: String
    ) {

        val texto =
            TextView(this)

        texto.text =
            titulo

        texto.textSize =
            22f

        texto.setTextColor(
            Color.WHITE
        )

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        texto.gravity =
            Gravity.CENTER_VERTICAL

        texto.ellipsize =
            TextUtils.TruncateAt.END

        pai.addView(
            texto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                setMargins(
                    dp(4),
                    dp(5),
                    dp(4),
                    dp(2)
                )
            }
        )
    }

    private fun criarCategoriasFilmes(
        pai: LinearLayout,
        categoriaSelecionada: String
    ) {

        val categorias =
            ArrayList<String>()

        categorias.add(
            "Todos"
        )

        filmes.forEach {

            val categoria =
                textoSeguro(
                    it.categoria
                )

            if (
                categoria.isNotEmpty() &&
                !categorias.contains(
                    categoria
                )
            ) {

                categorias.add(
                    categoria
                )
            }
        }

        val linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.CENTER_VERTICAL

        val scrollCategorias =
            ScrollView(this)

        scrollCategorias.isHorizontalScrollBarEnabled =
            false

        scrollCategorias.overScrollMode =
            View.OVER_SCROLL_NEVER

        val horizontal =
            LinearLayout(this)

        horizontal.orientation =
            LinearLayout.HORIZONTAL

        categorias.forEach { categoria ->

            val botao =
                criarBotaoCategoria(
                    categoria,
                    categoria ==
                        categoriaSelecionada
                )

            botao.setOnClickListener {

                mostrarListaFilmes(
                    categoria
                )
            }

            horizontal.addView(
                botao,
                LinearLayout.LayoutParams(
                    dp(125),
                    dp(45)
                ).apply {
                    setMargins(
                        dp(4),
                        dp(2),
                        dp(4),
                        dp(8)
                    )
                }
            )
        }

        scrollCategorias.addView(
            horizontal,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        linha.addView(
            scrollCategorias,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(53)
            )
        )

        pai.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(53)
            )
        )
    }

    private fun criarBotaoCategoria(
        texto: String,
        selecionado: Boolean
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            texto

        botao.textSize =
            14f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.maxLines =
            1

        botao.ellipsize =
            TextUtils.TruncateAt.END

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoCategoria(
                selecionado
            )

        botao.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )
        }

        return botao
    }

    private fun criarFundoCategoria(
        selecionado: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(8).toFloat()

            if (selecionado) {

                setColor(
                    Color.argb(
                        220,
                        90,
                        20,
                        20
                    )
                )

                setStroke(
                    dp(2),
                    Color.WHITE
                )

            } else {

                setColor(
                    Color.argb(
                        165,
                        25,
                        25,
                        25
                    )
                )

                setStroke(
                    dp(1),
                    Color.argb(
                        90,
                        255,
                        255,
                        255
                    )
                )
            }
        }
    }

    private fun criarGradeFilmes(
        pai: LinearLayout,
        lista: List<Filme>
    ) {

        var linha:
            LinearLayout? = null

        lista.forEachIndexed {
                indice,
                filme ->

            if (
                indice % 5 == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                pai.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(330)
                    ).apply {
                        setMargins(
                            0,
                            dp(5),
                            0,
                            dp(5)
                        )
                    }
                )
            }

            val card =
                criarCardFilme(
                    filme
                )

            cardsAtuais.add(
                card
            )

            linha?.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(320),
                    1f
                ).apply {
                    setMargins(
                        dp(4),
                        dp(4),
                        dp(4),
                        dp(4)
                    )
                }
            )
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0].post {

                cardsAtuais[0].requestFocus()
            }
        }
    }

    private fun criarCardFilme(
        filme: Filme
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.background =
            criarFundoCard()

        val capa =
            ImageView(this)

        prepararImageView(
            capa
        )

        capa.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(245)
            )

        card.addView(
            capa
        )

        carregarCapa(
            capa,
            filme.capa
        )

        val titulo =
            TextView(this)

        titulo.text =
            textoSeguro(
                filme.titulo
            )

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines =
            2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(45)
            ).apply {
                topMargin =
                    dp(5)
            }
        )

        val informacao =
            TextView(this)

        informacao.text =
            if (
                filme.ano > 0
            ) {
                "${filme.ano} • ${filme.categoria}"
            } else {
                filme.categoria
            }

        informacao.textSize =
            11f

        informacao.setTextColor(
            Color.LTGRAY
        )

        informacao.gravity =
            Gravity.CENTER

        informacao.maxLines =
            1

        informacao.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            informacao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )

            if (focado) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        view
                    )

                rolarParaView(
                    view
                )
            }
        }

        card.setOnClickListener {

            abrirFilme(
                filme
            )
        }

        return card
    }

    private fun criarFundoCard():
        GradientDrawable {

        return GradientDrawable().apply {

            cornerRadius =
                dp(10).toFloat()

            setColor(
                Color.argb(
                    190,
                    12,
                    12,
                    12
                )
            )

            setStroke(
                dp(1),
                Color.argb(
                    70,
                    255,
                    255,
                    255
                )
            )
        }
    }

    private fun criarMensagemVazia(
        pai: LinearLayout,
        mensagem: String
    ) {

        val texto =
            TextView(this)

        texto.text =
            mensagem

        texto.textSize =
            18f

        texto.setTextColor(
            Color.WHITE
        )

        texto.gravity =
            Gravity.CENTER

        pai.addView(
            texto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(180)
            )
        )
    }

    private fun rolarParaView(
        view: View
    ) {

        view.post {

            val margem =
                dp(40)

            val top =
                view.top

            val bottom =
                view.bottom

            val alturaTela =
                scroll.height

            val atual =
                scroll.scrollY

            if (
                top < atual + margem
            ) {

                scroll.smoothScrollTo(
                    0,
                    maxOf(
                        0,
                        top - margem
                    )
                )

            } else if (
                bottom >
                atual +
                alturaTela -
                margem
            ) {

                scroll.smoothScrollTo(
                    0,
                    bottom -
                        alturaTela +
                        margem
                )
            }
        }
    }    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "SÉRIES"
        )

        criarCategoriasSeries(
            conteudo
        )

        val ordenadas =
            lista.sortedWith(
                compareByDescending<Serie> {
                    it.ano
                }.thenBy {
                    it.titulo.lowercase()
                }
            )

        if (
            ordenadas.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhuma série encontrada."
            )

            return
        }

        criarGradeSeries(
            conteudo,
            ordenadas
        )
    }

    private fun criarCategoriasSeries(
        pai: LinearLayout
    ) {

        val categorias =
            ArrayList<String>()

        categorias.add(
            "Todos"
        )

        series.forEach {

            val categoria =
                textoSeguro(
                    it.categoria
                )

            if (
                categoria.isNotEmpty() &&
                !categorias.contains(
                    categoria
                )
            ) {

                categorias.add(
                    categoria
                )
            }
        }

        val scrollCategorias =
            ScrollView(this)

        scrollCategorias.isHorizontalScrollBarEnabled =
            false

        scrollCategorias.overScrollMode =
            View.OVER_SCROLL_NEVER

        val horizontal =
            LinearLayout(this)

        horizontal.orientation =
            LinearLayout.HORIZONTAL

        categorias.forEach {
                categoria ->

            val botao =
                criarBotaoCategoria(
                    categoria,
                    false
                )

            botao.setOnClickListener {

                val filtradas =
                    if (
                        categoria == "Todos"
                    ) {

                        series

                    } else {

                        series.filter {
                            it.categoria.equals(
                                categoria,
                                ignoreCase = true
                            )
                        }
                    }

                mostrarListaSeries(
                    filtradas
                )
            }

            horizontal.addView(
                botao,
                LinearLayout.LayoutParams(
                    dp(125),
                    dp(45)
                ).apply {
                    setMargins(
                        dp(4),
                        dp(2),
                        dp(4),
                        dp(8)
                    )
                }
            )
        }

        scrollCategorias.addView(
            horizontal,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        pai.addView(
            scrollCategorias,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(53)
            )
        )
    }

    private fun criarGradeSeries(
        pai: LinearLayout,
        lista: List<Serie>
    ) {

        var linha:
            LinearLayout? = null

        lista.forEachIndexed {
                indice,
                serie ->

            if (
                indice % 5 == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                pai.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(330)
                    ).apply {
                        setMargins(
                            0,
                            dp(5),
                            0,
                            dp(5)
                        )
                    }
                )
            }

            val card =
                criarCardSerie(
                    serie
                )

            cardsAtuais.add(
                card
            )

            linha?.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(320),
                    1f
                ).apply {
                    setMargins(
                        dp(4),
                        dp(4),
                        dp(4),
                        dp(4)
                    )
                }
            )
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0].post {
                cardsAtuais[0].requestFocus()
            }
        }
    }

    private fun criarCardSerie(
        serie: Serie
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.background =
            criarFundoCard()

        val capa =
            ImageView(this)

        prepararImageView(
            capa
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(245)
            )
        )

        carregarCapa(
            capa,
            serie.capa
        )

        val titulo =
            TextView(this)

        titulo.text =
            textoSeguro(
                serie.titulo
            )

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines =
            2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(45)
            ).apply {
                topMargin =
                    dp(5)
            }
        )

        val informacao =
            TextView(this)

        val temporadas =
            serie.temporadas.size

        informacao.text =
            if (
                serie.ano > 0
            ) {

                "${serie.ano} • " +
                    "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }

            } else {

                "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }
            }

        informacao.textSize =
            11f

        informacao.setTextColor(
            Color.LTGRAY
        )

        informacao.gravity =
            Gravity.CENTER

        informacao.maxLines =
            1

        informacao.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            informacao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )

            if (focado) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        view
                    )

                rolarParaView(
                    view
                )
            }
        }

        card.setOnClickListener {

            abrirSerie(
                serie
            )
        }

        return card
    }

    private fun mostrarTemporadasSerie(
        serie: Serie
    ) {

        historicoConteudo.add {
            mostrarListaSeries(
                series
            )
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            serie.titulo
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {

            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            serie.temporadas.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhuma temporada encontrada."
            )

            return
        }

        serie.temporadas
            .sortedBy {
                it.numero
            }
            .forEach { temporada ->

                val botao =
                    criarBotaoTemporada(
                        temporada
                    )

                botao.setOnClickListener {

                    mostrarEpisodios(
                        serie.titulo,
                        temporada
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun criarBotaoTemporada(
        temporada: Temporada
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            "Temporada ${temporada.numero}"

        botao.textSize =
            16f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER_VERTICAL

        botao.setPadding(
            dp(18),
            0,
            dp(18),
            0
        )

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoBotao()

        botao.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )
        }

        return botao
    }

    private fun criarBotaoVoltar():
        TextView {

        val botao =
            TextView(this)

        botao.text =
            "‹  VOLTAR"

        botao.textSize =
            15f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoBotao()

        botao.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )
        }

        return botao
        }    private fun mostrarEpisodios(
        tituloSerie: String,
        temporada: Temporada
    ) {

        historicoConteudo.add {

            val serie =
                series.firstOrNull {
                    it.titulo.equals(
                        tituloSerie,
                        ignoreCase = true
                    )
                }

            if (serie != null) {

                mostrarTemporadasSerie(
                    serie
                )
            }
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "$tituloSerie — Temporada ${temporada.numero}"
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {

            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            temporada.episodios.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum episódio encontrado."
            )

            return
        }

        temporada.episodios
            .sortedBy {
                it.numero
            }
            .forEach { episodio ->

                val botao =
                    criarBotaoEpisodio(
                        episodio
                    )

                botao.setOnClickListener {

                    abrirEpisodio(
                        tituloSerie,
                        temporada.numero,
                        episodio
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(64)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun criarBotaoEpisodio(
        episodio: Episodio
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            "Episódio ${episodio.numero}  •  " +
                textoSeguro(
                    episodio.titulo
                )

        botao.textSize =
            15f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER_VERTICAL

        botao.setPadding(
            dp(18),
            0,
            dp(18),
            0
        )

        botao.maxLines =
            1

        botao.ellipsize =
            TextUtils.TruncateAt.END

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoBotao()

        botao.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )

            if (focado) {
                rolarParaView(
                    view
                )
            }
        }

        return botao
    }

    private fun abrirSerie(
        serie: Serie
    ) {

        mostrarTemporadasSerie(
            serie
        )
    }

    private fun abrirFilme(
        filme: Filme
    ) {

        if (
            filme.video.isBlank()
        ) {

            Toast.makeText(
                this,
                "Vídeo não disponível.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        registrarHistorico(
            "Filme",
            filme.titulo
        )

        abrirVideo(
            filme.titulo,
            filme.video
        )
    }

    private fun abrirEpisodio(
        tituloSerie: String,
        numeroTemporada: Int,
        episodio: Episodio
    ) {

        if (
            episodio.video.isBlank()
        ) {

            Toast.makeText(
                this,
                "Vídeo não disponível.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        registrarHistorico(
            "Série",
            "$tituloSerie • T$numeroTemporada E${episodio.numero}"
        )

        abrirVideo(
            "$tituloSerie — " +
                "T${numeroTemporada}E${episodio.numero}",
            episodio.video
        )
    }

    private fun abrirVideo(
        titulo: String,
        url: String
    ) {

        try {

            val intent =
                Intent(
                    this,
                    PlayerActivity::class.java
                )

            intent.putExtra(
                "titulo",
                titulo
            )

            intent.putExtra(
                "video",
                url
            )

            intent.putExtra(
                "url",
                url
            )

            startActivity(
                intent
            )

        } catch (
            erro: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível abrir o vídeo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun registrarHistorico(
        tipo: String,
        titulo: String
    ) {

        val preferencias =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        val atual =
            preferencias.getStringSet(
                "HISTORICO",
                emptySet()
            )?.toMutableSet()
                ?: mutableSetOf()

        val chave =
            "$tipo::$titulo"

        atual.remove(
            chave
        )

        val novo =
            LinkedHashSet<String>()

        novo.add(
            chave
        )

        novo.addAll(
            atual
        )

        while (
            novo.size > 30
        ) {

            val ultimo =
                novo.lastOrNull()
                    ?: break

            novo.remove(
                ultimo
            )
        }

        preferencias
            .edit()
            .putStringSet(
                "HISTORICO",
                novo
            )
            .apply()
    }

    private fun lerHistorico():
        List<String> {

        val preferencias =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        return preferencias
            .getStringSet(
                "HISTORICO",
                emptySet()
            )
            ?.toList()
            ?: emptyList()
    }

    private fun limparHistorico() {

        getSharedPreferences(
            PREFS,
            MODE_PRIVATE
        )
            .edit()
            .remove(
                "HISTORICO"
            )
            .apply()
    }

    private fun voltarConteudo() {

        if (
            historicoConteudo.isEmpty()
        ) {

            mostrarListaFilmes(
                "Todos"
            )

            return
        }

        val anterior =
            historicoConteudo.removeAt(
                historicoConteudo.lastIndex
            )

        anterior.invoke()
    }

    private fun contarEpisodios(
        serie: Serie
    ): Int {

        var total =
            0

        serie.temporadas.forEach {
            total +=
                it.episodios.size
        }

        return total
    }

    private fun primeiraTemporada(
        serie: Serie
    ): Temporada? {

        return serie.temporadas
            .sortedBy {
                it.numero
            }
            .firstOrNull()
    }

    private fun primeiroEpisodio(
        serie: Serie
    ): Episodio? {

        return primeiraTemporada(
            serie
        )?.episodios
            ?.sortedBy {
                it.numero
            }
            ?.firstOrNull()
    }    private fun mostrarListaDoramas(
        lista: List<Dorama>
    ) {

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "DORAMAS"
        )

        criarCategoriasDoramas(
            conteudo
        )

        val ordenados =
            lista.sortedWith(
                compareByDescending<Dorama> {
                    it.ano
                }.thenBy {
                    it.titulo.lowercase()
                }
            )

        if (
            ordenados.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum dorama encontrado."
            )

            return
        }

        criarGradeDoramas(
            conteudo,
            ordenados
        )
    }

    private fun criarCategoriasDoramas(
        pai: LinearLayout
    ) {

        val categorias =
            ArrayList<String>()

        categorias.add(
            "Todos"
        )

        doramas.forEach {

            val categoria =
                textoSeguro(
                    it.categoria
                )

            if (
                categoria.isNotEmpty() &&
                !categorias.contains(
                    categoria
                )
            ) {

                categorias.add(
                    categoria
                )
            }
        }

        val scrollCategorias =
            ScrollView(this)

        scrollCategorias.isHorizontalScrollBarEnabled =
            false

        scrollCategorias.overScrollMode =
            View.OVER_SCROLL_NEVER

        val horizontal =
            LinearLayout(this)

        horizontal.orientation =
            LinearLayout.HORIZONTAL

        categorias.forEach {
                categoria ->

            val botao =
                criarBotaoCategoria(
                    categoria,
                    false
                )

            botao.setOnClickListener {

                val filtradas =
                    if (
                        categoria == "Todos"
                    ) {

                        doramas

                    } else {

                        doramas.filter {
                            it.categoria.equals(
                                categoria,
                                ignoreCase = true
                            )
                        }
                    }

                mostrarListaDoramas(
                    filtradas
                )
            }

            horizontal.addView(
                botao,
                LinearLayout.LayoutParams(
                    dp(125),
                    dp(45)
                ).apply {
                    setMargins(
                        dp(4),
                        dp(2),
                        dp(4),
                        dp(8)
                    )
                }
            )
        }

        scrollCategorias.addView(
            horizontal,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        pai.addView(
            scrollCategorias,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(53)
            )
        )
    }

    private fun criarGradeDoramas(
        pai: LinearLayout,
        lista: List<Dorama>
    ) {

        var linha:
            LinearLayout? = null

        lista.forEachIndexed {
                indice,
                dorama ->

            if (
                indice % 5 == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                pai.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(330)
                    ).apply {
                        setMargins(
                            0,
                            dp(5),
                            0,
                            dp(5)
                        )
                    }
                )
            }

            val card =
                criarCardDorama(
                    dorama
                )

            cardsAtuais.add(
                card
            )

            linha?.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(320),
                    1f
                ).apply {
                    setMargins(
                        dp(4),
                        dp(4),
                        dp(4),
                        dp(4)
                    )
                }
            )
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0].post {
                cardsAtuais[0].requestFocus()
            }
        }
    }

    private fun criarCardDorama(
        dorama: Dorama
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.background =
            criarFundoCard()

        val capa =
            ImageView(this)

        prepararImageView(
            capa
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(245)
            )
        )

        carregarCapa(
            capa,
            dorama.capa
        )

        val titulo =
            TextView(this)

        titulo.text =
            textoSeguro(
                dorama.titulo
            )

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines =
            2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(45)
            ).apply {
                topMargin =
                    dp(5)
            }
        )

        val informacao =
            TextView(this)

        val temporadas =
            dorama.temporadas.size

        informacao.text =
            if (
                dorama.ano > 0
            ) {

                "${dorama.ano} • " +
                    "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }

            } else {

                "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }
            }

        informacao.textSize =
            11f

        informacao.setTextColor(
            Color.LTGRAY
        )

        informacao.gravity =
            Gravity.CENTER

        informacao.maxLines =
            1

        informacao.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            informacao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )

            if (focado) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        view
                    )

                rolarParaView(
                    view
                )
            }
        }

        card.setOnClickListener {

            abrirDorama(
                dorama
            )
        }

        return card
    }

    private fun abrirDorama(
        dorama: Dorama
    ) {

        mostrarTemporadasDorama(
            dorama
        )
    }

    private fun mostrarTemporadasDorama(
        dorama: Dorama
    ) {

        historicoConteudo.add {
            mostrarListaDoramas(
                doramas
            )
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            dorama.titulo
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {
            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            dorama.temporadas.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhuma temporada encontrada."
            )

            return
        }

        dorama.temporadas
            .sortedBy {
                it.numero
            }
            .forEach { temporada ->

                val botao =
                    criarBotaoTemporada(
                        temporada
                    )

                botao.setOnClickListener {

                    mostrarEpisodiosDorama(
                        dorama.titulo,
                        temporada
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun mostrarEpisodiosDorama(
        tituloDorama: String,
        temporada: Temporada
    ) {

        historicoConteudo.add {

            val dorama =
                doramas.firstOrNull {
                    it.titulo.equals(
                        tituloDorama,
                        ignoreCase = true
                    )
                }

            if (dorama != null) {

                mostrarTemporadasDorama(
                    dorama
                )
            }
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "$tituloDorama — " +
                "Temporada ${temporada.numero}"
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {
            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            temporada.episodios.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum episódio encontrado."
            )

            return
        }

        temporada.episodios
            .sortedBy {
                it.numero
            }
            .forEach { episodio ->

                val botao =
                    criarBotaoEpisodio(
                        episodio
                    )

                botao.setOnClickListener {

                    abrirEpisodioDorama(
                        tituloDorama,
                        temporada.numero,
                        episodio
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(64)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun abrirEpisodioDorama(
        tituloDorama: String,
        numeroTemporada: Int,
        episodio: Episodio
    ) {

        if (
            episodio.video.isBlank()
        ) {

            Toast.makeText(
                this,
                "Vídeo não disponível.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        registrarHistorico(
            "Dorama",
            "$tituloDorama • " +
                "T$numeroTemporada E${episodio.numero}"
        )

        abrirVideo(
            "$tituloDorama — " +
                "T${numeroTemporada}E${episodio.numero}",
            episodio.video
        )
    }

    private fun mostrarListaAnimes(
        lista: List<Anime>
    ) {

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "ANIME"
        )

        criarCategoriasAnimes(
            conteudo
        )

        val ordenados =
            lista.sortedWith(
                compareByDescending<Anime> {
                    it.ano
                }.thenBy {
                    it.titulo.lowercase()
                }
            )

        if (
            ordenados.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum anime encontrado."
            )

            return
        }

        criarGradeAnimes(
            conteudo,
            ordenados
        )
    }

    private fun criarCategoriasAnimes(
        pai: LinearLayout
    ) {

        val categorias =
            ArrayList<String>()

        categorias.add(
            "Todos"
        )

        animes.forEach {

            val categoria =
                textoSeguro(
                    it.categoria
                )

            if (
                categoria.isNotEmpty() &&
                !categorias.contains(
                    categoria
                )
            ) {

                categorias.add(
                    categoria
                )
            }
        }

        val scrollCategorias =
            ScrollView(this)

        scrollCategorias.isHorizontalScrollBarEnabled =
            false

        scrollCategorias.overScrollMode =
            View.OVER_SCROLL_NEVER

        val horizontal =
            LinearLayout(this)

        horizontal.orientation =
            LinearLayout.HORIZONTAL

        categorias.forEach {
                categoria ->

            val botao =
                criarBotaoCategoria(
                    categoria,
                    false
                )

            botao.setOnClickListener {

                val filtrados =
                    if (
                        categoria == "Todos"
                    ) {

                        animes

                    } else {

                        animes.filter {
                            it.categoria.equals(
                                categoria,
                                ignoreCase = true
                            )
                        }
                    }

                mostrarListaAnimes(
                    filtrados
                )
            }

            horizontal.addView(
                botao,
                LinearLayout.LayoutParams(
                    dp(125),
                    dp(45)
                ).apply {
                    setMargins(
                        dp(4),
                        dp(2),
                        dp(4),
                        dp(8)
                    )
                }
            )
        }

        scrollCategorias.addView(
            horizontal,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        pai.addView(
            scrollCategorias,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(53)
            )
        )
    }    private fun criarGradeAnimes(
        pai: LinearLayout,
        lista: List<Anime>
    ) {

        var linha:
            LinearLayout? = null

        lista.forEachIndexed {
                indice,
                anime ->

            if (
                indice % 5 == 0
            ) {

                linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.gravity =
                    Gravity.TOP

                pai.addView(
                    linha,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(330)
                    ).apply {
                        setMargins(
                            0,
                            dp(5),
                            0,
                            dp(5)
                        )
                    }
                )
            }

            val card =
                criarCardAnime(
                    anime
                )

            cardsAtuais.add(
                card
            )

            linha?.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(320),
                    1f
                ).apply {
                    setMargins(
                        dp(4),
                        dp(4),
                        dp(4),
                        dp(4)
                    )
                }
            )
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0].post {
                cardsAtuais[0].requestFocus()
            }
        }
    }

    private fun criarCardAnime(
        anime: Anime
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.TOP or
                Gravity.CENTER_HORIZONTAL

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.setPadding(
            dp(4),
            dp(4),
            dp(4),
            dp(4)
        )

        card.background =
            criarFundoCard()

        val capa =
            ImageView(this)

        prepararImageView(
            capa
        )

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(245)
            )
        )

        carregarCapa(
            capa,
            anime.capa
        )

        val titulo =
            TextView(this)

        titulo.text =
            textoSeguro(
                anime.titulo
            )

        titulo.textSize =
            14f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER

        titulo.maxLines =
            2

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(45)
            ).apply {
                topMargin =
                    dp(5)
            }
        )

        val informacao =
            TextView(this)

        val temporadas =
            anime.temporadas.size

        informacao.text =
            if (
                anime.ano > 0
            ) {

                "${anime.ano} • " +
                    "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }

            } else {

                "$temporadas " +
                    if (
                        temporadas == 1
                    ) {
                        "temporada"
                    } else {
                        "temporadas"
                    }
            }

        informacao.textSize =
            11f

        informacao.setTextColor(
            Color.LTGRAY
        )

        informacao.gravity =
            Gravity.CENTER

        informacao.maxLines =
            1

        informacao.ellipsize =
            TextUtils.TruncateAt.END

        card.addView(
            informacao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.setOnFocusChangeListener {
                view,
                focado ->

            estilizarFoco(
                view,
                focado
            )

            if (focado) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        view
                    )

                rolarParaView(
                    view
                )
            }
        }

        card.setOnClickListener {

            abrirAnime(
                anime
            )
        }

        return card
    }

    private fun abrirAnime(
        anime: Anime
    ) {

        mostrarTemporadasAnime(
            anime
        )
    }

    private fun mostrarTemporadasAnime(
        anime: Anime
    ) {

        historicoConteudo.add {

            mostrarListaAnimes(
                animes
            )
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            anime.titulo
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {
            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            anime.temporadas.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhuma temporada encontrada."
            )

            return
        }

        anime.temporadas
            .sortedBy {
                it.numero
            }
            .forEach { temporada ->

                val botao =
                    criarBotaoTemporada(
                        temporada
                    )

                botao.setOnClickListener {

                    mostrarEpisodiosAnime(
                        anime.titulo,
                        temporada
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(58)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun mostrarEpisodiosAnime(
        tituloAnime: String,
        temporada: Temporada
    ) {

        historicoConteudo.add {

            val anime =
                animes.firstOrNull {
                    it.titulo.equals(
                        tituloAnime,
                        ignoreCase = true
                    )
                }

            if (anime != null) {

                mostrarTemporadasAnime(
                    anime
                )
            }
        }

        indiceCardAtual =
            0

        cardsAtuais.clear()

        conteudo.removeAllViews()

        criarTituloSecao(
            conteudo,
            "$tituloAnime — " +
                "Temporada ${temporada.numero}"
        )

        val voltar =
            criarBotaoVoltar()

        voltar.setOnClickListener {
            voltarConteudo()
        }

        conteudo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(180),
                dp(48)
            ).apply {
                setMargins(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(8)
                )
            }
        )

        if (
            temporada.episodios.isEmpty()
        ) {

            criarMensagemVazia(
                conteudo,
                "Nenhum episódio encontrado."
            )

            return
        }

        temporada.episodios
            .sortedBy {
                it.numero
            }
            .forEach { episodio ->

                val botao =
                    criarBotaoEpisodio(
                        episodio
                    )

                botao.setOnClickListener {

                    abrirEpisodioAnime(
                        tituloAnime,
                        temporada.numero,
                        episodio
                    )
                }

                conteudo.addView(
                    botao,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(64)
                    ).apply {
                        setMargins(
                            dp(4),
                            dp(5),
                            dp(4),
                            dp(5)
                        )
                    }
                )
            }
    }

    private fun abrirEpisodioAnime(
        tituloAnime: String,
        numeroTemporada: Int,
        episodio: Episodio
    ) {

        if (
            episodio.video.isBlank()
        ) {

            Toast.makeText(
                this,
                "Vídeo não disponível.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        registrarHistorico(
            "Anime",
            "$tituloAnime • " +
                "T$numeroTemporada E${episodio.numero}"
        )

        abrirVideo(
            "$tituloAnime — " +
                "T${numeroTemporada}E${episodio.numero}",
            episodio.video
        )
    }    private fun mostrarFavoritos() {

        historicoConteudo.clear()

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        categoriaAtual = "Favoritos"

        if (favoritos.isEmpty()) {

            conteudo.addView(
                criarSubtitulo(
                    "FAVORITOS"
                )
            )

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum conteúdo foi adicionado aos favoritos."

            vazio.textSize =
                18f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            vazio.setPadding(
                dp(20),
                dp(60),
                dp(20),
                dp(60)
            )

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            return
        }

        conteudo.addView(
            criarSubtitulo(
                "FAVORITOS"
            )
        )

        val filmesFavoritos =
            filmes.filter {
                favoritos.contains(
                    chaveFilme(it)
                )
            }

        val seriesFavoritas =
            series.filter {
                favoritos.contains(
                    chaveSerie(it)
                )
            }

        val doramasFavoritos =
            doramas.filter {
                favoritos.contains(
                    chaveDorama(it)
                )
            }

        val animesFavoritos =
            animes.filter {
                favoritos.contains(
                    chaveAnime(it)
                )
            }

        if (filmesFavoritos.isNotEmpty()) {

            conteudo.addView(
                criarSubtitulo(
                    "FILMES"
                )
            )

            criarGradeFilmes(
                filmesFavoritos
            )
        }

        if (seriesFavoritas.isNotEmpty()) {

            conteudo.addView(
                criarSubtitulo(
                    "SÉRIES"
                )
            )

            criarGradeSeries(
                seriesFavoritas
            )
        }

        if (doramasFavoritos.isNotEmpty()) {

            conteudo.addView(
                criarSubtitulo(
                    "DORAMAS"
                )
            )

            criarGradeDoramas(
                doramasFavoritos
            )
        }

        if (animesFavoritos.isNotEmpty()) {

            conteudo.addView(
                criarSubtitulo(
                    "ANIMES"
                )
            )

            criarGradeAnimes(
                animesFavoritos
            )
        }

        if (cardsAtuais.isNotEmpty()) {

            indiceCardAtual = 0

            cardsAtuais[0].requestFocus()
        }
    }


    private fun criarSubtitulo(
        texto: String
    ): TextView {

        val subtitulo =
            TextView(this)

        subtitulo.text =
            texto

        subtitulo.textSize =
            21f

        subtitulo.setTextColor(
            Color.WHITE
        )

        subtitulo.setTypeface(
            null,
            Typeface.BOLD
        )

        subtitulo.gravity =
            Gravity.CENTER_VERTICAL

        subtitulo.setPadding(
            dp(8),
            dp(14),
            dp(8),
            dp(8)
        )

        return subtitulo
    }


    private fun chaveFilme(
        filme: Filme
    ): String {

        return "FILME|" +
            filme.titulo
    }


    private fun chaveSerie(
        serie: Serie
    ): String {

        return "SERIE|" +
            serie.titulo
    }


    private fun chaveDorama(
        dorama: Dorama
    ): String {

        return "DORAMA|" +
            dorama.titulo
    }


    private fun chaveAnime(
        anime: Anime
    ): String {

        return "ANIME|" +
            anime.titulo
    }    private fun alternarFavorito(
        chave: String
    ) {

        if (favoritos.contains(chave)) {

            favoritos.remove(
                chave
            )

            Toast.makeText(
                this,
                "Removido dos favoritos",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            favoritos.add(
                chave
            )

            Toast.makeText(
                this,
                "Adicionado aos favoritos",
                Toast.LENGTH_SHORT
            ).show()
        }

        salvarFavoritos()
    }


    private fun alternarFavoritoDoFoco() {

        if (cardsAtuais.isEmpty()) {
            return
        }

        if (
            indiceCardAtual < 0 ||
            indiceCardAtual >= cardsAtuais.size
        ) {
            return
        }

        val card =
            cardsAtuais[indiceCardAtual]

        val titulo =
            obterTituloDoCard(card)
                ?: return

        var encontrado =
            false

        val filme =
            filmes.firstOrNull {
                it.titulo == titulo
            }

        if (filme != null) {

            alternarFavorito(
                chaveFilme(filme)
            )

            encontrado = true
        }

        if (!encontrado) {

            val serie =
                series.firstOrNull {
                    it.titulo == titulo
                }

            if (serie != null) {

                alternarFavorito(
                    chaveSerie(serie)
                )

                encontrado = true
            }
        }

        if (!encontrado) {

            val dorama =
                doramas.firstOrNull {
                    it.titulo == titulo
                }

            if (dorama != null) {

                alternarFavorito(
                    chaveDorama(dorama)
                )

                encontrado = true
            }
        }

        if (!encontrado) {

            val anime =
                animes.firstOrNull {
                    it.titulo == titulo
                }

            if (anime != null) {

                alternarFavorito(
                    chaveAnime(anime)
                )

                encontrado = true
            }
        }
    }


    private fun obterTituloDoCard(
        card: View
    ): String? {

        if (card !is LinearLayout) {
            return null
        }

        for (
            indice in 0 until card.childCount
        ) {

            val filho =
                card.getChildAt(indice)

            if (filho is TextView) {

                val texto =
                    filho.text
                        ?.toString()
                        ?.trim()

                if (
                    !texto.isNullOrEmpty() &&
                    texto != "▶" &&
                    texto != "★" &&
                    texto != "☆"
                ) {

                    return texto
                }
            }
        }

        return null
    }


    private fun limparFavoritos() {

        favoritos.clear()

        salvarFavoritos()

        Toast.makeText(
            this,
            "Favoritos limpos",
            Toast.LENGTH_SHORT
        ).show()

        mostrarFavoritos()
    }


    private fun atualizarTelaFavoritosSeNecessario() {

        if (
            categoriaAtual == "Favoritos"
        ) {

            mostrarFavoritos()
        }
    }    private fun criarMenuLateral() {

        menuLateral =
            LinearLayout(this)

        menuLateral.orientation =
            LinearLayout.VERTICAL

        menuLateral.setPadding(
            dp(18),
            dp(20),
            dp(18),
            dp(20)
        )

        menuLateral.setBackgroundColor(
            Color.argb(
                245,
                12,
                12,
                12
            )
        )

        menuLateral.elevation =
            dp(20).toFloat()

        menuLateral.visibility =
            View.VISIBLE

        menuLateral.translationX =
            -dp(360).toFloat()

        val parametros =
            FrameLayout.LayoutParams(
                dp(360),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        parametros.gravity =
            Gravity.START

        raiz.addView(
            menuLateral,
            parametros
        )

        botaoFecharMenu =
            TextView(this)

        botaoFecharMenu.text =
            "✕   MENU"

        botaoFecharMenu.textSize =
            21f

        botaoFecharMenu.setTextColor(
            Color.WHITE
        )

        botaoFecharMenu.setTypeface(
            null,
            Typeface.BOLD
        )

        botaoFecharMenu.gravity =
            Gravity.CENTER_VERTICAL

        botaoFecharMenu.isFocusable =
            true

        botaoFecharMenu.isFocusableInTouchMode =
            true

        botaoFecharMenu.background =
            criarFundoBotao()

        botaoFecharMenu.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        botaoFecharMenu.setOnClickListener {
            fecharMenu()
        }

        menuLateral.addView(
            botaoFecharMenu,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val separador =
            View(this)

        separador.setBackgroundColor(
            Color.argb(
                90,
                255,
                255,
                255
            )
        )

        menuLateral.addView(
            separador,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(15)
                bottomMargin = dp(15)
            }
        )

        menuConteudo =
            LinearLayout(this)

        menuConteudo.orientation =
            LinearLayout.VERTICAL

        menuLateral.addView(
            menuConteudo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        itensMenuFoco.clear()

        adicionarItemMenu(
            "⌂   INÍCIO"
        ) {
            fecharMenu()

            historicoConteudo.clear()

            mostrarListaFilmes(
                "Todos"
            )
        }

        adicionarItemMenu(
            "★   FAVORITOS"
        ) {
            fecharMenu()

            mostrarFavoritos()
        }

        adicionarItemMenu(
            "▶   CONTINUE ASSISTINDO"
        ) {
            fecharMenu()

            mostrarHistorico()
        }

        adicionarItemMenu(
            "🎬   FILMES"
        ) {
            fecharMenu()

            historicoConteudo.clear()

            mostrarListaFilmes(
                "Todos"
            )
        }

        adicionarItemMenu(
            "▣   SÉRIES"
        ) {
            fecharMenu()

            historicoConteudo.clear()

            mostrarListaSeries(
                series
            )
        }

        adicionarItemMenu(
            "♥   DORAMAS"
        ) {
            fecharMenu()

            historicoConteudo.clear()

            mostrarListaDoramas(
                doramas
            )
        }

        adicionarItemMenu(
            "◆   ANIME"
        ) {
            fecharMenu()

            historicoConteudo.clear()

            mostrarListaAnimes(
                animes
            )
        }

        adicionarItemMenu(
            "★   FAVORITAR ITEM FOCADO"
        ) {
            alternarFavoritoDoFoco()
        }

        adicionarItemMenu(
            "🗑   LIMPAR FAVORITOS"
        ) {
            limparFavoritos()
        }
    }


    private fun adicionarItemMenu(
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            16f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(16),
            0,
            dp(10),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true

        item.background =
            criarFundoBotao()

        item.setOnFocusChangeListener {
                view,
                focado ->

            if (focado) {

                view.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(100)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
        }

        item.setOnClickListener {
            acao()
        }

        menuConteudo.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                bottomMargin = dp(8)
            }
        )

        itensMenuFoco.add(
            item
        )
    }    private fun abrirMenu() {

        if (menuAberto) {
            return
        }

        menuAberto = true

        menuLateral.visibility =
            View.VISIBLE

        menuLateral.animate()
            .translationX(0f)
            .setDuration(220)
            .start()

        botaoFecharMenu.requestFocus()
    }


    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        menuLateral.animate()
            .translationX(
                -dp(360).toFloat()
            )
            .setDuration(220)
            .start()

        botaoMenu.requestFocus()
    }


    private fun mostrarHistorico() {

        historicoConteudo.clear()

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        categoriaAtual =
            "Continue Assistindo"

        conteudo.addView(
            criarSubtitulo(
                "CONTINUE ASSISTINDO"
            )
        )

        val preferencias =
            getSharedPreferences(
                PREFS,
                MODE_PRIVATE
            )

        val lista =
            preferencias
                .getStringSet(
                    "HISTORICO",
                    emptySet()
                )
                ?.toList()
                ?: emptyList()

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Você ainda não começou nenhum conteúdo."

            vazio.textSize =
                18f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            vazio.setPadding(
                dp(20),
                dp(60),
                dp(20),
                dp(60)
            )

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            return
        }

        val encontrados =
            ArrayList<Filme>()

        for (titulo in lista) {

            val filme =
                filmes.firstOrNull {
                    it.titulo == titulo
                }

            if (filme != null) {

                encontrados.add(
                    filme
                )
            }
        }

        if (encontrados.isNotEmpty()) {

            criarGradeFilmes(
                encontrados
            )
        }

        val nomesRestantes =
            lista.filter { titulo ->

                encontrados.none {
                    it.titulo == titulo
                }
            }

        if (nomesRestantes.isNotEmpty()) {

            val listaTexto =
                LinearLayout(this)

            listaTexto.orientation =
                LinearLayout.VERTICAL

            for (
                titulo in nomesRestantes
            ) {

                val item =
                    TextView(this)

                item.text =
                    "▶  $titulo"

                item.textSize =
                    17f

                item.setTextColor(
                    Color.WHITE
                )

                item.gravity =
                    Gravity.CENTER_VERTICAL

                item.setPadding(
                    dp(18),
                    0,
                    dp(10),
                    0
                )

                item.isFocusable =
                    true

                item.isFocusableInTouchMode =
                    true

                item.background =
                    criarFundoBotao()

                listaTexto.addView(
                    item,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                    ).apply {
                        bottomMargin = dp(8)
                    }
                )
            }

            conteudo.addView(
                listaTexto,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        if (cardsAtuais.isNotEmpty()) {

            indiceCardAtual = 0

            cardsAtuais[0].requestFocus()
        }
    }


    private fun limparHistorico() {

        getSharedPreferences(
            PREFS,
            MODE_PRIVATE
        )
            .edit()
            .remove(
                "HISTORICO"
            )
            .apply()

        historicoConteudo.clear()

        Toast.makeText(
            this,
            "Histórico limpo",
            Toast.LENGTH_SHORT
        ).show()

        mostrarHistorico()
    }


    private fun limparCacheCapas() {

        thread {

            try {

                cacheCapas.clear()

                capasEmCarregamento.clear()

                if (
                    cacheDiscoCapas.exists()
                ) {

                    cacheDiscoCapas
                        .listFiles()
                        ?.forEach {
                            it.delete()
                        }
                }

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Cache das capas limpo",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (
                erro: Exception
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Não foi possível limpar o cache",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }    private fun adicionarItemLimparCache() {

        adicionarItemMenu(
            "⌫   LIMPAR CACHE DAS CAPAS"
        ) {

            limparCacheCapas()
        }
    }


    private fun focarPrimeiroItemMenu() {

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }


    private fun focarUltimoItemMenu() {

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[
                itensMenuFoco.size - 1
            ].requestFocus()
        }
    }


    private fun atualizarFocoMenu(
        direcao: Int
    ) {

        if (
            itensMenuFoco.isEmpty()
        ) {
            return
        }

        var atual =
            -1

        val foco =
            currentFocus

        if (foco != null) {

            atual =
                itensMenuFoco.indexOf(
                    foco
                )
        }

        if (atual < 0) {

            focarPrimeiroItemMenu()

            return
        }

        var novo =
            atual + direcao

        if (
            novo < 0
        ) {

            novo =
                itensMenuFoco.size - 1
        }

        if (
            novo >= itensMenuFoco.size
        ) {

            novo = 0
        }

        itensMenuFoco[
            novo
        ].requestFocus()
    }


    private fun voltarParaConteudoPrincipal() {

        if (menuAberto) {

            fecharMenu()

            return
        }

        if (
            historicoConteudo.isNotEmpty()
        ) {

            voltarConteudo()

            return
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            indiceCardAtual = 0

            cardsAtuais[0]
                .requestFocus()
        }
    }    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action != KeyEvent.ACTION_DOWN
        ) {
            return super.dispatchKeyEvent(
                event
            )
        }

        if (menuAberto) {

            return tratarTeclasMenu(
                event.keyCode
            )
        }

        return tratarTeclasConteudo(
            event.keyCode
        )
    }


    private fun tratarTeclasMenu(
        codigo: Int
    ): Boolean {

        when (codigo) {

            KeyEvent.KEYCODE_DPAD_UP -> {

                atualizarFocoMenu(
                    -1
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                atualizarFocoMenu(
                    1
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                fecharMenu()

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                val foco =
                    currentFocus

                if (
                    foco != null
                ) {

                    foco.performClick()
                }

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                val foco =
                    currentFocus

                if (
                    foco != null
                ) {

                    foco.performClick()
                }

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                fecharMenu()

                return true
            }
        }

        return false
    }


    private fun tratarTeclasConteudo(
        codigo: Int
    ): Boolean {

        when (codigo) {

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                moverCard(
                    -1
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                moverCard(
                    1
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {

                moverCard(
                    -5
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                moverCard(
                    5
                )

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                val foco =
                    currentFocus

                if (
                    foco != null &&
                    cardsAtuais.contains(
                        foco
                    )
                ) {

                    foco.performClick()
                }

                return true
            }

            KeyEvent.KEYCODE_MENU -> {

                abrirMenu()

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                voltarParaConteudoPrincipal()

                return true
            }
        }

        return false
    }


    private fun moverCard(
        deslocamento: Int
    ) {

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        val foco =
            currentFocus

        if (foco != null) {

            val posicao =
                cardsAtuais.indexOf(
                    foco
                )

            if (posicao >= 0) {

                indiceCardAtual =
                    posicao
            }
        }

        var novoIndice =
            indiceCardAtual +
                deslocamento

        if (
            novoIndice < 0
        ) {

            novoIndice = 0
        }

        if (
            novoIndice >=
            cardsAtuais.size
        ) {

            novoIndice =
                cardsAtuais.size - 1
        }

        indiceCardAtual =
            novoIndice

        val novoFoco =
            cardsAtuais[
                indiceCardAtual
            ]

        novoFoco.requestFocus()

        rolarParaFoco(
            novoFoco
        )
    }


    private fun rolarParaFoco(
        view: View
    ) {

        view.post {

            scroll.smoothScrollTo(
                0,
                calcularPosicaoScroll(
                    view
                )
            )
        }
    }


    private fun calcularPosicaoScroll(
        view: View
    ): Int {

        var atual =
            view

        var distancia =
            0

        while (
            atual.parent is View &&
            atual !== scroll
        ) {

            distancia +=
                atual.top

            atual =
                atual.parent as View
        }

        val alturaTela =
            scroll.height

        val metadeTela =
            alturaTela / 2

        return (
            distancia -
                metadeTela +
                view.height
        ).coerceAtLeast(
            0
        )
    }    private fun sincronizarIndiceComFoco() {

        val foco =
            currentFocus
                ?: return

        val indice =
            cardsAtuais.indexOf(
                foco
            )

        if (indice >= 0) {

            indiceCardAtual =
                indice
        }
    }


    private fun garantirFocoValido() {

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        sincronizarIndiceComFoco()

        if (
            indiceCardAtual < 0
        ) {

            indiceCardAtual = 0
        }

        if (
            indiceCardAtual >=
            cardsAtuais.size
        ) {

            indiceCardAtual =
                cardsAtuais.size - 1
        }

        val foco =
            cardsAtuais[
                indiceCardAtual
            ]

        foco.isFocusable = true

        foco.isFocusableInTouchMode =
            true

        foco.requestFocus()
    }


    private fun configurarFocoCard(
        card: View
    ) {

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.setOnFocusChangeListener {
                view,
                focado ->

            if (focado) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        view
                    ).coerceAtLeast(0)

                view.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(120)
                    .start()

                rolarParaFoco(
                    view
                )

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }
    }


    private fun solicitarFocoInicial() {

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        indiceCardAtual = 0

        cardsAtuais[0].post {

            cardsAtuais[0]
                .requestFocus()
        }
    }


    private fun voltarAoTopo() {

        scroll.post {

            scroll.smoothScrollTo(
                0,
                0
            )
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            indiceCardAtual = 0

            cardsAtuais[0]
                .post {

                    cardsAtuais[0]
                        .requestFocus()
                }
        }
    }


    private fun selecionarCardAtual() {

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        garantirFocoValido()

        val foco =
            cardsAtuais[
                indiceCardAtual
            ]

        foco.performClick()
    }


    private fun abrirMenuComDpad() {

        if (
            menuAberto
        ) {
            return
        }

        abrirMenu()

        focarPrimeiroItemMenu()
    }


    private fun fecharMenuComDpad() {

        if (
            !menuAberto
        ) {
            return
        }

        fecharMenu()

        garantirFocoValido()
    }    private fun tratarTeclaVoltar(): Boolean {

        if (menuAberto) {

            fecharMenu()

            return true
        }

        if (
            historicoConteudo.isNotEmpty()
        ) {

            voltarConteudo()

            return true
        }

        return false
    }


    private fun voltarUmaTela() {

        if (menuAberto) {

            fecharMenu()

            return
        }

        if (
            historicoConteudo.isNotEmpty()
        ) {

            voltarConteudo()

            return
        }

        mostrarListaFilmes(
            "Todos"
        )
    }


    private fun atualizarFocoDepoisDaTela() {

        conteudo.post {

            if (
                cardsAtuais.isNotEmpty()
            ) {

                indiceCardAtual =
                    indiceCardAtual.coerceIn(
                        0,
                        cardsAtuais.size - 1
                    )

                cardsAtuais[
                    indiceCardAtual
                ].requestFocus()

            } else {

                botaoMenu.requestFocus()
            }
        }
    }


    private fun limparFocoAtual() {

        currentFocus?.clearFocus()
    }


    private fun focarMenuOuConteudo() {

        if (menuAberto) {

            focarPrimeiroItemMenu()

        } else {

            garantirFocoValido()
        }
    }


    private fun atualizarFocoAposCarregamento() {

        conteudo.post {

            if (
                cardsAtuais.isNotEmpty()
            ) {

                indiceCardAtual = 0

                cardsAtuais[0]
                    .requestFocus()

            } else {

                botaoMenu.requestFocus()
            }
        }
    }


    private fun fecharMenuEManterFoco() {

        if (!menuAberto) {
            return
        }

        fecharMenu()

        conteudo.post {

            garantirFocoValido()
        }
    }    override fun onResume() {

        super.onResume()

        if (
            !menuAberto &&
            cardsAtuais.isNotEmpty()
        ) {

            conteudo.post {

                if (
                    indiceCardAtual >= 0 &&
                    indiceCardAtual < cardsAtuais.size
                ) {

                    cardsAtuais[
                        indiceCardAtual
                    ].requestFocus()
                }
            }
        }
    }


    override fun onPause() {

        super.onPause()

        currentFocus?.clearFocus()
    }


    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (
            hasFocus &&
            !menuAberto
        ) {

            conteudo.post {

                if (
                    cardsAtuais.isNotEmpty()
                ) {

                    indiceCardAtual =
                        indiceCardAtual.coerceIn(
                            0,
                            cardsAtuais.size - 1
                        )

                    cardsAtuais[
                        indiceCardAtual
                    ].requestFocus()
                }
            }
        }
    }


    private fun atualizarConteudoAtual() {

        when {

            categoriaAtual == "Favoritos" -> {

                mostrarFavoritos()
            }

            categoriaAtual ==
                "Continue Assistindo" -> {

                mostrarHistorico()
            }

            else -> {

                mostrarListaFilmes(
                    categoriaAtual
                )
            }
        }
    }


    private fun restaurarFocoConteudo() {

        if (
            menuAberto
        ) {
            return
        }

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        indiceCardAtual =
            indiceCardAtual.coerceIn(
                0,
                cardsAtuais.size - 1
            )

        cardsAtuais[
            indiceCardAtual
        ].post {

            cardsAtuais[
                indiceCardAtual
            ].requestFocus()
        }
    }


    private fun atualizarDepoisDeVoltar() {

        conteudo.post {

            if (
                cardsAtuais.isNotEmpty()
            ) {

                indiceCardAtual =
                    indiceCardAtual.coerceIn(
                        0,
                        cardsAtuais.size - 1
                    )

                cardsAtuais[
                    indiceCardAtual
                ].requestFocus()

                rolarParaFoco(
                    cardsAtuais[
                        indiceCardAtual
                    ]
                )

            } else {

                botaoMenu.requestFocus()
            }
        }
    }    private fun mostrarTelaInicial() {

        historicoConteudo.clear()

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        categoriaAtual =
            "Todos"

        if (
            filmes.isEmpty()
        ) {

            val mensagem =
                TextView(this)

            mensagem.text =
                "Carregando catálogo..."

            mensagem.textSize =
                18f

            mensagem.setTextColor(
                Color.WHITE
            )

            mensagem.gravity =
                Gravity.CENTER

            conteudo.addView(
                mensagem,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(120)
                )
            )

            return
        }

        mostrarListaFilmes(
            "Todos"
        )
    }


    private fun atualizarTelaPrincipal() {

        runOnUiThread {

            if (
                filmes.isEmpty() &&
                series.isEmpty() &&
                doramas.isEmpty() &&
                animes.isEmpty()
            ) {

                return@runOnUiThread
            }

            if (
                categoriaAtual == "Favoritos"
            ) {

                mostrarFavoritos()

                return@runOnUiThread
            }

            if (
                categoriaAtual ==
                "Continue Assistindo"
            ) {

                mostrarHistorico()

                return@runOnUiThread
            }

            mostrarListaFilmes(
                categoriaAtual
            )
        }
    }


    private fun mostrarMensagem(
        texto: String
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val mensagem =
            TextView(this)

        mensagem.text =
            texto

        mensagem.textSize =
            18f

        mensagem.setTextColor(
            Color.WHITE
        )

        mensagem.gravity =
            Gravity.CENTER

        mensagem.setPadding(
            dp(20),
            dp(40),
            dp(20),
            dp(40)
        )

        conteudo.addView(
            mensagem,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        botaoMenu.requestFocus()
    }


    private fun mostrarErroCatalogo(
        mensagemErro: String
    ) {

        runOnUiThread {

            conteudo.removeAllViews()

            cardsAtuais.clear()

            indiceCardAtual = 0

            val mensagem =
                TextView(this)

            mensagem.text =
                "Não foi possível carregar o catálogo.\n\n" +
                    mensagemErro

            mensagem.textSize =
                17f

            mensagem.setTextColor(
                Color.WHITE
            )

            mensagem.gravity =
                Gravity.CENTER

            mensagem.setPadding(
                dp(25),
                dp(50),
                dp(25),
                dp(50)
            )

            conteudo.addView(
                mensagem,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            botaoMenu.requestFocus()
        }
    }


    private fun executarNoUi(
        acao: () -> Unit
    ) {

        if (
            isFinishing ||
            isDestroyed
        ) {
            return
        }

        runOnUiThread {

            if (
                !isFinishing &&
                !isDestroyed
            ) {

                acao()
            }
        }
    }    private fun fecharTecladoSeExistir() {

        val foco =
            currentFocus

        foco?.clearFocus()
    }


    private fun limparTelaAntesDeCarregar() {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0
    }


    private fun adicionarEspacoVertical(
        tamanho: Int
    ) {

        val espaco =
            View(this)

        conteudo.addView(
            espaco,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(tamanho)
            )
        )
    }


    private fun criarLinhaSeparadora(): View {

        val linha =
            View(this)

        linha.setBackgroundColor(
            Color.argb(
                70,
                255,
                255,
                255
            )
        )

        return linha
    }


    private fun adicionarLinhaSeparadora() {

        conteudo.addView(
            criarLinhaSeparadora(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {

                topMargin =
                    dp(8)

                bottomMargin =
                    dp(8)
            }
        )
    }


    private fun mostrarToast(
        mensagem: String
    ) {

        Toast.makeText(
            this,
            mensagem,
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun textoSeguro(
        texto: String?
    ): String {

        return texto
            ?.trim()
            ?.takeIf {
                it.isNotEmpty()
            }
            ?: "Sem informação"
    }


    private fun fecharMenuAoClicarFora() {

        if (
            menuAberto
        ) {

            fecharMenu()
        }
    }


    private fun voltarParaInicio() {

        historicoConteudo.clear()

        categoriaAtual =
            "Todos"

        mostrarListaFilmes(
            "Todos"
        )
    }


    private fun limparTudoVisual() {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        currentFocus?.clearFocus()
    }    private fun fecharAplicativo() {

        try {

            finish()

        } catch (
            erro: Exception
        ) {

            finishAffinity()
        }
    }


    private fun confirmarSaida() {

        fecharAplicativo()
    }


    private fun verificarCatalogoDisponivel(): Boolean {

        return filmes.isNotEmpty() ||
            series.isNotEmpty() ||
            doramas.isNotEmpty() ||
            animes.isNotEmpty()
    }


    private fun selecionarPrimeiroCard() {

        if (
            cardsAtuais.isEmpty()
        ) {

            return
        }

        indiceCardAtual = 0

        cardsAtuais[0].post {

            cardsAtuais[0]
                .requestFocus()

            rolarParaFoco(
                cardsAtuais[0]
            )
        }
    }


    private fun selecionarUltimoCard() {

        if (
            cardsAtuais.isEmpty()
        ) {

            return
        }

        indiceCardAtual =
            cardsAtuais.size - 1

        cardsAtuais[
            indiceCardAtual
        ].post {

            cardsAtuais[
                indiceCardAtual
            ].requestFocus()

            rolarParaFoco(
                cardsAtuais[
                    indiceCardAtual
                ]
            )
        }
    }


    private fun reconstruirFoco() {

        if (menuAberto) {

            focarPrimeiroItemMenu()

            return
        }

        selecionarPrimeiroCard()
    }    private fun manterFocoNoCardAtual() {

        if (
            menuAberto ||
            cardsAtuais.isEmpty()
        ) {
            return
        }

        indiceCardAtual =
            indiceCardAtual.coerceIn(
                0,
                cardsAtuais.size - 1
            )

        val card =
            cardsAtuais[
                indiceCardAtual
            ]

        card.post {

            if (
                !menuAberto
            ) {

                card.requestFocus()

                rolarParaFoco(
                    card
                )
            }
        }
    }


    private fun atualizarIndiceCard() {

        val foco =
            currentFocus
                ?: return

        val indice =
            cardsAtuais.indexOf(
                foco
            )

        if (
            indice >= 0
        ) {

            indiceCardAtual =
                indice
        }
    }


    private fun existeConteudo(): Boolean {

        return filmes.isNotEmpty() ||
            series.isNotEmpty() ||
            doramas.isNotEmpty() ||
            animes.isNotEmpty()
    }


    private fun quantidadeCards(): Int {

        return cardsAtuais.size
    }


    private fun indiceValido(
        indice: Int
    ): Boolean {

        return indice >= 0 &&
            indice < cardsAtuais.size
    }


    private fun obterCardAtual(): View? {

        if (
            !indiceValido(
                indiceCardAtual
            )
        ) {
            return null
        }

        return cardsAtuais[
            indiceCardAtual
        ]
    }


    private fun focarCard(
        indice: Int
    ) {

        if (
            !indiceValido(indice)
        ) {
            return
        }

        indiceCardAtual =
            indice

        val card =
            cardsAtuais[indice]

        card.post {

            card.requestFocus()

            rolarParaFoco(
                card
            )
        }
    }    private fun voltarAoInicioDoCatalogo() {

        historicoConteudo.clear()

        categoriaAtual =
            "Todos"

        mostrarListaFilmes(
            "Todos"
        )
    }


    private fun garantirTelaValida() {

        if (
            conteudo.childCount == 0 &&
            existeConteudo()
        ) {

            mostrarListaFilmes(
                "Todos"
            )
        }
    }


    private fun atualizarTelaDepoisDoMenu() {

        if (menuAberto) {
            return
        }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            manterFocoNoCardAtual()

        } else {

            botaoMenu.requestFocus()
        }
    }


    private fun prepararTelaFinal() {

        raiz.post {

            if (
                !menuAberto &&
                cardsAtuais.isNotEmpty()
            ) {

                manterFocoNoCardAtual()
            }
        }
    }


    override fun onDestroy() {

        cacheCapas.clear()

        capasEmCarregamento.clear()

        super.onDestroy()
    }
}
