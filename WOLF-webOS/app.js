/* =========================================================
   WOLF IPTV - webOS
   APP.JS
========================================================= */

"use strict";

/* =========================================================
   CONFIGURAÇÕES
========================================================= */

const CONFIG = {
    catalogoUrl:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/catalogo.json",

    seriesUrl:
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/series.json",

    backgroundUrl:
        "https://i.postimg.cc/Ghk8PP7w/wolf.png"
};

/* =========================================================
   ESTADO GLOBAL
========================================================= */

let filmes = [];
let series = [];
let doramas = [];
let animes = [];
let desenhos = [];

let todosOsItens = [];

let favoritos =
    JSON.parse(
        localStorage.getItem("wolf_favoritos") || "[]"
    );

let continueAssistindo =
    JSON.parse(
        localStorage.getItem("wolf_continue") || "[]"
    );

let categoriaAtual = "Todos";

let menuAberto = false;

let itemAtual = null;

let temporadaAtual = null;

let episodioAtual = null;

let navegacaoAtual = "conteudo";

let focoAtual = 0;

let elementosFocaveis = [];

/* =========================================================
   ELEMENTOS DA INTERFACE
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

function obterValor(obj, chaves, padrao = "") {
    if (!obj) return padrao;

    for (const chave of chaves) {
        if (
            obj[chave] !== undefined &&
            obj[chave] !== null &&
            obj[chave] !== ""
        ) {
            return obj[chave];
        }
    }

    return padrao;
}

function obterTitulo(item) {
    return obterValor(
        item,
        [
            "titulo",
            "title",
            "nome",
            "name"
        ],
        "Sem título"
    );
}

function obterImagem(item) {
    return obterValor(
        item,
        [
            "capa",
            "poster",
            "imagem",
            "image",
            "thumb",
            "thumbnail"
        ],
        ""
    );
}

function obterVideo(item) {
    return obterValor(
        item,
        [
            "video",
            "url",
            "link",
            "videoUrl",
            "video_url"
        ],
        ""
    );
}

function obterCategoria(item) {
    return obterValor(
        item,
        [
            "categoria",
            "category",
            "genero",
            "genre"
        ],
        "Outros"
    );
}

function mostrarToast(mensagem) {
    if (!toast) return;

    toast.textContent = mensagem;

    toast.style.opacity = "1";
    toast.style.transform =
        "translate(-50%, 0)";

    clearTimeout(
        mostrarToast.timeout
    );

    mostrarToast.timeout =
        setTimeout(() => {
            toast.style.opacity = "0";

            toast.style.transform =
                "translate(-50%, 25px)";
        }, 2200);
}

/* =========================================================
   LOCAL STORAGE
========================================================= */

function salvarFavoritos() {
    localStorage.setItem(
        "wolf_favoritos",
        JSON.stringify(favoritos)
    );
}

function salvarContinueAssistindo() {
    localStorage.setItem(
        "wolf_continue",
        JSON.stringify(
            continueAssistindo
        )
    );
}

/* =========================================================
   FAVORITOS
========================================================= */

function obterIdItem(item) {
    if (!item) return "";

    return String(
        item.id ||
        item.ID ||
        item.codigo ||
        item.slug ||
        obterTitulo(item)
    );
}

function estaNosFavoritos(item) {
    const id =
        obterIdItem(item);

    return favoritos.some(
        favorito =>
            String(
                favorito.id ||
                favorito
            ) === id
    );
}

function alternarFavorito(item) {
    const id =
        obterIdItem(item);

    const indice =
        favoritos.findIndex(
            favorito =>
                String(
                    favorito.id ||
                    favorito
                ) === id
        );

    if (indice >= 0) {
        favoritos.splice(
            indice,
            1
        );

        mostrarToast(
            "Removido dos favoritos"
        );
    } else {
        favoritos.push({
            id: id,
            titulo:
                obterTitulo(item),
            capa:
                obterImagem(item),
            video:
                obterVideo(item),
            categoria:
                obterCategoria(item)
        });

        mostrarToast(
            "Adicionado aos favoritos"
        );
    }

    salvarFavoritos();

    renderizarPaginaAtual();
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
   NORMALIZAÇÃO DO CATÁLOGO
========================================================= */

function normalizarFilmes(dados) {
    if (Array.isArray(dados)) {
        return dados;
    }

    if (
        dados &&
        Array.isArray(dados.filmes)
    ) {
        return dados.filmes;
    }

    if (
        dados &&
        Array.isArray(dados.movies)
    ) {
        return dados.movies;
    }

    if (
        dados &&
        Array.isArray(dados.catalogo)
    ) {
        return dados.catalogo;
    }

    return [];
}

function normalizarSeries(dados) {
    if (Array.isArray(dados)) {
        return dados;
    }

    if (
        dados &&
        Array.isArray(dados.series)
    ) {
        return dados.series;
    }

    if (
        dados &&
        Array.isArray(dados.seriesList)
    ) {
        return dados.seriesList;
    }

    return [];
}

/* =========================================================
   CLASSIFICAÇÃO
========================================================= */

function classificarConteudo() {
    doramas = [];
    animes = [];
    desenhos = [];

    filmes.forEach(item => {
        const texto =
            normalizarTexto(
                [
                    obterTitulo(item),
                    obterCategoria(item),
                    item.tipo || "",
                    item.genero || ""
                ].join(" ")
            );

        if (
            texto.includes("dorama") ||
            texto.includes("k-drama") ||
            texto.includes("coreano")
        ) {
            doramas.push(item);
        }

        if (
            texto.includes("anime")
        ) {
            animes.push(item);
        }

        if (
            texto.includes("desenho") ||
            texto.includes("cartoon") ||
            texto.includes("animacao")
        ) {
            desenhos.push(item);
        }
    });

    todosOsItens = [
        ...filmes,
        ...series,
        ...doramas,
        ...animes,
        ...desenhos
    ];
}

/* =========================================================
   CARREGAMENTO DOS CATÁLOGOS
========================================================= */

async function carregarJson(url) {
    const resposta =
        await fetch(url, {
            cache: "no-cache"
        });

    if (!resposta.ok) {
        throw new Error(
            `Erro HTTP ${resposta.status}`
        );
    }

    return await resposta.json();
}

async function carregarCatalogos() {
    try {
        const resultados =
            await Promise.allSettled([
                carregarJson(
                    CONFIG.catalogoUrl
                ),

                carregarJson(
                    CONFIG.seriesUrl
                )
            ]);

        if (
            resultados[0].status ===
            "fulfilled"
        ) {
            filmes =
                normalizarFilmes(
                    resultados[0].value
                );
        }

        if (
            resultados[1].status ===
            "fulfilled"
        ) {
            series =
                normalizarSeries(
                    resultados[1].value
                );
        }

        classificarConteudo();

        renderizarHome();

        mostrarToast(
            `${filmes.length} filmes • ${series.length} séries`
        );

    } catch (erro) {
        console.error(
            "Erro ao carregar catálogo:",
            erro
        );

        content.innerHTML = `
            <div class="errorMessage">
                Não foi possível carregar o catálogo.
            </div>
        `;
    }
}/* =========================================================
   ORDENAÇÃO
========================================================= */

function obterAno(item) {
    const ano =
        obterValor(
            item,
            [
                "ano",
                "year",
                "release_year",
                "data"
            ],
            ""
        );

    const numero =
        parseInt(
            String(ano).substring(0, 4),
            10
        );

    return Number.isNaN(numero)
        ? 0
        : numero;
}

function ordenarFilmes(itens) {
    return [...itens].sort(
        (a, b) => {

            const anoA =
                obterAno(a);

            const anoB =
                obterAno(b);

            /*
             * Filmes de 2026 aparecem primeiro,
             * conforme a organização do WOLF IPTV.
             */

            if (
                anoA === 2026 &&
                anoB !== 2026
            ) {
                return -1;
            }

            if (
                anoB === 2026 &&
                anoA !== 2026
            ) {
                return 1;
            }

            if (anoA !== anoB) {
                return anoB - anoA;
            }

            return obterTitulo(a)
                .localeCompare(
                    obterTitulo(b),
                    "pt-BR"
                );
        }
    );
}

function ordenarPorTitulo(itens) {
    return [...itens].sort(
        (a, b) =>
            obterTitulo(a)
                .localeCompare(
                    obterTitulo(b),
                    "pt-BR"
                )
    );
}

/* =========================================================
   CATEGORIAS
========================================================= */

function obterCategorias(itens) {
    const categorias = [];

    itens.forEach(item => {

        const categoria =
            obterCategoria(item);

        if (
            categoria &&
            !categorias.includes(
                categoria
            )
        ) {
            categorias.push(
                categoria
            );
        }
    });

    return categorias.sort(
        (a, b) =>
            String(a).localeCompare(
                String(b),
                "pt-BR"
            )
    );
}

function renderizarCategorias(
    container,
    itens,
    tipo
) {
    if (!container) return;

    const categorias =
        obterCategorias(itens);

    let html = `
        <button
            class="categoryButton focusable active"
            data-category="Todos"
            data-type="${tipo}"
        >
            Todos
        </button>
    `;

    categorias.forEach(
        categoria => {

            html += `
                <button
                    class="categoryButton focusable"
                    data-category="${escaparHtml(categoria)}"
                    data-type="${tipo}"
                >
                    ${escaparHtml(categoria)}
                </button>
            `;
        }
    );

    container.innerHTML = html;

    container
        .querySelectorAll(
            ".categoryButton"
        )
        .forEach(botao => {

            botao.addEventListener(
                "click",
                () => {

                    categoriaAtual =
                        botao.dataset.category ||
                        "Todos";

                    container
                        .querySelectorAll(
                            ".categoryButton"
                        )
                        .forEach(
                            b =>
                                b.classList.remove(
                                    "active"
                                )
                        );

                    botao.classList.add(
                        "active"
                    );

                    renderizarSecao(
                        itens,
                        tipo,
                        categoriaAtual
                    );

                    atualizarFoco();
                }
            );
        });
}

/* =========================================================
   FILTRO POR CATEGORIA
========================================================= */

function filtrarPorCategoria(
    itens,
    categoria
) {
    if (
        !categoria ||
        categoria === "Todos"
    ) {
        return [...itens];
    }

    const categoriaNormalizada =
        normalizarTexto(
            categoria
        );

    return itens.filter(
        item => {

            const valor =
                normalizarTexto(
                    obterCategoria(item)
                );

            return (
                valor ===
                categoriaNormalizada
            );
        }
    );
}

/* =========================================================
   CARD
========================================================= */

function criarCard(
    item,
    tipo = "filme"
) {
    const titulo =
        obterTitulo(item);

    const imagem =
        obterImagem(item);

    const ano =
        obterAno(item);

    const favorito =
        estaNosFavoritos(item);

    const id =
        obterIdItem(item);

    const card =
        document.createElement(
            "div"
        );

    card.className =
        "card focusable";

    card.tabIndex = 0;

    card.dataset.id = id;

    card.dataset.tipo = tipo;

    const imagemFallback =
        CONFIG.backgroundUrl;

    card.innerHTML = `
        <img
            class="card-image"
            src="${escaparHtml(
                imagem || imagemFallback
            )}"
            alt="${escaparHtml(
                titulo
            )}"
            loading="lazy"
        >

        <div class="card-info">

            <div class="card-title">
                ${escaparHtml(
                    titulo
                )}
            </div>

            <div class="card-subtitle">
                ${
                    ano
                        ? ano
                        : (
                            tipo === "serie"
                                ? "Série"
                                : "Filme"
                        )
                }
            </div>

        </div>

        ${
            ano === 2026
                ? `
                    <div class="card-badge">
                        2026
                    </div>
                `
                : ""
        }

        ${
            favorito
                ? `
                    <div class="card-favorite">
                        ♥
                    </div>
                `
                : ""
        }
    `;

    const img =
        card.querySelector(
            ".card-image"
        );

    if (img) {

        img.addEventListener(
            "error",
            () => {

                card.classList.add(
                    "image-error"
                );

                img.src =
                    imagemFallback;
            }
        );
    }

    card.addEventListener(
        "click",
        () => {
            abrirItem(
                item,
                tipo
            );
        }
    );

    card.addEventListener(
        "keydown",
        evento => {

            if (
                evento.key ===
                    "Enter" ||
                evento.key ===
                    " "
            ) {
                evento.preventDefault();

                abrirItem(
                    item,
                    tipo
                );
            }
        }
    );

    return card;
}

/* =========================================================
   GRID
========================================================= */

function criarGrid(
    itens,
    tipo
) {
    const grid =
        document.createElement(
            "div"
        );

    grid.className =
        "cardsGrid";

    if (
        !itens ||
        itens.length === 0
    ) {
        grid.innerHTML = `
            <div class="emptyMessage">
                Nenhum conteúdo encontrado.
            </div>
        `;

        return grid;
    }

    itens.forEach(
        item => {

            grid.appendChild(
                criarCard(
                    item,
                    tipo
                )
            );
        }
    );

    return grid;
}

/* =========================================================
   SEÇÃO
========================================================= */

function criarSecao(
    titulo,
    itens,
    tipo,
    categoria = "Todos"
) {
    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const tituloElemento =
        document.createElement(
            "div"
        );

    tituloElemento.className =
        "sectionTitle";

    tituloElemento.textContent =
        titulo;

    section.appendChild(
        tituloElemento
    );

    const filtrados =
        filtrarPorCategoria(
            itens,
            categoria
        );

    const ordenados =
        tipo === "filme"
            ? ordenarFilmes(
                filtrados
            )
            : ordenarPorTitulo(
                filtrados
            );

    section.appendChild(
        criarGrid(
            ordenados,
            tipo
        )
    );

    return section;
}

/* =========================================================
   RENDERIZAÇÃO DE SEÇÃO
========================================================= */

function renderizarSecao(
    itens,
    tipo,
    categoria = "Todos"
) {
    if (!content) return;

    const secaoExistente =
        content.querySelector(
            ".section"
        );

    if (!secaoExistente) {
        return;
    }

    const novaSecao =
        criarSecao(
            secaoExistente
                .querySelector(
                    ".sectionTitle"
                )
                ?.textContent ||
                "Conteúdo",
            itens,
            tipo,
            categoria
        );

    secaoExistente.replaceWith(
        novaSecao
    );

    atualizarFoco();
}

/* =========================================================
   HOME
========================================================= */

function renderizarHome() {

    if (!content) return;

    content.innerHTML = "";

    const filmesOrdenados =
        ordenarFilmes(
            filmes
        );

    const seriesOrdenadas =
        ordenarPorTitulo(
            series
        );

    if (
        filmesOrdenados.length
    ) {
        content.appendChild(
            criarSecao(
                "Filmes",
                filmesOrdenados,
                "filme"
            )
        );
    }

    if (
        seriesOrdenadas.length
    ) {
        content.appendChild(
            criarSecao(
                "Séries",
                seriesOrdenadas,
                "serie"
            )
        );
    }

    if (
        doramas.length
    ) {
        content.appendChild(
            criarSecao(
                "Doramas",
                doramas,
                "dorama"
            )
        );
    }

    atualizarFoco();
}

/* =========================================================
   PÁGINA ATUAL
========================================================= */

function renderizarPaginaAtual() {

    if (
        navegacaoAtual ===
        "favoritos"
    ) {
        renderizarFavoritos();
        return;
    }

    if (
        navegacaoAtual ===
        "continue"
    ) {
        renderizarContinueAssistindo();
        return;
    }

    if (
        navegacaoAtual ===
        "filmes"
    ) {
        renderizarLista(
            filmes,
            "Filmes",
            "filme"
        );
        return;
    }

    if (
        navegacaoAtual ===
        "series"
    ) {
        renderizarLista(
            series,
            "Séries",
            "serie"
        );
        return;
    }

    if (
        navegacaoAtual ===
        "doramas"
    ) {
        renderizarLista(
            doramas,
            "Doramas",
            "dorama"
        );
        return;
    }

    if (
        navegacaoAtual ===
        "anime"
    ) {
        renderizarLista(
            animes,
            "Anime",
            "anime"
        );
        return;
    }

    if (
        navegacaoAtual ===
        "desenhos"
    ) {
        renderizarLista(
            desenhos,
            "Desenhos",
            "desenho"
        );
        return;
    }

    renderizarHome();
}/* =========================================================
   LISTAS
========================================================= */

function renderizarLista(
    itens,
    titulo,
    tipo
) {
    if (!content) return;

    content.innerHTML = "";

    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const sectionTitle =
        document.createElement(
            "div"
        );

    sectionTitle.className =
        "sectionTitle";

    sectionTitle.textContent =
        titulo;

    section.appendChild(
        sectionTitle
    );

    const categoryRow =
        document.createElement(
            "div"
        );

    categoryRow.className =
        "categoryRow";

    section.appendChild(
        categoryRow
    );

    renderizarCategorias(
        categoryRow,
        itens,
        tipo
    );

    const grid =
        criarGrid(
            ordenarFilmes(
                itens
            ),
            tipo
        );

    section.appendChild(
        grid
    );

    content.appendChild(
        section
    );

    categoriaAtual =
        "Todos";

    atualizarFoco();
}

/* =========================================================
   FAVORITOS
========================================================= */

function renderizarFavoritos() {

    if (!content) return;

    content.innerHTML = "";

    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const title =
        document.createElement(
            "div"
        );

    title.className =
        "sectionTitle";

    title.textContent =
        "Favoritos";

    section.appendChild(
        title
    );

    const itens =
        favoritos
            .map(favorito => {

                const encontrado =
                    todosOsItens.find(
                        item =>
                            obterIdItem(
                                item
                            ) ===
                            String(
                                favorito.id ||
                                favorito
                            )
                    );

                return encontrado ||
                    favorito;
            });

    section.appendChild(
        criarGrid(
            itens,
            "favorito"
        )
    );

    content.appendChild(
        section
    );

    atualizarFoco();
}

/* =========================================================
   CONTINUE ASSISTINDO
========================================================= */

function renderizarContinueAssistindo() {

    if (!content) return;

    content.innerHTML = "";

    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const title =
        document.createElement(
            "div"
        );

    title.className =
        "sectionTitle";

    title.textContent =
        "Continue assistindo";

    section.appendChild(
        title
    );

    const itens =
        continueAssistindo.map(
            registro => {

                if (
                    registro.item
                ) {
                    return {
                        ...registro.item,
                        _progresso:
                            registro.progresso ||
                            0,
                        _duracao:
                            registro.duracao ||
                            0
                    };
                }

                return registro;
            }
        );

    const grid =
        document.createElement(
            "div"
        );

    grid.className =
        "cardsGrid";

    if (!itens.length) {

        grid.innerHTML = `
            <div class="emptyMessage">
                Você ainda não começou nenhum conteúdo.
            </div>
        `;

    } else {

        itens.forEach(
            item => {

                const card =
                    criarCard(
                        item,
                        item.tipo ||
                        "filme"
                    );

                const progresso =
                    Number(
                        item._progresso ||
                        item.progresso ||
                        0
                    );

                const duracao =
                    Number(
                        item._duracao ||
                        item.duracao ||
                        0
                    );

                if (
                    duracao > 0 &&
                    progresso > 0
                ) {

                    const porcentagem =
                        Math.min(
                            100,
                            (
                                progresso /
                                duracao
                            ) * 100
                        );

                    const barra =
                        document.createElement(
                            "div"
                        );

                    barra.className =
                        "progressBar";

                    barra.innerHTML = `
                        <div
                            style="width:${porcentagem}%"
                        ></div>
                    `;

                    card.appendChild(
                        barra
                    );
                }

                grid.appendChild(
                    card
                );
            }
        );
    }

    section.appendChild(
        grid
    );

    content.appendChild(
        section
    );

    atualizarFoco();
}

/* =========================================================
   MENU LATERAL
========================================================= */

function criarMenuItem(
    texto,
    acao,
    sub = false
) {
    const botao =
        document.createElement(
            "button"
        );

    botao.type = "button";

    botao.className =
        "menuItem focusable" +
        (
            sub
                ? " menuSubItem"
                : ""
        );

    botao.textContent =
        texto;

    botao.dataset.action =
        acao;

    botao.addEventListener(
        "click",
        () => {

            executarAcaoMenu(
                acao
            );
        }
    );

    return botao;
}

function criarGrupoMenu(
    titulo,
    itens
) {
    const grupo =
        document.createElement(
            "div"
        );

    grupo.className =
        "menuGroup";

    const tituloGrupo =
        document.createElement(
            "div"
        );

    tituloGrupo.className =
        "menuGroupTitle";

    tituloGrupo.textContent =
        titulo;

    grupo.appendChild(
        tituloGrupo
    );

    itens.forEach(
        item => {

            grupo.appendChild(
                criarMenuItem(
                    item.texto,
                    item.acao,
                    item.sub || false
                )
            );
        }
    );

    return grupo;
}

function montarMenu() {

    if (!menuContent) return;

    menuContent.innerHTML = "";

    menuContent.appendChild(
        criarGrupoMenu(
            "Principal",
            [
                {
                    texto:
                        "⌂  Início",
                    acao:
                        "home"
                },
                {
                    texto:
                        "♥  Favoritos",
                    acao:
                        "favoritos"
                },
                {
                    texto:
                        "▶  Continue assistindo",
                    acao:
                        "continue"
                },
                {
                    texto:
                        "⌕  Pesquisar",
                    acao:
                        "pesquisar"
                }
            ]
        )
    );

    menuContent.appendChild(
        criarGrupoMenu(
            "Filmes",
            [
                {
                    texto:
                        "Filmes",
                    acao:
                        "filmes"
                },
                {
                    texto:
                        "Lançamentos",
                    acao:
                        "filmes_2026",
                    sub: true
                }
            ]
        )
    );

    menuContent.appendChild(
        criarGrupoMenu(
            "Séries",
            [
                {
                    texto:
                        "Séries",
                    acao:
                        "series"
                }
            ]
        )
    );

    menuContent.appendChild(
        criarGrupoMenu(
            "Doramas",
            [
                {
                    texto:
                        "Doramas",
                    acao:
                        "doramas"
                }
            ]
        )
    );

    menuContent.appendChild(
        criarGrupoMenu(
            "Anime",
            [
                {
                    texto:
                        "Anime",
                    acao:
                        "anime"
                }
            ]
        )
    );

    menuContent.appendChild(
        criarGrupoMenu(
            "Desenhos",
            [
                {
                    texto:
                        "Desenhos",
                    acao:
                        "desenhos"
                }
            ]
        )
    );
}

/* =========================================================
   AÇÕES DO MENU
========================================================= */

function executarAcaoMenu(
    acao
) {
    switch (acao) {

        case "home":
            navegacaoAtual =
                "home";

            fecharMenu();

            renderizarHome();
            break;

        case "favoritos":
            navegacaoAtual =
                "favoritos";

            fecharMenu();

            renderizarFavoritos();
            break;

        case "continue":
            navegacaoAtual =
                "continue";

            fecharMenu();

            renderizarContinueAssistindo();
            break;

        case "pesquisar":
            fecharMenu();

            abrirPesquisa();
            break;

        case "filmes":
            navegacaoAtual =
                "filmes";

            fecharMenu();

            renderizarLista(
                filmes,
                "Filmes",
                "filme"
            );
            break;

        case "filmes_2026":

            navegacaoAtual =
                "filmes";

            fecharMenu();

            renderizarLista(
                filmes.filter(
                    item =>
                        obterAno(
                            item
                        ) === 2026
                ),
                "Filmes de 2026",
                "filme"
            );
            break;

        case "series":
            navegacaoAtual =
                "series";

            fecharMenu();

            renderizarLista(
                series,
                "Séries",
                "serie"
            );
            break;

        case "doramas":
            navegacaoAtual =
                "doramas";

            fecharMenu();

            renderizarLista(
                doramas,
                "Doramas",
                "dorama"
            );
            break;

        case "anime":
            navegacaoAtual =
                "anime";

            fecharMenu();

            renderizarLista(
                animes,
                "Anime",
                "anime"
            );
            break;

        case "desenhos":
            navegacaoAtual =
                "desenhos";

            fecharMenu();

            renderizarLista(
                desenhos,
                "Desenhos",
                "desenho"
            );
            break;
    }
}

/* =========================================================
   ABRIR / FECHAR MENU
========================================================= */

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

    navegacaoAtual =
        "menu";

    setTimeout(
        () => {

            const primeiro =
                menuContent.querySelector(
                    ".menuItem"
                );

            if (primeiro) {
                primeiro.focus();
            }

        },
        80
    );
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

    navegacaoAtual =
        "conteudo";

    atualizarFoco();
}

/* =========================================================
   PESQUISA
========================================================= */

function abrirPesquisa() {

    if (!searchOverlay) return;

    searchOverlay.classList.remove(
        "hidden"
    );

    if (searchInput) {

        searchInput.value =
            "";

        setTimeout(
            () => {
                searchInput.focus();
            },
            100
        );
    }
}

function fecharPesquisa() {

    if (!searchOverlay) return;

    searchOverlay.classList.add(
        "hidden"
    );

    if (searchInput) {
        searchInput.value = "";
    }

    atualizarFoco();
}

function executarPesquisa(
    termo
) {

    const busca =
        normalizarTexto(
            termo
        );

    if (!busca) {
        renderizarHome();
        return;
    }

    const resultados =
        todosOsItens.filter(
            item => {

                const texto =
                    normalizarTexto(
                        [
                            obterTitulo(
                                item
                            ),
                            obterCategoria(
                                item
                            ),
                            item.genero ||
                                "",
                            item.ano ||
                                ""
                        ].join(" ")
                    );

                return texto.includes(
                    busca
                );
            }
        );

    content.innerHTML = "";

    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const title =
        document.createElement(
            "div"
        );

    title.className =
        "sectionTitle";

    title.textContent =
        `Resultados para: ${termo}`;

    section.appendChild(
        title
    );

    section.appendChild(
        criarGrid(
            resultados,
            "busca"
        )
    );

    content.appendChild(
        section
    );

    atualizarFoco();
}

if (searchInput) {

    searchInput.addEventListener(
        "input",
        evento => {

            executarPesquisa(
                evento.target.value
            );
        }
    );
}/* =========================================================
   ABRIR ITEM
========================================================= */

function abrirItem(
    item,
    tipo = "filme"
) {
    if (!item) return;

    itemAtual = item;

    const titulo =
        obterTitulo(item);

    /*
     * Séries podem ter temporadas/episódios
     * dentro de diferentes propriedades.
     */

    const temporadas =
        obterTemporadas(item);

    if (
        tipo === "serie" ||
        tipo === "dorama" ||
        temporadas.length > 0
    ) {

        abrirSerie(
            item,
            tipo
        );

        return;
    }

    const video =
        obterVideo(item);

    if (!video) {

        mostrarToast(
            "Vídeo não encontrado."
        );

        return;
    }

    abrirPlayer(
        video,
        titulo,
        item
    );
}

/* =========================================================
   TEMPORADAS
========================================================= */

function obterTemporadas(
    serie
) {
    if (!serie) return [];

    const possiveis = [
        serie.temporadas,
        serie.seasons,
        serie.season,
        serie.episodes
    ];

    for (
        const valor of possiveis
    ) {

        if (
            Array.isArray(valor) &&
            valor.length
        ) {
            return valor;
        }
    }

    return [];
}

function obterNumeroTemporada(
    temporada,
    indice
) {
    const numero =
        obterValor(
            temporada,
            [
                "numero",
                "number",
                "season",
                "temporada",
                "id"
            ],
            indice + 1
        );

    const valor =
        parseInt(
            numero,
            10
        );

    return Number.isNaN(valor)
        ? indice + 1
        : valor;
}

function obterNomeTemporada(
    temporada,
    indice
) {
    return (
        obterValor(
            temporada,
            [
                "nome",
                "name",
                "titulo",
                "title"
            ],
            ""
        ) ||
        `Temporada ${
            obterNumeroTemporada(
                temporada,
                indice
            )
        }`
    );
}

/* =========================================================
   EPISÓDIOS
========================================================= */

function obterEpisodios(
    temporada
) {
    if (!temporada) return [];

    const possiveis = [
        temporada.episodios,
        temporada.episodes,
        temporada.ep,
        temporada.lista
    ];

    for (
        const valor of possiveis
    ) {

        if (
            Array.isArray(valor)
        ) {
            return valor;
        }
    }

    /*
     * Alguns JSONs podem possuir
     * os episódios diretamente.
     */

    if (
        temporada.video ||
        temporada.url ||
        temporada.link
    ) {
        return [
            temporada
        ];
    }

    return [];
}

function obterNumeroEpisodio(
    episodio,
    indice
) {
    const numero =
        obterValor(
            episodio,
            [
                "numero",
                "number",
                "episode",
                "episodio",
                "ep"
            ],
            indice + 1
        );

    const valor =
        parseInt(
            numero,
            10
        );

    return Number.isNaN(valor)
        ? indice + 1
        : valor;
}

function obterNomeEpisodio(
    episodio,
    indice
) {
    const numero =
        obterNumeroEpisodio(
            episodio,
            indice
        );

    const titulo =
        obterValor(
            episodio,
            [
                "nome",
                "name",
                "titulo",
                "title"
            ],
            ""
        );

    if (titulo) {
        return (
            `E${numero} - ${titulo}`
        );
    }

    return `Episódio ${numero}`;
}

/* =========================================================
   ABRIR SÉRIE
========================================================= */

function abrirSerie(
    serie,
    tipo
) {
    const temporadas =
        obterTemporadas(
            serie
        );

    /*
     * Caso o JSON tenha episódios diretamente
     * sem uma estrutura explícita de temporadas.
     */

    if (
        !temporadas.length &&
        (
            Array.isArray(
                serie.episodios
            ) ||
            Array.isArray(
                serie.episodes
            )
        )
    ) {

        temporadas.push({
            numero: 1,
            nome: "Temporada 1",
            episodios:
                serie.episodios ||
                serie.episodes
        });
    }

    if (
        !temporadas.length
    ) {

        const video =
            obterVideo(serie);

        if (video) {

            abrirPlayer(
                video,
                obterTitulo(
                    serie
                ),
                serie
            );

        } else {

            mostrarToast(
                "Nenhum episódio encontrado."
            );
        }

        return;
    }

    temporadaAtual =
        temporadas[0];

    episodioAtual =
        null;

    renderizarDetalhesSerie(
        serie,
        tipo,
        temporadas
    );
}

/* =========================================================
   DETALHES DA SÉRIE
========================================================= */

function renderizarDetalhesSerie(
    serie,
    tipo,
    temporadas
) {
    if (!content) return;

    content.innerHTML = "";

    const section =
        document.createElement(
            "section"
        );

    section.className =
        "section";

    const titulo =
        document.createElement(
            "div"
        );

    titulo.className =
        "sectionTitle";

    titulo.textContent =
        obterTitulo(
            serie
        );

    section.appendChild(
        titulo
    );

    /*
     * Botões de temporadas
     */

    const seasonRow =
        document.createElement(
            "div"
        );

    seasonRow.className =
        "categoryRow";

    temporadas.forEach(
        (
            temporada,
            indice
        ) => {

            const botao =
                document.createElement(
                    "button"
                );

            botao.type =
                "button";

            botao.className =
                "categoryButton focusable";

            botao.textContent =
                obterNomeTemporada(
                    temporada,
                    indice
                );

            botao.dataset.season =
                indice;

            if (
                temporada ===
                temporadaAtual
            ) {
                botao.classList.add(
                    "active"
                );
            }

            botao.addEventListener(
                "click",
                () => {

                    temporadaAtual =
                        temporada;

                    seasonRow
                        .querySelectorAll(
                            ".categoryButton"
                        )
                        .forEach(
                            item =>
                                item.classList.remove(
                                    "active"
                                )
                        );

                    botao.classList.add(
                        "active"
                    );

                    renderizarEpisodios(
                        episodiosDaTemporada(
                            temporada
                        ),
                        serie,
                        tipo
                    );
                }
            );

            seasonRow.appendChild(
                botao
            );
        }
    );

    section.appendChild(
        seasonRow
    );

    const episodioContainer =
        document.createElement(
            "div"
        );

    episodioContainer.id =
        "episodesContainer";

    section.appendChild(
        episodioContainer
    );

    content.appendChild(
        section
    );

    renderizarEpisodios(
        episodiosDaTemporada(
            temporadaAtual
        ),
        serie,
        tipo
    );

    atualizarFoco();
}

function episodiosDaTemporada(
    temporada
) {
    return obterEpisodios(
        temporada
    );
}

/* =========================================================
   RENDERIZAR EPISÓDIOS
========================================================= */

function renderizarEpisodios(
    episodios,
    serie,
    tipo
) {
    const container =
        document.getElementById(
            "episodesContainer"
        );

    if (!container) return;

    container.innerHTML = "";

    if (
        !episodios ||
        !episodios.length
    ) {

        container.innerHTML = `
            <div class="emptyMessage">
                Nenhum episódio encontrado nesta temporada.
            </div>
        `;

        atualizarFoco();

        return;
    }

    const grid =
        document.createElement(
            "div"
        );

    grid.className =
        "cardsGrid";

    episodios.forEach(
        (
            episodio,
            indice
        ) => {

            /*
             * Mantém o objeto original do episódio,
             * incluindo o link de vídeo.
             */

            const episodioCard =
                document.createElement(
                    "div"
                );

            episodioCard.className =
                "card focusable";

            episodioCard.tabIndex =
                0;

            const imagem =
                obterImagem(
                    episodio
                ) ||
                obterImagem(
                    serie
                ) ||
                CONFIG.backgroundUrl;

            const numero =
                obterNumeroEpisodio(
                    episodio,
                    indice
                );

            const nome =
                obterNomeEpisodio(
                    episodio,
                    indice
                );

            episodioCard.innerHTML = `
                <img
                    class="card-image"
                    src="${escaparHtml(
                        imagem
                    )}"
                    alt="${escaparHtml(
                        nome
                    )}"
                    loading="lazy"
                >

                <div class="card-info">

                    <div class="card-title">
                        ${escaparHtml(
                            nome
                        )}
                    </div>

                    <div class="card-subtitle">
                        ${escaparHtml(
                            obterNomeTemporada(
                                temporadaAtual,
                                0
                            )
                        )}
                    </div>

                </div>

                <div class="card-badge">
                    E${numero}
                </div>
            `;

            const img =
                episodioCard.querySelector(
                    ".card-image"
                );

            if (img) {

                img.addEventListener(
                    "error",
                    () => {

                        episodioCard.classList.add(
                            "image-error"
                        );

                        img.src =
                            CONFIG.backgroundUrl;
                    }
                );
            }

            episodioCard.addEventListener(
                "click",
                () => {

                    const video =
                        obterVideo(
                            episodio
                        );

                    if (!video) {

                        mostrarToast(
                            "Vídeo deste episódio não encontrado."
                        );

                        return;
                    }

                    episodioAtual =
                        episodio;

                    abrirPlayer(
                        video,
                        `${obterTitulo(
                            serie
                        )} - ${nome}`,
                        {
                            ...serie,
                            episodio:
                                episodio,
                            temporada:
                                temporadaAtual
                        }
                    );
                }
            );

            episodioCard.addEventListener(
                "keydown",
                evento => {

                    if (
                        evento.key ===
                            "Enter" ||
                        evento.key ===
                            " "
                    ) {

                        evento.preventDefault();

                        episodioCard.click();
                    }
                }
            );

            grid.appendChild(
                episodioCard
            );
        }
    );

    container.appendChild(
        grid
    );

    atualizarFoco();
}/* =========================================================
   PLAYER
========================================================= */

function abrirPlayer(
    video,
    titulo,
    item = null
) {
    if (!video) {

        mostrarToast(
            "Vídeo não encontrado."
        );

        return;
    }

    if (!playerOverlay) return;

    itemAtual =
        item || itemAtual;

    playerTitle.textContent =
        titulo || "WOLF IPTV";

    playerError.style.display =
        "none";

    playerLoading.style.display =
        "block";

    playerOverlay.classList.remove(
        "hidden"
    );

    navegacaoAtual =
        "player";

    videoPlayer.pause();

    videoPlayer.removeAttribute(
        "src"
    );

    videoPlayer.load();

    /*
     * O webOS usa o elemento HTML5
     * para reprodução dos vídeos.
     */

    videoPlayer.src = video;

    videoPlayer.load();

    videoPlayer.onloadedmetadata =
        () => {

            playerLoading.style.display =
                "none";

            restaurarProgresso(
                itemAtual
            );

            const promessa =
                videoPlayer.play();

            if (
                promessa &&
                typeof promessa.catch ===
                    "function"
            ) {
                promessa.catch(
                    erro => {

                        console.warn(
                            "Autoplay bloqueado:",
                            erro
                        );
                    }
                );
            }
        };

    videoPlayer.onerror =
        () => {

            playerLoading.style.display =
                "none";

            playerError.style.display =
                "block";

            mostrarToast(
                "Não foi possível reproduzir este vídeo."
            );
        };

    videoPlayer.ontimeupdate =
        () => {

            salvarProgresso(
                itemAtual
            );
        };
}

/* =========================================================
   FECHAR PLAYER
========================================================= */

function fecharPlayer() {

    if (!playerOverlay) return;

    salvarProgresso(
        itemAtual
    );

    videoPlayer.pause();

    videoPlayer.removeAttribute(
        "src"
    );

    videoPlayer.load();

    playerOverlay.classList.add(
        "hidden"
    );

    playerLoading.style.display =
        "none";

    playerError.style.display =
        "none";

    navegacaoAtual =
        "conteudo";

    atualizarFoco();
}

/* =========================================================
   CONTINUE ASSISTINDO
========================================================= */

function obterRegistroContinue(
    item
) {
    if (!item) return null;

    const id =
        obterIdItem(item);

    return continueAssistindo.find(
        registro =>
            String(
                registro.id ||
                (
                    registro.item
                        ? obterIdItem(
                            registro.item
                        )
                        : ""
                )
            ) === id
    );
}

function salvarProgresso(
    item
) {
    if (
        !item ||
        !videoPlayer ||
        !videoPlayer.duration ||
        !isFinite(
            videoPlayer.duration
        )
    ) {
        return;
    }

    const progresso =
        videoPlayer.currentTime || 0;

    const duracao =
        videoPlayer.duration || 0;

    /*
     * Se chegou praticamente ao final,
     * remove da lista de "Continue assistindo".
     */

    if (
        duracao > 0 &&
        progresso >=
            duracao - 15
    ) {

        removerContinue(
            item
        );

        return;
    }

    const id =
        obterIdItem(item);

    const registro = {
        id: id,

        titulo:
            obterTitulo(item),

        capa:
            obterImagem(item),

        video:
            obterVideo(item),

        categoria:
            obterCategoria(item),

        tipo:
            item.tipo ||
            (
                item.episodio
                    ? "serie"
                    : "filme"
            ),

        progresso:
            progresso,

        duracao:
            duracao,

        item:
            item
    };

    const indice =
        continueAssistindo.findIndex(
            existente =>
                String(
                    existente.id ||
                    (
                        existente.item
                            ? obterIdItem(
                                existente.item
                            )
                            : ""
                    )
                ) === id
        );

    if (indice >= 0) {

        continueAssistindo[
            indice
        ] = registro;

    } else {

        continueAssistindo.unshift(
            registro
        );
    }

    /*
     * Mantém apenas os conteúdos
     * mais recentes.
     */

    if (
        continueAssistindo.length >
        50
    ) {
        continueAssistindo =
            continueAssistindo.slice(
                0,
                50
            );
    }

    salvarContinueAssistindo();
}

function restaurarProgresso(
    item
) {
    if (
        !item ||
        !videoPlayer
    ) {
        return;
    }

    const registro =
        obterRegistroContinue(
            item
        );

    if (
        !registro ||
        !registro.progresso
    ) {
        return;
    }

    const aplicar =
        () => {

            try {

                if (
                    registro.progresso <
                    videoPlayer.duration
                ) {

                    videoPlayer.currentTime =
                        registro.progresso;
                }

            } catch (erro) {

                console.warn(
                    "Erro ao restaurar progresso:",
                    erro
                );
            }
        };

    if (
        videoPlayer.readyState >= 1
    ) {

        aplicar();

    } else {

        videoPlayer.addEventListener(
            "loadedmetadata",
            aplicar,
            {
                once: true
            }
        );
    }
}

function removerContinue(
    item
) {
    if (!item) return;

    const id =
        obterIdItem(item);

    continueAssistindo =
        continueAssistindo.filter(
            registro =>
                String(
                    registro.id ||
                    (
                        registro.item
                            ? obterIdItem(
                                registro.item
                            )
                            : ""
                    )
                ) !== id
        );

    salvarContinueAssistindo();
}

/* =========================================================
   NAVEGAÇÃO D-PAD
========================================================= */

function atualizarFoco() {

    if (
        menuAberto ||
        (
            playerOverlay &&
            !playerOverlay.classList.contains(
                "hidden"
            )
        ) ||
        (
            searchOverlay &&
            !searchOverlay.classList.contains(
                "hidden"
            )
        )
    ) {
        return;
    }

    elementosFocaveis =
        Array.from(
            document.querySelectorAll(
                "#content .card.focusable, " +
                "#content .categoryButton.focusable"
            )
        );

    if (
        !elementosFocaveis.length
    ) {
        return;
    }

    focoAtual =
        Math.min(
            focoAtual,
            elementosFocaveis.length - 1
        );

    elementosFocaveis.forEach(
        elemento =>
            elemento.classList.remove(
                "focused"
            )
    );

    const elemento =
        elementosFocaveis[
            focoAtual
        ];

    if (!elemento) return;

    elemento.classList.add(
        "focused"
    );

    try {

        elemento.focus({
            preventScroll:
                true
        });

    } catch (erro) {

        elemento.focus();
    }

    elemento.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "nearest"
    });
}

/* =========================================================
   D-PAD — MENU
========================================================= */

function navegarMenu(
    direcao
) {
    const itens =
        Array.from(
            menuContent.querySelectorAll(
                ".menuItem.focusable"
            )
        );

    if (!itens.length) return;

    let atual =
        itens.indexOf(
            document.activeElement
        );

    if (atual < 0) {
        atual = 0;
    }

    if (
        direcao === "up"
    ) {
        atual =
            Math.max(
                0,
                atual - 1
            );
    }

    if (
        direcao === "down"
    ) {
        atual =
            Math.min(
                itens.length - 1,
                atual + 1
            );
    }

    itens[atual].focus();

    itens[atual].scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}

/* =========================================================
   D-PAD — CONTEÚDO
========================================================= */

function obterPosicaoCard(
    elemento
) {
    const rect =
        elemento.getBoundingClientRect();

    return {
        x:
            rect.left +
            rect.width / 2,

        y:
            rect.top +
            rect.height / 2
    };
}

function navegarConteudo(
    direcao
) {
    const itens =
        elementosFocaveis;

    if (!itens.length) return;

    const atual =
        itens[
            focoAtual
        ];

    if (!atual) return;

    const posAtual =
        obterPosicaoCard(
            atual
        );

    let melhor = -1;

    let melhorDistancia =
        Infinity;

    itens.forEach(
        (
            elemento,
            indice
        ) => {

            if (
                elemento === atual
            ) {
                return;
            }

            const pos =
                obterPosicaoCard(
                    elemento
                );

            const dx =
                pos.x -
                posAtual.x;

            const dy =
                pos.y -
                posAtual.y;

            let valido = false;

            if (
                direcao === "left"
            ) {
                valido =
                    dx < -10;
            }

            if (
                direcao === "right"
            ) {
                valido =
                    dx > 10;
            }

            if (
                direcao === "up"
            ) {
                valido =
                    dy < -10;
            }

            if (
                direcao === "down"
            ) {
                valido =
                    dy > 10;
            }

            if (!valido) return;

            const distancia =
                Math.sqrt(
                    dx * dx +
                    dy * dy
                );

            if (
                distancia <
                melhorDistancia
            ) {
                melhorDistancia =
                    distancia;

                melhor =
                    indice;
            }
        }
    );

    if (melhor >= 0) {

        focoAtual =
            melhor;

        atualizarFoco();
    }
        }/* =========================================================
   TECLADO / CONTROLE REMOTO
========================================================= */

document.addEventListener(
    "keydown",
    evento => {

        const tecla =
            evento.key;

        /* ================================================
           PLAYER
        ================================================ */

        if (
            playerOverlay &&
            !playerOverlay.classList.contains(
                "hidden"
            )
        ) {

            if (
                tecla === "Escape" ||
                tecla === "Backspace"
            ) {

                evento.preventDefault();

                fecharPlayer();

                return;
            }

            return;
        }

        /* ================================================
           PESQUISA
        ================================================ */

        if (
            searchOverlay &&
            !searchOverlay.classList.contains(
                "hidden"
            )
        ) {

            if (
                tecla === "Escape" ||
                tecla === "Backspace"
            ) {

                evento.preventDefault();

                fecharPesquisa();

                return;
            }

            if (
                tecla === "Enter"
            ) {

                evento.preventDefault();

                if (
                    searchInput &&
                    searchInput.value
                ) {

                    executarPesquisa(
                        searchInput.value
                    );

                    fecharPesquisa();
                }

                return;
            }

            return;
        }

        /* ================================================
           MENU
        ================================================ */

        if (menuAberto) {

            if (
                tecla === "ArrowUp"
            ) {

                evento.preventDefault();

                navegarMenu(
                    "up"
                );

                return;
            }

            if (
                tecla === "ArrowDown"
            ) {

                evento.preventDefault();

                navegarMenu(
                    "down"
                );

                return;
            }

            if (
                tecla === "ArrowLeft" ||
                tecla === "Escape" ||
                tecla === "Backspace"
            ) {

                evento.preventDefault();

                fecharMenu();

                return;
            }

            if (
                tecla === "ArrowRight" ||
                tecla === "Enter"
            ) {

                evento.preventDefault();

                if (
                    document.activeElement &&
                    document.activeElement.classList.contains(
                        "menuItem"
                    )
                ) {

                    document.activeElement.click();
                }

                return;
            }

            return;
        }

        /* ================================================
           D-PAD
        ================================================ */

        if (
            tecla === "ArrowUp" ||
            tecla === "ArrowDown" ||
            tecla === "ArrowLeft" ||
            tecla === "ArrowRight"
        ) {

            evento.preventDefault();

            navegarConteudo(
                tecla === "ArrowUp"
                    ? "up"
                    : tecla === "ArrowDown"
                        ? "down"
                        : tecla === "ArrowLeft"
                            ? "left"
                            : "right"
            );

            return;
        }

        /* ================================================
           ENTER
        ================================================ */

        if (
            tecla === "Enter" ||
            tecla === " "
        ) {

            evento.preventDefault();

            const elemento =
                elementosFocaveis[
                    focoAtual
                ];

            if (
                elemento
            ) {

                elemento.click();
            }

            return;
        }

        /* ================================================
           VOLTAR
        ================================================ */

        if (
            tecla === "Escape" ||
            tecla === "Backspace"
        ) {

            evento.preventDefault();

            if (
                navegacaoAtual !==
                "home"
            ) {

                navegacaoAtual =
                    "home";

                focoAtual = 0;

                renderizarHome();

            } else {

                abrirMenu();
            }

            return;
        }

        /* ================================================
           MENU
        ================================================ */

        if (
            tecla === "m" ||
            tecla === "M"
        ) {

            evento.preventDefault();

            if (menuAberto) {
                fecharMenu();
            } else {
                abrirMenu();
            }

            return;
        }

        /* ================================================
           PESQUISA
        ================================================ */

        if (
            tecla === "/" ||
            tecla === "f" ||
            tecla === "F"
        ) {

            evento.preventDefault();

            abrirPesquisa();

            return;
        }
    }
);

/* =========================================================
   BOTÃO DO MENU
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
        () => {

            fecharMenu();
        }
    );
}

/* =========================================================
   BOTÃO FECHAR PLAYER
========================================================= */

if (closePlayer) {

    closePlayer.addEventListener(
        "click",
        () => {

            fecharPlayer();
        }
    );
}

/* =========================================================
   CLICK FORA DO MENU
========================================================= */

document.addEventListener(
    "click",
    evento => {

        if (!menuAberto) {
            return;
        }

        if (
            sideMenu.contains(
                evento.target
            )
        ) {
            return;
        }

        if (
            menuButton &&
            menuButton.contains(
                evento.target
            )
        ) {
            return;
        }

        fecharMenu();
    }
);

/* =========================================================
   TECLAS ESPECIAIS WEBOS
========================================================= */

document.addEventListener(
    "webOSRelaunch",
    () => {

        renderizarHome();

    }
);

/* =========================================================
   VISIBILIDADE
========================================================= */

document.addEventListener(
    "visibilitychange",
    () => {

        if (
            document.hidden
        ) {

            salvarProgresso(
                itemAtual
            );
        }
    }
);

window.addEventListener(
    "beforeunload",
    () => {

        salvarProgresso(
            itemAtual
        );
    }
);

/* =========================================================
   INICIALIZAÇÃO
========================================================= */

async function iniciarAplicativo() {

    try {

        configurarBackground();

        montarMenu();

        /*
         * Garante que os arrays do
         * armazenamento estejam válidos.
         */

        if (
            !Array.isArray(
                favoritos
            )
        ) {
            favoritos = [];
        }

        if (
            !Array.isArray(
                continueAssistindo
            )
        ) {
            continueAssistindo = [];
        }

        /*
         * Mantém o foco inicial
         * no primeiro conteúdo.
         */

        focoAtual = 0;

        navegacaoAtual =
            "home";

        /*
         * Carrega os dois JSONs.
         */

        await carregarCatalogos();

        /*
         * Aguarda o navegador montar
         * os cards antes de aplicar foco.
         */

        setTimeout(
            () => {

                atualizarFoco();

            },
            250
        );

    } catch (erro) {

        console.error(
            "Erro ao iniciar WOLF IPTV:",
            erro
        );

        if (content) {

            content.innerHTML = `
                <div class="errorMessage">
                    Erro ao iniciar o WOLF IPTV.
                </div>
            `;
        }
    }
}

/* =========================================================
   EVENTO DOM READY
========================================================= */

if (
    document.readyState ===
    "loading"
) {

    document.addEventListener(
        "DOMContentLoaded",
        iniciarAplicativo
    );

} else {

    iniciarAplicativo();
}

/* =========================================================
   EXPORTAÇÕES GLOBAIS
========================================================= */

window.WOLF = {

    abrirMenu,
    fecharMenu,

    abrirPesquisa,
    fecharPesquisa,

    abrirPlayer,
    fecharPlayer,

    renderizarHome,
    renderizarPaginaAtual,

    alternarFavorito,

    salvarProgresso
};

console.log(
    "🐺 WOLF IPTV webOS iniciado."
);
