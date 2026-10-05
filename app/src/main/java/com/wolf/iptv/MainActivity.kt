package com.wolf.iptv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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

class MainActivity : Activity() {

    companion object {

        private const val CATALOGO_URL =
            "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/refs/heads/main/catalogo.json"
    }

    private lateinit var conteudo: LinearLayout
    private lateinit var scroll: ScrollView

    private val cacheCapas =
        HashMap<String, Bitmap>()

    private val cacheDiscoCapas by lazy {
        File(cacheDir, "capas").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val filmes =
        ArrayList<Filme>()

    private val series =
        ArrayList<Serie>()

    private var menuAberto = false

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

        criarInterface()

        carregarFilmes()
        carregarSeries()
    }

    private fun criarInterface() {

        val raiz =
            LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.BLACK
        )

        val camada =
            android.widget.FrameLayout(this)

        val fundo =
            ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.FIT_CENTER

        fundo.alpha =
            0.55f

        fundo.setImageResource(
            R.drawable.wolf_background
        )

        camada.addView(
            fundo,
            android.widget.FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        conteudo =
            LinearLayout(this)

        conteudo.orientation =
            LinearLayout.VERTICAL

        conteudo.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        camada.addView(
            conteudo,
            android.widget.FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        raiz.addView(
            camada,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(
            raiz
        )

        criarMenu()
    }    private fun criarMenu() {

        val menu =
            LinearLayout(this)

        menu.orientation =
            LinearLayout.HORIZONTAL

        menu.gravity =
            Gravity.CENTER_VERTICAL

        menu.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8)
        )

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF IPTV"

        titulo.textSize =
            25f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        menu.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )

        val botaoMenu =
            TextView(this)

        botaoMenu.text =
            "☰"

        botaoMenu.textSize =
            32f

        botaoMenu.setTextColor(
            Color.WHITE
        )

        botaoMenu.gravity =
            Gravity.CENTER

        botaoMenu.isFocusable =
            true

        botaoMenu.isFocusableInTouchMode =
            true

        botaoMenu.setPadding(
            dp(20),
            0,
            dp(20),
            0
        )

        botaoMenu.background =
            criarFundoBotao()

        botaoMenu.setOnClickListener {

            abrirMenuLateral()
        }

        botaoMenu.setOnKeyListener {
                _, tecla, evento ->

            if (
                evento.action ==
                KeyEvent.ACTION_DOWN &&
                tecla ==
                KeyEvent.KEYCODE_ENTER
            ) {

                abrirMenuLateral()

                true

            } else {

                false
            }
        }

        menu.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(80),
                dp(60)
            )
        )

        conteudo.addView(
            menu,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        criarAbas()
    }

    private fun criarAbas() {

        val abas =
            LinearLayout(this)

        abas.orientation =
            LinearLayout.HORIZONTAL

        abas.gravity =
            Gravity.CENTER

        val filmes =
            criarBotaoAba(
                "FILMES"
            )

        val series =
            criarBotaoAba(
                "SÉRIES"
            )

        val doramas =
            criarBotaoAba(
                "DORAMAS"
            )

        abas.addView(
            filmes,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        abas.addView(
            series,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        abas.addView(
            doramas,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        conteudo.addView(
            abas,
            LinearLayout.LayoutParams(
                -1,
                dp(65)
            )
        )

        filmes.setOnClickListener {

            mostrarListaFilmes(
                filmes
            )
        }

        series.setOnClickListener {

            mostrarListaSeries(
                series
            )
        }

        doramas.setOnClickListener {

            mostrarDoramas()
        }
    }

    private fun criarBotaoAba(
        texto: String
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            texto

        botao.textSize =
            17f

        botao.gravity =
            Gravity.CENTER

        botao.setTextColor(
            Color.WHITE
        )

        botao.isFocusable =
            true

        botao.isFocusableInTouchMode =
            true

        botao.background =
            criarFundoBotao()

        return botao
    }

    private fun criarFundoBotao():
        GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.cornerRadius =
            dp(10).toFloat()

        fundo.setColor(
            Color.argb(
                150,
                30,
                30,
                30
            )
        )

        fundo.setStroke(
            dp(1),
            Color.argb(
                100,
                255,
                255,
                255
            )
        )

        return fundo
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
            resources.displayMetrics.density
        ).toInt()
    }    private fun abrirMenuLateral() {

        if (menuAberto) {
            fecharMenuLateral()
            return
        }

        menuAberto = true

        val menu =
            LinearLayout(this)

        menu.orientation =
            LinearLayout.VERTICAL

        menu.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        menu.setBackgroundColor(
            Color.argb(
                245,
                10,
                10,
                10
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            "MENU"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        menu.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            )
        )

        adicionarItemMenu(
            menu,
            "★  Favoritos"
        ) {
            mostrarFavoritos()
        }

        adicionarItemMenu(
            menu,
            "▶  Continue assistindo"
        ) {
            mostrarContinueAssistindo()
        }

        adicionarItemMenu(
            menu,
            "🎬  Filmes"
        ) {
            mostrarListaFilmes(
                "Todos"
            )
        }

        adicionarItemMenu(
            menu,
            "📺  Séries"
        ) {
            mostrarListaSeries(
                series
            )
        }

        adicionarItemMenu(
            menu,
            "🎭  Doramas"
        ) {
            mostrarDoramas()
        }

        adicionarItemMenu(
            menu,
            "🍥  Anime"
        ) {
            mostrarAnimes()
        }

        val janela =
            android.app.Dialog(this)

        janela.setContentView(
            menu
        )

        val largura =
            (resources.displayMetrics.widthPixels * 0.40f).toInt()

        janela.window?.setLayout(
            largura,
            -1
        )

        janela.window?.setGravity(
            Gravity.END
        )

        janela.setOnDismissListener {
            menuAberto = false
        }

        janela.show()

        janela.window?.setLayout(
            largura,
            -1
        )

        menu.requestFocus()
    }

    private fun adicionarItemMenu(
        menu: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            18f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(15),
            0,
            dp(15),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true

        item.background =
            criarFundoBotao()

        item.setOnClickListener {
            acao()
        }

        item.setOnKeyListener {
                _, tecla, evento ->

            if (
                evento.action ==
                KeyEvent.ACTION_DOWN &&
                (
                    tecla ==
                    KeyEvent.KEYCODE_ENTER ||
                    tecla ==
                    KeyEvent.KEYCODE_DPAD_CENTER
                )
            ) {

                acao()

                true

            } else {

                false
            }
        }

        menu.addView(
            item,
            LinearLayout.LayoutParams(
                -1,
                dp(58)
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

    private fun fecharMenuLateral() {
        menuAberto = false
    }

    private fun mostrarFavoritos() {

        Toast.makeText(
            this,
            "Favoritos",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun mostrarContinueAssistindo() {

        Toast.makeText(
            this,
            "Continue assistindo",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun mostrarAnimes() {

        Toast.makeText(
            this,
            "Anime",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun mostrarDoramas() {

        Toast.makeText(
            this,
            "Doramas",
            Toast.LENGTH_SHORT
        ).show()
    }    private fun carregarFilmes() {

        thread {

            try {

                val conexao =
                    URL(CATALOGO_URL)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    15000

                conexao.readTimeout =
                    15000

                conexao.requestMethod =
                    "GET"

                conexao.connect()

                val texto =
                    conexao.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                conexao.disconnect()

                val json =
                    JSONObject(texto)

                val array =
                    json.optJSONArray(
                        "filmes"
                    )

                if (array != null) {

                    val resultado =
                        ArrayList<Filme>()

                    for (
                        i in 0 until array.length()
                    ) {

                        val item =
                            array.optJSONObject(i)
                                ?: continue

                        resultado.add(
                            Filme(
                                titulo =
                                    item.optString(
                                        "titulo"
                                    ),

                                ano =
                                    item.optInt(
                                        "ano"
                                    ),

                                categoria =
                                    item.optString(
                                        "categoria"
                                    ),

                                capa =
                                    item.optString(
                                        "capa"
                                    ),

                                video =
                                    item.optString(
                                        "video"
                                    )
                            )
                        )
                    }

                    synchronized(
                        filmes
                    ) {

                        filmes.clear()

                        filmes.addAll(
                            resultado
                        )
                    }

                    runOnUiThread {

                        mostrarListaFilmes(
                            "Todos"
                        )
                    }
                }

            } catch (
                erro: Exception
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao carregar filmes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarListaFilmes(
        categoria: Any
    ) {

        val lista =
            synchronized(filmes) {
                ArrayList(filmes)
            }

        val filtrados =
            if (
                categoria is String &&
                categoria != "Todos"
            ) {

                lista.filter {
                    it.categoria.equals(
                        categoria,
                        ignoreCase = true
                    )
                }

            } else {

                lista
            }

        val area =
            LinearLayout(this)

        area.orientation =
            LinearLayout.VERTICAL

        area.setPadding(
            0,
            dp(15),
            0,
            dp(20)
        )

        val titulo =
            TextView(this)

        titulo.text =
            if (
                categoria is String &&
                categoria != "Todos"
            ) {
                categoria
            } else {
                "FILMES"
            }

        titulo.textSize =
            23f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setPadding(
            dp(5),
            dp(5),
            0,
            dp(10)
        )

        area.addView(
            titulo,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )

        val grade =
            android.widget.GridLayout(this)

        grade.columnCount =
            5

        grade.rowCount =
            android.widget.GridLayout.UNDEFINED

        for (
            filme in filtrados
        ) {

            val card =
                criarCardFilme(
                    filme
                )

            grade.addView(
                card,
                android.widget.GridLayout.LayoutParams().apply {

                    width =
                        dp(180)

                    height =
                        dp(255)

                    setMargins(
                        dp(7),
                        dp(7),
                        dp(7),
                        dp(7)
                    )
                }
            )
        }

        area.addView(
            grade,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        conteudo.removeViews(
            1,
            conteudo.childCount - 1
        )

        conteudo.addView(
            area,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )
    }

    private fun criarCardFilme(
        filme: Filme
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.background =
            criarFundoCard()

        val capa =
            ImageView(this)

        capa.scaleType =
            ImageView.ScaleType.CENTER_CROP

        capa.setBackgroundColor(
            Color.DKGRAY
        )

        capa.tag =
            filme.capa

        card.addView(
            capa,
            LinearLayout.LayoutParams(
                -1,
                dp(205)
            )
        )

        carregarImagem(
            filme.capa,
            capa
        )

        val nome =
            TextView(this)

        nome.text =
            filme.titulo

        nome.textSize =
            14f

        nome.setTextColor(
            Color.WHITE
        )

        nome.gravity =
            Gravity.CENTER

        nome.maxLines =
            2

        nome.setPadding(
            dp(5),
            dp(5),
            dp(5),
            dp(5)
        )

        card.addView(
            nome,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )

        card.setOnClickListener {

            abrirFilme(
                filme
            )
        }

        card.setOnKeyListener {
                _, tecla, evento ->

            if (
                evento.action ==
                KeyEvent.ACTION_DOWN &&
                (
                    tecla ==
                    KeyEvent.KEYCODE_ENTER ||
                    tecla ==
                    KeyEvent.KEYCODE_DPAD_CENTER
                )
            ) {

                abrirFilme(
                    filme
                )

                true

            } else {

                false
            }
        }

        return card
    }

    private fun criarFundoCard():
        GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.cornerRadius =
            dp(10).toFloat()

        fundo.setColor(
            Color.argb(
                180,
                20,
                20,
                20
            )
        )

        fundo.setStroke(
            dp(1),
            Color.argb(
                100,
                255,
                255,
                255
            )
        )

        return fundo
        }    private fun carregarSeries() {

        thread {

            try {

                val conexao =
                    URL(CATALOGO_URL)
                        .openConnection()
                            as HttpURLConnection

                conexao.connectTimeout =
                    15000

                conexao.readTimeout =
                    15000

                conexao.requestMethod =
                    "GET"

                conexao.connect()

                val texto =
                    conexao.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                conexao.disconnect()

                val json =
                    JSONObject(texto)

                val resultado =
                    ArrayList<Serie>()

                val arraySeries =
                    json.optJSONArray(
                        "series"
                    )

                val arrayDoramas =
                    json.optJSONArray(
                        "doramas"
                    )

                val arrayAnimes =
                    json.optJSONArray(
                        "animes"
                    )

                fun lerSeries(
                    array: JSONArray?
                ): ArrayList<Serie> {

                    val lista =
                        ArrayList<Serie>()

                    if (array == null) {
                        return lista
                    }

                    for (
                        i in 0 until array.length()
                    ) {

                        val item =
                            array.optJSONObject(i)
                                ?: continue

                        val temporadas =
                            ArrayList<Temporada>()

                        val arrayTemporadas =
                            item.optJSONArray(
                                "temporadas"
                            )

                        if (
                            arrayTemporadas != null
                        ) {

                            for (
                                j in 0 until
                                arrayTemporadas.length()
                            ) {

                                val temporadaObj =
                                    arrayTemporadas
                                        .optJSONObject(j)
                                        ?: continue

                                val episodios =
                                    ArrayList<Episodio>()

                                val arrayEpisodios =
                                    temporadaObj.optJSONArray(
                                        "episodios"
                                    )

                                if (
                                    arrayEpisodios != null
                                ) {

                                    for (
                                        k in 0 until
                                        arrayEpisodios.length()
                                    ) {

                                        val episodioObj =
                                            arrayEpisodios
                                                .optJSONObject(k)
                                                ?: continue

                                        episodios.add(
                                            Episodio(
                                                numero =
                                                    episodioObj
                                                        .optInt(
                                                            "numero"
                                                        ),

                                                titulo =
                                                    episodioObj
                                                        .optString(
                                                            "titulo"
                                                        ),

                                                video =
                                                    episodioObj
                                                        .optString(
                                                            "video"
                                                        )
                                            )
                                        )
                                    }
                                }

                                temporadas.add(
                                    Temporada(
                                        numero =
                                            temporadaObj
                                                .optInt(
                                                    "numero"
                                                ),

                                        episodios =
                                            episodios
                                    )
                                )
                            }
                        }

                        lista.add(
                            Serie(
                                titulo =
                                    item.optString(
                                        "titulo"
                                    ),

                                ano =
                                    item.optInt(
                                        "ano"
                                    ),

                                categoria =
                                    item.optString(
                                        "categoria"
                                    ),

                                capa =
                                    item.optString(
                                        "capa"
                                    ),

                                temporadas =
                                    temporadas
                            )
                        )
                    }

                    return lista
                }

                val listaSeries =
                    lerSeries(
                        arraySeries
                    )

                val listaDoramas =
                    lerSeries(
                        arrayDoramas
                    )

                val listaAnimes =
                    lerSeries(
                        arrayAnimes
                    )

                synchronized(
                    series
                ) {

                    series.clear()

                    series.addAll(
                        listaSeries
                    )
                }

                synchronized(
                    doramas
                ) {

                    doramas.clear()

                    doramas.addAll(
                        listaDoramas
                    )
                }

                synchronized(
                    animes
                ) {

                    animes.clear()

                    animes.addAll(
                        listaAnimes
                    )
                }

                runOnUiThread {

                    mostrarListaSeries(
                        series
                    )
                }

            } catch (
                erro: Exception
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao carregar séries",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarListaSeries(
        lista: List<Serie>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual =
            0

        if (
            lista.isEmpty()
        ) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhuma série encontrada"

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.textSize =
                20f

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(100)
                )
            )

            return
        }

        lista.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { serie ->

                    val card =
                        criarCardSerie(
                            serie
                        )

                    linha.addView(
                        card,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).apply {

                            leftMargin =
                                dp(4)

                            rightMargin =
                                dp(4)

                            bottomMargin =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(
                        card
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }

    private fun criarCardSerie(
        serie: Serie
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        card.isClickable =
            true

        card.background =
            criarFundoCard(
                false
            )

        val imagem =
            ImageView(this)

        imagem.tag =
            serie.capa

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            serie.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            )
        )

        val informacoes =
            LinearLayout(this)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.gravity =
            Gravity.CENTER_VERTICAL

        informacoes.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informacoes.setBackgroundColor(
            Color.argb(
                235,
                10,
                10,
                10
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize =
            14f

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.maxLines =
            1

        titulo.ellipsize =
            TextUtils.TruncateAt.END

        informacoes.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val detalhes =
            TextView(this)

        detalhes.text =
            "${serie.categoria} • ${serie.temporadas.size} temporada(s)"

        detalhes.setTextColor(
            Color.LTGRAY
        )

        detalhes.textSize =
            12f

        informacoes.addView(
            detalhes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).apply {

                gravity =
                    Gravity.BOTTOM
            }
        )

        val borda =
            View(this)

        borda.background =
            criarBordaVermelha()

        borda.visibility =
            View.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
                _, foco ->

            borda.visibility =
                if (foco) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            card.background =
                criarFundoCard(
                    foco
                )

            if (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(
                        card
                    )
            }
        }

        card.setOnClickListener {

            historicoConteudo.add {

                mostrarListaSeries(
                    series
                )
            }

            mostrarTemporadas(
                serie
            )
        }

        return card
    }    private fun mostrarTemporadas(
        serie: Serie
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual =
            0

        val titulo =
            TextView(this)

        titulo.text =
            serie.titulo

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        val subtitulo =
            TextView(this)

        subtitulo.text =
            "${serie.ano} • ${serie.categoria}"

        subtitulo.textSize =
            15f

        subtitulo.setTextColor(
            Color.LTGRAY
        )

        subtitulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            subtitulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        for (
            temporada in serie.temporadas
        ) {

            val botao =
                TextView(this)

            botao.text =
                "Temporada ${temporada.numero}"

            botao.textSize =
                18f

            botao.setTextColor(
                Color.WHITE
            )

            botao.gravity =
                Gravity.CENTER_VERTICAL

            botao.setPadding(
                dp(20),
                0,
                dp(20),
                0
            )

            botao.isFocusable =
                true

            botao.isFocusableInTouchMode =
                true

            botao.background =
                criarFundoBotao()

            botao.setOnClickListener {

                mostrarEpisodios(
                    serie,
                    temporada
                )
            }

            botao.setOnKeyListener {
                    _, tecla, evento ->

                if (
                    evento.action ==
                    KeyEvent.ACTION_DOWN &&
                    (
                        tecla ==
                        KeyEvent.KEYCODE_ENTER ||
                        tecla ==
                        KeyEvent.KEYCODE_DPAD_CENTER
                    )
                ) {

                    mostrarEpisodios(
                        serie,
                        temporada
                    )

                    true

                } else {

                    false
                }
            }

            conteudo.addView(
                botao,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(65)
                ).apply {

                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }
    }

    private fun mostrarEpisodios(
        serie: Serie,
        temporada: Temporada
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual =
            0

        val titulo =
            TextView(this)

        titulo.text =
            "${serie.titulo} - Temporada ${temporada.numero}"

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        val episodios =
            temporada.episodios

        episodios.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(95)
                    )

                grupo.forEach { episodio ->

                    val botao =
                        TextView(this)

                    botao.text =
                        "EP ${episodio.numero}\n${episodio.titulo}"

                    botao.textSize =
                        14f

                    botao.setTextColor(
                        Color.WHITE
                    )

                    botao.gravity =
                        Gravity.CENTER

                    botao.maxLines =
                        2

                    botao.isFocusable =
                        true

                    botao.isFocusableInTouchMode =
                        true

                    botao.background =
                        criarFundoBotao()

                    botao.setOnClickListener {

                        abrirEpisodio(
                            serie,
                            temporada,
                            episodio
                        )
                    }

                    botao.setOnKeyListener {
                            _, tecla, evento ->

                        if (
                            evento.action ==
                            KeyEvent.ACTION_DOWN &&
                            (
                                tecla ==
                                KeyEvent.KEYCODE_ENTER ||
                                tecla ==
                                KeyEvent.KEYCODE_DPAD_CENTER
                            )
                        ) {

                            abrirEpisodio(
                                serie,
                                temporada,
                                episodio
                            )

                            true

                        } else {

                            false
                        }
                    }

                    linha.addView(
                        botao,
                        LinearLayout.LayoutParams(
                            0,
                            dp(80),
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

                    cardsAtuais.add(
                        botao
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }

    private fun abrirEpisodio(
        serie: Serie,
        temporada: Temporada,
        episodio: Episodio
    ) {

        val intent =
            android.content.Intent(
                this,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "titulo",
            "${serie.titulo} - EP ${episodio.numero}"
        )

        intent.putExtra(
            "video",
            episodio.video
        )

        startActivity(
            intent
        )
    }

    private fun abrirFilme(
        filme: Filme
    ) {

        val intent =
            android.content.Intent(
                this,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "titulo",
            filme.titulo
        )

        intent.putExtra(
            "video",
            filme.video
        )

        startActivity(
            intent
        )
    }    private fun criarFundoBotao(): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                Color.argb(
                    180,
                    30,
                    30,
                    30
                )
            )

            cornerRadius =
                dp(8).toFloat()

            setStroke(
                dp(1),
                Color.argb(
                    120,
                    255,
                    255,
                    255
                )
            )
        }
    }

    private fun abrirVideo(
        url: String,
        titulo: String,
        capa: String
    ) {

        if (url.isBlank()) {
            return
        }

        val intent =
            Intent(
                this,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "VIDEO_URL",
            url
        )

        intent.putExtra(
            "VIDEO_TITLE",
            titulo
        )

        intent.putExtra(
            "VIDEO_COVER",
            capa
        )

        startActivity(
            intent
        )
    }

    private fun criarCardEpisodio(
        serie: Serie,
        episodio: Episodio
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setBackgroundColor(
            Color.DKGRAY
        )

        carregarImagem(
            serie.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.gravity =
            Gravity.CENTER

        info.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        info.setBackgroundColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )

        val texto =
            TextView(this)

        texto.text =
            "EP ${episodio.numero}\n${episodio.titulo}"

        texto.textSize =
            14f

        texto.setTextColor(
            Color.WHITE
        )

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        texto.gravity =
            Gravity.CENTER

        texto.maxLines =
            2

        texto.ellipsize =
            TextUtils.TruncateAt.END

        info.addView(
            texto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        card.addView(
            info,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(80),
                Gravity.BOTTOM
            )
        )

        card.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()

                setStroke(
                    dp(2),
                    Color.TRANSPARENT
                )
            }

        card.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setStroke(
                dp(3),
                if (focado) {
                    Color.RED
                } else {
                    Color.TRANSPARENT
                }
            )

            if (focado) {

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(120)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }

        card.setOnClickListener {

            abrirVideo(
                episodio.video,
                "${serie.titulo} - EP ${episodio.numero}",
                serie.capa
            )
        }

        return card
    }

    private fun criarCardTemporada(
        serie: Serie,
        temporada: Temporada
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setBackgroundColor(
            Color.DKGRAY
        )

        carregarImagem(
            serie.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val info =
            TextView(this)

        info.text =
            "Temporada ${temporada.numero}"

        info.textSize =
            17f

        info.setTextColor(
            Color.WHITE
        )

        info.setTypeface(
            null,
            Typeface.BOLD
        )

        info.gravity =
            Gravity.CENTER

        info.setBackgroundColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )

        card.addView(
            info,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65),
                Gravity.BOTTOM
            )
        )

        card.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()

                setStroke(
                    dp(2),
                    Color.TRANSPARENT
                )
            }

        card.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setStroke(
                dp(3),
                if (focado) {
                    Color.RED
                } else {
                    Color.TRANSPARENT
                }
            )

            if (focado) {

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(120)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }

        card.setOnClickListener {

            historicoConteudo.add(
                Runnable {
                    mostrarTemporadas(
                        serie
                    )
                }
            )

            mostrarEpisodios(
                serie,
                temporada
            )
        }

        return card
    }    private fun mostrarListaDoramas(
        lista: List<Dorama>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum dorama encontrado"

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            return
        }

        lista.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { dorama ->

                    val card =
                        criarCardDorama(
                            dorama
                        )

                    linha.addView(
                        card,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).apply {

                            leftMargin =
                                dp(4)

                            rightMargin =
                                dp(4)

                            bottomMargin =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(
                        card
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }

    private fun criarCardDorama(
        dorama: Dorama
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setBackgroundColor(
            Color.DKGRAY
        )

        carregarImagem(
            dorama.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.gravity =
            Gravity.CENTER

        info.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        info.setBackgroundColor(
            Color.argb(
                195,
                0,
                0,
                0
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            dorama.titulo

        titulo.textSize =
            15f

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

        info.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val detalhes =
            TextView(this)

        detalhes.text =
            "${dorama.ano} • ${dorama.categoria}"

        detalhes.textSize =
            12f

        detalhes.setTextColor(
            Color.LTGRAY
        )

        detalhes.gravity =
            Gravity.CENTER

        detalhes.maxLines =
            1

        info.addView(
            detalhes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dp(4)
            }
        )

        card.addView(
            info,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(82),
                Gravity.BOTTOM
            )
        )

        card.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()

                setStroke(
                    dp(2),
                    Color.TRANSPARENT
                )
        }

        card.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setStroke(
                dp(3),
                if (focado) {
                    Color.RED
                } else {
                    Color.TRANSPARENT
                }
            )

            if (focado) {

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(120)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }

        card.setOnClickListener {

            historicoConteudo.add(
                Runnable {
                    mostrarListaDoramas(
                        doramas
                    )
                }
            )

            mostrarTemporadasDorama(
                dorama
            )
        }

        return card
    }

    private fun mostrarTemporadasDorama(
        dorama: Dorama
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            dorama.titulo

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        dorama.temporadas.forEach {
            temporada ->

            val botao =
                TextView(this)

            botao.text =
                "Temporada ${temporada.numero}"

            botao.textSize =
                18f

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

            botao.setOnClickListener {

                mostrarEpisodiosDorama(
                    dorama,
                    temporada
                )
            }

            conteudo.addView(
                botao,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(65)
                ).apply {

                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }
    }

    private fun mostrarEpisodiosDorama(
        dorama: Dorama,
        temporada: TemporadaDorama
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "${dorama.titulo} - Temporada ${temporada.numero}"

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        temporada.episodios
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(95)
                    )

                grupo.forEach { episodio ->

                    val botao =
                        TextView(this)

                    botao.text =
                        "EP ${episodio.numero}\n${episodio.titulo}"

                    botao.textSize =
                        14f

                    botao.setTextColor(
                        Color.WHITE
                    )

                    botao.gravity =
                        Gravity.CENTER

                    botao.maxLines =
                        2

                    botao.isFocusable =
                        true

                    botao.isFocusableInTouchMode =
                        true

                    botao.background =
                        criarFundoBotao()

                    botao.setOnClickListener {

                        abrirVideo(
                            episodio.video,
                            "${dorama.titulo} - EP ${episodio.numero}",
                            dorama.capa
                        )
                    }

                    linha.addView(
                        botao,
                        LinearLayout.LayoutParams(
                            0,
                            dp(80),
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

                    cardsAtuais.add(
                        botao
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }    private fun mostrarListaAnimes(
        lista: List<Anime>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum anime encontrado"

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            return
        }

        lista.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { anime ->

                    val card =
                        criarCardAnime(
                            anime
                        )

                    linha.addView(
                        card,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).apply {

                            leftMargin =
                                dp(4)

                            rightMargin =
                                dp(4)

                            bottomMargin =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(
                        card
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }

    private fun criarCardAnime(
        anime: Anime
    ): View {

        val card =
            FrameLayout(this)

        card.isFocusable =
            true

        card.isFocusableInTouchMode =
            true

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.setBackgroundColor(
            Color.DKGRAY
        )

        carregarImagem(
            anime.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val info =
            LinearLayout(this)

        info.orientation =
            LinearLayout.VERTICAL

        info.gravity =
            Gravity.CENTER

        info.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        info.setBackgroundColor(
            Color.argb(
                195,
                0,
                0,
                0
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            anime.titulo

        titulo.textSize =
            15f

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

        info.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val detalhes =
            TextView(this)

        detalhes.text =
            "${anime.ano} • ${anime.categoria}"

        detalhes.textSize =
            12f

        detalhes.setTextColor(
            Color.LTGRAY
        )

        detalhes.gravity =
            Gravity.CENTER

        detalhes.maxLines =
            1

        info.addView(
            detalhes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dp(4)
            }
        )

        card.addView(
            info,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(82),
                Gravity.BOTTOM
            )
        )

        card.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()

                setStroke(
                    dp(2),
                    Color.TRANSPARENT
                )
            }

        card.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setStroke(
                dp(3),
                if (focado) {
                    Color.RED
                } else {
                    Color.TRANSPARENT
                }
            )

            if (focado) {

                view.animate()
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setDuration(120)
                    .start()

            } else {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
        }

        card.setOnClickListener {

            historicoConteudo.add(
                Runnable {
                    mostrarListaAnimes(
                        animes
                    )
                }
            )

            mostrarTemporadasAnime(
                anime
            )
        }

        return card
    }

    private fun mostrarTemporadasAnime(
        anime: Anime
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            anime.titulo

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        anime.temporadas.forEach {
            temporada ->

            val botao =
                TextView(this)

            botao.text =
                "Temporada ${temporada.numero}"

            botao.textSize =
                18f

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

            botao.setOnClickListener {

                mostrarEpisodiosAnime(
                    anime,
                    temporada
                )
            }

            conteudo.addView(
                botao,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(65)
                ).apply {

                    setMargins(
                        dp(5),
                        dp(5),
                        dp(5),
                        dp(5)
                    )
                }
            )
        }
    }

    private fun mostrarEpisodiosAnime(
        anime: Anime,
        temporada: TemporadaAnime
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "${anime.titulo} - Temporada ${temporada.numero}"

        titulo.textSize =
            22f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        temporada.episodios
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(95)
                    )

                grupo.forEach { episodio ->

                    val botao =
                        TextView(this)

                    botao.text =
                        "EP ${episodio.numero}\n${episodio.titulo}"

                    botao.textSize =
                        14f

                    botao.setTextColor(
                        Color.WHITE
                    )

                    botao.gravity =
                        Gravity.CENTER

                    botao.maxLines =
                        2

                    botao.isFocusable =
                        true

                    botao.isFocusableInTouchMode =
                        true

                    botao.background =
                        criarFundoBotao()

                    botao.setOnClickListener {

                        abrirVideo(
                            episodio.video,
                            "${anime.titulo} - EP ${episodio.numero}",
                            anime.capa
                        )
                    }

                    linha.addView(
                        botao,
                        LinearLayout.LayoutParams(
                            0,
                            dp(80),
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

                    cardsAtuais.add(
                        botao
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }    private fun abrirMenu() {

        if (menuAberto) {
            return
        }

        menuAberto = true

        menuLateral.visibility =
            View.VISIBLE

        menuLateral.alpha =
            0f

        menuLateral.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        menuLateral.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {

                menuLateral.visibility =
                    View.GONE
            }
            .start()

        if (
            cardsAtuais.isNotEmpty()
        ) {

            val indice =
                indiceCardAtual.coerceIn(
                    0,
                    cardsAtuais.size - 1
                )

            cardsAtuais[indice]
                .requestFocus()
        }
    }

    private fun criarItemMenu(
        texto: String,
        acao: () -> Unit
    ): TextView {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            18f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true

        item.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()
            }

        item.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setColor(
                if (focado) {
                    Color.argb(
                        220,
                        180,
                        0,
                        0
                    )
                } else {
                    Color.TRANSPARENT
                }
            )

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

        item.setOnKeyListener {
                _, tecla, evento ->

            if (
                evento.action ==
                KeyEvent.ACTION_DOWN &&
                (
                    tecla ==
                    KeyEvent.KEYCODE_ENTER ||
                    tecla ==
                    KeyEvent.KEYCODE_DPAD_CENTER
                )
            ) {

                acao()

                true

            } else {

                false
            }
        }

        itensMenuFoco.add(
            item
        )

        return item
    }

    private fun montarMenu() {

        menuConteudo.removeAllViews()

        itensMenuFoco.clear()

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF IPTV"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            dp(18),
            0,
            dp(10),
            0
        )

        menuConteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Favoritos"
            ) {

                fecharMenu()

                mostrarFavoritos()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Continue assistindo"
            ) {

                fecharMenu()

                mostrarContinueAssistindo()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Filmes"
            ) {

                fecharMenu()

                mostrarListaFilmes(
                    filmes
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Séries"
            ) {

                fecharMenu()

                mostrarListaSeries(
                    series
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Doramas"
            ) {

                fecharMenu()

                mostrarListaDoramas(
                    doramas
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Anime"
            ) {

                fecharMenu()

                mostrarListaAnimes(
                    animes
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }

    private fun mostrarFavoritos() {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "Favoritos"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        val lista =
            filmes.filter {
                favoritos.contains(
                    it.titulo
                )
            }

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum favorito"

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(300)
                )
            )

            return
        }

        lista.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { filme ->

                    val card =
                        criarCardFilme(
                            filme
                        )

                    linha.addView(
                        card,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).apply {

                            leftMargin =
                                dp(4)

                            rightMargin =
                                dp(4)

                            bottomMargin =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(
                        card
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }    private fun abrirMenu() {

        if (menuAberto) {
            return
        }

        menuAberto = true

        menuLateral.visibility =
            View.VISIBLE

        menuLateral.alpha =
            0f

        menuLateral.animate()
            .alpha(1f)
            .setDuration(180)
            .start()

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }

    private fun fecharMenu() {

        if (!menuAberto) {
            return
        }

        menuAberto = false

        menuLateral.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {

                menuLateral.visibility =
                    View.GONE
            }
            .start()

        if (
            cardsAtuais.isNotEmpty()
        ) {

            val indice =
                indiceCardAtual.coerceIn(
                    0,
                    cardsAtuais.size - 1
                )

            cardsAtuais[indice]
                .requestFocus()
        }
    }

    private fun criarItemMenu(
        texto: String,
        acao: () -> Unit
    ): TextView {

        val item =
            TextView(this)

        item.text =
            texto

        item.textSize =
            18f

        item.setTextColor(
            Color.WHITE
        )

        item.gravity =
            Gravity.CENTER_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.isFocusable =
            true

        item.isFocusableInTouchMode =
            true

        item.background =
            GradientDrawable().apply {

                setColor(
                    Color.TRANSPARENT
                )

                cornerRadius =
                    dp(8).toFloat()
            }

        item.setOnFocusChangeListener {
                view,
                focado ->

            val fundo =
                view.background
                    as GradientDrawable

            fundo.setColor(
                if (focado) {
                    Color.argb(
                        220,
                        180,
                        0,
                        0
                    )
                } else {
                    Color.TRANSPARENT
                }
            )

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

        item.setOnKeyListener {
                _, tecla, evento ->

            if (
                evento.action ==
                KeyEvent.ACTION_DOWN &&
                (
                    tecla ==
                    KeyEvent.KEYCODE_ENTER ||
                    tecla ==
                    KeyEvent.KEYCODE_DPAD_CENTER
                )
            ) {

                acao()

                true

            } else {

                false
            }
        }

        itensMenuFoco.add(
            item
        )

        return item
    }

    private fun montarMenu() {

        menuConteudo.removeAllViews()

        itensMenuFoco.clear()

        val titulo =
            TextView(this)

        titulo.text =
            "WOLF IPTV"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.setPadding(
            dp(18),
            0,
            dp(10),
            0
        )

        menuConteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Favoritos"
            ) {

                fecharMenu()

                mostrarFavoritos()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Continue assistindo"
            ) {

                fecharMenu()

                mostrarContinueAssistindo()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Filmes"
            ) {

                fecharMenu()

                mostrarListaFilmes(
                    filmes
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Séries"
            ) {

                fecharMenu()

                mostrarListaSeries(
                    series
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Doramas"
            ) {

                fecharMenu()

                mostrarListaDoramas(
                    doramas
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        menuConteudo.addView(
            criarItemMenu(
                "Anime"
            ) {

                fecharMenu()

                mostrarListaAnimes(
                    animes
                )
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        if (
            itensMenuFoco.isNotEmpty()
        ) {

            itensMenuFoco[0]
                .requestFocus()
        }
    }

    private fun mostrarFavoritos() {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "Favoritos"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        val lista =
            filmes.filter {
                favoritos.contains(
                    it.titulo
                )
            }

        if (lista.isEmpty()) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum favorito"

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(300)
                )
            )

            return
        }

        lista.chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(this)

                linha.orientation =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { filme ->

                    val card =
                        criarCardFilme(
                            filme
                        )

                    linha.addView(
                        card,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).apply {

                            leftMargin =
                                dp(4)

                            rightMargin =
                                dp(4)

                            bottomMargin =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(
                        card
                    )
                }

                conteudo.addView(
                    linha
                )
            }

        if (
            cardsAtuais.isNotEmpty()
        ) {

            cardsAtuais[0]
                .requestFocus()
        }
    }    private fun mostrarContinueAssistindo() {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        val titulo =
            TextView(this)

        titulo.text =
            "Continue assistindo"

        titulo.textSize =
            24f

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        if (
            historicoConteudo.isEmpty()
        ) {

            val vazio =
                TextView(this)

            vazio.text =
                "Nenhum conteúdo para continuar"

            vazio.textSize =
                20f

            vazio.setTextColor(
                Color.WHITE
            )

            vazio.gravity =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(300)
                )
            )

            return
        }

        val texto =
            TextView(this)

        texto.text =
            "Continue assistindo pelos últimos conteúdos acessados."

        texto.textSize =
            18f

        texto.setTextColor(
            Color.LTGRAY
        )

        texto.gravity =
            Gravity.CENTER

        conteudo.addView(
            texto,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(100)
            )
        )
    }

    private fun adicionarFavorito(
        filme: Filme
    ) {

        if (
            favoritos.contains(
                filme.titulo
            )
        ) {

            favoritos.remove(
                filme.titulo
            )

        } else {

            favoritos.add(
                filme.titulo
            )
        }

        salvarFavoritos()
    }

    private fun salvarFavoritos() {

        try {

            val preferencias =
                getSharedPreferences(
                    "WOLF_IPTV",
                    MODE_PRIVATE
                )

            preferencias
                .edit()
                .putStringSet(
                    "FAVORITOS",
                    favoritos
                )
                .apply()

        } catch (_: Exception) {
        }
    }

    private fun carregarFavoritos() {

        try {

            val preferencias =
                getSharedPreferences(
                    "WOLF_IPTV",
                    MODE_PRIVATE
                )

            favoritos.clear()

            favoritos.addAll(
                preferencias.getStringSet(
                    "FAVORITOS",
                    emptySet()
                ) ?: emptySet()
            )

        } catch (_: Exception) {
        }
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
            resources.displayMetrics.density
        ).toInt()
    }

    override fun onBackPressed() {

        if (menuAberto) {

            fecharMenu()

            return
        }

        if (
            historicoConteudo.isNotEmpty()
        ) {

            val anterior =
                historicoConteudo
                    .removeLastOrNull()

            if (anterior != null) {

                anterior.run()

                return
            }
        }

        super.onBackPressed()
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action ==
            KeyEvent.ACTION_DOWN
        ) {

            when (
                event.keyCode
            ) {

                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    if (
                        !menuAberto
                    ) {

                        abrirMenu()

                        return true
                    }
                }

                KeyEvent.KEYCODE_BACK -> {

                    if (menuAberto) {

                        fecharMenu()

                        return true
                    }
                }
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }    private fun moverMenu(
        direcao: Int
    ) {

        if (
            itensMenuFoco.isEmpty()
        ) {
            return
        }

        val atual =
            itensMenuFoco
                .indexOfFirst {
                    it.hasFocus()
                }

        if (atual < 0) {

            itensMenuFoco[0]
                .requestFocus()

            return
        }

        val novo =
            (
                atual + direcao
            ).coerceIn(
                0,
                itensMenuFoco.size - 1
            )

        itensMenuFoco[novo]
            .requestFocus()
    }

    private fun moverCard(
        deslocamento: Int
    ) {

        if (
            cardsAtuais.isEmpty()
        ) {
            return
        }

        val atual =
            cardsAtuais
                .indexOfFirst {
                    it.hasFocus()
                }

        if (atual < 0) {

            cardsAtuais[0]
                .requestFocus()

            indiceCardAtual = 0

            return
        }

        val novo =
            (
                atual + deslocamento
            ).coerceIn(
                0,
                cardsAtuais.size - 1
            )

        indiceCardAtual =
            novo

        cardsAtuais[novo]
            .requestFocus()
    }

    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        if (
            event.action !=
            KeyEvent.ACTION_DOWN
        ) {

            return super.dispatchKeyEvent(
                event
            )
        }

        if (menuAberto) {

            when (
                event.keyCode
            ) {

                KeyEvent.KEYCODE_DPAD_UP -> {

                    moverMenu(-1)

                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    moverMenu(1)

                    return true
                }

                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    return true
                }

                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {

                    val foco =
                        if (
                            botaoFecharMenu
                                .hasFocus()
                        ) {

                            botaoFecharMenu

                        } else {

                            itensMenuFoco
                                .firstOrNull {
                                    it.hasFocus()
                                }
                        }

                    foco?.performClick()

                    return true
                }

                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_MENU -> {

                    fecharMenu()

                    return true
                }
            }

            return true
        }

        when (
            event.keyCode
        ) {

            KeyEvent.KEYCODE_DPAD_UP -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    return true
                }

                val atual =
                    cardsAtuais
                        .indexOfFirst {
                            it.hasFocus()
                        }

                if (atual == 0) {

                    botaoMenu
                        .requestFocus()

                    return true
                }

                moverCard(-5)

                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    if (
                        cardsAtuais.isNotEmpty()
                    ) {

                        cardsAtuais[0]
                            .requestFocus()
                    }

                    return true
                }

                moverCard(5)

                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    return true
                }

                moverCard(-1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    return true
                }

                moverCard(1)

                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {

                if (
                    botaoMenu.hasFocus()
                ) {

                    abrirMenu()

                    return true
                }

                val foco =
                    cardsAtuais
                        .firstOrNull {
                            it.hasFocus()
                        }

                foco?.performClick()

                return true
            }

            KeyEvent.KEYCODE_MENU -> {

                abrirMenu()

                return true
            }

            KeyEvent.KEYCODE_BACK -> {

                if (
                    historicoConteudo
                        .isNotEmpty()
                ) {

                    val voltar =
                        historicoConteudo
                            .removeLast()

                    voltar()

                    return true
                }

                finish()

                return true
            }
        }

        return super.dispatchKeyEvent(
            event
        )
    }
}
