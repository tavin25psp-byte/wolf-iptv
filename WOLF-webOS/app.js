(function () {
    "use strict";

    var BASE_URL =
        "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/";

    var CATALOGO_URL = BASE_URL + "catalogo.json";

    var SERIES_URLS = [
        BASE_URL + "series.json",
        BASE_URL + "serie.json",
        BASE_URL + "s%C3%A9rie.json"
    ];

    var DORAMAS_URLS = [
        BASE_URL + "doramas.json",
        BASE_URL + "Doramas.json"
    ];

    var ANIMES_URLS = [
        BASE_URL + "animes.json",
        BASE_URL + "anime.json",
        BASE_URL + "Animes.json",
        BASE_URL + "Anime.json"
    ];

    var DESENHOS_URLS = [
        BASE_URL + "desenho.json",
        BASE_URL + "desenhos.json",
        BASE_URL + "Desenho.json",
        BASE_URL + "Desenhos.json"
    ];

    var BACKGROUND_URL =
        "https://i.postimg.cc/Ghk8PP7w/wolf.png";

    var filmes = [];
    var series = [];
    var doramas = [];
    var animes = [];
    var desenhos = [];

    var catalogosCarregados = false;
    var tipoAtual = "inicio";
    var categoriaAtual = "";
    var serieAtual = null;
    var temporadasAtual = [];
    var historicoConteudo = [];

    var elementosFocaveis = [];
    var indiceFoco = 0;

    var favoritos = [];
    var continuarAssistindo = [];

    var menuAberto = false;
    var pesquisaAberta = false;
    var playerAberto = false;

    var content;
    var contentScroll;
    var sideMenu;
    var menuContent;
    var menuButton;
    var closeMenu;
    var searchOverlay;
    var searchInput;
    var playerOverlay;
    var videoPlayer;
    var playerTitle;
    var playerLoading;
    var playerError;
    var toast;

    function qs(id) {
        return document.getElementById(id);
    }

    function iniciarElementos() {
        content = qs("content");
        contentScroll = qs("contentScroll");
        sideMenu = qs("sideMenu");
        menuContent = qs("menuContent");
        menuButton = qs("menuButton");
        closeMenu = qs("closeMenu");
        searchOverlay = qs("searchOverlay");
        searchInput = qs("searchInput");
        playerOverlay = qs("playerOverlay");
        videoPlayer = qs("videoPlayer");
        playerTitle = qs("playerTitle");
        playerLoading = qs("playerLoading");
        playerError = qs("playerError");
        toast = qs("toast");

        aplicarBackground();
    }

    function aplicarBackground() {
        var background = qs("background");

        if (background) {
            background.style.backgroundImage =
                'url("' + BACKGROUND_URL + '")';
        }
    }

    function mostrarToast(mensagem) {
        if (!toast) {
            return;
        }

        toast.textContent = mensagem;
        toast.classList.add("show");

        window.clearTimeout(mostrarToast.timer);

        mostrarToast.timer = window.setTimeout(function () {
            toast.classList.remove("show");
        }, 2500);
    }

    function normalizarTexto(valor) {
        if (valor === null || valor === undefined) {
            return "";
        }

        return String(valor)
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

    function escaparHTML(valor) {
        if (valor === null || valor === undefined) {
            return "";
        }

        return String(valor)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function obterTitulo(item) {
        if (!item) {
            return "";
        }

        return item.titulo ||
            item.title ||
            item.nome ||
            "";
    }

    function obterImagem(item) {
        if (!item) {
            return "";
        }

        return item.capa ||
            item.poster ||
            item.imagem ||
            item.image ||
            item._capa ||
            "";
    }

    function obterCategoria(item) {
        if (!item) {
            return "";
        }

        return item.categoria ||
            item.category ||
            item._serieCategoria ||
            "";
    }

    function obterVideo(item) {
        if (!item) {
            return "";
        }

        return item.video ||
            item.url ||
            item.link ||
            "";
    }

    function obterAno(item) {
        if (!item) {
            return "";
        }

        return item.ano ||
            item.year ||
            "";
    }

    function ordenarTitulo(a, b) {
        return normalizarTexto(
            obterTitulo(a)
        ).localeCompare(
            normalizarTexto(
                obterTitulo(b)
            )
        );
    }

    function ordenarFilmes(a, b) {
        var anoA = Number(obterAno(a)) || 0;
        var anoB = Number(obterAno(b)) || 0;

        if (anoA !== anoB) {
            return anoB - anoA;
        }

        return ordenarTitulo(a, b);
    }

    function gerarId(item) {
        if (!item) {
            return "";
        }

        if (item._tipo === "episodio") {
            return [
                "episodio",
                normalizarTexto(item._serieTitulo),
                String(item._temporadaNumero),
                String(item.numero)
            ].join("|");
        }

        if (item._tipo === "temporada") {
            return [
                "temporada",
                normalizarTexto(item._serieTitulo),
                String(item.numero)
            ].join("|");
        }

        if (item._tipo === "serie") {
            return [
                "serie",
                normalizarTexto(obterTitulo(item))
            ].join("|");
        }

        return [
            item._tipo || "item",
            normalizarTexto(obterTitulo(item)),
            normalizarTexto(obterVideo(item))
        ].join("|");
    }

    function carregarLocalStorage() {
        try {
            favoritos =
                JSON.parse(
                    localStorage.getItem("wolf_favoritos") || "[]"
                );

            continuarAssistindo =
                JSON.parse(
                    localStorage.getItem("wolf_continue") || "[]"
                );

            if (!Array.isArray(favoritos)) {
                favoritos = [];
            }

            if (!Array.isArray(continuarAssistindo)) {
                continuarAssistindo = [];
            }
        } catch (erro) {
            favoritos = [];
            continuarAssistindo = [];
        }
    }

    function salvarLocalStorage() {
        try {
            localStorage.setItem(
                "wolf_favoritos",
                JSON.stringify(favoritos)
            );

            localStorage.setItem(
                "wolf_continue",
                JSON.stringify(continuarAssistindo)
            );
        } catch (erro) {
            console.log("Erro ao salvar dados locais:", erro);
        }
    }

    function estaFavorito(item) {
        var id = gerarId(item);

        return favoritos.some(function (favorito) {
            return favorito._id === id;
        });
    }

    function alternarFavorito(item) {
        if (!item) {
            return;
        }

        var id = gerarId(item);
        var posicao = -1;

        for (var i = 0; i < favoritos.length; i++) {
            if (favoritos[i]._id === id) {
                posicao = i;
                break;
            }
        }

        if (posicao >= 0) {
            favoritos.splice(posicao, 1);
            mostrarToast("Removido dos favoritos");
        } else {
            var favorito = copiarItem(item);

            favorito._id = id;

            if (item._tipo === "episodio") {
                favorito._serieTitulo = item._serieTitulo;
                favorito._temporadaNumero = item._temporadaNumero;
                favorito._serieCategoria = item._serieCategoria;
                favorito._capa = item._capa;
            }

            favoritos.push(favorito);
            mostrarToast("Adicionado aos favoritos");
        }

        salvarLocalStorage();

        if (tipoAtual === "favoritos") {
            renderizarFavoritos();
        }
    }

    function copiarItem(item) {
        var copia = {};

        Object.keys(item).forEach(function (chave) {
            copia[chave] = item[chave];
        });

        return copia;
    }

    function registrarContinueAssistindo(item, progresso, duracao) {
        if (!item) {
            return;
        }

        var video = obterVideo(item);

        if (!video) {
            return;
        }

        var registro = copiarItem(item);

        registro._id = gerarId(item);
        registro._progresso = Number(progresso) || 0;
        registro._duracao = Number(duracao) || 0;
        registro._ultimaVez = Date.now();

        var encontrou = -1;

        for (var i = 0; i < continuarAssistindo.length; i++) {
            if (continuarAssistindo[i]._id === registro._id) {
                encontrou = i;
                break;
            }
        }

        if (encontrou >= 0) {
            continuarAssistindo.splice(encontrou, 1);
        }

        continuarAssistindo.unshift(registro);

        if (continuarAssistindo.length > 50) {
            continuarAssistindo =
                continuarAssistindo.slice(0, 50);
        }

        salvarLocalStorage();
    }

    function removerContinue(item) {
        var id = gerarId(item);

        continuarAssistindo =
            continuarAssistindo.filter(function (registro) {
                return registro._id !== id;
            });

        salvarLocalStorage();
    }

    function carregarImagem(img, url) {
        if (!img) {
            return;
        }

        if (!url) {
            img.src = "";
            img.classList.add("imageError");
            return;
        }

        img.onload = function () {
            img.classList.remove("imageError");
        };

        img.onerror = function () {
            img.classList.add("imageError");
        };

        img.src = url;
    }

    function criarCard(item, tipo) {
        var card = document.createElement("div");

        card.className = "movieCard focusable";
        card.setAttribute("tabindex", "0");

        var id = gerarId(item);

        card.setAttribute("data-id", id);

        var imagem = obterImagem(item);
        var titulo = obterTitulo(item);
        var categoria = obterCategoria(item);
        var ano = obterAno(item);

        var detalhe = "";

        if (tipo === "filme" || tipo === "desenho") {
            detalhe = [
                ano ? String(ano) : "",
                categoria
            ].filter(Boolean).join(" • ");
        }

        if (tipo === "serie" ||
            tipo === "dorama" ||
            tipo === "anime") {

            var totalTemporadas =
                item.temporadas &&
                Array.isArray(item.temporadas)
                    ? item.temporadas.length
                    : 0;

            detalhe = [
                categoria,
                totalTemporadas +
                    (totalTemporadas === 1
                        ? " temporada"
                        : " temporadas")
            ].filter(Boolean).join(" • ");
        }

        if (tipo === "episodio") {
            detalhe =
                "EP " +
                String(item.numero || "") +
                " • " +
                String(item.titulo || "");
        }

        if (tipo === "temporada") {
            detalhe =
                "Temporada " +
                String(item.numero || "");
        }

        if (tipo === "continue") {
            if (item._tipo === "episodio") {
                detalhe =
                    String(item._serieTitulo || "") +
                    " • T" +
                    String(item._temporadaNumero || "") +
                    " E" +
                    String(item.numero || "");
            } else {
                detalhe =
                    categoria ||
                    (ano ? String(ano) : "");
            }
        }

        var favorito = estaFavorito(item);

        card.innerHTML =
            '<div class="cardImageWrap">' +
                '<img class="cardImage" alt="">' +
                (favorito
                    ? '<div class="favoriteMark">♥</div>'
                    : '') +
                (tipo === "continue"
                    ? '<div class="continueBadge">▶</div>'
                    : '') +
            '</div>' +
            '<div class="cardTitle">' +
                escaparHTML(titulo) +
            '</div>' +
            '<div class="cardMeta">' +
                escaparHTML(detalhe) +
            '</div>';

        var img = card.querySelector(".cardImage");

        carregarImagem(img, imagem);

        if (tipo === "continue") {
            var progresso =
                Number(item._progresso) || 0;

            var duracao =
                Number(item._duracao) || 0;

            if (duracao > 0) {
                var porcentagem =
                    Math.max(
                        0,
                        Math.min(
                            100,
                            (progresso / duracao) * 100
                        )
                    );

                var barra =
                    document.createElement("div");

                barra.className = "continueProgress";

                barra.innerHTML =
                    '<div class="continueProgressFill" style="width:' +
                    porcentagem +
                    '%"></div>';

                card.appendChild(barra);
            }
        }

        card.addEventListener("click", function () {
            abrirItem(item, tipo);
        });

        card.addEventListener("keydown", function (event) {
            tratarTeclaCard(event, item, tipo);
        });

        return card;
    }

    function tratarTeclaCard(event, item, tipo) {
        var tecla = event.key;

        if (tecla === "Enter" ||
            tecla === "OK" ||
            tecla === " ") {

            event.preventDefault();
            abrirItem(item, tipo);
            return;
        }

        if (
            tecla === "f" ||
            tecla === "F"
        ) {
            event.preventDefault();
            alternarFavorito(item);
            return;
        }
    }

    function criarSecao(titulo, lista, tipo) {
        if (!lista || lista.length === 0) {
            return;
        }

        var secao = document.createElement("section");

        secao.className = "contentSection";

        var tituloSecao =
            document.createElement("h2");

        tituloSecao.className = "sectionTitle";
        tituloSecao.textContent = titulo;

        secao.appendChild(tituloSecao);

        var grid =
            document.createElement("div");

        grid.className = "cardsGrid";

        lista.forEach(function (item) {
            grid.appendChild(
                criarCard(item, tipo)
            );
        });

        secao.appendChild(grid);
        content.appendChild(secao);
    }

    function limparConteudo() {
        content.innerHTML = "";
    }

    function mostrarCarregando(texto) {
        limparConteudo();

        var loading =
            document.createElement("div");

        loading.className = "loading";

        loading.innerHTML =
            '<div class="loadingWolf">WOLF</div>' +
            '<div>' +
            escaparHTML(
                texto || "Carregando catálogo..."
            ) +
            '</div>';

        content.appendChild(loading);

        elementosFocaveis = [];
        indiceFoco = 0;
    }

    function mostrarErro(texto) {
        limparConteudo();

        var erro =
            document.createElement("div");

        erro.className = "emptyState";

        erro.innerHTML =
            "<h2>WOLF IPTV</h2>" +
            "<p>" +
            escaparHTML(texto) +
            "</p>" +
            '<button class="focusable retryButton" tabindex="0">' +
            "TENTAR NOVAMENTE" +
            "</button>";

        content.appendChild(erro);

        var botao =
            erro.querySelector(".retryButton");

        botao.addEventListener("click", function () {
            carregarCatalogos();
        });

        atualizarFocosConteudo();
    }

    function mostrarVazio(titulo, texto) {
        limparConteudo();

        var vazio =
            document.createElement("div");

        vazio.className = "emptyState";

        vazio.innerHTML =
            "<h2>" +
            escaparHTML(titulo) +
            "</h2>" +
            "<p>" +
            escaparHTML(
                texto ||
                "Nenhum conteúdo encontrado."
            ) +
            "</p>";

        content.appendChild(vazio);

        elementosFocaveis = [];
        indiceFoco = 0;
    }

    function atualizarFocosConteudo() {
        elementosFocaveis =
            Array.prototype.slice.call(
                content.querySelectorAll(
                    ".focusable"
                )
            );

        indiceFoco = 0;

        if (elementosFocaveis.length > 0) {
            aplicarFoco(0);
        }
    }

    function aplicarFoco(indice) {
        if (
            !elementosFocaveis ||
            elementosFocaveis.length === 0
        ) {
            return;
        }

        if (indice < 0) {
            indice = 0;
        }

        if (
            indice >= elementosFocaveis.length
        ) {
            indice =
                elementosFocaveis.length - 1;
        }

        elementosFocaveis.forEach(
            function (elemento) {
                elemento.classList.remove(
                    "focused"
                );
            }
        );

        indiceFoco = indice;

        var elemento =
            elementosFocaveis[indiceFoco];

        elemento.classList.add("focused");

        try {
            elemento.focus({
                preventScroll: true
            });
        } catch (erro) {
            elemento.focus();
        }

        manterFocoVisivel(elemento);
    }

    function manterFocoVisivel(elemento) {
        if (!elemento) {
            return;
        }

        try {
            elemento.scrollIntoView({
                behavior: "smooth",
                block: "center",
                inline: "nearest"
            });
        } catch (erro) {
            elemento.scrollIntoView();
        }
    }

    function aplicarFocoMenuButton() {
        elementosFocaveis = [menuButton];
        indiceFoco = 0;

        menuButton.classList.add("focused");

        try {
            menuButton.focus({
                preventScroll: true
            });
        } catch (erro) {
            menuButton.focus();
        }
    }

    function focoEstaNoMenuButton() {
        return (
            elementosFocaveis.length === 1 &&
            elementosFocaveis[0] === menuButton
        );
    }

    function moverFoco(direcao) {
        if (menuAberto) {
            moverFocoMenu(direcao);
            return;
        }

        if (focoEstaNoMenuButton()) {
            if (direcao === "down") {
                atualizarFocosConteudo();

                if (elementosFocaveis.length > 0) {
                    aplicarFoco(0);
                }

                return;
            }

            return;
        }

        if (elementosFocaveis.length === 0) {
            return;
        }

        var atual = indiceFoco;
        var novo = atual;

        if (direcao === "left") {
            novo = atual - 1;
        }

        if (direcao === "right") {
            novo = atual + 1;
        }

        if (direcao === "up") {
            novo = atual - 5;

            if (novo < 0) {
                aplicarFocoMenuButton();
                return;
            }
        }

        if (direcao === "down") {
            novo = atual + 5;
        }

        if (novo < 0) {
            novo = 0;
        }

        if (
            novo >= elementosFocaveis.length
        ) {
            novo =
                elementosFocaveis.length - 1;
        }

        aplicarFoco(novo);
    }

    function abrirMenu() {
        if (menuAberto) {
            return;
        }

        menuAberto = true;

        sideMenu.classList.add("open");
        sideMenu.setAttribute(
            "aria-hidden",
            "false"
        );

        renderizarMenu();
    }

    function fecharMenu() {
        if (!menuAberto) {
            return;
        }

        menuAberto = false;

        sideMenu.classList.remove("open");
        sideMenu.setAttribute(
            "aria-hidden",
            "true"
        );

        aplicarFocoMenuButton();
    }

    function renderizarMenu() {
        menuContent.innerHTML = "";

        adicionarItemMenu(
            "⌂  Início",
            function () {
                fecharMenu();
                abrirInicio();
            }
        );

        adicionarTituloMenu("FILMES");

        [
            "Ação",
            "Aventura",
            "Animação",
            "Comédia",
            "Drama",
            "Terror",
            "Ficção"
        ].forEach(function (categoria) {
            adicionarItemMenu(
                categoria,
                function () {
                    fecharMenu();
                    abrirFilmesCategoria(
                        categoria
                    );
                }
            );
        });

        adicionarTituloMenu("SÉRIES");

        [
            "Ação",
            "Aventura",
            "Comédia",
            "Drama",
            "Terror"
        ].forEach(function (categoria) {
            adicionarItemMenu(
                categoria,
                function () {
                    fecharMenu();
                    abrirSeriesCategoria(
                        categoria
                    );
                }
            );
        });

        adicionarTituloMenu("DORAMAS");

        [
            "Romance",
            "Ação",
            "Comédia",
            "Terror"
        ].forEach(function (categoria) {
            adicionarItemMenu(
                categoria,
                function () {
                    fecharMenu();
                    abrirDoramasCategoria(
                        categoria
                    );
                }
            );
        });

        adicionarTituloMenu("DESENHOS");

        [
            "Ação",
            "Aventura",
            "Animação",
            "Comédia",
            "Drama",
            "Terror",
            "Fantasia"
        ].forEach(function (categoria) {
            adicionarItemMenu(
                categoria,
                function () {
                    fecharMenu();
                    abrirDesenhosCategoria(
                        categoria
                    );
                }
            );
        });

        adicionarTituloMenu("ANIME");

        [
            "Ação",
            "Comédia",
            "Terror"
        ].forEach(function (categoria) {
            adicionarItemMenu(
                categoria,
                function () {
                    fecharMenu();
                    abrirAnimesCategoria(
                        categoria
                    );
                }
            );
        });

        adicionarTituloMenu("WOLF");

        adicionarItemMenu(
            "▶  Continuar assistindo",
            function () {
                fecharMenu();
                renderizarContinuar();
            }
        );

        adicionarItemMenu(
            "♥  Favoritos",
            function () {
                fecharMenu();
                renderizarFavoritos();
            }
        );

        adicionarItemMenu(
            "⌕  Pesquisa",
            function () {
                fecharMenu();
                abrirPesquisa();
            }
        );

        adicionarItemMenu(
            "↻  Atualizar catálogo",
            function () {
                fecharMenu();
                carregarCatalogos();
            }
        );

        elementosFocaveis =
            Array.prototype.slice.call(
                menuContent.querySelectorAll(
                    ".menuItem.focusable"
                )
            );

        indiceFoco = 0;

        if (elementosFocaveis.length > 0) {
            aplicarFoco(0);
        }
    }

    function adicionarTituloMenu(texto) {
        var titulo =
            document.createElement("div");

        titulo.className = "menuSectionTitle";
        titulo.textContent = texto;

        menuContent.appendChild(titulo);
    }

    function adicionarItemMenu(texto, acao) {
        var item =
            document.createElement("button");

        item.type = "button";
        item.className =
            "menuItem focusable";

        item.textContent = texto;

        item.addEventListener(
            "click",
            acao
        );

        item.addEventListener(
            "keydown",
            function (event) {
                if (
                    event.key === "Enter" ||
                    event.key === "OK" ||
                    event.key === " "
                ) {
                    event.preventDefault();
                    acao();
                }
            }
        );

        menuContent.appendChild(item);
    }

    function moverFocoMenu(direcao) {
        if (elementosFocaveis.length === 0) {
            return;
        }

        var novo = indiceFoco;

        if (direcao === "up") {
            novo--;
        }

        if (direcao === "down") {
            novo++;
        }

        if (direcao === "left") {
            fecharMenu();
            return;
        }

        if (direcao === "right") {
            return;
        }

        if (novo < 0) {
            novo = 0;
        }

        if (
            novo >= elementosFocaveis.length
        ) {
            novo =
                elementosFocaveis.length - 1;
        }

        aplicarFoco(novo);
    }

    function abrirInicio() {
        tipoAtual = "inicio";
        categoriaAtual = "";
        serieAtual = null;
        temporadasAtual = [];

        renderizarHome();
    }

    function renderizarHome() {
        limparConteudo();

        if (
            continuarAssistindo &&
            continuarAssistindo.length > 0
        ) {
            criarSecao(
                "Continuar assistindo",
                continuarAssistindo.slice(0, 10),
                "continue"
            );
        }

        if (filmes.length > 0) {
            criarSecao(
                "Filmes",
                filmes.slice(0, 20),
                "filme"
            );
        }

        if (series.length > 0) {
            criarSecao(
                "Séries",
                series.slice(0, 20),
                "serie"
            );
        }

        if (doramas.length > 0) {
            criarSecao(
                "Doramas",
                doramas.slice(0, 20),
                "dorama"
            );
        }

        if (animes.length > 0) {
            criarSecao(
                "Anime",
                animes.slice(0, 20),
                "anime"
            );
        }

        if (desenhos.length > 0) {
            criarSecao(
                "Desenhos",
                desenhos.slice(0, 20),
                "desenho"
            );
        }

        if (content.children.length === 0) {
            mostrarVazio(
                "WOLF IPTV",
                "Nenhum conteúdo disponível."
            );
            return;
        }

        atualizarFocosConteudo();
    }

    function filtrarCategoria(lista, categoria) {
        var categoriaNormalizada =
            normalizarTexto(categoria);

        return lista.filter(
            function (item) {
                return normalizarTexto(
                    obterCategoria(item)
                ) === categoriaNormalizada;
            }
        );
    }

    function abrirFilmesCategoria(categoria) {
        tipoAtual = "filmes";
        categoriaAtual = categoria;

        var lista =
            filtrarCategoria(
                filmes,
                categoria
            );

        lista.sort(ordenarFilmes);

        limparConteudo();

        if (lista.length === 0) {
            mostrarVazio(
                categoria,
                "Nenhum filme encontrado nesta categoria."
            );
            return;
        }

        criarSecao(
            "Filmes • " + categoria,
            lista,
            "filme"
        );

        atualizarFocosConteudo();
    }

    function abrirSeriesCategoria(categoria) {
        tipoAtual = "series";
        categoriaAtual = categoria;

        var lista =
            filtrarCategoria(
                series,
                categoria
            );

        lista.sort(ordenarTitulo);

        limparConteudo();

        if (lista.length === 0) {
            mostrarVazio(
                categoria,
                "Nenhuma série encontrada nesta categoria."
            );
            return;
        }

        criarSecao(
            "Séries • " + categoria,
            lista,
            "serie"
        );

        atualizarFocosConteudo();
    }

    function abrirDoramasCategoria(categoria) {
        tipoAtual = "doramas";
        categoriaAtual = categoria;

        var lista =
            filtrarCategoria(
                doramas,
                categoria
            );

        lista.sort(ordenarTitulo);

        limparConteudo();

        if (lista.length === 0) {
            mostrarVazio(
                categoria,
                "Nenhum dorama encontrado nesta categoria."
            );
            return;
        }

        criarSecao(
            "Doramas • " + categoria,
            lista,
            "dorama"
        );

        atualizarFocosConteudo();
    }

    function abrirAnimesCategoria(categoria) {
        tipoAtual = "animes";
        categoriaAtual = categoria;

        var lista =
            filtrarCategoria(
                animes,
                categoria
            );

        lista.sort(ordenarTitulo);

        limparConteudo();

        if (lista.length === 0) {
            mostrarVazio(
                categoria,
                "Nenhum anime encontrado nesta categoria."
            );
            return;
        }

        criarSecao(
            "Anime • " + categoria,
            lista,
            "anime"
        );

        atualizarFocosConteudo();
    }

    function abrirDesenhosCategoria(categoria) {
        tipoAtual = "desenhos";
        categoriaAtual = categoria;

        var lista =
            filtrarCategoria(
                desenhos,
                categoria
            );

        lista.sort(ordenarFilmes);

        limparConteudo();

        if (lista.length === 0) {
            mostrarVazio(
                categoria,
                "Nenhum desenho encontrado nesta categoria."
            );
            return;
        }

        criarSecao(
            "Desenhos • " + categoria,
            lista,
            "desenho"
        );

        atualizarFocosConteudo();
    }

    function renderizarContinuar() {
        tipoAtual = "continuar";
        categoriaAtual = "";

        limparConteudo();

        if (
            !continuarAssistindo ||
            continuarAssistindo.length === 0
        ) {
            mostrarVazio(
                "Continuar assistindo",
                "Você ainda não começou nenhum conteúdo."
            );
            return;
        }

        criarSecao(
            "Continuar assistindo",
            continuarAssistindo,
            "continue"
        );

        atualizarFocosConteudo();
    }

    function renderizarFavoritos() {
        tipoAtual = "favoritos";
        categoriaAtual = "";

        limparConteudo();

        if (
            !favoritos ||
            favoritos.length === 0
        ) {
            mostrarVazio(
                "Favoritos",
                "Você ainda não adicionou favoritos."
            );
            return;
        }

        criarSecao(
            "Favoritos",
            favoritos,
            "continue"
        );

        atualizarFocosConteudo();
    }

    function abrirItem(item, tipo) {
        if (!item) {
            return;
        }

        if (tipo === "filme" ||
            tipo === "desenho") {

            abrirVideo(
                item,
                obterTitulo(item)
            );

            return;
        }

        if (tipo === "continue") {
            if (item._tipo === "episodio") {
                abrirVideo(
                    item,
                    obterTitulo(item)
                );
            } else {
                abrirVideo(
                    item,
                    obterTitulo(item)
                );
            }

            return;
        }

        if (
            tipo === "serie" ||
            tipo === "dorama" ||
            tipo === "anime"
        ) {
            abrirTemporadas(item, tipo);
            return;
        }

        if (tipo === "temporada") {
            abrirEpisodios(
                item._serie,
                item,
                item._tipoSerie
            );
            return;
        }

        if (tipo === "episodio") {
            abrirVideo(
                item,
                obterTitulo(item)
            );
        }
    }

    function abrirTemporadas(serie, tipo) {
        if (!serie) {
            return;
        }

        historicoConteudo.push({
            tipo: tipoAtual,
            categoria: categoriaAtual
        });

        serieAtual = serie;
        temporadasAtual =
            Array.isArray(serie.temporadas)
                ? serie.temporadas
                : [];

        tipoAtual = "temporadas";
        categoriaAtual = "";

        limparConteudo();

        if (temporadasAtual.length === 0) {
            mostrarVazio(
                obterTitulo(serie),
                "Nenhuma temporada encontrada."
            );
            return;
        }

        var titulo =
            document.createElement("h1");

        titulo.className = "pageHeading";
        titulo.textContent =
            obterTitulo(serie);

        content.appendChild(titulo);

        var grid =
            document.createElement("div");

        grid.className = "cardsGrid";

        temporadasAtual.forEach(
            function (temporada) {
                var item = {
                    _tipo: "temporada",
                    numero: temporada.numero,
                    titulo:
                        "Temporada " +
                        String(temporada.numero),
                    capa:
                        obterImagem(serie),
                    _serie: serie,
                    _tipoSerie: tipo
                };

                grid.appendChild(
                    criarCard(
                        item,
                        "temporada"
                    )
                );
            }
        );

        content.appendChild(grid);

        atualizarFocosConteudo();
    }

    function abrirEpisodios(
        serie,
        temporada,
        tipoSerie
    ) {
        if (!serie || !temporada) {
            return;
        }

        historicoConteudo.push({
            tipo: "temporadas",
            serie: serie,
            tipoSerie: tipoSerie
        });

        serieAtual = serie;

        tipoAtual = "episodios";

        limparConteudo();

        var titulo =
            document.createElement("h1");

        titulo.className = "pageHeading";

        titulo.textContent =
            obterTitulo(serie) +
            " • Temporada " +
            String(temporada.numero);

        content.appendChild(titulo);

        var episodios =
            Array.isArray(temporada.episodios)
                ? temporada.episodios
                : [];

        if (episodios.length === 0) {
            mostrarVazio(
                "Temporada " +
                String(temporada.numero),
                "Nenhum episódio encontrado."
            );
            return;
        }

        var grid =
            document.createElement("div");

        grid.className = "cardsGrid";

        episodios.forEach(
            function (episodio) {
                var item = {
                    _tipo: "episodio",
                    numero: episodio.numero,
                    titulo:
                        episodio.titulo ||
                        ("Episódio " +
                        String(episodio.numero)),
                    video: episodio.video,
                    capa:
                        obterImagem(serie),
                    _capa:
                        obterImagem(serie),
                    _serieTitulo:
                        obterTitulo(serie),
                    _serieCategoria:
                        obterCategoria(serie),
                    _temporadaNumero:
                        temporada.numero
                };

                grid.appendChild(
                    criarCard(
                        item,
                        "episodio"
                    )
                );
            }
        );

        content.appendChild(grid);

        atualizarFocosConteudo();
    }

    function localizarItemPorId(id) {
        var listas = [
            filmes,
            series,
            doramas,
            animes,
            desenhos,
            favoritos,
            continuarAssistindo
        ];

        for (var i = 0; i < listas.length; i++) {
            var lista = listas[i];

            for (var j = 0; j < lista.length; j++) {
                var item = lista[j];

                if (gerarId(item) === id) {
                    return item;
                }
            }
        }

        var grupos = [
            series,
            doramas,
            animes
        ];

        for (var g = 0; g < grupos.length; g++) {
            var grupo = grupos[g];

            for (var s = 0; s < grupo.length; s++) {
                var serie = grupo[s];

                if (
                    !serie.temporadas ||
                    !Array.isArray(
                        serie.temporadas
                    )
                ) {
                    continue;
                }

                for (
                    var t = 0;
                    t < serie.temporadas.length;
                    t++
                ) {
                    var temporada =
                        serie.temporadas[t];

                    if (
                        !temporada.episodios ||
                        !Array.isArray(
                            temporada.episodios
                        )
                    ) {
                        continue;
                    }

                    for (
                        var e = 0;
                        e < temporada.episodios.length;
                        e++
                    ) {
                        var episodio =
                            temporada.episodios[e];

                        var itemEpisodio = {
                            _tipo: "episodio",
                            numero:
                                episodio.numero,
                            titulo:
                                episodio.titulo ||
                                "Episódio " +
                                String(
                                    episodio.numero
                                ),
                            video:
                                episodio.video,
                            capa:
                                obterImagem(serie),
                            _capa:
                                obterImagem(serie),
                            _serieTitulo:
                                obterTitulo(serie),
                            _serieCategoria:
                                obterCategoria(serie),
                            _temporadaNumero:
                                temporada.numero
                        };

                        if (
                            gerarId(
                                itemEpisodio
                            ) === id
                        ) {
                            return itemEpisodio;
                        }
                    }
                }
            }
        }

        return null;
    }

    function abrirVideo(item, titulo) {
        var video = obterVideo(item);

        if (!video) {
            mostrarToast(
                "Vídeo não disponível."
            );
            return;
        }

        playerAberto = true;

        playerOverlay.classList.remove(
            "hidden"
        );

        playerTitle.textContent =
            titulo || "WOLF IPTV";

        playerLoading.classList.add(
            "show"
        );

        playerError.classList.remove(
            "show"
        );

        videoPlayer.pause();

        videoPlayer.removeAttribute(
            "src"
        );

        videoPlayer.load();

        videoPlayer.src = video;

        var registroAnterior = null;

        for (
            var i = 0;
            i < continuarAssistindo.length;
            i++
        ) {
            if (
                continuarAssistindo[i]._id ===
                gerarId(item)
            ) {
                registroAnterior =
                    continuarAssistindo[i];
                break;
            }
        }

        videoPlayer.onloadedmetadata =
            function () {
                playerLoading.classList.remove(
                    "show"
                );

                if (
                    registroAnterior &&
                    registroAnterior._progresso > 5 &&
                    registroAnterior._progresso <
                    videoPlayer.duration - 10
                ) {
                    try {
                        videoPlayer.currentTime =
                            registroAnterior._progresso;
                    } catch (erro) {
                    }
                }

                var promessa =
                    videoPlayer.play();

                if (
                    promessa &&
                    typeof promessa.catch ===
                    "function"
                ) {
                    promessa.catch(
                        function () {
                        }
                    );
                }
            };

        videoPlayer.onerror =
            function () {
                playerLoading.classList.remove(
                    "show"
                );

                playerError.classList.add(
                    "show"
                );
            };

        videoPlayer.ontimeupdate =
            function () {
                if (
                    videoPlayer.duration &&
                    videoPlayer.currentTime > 5
                ) {
                    registrarContinueAssistindo(
                        item,
                        videoPlayer.currentTime,
                        videoPlayer.duration
                    );
                }
            };

        videoPlayer.onended =
            function () {
                removerContinue(item);
            };

        playerOverlay.focus();
    }

    function fecharPlayer() {
        if (!playerAberto) {
            return;
        }

        playerAberto = false;

        try {
            videoPlayer.pause();
        } catch (erro) {
        }

        videoPlayer.removeAttribute(
            "src"
        );

        videoPlayer.load();

        playerOverlay.classList.add(
            "hidden"
        );

        if (tipoAtual === "inicio") {
            atualizarFocosConteudo();
        } else if (
            elementosFocaveis.length > 0
        ) {
            aplicarFoco(indiceFoco);
        } else {
            aplicarFocoMenuButton();
        }
    }

    function abrirPesquisa() {
        pesquisaAberta = true;

        searchOverlay.classList.remove(
            "hidden"
        );

        searchInput.value = "";

        window.setTimeout(
            function () {
                searchInput.focus();
            },
            100
        );
    }

    function fecharPesquisa() {
        pesquisaAberta = false;

        searchOverlay.classList.add(
            "hidden"
        );

        aplicarFocoMenuButton();
    }

    function executarPesquisa(texto) {
        var termo =
            normalizarTexto(texto);

        if (!termo) {
            return;
        }

        fecharPesquisa();

        tipoAtual = "pesquisa";
        categoriaAtual = "";

        var resultados = [];

        filmes.forEach(
            function (item) {
                if (
                    normalizarTexto(
                        obterTitulo(item)
                    ).indexOf(termo) >= 0
                ) {
                    resultados.push({
                        item: item,
                        tipo: "filme"
                    });
                }
            }
        );

        series.forEach(
            function (item) {
                if (
                    normalizarTexto(
                        obterTitulo(item)
                    ).indexOf(termo) >= 0
                ) {
                    resultados.push({
                        item: item,
                        tipo: "serie"
                    });
                }
            }
        );

        doramas.forEach(
            function (item) {
                if (
                    normalizarTexto(
                        obterTitulo(item)
                    ).indexOf(termo) >= 0
                ) {
                    resultados.push({
                        item: item,
                        tipo: "dorama"
                    });
                }
            }
        );

        animes.forEach(
            function (item) {
                if (
                    normalizarTexto(
                        obterTitulo(item)
                    ).indexOf(termo) >= 0
                ) {
                    resultados.push({
                        item: item,
                        tipo: "anime"
                    });
                }
            }
        );

        desenhos.forEach(
            function (item) {
                if (
                    normalizarTexto(
                        obterTitulo(item)
                    ).indexOf(termo) >= 0
                ) {
                    resultados.push({
                        item: item,
                        tipo: "desenho"
                    });
                }
            }
        );

        limparConteudo();

        if (resultados.length === 0) {
            mostrarVazio(
                "Pesquisa",
                "Nenhum resultado para \"" +
                texto +
                "\"."
            );
            return;
        }

        var titulo =
            document.createElement("h1");

        titulo.className = "pageHeading";
        titulo.textContent =
            "Resultados: " + texto;

        content.appendChild(titulo);

        var grid =
            document.createElement("div");

        grid.className = "cardsGrid";

        resultados.forEach(
            function (resultado) {
                grid.appendChild(
                    criarCard(
                        resultado.item,
                        resultado.tipo
                    )
                );
            }
        );

        content.appendChild(grid);

        atualizarFocosConteudo();
    }

    function extrairLista(dados, tipo) {
        if (!dados) {
            return [];
        }

        if (Array.isArray(dados)) {
            return dados;
        }

        if (tipo === "filmes" &&
            Array.isArray(dados.filmes)) {
            return dados.filmes;
        }

        if (tipo === "series" &&
            Array.isArray(dados.series)) {
            return dados.series;
        }

        if (tipo === "doramas" &&
            Array.isArray(dados.doramas)) {
            return dados.doramas;
        }

        if (tipo === "animes" &&
            Array.isArray(dados.animes)) {
            return dados.animes;
        }

        if (tipo === "desenhos" &&
            Array.isArray(dados.desenhos)) {
            return dados.desenhos;
        }

        if (tipo === "desenhos" &&
            Array.isArray(dados.desenho)) {
            return dados.desenho;
        }

        return [];
    }

    function buscarJson(url) {
        return fetch(
            url,
            {
                method: "GET",
                cache: "no-store"
            }
        ).then(
            function (resposta) {
                if (!resposta.ok) {
                    throw new Error(
                        "HTTP " +
                        resposta.status
                    );
                }

                return resposta.text();
            }
        ).then(
            function (texto) {
                if (
                    !texto ||
                    !texto.trim()
                ) {
                    throw new Error(
                        "JSON vazio"
                    );
                }

                return JSON.parse(texto);
            }
        );
    }

    function carregarPrimeiroJson(
        urls,
        tipo
    ) {
        var indice = 0;

        function tentar() {
            if (indice >= urls.length) {
                return Promise.resolve([]);
            }

            var url = urls[indice];

            indice++;

            return buscarJson(url)
                .then(
                    function (dados) {
                        return extrairLista(
                            dados,
                            tipo
                        );
                    }
                )
                .catch(
                    function () {
                        return tentar();
                    }
                );
        }

        return tentar();
    }

    function prepararSeries(lista) {
        if (!Array.isArray(lista)) {
            return [];
        }

        return lista.map(
            function (serie) {
                var nova = copiarItem(serie);

                if (
                    !Array.isArray(
                        nova.temporadas
                    )
                ) {
                    nova.temporadas = [];
                }

                nova.temporadas =
                    nova.temporadas.map(
                        function (temporada) {
                            var temp =
                                copiarItem(
                                    temporada
                                );

                            if (
                                !Array.isArray(
                                    temp.episodios
                                )
                            ) {
                                temp.episodios = [];
                            }

                            temp.episodios =
                                temp.episodios.map(
                                    function (
                                        episodio
                                    ) {
                                        var ep =
                                            copiarItem(
                                                episodio
                                            );

                                        return ep;
                                    }
                                );

                            temp.episodios.sort(
                                function (a, b) {
                                    return (
                                        Number(
                                            a.numero
                                        ) -
                                        Number(
                                            b.numero
                                        )
                                    );
                                }
                            );

                            return temp;
                        }
                    );

                nova.temporadas.sort(
                    function (a, b) {
                        return (
                            Number(
                                a.numero
                            ) -
                            Number(
                                b.numero
                            )
                        );
                    }
                );

                return nova;
            }
        );
    }

    function contarEpisodiosComVideo(lista) {
        var total = 0;

        if (!Array.isArray(lista)) {
            return 0;
        }

        lista.forEach(
            function (serie) {
                if (
                    !Array.isArray(
                        serie.temporadas
                    )
                ) {
                    return;
                }

                serie.temporadas.forEach(
                    function (temporada) {
                        if (
                            !Array.isArray(
                                temporada.episodios
                            )
                        ) {
                            return;
                        }

                        temporada.episodios.forEach(
                            function (episodio) {
                                if (
                                    episodio.video
                                ) {
                                    total++;
                                }
                            }
                        );
                    }
                );
            }
        );

        return total;
    }

    function carregarCatalogos() {
        catalogosCarregados = false;

        mostrarCarregando(
            "Carregando catálogo..."
        );

        Promise.allSettled([
            buscarJson(CATALOGO_URL),
            carregarPrimeiroJson(
                SERIES_URLS,
                "series"
            ),
            carregarPrimeiroJson(
                DORAMAS_URLS,
                "doramas"
            ),
            carregarPrimeiroJson(
                ANIMES_URLS,
                "animes"
            ),
            carregarPrimeiroJson(
                DESENHOS_URLS,
                "desenhos"
            )
        ]).then(
            function (resultados) {
                var catalogo =
                    resultados[0];

                var resultadoSeries =
                    resultados[1];

                var resultadoDoramas =
                    resultados[2];

                var resultadoAnimes =
                    resultados[3];

                var resultadoDesenhos =
                    resultados[4];

                filmes = [];
                series = [];
                doramas = [];
                animes = [];
                desenhos = [];

                if (
                    catalogo.status ===
                    "fulfilled"
                ) {
                    filmes =
                        extrairLista(
                            catalogo.value,
                            "filmes"
                        );

                    if (
                        series.length === 0 &&
                        Array.isArray(
                            catalogo.value.series
                        )
                    ) {
                        series =
                            catalogo.value.series;
                    }

                    if (
                        doramas.length === 0 &&
                        Array.isArray(
                            catalogo.value.doramas
                        )
                    ) {
                        doramas =
                            catalogo.value.doramas;
                    }

                    if (
                        animes.length === 0 &&
                        Array.isArray(
                            catalogo.value.animes
                        )
                    ) {
                        animes =
                            catalogo.value.animes;
                    }

                    if (
                        desenhos.length === 0 &&
                        Array.isArray(
                            catalogo.value.desenhos
                        )
                    ) {
                        desenhos =
                            catalogo.value.desenhos;
                    }
                }

                if (
                    resultadoSeries.status ===
                    "fulfilled" &&
                    resultadoSeries.value.length > 0
                ) {
                    series =
                        resultadoSeries.value;
                }

                if (
                    resultadoDoramas.status ===
                    "fulfilled" &&
                    resultadoDoramas.value.length > 0
                ) {
                    doramas =
                        resultadoDoramas.value;
                }

                if (
                    resultadoAnimes.status ===
                    "fulfilled" &&
                    resultadoAnimes.value.length > 0
                ) {
                    animes =
                        resultadoAnimes.value;
                }

                if (
                    resultadoDesenhos.status ===
                    "fulfilled" &&
                    resultadoDesenhos.value.length > 0
                ) {
                    desenhos =
                        resultadoDesenhos.value;
                }

                filmes =
                    Array.isArray(filmes)
                        ? filmes
                        : [];

                series =
                    prepararSeries(series);

                doramas =
                    prepararSeries(doramas);

                animes =
                    prepararSeries(animes);

                desenhos =
                    Array.isArray(desenhos)
                        ? desenhos
                        : [];

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

                catalogosCarregados = true;

                console.log(
                    "WOLF IPTV carregado"
                );

                console.log(
                    "Filmes:",
                    filmes.length
                );

                console.log(
                    "Séries:",
                    series.length
                );

                console.log(
                    "Doramas:",
                    doramas.length
                );

                console.log(
                    "Anime:",
                    animes.length
                );

                console.log(
                    "Desenhos:",
                    desenhos.length
                );

                console.log(
                    "Episódios com vídeo:",
                    contarEpisodiosComVideo(
                        series
                    )
                );

                abrirInicio();
            }
        ).catch(
            function (erro) {
                console.log(
                    "Erro carregando catálogo:",
                    erro
                );

                mostrarErro(
                    "Não foi possível carregar o catálogo."
                );
            }
        );
    }

    function voltar() {
        if (playerAberto) {
            fecharPlayer();
            return;
        }

        if (pesquisaAberta) {
            fecharPesquisa();
            return;
        }

        if (menuAberto) {
            fecharMenu();
            return;
        }

        if (
            historicoConteudo &&
            historicoConteudo.length > 0
        ) {
            var anterior =
                historicoConteudo.pop();

            if (anterior.tipo === "temporadas") {
                if (anterior.serie) {
                    abrirTemporadas(
                        anterior.serie,
                        anterior.tipoSerie ||
                        "serie"
                    );
                } else {
                    abrirInicio();
                }

                return;
            }

            tipoAtual =
                anterior.tipo || "inicio";

            categoriaAtual =
                anterior.categoria || "";

            if (tipoAtual === "inicio") {
                abrirInicio();
                return;
            }

            if (tipoAtual === "filmes") {
                abrirFilmesCategoria(
                    categoriaAtual
                );
                return;
            }

            if (tipoAtual === "series") {
                abrirSeriesCategoria(
                    categoriaAtual
                );
                return;
            }

            if (tipoAtual === "doramas") {
                abrirDoramasCategoria(
                    categoriaAtual
                );
                return;
            }

            if (tipoAtual === "animes") {
                abrirAnimesCategoria(
                    categoriaAtual
                );
                return;
            }

            if (tipoAtual === "desenhos") {
                abrirDesenhosCategoria(
                    categoriaAtual
                );
                return;
            }
        }

        abrirInicio();
    }

    function tratarTeclaGlobal(event) {
        var tecla = event.key;

        if (
            tecla === "ArrowLeft" ||
            tecla === "Left"
        ) {
            if (!menuAberto) {
                return;
            }

            event.preventDefault();
            moverFoco("left");
            return;
        }

        if (
            tecla === "ArrowRight" ||
            tecla === "Right"
        ) {
            event.preventDefault();
            moverFoco("right");
            return;
        }

        if (
            tecla === "ArrowUp" ||
            tecla === "Up"
        ) {
            event.preventDefault();
            moverFoco("up");
            return;
        }

        if (
            tecla === "ArrowDown" ||
            tecla === "Down"
        ) {
            event.preventDefault();
            moverFoco("down");
            return;
        }

        if (
            tecla === "Escape" ||
            tecla === "Backspace"
        ) {
            event.preventDefault();
            voltar();
            return;
        }

        if (
            tecla === "MediaPlayPause"
        ) {
            if (
                playerAberto &&
                videoPlayer
            ) {
                if (videoPlayer.paused) {
                    videoPlayer.play();
                } else {
                    videoPlayer.pause();
                }
            }
        }

        if (
            tecla === "MediaPlay"
        ) {
            if (
                playerAberto &&
                videoPlayer
            ) {
                videoPlayer.play();
            }
        }

        if (
            tecla === "MediaPause"
        ) {
            if (
                playerAberto &&
                videoPlayer
            ) {
                videoPlayer.pause();
            }
        }
    }

    function configurarEventos() {
        menuButton.addEventListener(
            "click",
            function () {
                abrirMenu();
            }
        );

        menuButton.addEventListener(
            "keydown",
            function (event) {
                if (
                    event.key === "Enter" ||
                    event.key === "OK" ||
                    event.key === " "
                ) {
                    event.preventDefault();
                    abrirMenu();
                }
            }
        );

        closeMenu.addEventListener(
            "click",
            function () {
                fecharMenu();
            }
        );

        searchInput.addEventListener(
            "keydown",
            function (event) {
                if (
                    event.key === "Enter" ||
                    event.key === "OK"
                ) {
                    event.preventDefault();

                    executarPesquisa(
                        searchInput.value
                    );
                }

                if (
                    event.key === "Escape" ||
                    event.key === "Backspace"
                ) {
                    if (
                        searchInput.value === ""
                    ) {
                        event.preventDefault();
                        fecharPesquisa();
                    }
                }
            }
        );

        qs("closePlayer").addEventListener(
            "click",
            function () {
                fecharPlayer();
            }
        );

        playerOverlay.addEventListener(
            "click",
            function (event) {
                if (
                    event.target ===
                    playerOverlay
                ) {
                    fecharPlayer();
                }
            }
        );

        document.addEventListener(
            "keydown",
            tratarTeclaGlobal
        );

        window.addEventListener(
            "resize",
            function () {
                if (
                    elementosFocaveis.length > 0 &&
                    indiceFoco <
                    elementosFocaveis.length
                ) {
                    manterFocoVisivel(
                        elementosFocaveis[
                            indiceFoco
                        ]
                    );
                }
            }
        );
    }

    function iniciar() {
        iniciarElementos();
        carregarLocalStorage();
        configurarEventos();

        mostrarCarregando(
            "Carregando catálogo..."
        );

        carregarCatalogos();
    }

    if (
        document.readyState ===
        "loading"
    ) {
        document.addEventListener(
            "DOMContentLoaded",
            iniciar
        );
    } else {
        iniciar();
    }
})();
