"use strict";

/* =========================================================
   WOLF IPTV - webOS
   Versão completa
   Compatível com a estrutura do Android
========================================================= */

const CONFIG = {
    BASE_URL:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/",

    catalogoUrl:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/catalogo.json",

    seriesUrl:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/series.json",

    animesUrl:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/animes.json",

    doramasUrls: [
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/Doramas.json",
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/doramas.json"
    ],

    desenhosUrls: [
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/desenho.json",
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/desenhos.json"
    ],

    backgroundUrl:
        "https://i.postimg.cc/Ghk8PP7w/wolf.png"
};


/* =========================================================
   DADOS
========================================================= */

let filmes = [];
let series = [];
let doramas = [];
let animes = [];
let desenhos = [];

let todosOsItens = [];

let favoritos = [];
let continueAssistindo = [];

try {
    favoritos = JSON.parse(
        localStorage.getItem("wolf_favoritos") || "[]"
    );

    if (!Array.isArray(favoritos)) {
        favoritos = [];
    }
} catch (erro) {
    favoritos = [];
}

try {
    continueAssistindo = JSON.parse(
        localStorage.getItem("wolf_continue") || "[]"
    );

    if (!Array.isArray(continueAssistindo)) {
        continueAssistindo = [];
    }
} catch (erro) {
    continueAssistindo = [];
}


/* =========================================================
   ESTADO
========================================================= */

let navegacaoAtual = "home";
let menuAberto = false;

let itemAtual = null;
let temporadaAtual = null;
let episodioAtual = null;
let serieAtual = null;

let tipoAtual = "";

let focoAtual = 0;
let elementosFocaveis = [];

let historicoConteudo = [];

let ultimaLista = [];
let ultimaPagina = null;

let playerAberto = false;


/* =========================================================
   ELEMENTOS
========================================================= */

const content =
    document.getElementById("content");

const contentScroll =
    document.getElementById("contentScroll");

const sideMenu =
    document.getElementById("sideMenu");

const menuButton =
    document.getElementById("menuButton");

const closeMenu =
    document.getElementById("closeMenu");

const menuContent =
    document.getElementById("menuContent");

const searchOverlay =
    document.getElementById("searchOverlay");

const searchInput =
    document.getElementById("searchInput");

const playerOverlay =
    document.getElementById("playerOverlay");

const videoPlayer =
    document.getElementById("videoPlayer");

const closePlayer =
    document.getElementById("closePlayer");

const playerTitle =
    document.getElementById("playerTitle");

const playerLoading =
    document.getElementById("playerLoading");

const playerError =
    document.getElementById("playerError");

const toast =
    document.getElementById("toast");

const pageTitle =
    document.getElementById("pageTitle");


/* =========================================================
   UTILITÁRIOS
========================================================= */

function normalizarTexto(texto) {
    return String(texto || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase()
        .trim();
}


function escaparHtml(texto) {
    return String(texto || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


function mostrarToast(mensagem) {
    if (!toast) return;

    toast.textContent = mensagem;
    toast.style.opacity = "1";
    toast.style.transform =
        "translate(-50%, 0)";

    clearTimeout(mostrarToast.timeout);

    mostrarToast.timeout = setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform =
            "translate(-50%, 25px)";
    }, 2500);
}


function obterTitulo(item) {
    return String(item?.titulo || "");
}


function obterAno(item) {
    const valor =
        Number(item?.ano || 0);

    return Number.isFinite(valor)
        ? valor
        : 0;
}


function obterCategoria(item) {
    return String(
        item?.categoria || ""
    );
}


function obterImagem(item) {
    return String(
        item?.capa || ""
    );
}


function obterVideo(item) {
    return String(
        item?.video || ""
    );
}


function obterIdItem(item) {
    if (!item) return "";

    if (
        item._tipo === "episodio"
    ) {
        return [
            item._serieTitulo || "",
            "T" +
                (item._temporadaNumero || 1),
            "E" +
                (item.numero || 1)
        ].join("|");
    }

    if (
        item._tipo === "temporada"
    ) {
        return [
            "temporada",
            item._serieTitulo || "",
            item.numero || 1
        ].join("|");
    }

    return [
        item._tipo || "",
        obterTitulo(item)
    ].join("|");
}


function tipoBonito(tipo) {
    switch (tipo) {
        case "filme":
            return "Filme";

        case "serie":
            return "Série";

        case "dorama":
            return "Dorama";

        case "anime":
            return "Anime";

        case "desenho":
            return "Desenho";

        default:
            return "";
    }
}


/* =========================================================
   FAVORITOS
========================================================= */

function salvarFavoritos() {
    try {
        localStorage.setItem(
            "wolf_favoritos",
            JSON.stringify(favoritos)
        );
    } catch (erro) {
        console.error(
            "Erro ao salvar favoritos:",
            erro
        );
    }
}


function estaNosFavoritos(item) {
    const id =
        obterIdItem(item);

    return favoritos.some(
        favorito =>
            String(favorito.id || "") === id
    );
}


function alternarFavorito(item) {
    if (!item) return;

    const id =
        obterIdItem(item);

    const indice =
        favoritos.findIndex(
            favorito =>
                String(favorito.id || "") === id
        );

    if (indice >= 0) {
        favoritos.splice(indice, 1);

        mostrarToast(
            "Removido dos favoritos"
        );
    } else {
        favoritos.push({
            id: id,
            titulo: obterTitulo(item),
            ano: obterAno(item),
            categoria: obterCategoria(item),
            capa: obterImagem(item),
            video: obterVideo(item),
            _tipo:
                item._tipo || "filme"
        });

        mostrarToast(
            "Adicionado aos favoritos"
        );
    }

    salvarFavoritos();

    renderizarPaginaAtual();
}


/* =========================================================
   CONTINUAR ASSISTINDO
========================================================= */

function salvarContinueAssistindo() {
    try {
        localStorage.setItem(
            "wolf_continue",
            JSON.stringify(
                continueAssistindo
            )
        );
    } catch (erro) {
        console.error(
            "Erro ao salvar progresso:",
            erro
        );
    }
}


function registrarContinueAssistindo(
    item,
    progresso = 0,
    duracao = 0
) {
    if (!item) return;

    const id =
        obterIdItem(item);

    if (!id) return;

    const indice =
        continueAssistindo.findIndex(
            registro =>
                String(registro.id || "") === id
        );

    const registro = {
        id: id,
        titulo: obterTitulo(item),
        ano: obterAno(item),
        categoria: obterCategoria(item),
        capa: obterImagem(item),
        video: obterVideo(item),
        _tipo: item._tipo || "filme",
        progresso: Number(progresso || 0),
        duracao: Number(duracao || 0),
        ultimaAtualizacao:
            Date.now()
    };

    if (indice >= 0) {
        continueAssistindo[indice] =
            registro;
    } else {
        continueAssistindo.unshift(
            registro
        );
    }

    continueAssistindo =
        continueAssistindo
            .sort(
                (a, b) =>
                    Number(
                        b.ultimaAtualizacao || 0
                    ) -
                    Number(
                        a.ultimaAtualizacao || 0
                    )
            )
            .slice(0, 50);

    salvarContinueAssistindo();
}


function removerContinueAssistindo(id) {
    continueAssistindo =
        continueAssistindo.filter(
            item =>
                String(item.id || "") !==
                String(id || "")
        );

    salvarContinueAssistindo();
}


function obterProgresso(item) {
    const id =
        obterIdItem(item);

    const encontrado =
        continueAssistindo.find(
            registro =>
                String(registro.id || "") === id
        );

    return encontrado || null;
}


/* =========================================================
   BACKGROUND
========================================================= */

function configurarBackground() {
    const background =
        document.getElementById(
            "background"
        );

    if (!background) return;

    background.style.backgroundImage =
        `url("${CONFIG.backgroundUrl}")`;
}


/* =========================================================
   JSON
========================================================= */

async function carregarJson(url) {
    const resposta =
        await fetch(
            url,
            {
                cache: "no-store"
            }
        );

    if (!resposta.ok) {
        throw new Error(
            `HTTP ${resposta.status}: ${url}`
        );
    }

    return await resposta.json();
}


async function carregarPrimeiroJson(urls) {
    for (const url of urls) {
        try {
            return await carregarJson(url);
        } catch (erro) {
            console.warn(
                "Falha ao carregar:",
                url
            );
        }
    }

    return null;
}


function extrairFilmes(dados) {
    if (
        dados &&
        Array.isArray(dados.filmes)
    ) {
        return dados.filmes.map(
            item => ({
                ...item,
                _tipo: "filme"
            })
        );
    }

    if (Array.isArray(dados)) {
        return dados.map(
            item => ({
                ...item,
                _tipo: "filme"
            })
        );
    }

    return [];
}


function extrairSeries(
    dados,
    tipo
) {
    let lista = [];

    if (
        dados &&
        Array.isArray(dados.series)
    ) {
        lista =
            dados.series;
    } else if (
        dados &&
        Array.isArray(dados.doramas)
    ) {
        lista =
            dados.doramas;
    } else if (
        dados &&
        Array.isArray(dados.animes)
    ) {
        lista =
            dados.animes;
    } else if (
        Array.isArray(dados)
    ) {
        lista = dados;
    }

    return lista.map(
        item => ({
            ...item,
            _tipo: tipo
        })
    );
}


function extrairDesenhos(dados) {
    if (
        dados &&
        Array.isArray(dados.desenhos)
    ) {
        return dados.desenhos.map(
            item => ({
                ...item,
                _tipo: "desenho"
            })
        );
    }

    if (
        dados &&
        Array.isArray(dados.desenho)
    ) {
        return dados.desenho.map(
            item => ({
                ...item,
                _tipo: "desenho"
            })
        );
    }

    if (Array.isArray(dados)) {
        return dados.map(
            item => ({
                ...item,
                _tipo: "desenho"
            })
        );
    }

    return [];
}


async function carregarCatalogos() {
    try {
        mostrarToast(
            "Carregando catálogo..."
        );

        const resultados =
            await Promise.allSettled([
                carregarJson(
                    CONFIG.catalogoUrl
                ),
                carregarJson(
                    CONFIG.seriesUrl
                ),
                carregarJson(
                    CONFIG.animesUrl
                ),
                carregarPrimeiroJson(
                    CONFIG.doramasUrls
                ),
                carregarPrimeiroJson(
                    CONFIG.desenhosUrls
                )
            ]);

        const catalogo =
            resultados[0].status ===
            "fulfilled"
                ? resultados[0].value
                : null;

        const seriesJson =
            resultados[1].status ===
            "fulfilled"
                ? resultados[1].value
                : null;

        const animesJson =
            resultados[2].status ===
            "fulfilled"
                ? resultados[2].value
                : null;

        const doramasJson =
            resultados[3].status ===
            "fulfilled"
                ? resultados[3].value
                : null;

        const desenhosJson =
            resultados[4].status ===
            "fulfilled"
                ? resultados[4].value
                : null;

        filmes =
            extrairFilmes(
                catalogo
            );

        series =
            extrairSeries(
                seriesJson,
                "serie"
            );

        animes =
            extrairSeries(
                animesJson,
                "anime"
            );

        doramas =
            extrairSeries(
                doramasJson,
                "dorama"
            );

        desenhos =
            extrairDesenhos(
                desenhosJson
            );

        /*
         * Fallback para catálogos que possam
         * conter tudo dentro do catalogo.json.
         */
        if (
            catalogo &&
            !series.length &&
            Array.isArray(
                catalogo.series
            )
        ) {
            series =
                extrairSeries(
                    {
                        series:
                            catalogo.series
                    },
                    "serie"
                );
        }

        if (
            catalogo &&
            !doramas.length &&
            Array.isArray(
                catalogo.doramas
            )
        ) {
            doramas =
                extrairSeries(
                    {
                        doramas:
                            catalogo.doramas
                    },
                    "dorama"
                );
        }

        if (
            catalogo &&
            !animes.length &&
            Array.isArray(
                catalogo.animes
            )
        ) {
            animes =
                extrairSeries(
                    {
                        animes:
                            catalogo.animes
                    },
                    "anime"
                );
        }

        if (
            catalogo &&
            !desenhos.length &&
            Array.isArray(
                catalogo.desenhos
            )
        ) {
            desenhos =
                extrairDesenhos(
                    {
                        desenhos:
                            catalogo.desenhos
                    }
                );
        }

        ordenarCatalogos();

        reconstruirTodosOsItens();

        renderizarHome();

        mostrarToast(
            `${filmes.length} filmes • ` +
            `${series.length} séries • ` +
            `${doramas.length} doramas • ` +
            `${animes.length} animes`
        );

    } catch (erro) {
        console.error(
            "Erro ao carregar catálogo:",
            erro
        );

        if (content) {
            content.innerHTML = `
                <div class="errorMessage">
                    Não foi possível carregar o catálogo.
                    <br><br>
                    Verifique sua internet e tente atualizar.
                </div>
            `;
        }
    }
}


function ordenarCatalogos() {
    const ordenarFilmes =
        (a, b) => {
            const anoA =
                obterAno(a);

            const anoB =
                obterAno(b);

            if (anoA !== anoB) {
                return anoB - anoA;
            }

            return normalizarTexto(
                obterTitulo(a)
            ).localeCompare(
                normalizarTexto(
                    obterTitulo(b)
                )
            );
        };

    const ordenarTitulo =
        (a, b) =>
            normalizarTexto(
                obterTitulo(a)
            ).localeCompare(
                normalizarTexto(
                    obterTitulo(b)
            );

    filmes.sort(
        ordenarFilmes
    );

    series.sort(
        ordenarTitulo
    );

    doramas.sort(
        ordenarTitulo
    );

    animes.sort(
        ordenarTitulo
    );

    desenhos.sort(
        ordenarFilmes
    );
}


function reconstruirTodosOsItens() {
    todosOsItens = [
        ...filmes,
        ...series,
        ...doramas,
        ...animes,
        ...desenhos
    ];
}


/* =========================================================
   MENU
========================================================= */

const categoriasMenu = {
    Filmes: [
        "Ação",
        "Aventura",
        "Animação",
        "Comédia",
        "Drama",
        "Terror",
        "Ficção"
    ],

    Séries: [
        "Ação",
        "Aventura",
        "Comédia",
        "Drama",
        "Terror"
    ],

    Doramas: [
        "Romance",
        "Ação",
        "Comédia",
        "Terror"
    ],

    Desenhos: [
        "Ação",
        "Aventura",
        "Animação",
        "Comédia",
        "Drama",
        "Terror",
        "Fantasia"
    ],

    Anime: [
        "Ação",
        "Comédia",
        "Terror"
    ]
};


function construirMenu() {
    if (!menuContent) return;

    let html = "";

    html += `
        <button
            class="menuItem focusable"
            data-menu-action="home"
            type="button"
        >
            🏠 INÍCIO
        </button>
    `;

    html += `
        <button
            class="menuItem focusable"
            data-menu-action="continuar"
            type="button"
        >
            ▶ CONTINUAR ASSISTINDO
        </button>
    `;

    html += `
        <button
            class="menuItem focusable"
            data-menu-action="favoritos"
            type="button"
        >
            ♥ FAVORITOS
        </button>
    `;

    html += `
        <button
            class="menuItem focusable"
            data-menu-action="pesquisa"
            type="button"
        >
            🔍 PESQUISAR
        </button>
    `;

    html += `
        <button
            class="menuItem focusable"
            data-menu-action="atualizar"
            type="button"
        >
            ↻ ATUALIZAR CATÁLOGO
        </button>
    `;

    for (
        const [nome, categorias]
        of Object.entries(
            categoriasMenu
        )
    ) {
        const tipo =
            nome === "Filmes"
                ? "filme"
                : nome === "Séries"
                ? "serie"
                : nome === "Doramas"
                ? "dorama"
                : nome === "Anime"
                ? "anime"
                : "desenho";

        html += `
            <div class="menuSection">
                <div class="menuSectionTitle">
                    ${escaparHtml(nome)}
                </div>
        `;

        html += `
            <button
                class="menuItem focusable"
                data-menu-type="${tipo}"
                data-menu-category=""
                type="button"
            >
                Todos
            </button>
        `;

        categorias.forEach(
            categoria => {
                html += `
                    <button
                        class="menuItem menuSubItem focusable"
                        data-menu-type="${tipo}"
                        data-menu-category="${escaparHtml(categoria)}"
                        type="button"
                    >
                        ${escaparHtml(categoria)}
                    </button>
                `;
            }
        );

        html += `
            </div>
        `;
    }

    menuContent.innerHTML =
        html;

    const botoes =
        menuContent.querySelectorAll(
            ".menuItem"
        );

    botoes.forEach(
        botao => {
            botao.addEventListener(
                "click",
                () => {
                    executarAcaoMenu(
                        botao
                    );
                }
            );
        }
    );
}


function executarAcaoMenu(botao) {
    const action =
        botao.dataset.menuAction;

    if (action) {
        switch (action) {
            case "home":
                fecharMenu();
                abrirHome();
                break;

            case "continuar":
                fecharMenu();
                abrirContinuarAssistindo();
                break;

            case "favoritos":
                fecharMenu();
                abrirFavoritos();
                break;

            case "pesquisa":
                fecharMenu();
                abrirPesquisa();
                break;

            case "atualizar":
                fecharMenu();

                carregarCatalogos();
                break;
        }

        return;
    }

    const tipo =
        botao.dataset.menuType;

    const categoria =
        botao.dataset.menuCategory || "";

    if (tipo) {
        fecharMenu();

        abrirCategoria(
            tipo,
            categoria
        );
    }
}


function abrirMenu() {
    if (!sideMenu) return;

    menuAberto = true;

    sideMenu.classList.add(
        "open"
    );

    sideMenu.setAttribute(
        "aria-hidden",
        "false"
    );

    elementosFocaveis =
        Array.from(
            menuContent.querySelectorAll(
                ".focusable"
            )
        );

    focoAtual = 0;

    aplicarFoco();
}


function fecharMenu() {
    if (!sideMenu) return;

    menuAberto = false;

    sideMenu.classList.remove(
        "open"
    );

    sideMenu.setAttribute(
        "aria-hidden",
        "true"
    );

    elementosFocaveis =
        [];

    if (
        navegacaoAtual === "home"
    ) {
        focoAtual = 0;
        aplicarFocoMenuButton();
    } else {
        focoAtual = 0;
        atualizarFocoConteudo();
    }
}


/* =========================================================
   HOME
========================================================= */

function abrirHome() {
    historicoConteudo = [];

    navegacaoAtual =
        "home";

    tipoAtual = "";

    itemAtual = null;

    temporadaAtual = null;

    episodioAtual = null;

    serieAtual = null;

    renderizarHome();
}


function renderizarHome() {
    if (!content) return;

    navegacaoAtual =
        "home";

    if (pageTitle) {
        pageTitle.textContent =
            "WOLF IPTV";
    }

    let html = "";

    html += criarBannerPrincipal();

    html += criarSecao(
        "FILMES",
        filmes.slice(0, 20),
        "filmes"
    );

    if (series.length) {
        html += criarSecao(
            "SÉRIES",
            series.slice(0, 20),
            "series"
        );
    }

    if (doramas.length) {
        html += criarSecao(
            "DORAMAS",
            doramas.slice(0, 20),
            "doramas"
        );
    }

    if (animes.length) {
        html += criarSecao(
            "ANIME",
            animes.slice(0, 20),
            "animes"
        );
    }

    if (desenhos.length) {
        html += criarSecao(
            "DESENHOS",
            desenhos.slice(0, 20),
            "desenhos"
        );
    }

    content.innerHTML =
        html;

    conectarCards();

    focoAtual = 0;

    atualizarFocoConteudo();

    if (contentScroll) {
        contentScroll.scrollTop = 0;
    }
}


function criarBannerPrincipal() {
    const destaque =
        filmes[0] ||
        series[0] ||
        animes[0];

    if (!destaque) {
        return "";
    }

    const imagem =
        obterImagem(destaque);

    const titulo =
        obterTitulo(destaque);

    const categoria =
        obterCategoria(destaque);

    return `
        <section
            class="hero"
            style="
                background-image:
                linear-gradient(
                    90deg,
                    rgba(0,0,0,.92),
                    rgba(0,0,0,.42),
                    rgba(0,0,0,.08)
                ),
                url('${escaparHtml(imagem)}')
            "
        >
            <div class="heroContent">
                <div class="heroLabel">
                    WOLF IPTV
                </div>

                <div class="heroTitle">
                    ${escaparHtml(titulo)}
                </div>

                <div class="heroInfo">
                    ${escaparHtml(
                        categoria
                    )}
                </div>

                <button
                    class="heroButton focusable"
                    data-hero-id="${escaparHtml(
                        obterIdItem(
                            destaque
                        )
                    )}"
                    type="button"
                >
                    ▶ ASSISTIR
                </button>
            </div>
        </section>
    `;
}


function criarSecao(
    titulo,
    lista,
    secao
) {
    if (!lista.length) {
        return "";
    }

    let html = `
        <section class="contentSection">
            <div class="sectionHeader">
                <h2>${escaparHtml(titulo)}</h2>
            </div>

            <div
                class="cardsGrid"
                data-section="${escaparHtml(secao)}"
            >
    `;

    lista.forEach(
        item => {
            html += criarCard(
                item
            );
        }
    );

    html += `
            </div>
        </section>
    `;

    return html;
}


/* =========================================================
   CARDS
========================================================= */

function criarCard(item) {
    const id =
        obterIdItem(item);

    const titulo =
        obterTitulo(item);

    const imagem =
        obterImagem(item);

    const favorito =
        estaNosFavoritos(item);

    let detalhes = "";

    if (
        item._tipo === "filme" ||
        item._tipo === "desenho"
    ) {
        detalhes =
            `${obterAno(item)} • ${obterCategoria(item)}`;
    } else if (
        item._tipo === "serie" ||
        item._tipo === "dorama" ||
        item._tipo === "anime"
    ) {
        const temporadas =
            Array.isArray(
                item.temporadas
            )
                ? item.temporadas.length
                : 0;

        detalhes =
            `${obterCategoria(item)} • ` +
            `${temporadas} temporada(s)`;
    }

    const progresso =
        obterProgresso(item);

    let barra = "";

    if (
        progresso &&
        progresso.duracao > 0 &&
        progresso.progresso > 0
    ) {
        const porcentagem =
            Math.min(
                100,
                Math.max(
                    0,
                    (
                        progresso.progresso /
                        progresso.duracao
                    ) * 100
                )
            );

        barra = `
            <div class="progressBar">
                <div
                    class="progressFill"
                    style="width:${porcentagem}%"
                ></div>
            </div>
        `;
    }

    return `
        <button
            class="mediaCard focusable"
            type="button"
            data-item-id="${escaparHtml(id)}"
        >
            <div class="cardImageWrap">

                <img
                    class="cardImage"
                    src="${escaparHtml(imagem)}"
                    alt="${escaparHtml(titulo)}"
                    loading="lazy"
                    onerror="this.style.display='none';"
                >

                <div class="cardType">
                    ${escaparHtml(
                        tipoBonito(
                            item._tipo
                        )
                    )}
                </div>

                <div class="favoriteMark ${
                    favorito
                        ? "active"
                        : ""
                }">
                    ♥
                </div>

                ${
                    barra
                }

            </div>

            <div class="cardTitle">
                ${escaparHtml(titulo)}
            </div>

            <div class="cardDetails">
                ${escaparHtml(detalhes)}
            </div>
        </button>
    `;
}


function conectarCards() {
    const cards =
        content.querySelectorAll(
            ".mediaCard"
        );

    cards.forEach(
        card => {
            card.addEventListener(
                "click",
                () => {
                    const id =
                        card.dataset.itemId;

                    const item =
                        localizarItemPorId(
                            id
                        );

                    if (item) {
                        abrirItem(
                            item
                        );
                    }
                }
            );
        }
    );

    const heroButtons =
        content.querySelectorAll(
            ".heroButton"
        );

    heroButtons.forEach(
        button => {
            button.addEventListener(
                "click",
                () => {
                    const item =
                        localizarItemPorId(
                            button.dataset.heroId
                        );

                    if (item) {
                        abrirItem(
                            item
                        );
                    }
                }
            );
        }
    );
}


/* =========================================================
   LOCALIZAÇÃO DE ITENS
========================================================= */

function localizarItemPorId(id) {
    if (!id) return null;

    const lista = [
        ...filmes,
        ...series,
        ...doramas,
        ...animes,
        ...desenhos,
        ...favoritos,
        ...continueAssistindo
    ];

    return (
        lista.find(
            item =>
                obterIdItem(item) === id
        ) || null
    );
}


function localizarSerie(
    titulo,
    tipo
) {
    let lista = [];

    switch (tipo) {
        case "serie":
            lista = series;
            break;

        case "dorama":
            lista = doramas;
            break;

        case "anime":
            lista = animes;
            break;
    }

    return (
        lista.find(
            item =>
                normalizarTexto(
                    item.titulo
                ) ===
                normalizarTexto(
                    titulo
                )
        ) || null
    );
}


/* =========================================================
   ABRIR ITEM
========================================================= */

function abrirItem(item) {
    if (!item) return;

    if (
        item._tipo === "filme" ||
        item._tipo === "desenho"
    ) {
        abrirFilme(
            item
        );

        return;
    }

    if (
        item._tipo === "serie" ||
        item._tipo === "dorama" ||
        item._tipo === "anime"
    ) {
        abrirSerie(
            item
        );
    }
}


function abrirFilme(item) {
    itemAtual =
        item;

    tipoAtual =
        item._tipo;

    abrirPlayer(
        item,
        obterVideo(item)
    );
}


/* =========================================================
   SÉRIES / TEMPORADAS
========================================================= */

function abrirSerie(item) {
    serieAtual =
        item;

    tipoAtual =
        item._tipo;

    historicoConteudo.push({
        pagina:
            navegacaoAtual,
        tipo:
            tipoAtual
    });

    navegacaoAtual =
        "temporadas";

    renderizarTemporadas(
        item
    );
}


function renderizarTemporadas(item) {
    if (!content) return;

    const temporadas =
        Array.isArray(
            item.temporadas
        )
            ? item.temporadas
            : [];

    if (pageTitle) {
        pageTitle.textContent =
            item.titulo;
    }

    let html = `
        <div class="pageHeader">
            <button
                class="backButton focusable"
                id="backContentButton"
                type="button"
            >
                ← VOLTAR
            </button>

            <div class="pageHeading">
                ${escaparHtml(
                    item.titulo
                )}
            </div>

            <div class="pageSubheading">
                ${escaparHtml(
                    item.categoria || ""
                )}
            </div>
        </div>

        <div class="cardsGrid seasonsGrid">
    `;

    temporadas.forEach(
        temporada => {
            html += criarCardTemporada(
                item,
                temporada
            );
        }
    );

    html += `
        </div>
    `;

    content.innerHTML =
        html;

    const back =
        document.getElementById(
            "backContentButton"
        );

    if (back) {
        back.addEventListener(
            "click",
            voltarConteudo
        );
    }

    conectarCardsTemporadas();

    focoAtual = 0;

    atualizarFocoConteudo();

    contentScroll.scrollTop = 0;
}


function criarCardTemporada(
    serie,
    temporada
) {
    const numero =
        Number(
            temporada.numero || 0
        );

    const episodios =
        Array.isArray(
            temporada.episodios
        )
            ? temporada.episodios
            : [];

    const capa =
        obterImagem(serie);

    const id =
        [
            "temporada",
            serie.titulo,
            numero
        ].join("|");

    return `
        <button
            class="mediaCard seasonCard focusable"
            type="button"
            data-season-id="${escaparHtml(id)}"
        >
            <div class="cardImageWrap">
                <img
                    class="cardImage"
                    src="${escaparHtml(capa)}"
                    alt="Temporada ${numero}"
                    loading="lazy"
                    onerror="this.style.display='none';"
                >
            </div>

            <div class="cardTitle">
                Temporada ${numero}
            </div>

            <div class="cardDetails">
                ${episodios.length}
                episódio(s)
            </div>
        </button>
    `;
}


function conectarCardsTemporadas() {
    const cards =
        content.querySelectorAll(
            ".seasonCard"
        );

    cards.forEach(
        card => {
            card.addEventListener(
                "click",
                () => {
                    const id =
                        card.dataset.seasonId;

                    const partes =
                        id.split("|");

                    const numero =
                        Number(
                            partes[
                                partes.length - 1
                            ]
                        );

                    const temporada =
                        serieAtual?.temporadas?.find(
                            item =>
                                Number(
                                    item.numero
                                ) === numero
                        );

                    if (temporada) {
                        abrirTemporada(
                            temporada
                        );
                    }
                }
            );
        }
    );
}


function abrirTemporada(
    temporada
) {
    temporadaAtual =
        temporada;

    historicoConteudo.push({
        pagina:
            "temporadas"
    });

    navegacaoAtual =
        "episodios";

    renderizarEpisodios(
        temporada
    );
}


/* =========================================================
   EPISÓDIOS
========================================================= */

function renderizarEpisodios(
    temporada
) {
    if (!content) return;

    const episodios =
        Array.isArray(
            temporada.episodios
        )
            ? temporada.episodios
            : [];

    if (pageTitle) {
        pageTitle.textContent =
            `${serieAtual?.titulo || ""} • Temporada ${temporada.numero}`;
    }

    let html = `
        <div class="pageHeader">

            <button
                class="backButton focusable"
                id="backContentButton"
                type="button"
            >
                ← VOLTAR
            </button>

            <div class="pageHeading">
                ${escaparHtml(
                    serieAtual?.titulo || ""
                )}
            </div>

            <div class="pageSubheading">
                Temporada ${Number(
                    temporada.numero
                )}
            </div>

        </div>

        <div class="cardsGrid episodesGrid">
    `;

    episodios.forEach(
        episodio => {
            html += criarCardEpisodio(
                episodio
            );
        }
    );

    html += `
        </div>
    `;

    content.innerHTML =
        html;

    const back =
        document.getElementById(
            "backContentButton"
        );

    if (back) {
        back.addEventListener(
            "click",
            voltarConteudo
        );
    }

    const cards =
        content.querySelectorAll(
            ".episodeCard"
        );

    cards.forEach(
        card => {
            card.addEventListener(
                "click",
                () => {
                    const numero =
                        Number(
                            card.dataset.episode
                        );

                    const episodio =
                        episodios.find(
                            item =>
                                Number(
                                    item.numero
                                ) === numero
                        );

                    if (episodio) {
                        abrirEpisodio(
                            episodio
                        );
                    }
                }
            );
        }
    );

    focoAtual = 0;

    atualizarFocoConteudo();

    contentScroll.scrollTop = 0;
}


function criarCardEpisodio(
    episodio
) {
    const numero =
        Number(
            episodio.numero || 0
        );

    const titulo =
        episodio.titulo ||
        `Episódio ${numero}`;

    const video =
        episodio.video || "";

    const item = {
        ...episodio,
        titulo: titulo,
        _tipo: "episodio",
        _serieTitulo:
            serieAtual?.titulo || "",
        _temporadaNumero:
            temporadaAtual?.numero || 1
    };

    const progresso =
        obterProgresso(
            item
        );

    let barra = "";

    if (
        progresso &&
        progresso.duracao > 0 &&
        progresso.progresso > 0
    ) {
        const porcentagem =
            Math.min(
                100,
                Math.max(
                    0,
                    (
                        progresso.progresso /
                        progresso.duracao
                    ) * 100
                )
            );

        barra = `
            <div class="progressBar">
                <div
                    class="progressFill"
                    style="width:${porcentagem}%"
                ></div>
            </div>
        `;
    }

    return `
        <button
            class="mediaCard episodeCard focusable"
            type="button"
            data-episode="${numero}"
        >
            <div class="cardImageWrap">

                <img
                    class="cardImage"
                    src="${escaparHtml(
                        obterImagem(
                            serieAtual
                        )
                    )}"
                    alt="${escaparHtml(
                        titulo
                    )}"
                    loading="lazy"
                    onerror="this.style.display='none';"
                >

                <div class="episodeNumber">
                    EP ${numero}
                </div>

                ${
                    video
                        ? ""
                        : `
                            <div class="noVideo">
                                SEM VÍDEO
                            </div>
                        `
                }

                ${
                    barra
                }

            </div>

            <div class="cardTitle">
                EP ${numero}
            </div>

            <div class="cardDetails">
                ${escaparHtml(
                    titulo
                )}
            </div>
        </button>
    `;
}


function abrirEpisodio(
    episodio
) {
    if (!episodio) return;

    episodioAtual =
        episodio;

    const item = {
        ...episodio,
        titulo:
            episodio.titulo ||
            `Episódio ${episodio.numero}`,
        _tipo:
            "episodio",
        _serieTitulo:
            serieAtual?.titulo || "",
        _temporadaNumero:
            temporadaAtual?.numero || 1
    };

    itemAtual =
        item;

    const video =
        String(
            episodio.video || ""
        );

    if (!video) {
        mostrarToast(
            "Este episódio ainda não possui vídeo."
        );

        return;
    }

    abrirPlayer(
        item,
        video
    );
}


/* =========================================================
   CATEGORIAS
========================================================= */

function obterListaPorTipo(
    tipo
) {
    switch (tipo) {
        case "filme":
            return filmes;

        case "serie":
            return series;

        case "dorama":
            return doramas;

        case "anime":
            return animes;

        case "desenho":
            return desenhos;

        default:
            return [];
    }
}


function abrirCategoria(
    tipo,
    categoria
) {
    tipoAtual =
        tipo;

    const lista =
        obterListaPorTipo(
            tipo
        );

    let filtrada =
        lista;

    if (categoria) {
        const categoriaNormalizada =
            normalizarTexto(
                categoria
            );

        filtrada =
            lista.filter(
                item =>
                    normalizarTexto(
                        obterCategoria(
                            item
                        )
                    ).includes(
                        categoriaNormalizada
                    )
            );
    }

    filtrada =
        [...filtrada];

    if (
        tipo === "filme" ||
        tipo === "desenho"
    ) {
        filtrada.sort(
            (a, b) => {
                const anoA =
                    obterAno(a);

                const anoB =
                    obterAno(b);

                if (
                    anoA !== anoB
                ) {
                    return anoB - anoA;
                }

                return normalizarTexto(
                    obterTitulo(a)
                ).localeCompare(
                    normalizarTexto(
                        obterTitulo(b)
                    )
                );
            }
        );
    }

    navegacaoAtual =
        "categoria";

    ultimaLista =
        filtrada;

    ultimaPagina = {
        tipo:
            tipo,
        categoria:
            categoria
    };

    if (pageTitle) {
        pageTitle.textContent =
            categoria
                ? `${tipoBonito(tipo)} • ${categoria}`
                : tipoBonito(tipo);
    }

    renderizarLista(
        filtrada,
        categoria
            ? `${tipoBonito(tipo)} • ${categoria}`
            : tipoBonito(tipo)
    );
}


function renderizarLista(
    lista,
    titulo
) {
    if (!content) return;

    let html = `
        <div class="pageHeader">

            <button
                class="backButton focusable"
                id="backContentButton"
                type="button"
            >
                ← VOLTAR
            </button>

            <div class="pageHeading">
                ${escaparHtml(titulo)}
            </div>

            <div class="pageSubheading">
                ${lista.length} item(ns)
            </div>

        </div>

        <div class="cardsGrid">
    `;

    lista.forEach(
        item => {
            html += criarCard(
                item
            );
        }
    );

    html += `
        </div>
    `;

    content.innerHTML =
        html;

    const back =
        document.getElementById(
            "backContentButton"
        );

    if (back) {
        back.addEventListener(
            "click",
            voltarConteudo
        );
    }

    conectarCards();

    focoAtual = 0;

    atualizarFocoConteudo();

    contentScroll.scrollTop = 0;
}


/* =========================================================
   FAVORITOS
========================================================= */

function abrirFavoritos() {
    navegacaoAtual =
        "favoritos";

    if (pageTitle) {
        pageTitle.textContent =
            "FAVORITOS";
    }

    const lista =
        favoritos.map(
            favorito => ({
                ...favorito
            })
        );

    renderizarListaEspecial(
        lista,
        "FAVORITOS",
        "Você ainda não adicionou favoritos."
    );
}


function renderizarListaEspecial(
    lista,
    titulo,
    vazio
) {
    if (!content) return;

    let html = `
        <div class="pageHeader">

            <button
                class="backButton focusable"
                id="backContentButton"
                type="button"
            >
                ← VOLTAR
            </button>

            <div class="pageHeading">
                ${escaparHtml(titulo)}
            </div>

            <div class="pageSubheading">
                ${lista.length} item(ns)
            </div>

        </div>
    `;

    if (!lista.length) {
        html += `
            <div class="emptyMessage">
                ${escaparHtml(vazio)}
            </div>
        `;
    } else {
        html += `
            <div class="cardsGrid">
        `;

        lista.forEach(
            item => {
                html += criarCard(
                    item
                );
            }
        );

        html += `
            </div>
        `;
    }

    content.innerHTML =
        html;

    const back =
        document.getElementById(
            "backContentButton"
        );

    if (back) {
        back.addEventListener(
            "click",
            voltarConteudo
        );
    }

    conectarCards();

    focoAtual = 0;

    atualizarFocoConteudo();

    contentScroll.scrollTop = 0;
}


/* =========================================================
   CONTINUAR ASSISTINDO
========================================================= */

function abrirContinuarAssistindo() {
    navegacaoAtual =
        "continuar";

    if (pageTitle) {
        pageTitle.textContent =
            "CONTINUAR ASSISTINDO";
    }

    const lista =
        continueAssistindo
            .filter(
                item =>
                    item &&
                    item.video
            )
            .sort(
                (a, b) =>
                    Number(
                        b.ultimaAtualizacao || 0
                    ) -
                    Number(
                        a.ultimaAtualizacao || 0
                    )
            );

    renderizarListaEspecial(
        lista,
        "CONTINUAR ASSISTINDO",
        "Nenhum conteúdo para continuar."
    );
}


/* =========================================================
   PESQUISA
========================================================= */

function abrirPesquisa() {
    if (!searchOverlay) return;

    searchOverlay.classList.remove(
        "hidden"
    );

    searchInput.value = "";

    setTimeout(
        () => {
            searchInput.focus();
        },
        100
    );
}


function fecharPesquisa() {
    if (!searchOverlay) return;

    searchOverlay.classList.add(
        "hidden"
    );

    if (menuAberto) {
        abrirMenu();
    } else {
        atualizarFocoConteudo();
    }
}


function executarPesquisa(
    texto
) {
    const busca =
        normalizarTexto(
            texto
        );

    if (!busca) {
        mostrarToast(
            "Digite algo para pesquisar."
        );

        return;
    }

    const resultados =
        todosOsItens.filter(
            item => {
                const titulo =
                    normalizarTexto(
                        obterTitulo(item)
                    );

                const categoria =
                    normalizarTexto(
                        obterCategoria(item)
                    );

                return (
                    titulo.includes(
                        busca
                    ) ||
                    categoria.includes(
                        busca
                    )
                );
            }
        );

    fecharPesquisa();

    navegacaoAtual =
        "pesquisa";

    if (pageTitle) {
        pageTitle.textContent =
            `PESQUISA: ${texto}`;
    }

    renderizarListaEspecial(
        resultados,
        `RESULTADOS: ${texto}`,
        "Nenhum resultado encontrado."
    );
}


/* =========================================================
   PLAYER
========================================================= */

function abrirPlayer(
    item,
    videoUrl
) {
    if (!playerOverlay) return;

    if (!videoUrl) {
        mostrarToast(
            "Vídeo indisponível."
        );

        return;
    }

    playerAberto =
        true;

    itemAtual =
        item;

    if (playerTitle) {
        playerTitle.textContent =
            obterTitulo(item);
    }

    if (playerLoading) {
        playerLoading.style.display =
            "block";
    }

    if (playerError) {
        playerError.style.display =
            "none";
    }

    playerOverlay.classList.remove(
        "hidden"
    );

    videoPlayer.pause();

    videoPlayer.removeAttribute(
        "src"
    );

    videoPlayer.load();

    videoPlayer.src =
        videoUrl;

    const progresso =
        obterProgresso(
            item
        );

    const iniciarVideo =
        () => {
            if (
                progresso &&
                progresso.progresso > 10 &&
                videoPlayer.duration &&
                progresso.progresso <
                    videoPlayer.duration - 10
            ) {
                try {
                    videoPlayer.currentTime =
                        progresso.progresso;
                } catch (erro) {
                    console.warn(
                        "Não foi possível restaurar progresso."
                    );
                }
            }

            videoPlayer.play()
                .catch(
                    erro => {
                        console.warn(
                            "Autoplay bloqueado:",
                            erro
                        );
                    }
                );
        };

    videoPlayer.onloadedmetadata =
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }

            iniciarVideo();
        };

    videoPlayer.oncanplay =
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }
        };

    videoPlayer.onerror =
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }

            if (playerError) {
                playerError.style.display =
                    "block";
            }
        };

    videoPlayer.onended =
        () => {
            if (item) {
                removerContinueAssistindo(
                    obterIdItem(item)
                );
            }
        };

    videoPlayer.ontimeupdate =
        () => {
            if (
                !item ||
                !videoPlayer.duration ||
                !Number.isFinite(
                    videoPlayer.duration
                )
            ) {
                return;
            }

            if (
                videoPlayer.currentTime <
                5
            ) {
                return;
            }

            registrarContinueAssistindo(
                item,
                videoPlayer.currentTime,
                videoPlayer.duration
            );
        };
}


function fecharPlayer() {
    if (!videoPlayer) return;

    try {
        if (
            itemAtual &&
            videoPlayer.duration &&
            Number.isFinite(
                videoPlayer.duration
            ) &&
            videoPlayer.currentTime > 5
        ) {
            registrarContinueAssistindo(
                itemAtual,
                videoPlayer.currentTime,
                videoPlayer.duration
            );
        }
    } catch (erro) {
        console.warn(
            "Erro ao salvar progresso:",
            erro
        );
    }

    videoPlayer.pause();

    videoPlayer.removeAttribute(
        "src"
    );

    videoPlayer.load();

    playerOverlay.classList.add(
        "hidden"
    );

    playerAberto =
        false;

    itemAtual = null;

    if (playerError) {
        playerError.style.display =
            "none";
    }

    if (playerLoading) {
        playerLoading.style.display =
            "none";
    }

    atualizarFocoConteudo();
}


/* =========================================================
   NAVEGAÇÃO
========================================================= */

function obterFocaveisConteudo() {
    if (!content) return [];

    return Array.from(
        content.querySelectorAll(
            ".focusable"
        )
    );
}


function aplicarFoco() {
    elementosFocaveis.forEach(
        (elemento, indice) => {
            elemento.classList.toggle(
                "focused",
                indice === focoAtual
            );
        }
    );

    const atual =
        elementosFocaveis[
            focoAtual
        ];

    if (atual) {
        try {
            atual.scrollIntoView({
                behavior: "smooth",
                block: "nearest",
                inline: "nearest"
            });
        } catch (erro) {
            atual.scrollIntoView();
        }
    }
}


function aplicarFocoMenuButton() {
    elementosFocaveis =
        menuButton
            ? [menuButton]
            : [];

    focoAtual = 0;

    aplicarFoco();
}


function atualizarFocoConteudo() {
    elementosFocaveis =
        obterFocaveisConteudo();

    if (!elementosFocaveis.length) {
        aplicarFocoMenuButton();
        return;
    }

    if (
        focoAtual >=
        elementosFocaveis.length
    ) {
        focoAtual =
            elementosFocaveis.length - 1;
    }

    if (focoAtual < 0) {
        focoAtual = 0;
    }

    aplicarFoco();
}


function moverFoco(
    direcao
) {
    if (menuAberto) {
        moverFocoMenu(
            direcao
        );

        return;
    }

    if (playerAberto) {
        return;
    }

    if (
        searchOverlay &&
        !searchOverlay.classList.contains(
            "hidden"
        )
    ) {
        return;
    }

    if (
        navegacaoAtual === "home" &&
        elementosFocaveis.length === 0
    ) {
        aplicarFocoMenuButton();

        return;
    }

    const lista =
        elementosFocaveis.length
            ? elementosFocaveis
            : obterFocaveisConteudo();

    if (!lista.length) return;

    let novoIndice =
        focoAtual;

    if (
        direcao === "left"
    ) {
        novoIndice =
            Math.max(
                0,
                focoAtual - 1
            );
    }

    if (
        direcao === "right"
    ) {
        novoIndice =
            Math.min(
                lista.length - 1,
                focoAtual + 1
            );
    }

    if (
        direcao === "up"
    ) {
        novoIndice =
            Math.max(
                0,
                focoAtual - 5
            );

        if (
            focoAtual < 5 &&
            navegacaoAtual === "home"
        ) {
            aplicarFocoMenuButton();
            return;
        }
    }

    if (
        direcao === "down"
    ) {
        novoIndice =
            Math.min(
                lista.length - 1,
                focoAtual + 5
            );
    }

    focoAtual =
        novoIndice;

    elementosFocaveis =
        lista;

    aplicarFoco();
}


function moverFocoMenu(
    direcao
) {
    if (
        !elementosFocaveis.length
    ) {
        elementosFocaveis =
            Array.from(
                menuContent.querySelectorAll(
                    ".focusable"
                )
            );
    }

    if (
        !elementosFocaveis.length
    ) return;

    let novo =
        focoAtual;

    if (
        direcao === "up"
    ) {
        novo =
            Math.max(
                0,
                focoAtual - 1
            );
    }

    if (
        direcao === "down"
    ) {
        novo =
            Math.min(
                elementosFocaveis.length - 1,
                focoAtual + 1
            );
    }

    focoAtual =
        novo;

    aplicarFoco();
}


function selecionarFoco() {
    if (menuAberto) {
        const atual =
            elementosFocaveis[
                focoAtual
            ];

        if (atual) {
            executarAcaoMenu(
                atual
            );
        }

        return;
    }

    if (
        searchOverlay &&
        !searchOverlay.classList.contains(
            "hidden"
        )
    ) {
        return;
    }

    const atual =
        elementosFocaveis[
            focoAtual
        ];

    if (atual) {
        atual.click();
    }
}


/* =========================================================
   VOLTAR
========================================================= */

function voltarConteudo() {
    if (playerAberto) {
        fecharPlayer();
        return;
    }

    if (
        searchOverlay &&
        !searchOverlay.classList.contains(
            "hidden"
        )
    ) {
        fecharPesquisa();
        return;
    }

    if (menuAberto) {
        fecharMenu();
        return;
    }

    if (
        navegacaoAtual ===
            "episodios"
    ) {
        navegacaoAtual =
            "temporadas";

        if (
            serieAtual
        ) {
            renderizarTemporadas(
                serieAtual
            );
        }

        return;
    }

    if (
        navegacaoAtual ===
            "temporadas"
    ) {
        navegacaoAtual =
            "home";

        if (
            historicoConteudo.length
        ) {
            historicoConteudo.pop();
        }

        abrirHome();

        return;
    }

    if (
        navegacaoAtual ===
            "categoria" ||
        navegacaoAtual ===
            "favoritos" ||
        navegacaoAtual ===
            "continuar" ||
        navegacaoAtual ===
            "pesquisa"
    ) {
        abrirHome();

        return;
    }

    abrirHome();
}


/* =========================================================
   PÁGINA ATUAL
========================================================= */

function renderizarPaginaAtual() {
    switch (
        navegacaoAtual
    ) {
        case "home":
            renderizarHome();
            break;

        case "favoritos":
            abrirFavoritos();
            break;

        case "continuar":
            abrirContinuarAssistindo();
            break;

        case "categoria":
            if (ultimaPagina) {
                abrirCategoria(
                    ultimaPagina.tipo,
                    ultimaPagina.categoria
                );
            } else {
                abrirHome();
            }
            break;

        case "temporadas":
            if (serieAtual) {
                renderizarTemporadas(
                    serieAtual
                );
            } else {
                abrirHome();
            }
            break;

        case "episodios":
            if (temporadaAtual) {
                renderizarEpisodios(
                    temporadaAtual
                );
            } else {
                abrirHome();
            }
            break;

        default:
            abrirHome();
            break;
    }
}


/* =========================================================
   TECLADO / D-PAD LG
========================================================= */

function tratarTecla(
    evento
) {
    const tecla =
        evento.key ||
        evento.code;

    if (
        playerAberto
    ) {
        if (
            tecla === "Escape" ||
            tecla === "Backspace" ||
            tecla === "BrowserBack"
        ) {
            evento.preventDefault();

            fecharPlayer();
        }

        return;
    }

    if (
        searchOverlay &&
        !searchOverlay.classList.contains(
            "hidden"
        )
    ) {
        if (
            tecla === "Escape" ||
            tecla === "Backspace" ||
            tecla === "BrowserBack"
        ) {
            evento.preventDefault();

            fecharPesquisa();
        }

        if (
            tecla === "Enter"
        ) {
            if (
                document.activeElement ===
                searchInput
            ) {
                evento.preventDefault();

                executarPesquisa(
                    searchInput.value
                );
            }
        }

        return;
    }

    switch (tecla) {
        case "ArrowLeft":
        case "LEFT":
            evento.preventDefault();

            moverFoco(
                "left"
            );

            break;

        case "ArrowRight":
        case "RIGHT":
            evento.preventDefault();

            moverFoco(
                "right"
            );

            break;

        case "ArrowUp":
        case "UP":
            evento.preventDefault();

            moverFoco(
                "up"
            );

            break;

        case "ArrowDown":
        case "DOWN":
            evento.preventDefault();

            moverFoco(
                "down"
            );

            break;

        case "Enter":
        case "OK":
            evento.preventDefault();

            selecionarFoco();

            break;

        case "Escape":
        case "Backspace":
        case "BrowserBack":
        case "GoBack":
            evento.preventDefault();

            voltarConteudo();

            break;

        case "Menu":
        case "ContextMenu":
            evento.preventDefault();

            if (menuAberto) {
                fecharMenu();
            } else {
                abrirMenu();
            }

            break;
    }
}


document.addEventListener(
    "keydown",
    tratarTecla,
    true
);


/* =========================================================
   BOTÕES
========================================================= */

if (menuButton) {
    menuButton.addEventListener(
        "click",
        () => {
            if (menuAberto) {
                fecharMenu();
            } else {
                abrirMenu();
            }
        }
    );
}


if (closeMenu) {
    closeMenu.addEventListener(
        "click",
        fecharMenu
    );
}


if (closePlayer) {
    closePlayer.addEventListener(
        "click",
        fecharPlayer
    );
}


if (searchInput) {
    searchInput.addEventListener(
        "keydown",
        evento => {
            if (
                evento.key === "Enter"
            ) {
                evento.preventDefault();

                executarPesquisa(
                    searchInput.value
                );
            }

            if (
                evento.key === "Escape"
            ) {
                evento.preventDefault();

                fecharPesquisa();
            }
        }
    );
}


/* =========================================================
   CLIQUE FORA
========================================================= */

if (sideMenu) {
    sideMenu.addEventListener(
        "click",
        evento => {
            if (
                evento.target ===
                sideMenu
            ) {
                fecharMenu();
            }
        }
    );
}


if (searchOverlay) {
    searchOverlay.addEventListener(
        "click",
        evento => {
            if (
                evento.target ===
                searchOverlay
            ) {
                fecharPesquisa();
            }
        }
    );
}


/* =========================================================
   MOUSE / CONTROLE
========================================================= */

document.addEventListener(
    "click",
    evento => {
        const elemento =
            evento.target.closest(
                ".focusable"
            );

        if (!elemento) return;

        elementosFocaveis =
            menuAberto
                ? Array.from(
                    menuContent.querySelectorAll(
                        ".focusable"
                    )
                )
                : obterFocaveisConteudo();

        const indice =
            elementosFocaveis.indexOf(
                elemento
            );

        if (indice >= 0) {
            focoAtual =
                indice;

            aplicarFoco();
        }
    }
);


/* =========================================================
   FOCO AUTOMÁTICO
========================================================= */

document.addEventListener(
    "focusin",
    evento => {
        const elemento =
            evento.target.closest(
                ".focusable"
            );

        if (!elemento) return;

        if (menuAberto) {
            elementosFocaveis =
                Array.from(
                    menuContent.querySelectorAll(
                        ".focusable"
                    )
                );
        } else {
            elementosFocaveis =
                obterFocaveisConteudo();
        }

        const indice =
            elementosFocaveis.indexOf(
                elemento
            );

        if (indice >= 0) {
            focoAtual =
                indice;

            aplicarFoco();
        }
    }
);


/* =========================================================
   EVENTOS DO VÍDEO
========================================================= */

if (videoPlayer) {
    videoPlayer.addEventListener(
        "play",
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }
        }
    );

    videoPlayer.addEventListener(
        "waiting",
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "block";
            }
        }
    );

    videoPlayer.addEventListener(
        "playing",
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }
        }
    );

    videoPlayer.addEventListener(
        "loadeddata",
        () => {
            if (playerLoading) {
                playerLoading.style.display =
                    "none";
            }
        }
    );
}


/* =========================================================
   VISIBILIDADE
========================================================= */

document.addEventListener(
    "visibilitychange",
    () => {
        if (
            document.hidden &&
            videoPlayer &&
            !videoPlayer.paused
        ) {
            try {
                videoPlayer.pause();
            } catch (erro) {}
        }
    }
);


/* =========================================================
   INICIALIZAÇÃO
========================================================= */

function inicializarApp() {
    configurarBackground();

    construirMenu();

    if (content) {
        content.innerHTML = `
            <div class="loading">
                <div class="loadingWolf">
                    WOLF
                </div>
                <div>
                    Carregando catálogo...
                </div>
            </div>
        `;
    }

    aplicarFocoMenuButton();

    carregarCatalogos();
}


window.addEventListener(
    "load",
    inicializarApp
);


/* =========================================================
   SUPORTE A webOS
========================================================= */

window.addEventListener(
    "unload",
    () => {
        try {
            if (
                videoPlayer &&
                itemAtual &&
                videoPlayer.duration &&
                videoPlayer.currentTime > 5
            ) {
                registrarContinueAssistindo(
                    itemAtual,
                    videoPlayer.currentTime,
                    videoPlayer.duration
                );
            }
        } catch (erro) {}
    }
);


/* =========================================================
   CORREÇÃO DE RETORNO DO CONTROLE
========================================================= */

window.addEventListener(
    "popstate",
    () => {
        voltarConteudo();
    }
);


/* =========================================================
   EXPOSIÇÃO GLOBAL
========================================================= */

window.WOLF = {
    abrirMenu,
    fecharMenu,
    abrirHome,
    abrirPesquisa,
    fecharPesquisa,
    abrirFavoritos,
    abrirContinuarAssistindo,
    carregarCatalogos,
    alternarFavorito,
    abrirPlayer,
    fecharPlayer,
    voltarConteudo
};
