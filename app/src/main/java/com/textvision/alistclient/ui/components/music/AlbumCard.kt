package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.DecoBadge
import com.textvision.alistclient.ui.components.DecoColor
import com.textvision.alistclient.ui.components.DecoPosition
import com.textvision.alistclient.ui.components.DecoSize
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.MusicPink
import com.textvision.alistclient.ui.theme.MusicViolet

/** Album cover size preset. */
enum class AlbumCardSize(val cover: Dp) {
    SMALL(105.dp),
    LARGE(140.dp),
}

/** A decoration badge configuration for an [AlbumCard] cover. */
data class AlbumDecoBadge(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: DecoColor = DecoColor.PINK,
)

/**
 * 专辑卡 — square [ArtworkCover] cover with an optional bottom-right [DecoBadge],
 * followed by album name + artist.
 *
 * Two layout modes:
 *  - [fill] = false (default): fixed-width column sized by [size], for horizontal carousels.
 *  - [fill] = true: cover fills the parent width as a square (`aspectRatio(1f)`), for grid cells.
 */
@Composable
fun AlbumCard(
    name: String,
    artist: String,
    gradient: Brush,
    artworkData: ByteArray? = null,
    modifier: Modifier = Modifier,
    decoBadge: AlbumDecoBadge? = null,
    size: AlbumCardSize = AlbumCardSize.SMALL,
    fill: Boolean = false,
) {
    val columnModifier = if (fill) modifier.fillMaxWidth() else modifier.width(size.cover)
    Column(modifier = columnModifier) {
        val coverModifier = if (fill) {
            Modifier.fillMaxWidth().aspectRatio(1f)
        } else {
            Modifier.size(size.cover)
        }
        Box(coverModifier) {
            ArtworkCover(name, artworkData, gradient, modifier = Modifier.fillMaxSize())
            if (decoBadge != null) {
                DecoBadge(
                    icon = decoBadge.icon,
                    position = DecoPosition.BR,
                    size = if (size == AlbumCardSize.LARGE || fill) DecoSize.LG else DecoSize.MD,
                    color = decoBadge.color,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// size() comes from foundation.layout.size import above.

@Preview(name = "AlbumCard small")
@Composable
private fun AlbumCardSmallPreview() {
    MaterialTheme {
        AlbumCard(
            name = "夏日海岸",
            artist = "云端乐团",
            gradient = Brush.linearGradient(listOf(CandyPink, CandyLilac)),
            decoBadge = AlbumDecoBadge(AppIcons.decoStar, DecoColor.LEMON),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "AlbumCard large")
@Composable
private fun AlbumCardLargePreview() {
    MaterialTheme {
        AlbumCard(
            name = "午夜电波",
            artist = "霓虹计划",
            gradient = Brush.linearGradient(listOf(MusicPink, MusicViolet)),
            size = AlbumCardSize.LARGE,
            modifier = Modifier.padding(16.dp),
        )
    }
}
