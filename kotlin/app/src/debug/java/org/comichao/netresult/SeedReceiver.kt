package org.comichao.netresult

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.comichao.netresult.data.AppDatabase
import org.comichao.netresult.data.Repository
import java.io.File

/**
 * Debug builds only. Imports `files/seed.csv` from the app's private storage:
 *   adb shell run-as <pkg> cp /data/local/tmp/seed.csv files/seed.csv
 *   adb shell am broadcast -n <pkg>/org.comichao.netresult.SeedReceiver
 * Optional extras: `--es palette clay`, `--ez reset true`.
 */
class SeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val repo = Repository(context, AppDatabase.get(context))
            if (intent.getBooleanExtra("reset", false)) repo.resetAll()
            File(context.filesDir, "seed.csv").takeIf { it.exists() }?.let { repo.importCsv(it.readText()) }
            intent.getStringExtra("palette")?.let { repo.setPref("palette", it) }
            pending.finish()
        }
    }
}
