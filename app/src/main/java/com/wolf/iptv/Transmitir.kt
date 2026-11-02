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
