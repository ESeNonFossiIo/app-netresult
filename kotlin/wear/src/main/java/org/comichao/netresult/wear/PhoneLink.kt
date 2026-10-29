package org.comichao.netresult.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.comichao.netresult.SESSION_PATH
import org.comichao.netresult.WatchSession

/**
 * Sends finished sessions to the phone. A session the phone could not be reached for is kept and retried
 * the next time the app opens, so a match played out of Bluetooth range is not lost.
 */
class PhoneLink(private val context: Context) {
    private val prefs = context.getSharedPreferences("pending", Context.MODE_PRIVATE)

    /** True if the phone received it; otherwise it is queued. */
    suspend fun send(session: WatchSession): Boolean {
        val json = session.toJson()
        if (deliver(json)) return true
        prefs.edit().putStringSet("sessions", pending() + json).apply()
        return false
    }

    suspend fun retryPending() {
        for (json in pending()) if (deliver(json)) prefs.edit().putStringSet("sessions", pending() - json).apply()
    }

    fun pendingCount() = pending().size

    private fun pending(): Set<String> = prefs.getStringSet("sessions", emptySet()) ?: emptySet()

    private suspend fun deliver(json: String): Boolean = runCatching {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        nodes.forEach { Wearable.getMessageClient(context).sendMessage(it.id, SESSION_PATH, json.toByteArray()).await() }
        nodes.isNotEmpty()
    }.getOrDefault(false)
}
