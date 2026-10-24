package app.marlboroadvance.mpvex.domain.lyrics

data class LrcLine(val timeMs: Long, val text: String) {
    val timestampMs: Long get() = timeMs
}

object LrcParser {
    // [mm:ss.xx] / [mm:ss.xxx] / [mm:ss]
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(lrc: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        for (raw in lrc.lineSequence()) {
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) continue // Skip [ar:..], [ti:..] metadata
            val text = raw.substring(tags.last().range.last + 1).trim()
            for (m in tags) {
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val millis = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                lines += LrcLine(minutes * 60_000 + seconds * 1_000 + millis, text)
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    /** Returns active line index at positionMs using binary search (-1 if before first line). */
    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                ans = mid
                lo = mid + 1
            } else hi = mid - 1
        }
        return ans
    }
}
