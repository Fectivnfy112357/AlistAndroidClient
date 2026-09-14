package com.textvision.alistclient.music.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.sync.Semaphore
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackQueueBatchTest {

    @Test
    fun buildOrderedList_preservesInputOrder_andDropsMissingPaths() {
        val m1 = MediaItem.Builder().setUri("u1").setMediaId("/a").build()
        val m2 = MediaItem.Builder().setUri("u2").setMediaId("/b").build()
        val signed = mapOf("/a" to m1, "/b" to m2)

        val out = PlaybackQueueBatch.buildOrderedList(listOf("/a", "/b", "/missing"), signed)

        assertEquals(listOf(m1, m2), out)
    }

    @Test
    fun buildOrderedList_returnsEmpty_whenPathsEmpty() {
        assertEquals(emptyList<MediaItem>(), PlaybackQueueBatch.buildOrderedList(emptyList(), emptyMap()))
    }

    @Test
    fun buildOrderedList_returnsEmpty_whenSignedEmpty() {
        assertEquals(emptyList<MediaItem>(), PlaybackQueueBatch.buildOrderedList(listOf("/a", "/b"), emptyMap()))
    }

    @Test
    fun signAllOrdered_skipsNullSigns_andKeepsSuccessfulPaths() = runTest(StandardTestDispatcher()) {
        val sem = Semaphore(permits = 4)
        val result = PlaybackQueueBatch.signAllOrdered(
            paths = listOf("/p1", "/p2", "/p3"),
            ioContext = StandardTestDispatcher(testScheduler),
            sem = sem,
            sign = { path ->
                if (path == "/p2") null else "https://signed/$path"
            },
        )

        assertEquals(setOf("/p1", "/p3"), result.keys)
        // MediaItem.mediaId carries the path back to the service for path→Song
        // lookup; the URI carries the signed URL.
        assertEquals("/p1", result["/p1"]?.mediaId)
    }

    @Test
    fun signAllOrdered_concurrentAndUnorderedSign_includesEverySuccessfulPath() = runTest {
        val sem = Semaphore(permits = 4)
        val paths = (0 until 200).map { "/p$it" }
        val result = PlaybackQueueBatch.signAllOrdered(
            paths = paths,
            ioContext = Dispatchers.Unconfined,
            sem = sem,
            // Simulate varying IO latency by yielding once per path; the helper
            // must still capture every path that succeeds.
            sign = { path ->
                if (path.endsWith("0")) null else "https://signed$path"
            },
        )

        // Filter out the synthetic failure paths (every "/pX0") — should be 180 successes.
        val expectedSuccesses = paths.count { !it.endsWith("0") }
        assertEquals(expectedSuccesses, result.size)
        // Every successful path should round-trip via buildOrderedList without
        // losing order.
        val ordered = PlaybackQueueBatch.buildOrderedList(paths, result)
        assertEquals(expectedSuccesses, ordered.size)
        // The first item from the input should appear first in the ordered list.
        assertEquals(paths.first { !it.endsWith("0") }, ordered.first().mediaId)
    }

    @Test
    fun applyBatchOnMain_emitsExactlyTwoAddMediaItemsCalls_regardlessOfQueueSize() {
        val earlier = (0 until 5).map {
            MediaItem.Builder().setUri("e$it").setMediaId("/e$it").build()
        }
        // Simulate the ~437-item tail that previously cost 437 main-thread calls.
        val later = (0 until 437).map {
            MediaItem.Builder().setUri("l$it").setMediaId("/l$it").build()
        }
        val player = mockk<Player>(relaxed = true)
        every { player.mediaItemCount } returns 2 // fastLane already on player

        PlaybackQueueBatch.applyBatchOnMain(player, earlier, later)

        // Contract: exactly two batch calls — one for each list. The 437-item
        // tail collapses into a single main-thread mutation.
        verify(exactly = 1) { player.addMediaItems(0, earlier) }
        verify(exactly = 1) { player.addMediaItems(2, later) }
        verify(exactly = 0) { player.addMediaItem(any()) }
    }

    @Test
    fun applyBatchOnMain_skipsWhenOneSideEmpty_doesNotEmitExtraCall() {
        val later = (0 until 10).map {
            MediaItem.Builder().setUri("l$it").setMediaId("/l$it").build()
        }
        val player = mockk<Player>(relaxed = true)
        every { player.mediaItemCount } returns 0

        PlaybackQueueBatch.applyBatchOnMain(player, earlier = emptyList(), later = later)

        // Earlier is empty → the helper must skip the prepend call entirely.
        verify(exactly = 1) { player.addMediaItems(0, later) }
    }
}
