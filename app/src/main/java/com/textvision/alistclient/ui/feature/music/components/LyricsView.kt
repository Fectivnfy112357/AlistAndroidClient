package com.textvision.alistclient.ui.feature.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.textvision.alistclient.music.data.model.LrcLine

internal fun nextLyricScrollTarget(previous: Int, current: Int, lineCount: Int): Int? =
    current.takeIf { it in 0 until lineCount && it != previous }

@Composable
fun LyricsView(
    lines: List<LrcLine>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    height: Dp = 360.dp,
) {
    val listState = rememberLazyListState()
    val lastRequestedIndex = remember(lines) { mutableIntStateOf(-1) }
    val scrollTarget = nextLyricScrollTarget(
        previous = lastRequestedIndex.intValue,
        current = currentIndex,
        lineCount = lines.size,
    )

    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        lastRequestedIndex.intValue = target
        listState.animateScrollToItem(index = target, scrollOffset = -120)
    }

    if (lines.isEmpty()) {
        Text(
            text = "暂无歌词",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(16.dp),
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().height(height),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 120.dp),
    ) {
        items(lines.size) { index ->
            val isCurrent = index == currentIndex
            Text(
                text = lines[index].text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isCurrent)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}
