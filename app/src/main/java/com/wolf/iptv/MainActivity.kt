package com.example.wolfiptv

import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF050609)
private val Panel = Color(0xFF10131B)
private val Red = Color(0xFFFF1733)
private val Muted = Color(0xFF9DA5B5)
private val White = Color(0xFFF7F8FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Bg, surface = Panel, primary = Red,
                onBackground = White, onSurface = White
            )) {
                WolfHome()
            }
        }
    }
}

private data class Media(
    val title: String, val year: String, val genre: String, val art: String,
    val colors: List<Color>,
    val cover: String = "",
    val video: String = "",
    val synopsis: String = ""
)

@Composable
private fun WolfHome() {
    var selected by remember { mutableStateOf("Início") }
    var query by remember { mutableStateOf("") }
    var chosenMedia by remember { mutableStateOf<Media?>(null) }
    var loadingCatalog by remember { mutableStateOf(true) }
    var catalogError by remember { mutableStateOf("") }
    var movies by remember { mutableStateOf<List<Media>>(emptyList()) }
    var series by remember { mutableStateOf<List<Media>>(emptyList()) }
    var animes by remember { mutableStateOf<List<Media>>(emptyList()) }
    var drawings by remember { mutableStateOf<List<Media>>(emptyList()) }

    LaunchedEffect(Unit) {
        Thread {
            try {
                val base = "https://raw.githubusercontent.com/tavin25psp-byte/wolf-iptv/main/"
                fun safeLoad(file: String, keys: List<String>): List<Media> = try {
                    fetchMedia(base + file, keys)
                } catch (_: Exception) {
                    emptyList()
                }
                val loadedMovies = safeLoad("catalogo.json", listOf("filmes", "catalogo", "movies"))
                val loadedSeries = safeLoad("series.json", listOf("series", "séries"))
                val loadedAnimes = safeLoad("animes.json", listOf("animes", "anime"))
                val loadedDrawings = safeLoad("desenho.json", listOf("desenhos", "desenho", "cartoons"))
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    movies = loadedMovies
                    series = loadedSeries
                    animes = loadedAnimes
                    drawings = loadedDrawings
                    loadingCatalog = false
                    if (loadedMovies.isEmpty() && loadedSeries.isEmpty() && loadedAnimes.isEmpty() && loadedDrawings.isEmpty()) {
                        catalogError = "Os JSONs foram acessados, mas não encontrei itens. Confira os nomes dos arquivos e o formato."
                    }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    loadingCatalog = false
                    catalogError = "Não foi possível carregar os catálogos. Verifique a internet e os nomes dos arquivos no GitHub."
                }
            }
        }.start()
    }

    /* Dados remotos são usados no lugar de exemplos fixos. */
    val fallbackMovies = emptyList<Media>()
    val displayMovies = (if (movies.isNotEmpty()) movies else fallbackMovies).filter { query.isBlank() || it.title.contains(query, true) }
    val displaySeries = series.filter { query.isBlank() || it.title.contains(query, true) }
    val displayAnimes = animes.filter { query.isBlank() || it.title.contains(query, true) }
    val displayDrawings = drawings.filter { query.isBlank() || it.title.contains(query, true) }

    Row(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.width(218.dp).fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF12070B), Bg)))
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, bottom = 22.dp)) {
                Text("🐺", fontSize = 42.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("WOLF", fontSize = 27.sp, fontWeight = FontWeight.Black, color = White, lineHeight = 27.sp)
                    Text("IPTV", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Red, lineHeight = 21.sp)
                }
            }
            val nav = listOf(
                "Início" to Icons.Default.Home, "Filmes" to Icons.Default.Movie,
                "Séries" to Icons.Default.LiveTv, "Doramas" to Icons.Default.Favorite,
                "Anime" to Icons.Default.AutoAwesome, "Favoritos" to Icons.Default.Star,
                "Continue assistindo" to Icons.Default.History, "Buscar" to Icons.Default.Search,
                "Configurações" to Icons.Default.Settings
            )
            nav.forEach { (label, icon) ->
                val active = selected == label
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) Brush.horizontalGradient(listOf(Red, Color(0xFF61000E))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { selected = label }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = label, tint = if (active) White else Color(0xFFE5E7EF), modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(13.dp))
                    Text(label, color = White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 20.dp, top = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Buscar filmes, séries, doramas...", color = Muted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = White) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF333A4B), unfocusedBorderColor = Color(0xFF252A36),
                        focusedContainerColor = Color(0xCC0D1018), unfocusedContainerColor = Color(0xCC0D1018),
                        focusedTextColor = White, unfocusedTextColor = White
                    ),
                    modifier = Modifier.weight(1f).height(54.dp)
                )
                Spacer(Modifier.width(14.dp))
                listOf(Icons.Default.WorkspacePremium, Icons.Default.Favorite, Icons.Default.History, Icons.Default.Settings, Icons.Default.Person).forEach { icon ->
                    Box(Modifier.padding(horizontal = 4.dp).size(39.dp).clip(CircleShape)
                        .background(Color(0xAA111521)).clickable { selected = "Favoritos" },
                        contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = White, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("10:42", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Dom, 09 Nov", color = Muted, fontSize = 10.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(18.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF32050B), Color(0xFF100B13), Color(0xFF07182E))))
                    ) {
                        // Decorative light streaks give the hero a cinematic red/blue look.
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                            0.0f to Color(0x66FF102A), 0.42f to Color.Transparent,
                            0.72f to Color(0x33216EFF), 1.0f to Color(0x77216EFF)
                        )))
                        Text("🐺", modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 6.dp), fontSize = 118.sp)
                        Text("🥷", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 100.dp, bottom = 0.dp), fontSize = 155.sp)
                        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.62f).padding(start = 24.dp, top = 8.dp, bottom = 8.dp)) {
                            Surface(color = Red, shape = RoundedCornerShape(20.dp)) {
                                Text("🔥  LANÇAMENTO 2026", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                            Spacer(Modifier.height(9.dp))
                            Text("MORTAL KOMBAT II", fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black, color = White)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tag("2026"); Tag("Ação"); Tag("HD")
                            }
                            Spacer(Modifier.height(9.dp))
                            Text("Os campeões favoritos de lendas, agora em uma nova batalha. O destino da Terra está em jogo. Prepare-se para o maior torneio de todos os tempos.",
                                color = Color(0xFFE1E3E9), fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { chosenMedia = displayMovies.firstOrNull() }, colors = ButtonDefaults.buttonColors(containerColor = Red),
                                    shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 8.dp)) {
                                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(5.dp)); Text("Assistir agora", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { chosenMedia = displayMovies.firstOrNull() }, border = BorderStroke(1.dp, Color(0xFF454B5C)),
                                    shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)) {
                                    Icon(Icons.Default.Star, null, tint = White); Spacer(Modifier.width(5.dp)); Text("Mais info", color = White)
                                }
                            }
                        }
                    }
                }
                item {
                    if (loadingCatalog) Text("Carregando catálogos do WOLF IPTV...", color = Muted, fontSize = 13.sp)
                    if (catalogError.isNotBlank()) Text(catalogError, color = Color(0xFFFF7B87), fontSize = 12.sp)
                    if (displayMovies.isNotEmpty()) MediaSection("🔥", "Lançamentos", displayMovies.sortedByDescending { it.year.filter(Char::isDigit).toIntOrNull() ?: 0 }.take(15), onClick = { chosenMedia = it })
                }
                item { CategorySection("🎬", "Filmes", listOf("Ação", "Terror", "Comédia", "Drama", "Romance", "Ficção Científica", "Documentário")) }
                item { if (displayMovies.isNotEmpty()) CategorySection("🎬", "Filmes", displayMovies.map { it.genre }.filter { it.isNotBlank() }.distinct().take(12)) }
                item { if (displaySeries.isNotEmpty()) MediaSection("▣", "Séries", displaySeries, onClick = { chosenMedia = it }) }
                item { if (displayAnimes.isNotEmpty()) MediaSection("✦", "Animes", displayAnimes, onClick = { chosenMedia = it }) }
                item { if (displayDrawings.isNotEmpty()) MediaSection("🎨", "Desenhos", displayDrawings, onClick = { chosenMedia = it }) }
            }
        }
    }

    chosenMedia?.let { media ->
        AlertDialog(
            onDismissRequest = { chosenMedia = null },
            title = { Text(media.title, color = White, fontWeight = FontWeight.Bold) },
            text = { Text("${media.year}  •  ${media.genre}  •  HD\n\n${media.synopsis.ifBlank { "Conteúdo carregado do catálogo WOLF IPTV." }}\n\n${if (media.video.isNotBlank()) "Link de vídeo disponível." else "Este item ainda não tem link de vídeo no JSON."}", color = Color(0xFFD5D8E2)) },
            confirmButton = { TextButton(onClick = { chosenMedia = null }) { Text("Fechar", color = Red) } },
            containerColor = Panel
        )
    }
}

private fun fetchMedia(url: String, possibleKeys: List<String>): List<Media> {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 15000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "WOLF-IPTV-Android")
    }
    return try {
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        parseMedia(body, possibleKeys)
    } finally {
        connection.disconnect()
    }
}

private fun parseMedia(jsonText: String, possibleKeys: List<String>): List<Media> {
    val root = jsonText.trim()
    val array: JSONArray = when {
        root.startsWith("[") -> JSONArray(root)
        root.startsWith("{") -> {
            val obj = JSONObject(root)
            var found: JSONArray? = null
            for (key in possibleKeys) {
                val value = obj.opt(key)
                if (value is JSONArray) { found = value; break }
            }
            found ?: findFirstArray(obj) ?: JSONArray()
        }
        else -> JSONArray()
    }
    val result = mutableListOf<Media>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val title = firstString(item, "titulo", "nome", "title", "name")
        if (title.isBlank()) continue
        val year = firstString(item, "ano", "year", "temporada").ifBlank { "HD" }
        val genre = firstString(item, "categoria", "genero", "gênero", "genre", "tipo").ifBlank { "Geral" }
        val cover = firstString(item, "capa", "poster", "imagem", "thumb", "thumbnail", "logo")
        val video = firstString(item, "video", "url", "link", "stream", "arquivo")
        val synopsis = firstString(item, "sinopse", "descricao", "descrição", "overview")
        val palette = when (i % 5) {
            0 -> listOf(Color(0xFF8E101D), Color(0xFF180B15))
            1 -> listOf(Color(0xFF087DAD), Color(0xFF13233C))
            2 -> listOf(Color(0xFF8B6A2D), Color(0xFF17150E))
            3 -> listOf(Color(0xFF9C1E6C), Color(0xFF170D27))
            else -> listOf(Color(0xFF68727C), Color(0xFF111820))
        }
        result.add(Media(title, year, genre, "▶", palette, cover, video, synopsis))
    }
    return result
}

private fun firstString(obj: JSONObject, vararg keys: String): String {
    for (key in keys) {
        val value = obj.opt(key)
        if (value != null && value != JSONObject.NULL && value !is JSONObject && value !is JSONArray) {
            val text = value.toString().trim()
            if (text.isNotBlank() && text != "null") return text
        }
    }
    return ""
}

private fun findFirstArray(obj: JSONObject): JSONArray? {
    val keys = obj.keys()
    while (keys.hasNext()) {
        val value = obj.opt(keys.next())
        if (value is JSONArray && value.length() > 0 && value.opt(0) is JSONObject) return value
    }
    return null
}

@Composable
private fun Tag(text: String) {
    Surface(color = Color(0xCC111722), shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Color(0xFF3D4658))) {
        Text(text, color = White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun MediaSection(icon: String, title: String, items: List<Media>, onClick: (Media) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(title, color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Ver todos  ›", color = Color(0xFFE2E5ED), fontSize = 12.sp, modifier = Modifier.clickable { })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEachIndexed { index, media ->
                MediaCard(media, index == 0, onClick = { onClick(media) })
            }
        }
    }
}

@Composable
private fun MediaCard(media: Media, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(218.dp).height(130.dp).shadow(if (highlighted) 7.dp else 2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(media.colors[0], media.colors[1])))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x22000000), Color(0xAA000000)))))
        Text(media.art, fontSize = 60.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 13.dp, top = 4.dp))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(media.title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MiniTag(media.year); MiniTag(media.genre); MiniTag("HD")
            }
        }
        if (highlighted) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Red).align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun MiniTag(text: String) {
    Text(text, color = Color(0xFFF0F2F8), fontSize = 10.sp,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xCC161B26))
            .padding(horizontal = 7.dp, vertical = 3.dp), maxLines = 1)
}

@Composable
private fun CategorySection(icon: String, title: String, categories: List<String>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 20.sp); Spacer(Modifier.width(8.dp))
            Text(title, color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("Ver todos  ›", color = Color(0xFFE2E5ED), fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEachIndexed { index, category ->
                Box(Modifier.width(145.dp).height(65.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(
                        listOf(Color(0xFF57121B), Color(0xFF20283C), Color(0xFF4A2C15), Color(0xFF351D36))[index % 4],
                        Color(0xFF10131B)
                    ))).clickable { }) {
                    Text(category, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = White, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                }
            }
        }
    }
}
