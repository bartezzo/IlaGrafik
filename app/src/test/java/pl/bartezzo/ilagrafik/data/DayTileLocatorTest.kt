package pl.bartezzo.ilagrafik.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class DayTileLocatorTest {

    @Test
    fun `rozpoznaje nazwy dni z polskimi znakami i bez nich`() {
        assertEquals(DayOfWeek.MONDAY, DayTileLocator.dayOf("Poniedziałek"))
        assertEquals(DayOfWeek.MONDAY, DayTileLocator.dayOf("PONIEDZIALEK"))
        assertEquals(DayOfWeek.TUESDAY, DayTileLocator.dayOf("Wtorek"))
        assertEquals(DayOfWeek.WEDNESDAY, DayTileLocator.dayOf("Środa"))
        assertEquals(DayOfWeek.WEDNESDAY, DayTileLocator.dayOf("Sroda:"))
        assertEquals(DayOfWeek.THURSDAY, DayTileLocator.dayOf("Czwartek"))
        assertEquals(DayOfWeek.FRIDAY, DayTileLocator.dayOf("Piątek"))
        assertEquals(DayOfWeek.SATURDAY, DayTileLocator.dayOf("Sobota"))
        assertEquals(DayOfWeek.SUNDAY, DayTileLocator.dayOf("Niedziela"))
    }

    @Test
    fun `toleruje drobne bledy rozpoznawania tekstu`() {
        assertEquals(DayOfWeek.MONDAY, DayTileLocator.dayOf("Poniedzialck"))
        assertEquals(DayOfWeek.FRIDAY, DayTileLocator.dayOf("Piqtek"))
        assertEquals(DayOfWeek.THURSDAY, DayTileLocator.dayOf("Czwartck"))
    }

    @Test
    fun `nie myli zwyklych slow z dniami`() {
        listOf("Ola", "Anna", "basen", "tenis", "8:00", "odbiór", "Grafik", "(przedstawienie)")
            .forEach { assertNull(it, DayTileLocator.dayOf(it)) }
    }

    @Test
    fun `wykrywa poziome linie tabeli`() {
        val width = 100
        val height = 60
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        // Linie: y=10 (pełna), y=30..31 (gruba, od x=20), y=50 (za krótka - podkreślenie).
        val lines = DayTileLocator.findHorizontalLines(width, height) { y, row ->
            for (x in 0 until width) {
                row[x] = when {
                    y == 10 -> black
                    (y == 30 || y == 31) && x >= 20 -> black
                    y == 50 && x < 30 -> black
                    else -> white
                }
            }
        }
        assertEquals(listOf(HLine(10, 10, 0, 99), HLine(30, 31, 20, 99)), lines)
    }

    /** Układ jak w typowym grafiku: jedna kolumna, dzień w każdym wierszu tabeli. */
    private val boxes = listOf(
        TextBox("Grafik", 321, 240, 462, 280),
        TextBox("02-06.11", 478, 242, 723, 280),
        TextBox("Poniedziałek", 141, 360, 337, 388),
        TextBox("8:00", 139, 470, 206, 496),
        TextBox("Anna", 344, 469, 394, 496),
        TextBox("Wtorek", 139, 687, 256, 714),
        TextBox("9:00", 139, 906, 206, 931),
        TextBox("Środa", 139, 1064, 227, 1097),
        TextBox("(tenis)", 456, 1394, 556, 1429),
        TextBox("Czwartek", 139, 1451, 284, 1478),
        TextBox("Piątek", 141, 1777, 235, 1812),
        TextBox("9:30", 139, 2040, 206, 2066),
    )
    private val lines = listOf(
        HLine(0, 0, 0, 722),
        HLine(344, 344, 119, 722),
        HLine(671, 671, 119, 722),
        HLine(1053, 1054, 119, 722),
        HLine(1434, 1435, 119, 722),
        HLine(1761, 1761, 119, 722),
        HLine(2089, 2089, 119, 722),
        HLine(2112, 2112, 0, 722),
    )

    private fun tile(day: DayOfWeek) = DayTileLocator.findTile(boxes, lines, 723, 2113, day)

    @Test
    fun `wyznacza kafelek kazdego dnia miedzy liniami tabeli`() {
        assertEquals(Tile(119, 344, 722, 671), tile(DayOfWeek.MONDAY))
        assertEquals(Tile(119, 671, 722, 1053), tile(DayOfWeek.TUESDAY))
        assertEquals(Tile(119, 1054, 722, 1434), tile(DayOfWeek.WEDNESDAY))
        assertEquals(Tile(119, 1435, 722, 1761), tile(DayOfWeek.THURSDAY))
        assertEquals(Tile(119, 1761, 722, 2089), tile(DayOfWeek.FRIDAY))
    }

    @Test
    fun `zwraca null dla dnia, ktorego nie ma w grafiku`() {
        assertNull(tile(DayOfWeek.SATURDAY))
        assertNull(tile(DayOfWeek.SUNDAY))
    }

    @Test
    fun `bez linii tabeli kafelek konczy sie na nastepnym dniu`() {
        val tile = DayTileLocator.findTile(boxes, emptyList(), 723, 2113, DayOfWeek.TUESDAY)
        assertEquals(Tile(112, 660, 723, 1064), tile)
    }
}
