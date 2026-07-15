package com.textvision.alistclient.ui.feature.music

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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
import com.textvision.alistclient.ui.feature.music.model.UiSong
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private const val PageSize = 40
private const val LoadAhead = 8
private val LibraryTabs = listOf("概览", "歌曲", "专辑", "艺人")

// Stable, reusable Brushes. `Brush.linearGradient(...)` allocates a new
// instance each call and is treated as unstable by Compose, defeating the
// recompose-skipping for every ListItem / SongRow / AlbumCard. Reusing the
// same Brush reference lets LazyList keep items stable across scrolls.
private val RowGradient: Brush = Brush.linearGradient(listOf(CandyPink, Brand500))
private val RecentAlbumGradient: Brush = Brush.linearGradient(listOf(CandyPink, CandyLilac))
private val AlbumGridGradient: Brush = Brush.linearGradient(listOf(CandyLemon, MusicMagenta))
private val ArtistGradient: Brush = Brush.linearGradient(listOf(CandyMint, Brand500))
private val MiniPlayerGradient: Brush = Brush.linearGradient(listOf(CandyPink, MusicMagenta, MusicPink))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(
    onOpenPreview: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: MusicLibraryViewModel = hiltViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Fire-and-forget click handler: dispatch playQueue (Intent to the Service),
    // then jump to the preview page immediately. The Service loads the queue and
    // buffers the first track in the background while the preview page is already
    // on screen; ExoPlayer's playWhenReady=true is set during queue load, so audio
    // starts as soon as the buffer is ready. Serially waiting for queue readiness
    // here was a bad fix that just delayed navigation by the same buffer wait — the
    // user sees "nothing happens for several seconds" and we don't actually win
    // any perceived latency.
    val onPlayQueue: (List<UiSong>, Int) -> Unit = { songs, start ->
        viewModel.onPlayQueueClick(context, songs, start)
        onOpenPreview()
    }

    AppScaffold(transparentBase = true, background = {}) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "音乐库",
                    subtitle = "${ui.artists.size} 位艺人 · ${ui.albums.size} 张专辑 · ${ui.songs.size} 首歌",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = viewModel::onRescanClick) {
                            Icon(AppIcons.refresh, contentDescription = "重新扫描")
                        }
                    },
                )
                when (val index = ui.indexState) {
                    UiIndexState.NotIndexed -> ScanningBlock(0, 0)
                    is UiIndexState.Scanning -> ScanningBlock(index.artistsDone, index.songsFound)
                    UiIndexState.Ready -> {
                        PrimaryTabRow(selectedTabIndex = selectedTab) {
                            LibraryTabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) },
                                )
                            }
                        }
                        when (selectedTab) {
                            0 -> OverviewTab(ui, onPlayQueue)
                            1 -> SongsTab(ui, playback.current?.path, onPlayQueue)
                            2 -> AlbumsTab(ui)
                            else -> ArtistsTab(ui)
                        }
                    }
                    is UiIndexState.Failed -> ErrorState(
                        message = index.message,
                        onRetry = viewModel::onRescanClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            playback.current?.let { current ->
                MiniPlayer(
                    name = current.title,
                    artist = current.artist,
                    gradient = MiniPlayerGradient,
                    isPlaying = playback.isPlaying,
                    onPlayPause = { viewModel.onTogglePlayPause() },
                    onClick = onOpenPreview,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(ui: MusicLibraryUiState, onPlayQueue: (List<UiSong>, Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            MusicHeroCard(
                title = "我的音乐库",
                subtitle = "${ui.artists.size} 位艺人 · ${ui.albums.size} 张专辑 · ${ui.songs.size} 首歌",
                isPlaying = false,
                onPlayPause = { if (ui.songs.isNotEmpty()) onPlayQueue(ui.songs, 0) },
                onFavorite = {},
                onQueue = {},
            )
        }
        if (ui.recentAlbums.isNotEmpty()) {
            item { SectionHeader("最近添加", "新入库的专辑") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.recentAlbums, key = { "${it.artist}/${it.name}" }) { album ->
                        AlbumCard(album.name, album.artist, RecentAlbumGradient, artworkData = album.artworkData, size = AlbumCardSize.LARGE)
                    }
                }
            }
        }
        if (ui.songs.isNotEmpty()) {
            item { SectionHeader("继续聆听", "从你的音乐里开始") }
            item {
                Column {
                    ui.songs.take(5).forEach { song ->
                        SongRow(song.title, song.artist, "", RowGradient, artworkData = song.artworkData, onClick = {
                            // B2: a single row tap is "play this song now"; the rest
                            // of the queue will arrive from UI catalogue navigation
                            // (album/artist clicks) rather than auto-appending the
                            // entire library. 5-row hero card should feel like a
                            // feature shelf, not "play all 439 songs after this one".
                            onPlayQueue(listOf(song), 0)
                        })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SongsTab(ui: MusicLibraryUiState, playingPath: String?, onPlayQueue: (List<UiSong>, Int) -> Unit) {
    var requested by rememberSaveable { mutableIntStateOf(PageSize) }
    val state = rememberLazyListState()
    val visible = ui.songs.take(visibleItemCount(ui.songs.size, requested))
    LaunchedEffect(state, visible.size, ui.songs.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it >= visible.lastIndex - LoadAhead && visible.size < ui.songs.size }
            .distinctUntilChanged().filter { it }.collect { requested += PageSize }
    }
    // Brush is unstable; constructing one per row would defeat LazyList's
    // recompose-skipping. Resolved once with @Stable lambdas from the theme
    // singletons, the same reference is shared across all rows.
    val songBrush = RowGradient
    PagedList(
        title = "所有歌曲",
        subtitle = "按索引顺序 · 已显示 ${visible.size} / ${ui.songs.size}",
        state = state,
    ) {
        items(visible, key = { it.path }) { song ->
            SongRow(
                song.title, song.artist, "", songBrush,
                artworkData = song.artworkData,
                isPlaying = song.path == playingPath,
                onClick = { onPlayQueue(ui.songs, ui.songs.indexOf(song)) },
            )
        }
        if (visible.size < ui.songs.size) item { LoadingMore() }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun AlbumsTab(ui: MusicLibraryUiState) {
    var requested by rememberSaveable { mutableIntStateOf(PageSize) }
    val state = rememberLazyGridState()
    val visible = ui.albums.take(visibleItemCount(ui.albums.size, requested))
    LaunchedEffect(state, visible.size, ui.albums.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it >= visible.lastIndex - LoadAhead && visible.size < ui.albums.size }
            .distinctUntilChanged().filter { it }.collect { requested += PageSize }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 116.dp), state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { SectionHeader("全部专辑", "已显示 ${visible.size} / ${ui.albums.size}") }
        items(visible, key = { "${it.artist}/${it.name}" }) { album ->
            AlbumCard(album.name, album.artist, AlbumGridGradient, artworkData = album.artworkData, fill = true)
        }
        if (visible.size < ui.albums.size) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { LoadingMore() }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun ArtistsTab(ui: MusicLibraryUiState) {
    var requested by rememberSaveable { mutableIntStateOf(PageSize) }
    val state = rememberLazyGridState()
    val visible = ui.artists.take(visibleItemCount(ui.artists.size, requested))
    LaunchedEffect(state, visible.size, ui.artists.size) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { it >= visible.lastIndex - LoadAhead && visible.size < ui.artists.size }
            .distinctUntilChanged().filter { it }.collect { requested += PageSize }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), state = state,
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) { SectionHeader("艺人", "已显示 ${visible.size} / ${ui.artists.size}") }
        items(visible, key = { it.path }) { artist ->
            ArtistCard(artist.name, artist.songCount, ArtistGradient, artworkData = artist.artworkData, modifier = Modifier.fillMaxWidth())
        }
        if (visible.size < ui.artists.size) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) { LoadingMore() }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun PagedList(title: String, subtitle: String, state: androidx.compose.foundation.lazy.LazyListState, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(state = state, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { SectionHeader(title, subtitle) }
        content()
    }
}

@Composable private fun LoadingMore() = Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    CircularProgressIndicator(Modifier.height(20.dp))
    Text("正在加载更多", modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun ScanningBlock(artistsDone: Int, songsFound: Int) = Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
    CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text(if (artistsDone == 0) "准备扫描..." else "扫描中：$artistsDone 位艺人 · $songsFound 首歌曲", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun SectionHeader(title: String, subtitle: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
        Modifier
            .size(width = 4.dp, height = 18.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary)
    )
    Spacer(Modifier.width(10.dp))
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
