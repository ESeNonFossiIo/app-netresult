package org.comichao.netresult.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import org.comichao.netresult.LiveLink

/** Keeps the live match up to date while the watch app is closed: score changes made on the phone land here. */
class WatchLinkService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        runBlocking { LiveLink.handle(this@WatchLinkService, event.path, event.data, event.sourceNodeId) }
    }
}
