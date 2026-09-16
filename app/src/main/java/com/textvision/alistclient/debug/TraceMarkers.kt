package com.textvision.alistclient.debug

import android.os.Build
import android.os.Trace
import java.util.concurrent.atomic.AtomicInteger

/**
 * TEMPORARY instrumentation for the "首次切 Tab / 首次进首页立刻滚动" jank
 * investigation (2026-09-16).
 *
 * Every marker name carries [PREFIX], so the whole set can be found and removed
 * again with a single `grep -rn "alist:" app/src/main`.
 *
 * Sections are emitted as **async** sections (`S|pid|name|cookie` /
 * `F|pid|name|cookie` in atrace text output) rather than B/E. The marked work
 * runs inside coroutines that can resume on a different thread, and B/E pairs
 * are thread-affine — a suspension point between [begin] and [end] would leave
 * an unbalanced `E` on the new thread. Async sections are keyed by cookie, so
 * they survive thread hops.
 *
 * Both calls are no-ops unless an app-level trace is actually being recorded
 * (`atrace -a com.textvision.alistclient`), so the cost while tracing is off is
 * a single `Trace.isEnabled()` call per instrumented operation.
 */
internal object TraceMarkers {

    /** Marker name prefix — also the single grep key used to remove this file's call sites. */
    const val PREFIX = "alist:"

    /** Returned by [begin] when no trace is active; [end] ignores it. */
    const val NO_COOKIE = -1

    /** `android.os.Trace` rejects section names longer than 127 chars. */
    private const val MAX_NAME_LEN = 120

    private val cookies = AtomicInteger(0)

    private fun tracing(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Trace.isEnabled()

    /**
     * Open an async section named [name]. Returns the cookie to pass back to
     * [end], or [NO_COOKIE] when tracing is off (then [end] is a no-op).
     */
    fun begin(name: String): Int {
        if (!tracing()) return NO_COOKIE
        val cookie = cookies.incrementAndGet()
        Trace.beginAsyncSection(fullName(name), cookie)
        return cookie
    }

    /** Close the async section opened by [begin] for the same [name]. */
    fun end(name: String, cookie: Int) {
        if (cookie == NO_COOKIE) return
        // The SDK guard is repeated here (not just in [tracing]) because the
        // cookie alone doesn't let lint prove the call site is API 29+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        Trace.endAsyncSection(fullName(name), cookie)
    }

    private fun fullName(name: String): String {
        val prefixed = PREFIX + name
        return if (prefixed.length <= MAX_NAME_LEN) prefixed else prefixed.substring(0, MAX_NAME_LEN)
    }
}
