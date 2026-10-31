package com.wolf.iptv

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.util.Locale

/**
 * Baixa vídeos para o celular usando o DownloadManager do Android.
 *
 * - Roda em segundo plano (pode fechar o app), retoma se a internet cair
 *   e mostra a notificação de progresso sozinho.
 * - O vídeo é salvo em  Movies/Wolf IPTV  (aparece na galeria no álbum "Wolf IPTV").
 *   Se o Android não deixar, salva em  Download/Wolf IPTV.
 *   Se também não deixar (Android 9 ou menor sem permissão), salva na pasta do app.
 * - Não precisa mexer no AndroidManifest (só do INTERNET, que o app já tem).
 */
object WolfDownloads {

    enum class Estado { NENHUM, BAIXANDO, PRONTO, FALHOU }

    enum class Resultado {
        INICIADO,
        INICIADO_NO_APP,   // salvo só na pasta do app (não aparece na galeria)
        JA_BAIXADO,
        JA_BAIXANDO,
        SEM_LINK,
        FORMATO_INVALIDO,  // transmissão ao vivo (.m3u8 / .ts), não é um arquivo
        ERRO
    }

    private const val PREFS = "wolf_downloads"
    private const val PASTA = "Wolf IPTV"
    private val EXTENSOES_VIDEO = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp")
    private val EXTENSOES_AO_VIVO = setOf("m3u8", "ts", "mpd")

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun gerenciador(ctx: Context) =
        ctx.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private fun chave(url: String) = "dl_$url"

    private fun extensaoDoLink(url: String): String {
        val ultimo = try { Uri.parse(url).lastPathSegment ?: "" } catch (_: Exception) { "" }
        return ultimo.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }

    private fun mimeDe(ext: String): String = when (ext) {
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "3gp" -> "video/3gpp"
        else -> "video/mp4"
    }

    /** Tira caracteres que o Android não aceita em nome de arquivo. */
    fun nomeSeguro(texto: String): String {
        val limpo = texto
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
        return limpo.take(90).ifBlank { "video" }
    }

    /** Situação do download deste link (baixando, pronto, falhou ou nunca baixado). */
    fun estado(ctx: Context, url: String): Estado {
        if (url.isBlank()) return Estado.NENHUM
        val id = prefs(ctx).getLong(chave(url), -1L)
        if (id < 0L) return Estado.NENHUM
        return try {
            val cursor = gerenciador(ctx).query(DownloadManager.Query().setFilterById(id))
            if (cursor == null) {
                Estado.NENHUM
            } else {
                cursor.use { c ->
                    if (!c.moveToFirst()) {
                        // Foi apagado pelo app de Downloads do sistema
                        prefs(ctx).edit().remove(chave(url)).apply()
                        Estado.NENHUM
                    } else {
                        when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_SUCCESSFUL -> Estado.PRONTO
                            DownloadManager.STATUS_FAILED -> Estado.FALHOU
                            else -> Estado.BAIXANDO // pendente, rodando ou pausado (esperando rede)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            Estado.NENHUM
        }
    }

    /**
     * Começa o download.
     * [titulo] aparece na notificação; [nomeBase] vira o nome do arquivo (sem extensão).
     */
    fun baixar(ctx: Context, url: String, titulo: String, nomeBase: String): Resultado {
        if (url.isBlank()) return Resultado.SEM_LINK

        when (estado(ctx, url)) {
            Estado.PRONTO -> return Resultado.JA_BAIXADO
            Estado.BAIXANDO -> return Resultado.JA_BAIXANDO
            Estado.FALHOU -> remover(ctx, url)
            Estado.NENHUM -> {}
        }

        val ext = extensaoDoLink(url)
        if (ext in EXTENSOES_AO_VIVO) return Resultado.FORMATO_INVALIDO
        val extFinal = if (ext in EXTENSOES_VIDEO) ext else "mp4"
        val arquivo = "${nomeSeguro(nomeBase)}.$extFinal"
        val dm = gerenciador(ctx)

        fun novaRequisicao(): DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(titulo)
                .setDescription(PASTA)
                .setMimeType(mimeDe(extFinal))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        var id = -1L
        var naGaleria = true

        // 1) Movies/Wolf IPTV  (galeria)
        try {
            id = dm.enqueue(
                novaRequisicao().setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES, "$PASTA/$arquivo"
                )
            )
        } catch (_: Exception) {
        }

        // 2) Download/Wolf IPTV  (a galeria também costuma mostrar)
        if (id < 0L) {
            try {
                id = dm.enqueue(
                    novaRequisicao().setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, "$PASTA/$arquivo"
                    )
                )
            } catch (_: Exception) {
            }
        }

        // 3) Pasta do próprio app (não aparece na galeria)
        if (id < 0L) {
            try {
                id = dm.enqueue(
                    novaRequisicao().setDestinationInExternalFilesDir(
                        ctx.applicationContext, Environment.DIRECTORY_MOVIES, "$PASTA/$arquivo"
                    )
                )
                naGaleria = false
            } catch (_: Exception) {
            }
        }

        if (id < 0L) return Resultado.ERRO
        prefs(ctx).edit().putLong(chave(url), id).apply()
        return if (naGaleria) Resultado.INICIADO else Resultado.INICIADO_NO_APP
    }

    /** Cancela o download em andamento ou apaga o vídeo já baixado. */
    fun remover(ctx: Context, url: String) {
        val id = prefs(ctx).getLong(chave(url), -1L)
        if (id >= 0L) {
            try { gerenciador(ctx).remove(id) } catch (_: Exception) {}
        }
        prefs(ctx).edit().remove(chave(url)).apply()
    }
}
