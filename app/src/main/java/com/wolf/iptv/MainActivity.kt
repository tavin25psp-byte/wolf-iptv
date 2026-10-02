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

    private lateinit var botaoFilmes: Button
    private lateinit var botaoSeries: Button
    private lateinit var botaoDoramas: Button
    private lateinit var botaoFavoritos: Button

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
        fundo.setPadding(40, 25, 40, 25)

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
                70,
                1f
            )
        )

        val busca = Button(this)

        busca.text = "🔍 BUSCAR"
        busca.textSize = 15f
        busca.isFocusable = true

        topo.addView(
            busca,
            LinearLayout.LayoutParams(
                180,
                65
            )
        )

        fundo.addView(topo)

        // =========================
        // ESPAÇO
        // =========================

        val espaco = View(this)

        fundo.addView(
            espaco,
            LinearLayout.LayoutParams(
                1,
                25
            )
        )

        // =========================
        // MENU
        // =========================

        val menu = HorizontalScrollView(this)

        menu.isHorizontalScrollBarEnabled = false

        val menuInterno = LinearLayout(this)

        menuInterno.orientation = LinearLayout.HORIZONTAL

        botaoFilmes = criarBotao("FILMES")
        botaoSeries = criarBotao("SÉRIES")
        botaoDoramas = criarBotao("DORAMAS")
        botaoFavoritos = criarBotao("FAVORITOS")

        menuInterno.addView(botaoFilmes)
        menuInterno.addView(botaoSeries)
        menuInterno.addView(botaoDoramas)
        menuInterno.addView(botaoFavoritos)

        menu.addView(menuInterno)

        fundo.addView(
            menu,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75
            )
        )

        // =========================
        // CONTEÚDO
        // =========================

        val scroll = ScrollView(this)

        val conteudo = LinearLayout(this)

        conteudo.orientation = LinearLayout.VERTICAL

        val titulo = TextView(this)

        titulo.text = "DESTAQUES"
        titulo.textSize = 24f
        titulo.setTextColor(Color.WHITE)
        titulo.setTypeface(null, Typeface.BOLD)

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // =========================
        // PRIMEIRO CARD
        // =========================

        val card = criarCard(
            "Jumanji",
            "Jumanji - Bem-Vindo à Selva"
        )

        conteudo.addView(card)

        // =========================
        // MAIS CONTEÚDO
        // =========================

        val tituloFilmes = TextView(this)

        tituloFilmes.text = "FILMES"
        tituloFilmes.textSize = 24f
        tituloFilmes.setTextColor(Color.WHITE)
        tituloFilmes.setTypeface(null, Typeface.BOLD)

        tituloFilmes.setPadding(0, 35, 0, 10)

        conteudo.addView(tituloFilmes)

        val mensagem = TextView(this)

        mensagem.text =
            "Sua biblioteca de filmes, séries e doramas aparecerá aqui."

        mensagem.textSize = 17f
        mensagem.setTextColor(Color.LTGRAY)

        conteudo.addView(
            mensagem,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80
            )
        )

        scroll.addView(conteudo)

        fundo.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(fundo)

        botaoFilmes.requestFocus()
    }

    private fun criarBotao(texto: String): Button {

        val botao = Button(this)

        botao.text = texto
        botao.textSize = 15f
        botao.isFocusable = true
        botao.isFocusableInTouchMode = true

        botao.setTextColor(Color.WHITE)

        botao.setOnFocusChangeListener { view, temFoco ->

            if (temFoco) {
                view.setBackgroundColor(Color.WHITE)
                (view as Button).setTextColor(Color.BLACK)
            } else {
                view.setBackgroundColor(Color.DKGRAY)
                (view as Button).setTextColor(Color.WHITE)
            }
        }

        return botao
    }

    private fun criarCard(
        titulo: String,
        nomeVideo: String
    ): Button {

        val card = Button(this)

        card.text =
            "🎬\n\n$nomeVideo\n\n▶ ASSISTIR"

        card.textSize = 18f
        card.gravity = Gravity.CENTER
        card.isFocusable = true
        card.isFocusableInTouchMode = true

        card.setTextColor(Color.WHITE)
        card.setBackgroundColor(Color.rgb(30, 30, 30))

        card.setOnFocusChangeListener { view, temFoco ->

            if (temFoco) {
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

        return card
    }

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
