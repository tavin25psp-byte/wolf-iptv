import json
import re
import urllib.request

M3U_URL = "http://rrcwb.biz/get.php?username=382395&password=TttQTy&type=m3u_plus"

def extrair_canais():
    req = urllib.request.Request(M3U_URL, headers={'User-Agent': 'Mozilla/5.0'})
    try:
        with urllib.request.urlopen(req) as response:
            conteudo = response.read().decode('utf-8', errors='ignore')
    except Exception as e:
        print(f"Erro ao descarregar a lista: {e}")
        return []

    canais = []
    linhas = conteudo.splitlines()
    
    tvg_name_regex = re.compile(r'tvg-name="([^"]*)"')
    tvg_logo_regex = re.compile(r'tvg-logo="([^"]*)"')
    group_title_regex = re.compile(r'group-title="([^"]*)"')
    
    i = 0
    while i < len(linhas):
        linha = linhas[i].strip()
        if linha.startswith('#EXTINF:'):
            tvg_name = tvg_name_regex.search(linha)
            tvg_logo = tvg_logo_regex.search(linha)
            group_title = group_title_regex.search(linha)
            nome_canal = linha.split(',')[-1].strip() if ',' in linha else "Canal Sem Nome"
            
            i += 1
            while i < len(linhas) and not linhas[i].strip().startswith('http'):
                i += 1
                
            if i < len(linhas):
                stream_url = linhas[i].strip()
                canais.append({
                    "name": nome_canal,
                    "tvg_name": tvg_name.group(1) if tvg_name else nome_canal,
                    "logo": tvg_logo.group(1) if tvg_logo else "",
                    "group": group_title.group(1) if group_title else "Geral",
                    "url": stream_url
                })
        i += 1

    return canais

lista_canais = extrair_canais()

if lista_canais:
    with open("canais.json", "w", encoding="utf-8") as f:
        json.dump({"total": len(lista_canais), "channels": lista_canais}, f, ensure_ascii=False, indent=2)
    print(f"Sucesso! {len(lista_canais)} canais extraídos.")
  
