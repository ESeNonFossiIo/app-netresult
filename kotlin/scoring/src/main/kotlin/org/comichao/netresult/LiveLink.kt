package org.comichao.netresult

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Sends live-match messages to the other device and handles the ones that arrive. */
object LiveLink {
    suspend fun broadcast(c: Context, path: String, data: ByteArray = ByteArray(0)): Boolean = runCatching {
        val nodes = Wearable.getNodeClient(c).connectedNodes.await()
        nodes.forEach { Wearable.getMessageClient(c).sendMessage(it.id, path, data).await() }
        nodes.isNotEmpty()
    }.getOrDefault(false)

    suspend fun push(c: Context, s: LiveState) = broadcast(c, LIVE_PATH, s.toJson().toByteArray())
    suspend fun end(c: Context, saved: Boolean) = broadcast(c, LIVE_END_PATH, (if (saved) "1" else "0").toByteArray())
    suspend fun request(c: Context) = broadcast(c, LIVE_REQUEST_PATH)

    /** Handles a live-match message; returns false if [path] is not one of ours. */
    suspend fun handle(c: Context, path: String, data: ByteArray, fromNode: String): Boolean {
        when (path) {
            LIVE_PATH -> runCatching { LiveState.fromJson(String(data)) }.getOrNull()?.let { LiveBus.remote(c, it) }
            LIVE_END_PATH -> LiveBus.end(c, String(data) == "1")
            LIVE_REQUEST_PATH -> {
                LiveBus.load(c)
                LiveBus.state.value?.let {
                    runCatching { Wearable.getMessageClient(c).sendMessage(fromNode, LIVE_PATH, it.toJson().toByteArray()).await() }
                }
            }
            else -> return false
        }
        return true
    }
}
