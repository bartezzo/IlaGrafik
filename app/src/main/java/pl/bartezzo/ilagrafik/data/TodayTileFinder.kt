package pl.bartezzo.ilagrafik.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Odczytuje tekst z grafiku (ML Kit, bez internetu) i wyznacza kafelek danego dnia.
 * Zwrócony prostokąt jest w pikselach przekazanej bitmapy.
 */
class TodayTileFinder {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun find(bitmap: Bitmap, day: DayOfWeek): Tile? = withContext(Dispatchers.Default) {
        val scale = minOf(1f, MAX_ANALYSIS_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val analysis = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        try {
            val text = recognize(analysis)
            val boxes = text.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }
                .mapNotNull { element ->
                    element.boundingBox?.let {
                        TextBox(element.text, it.left, it.top, it.right, it.bottom)
                    }
                }
            val lines = DayTileLocator.findHorizontalLines(analysis.width, analysis.height) { y, row ->
                analysis.getPixels(row, 0, analysis.width, 0, y, analysis.width, 1)
            }
            DayTileLocator.findTile(boxes, lines, analysis.width, analysis.height, day)
                ?.let { tile ->
                    Tile(
                        left = (tile.left / scale).toInt(),
                        top = (tile.top / scale).toInt(),
                        right = (tile.right / scale).toInt().coerceAtMost(bitmap.width),
                        bottom = (tile.bottom / scale).toInt().coerceAtMost(bitmap.height)
                    )
                }
        } finally {
            if (analysis !== bitmap) analysis.recycle()
        }
    }

    private suspend fun recognize(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private companion object {
        const val MAX_ANALYSIS_SIZE = 2048
    }
}
