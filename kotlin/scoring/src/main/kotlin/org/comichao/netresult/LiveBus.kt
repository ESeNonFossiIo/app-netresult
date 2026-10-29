package org.comichao.netresult

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one live match of this device, shared by the UI and the background message service. Every change is
 * written to disk, so the match survives the app being killed. Remote changes only win if they are newer.
 */
object LiveBus {
    private const val STALE_MS = 6 * 60 * 60 * 1000L   // a match nobody touched for 6 hours is abandoned

    private val _state = MutableStateFlow<LiveState?>(null)
    val state: StateFlow<LiveState?> = _state

    /** Emits `true` when the match was finished and saved elsewhere, `false` when it was discarded there. */
    private val _ended = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val ended: SharedFlow<Boolean> = _ended

    private var loaded = false
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("live", Context.MODE_PRIVATE)

    @Synchronized fun load(c: Context) {
        if (loaded) return
        loaded = true
        _state.value = prefs(c).getString("state", null)?.let { runCatching { LiveState.fromJson(it) }.getOrNull() }
            ?.takeIf { System.currentTimeMillis() - it.rev < STALE_MS }
    }

    /** A change made on this device. */
    @Synchronized fun set(c: Context, s: LiveState?) {
        load(c)
        _state.value = s
        prefs(c).edit().apply { if (s == null) remove("state") else putString("state", s.toJson()) }.apply()
    }

    /** A change received from the other device: ignored unless newer, or for a match already ended here. */
    @Synchronized fun remote(c: Context, incoming: LiveState) {
        load(c)
        if (incoming.startedAt == prefs(c).getLong("endedStart", 0)) return
        val cur = _state.value
        if (cur == null || incoming.rev > cur.rev) set(c, incoming)
    }

    /** The match is over (saved or discarded), here or on the other device. */
    @Synchronized fun end(c: Context, saved: Boolean) {
        load(c)
        _state.value?.let { prefs(c).edit().putLong("endedStart", it.startedAt).apply() }
        set(c, null)
        _ended.tryEmit(saved)
    }
}
