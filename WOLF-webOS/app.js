'use strict';

const BASE = 'https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/';

const SOURCES = {
    Filmes: ['catalogo.json'],
    Séries: ['series.json', 'serie.json'],
    Doramas: ['doramas.json', 'Doramas.json'],
    Anime: ['animes.json', 'anime.json', 'Animes.json', 'Anime.json'],
    Desenhos: ['desenho.json', 'desenhos.json', 'Desenho.json', 'Desenhos.json']
};

let data = {
    Filmes: [],
    Séries: [],
    Doramas: [],
    Anime: [],
    Desenhos: []
};

let current = 'Filmes';
let focusables = [];
let focusIndex = 0;
let drawerOpen = false;
let history = [];

const $ = s => document.querySelector(s);

const content = $('#content');
const drawer = $('#drawer');
const drawerItems = $('#drawerItems');
const status = $('#status');
const player = $('#player');
const video = $('#video');

function norm(s) {
    return String(s || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();
}

async function getJson(names) {
    for (const n of names) {
        try {
            const r = await fetch(BASE + n, {
                cache: 'no-store'
            });

            if (r.ok) {
                return await r.json();
            }
        } catch (e) {}
    }

    return null;
}

async function load() {
    status.textContent = 'Carregando catálogo...';

    for (const [k, names] of Object.entries(SOURCES)) {
        const j = await getJson(names);

        data[k] =
            j?.filmes ||
            j?.series ||
            j?.doramas ||
            j?.animes ||
            j?.desenhos ||
            [];
    }

    buildMenu();
    renderHome();

    status.textContent = 'WOLF IPTV';
}

function buildMenu() {
    drawerItems.innerHTML = '';

    [
        ['Filmes', '🎬'],
        ['Séries', '📺'],
        ['Doramas', '🌸'],
        ['Anime', '⚡'],
        ['Desenhos', '🧸']
    ].forEach(([name, ico]) => {

        const d = document.createElement('div');

        d.className = 'drawerItem focusable';
        d.tabIndex = 0;
        d.textContent = ico + '  ' + name;

        d.onclick = () => {
            closeDrawer();
            showList(name);
        };

        drawerItems.appendChild(d);
    });

    const f = document.createElement('div');

    f.className = 'drawerItem focusable';
    f.tabIndex = 0;
    f.textContent = '⭐  Favoritos';

    f.onclick = () => {
        closeDrawer();
        showFavorites();
    };

    drawerItems.appendChild(f);
}

function setupFocus() {
    focusables = [
        ...document.querySelectorAll('.focusable')
    ];

    focusables.forEach((e, i) => {

        e.onfocus = () => {
            focusIndex = i;

            e.classList.add('focus');

            e.scrollIntoView({
                block: 'nearest',
                inline: 'nearest'
            });
        };

        e.onblur = () => {
            e.classList.remove('focus');
        };
    });

    if (focusables.length) {
        focusIndex = Math.min(
            focusIndex,
            focusables.length - 1
        );

        focusables[focusIndex].focus();
    }
}

function img(src) {
    return src || '';
}

function card(item, type) {

    const d = document.createElement('div');

    d.className = 'card focusable';
    d.tabIndex = 0;

    const im = document.createElement('img');

    im.loading = 'lazy';
    im.src = img(item.capa);

    im.onerror = () => {
        im.style.display = 'none';

        if (im.nextElementSibling) {
            im.nextElementSibling.style.display = 'flex';
        }
    };

    d.appendChild(im);

    const ph = document.createElement('div');

    ph.className = 'placeholder';
    ph.style.display = 'none';
    ph.textContent = 'WOLF';

    d.appendChild(ph);

    const m = document.createElement('div');

    m.className = 'meta';

    m.innerHTML = `
        <b>${escapeHtml(item.titulo || 'Sem título')}</b>
        <div class="year">
            ${item.ano || ''}
            ${item.categoria ? ' • ' + escapeHtml(item.categoria) : ''}
        </div>
    `;

    d.appendChild(m);

    d.onclick = () => {

        if (type === 'Filmes' || type === 'Desenhos') {
            play(item.video, item.titulo);
        } else {
            showSeries(item, type);
        }

    };

    return d;
}

function escapeHtml(s) {

    return String(s).replace(
        /[&<>"']/g,
        c => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[c])
    );
}

function renderHome() {

    content.innerHTML = '';

    for (const k of Object.keys(data)) {

        if (!data[k].length) {
            continue;
        }

        const sec = document.createElement('section');

        sec.className = 'section';

        sec.innerHTML = `<h2>${k}</h2>`;

        const row = document.createElement('div');

        row.className = 'row';

        data[k]
            .slice(0, 10)
            .forEach(x => row.appendChild(card(x, k)));

        sec.appendChild(row);

        content.appendChild(sec);
    }

    focusIndex = 0;

    setupFocus();
}

function showList(type) {

    current = type;

    content.innerHTML = '';

    const back = document.createElement('div');

    back.className = 'episode back focusable';
    back.tabIndex = 0;
    back.textContent = '← Voltar';

    back.onclick = () => renderHome();

    content.appendChild(back);

    const sec = document.createElement('section');

    sec.className = 'section';

    sec.innerHTML = `<h1>${type}</h1>`;

    const row = document.createElement('div');

    row.className = 'row';

    data[type].forEach(x => {
        row.appendChild(card(x, type));
    });

    sec.appendChild(row);

    content.appendChild(sec);

    focusIndex = 0;

    setupFocus();
}

function showSeries(s, type) {

    content.innerHTML = '';

    const back = document.createElement('div');

    back.className = 'episode back focusable';
    back.tabIndex = 0;
    back.textContent = '← ' + type;

    back.onclick = () => showList(type);

    content.appendChild(back);

    const detail = document.createElement('div');

    detail.className = 'detail';

    detail.innerHTML = `
        <img src="${img(s.capa)}">

        <div>
            <h1>${escapeHtml(s.titulo)}</h1>

            <div>
                ${escapeHtml(s.categoria || '')}
            </div>

            <p>
                ${s.temporadas?.length || 0}
                temporada(s)
            </p>
        </div>
    `;

    content.appendChild(detail);

    (s.temporadas || []).forEach(t => {

        const h = document.createElement('div');

        h.className = 'seasonTitle';

        h.textContent =
            'Temporada ' + t.numero;

        content.appendChild(h);

        (t.episodios || []).forEach(e => {

            const b = document.createElement('div');

            b.className = 'episode focusable';
            b.tabIndex = 0;

            b.textContent =
                '▶  ' +
                (e.titulo ||
                    ('Episódio ' + e.numero));

            b.onclick = () => {

                if (e.video) {

                    play(
                        e.video,
                        `${s.titulo} — ${e.titulo}`
                    );

                } else {

                    toast(
                        'Este episódio ainda não possui vídeo.'
                    );

                }
            };

            content.appendChild(b);
        });
    });

    focusIndex = 0;

    setupFocus();
}

function showFavorites() {

    const all = [
        ...data.Filmes,
        ...data.Desenhos
    ].filter(
        x => localStorage.getItem(
            'fav:' + x.titulo
        ) === '1'
    );

    content.innerHTML =
        '<h1>⭐ Favoritos</h1>';

    const row = document.createElement('div');

    row.className = 'row';

    all.forEach(x => {
        row.appendChild(
            card(x, 'Filmes')
        );
    });

    content.appendChild(row);

    setupFocus();
}

function toast(t) {

    $('#toast').textContent = t;

    $('#toast').style.display = 'block';

    setTimeout(() => {
        $('#toast').style.display = 'none';
    }, 2200);
}

function play(url, title) {

    if (!url) {
        toast('Vídeo indisponível.');
        return;
    }

    $('#playerTitle').textContent =
        title || 'WOLF IPTV';

    player.hidden = false;

    video.src = url;

    video.play().catch(() => {});

    $('#closePlayer').focus();
}

function closePlayer() {

    video.pause();

    video.removeAttribute('src');

    video.load();

    player.hidden = true;

    setupFocus();
}

function openDrawer() {

    drawerOpen = true;

    drawer.classList.add('open');

    drawer.setAttribute(
        'aria-hidden',
        'false'
    );

    setTimeout(() => {

        focusables = [
            ...drawer.querySelectorAll(
                '.focusable'
            )
        ];

        focusIndex = 0;

        setupFocus();

    }, 30);
}

function closeDrawer() {

    drawerOpen = false;

    drawer.classList.remove('open');

    drawer.setAttribute(
        'aria-hidden',
        'true'
    );

    focusIndex = 0;

    setupFocus();
}

$('#menuBtn').onclick = openDrawer;

$('#closePlayer').onclick = closePlayer;

document.addEventListener(
    'keydown',
    e => {

        const k =
            e.keyCode ||
            e.which;

        if (!player.hidden) {

            if (
                k === 461 ||
                k === 27
            ) {

                e.preventDefault();

                closePlayer();
            }

            return;
        }

        if (drawerOpen) {

            if (
                k === 461 ||
                k === 27
            ) {

                e.preventDefault();

                closeDrawer();

                return;
            }

            navDrawer(k);

            return;
        }

        if (
            k === 461 ||
            k === 27
        ) {

            e.preventDefault();

            renderHome();

            return;
        }

        if (k === 13) {

            e.preventDefault();

            focusables[
                focusIndex
            ]?.click();

            return;
        }

        if (
            k === 37 ||
            k === 38 ||
            k === 39 ||
            k === 40
        ) {

            e.preventDefault();

            nav(k);

            return;
        }

        if (k === 77) {
            openDrawer();
        }

    }
);

function nav(k) {

    if (!focusables.length) {
        return;
    }

    let idx = focusIndex;

    const cols = 5;

    if (k === 37) {
        idx = Math.max(
            0,
            idx - 1
        );
    }

    if (k === 39) {
        idx = Math.min(
            focusables.length - 1,
            idx + 1
        );
    }

    if (k === 38) {
        idx = Math.max(
            0,
            idx - cols
        );
    }

    if (k === 40) {
        idx = Math.min(
            focusables.length - 1,
            idx + cols
        );
    }

    focusables[idx]?.focus();
}

function navDrawer(k) {

    const fs = [
        ...drawer.querySelectorAll(
            '.focusable'
        )
    ];

    let i =
        fs.indexOf(
            document.activeElement
        );

    if (i < 0) {
        i = 0;
    }

    if (k === 38) {
        i = Math.max(
            0,
            i - 1
        );
    }

    if (k === 40) {
        i = Math.min(
            fs.length - 1,
            i + 1
        );
    }

    if (k === 13) {
        fs[i]?.click();
    } else {
        fs[i]?.focus();
    }
}

load();
