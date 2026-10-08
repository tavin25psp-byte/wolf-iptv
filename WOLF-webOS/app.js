(function () {
  'use strict';

  var BASE = 'https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/';

  var FONTES = {
    filmes:   { titulo: 'Filmes',   tipo: 'filme', arquivos: ['catalogo.json'], chaves: ['filmes'] },
    series:   { titulo: 'Séries',   tipo: 'serie', arquivos: ['series.json', 'serie.json', 's%C3%A9rie.json'], chaves: ['series'] },
    doramas:  { titulo: 'Doramas',  tipo: 'serie', arquivos: ['doramas.json', 'Doramas.json'], chaves: ['doramas'] },
    animes:   { titulo: 'Animes',   tipo: 'serie', arquivos: ['animes.json', 'anime.json', 'Animes.json', 'Anime.json'], chaves: ['animes'] },
    desenhos: { titulo: 'Desenhos', tipo: 'filme', arquivos: ['desenho.json', 'desenhos.json', 'Desenho.json', 'Desenhos.json'], chaves: ['desenhos', 'desenho'] }
  };
  var ORDEM = ['filmes', 'series', 'doramas', 'animes', 'desenhos'];
  var LOTE = 60;

  var dados = {};
  var raiz = {};
  var historico = [];
  var estado = { secao: 'filmes', categoria: 'Todos', zona: 'tabs', idx: 0, modo: 'catalogo', cards: [], els: [], mostrados: 0 };
  var tabsEls = [];
  var chipsEls = [];
  var playerAberto = false;
  var timerBarra = null;
  var timerAviso = null;

  function $(id) { return document.getElementById(id); }

  function norm(s) {
    s = String(s || '').toLowerCase();
    try { s = s.normalize('NFD').replace(/[\u0300-\u036f]/g, ''); } catch (e) {}
    return s.replace(/^\s+|\s+$/g, '');
  }

  // ---------- rede ----------
  function pegar(url, ok, erro) {
    var x = new XMLHttpRequest();
    x.open('GET', url, true);
    x.timeout = 20000;
    x.onload = function () {
      if (x.status >= 200 && x.status < 300) ok(x.responseText); else erro();
    };
    x.onerror = erro;
    x.ontimeout = erro;
    x.send();
  }

  function pegarPrimeiro(nomes, ok, falha) {
    var i = 0;
    (function tenta() {
      if (i >= nomes.length) { falha(); return; }
      pegar(BASE + nomes[i++], ok, tenta);
    })();
  }

  function extrair(obj, chaves) {
    if (!obj) return null;
    if (obj instanceof Array) return obj;
    for (var i = 0; i < chaves.length; i++) {
      if (obj[chaves[i]] instanceof Array) return obj[chaves[i]];
    }
    return null;
  }

  function lerJson(txt) {
    try { return JSON.parse(String(txt).replace(/^\uFEFF/, '')); } catch (e) { return null; }
  }

  function ordenarFilmes(lista) {
    lista.sort(function (a, b) {
      var d = (b.ano || 0) - (a.ano || 0);
      if (d !== 0) return d;
      return norm(a.titulo) < norm(b.titulo) ? -1 : 1;
    });
    return lista;
  }

  function limparLista(lista, tipo) {
    var r = [];
    for (var i = 0; i < lista.length; i++) {
      if (lista[i] && lista[i].titulo) r.push(lista[i]);
    }
    if (tipo === 'filme') ordenarFilmes(r);
    return r;
  }

  function carregar() {
    $('carregando').style.display = 'block';
    $('carregando').textContent = 'Carregando catálogo...';

    pegarPrimeiro(['catalogo.json'], function (txt) {
      raiz = lerJson(txt) || {};
      proxima(0);
    }, function () {
      $('carregando').textContent = 'Erro ao carregar o catálogo. Verifique a internet e reabra o app.';
    });
  }

  function proxima(i) {
    if (i >= ORDEM.length) { pronto(); return; }
    var chave = ORDEM[i];
    var f = FONTES[chave];

    function daRaiz() {
      dados[chave] = limparLista(extrair(raiz, f.chaves) || [], f.tipo);
      proxima(i + 1);
    }

    if (chave === 'filmes') { daRaiz(); return; }

    pegarPrimeiro(f.arquivos, function (txt) {
      var arr = extrair(lerJson(txt), f.chaves);
      if (arr) { dados[chave] = limparLista(arr, f.tipo); proxima(i + 1); }
      else daRaiz();
    }, daRaiz);
  }

  function pronto() {
    $('carregando').style.display = 'none';
    montarTabs();
    abrirSecao('filmes');
    setFoco('grid', 0);
  }

  // ---------- assistidos ----------
  function chaveEp(serie, t, e) { return serie.titulo + '|T' + t + '|E' + e; }
  function lerVistos() {
    try { return JSON.parse(localStorage.getItem('vistos') || '{}'); } catch (e) { return {}; }
  }
  function vistoSim(chave) { return !!lerVistos()[chave]; }
  function marcarVisto(chave) {
    try { var v = lerVistos(); v[chave] = 1; localStorage.setItem('vistos', JSON.stringify(v)); } catch (e) {}
  }

  // ---------- telas ----------
  function montarTabs() {
    var box = $('tabs');
    box.innerHTML = '';
    tabsEls = [];
    ORDEM.forEach(function (chave, i) {
      var d = document.createElement('div');
      d.className = 'tab';
      d.textContent = FONTES[chave].titulo + ' (' + (dados[chave] || []).length + ')';
      d.onclick = function () { setFoco('tabs', i); abrirSecao(chave); setFoco('grid', 0); };
      d.onmouseenter = function () { setFoco('tabs', i); };
      box.appendChild(d);
      tabsEls.push(d);
    });
  }

  function marcarTabAtiva() {
    for (var i = 0; i < tabsEls.length; i++) {
      tabsEls[i].className = 'tab' + (ORDEM[i] === estado.secao ? ' on' : '') +
        (estado.zona === 'tabs' && estado.idx === i ? ' foco' : '');
    }
  }

  function abrirSecao(chave) {
    estado.secao = chave;
    estado.categoria = 'Todos';
    historico = [];
    marcarTabAtiva();
    montarChips();
    renderCatalogo();
  }

  function montarChips() {
    var box = $('chips');
    box.innerHTML = '';
    chipsEls = [];
    var lista = dados[estado.secao] || [];
    var vistas = {}, cats = [];
    lista.forEach(function (it) {
      var c = it.categoria;
      if (c && !vistas[norm(c)]) { vistas[norm(c)] = 1; cats.push(c); }
    });
    cats.sort();
    ['Todos'].concat(cats).forEach(function (cat) {
      var d = document.createElement('div');
      d.className = 'chip' + (cat === estado.categoria ? ' on' : '');
      d.textContent = cat;
      d.onclick = function () {
        estado.categoria = cat;
        marcarChips();
        renderCatalogo();
        setFoco('grid', 0);
      };
      d.onmouseenter = function () { setFoco('chips', chipsEls.indexOf(d)); };
      box.appendChild(d);
      chipsEls.push(d);
    });
  }

  function marcarChips() {
    for (var i = 0; i < chipsEls.length; i++) {
      chipsEls[i].className = 'chip' + (chipsEls[i].textContent === estado.categoria ? ' on' : '');
    }
  }

  function modoCatalogo() {
    estado.modo = 'catalogo';
    $('chips').style.display = 'block';
    $('trilha').style.display = 'none';
  }

  function modoTrilha(texto) {
    estado.modo = 'trilha';
    $('chips').style.display = 'none';
    $('trilha').style.display = 'block';
    $('trilha').textContent = texto;
  }

  function renderCatalogo() {
    modoCatalogo();
    var f = FONTES[estado.secao];
    var lista = (dados[estado.secao] || []).filter(function (it) {
      return estado.categoria === 'Todos' || norm(it.categoria) === norm(estado.categoria);
    });

    var cards = lista.map(function (it) {
      if (f.tipo === 'filme') {
        return {
          titulo: it.titulo,
          sub: (it.ano ? it.ano + ' • ' : '') + (it.categoria || ''),
          capa: it.capa,
          acao: function () { tocar(it.titulo, it.video); }
        };
      }
      var nt = (it.temporadas || []).length;
      return {
        titulo: it.titulo,
        sub: (it.categoria || '') + ' • ' + nt + ' temporada(s)',
        capa: it.capa,
        acao: function () { abrirSerie(it); }
      };
    });
    mostrarCards(cards, 'Nada encontrado aqui');
  }

  function abrirSerie(serie) {
    var salvo = estado.idx;
    historico.push(function () { renderCatalogo(); setFoco('grid', salvo); });
    mostrarTemporadas(serie);
    setFoco('grid', 0);
  }

  function mostrarTemporadas(serie) {
    modoTrilha(serie.titulo);
    var cards = (serie.temporadas || []).map(function (t) {
      return {
        titulo: 'Temporada ' + t.numero,
        sub: (t.episodios || []).length + ' episódios',
        capa: serie.capa,
        acao: function () {
          var salvo = estado.idx;
          historico.push(function () { mostrarTemporadas(serie); setFoco('grid', salvo); });
          mostrarEpisodios(serie, t);
          setFoco('grid', 0);
        }
      };
    });
    mostrarCards(cards, 'Sem temporadas');
  }

  function mostrarEpisodios(serie, temp) {
    modoTrilha(serie.titulo + '  ›  Temporada ' + temp.numero);
    var cards = (temp.episodios || []).map(function (ep) {
      var chave = chaveEp(serie, temp.numero, ep.numero);
      var sub = ep.titulo;
      if (!sub || /^epis[oó]dio\s*\d+$/i.test(sub)) sub = serie.titulo + ' • T' + temp.numero;
      return {
        titulo: 'EP ' + ep.numero,
        sub: sub,
        capa: serie.capa,
        visto: vistoSim(chave),
        acao: function (el) {
          if (ep.video) {
            marcarVisto(chave);
            if (el) el.className = el.className.replace(' visto', '') + ' visto';
          }
          tocar(serie.titulo + ' - EP ' + ep.numero, ep.video);
        }
      };
    });
    mostrarCards(cards, 'Sem episódios');
  }

  // ---------- cards ----------
  function mostrarCards(cards, textoVazio) {
    estado.cards = cards;
    estado.els = [];
    estado.mostrados = 0;
    var grid = $('grid');
    grid.innerHTML = '';
    $('main').scrollTop = 0;

    if (!cards.length) {
      var v = document.createElement('div');
      v.className = 'vazio';
      v.textContent = textoVazio;
      grid.appendChild(v);
      return;
    }
    adicionarLote();
  }

  function adicionarLote() {
    var grid = $('grid');
    var fim = Math.min(estado.mostrados + LOTE, estado.cards.length);
    for (var i = estado.mostrados; i < fim; i++) {
      grid.appendChild(criarCard(estado.cards[i], i));
    }
    estado.mostrados = fim;
  }

  function criarCard(c, i) {
    var d = document.createElement('div');
    d.className = 'card' + (c.visto ? ' visto' : '');

    var img = document.createElement('img');
    img.alt = '';
    img.onerror = function () { img.style.visibility = 'hidden'; };
    if (c.capa) img.src = c.capa;
    d.appendChild(img);

    var selo = document.createElement('div');
    selo.className = 'selo';
    selo.textContent = '✓ ASSISTIDO';
    d.appendChild(selo);

    var info = document.createElement('div');
    info.className = 'info';
    var t = document.createElement('div');
    t.className = 't';
    t.textContent = c.titulo;
    var s = document.createElement('div');
    s.className = 's';
    s.textContent = c.sub || '';
    info.appendChild(t);
    info.appendChild(s);
    d.appendChild(info);

    d.onclick = function () { setFoco('grid', i); c.acao(d); };
    d.onmouseenter = function () { setFoco('grid', i); };
    estado.els.push(d);
    return d;
  }

  function colunas() {
    var els = estado.els;
    if (!els.length) return 1;
    var topo = els[0].offsetTop, n = 0;
    while (n < els.length && els[n].offsetTop === topo) n++;
    return Math.max(n, 1);
  }

  // ---------- foco ----------
  function elementos(zona) {
    if (zona === 'tabs') return tabsEls;
    if (zona === 'chips') return chipsEls;
    return estado.els;
  }

  function limparFoco() {
    var fs = document.querySelectorAll('.foco');
    for (var i = 0; i < fs.length; i++) {
      fs[i].className = fs[i].className.replace(/\s*foco/g, '');
    }
  }

  function setFoco(zona, idx) {
    if (zona === 'chips' && estado.modo !== 'catalogo') zona = 'tabs';
    var lista = elementos(zona);

    if (!lista.length) {
      if (zona !== 'tabs') setFoco('tabs', 0);
      return;
    }

    if (zona === 'grid') {
      while (idx >= lista.length && estado.mostrados < estado.cards.length) adicionarLote();
      if (idx > estado.els.length - colunas() * 2 && estado.mostrados < estado.cards.length) adicionarLote();
      lista = estado.els;
    }

    if (idx < 0) idx = 0;
    if (idx >= lista.length) idx = lista.length - 1;

    limparFoco();
    estado.zona = zona;
    estado.idx = idx;

    var e = lista[idx];
    e.className += ' foco';

    if (zona === 'grid') {
      var m = $('main');
      var topo = e.offsetTop, alt = e.offsetHeight, vis = m.clientHeight;
      if (topo - 14 < m.scrollTop) m.scrollTop = Math.max(0, topo - 14);
      else if (topo + alt + 14 > m.scrollTop + vis) m.scrollTop = topo + alt + 14 - vis;
    } else if (zona === 'chips') {
      $('chips').scrollLeft = Math.max(0, e.offsetLeft - 200);
    }
  }

  function mover(dx, dy) {
    var z = estado.zona, i = estado.idx;
    var temChips = estado.modo === 'catalogo' && chipsEls.length > 0;

    if (z === 'tabs') {
      if (dx) setFoco('tabs', i + dx);
      else if (dy > 0) setFoco(temChips ? 'chips' : 'grid', 0);
    } else if (z === 'chips') {
      if (dx) setFoco('chips', i + dx);
      else if (dy < 0) setFoco('tabs', ORDEM.indexOf(estado.secao));
      else if (dy > 0) setFoco('grid', 0);
    } else {
      var n = estado.els.length, c = colunas();
      if (dx) {
        setFoco('grid', i + dx);
      } else if (dy < 0) {
        if (i - c >= 0) setFoco('grid', i - c);
        else setFoco(temChips ? 'chips' : 'tabs', temChips ? 0 : ORDEM.indexOf(estado.secao));
      } else if (dy > 0) {
        var ultimaLinha = Math.floor((estado.cards.length - 1) / c) * c;
        if (i < ultimaLinha) setFoco('grid', Math.min(i + c, estado.cards.length - 1));
      }
    }
  }

  function ativar() {
    var lista = elementos(estado.zona);
    if (lista[estado.idx]) lista[estado.idx].onclick();
  }

  function voltar() {
    if (historico.length) {
      var f = historico.pop();
      f();
      return true;
    }
    return false;
  }

  // ---------- aviso ----------
  function aviso(texto) {
    var a = $('aviso');
    a.textContent = texto;
    a.style.display = 'block';
    clearTimeout(timerAviso);
    timerAviso = setTimeout(function () { a.style.display = 'none'; }, 2800);
  }

  // ---------- player ----------
  function fmt(s) {
    if (!isFinite(s) || s < 0) s = 0;
    s = Math.floor(s);
    var h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), x = s % 60;
    return (h > 0 ? h + ':' + (m < 10 ? '0' : '') : '') + m + ':' + (x < 10 ? '0' : '') + x;
  }

  function mostrarBarra() {
    $('pbar').style.display = 'block';
    $('ptitulo').style.display = 'block';
    clearTimeout(timerBarra);
    timerBarra = setTimeout(function () {
      $('pbar').style.display = 'none';
      $('ptitulo').style.display = 'none';
    }, 4000);
  }

  function tocar(titulo, url) {
    if (!url) { aviso('Vídeo ainda não disponível'); return; }
    var v = $('video');
    $('ptitulo').textContent = titulo;
    $('player').style.display = 'block';
    $('pespera').style.display = 'block';
    playerAberto = true;
    v.src = url;
    try { v.play(); } catch (e) {}
    mostrarBarra();
  }

  function fecharPlayer() {
    var v = $('video');
    try { v.pause(); } catch (e) {}
    v.removeAttribute('src');
    try { v.load(); } catch (e) {}
    $('player').style.display = 'none';
    playerAberto = false;
  }

  function iniciarPlayer() {
    var v = $('video');
    v.addEventListener('timeupdate', function () {
      var d = v.duration || 0;
      $('pprog').style.width = (d ? (v.currentTime / d * 100) : 0) + '%';
      $('ptempo').textContent = fmt(v.currentTime) + ' / ' + fmt(d);
    });
    v.addEventListener('waiting', function () { $('pespera').style.display = 'block'; });
    v.addEventListener('playing', function () { $('pespera').style.display = 'none'; });
    v.addEventListener('ended', fecharPlayer);
    v.addEventListener('error', function () {
      if (!playerAberto) return;
      aviso('Não foi possível reproduzir este vídeo');
      fecharPlayer();
    });
  }

  function teclaPlayer(e) {
    var v = $('video');
    var k = e.keyCode;
    e.preventDefault();

    if (k === 461 || k === 27 || k === 413 || k === 8) { fecharPlayer(); return; }
    if (k === 13 || k === 415 || k === 19) {
      if (k === 415) v.play();
      else if (k === 19) v.pause();
      else if (v.paused) v.play(); else v.pause();
    } else if (k === 37 || k === 412) {
      v.currentTime = Math.max(0, v.currentTime - 10);
    } else if (k === 39 || k === 417) {
      v.currentTime = v.currentTime + 10;
    }
    mostrarBarra();
  }

  // ---------- teclas ----------
  document.addEventListener('keydown', function (e) {
    var k = e.keyCode;

    if (playerAberto) { teclaPlayer(e); return; }

    if (k === 37) { mover(-1, 0); e.preventDefault(); }
    else if (k === 39) { mover(1, 0); e.preventDefault(); }
    else if (k === 38) { mover(0, -1); e.preventDefault(); }
    else if (k === 40) { mover(0, 1); e.preventDefault(); }
    else if (k === 13) { ativar(); e.preventDefault(); }
    else if (k === 461 || k === 27) {
      // Se não houver para onde voltar, deixa o webOS fechar o app.
      if (voltar()) e.preventDefault();
    }
  });

  iniciarPlayer();
  carregar();
})();
        
