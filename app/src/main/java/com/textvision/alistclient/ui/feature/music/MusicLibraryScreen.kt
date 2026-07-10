package com.textvision.alistclient.ui.feature.music

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.music.AlbumCard
import com.textvision.alistclient.ui.components.music.AlbumCardSize
import com.textvision.alistclient.ui.components.music.AlbumDecoBadge
import com.textvision.alistclient.ui.components.music.ArtistCard
import com.textvision.alistclient.ui.components.music.MiniPlayer
import com.textvision.alistclient.ui.components.music.MusicHeroCard
import com.textvision.alistclient.ui.components.music.SongRow
import com.textvision.alistclient.ui.components.ChipKind
import com.textvision.alistclient.ui.components.Chip
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.foundation.CloudDecor
import com.textvision.alistclient.ui.foundation.SkyBlueBackground
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.MusicMagenta
import com.textvision.alistclient.ui.theme.MusicPink
import com.textvision.alistclient.ui.theme.MusicViolet

/**
 * 音乐库屏 — placeholder for the future music library.
 *
 * 6 sections (filter chips / hero / recent albums / artists / album grid /
 * all songs) + sticky [MiniPlayer] at the bottom. Visual-only (no playback,
 * no API — spec §1.2 YAGNI).
 */
@Composable
fun MusicLibraryScreen(
    onBack: () -> Unit = {},
    onOpenPreview: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        SkyBlueBackground()
        CloudDecor()
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = "音乐库",
                subtitle = "${StubData.songCount} 首 · ${StubData.artistCount} 位艺人",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) { Icon(AppIcons.sort, "排序") }
                    IconButton(onClick = {}) { Icon(AppIcons.search, "搜索") }
                },
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                // Section 1: filter chips
                item("filter_chips") { FilterChipsSection() }
                // Section 2: hero
                item("hero") { HeroSection() }
                // Section 3: 最近添加 (4 AlbumCard)
                item("section_recent") {
                    SectionHead(
                        title = "最近添加",
                        trailing = "查看全部 →",
                    )
                }
                item("recent_row") { RecentAlbumsRow() }
                // Section 4: 艺人 (4 ArtistCard)
                item("section_artists") {
                    SectionHead(
                        title = "艺人",
                        trailing = "全部 ${StubData.artistCount} 位 →",
                    )
                }
                item("artists_row") { ArtistsRow() }
                // Section 5: 专辑 grid (2 col)
                item("section_albums") {
                    SectionHead(
                        title = "精选专辑",
                        trailing = "更多 →",
                    )
                }
                item("albums_grid") { AlbumGridSection() }
                // Section 6: 全部歌曲
                item("section_songs") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "全部歌曲",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        ShuffleButton()
                    }
                }
                items(StubData.songs, key = { it.id }) { song ->
                    SongRow(
                        name = song.name,
                        artist = song.artist,
                        duration = song.duration,
                        gradient = song.gradient,
                        isPlaying = song.id == StubData.songs.first().id,
                        onClick = {},
                        onMore = {},
                    )
                }
            }
        }
        // Sticky bottom MiniPlayer
        MiniPlayer(
            name = StubData.songs.first().name,
            artist = StubData.songs.first().artist,
            gradient = StubData.songs.first().gradient,
            isPlaying = true,
            onPlayPause = {},
            onClick = onOpenPreview,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ─── Sections ──────────────────────────────────────────────────────────────

@Composable
private fun FilterChipsSection() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chips = listOf(
            "全部 · ${StubData.songCount}" to ChipKind.PRIMARY,
            "最近添加 · 18" to ChipKind.GRAY,
            "最爱 · 24" to ChipKind.GRAY,
            "下载 · 12" to ChipKind.GRAY,
        )
        items(chips) { (label, kind) ->
            Chip(label = label, kind = kind)
        }
    }
}

@Composable
private fun HeroSection() {
    MusicHeroCard(
        title = "音乐功能即将推出",
        subtitle = "目前为占位界面，敬请期待",
        isPlaying = true,
        onPlayPause = {},
        onFavorite = {},
        onQueue = {},
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RecentAlbumsRow() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(StubData.recentAlbums) { album ->
            AlbumCard(
                name = album.name,
                artist = album.artist,
                gradient = album.gradient,
                decoBadge = AlbumDecoBadge(AppIcons.decoStar),
                size = AlbumCardSize.SMALL,
            )
        }
    }
}

@Composable
private fun ArtistsRow() {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(StubData.artists) { artist ->
            ArtistCard(
                name = artist.name,
                count = artist.count,
                gradient = artist.gradient,
            )
        }
    }
}

@Composable
private fun AlbumGridSection() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(
            items = StubData.featuredAlbums,
            key = { it.name },
        ) { album ->
            AlbumCard(
                name = album.name,
                artist = album.artist,
                gradient = album.gradient,
                decoBadge = AlbumDecoBadge(AppIcons.decoHeart),
                size = AlbumCardSize.LARGE,
            )
        }
    }
}

@Composable
private fun ShuffleButton() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.shuffle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "随机播放",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Section header — 4dp gradient accent bar + title + trailing "查看全部 →". */
@Composable
private fun SectionHead(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(listOf(CandyPink, CandyLilac)),
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Stub data ─────────────────────────────────────────────────────────────

/** Placeholder data for the music library — visual-only, no real playback. */
private object StubData {
    const val songCount = 128
    const val artistCount = 36

    val gradientA = Brush.linearGradient(listOf(MusicPink, CandyLemon))
    val gradientB = Brush.linearGradient(listOf(CandyMint, Brand500))
    val gradientC = Brush.linearGradient(listOf(CandyPink, MusicMagenta))
    val gradientD = Brush.linearGradient(listOf(MusicViolet, CandyLilac))

    data class Album(val name: String, val artist: String, val gradient: Brush)
    data class Artist(val name: String, val count: Int, val gradient: Brush)
    data class Song(val id: Int, val name: String, val artist: String, val duration: String, val gradient: Brush)

    val recentAlbums = listOf(
        Album("夏日海岸", "云端乐团", gradientA),
        Album("午夜电波", "霓虹计划", gradientC),
        Album("蓝色多瑙", "湖畔合唱", gradientB),
        Album("星河漫步", "光年合唱", gradientD),
    )

    val artists = listOf(
        Artist("云端乐团", 24, gradientA),
        Artist("霓虹计划", 18, gradientC),
        Artist("湖畔合唱", 31, gradientB),
        Artist("光年合唱", 12, gradientD),
    )

    val featuredAlbums = listOf(
        Album("海岸精选", "云端乐团", gradientA),
        Album("电波之夜", "霓虹计划", gradientC),
    )

    val songs = listOf(
        Song(1, "夏日海岸", "云端乐团", "3:42", gradientA),
        Song(2, "午夜电波", "霓虹计划", "4:08", gradientC),
        Song(3, "蓝色多瑙", "湖畔合唱", "5:21", gradientB),
        Song(4, "星河漫步", "光年合唱", "4:36", gradientD),
        Song(5, "海风轻拂", "云端乐团", "3:12", gradientA),
        Song(6, "霓虹之夜", "霓虹计划", "5:02", gradientC),
        Song(7, "湖面涟漪", "湖畔合唱", "4:18", gradientB),
        Song(8, "流星雨", "光年合唱", "3:54", gradientD),
        Song(9, "雨后初晴", "云端乐团", "4:25", gradientA),
        Song(10, "都市节拍", "霓虹计划", "3:38", gradientC),
    )
}

// ─── Previews ──────────────────────────────────────────────────────────────

@Preview(name = "MusicLibrary Light", showBackground = true)
@Composable
private fun MusicLibraryScreenLightPreview() {
    AlistTheme { MusicLibraryScreen() }
}

@Preview(name = "MusicLibrary Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MusicLibraryScreenDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) { MusicLibraryScreen() }
}

@Preview(name = "MusicLibrary LargeFont", showBackground = true, fontScale = 1.4f)
@Composable
private fun MusicLibraryScreenLargeFontPreview() {
    AlistTheme { MusicLibraryScreen() }
}