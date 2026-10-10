package com.wolf.iptv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ProgressoItem(
    val url: String,
    val titulo: String,
    val capa: String,
    val posicao: Long,
    val duracao: Long,
    val atualizado: Long
) {
    fun resumo(): String =
        if (duracao > 0) "Parou em ${WolfProgress.formatar(posicao)}"
        else "Em andamento"
}

object WolfProgress {

    private const val PREFS = "wolf_progress"
    private const val KEY = "itens"

    private fun carregar(ctx: Context): MutableList<ProgressoItem> {
        val txt = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val lista = mutableListOf<ProgressoItem>()
        try {
            val arr = JSONArray(txt)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lista.add(
                    ProgressoItem(
                        o.getString("url"),
                        o.optString("titulo"),
                        o.optString("capa"),
                        o.optLong("posicao"),
                        o.optLong("duracao"),
                        o.optLong("atualizado")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return lista
    }

    private fun gravar(ctx: Context, lista: List<ProgressoItem>) {
        val arr = JSONArray()
        lista.forEach {
            arr.put(
                JSONObject()
                    .put("url", it.url)
                    .put("titulo", it.titulo)
                    .put("capa", it.capa)
                    .put("posicao", it.posicao)
                    .put("duracao", it.duracao)
                    .put("atualizado", it.atualizado)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun registrar(ctx: Context, url: String, titulo: String, capa: String) {
        val lista = carregar(ctx)
        val atual = lista.find { it.url == url }
        lista.removeAll { it.url == url }
        lista.add(
            ProgressoItem(
                url, titulo, capa,
                atual?.posicao ?: 0L,
                atual?.duracao ?: 0L,
                System.currentTimeMillis()
            )
        )
        gravar(ctx, lista)
    }

    fun salvar(ctx: Context, url: String, posicao: Long, duracao: Long) {
        val lista = carregar(ctx)
        val atual = lista.find { it.url == url } ?: return
        lista.removeAll { it.url == url }
        lista.add(
            atual.copy(
                posicao = posicao,
                duracao = duracao,
                atualizado = System.currentTimeMillis()
            )
        )
        gravar(ctx, lista)
    }

    fun posicao(ctx: Context, url: String): Long =
        carregar(ctx).find { it.url == url }?.posicao ?: 0L

    fun apagar(ctx: Context, url: String) {
        val lista = carregar(ctx)
        lista.removeAll { it.url == url }
        gravar(ctx, lista)
    }

    fun emAndamento(ctx: Context): List<ProgressoItem> =
        carregar(ctx)
            .filter { it.posicao > 5000L }
            .filter { it.duracao <= 0L || it.posicao < it.duracao - 10000L }
            .sortedByDescending { it.atualizado }

    fun formatar(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val seg = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, seg)
        else String.format("%02d:%02d", m, seg)
    }
}
