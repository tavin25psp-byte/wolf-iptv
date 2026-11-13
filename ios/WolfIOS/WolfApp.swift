import SwiftUI
import AVKit

let baseURL = "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/"

struct Episodio: Codable, Hashable {
    let numero: Int?
    let titulo: String?
    let video: String
}
struct Temporada: Codable, Hashable {
    let numero: Int?
    let episodios: [Episodio]
}
struct Item: Codable, Hashable, Identifiable {
    var id: String { titulo }
    let titulo: String
    let categoria: String?
    let capa: String?
    let video: String?
    let sinopse: String?
    let descricao: String?
    let temporadas: [Temporada]?
    var resumo: String { sinopse ?? descricao ?? "" }
}
struct Canal: Codable, Hashable, Identifiable {
    var id: String { nome + url }
    let nome: String
    let url: String
    let logo: String?
    init(from d: Decoder) throws {
        let c = try d.container(keyedBy: K.self)
        nome = (try? c.decode(String.self, forKey: .nome)) ?? (try? c.decode(String.self, forKey: .name)) ?? ""
        url  = (try? c.decode(String.self, forKey: .url))  ?? (try? c.decode(String.self, forKey: .link)) ?? ""
        logo = try? c.decode(String.self, forKey: .logo)
    }
    enum K: String, CodingKey { case nome, name, url, link, logo }
}

func carregar<T: Decodable>(_ tipo: T.Type, arquivos: [String], chaves: [String]) async -> [T] {
    let dec = JSONDecoder()
    for arq in arquivos {
        guard let url = URL(string: baseURL + arq),
              let (data, resp) = try? await URLSession.shared.data(from: url),
              (resp as? HTTPURLResponse)?.statusCode == 200 else { continue }
        if let arr = try? dec.decode([T].self, from: data) { return arr }
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            for k in chaves {
                if let a = obj[k],
                   let d = try? JSONSerialization.data(withJSONObject: a),
                   let arr = try? dec.decode([T].self, from: d) { return arr }
            }
        }
    }
    return []
}

@MainActor
final class Catalogo: ObservableObject {
    @Published var filmes: [Item] = []
    @Published var series: [Item] = []
    @Published var animes: [Item] = []
    @Published var canais: [Canal] = []

    func carregarTudo() async {
        async let f = carregar(Item.self, arquivos: ["catalogo.json"], chaves: ["filmes"])
        async let s = carregar(Item.self, arquivos: ["series.json", "serie.json"], chaves: ["series"])
        async let a = carregar(Item.self, arquivos: ["animes.json", "anime.json"], chaves: ["animes"])
        async let c = carregar(Canal.self, arquivos: ["canais.json"], chaves: ["canais", "channels"])
        (filmes, series, animes, canais) = await (f, s, a, c)
    }
}

@main
struct WolfApp: App {
    @StateObject private var cat = Catalogo()
    var body: some Scene {
        WindowGroup {
            TabView {
                GradeView(titulo: "Filmes", itens: cat.filmes).tabItem { Label("Filmes", systemImage: "film") }
                GradeView(titulo: "Séries", itens: cat.series).tabItem { Label("Séries", systemImage: "tv") }
                GradeView(titulo: "Anime", itens: cat.animes).tabItem { Label("Anime", systemImage: "sparkles.tv") }
                CanaisView(canais: cat.canais).tabItem { Label("Ao vivo", systemImage: "antenna.radiowaves.left.and.right") }
            }
            .preferredColorScheme(.dark)
            .task { await cat.carregarTudo() }
        }
    }
}

struct GradeView: View {
    let titulo: String
    let itens: [Item]
    @State private var busca = ""
    private var filtrados: [Item] {
        busca.isEmpty ? itens : itens.filter { $0.titulo.localizedCaseInsensitiveContains(busca) }
    }
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 110), spacing: 12)], spacing: 12) {
                    ForEach(filtrados) { item in
                        NavigationLink(value: item) {
                            VStack {
                                AsyncImage(url: URL(string: item.capa ?? "")) { img in
                                    img.resizable().scaledToFill()
                                } placeholder: { Color.gray.opacity(0.3) }
                                .frame(height: 160).clipped().cornerRadius(8)
                                Text(item.titulo).font(.caption).lineLimit(2).foregroundColor(.primary)
                            }
                        }
                    }
                }.padding()
            }
            .navigationTitle(titulo)
            .searchable(text: $busca)
            .navigationDestination(for: Item.self) { DetalheView(item: $0) }
        }
    }
}

struct DetalheView: View {
    let item: Item
    var body: some View {
        List {
            if !item.resumo.isEmpty { Text(item.resumo).font(.subheadline) }
            if let v = item.video, !v.isEmpty {
                NavigationLink("▶ Assistir") { PlayerView(url: v) }
            }
            ForEach(item.temporadas ?? [], id: \.self) { t in
                Section("Temporada \(t.numero ?? 0)") {
                    ForEach(t.episodios, id: \.self) { ep in
                        NavigationLink("\(ep.numero ?? 0). \(ep.titulo ?? "Episódio")") {
                            PlayerView(url: ep.video)
                        }
                    }
                }
            }
        }
        .navigationTitle(item.titulo)
    }
}

struct CanaisView: View {
    let canais: [Canal]
    var body: some View {
        NavigationStack {
            List(canais) { c in
                NavigationLink(c.nome) { PlayerView(url: c.url) }
            }.navigationTitle("Ao vivo")
        }
    }
}

struct PlayerView: View {
    let url: String
    @State private var player: AVPlayer?
    var body: some View {
        VideoPlayer(player: player)
            .ignoresSafeArea()
            .onAppear {
                let decoded = url.removingPercentEncoding ?? url
                if let u = URL(string: decoded) ?? URL(string: url) {
                    player = AVPlayer(url: u); player?.play()
                }
            }
            .onDisappear { player?.pause() }
    }
}
