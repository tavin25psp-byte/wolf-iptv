"""Confere ano, gênero e sinopse de filmes.json usando o TMDB.
Só consulta filmes novos (os já consultados ficam em cache_tmdb.json)."""
import json, os, re, sys, time, unicodedata
import requests

KEY = os.environ["TMDB_KEY"]
ARQ = os.environ.get("ARQUIVO", "catalogo.json")
MANTER = {"Natalino", "Religioso", "Nacional", "Família", "Infantil"}  # categorias que o TMDB não tem
GEN = {28:"Ação",12:"Aventura",16:"Animação",35:"Comédia",80:"Crime",99:"Documentário",
       18:"Drama",10751:"Família",14:"Fantasia",36:"História",27:"Terror",10402:"Música",
       9648:"Mistério",10749:"Romance",878:"Ficção",10770:"Cinema TV",53:"Suspense",
       10752:"Guerra",37:"Faroeste"}

def norm(s):
    s = unicodedata.normalize("NFD", (s or "").lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", " ", s).strip()

def get(params):
    for t in range(5):
        r = requests.get("https://api.themoviedb.org/3/search/movie", params=params, timeout=20)
        if r.status_code == 429:
            time.sleep(2 + t * 2); continue
        r.raise_for_status()
        return r.json().get("results", [])
    return []

def buscar(titulo, ano):
    base = {"api_key": KEY, "query": titulo, "language": "pt-BR", "include_adult": "false"}
    res = get({**base, "year": ano}) if ano else []
    return res or get(base)

cache = json.load(open("cache_tmdb.json", encoding="utf-8")) if os.path.exists("cache_tmdb.json") else {}
dados = json.load(open(ARQ, encoding="utf-8"))
filmes = dados["filmes"] if isinstance(dados, dict) else dados
revisar, novos = [], 0

def salvar():
    json.dump(cache, open("cache_tmdb.json", "w", encoding="utf-8"), ensure_ascii=False)
    json.dump(dados, open(ARQ, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    json.dump(revisar, open("revisar.json", "w", encoding="utf-8"), ensure_ascii=False, indent=2)

for i, f in enumerate(filmes):
    k = f"{f['titulo']}|{f.get('ano') or ''}"
    if k not in cache:
        try:
            res = buscar(f["titulo"], f.get("ano"))
        except Exception as e:
            print("erro:", f["titulo"], e); continue
        ok = [x for x in res if norm(x.get("title")) == norm(f["titulo"]) or norm(x.get("original_title")) == norm(f["titulo"])]
        cache[k] = max(ok, key=lambda x: x.get("popularity", 0)) if ok else None
        novos += 1
        time.sleep(0.05)
        if novos % 300 == 0:
            salvar(); print(f"{i}/{len(filmes)}", flush=True)
    m = cache[k]
    if not m or m.get("adult"):
        revisar.append(f["titulo"]); continue
    if m.get("release_date"): f["ano"] = int(m["release_date"][:4])
    g = [GEN[x] for x in m.get("genre_ids", []) if x in GEN]
    if g and f.get("categoria") not in MANTER: f["categoria"] = g[0]
    if m.get("overview") and not f.get("sinopse"): f["sinopse"] = m["overview"]

salvar()
print(f"pronto: {len(filmes)-len(revisar)} conferidos, {len(revisar)} para revisar, {novos} consultas novas")
