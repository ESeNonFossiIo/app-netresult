package org.comichao.netresult.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import org.comichao.netresult.R
import org.comichao.netresult.health.HealthSync

/** Settings card: lets sessions from the watch (and Live match) be written to Health Connect. */
@Composable
fun HealthAccessCard() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= 26 && HealthSync.available(context)) HealthAccess()
}

@RequiresApi(26)
@Composable
private fun HealthAccess() {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { allowed = HealthSync.granted(context) }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        allowed = it.containsAll(HealthSync.permissions)
    }
    SectionCard(stringResource(R.string.health_connect)) {
        Text(stringResource(if (allowed) R.string.health_allowed else R.string.health_watch_hint),
            style = MaterialTheme.typography.bodySmall)
        if (!allowed) FilledTonalButton({ launcher.launch(HealthSync.permissions) }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.health_allow))
        }
    }
}
