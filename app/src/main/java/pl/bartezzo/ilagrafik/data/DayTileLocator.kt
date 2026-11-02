package pl.bartezzo.ilagrafik.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Słowo rozpoznane na obrazku wraz z jego położeniem (w pikselach). */
data class TextBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Pozioma linia tabeli wykryta na obrazku. */
data class HLine(val top: Int, val bottom: Int, val left: Int, val right: Int)

/** Prostokąt kafelka jednego dnia (w pikselach obrazka). */
data class Tile(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Logika wyszukiwania kafelka danego dnia w grafiku, niezależna od Androida.
 *
 * Założenie o układzie: każdy dzień zaczyna się od swojej nazwy (np. "Poniedziałek"),
 * a kafelki są oddzielone poziomymi liniami tabeli. Kafelek dnia to obszar od linii
 * nad nazwą dnia do pierwszej linii pod nią.
 */
object DayTileLocator {

    private val DAY_NAMES = listOf(
        "poniedzialek", "wtorek", "sroda", "czwartek", "piatek", "sobota", "niedziela"
    )

    /** Małe litery, bez polskich znaków i bez znaków innych niż litery. */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT).replace('ł', 'l')
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return decomposed.filter {
            it.isLetter() && Character.getType(it) != Character.NON_SPACING_MARK.toInt()
        }
    }

    /** Rozpoznaje nazwę dnia tygodnia, tolerując drobne błędy rozpoznawania tekstu. */
    fun dayOf(text: String): DayOfWeek? {
        val word = normalize(text)
        if (word.length < 4) return null
        val index = DAY_NAMES.indexOfFirst { name ->
            levenshtein(word, name) <= if (name.length > 6) 2 else 1
        }
        return if (index >= 0) DayOfWeek.of(index + 1) else null
    }

    /**
     * Wykrywa poziome linie: wiersze pikseli, w których ciągły ciemny odcinek
     * zajmuje co najmniej [minFraction] szerokości obrazka.
     *
     * @param readRow wypełnia tablicę kolorami ARGB pikseli z wiersza y.
     */
    fun findHorizontalLines(
        width: Int,
        height: Int,
        minFraction: Float = 0.4f,
        darkThreshold: Int = 110,
        readRow: (y: Int, pixels: IntArray) -> Unit,
    ): List<HLine> {
        val minRun = (width * minFraction).toInt()
        val row = IntArray(width)
        val lines = mutableListOf<HLine>()
        var current: HLine? = null

        for (y in 0 until height) {
            readRow(y, row)
            var bestLen = 0
            var bestStart = 0
            var runStart = 0
            var runLen = 0
            for (x in 0 until width) {
                val c = row[x]
                val luma = (299 * ((c shr 16) and 0xFF) +
                    587 * ((c shr 8) and 0xFF) +
                    114 * (c and 0xFF)) / 1000
                if (luma < darkThreshold) {
                    if (runLen == 0) runStart = x
                    runLen++
                    if (runLen > bestLen) {
                        bestLen = runLen
                        bestStart = runStart
                    }
                } else {
                    runLen = 0
                }
            }

            if (bestLen >= minRun) {
                val left = bestStart
                val right = bestStart + bestLen - 1
                val prev = current
                current = if (prev != null && prev.bottom == y - 1) {
                    prev.copy(bottom = y, left = min(prev.left, left), right = max(prev.right, right))
                } else {
                    prev?.let { lines.add(it) }
                    HLine(y, y, left, right)
                }
            }
        }
        current?.let { lines.add(it) }
        return lines
    }

    /** Zwraca kafelek dnia [day] albo null, jeśli tego dnia nie ma na obrazku. */
    fun findTile(
        boxes: List<TextBox>,
        lines: List<HLine>,
        width: Int,
        height: Int,
        day: DayOfWeek,
    ): Tile? {
        val labels = boxes
            .mapNotNull { box -> dayOf(box.text)?.let { it to box } }
            .sortedBy { it.second.top }
        val label = labels.firstOrNull { it.first == day }?.second ?: return null
        val labelHeight = label.bottom - label.top
        val sortedLines = lines.sortedBy { it.top }

        val above = sortedLines.lastOrNull { it.bottom <= label.top }
        val top = above?.bottom ?: max(0, label.top - labelHeight)

        val next = labels.firstOrNull { it.second.top > label.bottom }?.second
        val limit = next?.top ?: height
        val below = sortedLines.firstOrNull { it.top > label.bottom && it.top <= limit }
        val bottom = below?.top ?: limit

        val left = above?.left ?: max(0, label.left - labelHeight)
        val right = above?.right ?: width
        if (right <= left || bottom <= top) return null
        return Tile(left, top, right, bottom)
    }

    private fun levenshtein(a: String, b: String): Int {
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prevDiag = d[0]
            d[0] = i
            for (j in 1..b.length) {
                val temp = d[j]
                d[j] = minOf(
                    d[j] + 1,
                    d[j - 1] + 1,
                    prevDiag + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                prevDiag = temp
            }
        }
        return d[b.length]
    }
}
