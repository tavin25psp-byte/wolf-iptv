package com.wolf.iptv

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var categorias: LinearLayout
    private lateinit var conteudo: LinearLayout

    private lateinit var botaoFilmes: Button
    private lateinit var botaoSeries: Button
    private lateinit var botaoDoramas: Button

    private var abaAtual = "FILMES"

    private val categoriasFilmes = arrayOf(
        "TODOS",
        "AÇÃO",
        "AVENTURA",
        "COMÉDIA",
        "TERROR",
        "ROMANCE",
        "DRAMA",
        "FICÇÃO",
        "ANIMAÇÃO"
    )

    private val categoriasSeries = arrayOf(
        "TODAS",
        "AÇÃO",
        "AVENTURA",
        "COMÉDIA",
        "DRAMA",
        "TERROR",
        "CRIME",
        "FANTASIA",
        "FICÇÃO"
    )

    private val categoriasDoramas = arrayOf(
        "TODOS",
        "ROMANCE",
        "AÇÃO",
        "COMÉDIA",
        "DRAMA",
        "ESCOLAR",
        "HISTÓRICO",
        "FANTASIA",
        "SUSPENSE"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        construirInterface()
    }

    private fun construirInterface() {

        val fundo = LinearLayout(this)

        fundo.orientation = LinearLayout.VERTICAL
        fundo.setBackgroundColor(Color.BLACK)
        fundo.setPadding(35, 20, 35, 20)

        // =========================
        // TOPO
        // =========================

        val topo = LinearLayout(this)

        topo.orientation = LinearLayout.HORIZONTAL
        topo.gravity = Gravity.CENTER_VERTICAL

        val logo = TextView(this)

        logo.text = "WOLF IPTV"
        logo.textSize = 30f
        logo.setTextColor(Color.WHITE)
        logo.setTypeface(null, Typeface.BOLD)

        topo.addView(
            logo,
            LinearLayout.LayoutParams(
                0,
                65,
                1f
            )
        )

        val buscar = criarBotaoTopo("BUSCAR")

        topo.addView(
            buscar,
            LinearLayout.LayoutParams(
                180,
                60
            )
        )

        fundo.addView(
            topo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            )
        )

        // =========================
        // MENU PRINCIPAL
        // =========================

        val scrollMenu = HorizontalScrollView(this)

        scrollMenu.isHorizontalScrollBarEnabled = false

        val menuPrincipal = LinearLayout(this)

        menuPrincipal.orientation = LinearLayout.HORIZONTAL

        botaoFilmes = criarBotaoPrincipal("FILMES")
        botaoSeries = criarBotaoPrincipal("SÉRIES")
        botaoDoramas = criarBotaoPrincipal("DORAMAS")

        menuPrincipal.addView(botaoFilmes)
        menuPrincipal.addView(botaoSeries)
        menuPrincipal.addView(botaoDoramas)

        scrollMenu.addView(menuPrincipal)

        fundo.addView(
            scrollMenu,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            )
        )

        // =========================
        // TÍTULO CATEGORIAS
        // =========================

        val tituloCategorias = TextView(this)

        tituloCategorias.text = "CATEGORIAS"
        tituloCategorias.textSize = 17f
        tituloCategorias.setTextColor(Color.LTGRAY)
        tituloCategorias.setTypeface(null, Typeface.BOLD)

        fundo.addView(
            tituloCategorias,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                45
            )
        )

        // =========================
        // CATEGORIAS
        // =========================

        val scrollCategorias = HorizontalScrollView(this)

        scrollCategorias.isHorizontalScrollBarEnabled = false

        categorias = LinearLayout(this)

        categorias.orientation = LinearLayout.HORIZONTAL

        scrollCategorias.addView(categorias)

        fundo.addView(
            scrollCategorias,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // =========================
        // CONTEÚDO
        // =========================

        val scrollConteudo = ScrollView(this)

        conteudo = LinearLayout(this)

        conteudo.orientation = LinearLayout.VERTICAL

        scrollConteudo.addView(conteudo)

        fundo.addView(
            scrollConteudo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(fundo)

        abrirFilmes()

        botaoFilmes.requestFocus()
    }

    // =========================
    // BOTÃO PRINCIPAL
    // =========================

    private fun criarBotaoPrincipal(texto: String): Button {

        val botao = Button(this)

        botao.text = texto
        botao.textSize = 16f
        botao.isFocusable = true
        botao.isFocusableInTouchMode = true

        botao.setTextColor(Color.WHITE)
        botao.setBackgroundColor(Color.rgb(35, 35, 35))

        botao.setPadding(25, 0, 25, 0)

        botao.setOnFocusChangeListener { view, foco ->

            if (foco) {

                view.setBackgroundColor(Color.WHITE)
                (view as Button).setTextColor(Color.BLACK)

            } else {

                if (abaAtual == texto) {

                    view.setBackgroundColor(Color.WHITE)
                    (view as Button).setTextColor(Color.BLACK)

                } else {

                    view.setBackgroundColor(Color.rgb(35, 35, 35))
                    (view as Button).setTextColor(Color.WHITE)
                }
            }
        }

        when (texto) {

            "FILMES" -> {
                botao.setOnClickListener {
                    abrirFilmes()
                }
            }

            "SÉRIES" -> {
                botao.setOnClickListener {
                    abrirSeries()
                }
            }

            "DORAMAS" -> {
                botao.setOnClickListener {
                    abrirDoramas()
                }
            }
        }

        return botao
    }

    // =========================
    // BOTÃO BUSCAR
    // =========================

    private fun criarBotaoTopo(texto: String): Button {

        val botao = Button(this)

        botao.text = texto
        botao.textSize = 14f
        botao.isFocusable = true
        botao.isFocusableInTouchMode = true

        botao.setTextColor(Color.WHITE)
        botao.setBackgroundColor(Color.rgb(35, 35, 35))

        return botao
    }

    // =========================
    // FILMES
    // =========================

    private fun abrirFilmes() {

        abaAtual = "FILMES"

        atualizarMenuPrincipal()

        criarCategorias(categoriasFilmes)

        mostrarConteudo(
            "FILMES",
            "TODOS"
        )
    }

    // =========================
    // SÉRIES
    // =========================

    private fun abrirSeries() {

        abaAtual = "SÉRIES"

        atualizarMenuPrincipal()

        criarCategorias(categoriasSeries)

        mostrarConteudo(
            "SÉRIES",
            "TODAS"
        )
    }

    // =========================
    // DORAMAS
    // =========================

    private fun abrirDoramas() {

        abaAtual = "DORAMAS"

        atualizarMenuPrincipal()

        criarCategorias(categoriasDoramas)

        mostrarConteudo(
            "DORAMAS",
            "TODOS"
        )
    }

    // =========================
    // ATUALIZAR MENU
    // =========================

    private fun atualizarMenuPrincipal() {

        val botoes = arrayOf(
            botaoFilmes,
            botaoSeries,
            botaoDoramas
        )

        for (botao in botoes) {

            if (botao.text.toString() == abaAtual) {

                botao.setBackgroundColor(Color.WHITE)
                botao.setTextColor(Color.BLACK)

            } else {

                botao.setBackgroundColor(Color.rgb(35, 35, 35))
                botao.setTextColor(Color.WHITE)
            }
        }
    }

    // =========================
    // CRIAR CATEGORIAS
    // =========================

    private fun criarCategorias(lista: Array<String>) {

        categorias.removeAllViews()

        for (categoria in lista) {

            val botao = Button(this)

            botao.text = categoria
            botao.textSize = 13f
            botao.isFocusable = true
            botao.isFocusableInTouchMode = true

            botao.setTextColor(Color.WHITE)
            botao.setBackgroundColor(Color.rgb(30, 30, 30))

            botao.setPadding(20, 0, 20, 0)

            botao.setOnFocusChangeListener { view, foco ->

                if (foco) {

                    view.setBackgroundColor(Color.WHITE)
                    (view as Button).setTextColor(Color.BLACK)

                } else {

                    view.setBackgroundColor(Color.rgb(30, 30, 30))
                    (view as Button).setTextColor(Color.WHITE)
                }
            }

            botao.setOnClickListener {

                mostrarConteudo(
                    abaAtual,
                    categoria
                )
            }

            categorias.addView(
                botao,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    60
                )
            )
        }
    }

    // =========================
    // CONTEÚDO
    // =========================

    private fun mostrarConteudo(
        tipo: String,
        categoria: String
    ) {

        conteudo.removeAllViews()

        val titulo = TextView(this)

        titulo.text = "$tipo • $categoria"
        titulo.textSize = 24f
        titulo.setTextColor(Color.WHITE)
        titulo.setTypeface(null, Typeface.BOLD)

        titulo.setPadding(0, 15, 0, 15)

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70
            )
        )

        if (tipo == "FILMES" && categoria == "TODOS") {

            criarCardFilme(
                "Jumanji",
                "Jumanji - Bem-Vindo à Selva"
            )

        } else {

            val mensagem = TextView(this)

            mensagem.text =
                "Nenhum conteúdo cadastrado nesta categoria ainda."

            mensagem.textSize = 17f
            mensagem.setTextColor(Color.LTGRAY)

            mensagem.gravity = Gravity.CENTER_VERTICAL

            conteudo.addView(
                mensagem,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    100
                )
            )
        }
    }

    // =========================
    // CARD DE FILME
    // =========================

    private fun criarCardFilme(
        titulo: String,
        nome: String
    ) {

        val linha = LinearLayout(this)

        linha.orientation = LinearLayout.HORIZONTAL

        linha.gravity = Gravity.CENTER_VERTICAL

        val card = Button(this)

        card.text =
            "🎬\n\n$nome\n\n▶ ASSISTIR"

        card.textSize = 16f

        card.gravity = Gravity.CENTER

        card.isFocusable = true
        card.isFocusableInTouchMode = true

        card.setTextColor(Color.WHITE)
        card.setBackgroundColor(Color.rgb(30, 30, 30))

        card.setOnFocusChangeListener { view, foco ->

            if (foco) {

                view.setBackgroundColor(Color.WHITE)
                (view as Button).setTextColor(Color.BLACK)

            } else {

                view.setBackgroundColor(Color.rgb(30, 30, 30))
                (view as Button).setTextColor(Color.WHITE)
            }
        }

        card.setOnClickListener {

            val intent = Intent(
                this,
                PlayerActivity::class.java
            )

            intent.putExtra(
                "VIDEO_URL",
                "https://wolf-channel-cdn.b-cdn.net/Jumanji%20-%20Bem-Vindo%20%C3%80%20Selva%20-%20Dublado.mp4"
            )

            startActivity(intent)
        }

        linha.addView(
            card,
            LinearLayout.LayoutParams(
                260,
                300
            )
        )

        conteudo.addView(
            linha,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                320
            )
        )
    }

    // =========================
    // CONTROLE REMOTO
    // =========================

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {

        if (event.action == KeyEvent.ACTION_DOWN) {

            when (event.keyCode) {

                KeyEvent.KEYCODE_BACK -> {

                    finish()

                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
