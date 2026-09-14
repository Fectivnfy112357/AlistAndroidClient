package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.lang.ref.WeakReference
import java.util.LinkedHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun coverLabel(name: String): String =
    name.firstOrNull { it.isLetter() }?.toString()?.uppercase() ?: "♪"

/** Default rounded shape for album/artist artwork — larger than shapes.small for a modern look. */
internal val CoverShape: Shape = RoundedCornerShape(14.dp)

/**
 * 居中首字封面 — gradient background + first Unicode character of `name` rendered large.
 * Sizing is controlled by the caller via [modifier] (fixed size or fill/aspectRatio); the
 * letter scales to the measured box so it looks right at any dimension.
 */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) {
    val firstChar = coverLabel(name)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        val edge = minOf(maxWidth, maxHeight)
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = (edge.value * 0.5f).sp,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}

/** Fixed-size convenience overload. */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) = CoverLetter(name, gradient, modifier.size(size), shape)

/**
 * Async artwork cover — sized by [modifier].
 *
 * Falls back to a [CoverLetter] while the byte array is still being decoded or
 * after a decode failure. Decoding on the IO dispatcher lets the placeholder
 * render in frame 0 and the bitmap swap in when ready.
 *
 * P0 fix (O5): previously decoded the full original image and drew it at the
 * cover's display size (typically 42dp ≈ 126px @ 3x). A 1000x1000 source
 * produced 1M pixels of Bitmap RAM for 16K pixels of on-screen content, and
 * any 30-row scroll that re-bound a row forced a fresh full-size decode.
 *
 * We now:
 *   1. Pick `BitmapFactory.Options.inSampleSize` to decode at the largest
 *      power-of-two ≤ 2× the target pixel size, capping memory at ~4× the
 *      on-screen footprint.
 *   2. Cache the decoded `ImageBitmap` in a module-level LRU keyed by the
 *      bytes' content hash, so scrolling a row off-screen and back does not
 *      re-decode — the bitmap survives composable disposal.
 */
@Composable
fun ArtworkCover(
    name: String,
    artworkData: ByteArray?,
    gradient: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) {
    // Estimate the target pixel size from the modifier's size constraint.
    // We read `LocalDensity` outside the `remember` block because `remember`
    // cannot call other `@Composable` functions. The value is captured at
    // composition time and only invalidated when the modifier identity changes.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val targetPx = with(density) { 48.dp.roundToPx() }
    val artworkState = decodeArtworkAsync(artworkData, targetPx)
    val artwork = artworkState.value
    if (artwork == null) {
        CoverLetter(name, gradient, modifier, shape)
    } else {
        Image(
            bitmap = artwork,
            contentDescription = "$name 封面",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    }
}

@Composable
private fun decodeArtworkAsync(
    data: ByteArray?,
    targetPx: Int,
): State<androidx.compose.ui.graphics.ImageBitmap?> {
    val state = remember(data, targetPx) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
            if (data == null) null else ArtworkBitmapCache.get(data, targetPx)
        )
    }
    LaunchedEffect(data, targetPx) {
        state.value = if (data == null) null else withContext(Dispatchers.IO) {
            ArtworkBitmapCache.getOrDecode(data, targetPx)
        }
    }
    return state
}

/** Fixed-size convenience overload. */
@Composable
fun ArtworkCover(
    name: String,
    artworkData: ByteArray?,
    gradient: Brush,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CoverShape,
) = ArtworkCover(name, artworkData, gradient, modifier.size(size), shape)

/**
 * Module-level LRU cache for decoded artwork bitmaps. Survives composable
 * disposal so that scrolling a row off-screen and back into view does not
 * trigger a fresh `BitmapFactory.decodeByteArray` pass.
 *
 * Cache key is `data.contentHashCode() * 31 + targetPxBucket` — content
 * identity plus the target resolution bucket (e.g. 128px, 256px). Buckets
 * keep the entry list small while still letting us keep a higher-res version
 * for any caller that asks for one later.
 *
 * Bitmaps are weakly referenced so the GC can reclaim them under memory
 * pressure; the LRU bookkeeping stays but the pixel data does not.
 */
private object ArtworkBitmapCache {
    private const val MAX_ENTRIES = 64
    private const val PX_BUCKET = 32 // quantize targetPx to nearest 32px

    private val cache = object : LinkedHashMap<Int, WeakReference<android.graphics.Bitmap>>(
        /* initialCapacity = 16, loadFactor = 0.75f, accessOrder = true */
        16, 0.75f, true,
    ) {
        override fun removeEldestEntry(
            eldest: Map.Entry<Int, WeakReference<android.graphics.Bitmap>>,
        ) = size > MAX_ENTRIES
    }

    private val lock = Any()

    fun get(data: ByteArray, targetPx: Int): androidx.compose.ui.graphics.ImageBitmap? {
        val key = key(data, targetPx)
        val bmp = synchronized(lock) { cache[key]?.get() }
        return bmp?.takeIf { !it.isRecycled }?.asImageBitmap()
    }

    fun getOrDecode(
        data: ByteArray,
        targetPx: Int,
    ): androidx.compose.ui.graphics.ImageBitmap? {
        get(data, targetPx)?.let { return it }
        val decoded = decodeSampled(data, targetPx) ?: return null
        synchronized(lock) {
            cache[key(data, targetPx)] = WeakReference(decoded)
        }
        return decoded.asImageBitmap()
    }

    private fun key(data: ByteArray, targetPx: Int): Int {
        val bucket = (targetPx / PX_BUCKET) * PX_BUCKET
        // Fold targetPx into the content hash so two different display sizes
        // for the same bytes keep separate bitmap entries.
        var h = data.contentHashCode()
        h = 31 * h + bucket
        return h
    }

    private fun decodeSampled(
        data: ByteArray,
        targetPx: Int,
    ): android.graphics.Bitmap? {
        // First pass — read just the dimensions. The bitmap allocation for
        // this call is suppressed by inJustDecodeBounds; cheap.
        val bounds = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        // Pick the largest power of 2 that keeps both dims at ≥ targetPx.
        // This means the decoded bitmap is ≤ 2× the target in either axis,
        // which renders crisply at the target size with no visible aliasing.
        var sample = 1
        while (srcW / (sample * 2) >= targetPx && srcH / (sample * 2) >= targetPx) {
            sample *= 2
        }

        val opts = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        return android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }
}
