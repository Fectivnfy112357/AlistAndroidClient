package com.textvision.alistclient.ui.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.debug.TraceMarkers
import com.textvision.alistclient.startup.AppStartupWarmer
import com.textvision.alistclient.startup.WarmupProgress
import com.textvision.alistclient.startup.WarmupStep
import com.textvision.alistclient.ui.icons.AppIcons
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Gate that holds the main App UI behind a splash until
 * [AppStartupWarmer.warmUpAll] completes (or [SPLASH_MAX_MS] elapses, whichever
 * comes first). Renders inside the same `setContent` block as the regular
 * `AppNavHost`; switching between the two is a single composition swap.
 *
 * The splash is intentionally minimal:
 * - brand title
 * - linear progress (0..1, animated by Compose, driven by [WarmupProgress])
 * - per-step tick list ("首页 ✓", "文件 ✓", …) so the user can see things are
 *   happening
 * - a small log line summarising the outcome when the gate falls
 *
 * No third-party deps; the screen is just `Column` + `Box` + `Text`.
 *
 * The gate never blocks longer than [SPLASH_MAX_MS] — the underlying warm-up
 * keeps running in the background so VMs that subscribe to `warmCache` can
 * still pick up late arrivals.
 */
@Composable
fun SplashGate(
    warmer: AppStartupWarmer,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableStateOf(WarmupProgress()) }
    var outcome by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(warmer) {
        // alist: temporary jank instrumentation, see TraceMarkers.
        // The span of this async section is the wall-clock cost the splash adds
        // to cold start, and it is what decides whether SPLASH_MAX_MS is ever hit.
        val awaitCookie = TraceMarkers.begin("splash:await")
        // Drive the UI from the warmer's progress flow rather than polling.
        // collect-as-side-effect so the gate always reflects the latest
        // completed step without us having to manage a separate state.
        val collector = kotlinx.coroutines.GlobalScope.launch {
            warmer.progress.collect { p -> progress = p }
        }
        val finished = withTimeoutOrNull(SPLASH_MAX_MS) {
            warmer.warmUpAll()
            true
        } ?: false
        collector.cancel()
        TraceMarkers.end("splash:await", awaitCookie)
        outcome = when {
            finished && progress.done -> "预热完成"
            finished && !progress.done -> "已完成 ${progress.completedSteps}/${progress.totalSteps}"
            else -> "网络较慢，已提前进入"
        }
        ready = true
        onReady()
    }

    val animatedFraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(durationMillis = 240, easing = LinearEasing),
        label = "splash-progress",
    )

    if (ready) {
        // A single frame between gate and the real app — lets Compose
        // release the splash state before AppNavHost mounts.
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = AppIcons.cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "Alist",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "正在预热…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            // Linear progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = "${progress.completedSteps} / ${progress.totalSteps}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Per-step status — checked once done, dot while running.
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                WarmupStep.values().forEachIndexed { idx, step ->
                    val finished = progress.completedSteps > idx || (progress.done && progress.totalSteps > 0)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (finished) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                        )
                        Text(
                            text = step.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (finished) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            outcome?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Hard cap on how long the splash gate is allowed to block the UI. */
internal const val SPLASH_MAX_MS = 5_000L