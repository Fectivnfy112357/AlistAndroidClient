package com.textvision.alistclient.music.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Pure helpers for the playback queue batching path. Extracted from
 * `MusicPlaybackService.enqueueTail` so they can be unit-tested without
 * spinning up Robolectric or a live ExoPlayer.
 *
 * The contract is: given `n` pending paths, return a `path -> MediaItem`
 * lookup preserving original order (paths whose sign returns `null` are simply
 * absent from the lookup) and an ordered list view helpers can walk.
 */
internal object PlaybackQueueBatch {

    fun buildOrderedList(
        paths: List<String>,
        signed: Map<String, MediaItem>,
    ): List<MediaItem> {
        if (paths.isEmpty()) return emptyList()
        val out = ArrayList<MediaItem>(paths.size)
        for (path in paths) {
            val item = signed[path] ?: continue
            out += item
        }
        return out
    }

    /**
     * Sign every path with bounded parallelism over [ioContext]. Caller supplies
     * the signing lambda so the helper stays oblivious to the actual HTTP layer.
     *
     * Returns a `path -> MediaItem` map; failed signs are dropped (the path
     * will be absent from the map and dropped by [buildOrderedList]). Order in
     * the original [paths] list is the responsibility of the caller — this
     * function only models the parallelism, not the index assignment.
     */
    suspend fun signAllOrdered(
        paths: List<String>,
        ioContext: CoroutineContext,
        sem: Semaphore,
        sign: suspend (String) -> String?,
    ): Map<String, MediaItem> = coroutineScope {
        paths.map { path ->
            async(ioContext) {
                val signed = sem.withPermit { sign(path) } ?: return@async null
                path to MediaItem.Builder().setUri(signed).setMediaId(path).build()
            }
        }.awaitAll().filterNotNull().toMap()
    }

    /**
     * Apply the batched queue update on a single main-thread step. Replaces the
     * previous "one `addMediaItem` per path" loop that produced ~437 main-thread
     * mutations at player entry. This helper issues **at most two** mutations:
     * one `addMediaItems(0, earlier)` to prepend the prefix, and at most one
     * `addMediaItems(currentCount, later)` to append the suffix.
     *
     * `exoPlayer.mediaItemCount` is captured at call time (not earlier) so the
     * second index reads correctly after the first batch was applied.
     */
    fun applyBatchOnMain(
        exoPlayer: Player,
        earlier: List<MediaItem>,
        later: List<MediaItem>,
    ) {
        if (earlier.isNotEmpty()) exoPlayer.addMediaItems(0, earlier)
        if (later.isNotEmpty()) exoPlayer.addMediaItems(exoPlayer.mediaItemCount, later)
    }
}

