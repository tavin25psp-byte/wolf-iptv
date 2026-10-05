pacote com.wolf.iptv

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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

classe de dados Filme(
    val titulo: String,
    val ano: Int,
    categoria val: String,
    val capa: String,
    vídeo val: String
)

classe de dados Episódio(
    val numero: Int,
    val titulo: String,
    vídeo val: String
)

classe de dados Temporada(
    val numero: Int,
    val episodios: Lista<Episódio>
)

classe de dados Série(
    val titulo: String,
    categoria val: String,
    val capa: String,
    val temporadas: List<Temporada>
)

classe MainActivity : AppCompatActivity() {

    valor privado CATÁLOGO_URL =
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/catalogo.json"

    private lateinit var raiz: FrameLayout
    private lateinit var conteudo: LinearLayout
    private lateinit var botaoMenu: TextView

    private lateinit var menuLateral: LinearLayout
    private lateinit var menuScroll: ScrollView
    private lateinit var menuConteudo: LinearLayout
    private lateinit var botaoFecharMenu: TextView

    private var menuAberto = false

    valor privado itensMenuFoco =
        listaMutávelDe<View>()

    valor privado cacheCapas =
        HashMap<String, Bitmap>()

    private val históricoConteudo =
        listaMutávelDe<() -> Unidade>()

    filmes de val privados =
        listaMutávelDe<Filme>()

    séries val privadas =
        listaMutávelDe<Série>()

    val doramas privados =
        listaMutávelDe<Série>()

    animes val privados =
        listaMutávelDe<Série>()

    cartões val privados Atuais =
        listaMutávelDe<View>()

    var privado índiceCardAtual = 0

    valor privado favoritos =
        mutableSetOf<String>()

    sobrescrever fun onCreate(
        estadoDaInstanciaSavada: Pacote?
    ) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            Exibir.SYSTEM_UI_FLAG_FULLSCREEN ou
            Exibir.SYSTEM_UI_FLAG_HIDE_NAVIGATION ou
            Exibir.SYSTEM_UI_FLAG_IMMERSIVE_STICKY ou
            Exibir.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN ou
            Exibir.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION ou
            Visualizar.SYSTEM_UI_FLAG_LAYOUT_ESTÁVEL

        criarInterface()

        carregarFilmes()
        carregarSeries()
    }
        private fun criarInterface() {

        raiz = FrameLayout(this)

        raiz.setBackgroundColor(
            Cor: PRETO
        )

        val fundo = ImageView(this)

        fundo.scaleType =
            ImageView.ScaleType.CENTER_CROP

        fundo.alpha = 0,55f

        carregarImagem(
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

        val camada =
            LinearLayout(isto)

        camada.orientação =
            LinearLayout.VERTICAL

        camada.setPadding(
            dp(20),
            dp(12),
            dp(20),
            dp(12)
        )

        raiz.addView(
            camada,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        botaoMenu =
            TextView(this)

        botaoMenu.text =
            "â˜° CARDÁPIO DO LOBO"

        botaoMenu.setTextColor(
            Cor: BRANCA
        )

        botaoMenu.textSize = 20f

        botaoMenu.setTypeface(
            nulo,
            Tipo de letra.NEGRO
        )

        botaoMenu.gravidade =
            Gravity.CENTER

        botaoMenu.isFocusable = true
        botaoMenu.isClickable = true

        botaoMenu.background =
            criarFundoCard(falso)

        botaoMenu.setOnFocusChangeListener {
            _, foco ->

            botaoMenu.background =
                criarFundoCard(foco)
        }

        botaoMenu.setOnClickListener {
            abrirMenu()
        }

        camada.addView(
            botaoMenu,
            LinearLayout.LayoutParams(
                dp(235),
                dp(58)
            )
        )

        val scroll =
            ScrollView(isto)

        scroll.isFocusable = false

        scroll.isFocusableInTouchMode =
            falso

        conteudo =
            LinearLayout(isto)

        conteudo.orientação =
            LinearLayout.VERTICAL

        scroll.addView(
            contado,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        camada.addView(
            rolar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        definirVisualizaçãoDeConteúdo(raiz)
    }

    diversão privada dp(
        valor: Int
    ): Int {

        retornar (
            valor *
            recursos.exibirMétricas.densidade
        ).toInt()
    }

    diversão privada criarFundoCard(
        foco: Booleano
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            se (foco) {
                Cor.argb(
                    235,
                    10,
                    10,
                    10
                )
            } outro {
                Cor.argb(
                    190,
                    10,
                    10,
                    10
                )
            }
        )

        fundo.cornerRadius =
            dp(8).toFloat()

        se (foco) {

            fundo.setStroke(
                dp(4),
                Cor vermelha.
            )
        }

        retornar fundo
    }

    diversão privada criarBordaVermelha():
        GradientDrawable {

        val borda =
            GradientDrawable()

        borda.setColor(
            Cor. TRANSPARENTE
        )

        borda.cornerRadius =
            dp(8).toFloat()

        borda.setStroke(
            dp(4),
            Cor vermelha.
        )

        retornar borda
    }

    diversão privada carregarImagem(
        URL: String,
        imagem: Visualização de Imagem
    ) {

        se (url.isBlank()) retornar

        val salva =
            cacheCapas[url]

        se (salva != nulo) {

            imagem.setImageBitmap(
                salva
            )

            retornar
        }

        fio {

            tentar {

                val conexao =
                    URL(url)
                        .openConnection()
                            como HttpURLConnection

                conexao.connectTimeout =
                    15000

                conexao.readTimeout =
                    15000

                conexao.doInput = true

                conexao.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        conexao.inputStream
                    )

                conexao.desconectar()

                se (bitmap != nulo) {

                    cacheCapas[url] =
                        bitmap

                    executarNaUiThread {

                        imagem.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
            }
        }
    }private fun carregarFilmes() {

        fio {

            var conexão: HttpURLConnection? = nulo

            tentar {

                conexao =
                    URL(CATALOGO_URL)
                        .openConnection() como HttpURLConnection

                conexao.connectTimeout = 20000
                conexao.readTimeout = 20000
                conexao.requestMethod = "GET"
                conexao.doInput = true

                conexao.setRequestProperty(
                    "Agente do usuário",
                    "Mozilla/5.0"
                )

                conexao.connect()

                val código =
                    código de resposta do conexao

                se (código !em 200..299) {
                    lançar exceção(
                        "HTTP $código"
                    )
                }

                val resposta =
                    conexao.inputStream
                        .bufferedReader(
                            Charsets.UTF_8
                        )
                        .usar {
                            ele.lerTexto()
                        }

                se (resposta.isBlank()) {
                    lançar exceção(
                        "Catálogo de ouro"
                    )
                }

                val texto =
                    resposta.trim()

                val raizJson =
                    JSONObject(texto)

                val novosFilmes =
                    ArrayList<Filme>()

                val listaFilmes =
                    raizJson.optJSONArray(
                        "filmes"
                    )

                se (listaFilmes != nulo) {

                    para (i em 0 até listaFilmes.length()) {

                        tentar {

                            item val =
                                listaFilmes.getJSONObject(i)

                            val titulo =
                                item.optString(
                                    "título",
                                    ""
                                )

                            val ano =
                                item.optInt(
                                    "ano",
                                    0
                                )

                            val nome =
                                item.optString(
                                    "categoria",
                                    ""
                                )

                            val capa =
                                item.optString(
                                    "capa",
                                    ""
                                )

                            vídeo val =
                                item.optString(
                                    "vídeo",
                                    ""
                                )

                            se (
                                título.isNotBlank()
                            ) {

                                novosFilmes.adicionar(
                                    Filme(
                                        título = título,
                                        ano = ano,
                                        categoria = categoria,
                                        capa = capa,
                                        vídeo = vídeo
                                    )
                                )
                            }

                        } catch (_: Exception) {}
                    }
                }

                novosFilmes.sortWith(
                    compareByDescending<Filme> {
                        ele.ano
                    }.thenBy {
                        it.titulo.lowercase()
                    }
                )

                série divertida (
                    array: JSONArray?
                ): ArrayList<Serie> {

                    val resultado =
                        ArrayList<Serie>()

                    se (array == nulo) {
                        retornar resultado
                    }

                    para (
                        i em 0 até array.length()
                    ) {

                        tentar {

                            val objeto =
                                array.getJSONObject(i)

                            val titulo =
                                objeto.optString(
                                    "título",
                                    ""
                                )

                            val nome =
                                objeto.optString(
                                    "categoria",
                                    ""
                                )

                            val capa =
                                objeto.optString(
                                    "capa",
                                    ""
                                )

                            val temporadas =
                                ArrayList<Temporada>()

                            val arrayTemporadas =
                                objeto.optJSONArray(
                                    "temporadas"
                                )

                            se (
                                arrayTemporadas != null
                            ) {

                                para (
                                    t em 0 até
                                        arrayTemporadas.length()
                                ) {

                                    tentar {

                                        val objetoTemporada =
                                            arrayTemporadas
                                                .getJSONObject(t)

                                        val numeroTemporada =
                                            objetoTemporada
                                                .optInt(
                                                    "número",
                                                    t + 1
                                                )

                                        val episodios =
                                            ArrayList<Episódio>()

                                        val arrayEpisódios =
                                            objetoTemporada
                                                .optJSONArray(
                                                    "episódios"
                                                )

                                        se (
                                            arrayEpisódios != null
                                        ) {

                                            para (
                                                e em 0 até
                                                    arrayEpisodios.length()
                                            ) {

                                                tentar {

                                                    val objetoEpisodio =
                                                        arrayEpisódios
                                                            .getJSONObject(e)

                                                    val numeroEpisodio =
                                                        objetoEpisódio
                                                            .optInt(
                                                                "número",
                                                                e + 1
                                                            )

                                                    val tituloEpisodio =
                                                        objetoEpisódio
                                                            .optString(
                                                                "título",
                                                                ""
                                                            )

                                                    val videoEpisodio =
                                                        objetoEpisódio
                                                            .optString(
                                                                "vídeo",
                                                                ""
                                                            )

                                                    episódios.adicionar(
                                                        Episódio(
                                                            numero =
                                                                numeroEpisódio,
                                                            título =
                                                                títuloEpisódio,
                                                            vídeo =
                                                                vídeoEpisódio
                                                        )
                                                    )

                                                } catch (_: Exception) {}
                                            }
                                        }

                                        temporadas.adicionar(
                                            Temporada(
                                                numero =
                                                    numeroTemporada,
                                                episódios =
                                                    episódios
                                            )
                                        )

                                    } catch (_: Exception) {}
                                }
                            }

                            se (
                                título.isNotBlank()
                            ) {

                                resultado.adicionar(
                                    Série(
                                        título =
                                            título,
                                        Apelido =
                                            categoria,
                                        capa =
                                            capa,
                                        temporadas =
                                            temporadas
                                    )
                                )
                            }

                        } catch (_: Exception) {}
                    }

                    retornar resultado
                }

                val novasSeries =
                    lerSeries(
                        raizJson.optJSONArray(
                            "série"
                        )
                    )

                val novosDoramas =
                    lerSeries(
                        raizJson.optJSONArray(
                            "doramas"
                        )
                    )

                val novosAnimes =
                    lerSeries(
                        raizJson.optJSONArray(
                            "animes"
                        )
                    )

                executarNaUiThread {

                    filmes.limpar()
                    filmes.adicionarTodos(
                        novosFilmes
                    )

                    série.limpar()
                    série.adicionarTudo(
                        novasSeries
                    )

                    doramas.limpar()
                    doramas.adicionarTodos(
                        novosDoramas
                    )

                    animes.limpar()
                    animes.addAll(
                        novosAnimes
                    )

                    mostrarListaCards(
                        filmes
                    )

                    Toast.makeText(
                        esta@MainActivity,
                        "Catálogo protegido: " +
                            "${filmes.size} filmes, " +
                            "${series.size} séries, " +
                            "${doramas.size} doramas e " +
                            "${animes.size} animes",
                        Torrada.COMPRIMENTO_LONGO
                    ).mostrar()
                }

            } pegar (
                erro: Exceção
            ) {

                executarNaUiThread {

                    filmes.limpar()
                    série.limpar()
                    doramas.limpar()
                    animes.limpar()

                    conteudo.removeAllViews()

                    val erroTexto =
                        TextView(this@MainActivity)

                    erroTexto.texto =
                        "ERRO NO CATÍ LOGO\n\n" +
                        "${erro.message ?: "Erro desconhecido"}"

                    erroTexto.textSize =
                        20f

                    erroTexto.setTextColor(
                        Cor: BRANCA
                    )

                    erroTexto.gravidade =
                        Gravity.CENTER

                    erroTexto.setPadding(
                        dp(30),
                        dp(30),
                        dp(30),
                        dp(30)
                    )

                    conteudo.addView(
                        erroTexto
                    )

                    Toast.makeText(
                        esta@MainActivity,
                        "Erro ao carregar catálogo",
                        Torrada.COMPRIMENTO_LONGO
                    ).mostrar()
                }

            } finalmente {

                conexao?.desconectar()
            }
        }
    }

    fun privado carregarSeries() {}função privada mostrarListaCards(
        lista: Lista<Filme>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        se (lista.isEmpty()) {

            val REM =
                TextView(this)

            texto =
                "Nenhum filme encontrado"

            vazio.setTextColor(
                Cor: BRANCA
            )

            vazio.textSize = 20f

            gravidade =
                Gravity.CENTER

            .setPadding(
                dp(20),
                dp(40),
                dp(20),
                dp(40)
            )

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(100)
                )
            )

            retornar
        }

        lista.chunked(5).forEach { grupo ->

            val linha =
                LinearLayout(isto)

            linha.orientação =
                LinearLayout.HORIZONTAL

            linha.layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )

            grupo.forEach { filme ->

                cartão val =
                    criarCard(filme)

                linha.addView(
                    cartão,
                    LinearLayout.LayoutParams(
                        0,
                        dp(335),
                        1f
                    ).aplicar {

                        margemesquerda =
                            dp(4)

                        margemDireita =
                            dp(4)

                        margemInferior =
                            dp(12)
                    }
                )

                cardsAtuais.add(card)
            }

            conteudo.addView(linha)
        }

        se (cardsAtuais.isNotEmpty()) {

            cartasAtuais[0]
                .requestFocus()
        }
    }

    diversão privada criarCard(
        filme: Filme
    ): Visualizar {

        cartão val =
            FrameLayout(isto)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(falso)

        val imagem =
            ImageView(this)

        tipo de escala da imagem =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            filme.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            ).aplicar {
                gravidade = Gravidade.TOPO
            }
        )

        val informacoes =
            LinearLayout(isto)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.gravity =
            Gravidade.CENTRO_VERTICAL

        informações.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informações.setBackgroundColor(
            Cor.argb(
                235,
                10,
                10,
                10
            )
        )

        val titulo =
            TextView(this)

        título.texto =
            filme.título

        título.setTextColor(
            Cor: BRANCA
        )

        tamanho.texto do título = 14f

        título.definirTipo(
            nulo,
            Tipo de letra.NEGRO
        )

        titulo.maxLines = 1

        tamanho da elipse do título =
            TextUtils.TruncateAt.END

        informações.adicionarVisualização(
            título,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val =
            TextView(this)

        detalhes.texto =
            "${filme.ano} â€¢ ${filme.categoria}"

        detalhes.setTextColor(
            Cor: Cinza Claro
        )

        detalhes.textSize = 12f

        informações.adicionarVisualização(
            ,
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
            ).aplicar {
                gravidade = Gravidade.FUNDO
            }
        )

        val borda =
            Ver(isto)

        borda.fundo =
            criarBordaVermelha()

        borda.visibilidade =
            Ver.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
            _, foco ->

            borda.visibilidade =
                se (foco) {
                    Visualizar.VISÍVEL
                } outro {
                    Ver.GONE
                }

            card.background =
                criarFundoCard(foco)

            se (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            abrirVídeo(
                filme.título,
                filme.vídeo,
                filme.capa
            )
        }

        cartão de devolução
    }

    diversão privada mostrarListaSeries(
        lista: Lista<Série>
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        se (lista.isEmpty()) {

            val REM =
                TextView(this)

            texto =
                "Nenhuma série encontrada"

            vazio.setTextColor(
                Cor: BRANCA
            )

            vazio.textSize = 20f

            gravidade =
                Gravity.CENTER

            conteudo.addView(
                vazio,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(100)
                )
            )

            retornar
        }

        lista.chunked(5).forEach { grupo ->

            val linha =
                LinearLayout(isto)

            linha.orientação =
                LinearLayout.HORIZONTAL

            linha.layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(350)
                )

            grupo.forEach { série ->

                cartão val =
                    criarCardSerie(serie)

                linha.addView(
                    cartão,
                    LinearLayout.LayoutParams(
                        0,
                        dp(335),
                        1f
                    ).aplicar {

                        margemesquerda =
                            dp(4)

                        margemDireita =
                            dp(4)

                        margemInferior =
                            dp(12)
                    }
                )

                cardsAtuais.add(card)
            }

            conteudo.addView(linha)
        }

        se (cardsAtuais.isNotEmpty()) {

            cartasAtuais[0]
                .requestFocus()
        }
    }privado divertido criarCardSerie(
        série: Série
    ): Visualizar {

        cartão val =
            FrameLayout(isto)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(falso)

        val imagem =
            ImageView(this)

        tipo de escala da imagem =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            série.capa,
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
            LinearLayout(isto)

        informacoes.orientation =
            LinearLayout.VERTICAL

        informacoes.gravity =
            Gravidade.CENTRO_VERTICAL

        informações.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informações.setBackgroundColor(
            Cor.argb(
                235,
                10,
                10,
                10
            )
        )

        val titulo =
            TextView(this)

        título.texto =
            serie.titulo

        título.setTextColor(
            Cor: BRANCA
        )

        tamanho.texto do título = 14f

        título.definirTipo(
            nulo,
            Tipo de letra.NEGRO
        )

        titulo.maxLines = 1

        tamanho da elipse do título =
            TextUtils.TruncateAt.END

        informações.adicionarVisualização(
            título,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val =
            TextView(this)

        detalhes.texto =
            "${serie.categoria} â€¢ " +
            "${serie.temporadas.size} temporada(s)"

        detalhes.setTextColor(
            Cor: Cinza Claro
        )

        detalhes.textSize = 12f

        informações.adicionarVisualização(
            ,
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
            ).aplicar {
                gravidade = Gravidade.FUNDO
            }
        )

        val borda =
            Ver(isto)

        borda.fundo =
            criarBordaVermelha()

        borda.visibilidade =
            Ver.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
            _, foco ->

            borda.visibilidade =
                se (foco) {
                    Visualizar.VISÍVEL
                } outro {
                    Ver.GONE
                }

            card.background =
                criarFundoCard(foco)

            se (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            históricoConteudo.adicionar {
                mostrarListaSeries(séries)
            }

            mostrarTemporadas(serie)
        }

        cartão de devolução
    }

    diversão privada mostrarTemporadas(
        série: Série
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        série.temporadas
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(isto)

                linha.orientação =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { temporada ->

                    cartão val =
                        criarCardTemporada(
                            série,
                            temporada
                        )

                    linha.addView(
                        cartão,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).aplicar {

                            margemesquerda =
                                dp(4)

                            margemDireita =
                                dp(4)

                            margemInferior =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(card)
                }

                conteudo.addView(linha)
            }

        se (cardsAtuais.isNotEmpty()) {

            cartasAtuais[0]
                .requestFocus()
        }
    }

    diversão privada criarCardTemporada(
        série: Série,
        temporada: Temporada
    ): Visualizar {

        cartão val =
            FrameLayout(isto)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(falso)

        val imagem =
            ImageView(this)

        tipo de escala da imagem =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            série.capa,
            imagem
        )

        card.addView(
            imagem,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(275)
            )
        )

        val titulo =
            TextView(this)

        título.texto =
            "Temporada ${temporada.numero}"

        título.setTextColor(
            Cor: BRANCA
        )

        tamanho.texto do título = 16f

        título.definirTipo(
            nulo,
            Tipo de letra.NEGRO
        )

        título.gravidade =
            Gravity.CENTER

        título.definirCorDeFundo(
            Cor.argb(
                235,
                10,
                10,
                10
            )
        )

        card.addView(
            título,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).aplicar {
                gravidade = Gravidade.FUNDO
            }
        )

        val borda =
            Ver(isto)

        borda.fundo =
            criarBordaVermelha()

        borda.visibilidade =
            Ver.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
            _, foco ->

            borda.visibilidade =
                se (foco) {
                    Visualizar.VISÍVEL
                } outro {
                    Ver.GONE
                }

            card.background =
                criarFundoCard(foco)

            se (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            históricoConteudo.adicionar {
                mostrarTemporadas(serie)
            }

            mostrarEpisódios(
                série,
                temporada
            )
        }

        cartão de devolução
    }diversão privada mostrarEpisódios(
        série: Série,
        temporada: Temporada
    ) {

        conteudo.removeAllViews()

        cardsAtuais.clear()

        indiceCardAtual = 0

        temporada.episódios
            .chunked(5)
            .forEach { grupo ->

                val linha =
                    LinearLayout(isto)

                linha.orientação =
                    LinearLayout.HORIZONTAL

                linha.layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(350)
                    )

                grupo.forEach { episódio ->

                    cartão val =
                        criarCardEpisódio(
                            série,
                            episódio
                        )

                    linha.addView(
                        cartão,
                        LinearLayout.LayoutParams(
                            0,
                            dp(335),
                            1f
                        ).aplicar {

                            margemesquerda =
                                dp(4)

                            margemDireita =
                                dp(4)

                            margemInferior =
                                dp(12)
                        }
                    )

                    cardsAtuais.add(card)
                }

                conteudo.addView(linha)
            }

        se (cardsAtuais.isNotEmpty()) {

            cartasAtuais[0]
                .requestFocus()
        }
    }

    diversão privada criarCardEpisódio(
        série: Série,
        episódio: Episódio
    ): Visualizar {

        cartão val =
            FrameLayout(isto)

        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.isClickable = true

        card.background =
            criarFundoCard(falso)

        val imagem =
            ImageView(this)

        tipo de escala da imagem =
            ImageView.ScaleType.FIT_CENTER

        carregarImagem(
            série.capa,
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
            TextView(this)

        informacoes.text =
            "EP ${episodio.numero} â€¢ " +
            episódio.título

        informacoes.setTextColor(
            Cor: BRANCA
        )

        informacoes.textSize = 14f

        informacoes.setTypeface(
            nulo,
            Tipo de letra.NEGRO
        )

        informacoes.gravity =
            Gravidade.CENTRO_VERTICAL

        informações.setPadding(
            dp(8),
            dp(3),
            dp(8),
            dp(3)
        )

        informacoes.maxLines = 2

        informacoes.ellipsize =
            TextUtils.TruncateAt.END

        informações.setBackgroundColor(
            Cor.argb(
                235,
                10,
                10,
                10
            )
        )

        card.addView(
            informacoes,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            ).aplicar {
                gravidade = Gravidade.FUNDO
            }
        )

        val borda =
            Ver(isto)

        borda.fundo =
            criarBordaVermelha()

        borda.visibilidade =
            Ver.GONE

        card.addView(
            borda,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        card.setOnFocusChangeListener {
            _, foco ->

            borda.visibilidade =
                se (foco) {
                    Visualizar.VISÍVEL
                } outro {
                    Ver.GONE
                }

            card.background =
                criarFundoCard(foco)

            se (foco) {

                indiceCardAtual =
                    cardsAtuais.indexOf(card)
            }
        }

        card.setOnClickListener {

            abrirVídeo(
                "${serie.titulo} - " +
                    "EP ${episodio.numero}",
                episódio.vídeo,
                série.capa
            )
        }

        cartão de devolução
    }

    diversão privada abrirVídeo(
        título: Corda,
        Vídeo: Corda,
        capa: Corda
    ) {

        se (video.isBlank()) {

            Toast.makeText(
                esse,
                "Vídeo ainda não disponível",
                Torrada.COMPRIMENTO_CURTO
            ).mostrar()

            retornar
        }

        val intent =
            Intenção(
                esse,
                PlayerActivity::class.java
            )

        intent.putExtra(
            "URL_DO_VÍDEO",
            vídeo
        )

        intent.putExtra(
            "TÍTULO_DO_VÍDEO",
            título
        )

        intent.putExtra(
            "CAPA_DE_VÍDEO",
            capa
        )

        iniciarAtividade(intenção)
    }
        diversão privada adicionarItemMenu(
        texto: String,
        acao: () -> Unidade
    ) {

        item val =
            TextView(this)

        item.texto =
            texto

        item.setTextColor(
            Cor: BRANCA
        )

        item.textSize = 16f

        item.gravidade =
            Gravidade.CENTRO_VERTICAL

        item.setPadding(
            dp(18),
            0,
            dp(12),
            0
        )

        item.isFocusable = true
        item.isFocusableInTouchMode = true
        item.isClickable = true

        item.background =
            criarFundoCard(falso)

        item.setOnFocusChangeListener {
            _, foco ->

            item.background =
                criarFundoCard(foco)
        }

        item.setOnClickListener {
            acao()
        }

        menuConteúdo.adicionarVisualização(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).aplicar {
                margemInferior = dp(4)
            }
        )

        itensMenuFoco.adicionar(item)
    }

    função privada abrirMenu() {

        se (menuAberto) retornar

        menuAberto = verdadeiro

        itensMenuFoco.limpar()

        menuLateral =
            LinearLayout(isto)

        menuLateral.orientação =
            LinearLayout.VERTICAL

        menuLateral.setBackgroundColor(
            Cor.argb(
                245,
                5,
                5,
                5
            )
        )

        val params =
            FrameLayout.LayoutParams(
                dp(360),
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        params.gravity =
            Gravity.START ou Gravity.TOP

        raiz.addView(
            menuLateral,
            parâmetros
        )

        val cabecalho =
            LinearLayout(isto)

        cabecalho.orientação =
            LinearLayout.HORIZONTAL

        cabecalho.gravidade =
            Gravidade.CENTRO_VERTICAL

        cabecalho.setPadding(
            dp(15),
            dp(8),
            dp(10),
            dp(8)
        )

        menuLateral.addView(
            cabecalho,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
            )
        )

        val tituloMenu =
            TextView(this)

        tituloMenu.text =
            "CARDÁPIO DO LOBO"

        tituloMenu.setTextColor(
            Cor: BRANCA
        )

        tituloMenu.textSize = 21f

        tituloMenu.setTypeface(
            nulo,
            Tipo de letra.NEGRO
        )

        tituloMenu.gravity =
            Gravidade.CENTRO_VERTICAL

        cabecalho.addView(
            títuloMenu,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        botaoFecharMenu =
            TextView(this)

        botaoFecharMenu.text =
            "âœ•"

        botaoFecharMenu.setTextColor(
            Cor: BRANCA
        )

        botaoFecharMenu.textSize = 24f

        botaoFecharMenu.gravity =
            Gravity.CENTER

        botaoFecharMenu.isFocusable =
            verdadeiro

        botaoFecharMenu.isFocusableInTouchMode =
            verdadeiro

        botaoFecharMenu.isClickable =
            verdadeiro

        botaoFecharMenu.background =
            criarFundoCard(falso)

        botaoFecharMenu.setOnFocusChangeListener {
            _, foco ->

            botaoFecharMenu.background =
                criarFundoCard(foco)
        }

        botaoFecharMenu.setOnClickListener {
            fecharMenu()
        }

        cabecalho.addView(
            botaoFecharMenu,
            LinearLayout.LayoutParams(
                dp(55),
                dp(48)
            )
        )

        menuRolar =
            ScrollView(isto)

        menuScroll.isFocusable = false

        menuConteúdo =
            LinearLayout(isto)

        menuConteudo.orientação =
            LinearLayout.VERTICAL

        menuConteúdo.setPadding(
            dp(10),
            dp(5),
            dp(10),
            dp(20)
        )

        menuScroll.addView(
            menuConteúdo,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        menuLateral.addView(
            menuRolar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        adicionarItemMenu(
            "ðŸ”„ Atualizar catálogo"
        ) {
            fecharMenu()
            Toast.makeText(
                esse,
                "Atualizando catálogo...",
                Torrada.COMPRIMENTO_CURTO
            ).mostrar()
            carregarFilmes()
        }

        adicionarItemMenu(
            "â–¶ Continuar assistindo"
        ) {

            fecharMenu()

            Toast.makeText(
                esse,
                "Continue a",
                Torrada.COMPRIMENTO_CURTO
            ).mostrar()
        }

        adicionarItemMenu(
            "â˜… Favoritos (${favoritos.size})"
        ) {

            fecharMenu()

            val lista =
                filmes.filter {
                    favoritos.contém(
                        it.título
                    )
                }

            mostrarListaCards(lista)
        }

        adicionarItemMenu(
            "âŒ• Pesquisa"
        ) {
            abrirPesquisa()
        }

        adicionarTituloMenu(
            "FILMES"
        )

        adicionarItemMenu(
            "ðŸŽ¬ Todos os filmes (${filmes.size})"
        ) {

            fecharMenu()
            mostrarListaCards(filmes)
        }

        adicionarItemMenu(
            "ðŸ”¥ Ação (${filmes.count {
                it.categoria.equals("Açã", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Açã", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ ¹ Aventura (${filmes.count {
                it.categoria.equals("Aventura", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Aventura", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ§¸ Animação (${filmes.count {
                it.categoria.equals("Animação", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Animação", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ˜‚ Comédia (${filmes.count {
                it.categoria.equals("Comédia", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Comédia", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸŽ Drama (${filmes.count {
                it.categoria.equals("Drama", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Drama", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ'» Terror (${filmes.count {
                it.categoria.equals("Terror", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("Terror", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸš€ Ficção (${filmes.count {
                it.categoria.equals("FicÃ§Ã£o", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaCards(
                filmes.filter {
                    it.categoria.equals("FicÃ§Ã£o", true)
                }
            )
        }

        adicionarTituloMenu(
            "SÉRIES"
        )

        adicionarItemMenu(
            "ðŸ“º Todas as séries (${series.size})"
        ) {

            fecharMenu()
            mostrarListaSeries(séries)
        }

        adicionarItemMenu(
            "ðŸ”¥ Açã§çã (${series.count {
                it.categoria.equals("Açã", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                série.filtrar {
                    it.categoria.equals("Açã", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ ¹ Aventura (${series.count {
                it.categoria.equals("Aventura", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                série.filtrar {
                    it.categoria.equals("Aventura", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ˜‚ Comédia (${series.count {
                it.categoria.equals("Comédia", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                série.filtrar {
                    it.categoria.equals("Comédia", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸŽ Drama (${series.count {
                it.categoria.equals("Drama", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                série.filtrar {
                    it.categoria.equals("Drama", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ'» Terror (${series.count {
                it.categoria.equals("Terror", true)
            }})"
        ) {

            fecharMenu()

            mostrarListaSeries(
                série.filtrar {
                    it.categoria.equals("Terror", true)
                }
            )
        }// ===============================
        // DORAMAS
        // ===============================

        adicionarTituloMenu("DORAMAS")

        adicionarItemMenu(
            "ðŸ“º Todos os Doramas (${doramas.size})"
        ) {
            fecharMenu()
            mostrarListaSeries(doramas)
        }

        adicionarItemMenu(
            "ðŸ'– Romance (${doramas.count {
                it.categoria.equals("romance", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("romance", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ”¥ Ação (${doramas.count {
                it.categoria.equals("acao", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("acao", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ˜‚ Comédia (${doramas.count {
                it.categoria.equals("comedia", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("comedia", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ'» Terror (${doramas.count {
                it.categoria.equals("terror", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                doramas.filter {
                    it.categoria.equals("terror", true)
                }
            )
        }


        // ===============================
        // ANIME
        // ===============================

        adicionarTituloMenu("ANIME")

        adicionarItemMenu(
            "ðŸ ¥ Todos os Animes (${animes.size})"
        ) {
            fecharMenu()
            mostrarListaSeries(animes)
        }

        adicionarItemMenu(
            "ðŸ”¥ Açã§çã (${animes.count {
                it.categoria.equals("acao", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("acao", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ˜‚ Comédia (${animes.count {
                it.categoria.equals("comedia", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("comedia", true)
                }
            )
        }

        adicionarItemMenu(
            "ðŸ'» Terror (${animes.count {
                it.categoria.equals("terror", true)
            }})"
        ) {
            fecharMenu()
            mostrarListaSeries(
                animes.filter {
                    it.categoria.equals("terror", true)
                }
            )
        }


        // ===============================
        // FOCO INICIAL DO MENU
        // ===============================

        botaoFecharMenu.requestFocus()

    }


    // ===============================
    // TÍTULO DO MENU
    // ===============================

    private fun adicionarTituloMenu(
        texto: String
    ) {

        val titulo = TextView(this)

        título.texto = texto

        tamanho.texto do título = 16f

        título.setTextColor(
            Cor: Cinza Claro
        )

        título.setPadding(
            dp(18),
            dp(18),
            dp(12),
            dp(8)
        )

        título.éFocável = falso

        menuConteúdo.adicionarVisualização(
            título
        )
    }


    // ===============================
    // MENU FECHAR
    // ===============================

    função privada fecharMenu() {

        menuAberto = falso

        menuLateral.visibility =
            Ver.GONE

        botaoMenu.requestFocus()
    }


    // ===============================
    // MOVIMENTAÇÃO DO MENU
    // ===============================

    Menu de movimentação de diversão privada (
        Direção: Int
    ) {

        se (itensMenuFoco.isEmpty()) {
            retornar
        }

        var índice =
            itensMenuFoco.indexOfFirst {
                ele.temFoco()
            }

        se (índice < 0) {
            índice = 0
        }

        índice += direção

        se (índice < 0) {
            índice =
                itensMenuFoco.size - 1
        }

        se (índice >= itensMenuFoco.size) {
            índice = 0
        }

        val próximo =
            itensMenuFoco[índice]

        próximo.requestFocus()

        ajustarScrollMenu(
            próximo
        )
    }


    // ===============================
    // AJUSTAR ROLAR PARA MENU
    // ===============================

    diversão privada ajustarScrollMenu(
        Visualizar: Visualizar
    ) {

        ver.post {

            menuScroll.smoothScrollTo(
                0,
                ver.topo
            )
        }
    }


    // ===============================
    // MOVIMENTAÇÃO DOS CARDS
    // ===============================

    Cartão de mudança de diversão privada (
        Direção: Int
    ) {

        se (cardsAtuais.isEmpty()) {
            retornar
        }

        var índice =
            cardsAtuais.indexOfFirst {
                ele.temFoco()
            }

        se (índice < 0) {
            índice = indiceCardAtual
        }

        índice += direção

        se (índice < 0) {
            índice = 0
        }

        se (índice >= cardsAtuais.size) {
            índice =
                cardsAtuais.size - 1
        }

        indiceCardAtual = índice

        cardsAtuais[indice].requestFocus()
    }


    // ===============================
    // PESQUISA
    // ===============================

    função privada abrirPesquisa() {

        val campo =
            EditText(isto)

        campo.dica =
            "Digite o nome..."

        campo.textSize =
            20f

        campo.setSingleLine(true)

        campo.isFocusable = true
        campo.isFocusableInTouchMode = true

        campo.requestFocus()

        Toast.makeText(
            esse,
            "Use a busca pelo menu",
            Torrada.COMPRIMENTO_CURTO
        ).mostrar()
    }


    // ===============================
    // CONTROLE DO D-PAD
    // ===============================

    substituir fun dispatchKeyEvent(
        evento: KeyEvent
    ): Booleano {

        se (
            evento.ação ==
            KeyEvent.AÇÃO_DOWN
        ) {

            quando (evento.keyCode) {

                KeyEvent.KEYCODE_MENU -> {

                    se (menuAberto) {
                        fecharMenu()
                    } outro {
                        abrirMenu()
                    }

                    retornar verdadeiro
                }


                KeyEvent.KEYCODE_DPAD_LEFT -> {

                    se (!menuAberto) {

                        moverCard(-1)

                        retornar verdadeiro
                    }
                }


                KeyEvent.KEYCODE_DPAD_RIGHT -> {

                    se (!menuAberto) {

                        moverCard(1)

                        retornar verdadeiro
                    }
                }


                KeyEvent.KEYCODE_DPAD_UP -> {

                    se (menuAberto) {

                        moverMenu(-1)

                        retornar verdadeiro
                    }
                }


                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    se (menuAberto) {

                        moverMenu(1)

                        retornar verdadeiro
                    }
                }


                KeyEvent.KEYCODE_BACK -> {

                    se (menuAberto) {

                        fecharMenu()

                        retornar verdadeiro
                    }
                }
            }
        }

        retornar super.dispatchKeyEvent(
            evento
        )
    }

}
