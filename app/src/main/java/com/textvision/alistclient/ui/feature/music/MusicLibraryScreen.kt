package com.textvision.alistclient.ui.feature.music

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.ui.components.ErrorState
import com.textvision.alistclient.ui.components.music.AlbumCard
import com.textvision.alistclient.ui.components.music.AlbumCardSize
import com.textvision.alistclient.ui.components.music.ArtistCard
import com.textvision.alistclient.ui.components.music.MiniPlayer
import com.textvision.alistclient.ui.components.music.MusicHeroCard
import com.textvision.alistclient.ui.components.music.SongRow
import com.textvision.alistclient.ui.feature.music.dto.UiIndexState
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.MusicMagenta
import com.textvision.alistclient.ui.theme.MusicPink

@Composable
fun MusicLibraryScreen(
    onOpenPreview: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MusicLibraryViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(transparentBase = true, background = {}) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "音乐库",
                    subtitle = "${ui.songs.size} 首 · ${ui.artists.size} 位艺人",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = viewModel::onRescanClick) {
                            Icon(AppIcons.refresh, contentDescription = "重新扫描")
                        }
                    },
                )

                when (val s = ui.indexState) {
                    UiIndexState.NotIndexed -> ScanningBlock(0, 0)
                    is UiIndexState.Scanning -> ScanningBlock(s.artistsDone, s.songsFound)
                    UiIndexState.Ready -> LibraryBody(
                        ui = ui,
                        onPlayQueue = { songs, idx ->
                            viewModel.onPlayQueueClick(context, songs, idx)
                            onOpenPreview()
                        },
                    )
                    is UiIndexState.Failed -> ErrorState(
                        message = s.message,
                        onRetry = viewModel::onRescanClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            val currentSong = playback.current
            if (currentSong != null) {
                MiniPlayer(
                    name = currentSong.title,
                    artist = currentSong.artist,
                    gradient = MusicPinkHeroGradient,
                    isPlaying = playback.isPlaying,
                    onPlayPause = { /* Play/pause from mini player is intentionally a no-op
                        here; the player screen owns that control. */ },
                    onClick = onOpenPreview,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private val MusicPinkHeroGradient = Brush.linearGradient(
    listOf(CandyPink, MusicMagenta, MusicPink),
)

@Composable
private fun ScanningBlock(artistsDone: Int, songsFound: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (artistsDone == 0) "准备扫描..."
            else "扫描中：$artistsDone 位艺人 · $songsFound 首歌曲",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryBody(
    ui: MusicLibraryUiState,
    onPlayQueue: (List<com.textvision.alistclient.ui.feature.music.model.UiSong>, Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item("hero") {
            MusicHeroCard(
                title = "我的音乐库",
                subtitle = "${ui.artists.size} 位艺人 · ${ui.albums.size} 张专辑 · ${ui.songs.size} 首歌曲",
                isPlaying = false,
                onPlayPause = {
                    if (ui.songs.isNotEmpty()) onPlayQueue(ui.songs, 0)
                },
                onFavorite = {},
                onQueue = {},
            )
        }

        if (ui.recentAlbums.isNotEmpty()) {
            item("recent_header") { SectionHeader("最近添加") }
            item("recent_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.recentAlbums, key = { "${it.artist}/${it.name}" }) { album ->
                        AlbumCard(
                            name = album.name,
                            artist = album.artist,
                            gradient = Brush.linearGradient(listOf(CandyPink, CandyLilac)),
                            size = AlbumCardSize.LARGE,
                        )
                    }
                }
            }
        }

        if (ui.artists.isNotEmpty()) {
            item("artists_header") { SectionHeader("艺人") }
            item("artists_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.artists, key = { it.path }) { artist ->
                        ArtistCard(
                            name = artist.name,
                            count = artist.albumCount,
                            gradient = Brush.linearGradient(listOf(CandyMint, Brand500)),
                        )
                    }
                }
            }
        }

        if (ui.albums.isNotEmpty()) {
            item("albums_header") { SectionHeader("全部专辑") }
            ui.albums.chunked(2).forEachIndexed { idx, pair ->
                item("albums_row_$idx") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        pair.forEach { album ->
                            Box(modifier = Modifier.weight(1f)) {
                                AlbumCard(
                                    name = album.name,
                                    artist = album.artist,
                                    gradient = Brush.linearGradient(listOf(CandyLemon, MusicMagenta)),
                                    size = AlbumCardSize.LARGE,
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Box(modifier = Modifier.weight(1f)) {}
                        }
                    }
                }
            }
        }

        if (ui.songs.isNotEmpty()) {
            item("songs_header") { SectionHeader("所有歌曲") }
            items(ui.songs, key = { it.path }) { song ->
                SongRow(
                    name = song.title,
                    artist = song.artist,
                    duration = "",
                    gradient = Brush.linearGradient(listOf(CandyPink, Brand500)),
                    onClick = {
                        val idx = ui.songs.indexOf(song)
                        onPlayQueue(ui.songs, idx)
                    },
                )
            }
        }

        item("spacer") { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
