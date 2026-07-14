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
                            0 -> OverviewTab(ui) { songs, start ->
                                viewModel.onPlayQueueClick(context, songs, start)
                                onOpenPreview()
                            }
                            1 -> SongsTab(ui, playback.current?.path) { songs, start ->
                                viewModel.onPlayQueueClick(context, songs, start)
                                onOpenPreview()
                            }
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
                    gradient = Brush.linearGradient(listOf(CandyPink, MusicMagenta, MusicPink)),
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
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
                        AlbumCard(album.name, album.artist, Brush.linearGradient(listOf(CandyPink, CandyLilac)), size = AlbumCardSize.LARGE)
                    }
                }
            }
        }
        if (ui.songs.isNotEmpty()) {
            item { SectionHeader("继续聆听", "从你的音乐里开始") }
            items(ui.songs.take(5), key = { it.path }) { song ->
                SongRow(song.title, song.artist, "", Brush.linearGradient(listOf(CandyPink, Brand500)), onClick = {
                    onPlayQueue(ui.songs, ui.songs.indexOf(song))
                })
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
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
    PagedList(
        title = "所有歌曲",
        subtitle = "按索引顺序 · 已显示 ${visible.size} / ${ui.songs.size}",
        state = state,
    ) {
        items(visible, key = { it.path }) { song ->
            SongRow(
                song.title, song.artist, "", Brush.linearGradient(listOf(CandyPink, Brand500)),
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
        columns = GridCells.Fixed(2), state = state,
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { SectionHeader("全部专辑", "已显示 ${visible.size} / ${ui.albums.size}") }
        items(visible, key = { "${it.artist}/${it.name}" }) { album ->
            AlbumCard(album.name, album.artist, Brush.linearGradient(listOf(CandyLemon, MusicMagenta)), size = AlbumCardSize.LARGE)
        }
        if (visible.size < ui.albums.size) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { LoadingMore() }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { Spacer(Modifier.height(120.dp)) }
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
            ArtistCard(artist.name, artist.songCount, Brush.linearGradient(listOf(CandyMint, Brand500)), modifier = Modifier.fillMaxWidth())
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

@Composable private fun SectionHeader(title: String, subtitle: String) = Column {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
