package com.textvision.alistclient.ui.feature.music

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.feature.home.ChipKind
import com.textvision.alistclient.ui.feature.home.StatusChip
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.foundation.CloudDecor
import com.textvision.alistclient.ui.foundation.SkyBlueBackground
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.MusicMagenta
import com.textvision.alistclient.ui.theme.MusicPink
import com.textvision.alistclient.ui.theme.MusicViolet

/**
 * 音乐预览屏 — full-bleed immersive placeholder for the future player.
 *
 * Visual-only (no playback, no API — spec §1.2 YAGNI). Lays out the prototype
 * 300dp pink→violet cover, "即将推出" headline + body, HIRES/FLAC chips, and
 * two skeleton containers (`AppMusicPlayerControlsPlaceholder` /
 * `AppLyricsCardPlaceholder`) so the chrome shape matches the eventual
 * real player screen.
 */
@Composable
fun MusicPreviewScreen(onBack: () -> Unit = {}) {
    Box(Modifier.fillMaxSize()) {
        SkyBlueBackground()
        CloudDecor()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "正在播放",
                subtitle = "来自「音乐库」",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) { Icon(AppIcons.heart, "收藏") }
                    IconButton(onClick = {}) { Icon(AppIcons.more, "更多") }
                },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                // 300dp 渐变封面 + 几何 SVG 占位
                Box(
                    Modifier.size(300.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.linearGradient(
                                listOf(MusicPink, MusicMagenta, MusicViolet),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.musicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(80.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "音乐功能即将推出",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "目前为占位界面，敬请期待",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("HIRES", kind = ChipKind.LILAC)
                    StatusChip("FLAC · 24bit", kind = ChipKind.MINT)
                }
                Spacer(Modifier.height(24.dp))
                AppMusicPlayerControlsPlaceholder()
                Spacer(Modifier.height(16.dp))
                AppLyricsCardPlaceholder()
                Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Skeleton container matching the prototype player chrome:
 * progress bar (6dp gradient) + 5 round control buttons (shuffle / prev / play / next / repeat).
 */
@Composable
private fun AppMusicPlayerControlsPlaceholder() {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // progress bar placeholder
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.height(8.dp))
            // time row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    Modifier
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .size(width = 32.dp, height = 8.dp),
                )
                Box(
                    Modifier
                        .size(width = 32.dp, height = 8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Spacer(Modifier.height(16.dp))
            // 5 round control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Box(
                    Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Box(
                    Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

/**
 * Skeleton container matching the prototype lyrics preview card:
 * title row + 3 lyric lines (middle line uses primary color / font weight).
 */
@Composable
private fun AppLyricsCardPlaceholder() {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "歌词",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "展开 ↓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(12.dp))
            // 3 lyric lines — placeholder boxes
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.85f)
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

// ─── Previews ──────────────────────────────────────────────────────────────

@Preview(name = "MusicPreview Light", showBackground = true)
@Composable
private fun MusicPreviewScreenLightPreview() {
    AlistTheme { MusicPreviewScreen() }
}

@Preview(name = "MusicPreview Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MusicPreviewScreenDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) { MusicPreviewScreen() }
}

@Preview(name = "MusicPreview LargeFont", showBackground = true, fontScale = 1.4f)
@Composable
private fun MusicPreviewScreenLargeFontPreview() {
    AlistTheme { MusicPreviewScreen() }
}