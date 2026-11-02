package com.wolf.iptv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Transmissão do vídeo para a TV:
 *  - Google Cast (Chromecast, Google TV, Android TV, Sony, Philips, TCL, Xiaomi...)
 *  - DLNA/UPnP    (Samsung, LG e muitas outras)
 *  - Espelhar tela (abre o Smart View / Miracast do sistema)
 *
 * Uso (no player ou em qualquer Activity):
 *   Transmitir.mostrar(this, titulo, url, capa, posicaoAtualMs, aoVivo = false,
 *       aoEspelhar = { ... }) { pausarPlayerLocal() }
 *   aoEspelhar: chamado antes de abrir o espelhamento do sistema (para o player não pausar ao sair da tela).
 *   aoIniciar:  chamado quando a transmissão começou (para pausar o player local).
 */
object Transmitir {

    private class Alvo(
        val nome: String,
        val tipo: String,                         // "Cast" ou "DLNA"
        val rota: MediaRouter.RouteInfo? = null,  // Cast
        val controlUrl: String? = null            // DLNA
    )

    // ───────────────────────── UI ─────────────────────────

    fun mostrar(
        activity: Activity,
        titulo: String,
        url: String,
        capa: String = "",
        posicaoMs: Long = 0L,
        aoVivo: Boolean = false,
        aoEspelhar: () -> Unit = {},
        aoIniciar: () -> Unit = {}
    ) {
        if (url.isBlank()) {
            Toast.makeText(activity, "Vídeo indisponível", Toast.LENGTH_SHORT).show()
            return
        }

        val scope = CoroutineScope(Dispatchers.Main + Job())
        val cor = corTexto(activity)

        val lista = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8))
        }

        var alvosCast = listOf<Alvo>()
        val alvosDlna = mutableListOf<Alvo>()
        var dialog: AlertDialog? = null

        // Cast (pode não existir em aparelhos sem Google Play Services)
        val castCtx: CastContext? = try {
            CastContext.getSharedInstance(activity)
        } catch (_: Throwable) {
            null
        }
        val router = MediaRouter.getInstance(activity)
        val selector = castCtx?.mergedSelector

        fun escolher(a: Alvo) {
            if (a.rota != null && castCtx != null) {
                pendenteCast = { sessao ->
                    carregarCast(sessao, titulo, url, capa, posicaoMs, aoVivo)
                    Toast.makeText(activity, "Transmitindo para ${a.nome}", Toast.LENGTH_SHORT).show()
                    aoIniciar()
                }
                castCtx.sessionManager.removeSessionManagerListener(listenerCast, CastSession::class.java)
                castCtx.sessionManager.addSessionManagerListener(listenerCast, CastSession::class.java)

                val atual = castCtx.sessionManager.currentCastSession
                if (atual != null && atual.isConnected && router.selectedRoute.id == a.rota.id) {
                    executarPendente(atual)
                } else {
                    router.selectRoute(a.rota)
                }
                dialog?.dismiss()
            } else if (a.controlUrl != null) {
                Toast.makeText(activity, "Enviando para ${a.nome}...", Toast.LENGTH_SHORT).show()
                scope.launch {
                    try {
                        enviarDlna(a.controlUrl, titulo, url, aoVivo, posicaoMs)
                        Toast.makeText(activity, "Transmitindo para ${a.nome}", Toast.LENGTH_SHORT).show()
                        aoIniciar()
                        dialog?.dismiss()
                    } catch (e: Throwable) {
                        Toast.makeText(
                            activity,
                            "Não consegui transmitir: ${e.message ?: "erro"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        fun linha(a: Alvo) = TextView(activity).apply {
            text = (if (a.tipo == "Cast") "📺  " else "📡  ") + a.nome + "   (" + a.tipo + ")"
            textSize = 17f
            setTextColor(cor)
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            isFocusable = true
            isClickable = true
            setOnFocusChangeListener { v, f -> v.setBackgroundColor(if (f) 0x33888888 else 0) }
            setOnClickListener { escolher(a) }
        }

        fun render() {
            lista.removeAllViews()
            val todos = alvosCast + alvosDlna
            lista.addView(TextView(activity).apply {
                text = if (todos.isEmpty())
                    "Procurando TVs na rede Wi-Fi..."
                else
                    "Toque na TV para transmitir:"
                textSize = 14f
                setTextColor(cor)
                alpha = 0.7f
                setPadding(dp(activity, 16), dp(activity, 4), dp(activity, 16), dp(activity, 8))
            })
            todos.forEach { lista.addView(linha(it)) }
            if (todos.isEmpty() || alvosDlna.isEmpty()) {
                lista.addView(TextView(activity).apply {
                    text = "Celular e TV precisam estar no mesmo Wi-Fi. " +
                        "Se a TV não aparecer, use \"Espelhar tela\"."
                    textSize = 12f
                    setTextColor(cor)
                    alpha = 0.55f
                    setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 4))
                })
            }
        }

        fun atualizarCast() {
            alvosCast = if (selector == null) emptyList() else
                router.routes
                    .filter { !it.isDefault && it.matchesSelector(selector) }
                    .map { Alvo(it.name, "Cast", rota = it) }
            render()
        }

        val callbackRotas = object : MediaRouter.Callback() {
            override fun onRouteAdded(r: MediaRouter, route: MediaRouter.RouteInfo) = atualizarCast()
            override fun onRouteRemoved(r: MediaRouter, route: MediaRouter.RouteInfo) = atualizarCast()
            override fun onRouteChanged(r: MediaRouter, route: MediaRouter.RouteInfo) = atualizarCast()
        }

        if (selector != null) {
            router.addCallback(selector, callbackRotas, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        }
        atualizarCast()

        // DLNA
        scope.launch {
            descobrirDlna(activity) { achado ->
                if (alvosDlna.none { it.controlUrl == achado.controlUrl }) {
                    alvosDlna.add(achado)
                    render()
                }
            }
        }

        val dlg = AlertDialog.Builder(activity)
            .setTitle("Transmitir para a TV")
            .setView(ScrollView(activity).apply { addView(lista) })
            .setNeutralButton("Espelhar tela") { _, _ ->
                aoEspelhar()
                espelharTela(activity)
            }
            .setNegativeButton("Fechar", null)
            .create()
        dialog = dlg
        dlg.setOnDismissListener {
            scope.cancel()
            if (selector != null) {
                try {
                    router.removeCallback(callbackRotas)
                } catch (_: Throwable) {
                }
            }
        }
        dlg.show()
    }

    // ───────────────────────── Espelhar tela (sistema) ─────────────────────────

    private fun espelharTela(a: Activity) {
        try {
            a.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
        } catch (_: Throwable) {
            Toast.makeText(
                a,
                "Abra o Smart View / Espelhar tela no painel de notificações do celular",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ───────────────────────── Google Cast ─────────────────────────

    private var pendenteCast: ((CastSession) -> Unit)? = null

    private fun executarPendente(sessao: CastSession) {
        val p = pendenteCast
        pendenteCast = null
        p?.invoke(sessao)
    }

    private val listenerCast = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(s: CastSession, sessionId: String) = executarPendente(s)
        override fun onSessionResumed(s: CastSession, wasSuspended: Boolean) = executarPendente(s)
        override fun onSessionStartFailed(s: CastSession, error: Int) { pendenteCast = null }
        override fun onSessionStarting(s: CastSession) {}
        override fun onSessionEnding(s: CastSession) {}
        override fun onSessionEnded(s: CastSession, error: Int) {}
        override fun onSessionResuming(s: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(s: CastSession, error: Int) {}
        override fun onSessionSuspended(s: CastSession, reason: Int) {}
    }

    private fun carregarCast(
        sessao: CastSession,
        titulo: String,
        url: String,
        capa: String,
        posicaoMs: Long,
        aoVivo: Boolean
    ) {
        val cliente = sessao.remoteMediaClient ?: return
        val meta = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, titulo)
            if (capa.startsWith("http")) addImage(WebImage(Uri.parse(capa)))
        }
        val info = MediaInfo.Builder(url)
            .setStreamType(if (aoVivo) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mime(url, aoVivo))
            .setMetadata(meta)
            .build()
        val req = MediaLoadRequestData.Builder()
            .setMediaInfo(info)
            .setAutoplay(true)
            .setCurrentTime(if (aoVivo) 0L else posicaoMs)
            .build()
        cliente.load(req)
    }

    // ───────────────────────── DLNA / UPnP ─────────────────────────

    /** Procura TVs/receptores DLNA na rede por ~6s (SSDP) e chama [aoAchar] na thread principal. */
    private suspend fun descobrirDlna(ctx: Context, aoAchar: (Alvo) -> Unit): Unit =
        withContext(Dispatchers.IO) {
            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("wolf-dlna").apply {
                setReferenceCounted(false)
                acquire()
            }
            var socket: DatagramSocket? = null
            try {
                val sock = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(0))
                    soTimeout = 1000
                }
                socket = sock
                val msg = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 2\r\n" +
                        "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n\r\n"
                    ).toByteArray()
                val destino = InetSocketAddress(InetAddress.getByName("239.255.255.250"), 1900)
                repeat(2) { sock.send(DatagramPacket(msg, msg.size, destino)) }

                val vistos = HashSet<String>()
                val fim = System.currentTimeMillis() + 6000
                val buf = ByteArray(4096)
                while (isActive && System.currentTimeMillis() < fim) {
                    try {
                        val p = DatagramPacket(buf, buf.size)
                        sock.receive(p)
                        val resp = String(p.data, 0, p.length)
                        val loc = Regex("(?im)^location:\\s*(\\S+)").find(resp)?.groupValues?.get(1)
                            ?: continue
                        if (!vistos.add(loc)) continue
                        val alvo = lerDispositivo(loc)
                        if (alvo != null) withContext(Dispatchers.Main) { aoAchar(alvo) }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    socket?.close()
                } catch (_: Throwable) {
                }
                if (lock.isHeld) lock.release()
            }
        }

    private fun lerDispositivo(location: String): Alvo? {
        return try {
            val xml = httpGet(location)
            val nome = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.DOT_MATCHES_ALL)
                .find(xml)?.groupValues?.get(1)?.trim()
                ?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")
                ?: "TV"
            val servico = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL)
                .findAll(xml)
                .map { it.groupValues[1] }
                .firstOrNull { it.contains("service:AVTransport") }
                ?: return null
            val controle = Regex("<controlURL>(.*?)</controlURL>", RegexOption.DOT_MATCHES_ALL)
                .find(servico)?.groupValues?.get(1)?.trim()
                ?: return null
            val absoluto = URL(URL(location), controle).toString()
            Alvo(nome, "DLNA", controlUrl = absoluto)
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun enviarDlna(
        controlUrl: String,
        titulo: String,
        url: String,
        aoVivo: Boolean,
        posicaoMs: Long
    ): Unit = withContext(Dispatchers.IO) {
        val tipo = mime(url, aoVivo)
        val didl =
            "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"0\" restricted=\"1\">" +
                "<dc:title>${xml(titulo)}</dc:title>" +
                "<upnp:class>object.item.videoItem</upnp:class>" +
                "<res protocolInfo=\"http-get:*:$tipo:*\">${xml(url)}</res>" +
                "</item></DIDL-Lite>"

        runCatching { soap(controlUrl, "Stop", "<InstanceID>0</InstanceID>") }
        soap(
            controlUrl, "SetAVTransportURI",
            "<InstanceID>0</InstanceID><CurrentURI>${xml(url)}</CurrentURI>" +
                "<CurrentURIMetaData>${xml(didl)}</CurrentURIMetaData>"
        )
        soap(controlUrl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")

        if (!aoVivo && posicaoMs > 5000) {
            delay(2000)
            runCatching {
                soap(
                    controlUrl, "Seek",
                    "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>${hms(posicaoMs)}</Target>"
                )
            }
        }
    }

    private fun soap(controlUrl: String, acao: String, corpo: String) {
        val envelope =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body>" +
                "<u:$acao xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\">$corpo</u:$acao>" +
                "</s:Body></s:Envelope>"
        val conn = URL(controlUrl).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 6000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            conn.setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#$acao\"")
            conn.outputStream.use { it.write(envelope.toByteArray()) }
            val codigo = conn.responseCode
            if (codigo !in 200..299) throw Exception("A TV recusou ($acao, HTTP $codigo)")
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ───────────────────────── Utilitários ─────────────────────────

    private fun mime(url: String, aoVivo: Boolean): String {
        val u = url.substringBefore('?').lowercase()
        return when {
            u.endsWith(".m3u8") -> "application/x-mpegURL"
            u.endsWith(".mkv") -> "video/x-matroska"
            u.endsWith(".webm") -> "video/webm"
            u.endsWith(".avi") -> "video/x-msvideo"
            u.endsWith(".ts") -> "video/mp2t"
            u.endsWith(".mp4") -> "video/mp4"
            else -> if (aoVivo) "video/mp2t" else "video/mp4"
        }
    }

    private fun xml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun hms(ms: Long): String {
        val t = ms / 1000
        return String.format("%02d:%02d:%02d", t / 3600, (t % 3600) / 60, t % 60)
    }

    private fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    private fun corTexto(c: Context): Int {
        val tv = TypedValue()
        return if (c.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
            if (tv.resourceId != 0) ContextCompat.getColor(c, tv.resourceId) else tv.data
        } else {
            0xFF888888.toInt()
        }
    }
}
