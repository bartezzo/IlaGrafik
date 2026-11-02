package pl.bartezzo.ilagrafik.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Przechowuje aktualny obrazek z grafikiem w prywatnej pamięci aplikacji.
 * Wybrany przez użytkownika plik jest kopiowany, więc grafik zostaje
 * nawet po usunięciu oryginału z dysku.
 */
class GrafikRepository(private val context: Context) {

    private val imageFile: File
        get() = File(context.filesDir, FILE_NAME)

    fun hasImage(): Boolean = imageFile.exists() && imageFile.length() > 0

    /** Kopiuje obrazek spod [uri] i zastępuje nim dotychczasowy grafik. */
    suspend fun replaceWith(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Nie można otworzyć wybranego pliku")

        // Upewniamy się, że to faktycznie obrazek, zanim nadpiszemy stary grafik.
        if (decode(tmp) == null) {
            tmp.delete()
            error("Wybrany plik nie jest obsługiwanym obrazkiem")
        }
        if (!tmp.renameTo(imageFile)) {
            tmp.copyTo(imageFile, overwrite = true)
            tmp.delete()
        }
    }

    /** Wczytuje aktualny grafik albo zwraca null, jeśli jeszcze go nie ma. */
    suspend fun load(): Bitmap? = withContext(Dispatchers.IO) {
        if (hasImage()) decode(imageFile) else null
    }

    private fun decode(file: File): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder sam uwzględnia obrót zdjęcia zapisany w EXIF.
            val source = ImageDecoder.createSource(file)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Bitmapa programowa: potrzebny jest odczyt pikseli przy szukaniu dzisiejszego dnia.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val size = info.size
                val scale = maxOf(size.width, size.height).toFloat() / MAX_DIMENSION
                if (scale > 1f) {
                    decoder.setTargetSize(
                        (size.width / scale).toInt(),
                        (size.height / scale).toInt()
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) {
                sample *= 2
            }
            BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }
    }.getOrNull()

    private companion object {
        const val FILE_NAME = "grafik_image"
        const val MAX_DIMENSION = 4096
    }
}
