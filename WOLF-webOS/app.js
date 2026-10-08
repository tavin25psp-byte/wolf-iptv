/* =========================================================
   WOLF IPTV - WEBOS
   Baseado no MainActivity.kt do Android
   ========================================================= */

"use strict";

/* =========================================================
   CONFIGURAÇÃO
   ========================================================= */

const BASE_URL =
    "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/";

const CATALOGO_URL =
    BASE_URL + "catalogo.json";

const ARQUIVOS_SERIES = [
    "series.json",
    "serie.json",
    "s%C3%A9rie.json"
];

const ARQUIVOS_DORAMAS = [
    "doramas.json",
    "Doramas.json"
];

const ARQUIVOS_ANIMES = [
    "animes.json",
    "anime.json",
    "Animes.json",
    "Anime.json"
];

const ARQUIVOS_DESENHOS = [
    "desenho.json",
    "desenhos.json",
    "Desenho.json",
    "Desenhos.json"
];

const FUNDO_WOLF =
    "https://i.postimg.cc/Ghk8PP7w/wolf.png";

/*
 * O Android trabalha com 5 cards por fileira.
 */
const COLUNAS = 5;


/* =========================================================
   ESTADO
   ========================================================= */

const state = {

    filmes: [],
    series: [],
    doramas: [],
    animes: [],
    desenhos: [],

    favoritos: new Set(),

    cards: [],

    currentIndex: 0,

    menuOpen: false,

    menuItems: [],

    history: [],

    currentSeriesList: [],

    currentPage: "filmes",

    playerOpen: false,

    searchOpen: false

};


/* =========================================================
   ELEMENTOS
   ========================================================= */

const $ = (id) =>
    document.getElementById(id);

const content =
    $("content");

const contentScroll =
    $("contentScroll");

const menuButton =
    $("menuButton");

const sideMenu =
    $("sideMenu");

const closeMenu =
    $("closeMenu");

const menuContent =
    $("menuContent");

const searchOverlay =
    $("searchOverlay");

const searchInput =
    $("searchInput");

const playerOverlay =
    $("playerOverlay");

const videoPlayer =
    $("videoPlayer");

const playerTitle =
    $("playerTitle");

const playerLoading =
    $("playerLoading");

const playerError =
    $("playerError");

const closePlayer =
    $("closePlayer");

const toastElement =
    $("toast");


/* =========================================================
   INICIALIZAÇÃO
   ========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    iniciar
);


function iniciar() {

    carregarFavoritos();

    configurarFundo();

    configurarEventos();

    carregarCatalogo();

}


/* =========================================================
   FUNDO
   ========================================================= */

function configurarFundo() {

    const background =
        $("background");

    background.style.backgroundImage =
        `url("${FUNDO_WOLF}")`;

}


/* =========================================================
   EVENTOS
   ========================================================= */

function configurarEventos() {

    menuButton.addEventListener(
        "click",
        abrirMenu
    );

    closeMenu.addEventListener(
        "click",
        fecharMenu
    );

    closePlayer.addEventListener(
        "click",
        fecharPlayer
    );

    document.addEventListener(
        "keydown",
        tratarTecla
    );

    searchInput.addEventListener(
        "input",
        tratarPesquisa
    );

    searchInput.addEventListener(
        "keydown",
        function(event) {

            if (event.key === "Enter") {

                event.preventDefault();

                fecharPesquisa();

            }

        }
    );

    videoPlayer.addEventListener(
        "waiting",
        function() {

            playerLoading.style.display =
                "block";

        }
    );

    videoPlayer.addEventListener(
        "playing",
        function() {

            playerLoading.style.display =
                "none";

            playerError.style.display =
                "none";

        }
    );

    videoPlayer.addEventListener(
        "error",
        function() {

            playerLoading.style.display =
                "none";

            playerError.style.display =
                "block";

        }
    );

}


/* =========================================================
   NORMALIZAÇÃO
   ========================================================= */

function normalizarTexto(texto) {

    return String(texto || "")
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .toLowerCase()
        .trim();

}


/* =========================================================
   CARREGAMENTO DO CATÁLOGO
   ========================================================= */

async function carregarCatalogo() {

    mostrarCarregando(
        "Carregando catálogo..."
    );

    try {

        const catalogo =
            await baixarJSON(
                CATALOGO_URL
            );

        const novosFilmes =
            lerFilmes(
                catalogo
            );

        /*
         * Mantém o mesmo comportamento do Android:
         * filmes mais novos primeiro.
         */
        novosFilmes.sort(
            (a, b) => {

                const anoA =
                    Number(a.ano || 0);

                const anoB =
                    Number(b.ano || 0);

                if (anoA !== anoB) {
                    return anoB - anoA;
                }

                return a.titulo.localeCompare(
                    b.titulo,
                    "pt-BR"
                );

            }
        );


        const textoSeries =
            await baixarPrimeiro(
                ARQUIVOS_SERIES
            );

        const textoDoramas =
            await baixarPrimeiro(
                ARQUIVOS_DORAMAS
            );

        const textoAnimes =
            await baixarPrimeiro(
                ARQUIVOS_ANIMES
            );

        const textoDesenhos =
            await baixarPrimeiro(
                ARQUIVOS_DESENHOS
            );


        const novasSeries =
            lerSeriesArquivo(
                textoSeries,
                "series",
                catalogo
            );

        const novosDoramas =
            lerSeriesArquivo(
                textoDoramas,
                "doramas",
                catalogo
            );

        const novosAnimes =
            lerSeriesArquivo(
                textoAnimes,
                "animes",
                catalogo
            );

        const novosDesenhos =
            lerDesenhosArquivo(
                textoDesenhos
            );


        state.filmes =
            novosFilmes;

        state.series =
            novasSeries;

        state.doramas =
            novosDoramas;

        state.animes =
            novosAnimes;

        state.desenhos =
            novosDesenhos;


        mostrarFilmes(
            state.filmes
        );


        mostrarToast(
            "Catálogo carregado: " +
            state.filmes.length +
            " filmes, " +
            state.series.length +
            " séries, " +
            state.doramas.length +
            " doramas, " +
            state.animes.length +
            " animes e " +
            state.desenhos.length +
            " desenhos"
        );


    } catch (erro) {

        console.error(
            "Erro no catálogo:",
            erro
        );

        mostrarErroCatalogo(
            erro
        );

    }

}


/* =========================================================
   DOWNLOAD JSON
   ========================================================= */

async function baixarJSON(url) {

    const resposta =
        await fetch(
            url + "?v=" + Date.now(),
            {
                method: "GET",
                cache: "no-store"
            }
        );

    if (!resposta.ok) {

        throw new Error(
            "HTTP " +
            resposta.status +
            " - " +
            url
        );

    }

    return await resposta.json();

}


async function baixarTexto(url) {

    try {

        const resposta =
            await fetch(
                url + "?v=" + Date.now(),
                {
                    method: "GET",
                    cache: "no-store"
                }
            );

        if (!resposta.ok) {
            return null;
        }

        return await resposta.text();

    } catch (erro) {

        console.warn(
            "Falha:",
            url,
            erro
        );

        return null;

    }

}


async function baixarPrimeiro(nomes) {

    for (const nome of nomes) {

        const texto =
            await baixarTexto(
                BASE_URL + nome
            );

        if (
            texto &&
            texto.trim()
        ) {

            return texto;

        }

    }

    return null;

}


/* =========================================================
   LEITURA DE FILMES
   ========================================================= */

function lerFilmes(json) {

    const lista =
        Array.isArray(json)
            ? json
            : (
                Array.isArray(json?.filmes)
                    ? json.filmes
                    : []
            );

    return lista
        .map(item => {

            return {

                titulo:
                    String(
                        item?.titulo || ""
                    ),

                ano:
                    Number(
                        item?.ano || 0
                    ),

                categoria:
                    String(
                        item?.categoria || ""
                    ),

                capa:
                    String(
                        item?.capa || ""
                    ),

                video:
                    String(
                        item?.video || ""
                    )

            };

        })
        .filter(
            item =>
                item.titulo.trim() !== ""
        );

}


/* =========================================================
   LEITURA DE SÉRIES
   ========================================================= */

function lerSeriesArquivo(
    texto,
    chave,
    catalogoAntigo
) {

    let lista = null;

    if (texto) {

        try {

            const json =
                JSON.parse(
                    texto.trim()
                );

            if (Array.isArray(json)) {

                lista = json;

            } else if (
                Array.isArray(
                    json?.[chave]
                )
            ) {

                lista =
                    json[chave];

            }

        } catch (erro) {

            console.warn(
                "JSON inválido:",
                chave,
                erro
            );

        }

    }


    /*
     * Compatibilidade com o formato antigo
     * dentro do catalogo.json.
     */
    if (!lista && catalogoAntigo) {

        if (
            Array.isArray(
                catalogoAntigo[chave]
            )
        ) {

            lista =
                catalogoAntigo[chave];

        }

    }


    if (!Array.isArray(lista)) {

        return [];

    }


    return lista
        .map(
            lerSerie
        )
        .filter(
            serie =>
                serie.titulo.trim() !== ""
        );

}


/* =========================================================
   LEITURA DE UMA SÉRIE
   ========================================================= */

function lerSerie(item) {

    const temporadasRaw =
        Array.isArray(
            item?.temporadas
        )
            ? item.temporadas
            : [];


    const temporadas =
        temporadasRaw.map(
            (temporada, index) => {

                const episodiosRaw =
                    Array.isArray(
                        temporada?.episodios
                    )
                        ? temporada.episodios
                        : [];


                const episodios =
                    episodiosRaw
                        .map(
                            (episodio, epIndex) => {

                                return {

                                    numero:
                                        Number(
                                            episodio?.numero ??
                                            epIndex + 1
                                        ),

                                    titulo:
                                        String(
                                            episodio?.titulo ||
                                            ""
                                        ),

                                    video:
                                        String(
                                            episodio?.video ||
                                            ""
                                        )

                                };

                            }
                        );


                return {

                    numero:
                        Number(
                            temporada?.numero ??
                            index + 1
                        ),

                    episodios

                };

            }
        );


    return {

        titulo:
            String(
                item?.titulo || ""
            ),

        categoria:
            String(
                item?.categoria || ""
            ),

        capa:
            String(
                item?.capa || ""
            ),

        temporadas

    };

}


/* =========================================================
   DESENHOS
   ========================================================= */

function lerDesenhosArquivo(texto) {

    if (!texto) {
        return [];
    }

    try {

        const json =
            JSON.parse(
                texto.trim()
            );

        let lista = [];

        if (Array.isArray(json)) {

            lista = json;

        } else if (
            Array.isArray(json?.desenhos)
        ) {

            lista = json.desenhos;

        } else if (
            Array.isArray(json?.filmes)
        ) {

            lista = json.filmes;

        }


        return lerFilmes(
            lista
        );

    } catch (erro) {

        console.warn(
            "Erro nos desenhos:",
            erro
        );

        return [];

    }

}


/* =========================================================
   LOADING
   ========================================================= */

function mostrarCarregando(texto) {

    content.innerHTML = `

        <div class="loading">

            <div class="loadingWolf">
                WOLF
            </div>

            <div>
                ${escaparHTML(texto)}
            </div>

        </div>

    `;

}


/* =========================================================
   ERRO
   ========================================================= */

function mostrarErroCatalogo(erro) {

    content.innerHTML = `

        <div class="catalog-error">

            <strong>ERRO NO CATÁLOGO</strong>

            <br><br>

            ${escaparHTML(
                erro?.message ||
                "Erro desconhecido"
            )}

            <br><br>

            Verifique sua conexão com a internet.

        </div>

    `;

}


/* =========================================================
   FILMES
   ========================================================= */

function mostrarFilmes(lista) {

    state.currentPage =
        "filmes";

    state.currentIndex =
        0;

    state.cards = [];

    content.innerHTML = "";

    if (!lista || lista.length === 0) {

        mostrarVazio(
            "Nenhum filme encontrado"
        );

        return;

    }


    const grid =
        criarGrid();


    lista.forEach(
        filme => {

            const card =
                criarCardFilme(
                    filme
                );

            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   CARD FILME
   ========================================================= */

function criarCardFilme(filme) {

    const card =
        document.createElement(
            "div"
        );

    card.className =
        "card";

    card.tabIndex = 0;

    card.dataset.type =
        "filme";

    const imagem =
        criarImagem(
            filme.capa,
            filme.titulo
        );

    card.appendChild(
        imagem
    );


    const info =
        document.createElement(
            "div"
        );

    info.className =
        "card-info";


    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "card-title";

    titulo.textContent =
        filme.titulo;


    const detalhes =
        document.createElement(
            "div"
        );

    detalhes.className =
        "card-details";

    detalhes.textContent =
        `${filme.ano} • ${filme.categoria}`;


    info.appendChild(
        titulo
    );

    info.appendChild(
        detalhes
    );

    card.appendChild(
        info
    );


    card.addEventListener(
        "click",
        function() {

            abrirVideo(
                filme.titulo,
                filme.video,
                filme.capa
            );

        }
    );


    card.addEventListener(
        "focus",
        function() {

            state.currentIndex =
                state.cards.indexOf(
                    card
                );

            manterCardVisivel(
                card
            );

        }
    );


    return card;

}


/* =========================================================
   SÉRIES
   ========================================================= */

function mostrarSeries(lista) {

    state.currentPage =
        "series";

    state.currentSeriesList =
        lista || [];

    state.currentIndex =
        0;

    state.cards = [];

    content.innerHTML = "";


    if (
        !lista ||
        lista.length === 0
    ) {

        mostrarVazio(
            "Nenhuma série encontrada"
        );

        return;

    }


    const grid =
        criarGrid();


    lista.forEach(
        serie => {

            const card =
                criarCardSerie(
                    serie
                );

            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   CARD SÉRIE
   ========================================================= */

function criarCardSerie(serie) {

    const card =
        document.createElement(
            "div"
        );

    card.className =
        "card";

    card.tabIndex = 0;

    const imagem =
        criarImagem(
            serie.capa,
            serie.titulo
        );

    card.appendChild(
        imagem
    );


    const info =
        document.createElement(
            "div"
        );

    info.className =
        "card-info";


    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "card-title";

    titulo.textContent =
        serie.titulo;


    const detalhes =
        document.createElement(
            "div"
        );

    detalhes.className =
        "card-details";

    detalhes.textContent =
        `${serie.categoria} • ${serie.temporadas.length} temporada(s)`;


    info.appendChild(
        titulo
    );

    info.appendChild(
        detalhes
    );

    card.appendChild(
        info
    );


    card.addEventListener(
        "click",
        function() {

            state.history.push(
                function() {

                    mostrarSeries(
                        state.currentSeriesList
                    );

                }
            );

            mostrarTemporadas(
                serie
            );

        }
    );


    card.addEventListener(
        "focus",
        function() {

            state.currentIndex =
                state.cards.indexOf(
                    card
                );

            manterCardVisivel(
                card
            );

        }
    );


    return card;

}


/* =========================================================
   TEMPORADAS
   ========================================================= */

function mostrarTemporadas(serie) {

    state.currentPage =
        "temporadas";

    state.currentIndex =
        0;

    state.cards =
        [];

    content.innerHTML = "";


    if (
        !serie.temporadas ||
        serie.temporadas.length === 0
    ) {

        mostrarVazio(
            "Nenhuma temporada encontrada"
        );

        return;

    }


    const grid =
        criarGrid();


    serie.temporadas.forEach(
        temporada => {

            const card =
                criarCardTemporada(
                    serie,
                    temporada
                );

            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   CARD TEMPORADA
   ========================================================= */

function criarCardTemporada(
    serie,
    temporada
) {

    const card =
        document.createElement(
            "div"
        );

    card.className =
        "card season-card";

    card.tabIndex = 0;


    const imagem =
        criarImagem(
            serie.capa,
            serie.titulo
        );

    card.appendChild(
        imagem
    );


    const info =
        document.createElement(
            "div"
        );

    info.className =
        "card-info";


    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "card-title season-title";

    titulo.textContent =
        `Temporada ${temporada.numero}`;


    info.appendChild(
        titulo
    );


    card.appendChild(
        info
    );


    card.addEventListener(
        "click",
        function() {

            state.history.push(
                function() {

                    mostrarTemporadas(
                        serie
                    );

                }
            );

            mostrarEpisodios(
                serie,
                temporada
            );

        }
    );


    card.addEventListener(
        "focus",
        function() {

            state.currentIndex =
                state.cards.indexOf(
                    card
                );

            manterCardVisivel(
                card
            );

        }
    );


    return card;

}


/* =========================================================
   EPISÓDIOS
   ========================================================= */

function mostrarEpisodios(
    serie,
    temporada
) {

    state.currentPage =
        "episodios";

    state.currentIndex =
        0;

    state.cards =
        [];

    content.innerHTML = "";


    if (
        !temporada.episodios ||
        temporada.episodios.length === 0
    ) {

        mostrarVazio(
            "Nenhum episódio encontrado"
        );

        return;

    }


    const grid =
        criarGrid();


    temporada.episodios.forEach(
        episodio => {

            const card =
                criarCardEpisodio(
                    serie,
                    temporada,
                    episodio
                );

            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   CARD EPISÓDIO
   ========================================================= */

function criarCardEpisodio(
    serie,
    temporada,
    episodio
) {

    const chave =
        chaveEpisodio(
            serie,
            temporada.numero,
            episodio.numero
        );


    const assistido =
        episodioAssistido(
            chave
        );


    const card =
        document.createElement(
            "div"
        );

    card.className =
        "card episode-card";

    card.tabIndex = 0;


    if (assistido) {

        card.classList.add(
            "watched"
        );

    }


    const imagem =
        criarImagem(
            serie.capa,
            serie.titulo
        );

    card.appendChild(
        imagem
    );


    if (assistido) {

        const selo =
            document.createElement(
                "div"
            );

        selo.className =
            "watched-badge";

        selo.textContent =
            "✓ ASSISTIDO";

        card.appendChild(
            selo
        );

    }


    const info =
        document.createElement(
            "div"
        );

    info.className =
        "card-info";


    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "card-title episode-title";

    titulo.textContent =
        `${assistido ? "✓ " : ""}EP ${episodio.numero} • ${episodio.titulo}`;


    info.appendChild(
        titulo
    );

    card.appendChild(
        info
    );


    card.addEventListener(
        "click",
        function() {

            if (
                episodio.video &&
                episodio.video.trim()
            ) {

                marcarEpisodio(
                    chave,
                    true
                );

            }


            atualizarCardEpisodio(
                card,
                serie,
                temporada,
                episodio,
                chave
            );


            abrirVideo(
                `${serie.titulo} - EP ${episodio.numero}`,
                episodio.video,
                serie.capa
            );

        }
    );


    /*
     * No controle LG/webOS não existe um equivalente
     * universal de "long click" igual ao Android.
     * Segurar ENTER é tratado no keydown.
     */
    card.addEventListener(
        "focus",
        function() {

            state.currentIndex =
                state.cards.indexOf(
                    card
                );

            manterCardVisivel(
                card
            );

        }
    );


    card._wolfEpisode = {
        serie,
        temporada,
        episodio,
        chave
    };


    return card;

}


/* =========================================================
   ATUALIZAR CARD EPISÓDIO
   ========================================================= */

function atualizarCardEpisodio(
    card,
    serie,
    temporada,
    episodio,
    chave
) {

    const visto =
        episodioAssistido(
            chave
        );


    if (visto) {

        card.classList.add(
            "watched"
        );

    } else {

        card.classList.remove(
            "watched"
        );

    }


    const titulo =
        card.querySelector(
            ".episode-title"
        );

    if (titulo) {

        titulo.textContent =
            `${visto ? "✓ " : ""}EP ${episodio.numero} • ${episodio.titulo}`;

    }


    const badge =
        card.querySelector(
            ".watched-badge"
        );


    if (visto && !badge) {

        const novoBadge =
            document.createElement(
                "div"
            );

        novoBadge.className =
            "watched-badge";

        novoBadge.textContent =
            "✓ ASSISTIDO";

        card.appendChild(
            novoBadge
        );

    }


    if (!visto && badge) {

        badge.remove();

    }

}


/* =========================================================
   CRIAR GRID
   ========================================================= */

function criarGrid() {

    const grid =
        document.createElement(
            "div"
        );

    grid.className =
        "cardsGrid";

    return grid;

}


/* =========================================================
   IMAGENS
   ========================================================= */

function criarImagem(
    url,
    alt
) {

    const imagem =
        document.createElement(
            "img"
        );

    imagem.className =
        "card-image";

    imagem.alt =
        alt || "WOLF";


    if (
        url &&
        String(url).trim()
    ) {

        imagem.src =
            String(url).trim();

    } else {

        imagem.src =
            FUNDO_WOLF;

    }


    imagem.loading =
        "lazy";


    imagem.onerror =
        function() {

            if (
                imagem.src !==
                FUNDO_WOLF
            ) {

                imagem.src =
                    FUNDO_WOLF;

            }

        };


    return imagem;

}


/* =========================================================
   VAZIO
   ========================================================= */

function mostrarVazio(texto) {

    content.innerHTML = `

        <div class="empty">

            ${escaparHTML(texto)}

        </div>

    `;

    state.cards =
        [];

}


/* =========================================================
   FOCO
   ========================================================= */

function focarPrimeiroCard() {

    if (
        state.cards.length === 0
    ) {

        menuButton.focus();

        return;

    }


    state.currentIndex =
        0;


    requestAnimationFrame(
        function() {

            state.cards[0].focus();

        }
    );

}


function manterCardVisivel(card) {

    if (!card) {
        return;
    }

    setTimeout(
        function() {

            card.scrollIntoView({
                behavior: "smooth",
                block: "center",
                inline: "nearest"
            });

        },
        20
    );

}


/* =========================================================
   MOVIMENTAÇÃO D-PAD
   ========================================================= */

function moverCard(direcao) {

    if (
        state.cards.length === 0
    ) {

        return;

    }


    let indice =
        state.cards.indexOf(
            document.activeElement
        );


    if (indice < 0) {

        indice =
            state.currentIndex || 0;

    }


    indice += direcao;


    if (indice < 0) {

        indice = 0;

    }


    if (
        indice >=
        state.cards.length
    ) {

        indice =
            state.cards.length - 1;

    }


    state.currentIndex =
        indice;


    state.cards[
        indice
    ].focus();

}


function moverCardLinha(
    direcao
) {

    moverCard(
        direcao * COLUNAS
    );

}


/* =========================================================
   TECLADO / CONTROLE LG
   ========================================================= */

let enterPressTimer = null;

document.addEventListener(
    "keyup",
    function(event) {

        if (
            event.key === "Enter" ||
            event.keyCode === 13
        ) {

            if (enterPressTimer) {

                clearTimeout(
                    enterPressTimer
                );

                enterPressTimer =
                    null;

            }

        }

    }
);


function tratarTecla(event) {

    const key =
        event.key;

    const code =
        event.keyCode;


    /*
     * Se o player estiver aberto.
     */
    if (state.playerOpen) {

        if (
            key === "Escape" ||
            key === "Backspace" ||
            code === 461
        ) {

            event.preventDefault();

            fecharPlayer();

        }

        return;

    }


    /*
     * Pesquisa.
     */
    if (state.searchOpen) {

        if (
            key === "Escape" ||
            key === "Backspace" ||
            code === 461
        ) {

            event.preventDefault();

            fecharPesquisa();

        }

        return;

    }


    /*
     * MENU.
     */
    if (
        key === "m" ||
        key === "M" ||
        key === "Menu" ||
        code === 18 ||
        code === 82
    ) {

        event.preventDefault();

        if (state.menuOpen) {

            fecharMenu();

        } else {

            abrirMenu();

        }

        return;

    }


    /*
     * BACK.
     */
    if (
        key === "Escape" ||
        key === "Backspace" ||
        code === 461
    ) {

        event.preventDefault();

        if (state.menuOpen) {

            fecharMenu();

            return;

        }


        if (
            state.history.length > 0
        ) {

            const voltar =
                state.history.pop();

            voltar();

            return;

        }

        return;

    }


    /*
     * Se o menu estiver aberto,
     * usamos navegação vertical.
     */
    if (state.menuOpen) {

        tratarTeclaMenu(
            event
        );

        return;

    }


    /*
     * SETAS.
     */

    switch (key) {

        case "ArrowLeft":

            event.preventDefault();

            moverCard(-1);

            break;


        case "ArrowRight":

            event.preventDefault();

            moverCard(1);

            break;


        case "ArrowUp":

            event.preventDefault();

            if (
                state.cards.includes(
                    document.activeElement
                )
            ) {

                const indice =
                    state.cards.indexOf(
                        document.activeElement
                    );

                if (
                    indice >= COLUNAS
                ) {

                    moverCardLinha(-1);

                } else {

                    menuButton.focus();

                }

            }

            break;


        case "ArrowDown":

            event.preventDefault();

            if (
                document.activeElement ===
                menuButton
            ) {

                focarPrimeiroCard();

            } else {

                moverCardLinha(1);

            }

            break;


        case "Enter":

            /*
             * O navegador normalmente dispara click
             * automaticamente. Só tratamos o caso
             * especial do episódio segurado.
             */
            tratarEnter(
                event
            );

            break;

    }

}


/* =========================================================
   ENTER / EPISÓDIO ASSISTIDO
   ========================================================= */

function tratarEnter(event) {

    const elemento =
        document.activeElement;


    if (
        !elemento ||
        !elemento.classList.contains(
            "episode-card"
        )
    ) {

        return;

    }


    /*
     * Não impedimos o click normal.
     * Um timer permite detectar ENTER segurado.
     */
    if (enterPressTimer) {

        return;

    }


    enterPressTimer =
        setTimeout(
            function() {

                enterPressTimer =
                    null;

                if (
                    document.activeElement ===
                    elemento
                ) {

                    alternarAssistido(
                        elemento
                    );

                }

            },
            800
        );

}


/* =========================================================
   ALTERNAR ASSISTIDO
   ========================================================= */

function alternarAssistido(card) {

    const dados =
        card._wolfEpisode;

    if (!dados) {
        return;
    }


    const atual =
        episodioAssistido(
            dados.chave
        );


    marcarEpisodio(
        dados.chave,
        !atual
    );


    atualizarCardEpisodio(
        card,
        dados.serie,
        dados.temporada,
        dados.episodio,
        dados.chave
    );


    mostrarToast(
        !atual
            ? "Marcado como assistido"
            : "Marcado como não assistido"
    );

}


/* =========================================================
   MENU
   ========================================================= */

function abrirMenu() {

    if (state.menuOpen) {
        return;
    }


    state.menuOpen =
        true;


    menuContent.innerHTML =
        "";

    state.menuItems =
        [];


    criarItemMenu(
        "🔄  Atualizar catálogo",
        function() {

            fecharMenu();

            mostrarToast(
                "Atualizando catálogo..."
            );

            carregarCatalogo();

        }
    );


    criarItemMenu(
        "▶  Continuar assistindo",
        function() {

            fecharMenu();

            mostrarContinuarAssistindo();

        }
    );


    criarItemMenu(
        `★  Favoritos (${state.favoritos.size})`,
        function() {

            fecharMenu();

            mostrarFavoritos();

        }
    );


    criarItemMenu(
        "⌕  Pesquisa",
        function() {

            abrirPesquisa();

        }
    );


    criarTituloMenu(
        "FILMES"
    );


    criarItemMenu(
        `🎬  Todos os filmes (${state.filmes.length})`,
        function() {

            fecharMenu();

            mostrarFilmes(
                state.filmes
            );

        }
    );


    const categoriasFilmes = [
        ["🔥", "Ação"],
        ["🏹", "Aventura"],
        ["🧸", "Animação"],
        ["😂", "Comédia"],
        ["🎭", "Drama"],
        ["👻", "Terror"],
        ["🚀", "Ficção"]
    ];


    categoriasFilmes.forEach(
        ([icone, categoria]) => {

            const quantidade =
                contarCategoria(
                    state.filmes,
                    categoria
                );


            criarItemMenu(
                `${icone}  ${categoria} (${quantidade})`,
                function() {

                    fecharMenu();

                    mostrarFilmes(
                        state.filmes.filter(
                            filme =>
                                normalizarTexto(
                                    filme.categoria
                                ) ===
                                normalizarTexto(
                                    categoria
                                )
                        )
                    );

                }
            );

        }
    );


    criarTituloMenu(
        "SÉRIES"
    );


    criarItemMenu(
        `📺  Todas as séries (${state.series.length})`,
        function() {

            fecharMenu();

            mostrarSeries(
                state.series
            );

        }
    );


    const categoriasSeries = [
        ["🔥", "Ação"],
        ["🏹", "Aventura"],
        ["😂", "Comédia"],
        ["🎭", "Drama"],
        ["👻", "Terror"]
    ];


    categoriasSeries.forEach(
        ([icone, categoria]) => {

            const quantidade =
                contarCategoria(
                    state.series,
                    categoria
                );


            criarItemMenu(
                `${icone}  ${categoria} (${quantidade})`,
                function() {

                    fecharMenu();

                    mostrarSeries(
                        state.series.filter(
                            serie =>
                                normalizarTexto(
                                    serie.categoria
                                ) ===
                                normalizarTexto(
                                    categoria
                                )
                        )
                    );

                }
            );

        }
    );


    criarTituloMenu(
        "DORAMAS"
    );


    criarItemMenu(
        `📺  Todos os Doramas (${state.doramas.length})`,
        function() {

            fecharMenu();

            mostrarSeries(
                state.doramas
            );

        }
    );


    const categoriasDoramas = [
        ["💖", "Romance"],
        ["🔥", "Ação"],
        ["😂", "Comédia"],
        ["👻", "Terror"]
    ];


    categoriasDoramas.forEach(
        ([icone, categoria]) => {

            const quantidade =
                contarCategoria(
                    state.doramas,
                    categoria
                );


            criarItemMenu(
                `${icone}  ${categoria} (${quantidade})`,
                function() {

                    fecharMenu();

                    mostrarSeries(
                        state.doramas.filter(
                            serie =>
                                normalizarTexto(
                                    serie.categoria
                                ) ===
                                normalizarTexto(
                                    categoria
                                )
                        )
                    );

                }
            );

        }
    );


    criarTituloMenu(
        "DESENHOS"
    );


    criarItemMenu(
        `🧸  Todos os desenhos (${state.desenhos.length})`,
        function() {

            fecharMenu();

            mostrarFilmes(
                state.desenhos
            );

        }
    );


    const categoriasDesenhos = [
        ["🔥", "Ação"],
        ["🏹", "Aventura"],
        ["🧸", "Animação"],
        ["😂", "Comédia"],
        ["🎭", "Drama"],
        ["👻", "Terror"],
        ["✨", "Fantasia"]
    ];


    categoriasDesenhos.forEach(
        ([icone, categoria]) => {

            const quantidade =
                contarCategoria(
                    state.desenhos,
                    categoria
                );


            criarItemMenu(
                `${icone}  ${categoria} (${quantidade})`,
                function() {

                    fecharMenu();

                    mostrarFilmes(
                        state.desenhos.filter(
                            desenho =>
                                normalizarTexto(
                                    desenho.categoria
                                ) ===
                                normalizarTexto(
                                    categoria
                                )
                        )
                    );

                }
            );

        }
    );


    criarTituloMenu(
        "ANIME"
    );


    criarItemMenu(
        `🍥  Todos os Animes (${state.animes.length})`,
        function() {

            fecharMenu();

            mostrarSeries(
                state.animes
            );

        }
    );


    const categoriasAnimes = [
        ["🔥", "Ação"],
        ["😂", "Comédia"],
        ["👻", "Terror"]
    ];


    categoriasAnimes.forEach(
        ([icone, categoria]) => {

            const quantidade =
                contarCategoria(
                    state.animes,
                    categoria
                );


            criarItemMenu(
                `${icone}  ${categoria} (${quantidade})`,
                function() {

                    fecharMenu();

                    mostrarSeries(
                        state.animes.filter(
                            anime =>
                                normalizarTexto(
                                    anime.categoria
                                ) ===
                                normalizarTexto(
                                    categoria
                                )
                        )
                    );

                }
            );

        }
    );


    sideMenu.classList.add(
        "open"
    );

    sideMenu.setAttribute(
        "aria-hidden",
        "false"
    );


    requestAnimationFrame(
        function() {

            if (
                state.menuItems.length > 0
            ) {

                state.menuItems[0].focus();

            }

        }
    );

}


/* =========================================================
   CRIAR ITEM MENU
   ========================================================= */

function criarItemMenu(
    texto,
    acao
) {

    const item =
        document.createElement(
            "button"
        );

    item.type =
        "button";

    item.className =
        "menu-item";

    item.tabIndex =
        0;

    item.textContent =
        texto;


    item.addEventListener(
        "click",
        function() {

            acao();

        }
    );


    item.addEventListener(
        "focus",
        function() {

            item.scrollIntoView({
                behavior: "smooth",
                block: "nearest"
            });

        }
    );


    menuContent.appendChild(
        item
    );

    state.menuItems.push(
        item
    );


    return item;

}


/* =========================================================
   TÍTULO DO MENU
   ========================================================= */

function criarTituloMenu(
    texto
) {

    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "menu-section";

    titulo.textContent =
        texto;

    menuContent.appendChild(
        titulo
    );

}


/* =========================================================
   NAVEGAÇÃO DO MENU
   ========================================================= */

function tratarTeclaMenu(
    event
) {

    const key =
        event.key;


    if (
        key === "ArrowUp"
    ) {

        event.preventDefault();

        moverMenu(
            -1
        );

        return;

    }


    if (
        key === "ArrowDown"
    ) {

        event.preventDefault();

        moverMenu(
            1
        );

        return;

    }


    if (
        key === "ArrowLeft"
    ) {

        event.preventDefault();

        return;

    }


    if (
        key === "ArrowRight"
    ) {

        event.preventDefault();

        return;

    }

}


function moverMenu(
    direcao
) {

    if (
        state.menuItems.length === 0
    ) {

        return;

    }


    let indice =
        state.menuItems.indexOf(
            document.activeElement
        );


    if (indice < 0) {

        indice = 0;

    }


    indice += direcao;


    if (indice < 0) {

        indice =
            state.menuItems.length - 1;

    }


    if (
        indice >=
        state.menuItems.length
    ) {

        indice = 0;

    }


    state.menuItems[
        indice
    ].focus();

}


/* =========================================================
   FECHAR MENU
   ========================================================= */

function fecharMenu() {

    state.menuOpen =
        false;


    sideMenu.classList.remove(
        "open"
    );

    sideMenu.setAttribute(
        "aria-hidden",
        "true"
    );


    menuButton.focus();

}


/* =========================================================
   CONTINUAR ASSISTINDO
   ========================================================= */

function mostrarContinuarAssistindo() {

    const assistidos =
        obterAssistidos();


    if (
        assistidos.length === 0
    ) {

        mostrarVazio(
            "Nenhum episódio assistido ainda"
        );

        return;

    }


    /*
     * Recuperamos os episódios do catálogo
     * e mostramos os que já foram marcados.
     */

    const encontrados = [];


    [
        ...state.series,
        ...state.doramas,
        ...state.animes
    ].forEach(
        serie => {

            (serie.temporadas || [])
                .forEach(
                    temporada => {

                        (temporada.episodios || [])
                            .forEach(
                                episodio => {

                                    const chave =
                                        chaveEpisodio(
                                            serie,
                                            temporada.numero,
                                            episodio.numero
                                        );


                                    if (
                                        assistidos.includes(
                                            chave
                                        )
                                    ) {

                                        encontrados.push({
                                            serie,
                                            temporada,
                                            episodio
                                        });

                                    }

                                }
                            );

                    }
                );

        }
    );


    if (
        encontrados.length === 0
    ) {

        mostrarVazio(
            "Nenhum episódio assistido encontrado"
        );

        return;

    }


    state.currentPage =
        "continuar";

    state.cards =
        [];

    state.currentIndex =
        0;

    content.innerHTML = "";


    const grid =
        criarGrid();


    encontrados.forEach(
        item => {

            const card =
                criarCardEpisodio(
                    item.serie,
                    item.temporada,
                    item.episodio
                );

            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   FAVORITOS
   ========================================================= */

function mostrarFavoritos() {

    const lista =
        state.filmes.filter(
            filme =>
                state.favoritos.has(
                    filme.titulo
                )
        );


    mostrarFilmes(
        lista
    );

}


/*
 * A versão Android possui o conjunto de favoritos,
 * mas o trecho atual do MainActivity não adiciona/remover
 * favorito no card. Aqui deixamos persistência pronta.
 */

function alternarFavorito(
    titulo
) {

    if (
        state.favoritos.has(
            titulo
        )
    ) {

        state.favoritos.delete(
            titulo
        );

    } else {

        state.favoritos.add(
            titulo
        );

    }


    salvarFavoritos();

}


/* =========================================================
   STORAGE
   ========================================================= */

function carregarFavoritos() {

    try {

        const dados =
            localStorage.getItem(
                "wolf_favoritos"
            );


        if (!dados) {
            return;
        }


        const lista =
            JSON.parse(
                dados
            );


        if (
            Array.isArray(lista)
        ) {

            state.favoritos =
                new Set(
                    lista
                );

        }

    } catch (erro) {

        console.warn(
            "Erro favoritos:",
            erro
        );

    }

}


function salvarFavoritos() {

    try {

        localStorage.setItem(
            "wolf_favoritos",
            JSON.stringify(
                Array.from(
                    state.favoritos
                )
            )
        );

    } catch (erro) {

        console.warn(
            "Erro salvando favoritos:",
            erro
        );

    }

}


/* =========================================================
   EPISÓDIOS ASSISTIDOS
   ========================================================= */

function chaveEpisodio(
    serie,
    numeroTemporada,
    numeroEpisodio
) {

    return (
        `${serie.titulo}|T${numeroTemporada}|E${numeroEpisodio}`
    );

}


function obterAssistidos() {

    try {

        const dados =
            localStorage.getItem(
                "wolf_assistidos"
            );


        if (!dados) {
            return [];
        }


        const lista =
            JSON.parse(
                dados
            );


        return Array.isArray(lista)
            ? lista
            : [];

    } catch (erro) {

        return [];

    }

}


function episodioAssistido(
    chave
) {

    return obterAssistidos()
        .includes(
            chave
        );

}


function marcarEpisodio(
    chave,
    assistido
) {

    const conjunto =
        new Set(
            obterAssistidos()
        );


    if (assistido) {

        conjunto.add(
            chave
        );

    } else {

        conjunto.delete(
            chave
        );

    }


    try {

        localStorage.setItem(
            "wolf_assistidos",
            JSON.stringify(
                Array.from(
                    conjunto
                )
            )
        );

    } catch (erro) {

        console.warn(
            "Erro salvando episódio:",
            erro
        );

    }

}


/* =========================================================
   PLAYER
   ========================================================= */

function abrirVideo(
    titulo,
    video,
    capa
) {

    if (
        !video ||
        !String(video).trim()
    ) {

        mostrarToast(
            "Vídeo ainda não disponível"
        );

        return;

    }


    state.playerOpen =
        true;


    playerTitle.textContent =
        titulo || "WOLF IPTV";


    playerError.style.display =
        "none";


    playerLoading.style.display =
        "block";


    playerOverlay.classList.remove(
        "hidden"
    );


    /*
     * Poster.
     */
    if (
        capa &&
        String(capa).trim()
    ) {

        videoPlayer.poster =
            String(capa).trim();

    } else {

        videoPlayer.removeAttribute(
            "poster"
        );

    }


    /*
     * Para vídeos MP4 e outros formatos
     * suportados nativamente pelo webOS.
     */
    videoPlayer.src =
        String(video).trim();


    videoPlayer.load();


    const promessa =
        videoPlayer.play();


    if (
        promessa &&
        typeof promessa.catch ===
        "function"
    ) {

        promessa.catch(
            function(erro) {

                console.warn(
                    "Autoplay bloqueado:",
                    erro
                );

                playerLoading.style.display =
                    "none";

            }
        );

    }

}


function fecharPlayer() {

    state.playerOpen =
        false;


    try {

        videoPlayer.pause();

    } catch (_) {}


    videoPlayer.removeAttribute(
        "src"
    );

    videoPlayer.load();


    playerOverlay.classList.add(
        "hidden"
    );


    /*
     * Devolve o foco para o card
     * que estava selecionado.
     */
    if (
        state.cards.length > 0 &&
        state.cards[state.currentIndex]
    ) {

        state.cards[
            state.currentIndex
        ].focus();

    } else {

        menuButton.focus();

    }

}


/* =========================================================
   PESQUISA
   ========================================================= */

function abrirPesquisa() {

    if (state.menuOpen) {

        fecharMenu();

    }


    state.searchOpen =
        true;


    searchOverlay.classList.remove(
        "hidden"
    );


    searchInput.value =
        "";


    requestAnimationFrame(
        function() {

            searchInput.focus();

        }
    );

}


function fecharPesquisa() {

    state.searchOpen =
        false;


    searchOverlay.classList.add(
        "hidden"
    );


    searchInput.blur();


    if (
        state.cards.length > 0
    ) {

        state.cards[
            state.currentIndex || 0
        ].focus();

    } else {

        menuButton.focus();

    }

}


function tratarPesquisa() {

    const texto =
        normalizarTexto(
            searchInput.value
        );


    if (!texto) {

        return;

    }


    const filmesEncontrados =
        state.filmes.filter(
            filme =>
                normalizarTexto(
                    filme.titulo
                ).includes(texto)
        );


    const seriesEncontradas =
        state.series.filter(
            serie =>
                normalizarTexto(
                    serie.titulo
                ).includes(texto)
        );


    const doramasEncontrados =
        state.doramas.filter(
            serie =>
                normalizarTexto(
                    serie.titulo
                ).includes(texto)
        );


    const animesEncontrados =
        state.animes.filter(
            serie =>
                normalizarTexto(
                    serie.titulo
                ).includes(texto)
        );


    mostrarResultadoPesquisa(
        filmesEncontrados,
        seriesEncontradas,
        doramasEncontrados,
        animesEncontrados
    );

}


/* =========================================================
   RESULTADO PESQUISA
   ========================================================= */

function mostrarResultadoPesquisa(
    filmes,
    series,
    doramas,
    animes
) {

    state.currentPage =
        "pesquisa";

    state.cards =
        [];

    state.currentIndex =
        0;


    content.innerHTML = "";


    const tudo = [
        ...filmes.map(
            item => ({
                tipo: "filme",
                item
            })
        ),

        ...series.map(
            item => ({
                tipo: "serie",
                item
            })
        ),

        ...doramas.map(
            item => ({
                tipo: "serie",
                item
            })
        ),

        ...animes.map(
            item => ({
                tipo: "serie",
                item
            })
        )
    ];


    if (
        tudo.length === 0
    ) {

        mostrarVazio(
            "Nenhum resultado encontrado"
        );

        return;

    }


    const grid =
        criarGrid();


    tudo.forEach(
        resultado => {

            let card;


            if (
                resultado.tipo ===
                "filme"
            ) {

                card =
                    criarCardFilme(
                        resultado.item
                    );

            } else {

                card =
                    criarCardSerie(
                        resultado.item
                    );

            }


            grid.appendChild(
                card
            );

            state.cards.push(
                card
            );

        }
    );


    content.appendChild(
        grid
    );


    focarPrimeiroCard();

}


/* =========================================================
   CATEGORIA
   ========================================================= */

function contarCategoria(
    lista,
    categoria
) {

    return lista.filter(
        item =>
            normalizarTexto(
                item.categoria
            ) ===
            normalizarTexto(
                categoria
            )
    ).length;

}


/* =========================================================
   TOAST
   ========================================================= */

let toastTimer = null;


function mostrarToast(
    mensagem
) {

    toastElement.textContent =
        mensagem;


    toastElement.classList.add(
        "show"
    );


    if (toastTimer) {

        clearTimeout(
            toastTimer
        );

    }


    toastTimer =
        setTimeout(
            function() {

                toastElement.classList.remove(
                    "show"
                );

            },
            3000
        );

}


/* =========================================================
   HTML SEGURO
   ========================================================= */

function escaparHTML(
    texto
) {

    return String(texto || "")
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );

}


/* =========================================================
   ATALHOS EXTRAS
   ========================================================= */

/*
 * Alguns controles LG entregam keyCode em vez de
 * nomes de teclas modernos. Estes atalhos ajudam
 * na compatibilidade com diferentes versões do webOS.
 */

window.addEventListener(
    "keydown",
    function(event) {

        /*
         * Back LG.
         */
        if (
            event.keyCode === 461
        ) {

            event.preventDefault();

        }

    },
    true
);


/* =========================================================
   PREVENIR MENU CONTEXTUAL
   ========================================================= */

document.addEventListener(
    "contextmenu",
    function(event) {

        event.preventDefault();

    }
);


/* =========================================================
   PREVENIR ZOOM
   ========================================================= */

document.addEventListener(
    "gesturestart",
    function(event) {

        event.preventDefault();

    }
);


/* =========================================================
   FINAL
   ========================================================= */

console.log(
    "WOLF IPTV webOS carregado."
);
