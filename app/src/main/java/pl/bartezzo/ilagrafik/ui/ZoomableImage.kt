package pl.bartezzo.ilagrafik.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.bartezzo.ilagrafik.data.Tile

/** Stan przybliżenia obrazka; pozwala też przybliżyć widok na wybrany fragment. */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    internal var viewSize by mutableStateOf(IntSize.Zero)
    internal var imageSize by mutableStateOf(IntSize.Zero)

    internal fun applyGesture(pan: Offset, zoom: Float) {
        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = clamp(offset + pan, scale)
    }

    suspend fun reset() = animateTo(1f, Offset.Zero)

    suspend fun zoomAroundCenter(target: Float) = animateTo(target, Offset.Zero)

    /** Przybliża i przesuwa widok tak, żeby [tile] (w pikselach obrazka) wypełnił ekran. */
    suspend fun zoomTo(tile: Tile) {
        val view = snapshotFlow { viewSize }.first { it.width > 0 && it.height > 0 }
        val image = imageSize
        if (image.width == 0 || image.height == 0 || tile.width <= 0 || tile.height <= 0) return

        val w = view.width.toFloat()
        val h = view.height.toFloat()
        // Skala, z jaką obrazek jest wyświetlany w trybie "dopasuj".
        val fit = minOf(w / image.width, h / image.height)
        val left0 = (w - image.width * fit) / 2
        val top0 = (h - image.height * fit) / 2

        val targetScale = (minOf(w / (tile.width * fit), h / (tile.height * fit)) * FOCUS_MARGIN)
            .coerceIn(MIN_SCALE, MAX_SCALE)
        val centerX = left0 + (tile.left + tile.width / 2f) * fit
        val centerY = top0 + (tile.top + tile.height / 2f) * fit
        val targetOffset = Offset(-(centerX - w / 2) * targetScale, -(centerY - h / 2) * targetScale)

        animateTo(targetScale, clamp(targetOffset, targetScale))
    }

    private suspend fun animateTo(targetScale: Float, targetOffset: Offset) {
        val startScale = scale
        val startOffset = offset
        animate(0f, 1f, animationSpec = tween(durationMillis = 450)) { f, _ ->
            scale = startScale + (targetScale - startScale) * f
            offset = startOffset + (targetOffset - startOffset) * f
        }
    }

    private fun clamp(value: Offset, forScale: Float): Offset {
        if (forScale <= 1f) return Offset.Zero
        val maxX = viewSize.width * (forScale - 1) / 2
        val maxY = viewSize.height * (forScale - 1) / 2
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 6f
        const val FOCUS_MARGIN = 0.94f
    }
}

/**
 * Obrazek, który można przybliżać dwoma palcami i przesuwać.
 * Dwukrotne stuknięcie przełącza między widokiem całości a powiększeniem.
 */
@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    state: ZoomState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    SideEffect { state.imageSize = IntSize(bitmap.width, bitmap.height) }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.viewSize = it }
            .pointerInput(state) {
                detectTapGestures(
                    onDoubleTap = {
                        scope.launch {
                            if (state.scale > 1f) state.reset() else state.zoomAroundCenter(2.5f)
                        }
                    }
                )
            }
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    state.applyGesture(pan, zoom)
                }
            }
            .graphicsLayer {
                scaleX = state.scale
                scaleY = state.scale
                translationX = state.offset.x
                translationY = state.offset.y
            }
    )
}
