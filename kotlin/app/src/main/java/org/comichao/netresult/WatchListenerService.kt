package org.comichao.netresult

import android.os.Build
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import org.comichao.netresult.data.AppDatabase
import org.comichao.netresult.data.Repository
import org.comichao.netresult.health.HealthSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Receives finished matches and warm-ups from the watch: matches go to the history, both go to Health Connect. */
class WatchListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != SESSION_PATH) {
            runBlocking { LiveLink.handle(this@WatchListenerService, event.path, event.data, event.sourceNodeId) }
            return
        }
        val session = runCatching { WatchSession.fromJson(String(event.data, Charsets.UTF_8)) }.getOrNull() ?: return
        runBlocking {   // runs on a binder thread, and the service lives until this returns
            val isMatch = session.kind == WatchSession.MATCH
            LiveBus.load(this@WatchListenerService)
            if (isMatch && session.sets.isNotEmpty()) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(session.startMillis))
                // A match started on the phone as doubles keeps its players when it is finished on the watch.
                val live = LiveBus.state.value?.takeIf { it.startedAt == session.startMillis }
                val info = GameInfo(
                    date = date, opponent = live?.opponent ?: session.opponent.ifBlank { "Opponent" },
                    doubles = live?.doubles == true, partner = live?.partner.orEmpty(), opponent2 = live?.opponent2.orEmpty(),
                    startTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(session.startMillis)),
                    duration = ((session.endMillis - session.startMillis) / 60_000).toInt().takeIf { it > 0 },
                )
                Repository(this@WatchListenerService, AppDatabase.get(this@WatchListenerService)).addGame(info, session.sets)
            }
            // The match was finished on the watch: close the mirrored live match here too. Only if it is the same
            // match; an older session the watch resent after being offline must not end the current one.
            if (isMatch && LiveBus.state.value?.startedAt == session.startMillis) LiveBus.end(this@WatchListenerService, true)
            if (Build.VERSION.SDK_INT >= 26) runCatching {
                if (HealthSync.granted(this@WatchListenerService)) HealthSync.writeSession(
                    this@WatchListenerService,
                    if (isMatch) "Tennis vs ${LiveBus.state.value?.takeIf { it.startedAt == session.startMillis }?.label ?: session.opponent}" else "Tennis warm-up",
                    session.startMillis, session.endMillis, session.heartRate, session.kcal)
            }
        }
    }
}
