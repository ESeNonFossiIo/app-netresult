package org.comichao.netresult.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import org.comichao.netresult.R
import org.comichao.netresult.health.HealthSync

/** Score a match point by point. The display stays on while this screen is open. */
@Composable
fun LiveMatchScreen(vm: MainViewModel, onBack: () -> Unit) {
    val live by vm.live.collectAsState()

    // Keep the display on and let the match sit on top of the lock screen, so the app stays in front.
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        setOverLockScreen(activity, true)
        onDispose { view.keepScreenOn = false; setOverLockScreen(activity, false) }
    }

    val l = live
    if (l == null) LiveSetup(vm, onBack) else LivePlay(vm, l, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveSetup(vm: MainViewModel, onBack: () -> Unit) {
    val opponents by vm.opponents.collectAsState()
    val context = LocalContext.current
    var opponent by remember { mutableStateOf("") }
    var health by remember { mutableStateOf(false) }

    Scaffold(topBar = { BrandTopBar(stringResource(R.string.live_match), onBack) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(stringResource(R.string.opponent)) {
                OutlinedTextField(opponent, { opponent = it }, Modifier.fillMaxWidth(), singleLine = true,
                    shape = MaterialTheme.shapes.small, placeholder = { Text(stringResource(R.string.enter_opponent)) })
                val suggestions = opponents.filter { it != opponent }
                if (suggestions.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { name -> SuggestionChip(onClick = { opponent = name }, label = { Text(name) }) }
                }
            }
            if (Build.VERSION.SDK_INT >= 26 && HealthSync.available(context)) HealthToggle(health) { health = it }
            Button({ vm.startLive(opponent, health) }, Modifier.fillMaxWidth().height(60.dp), enabled = opponent.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = LocalPalette.current.accent, contentColor = LocalPalette.current.onAccent)) {
                Text(stringResource(R.string.start_match), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** Checkbox that asks Health Connect for write access when ticked. */
@RequiresApi(26)
@Composable
private fun HealthToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        onChange(granted.containsAll(HealthSync.permissions))
    }
    Row(Modifier.fillMaxWidth().clickable { if (checked) onChange(false) else launcher.launch(HealthSync.permissions) },
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.record_health), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.record_health_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LivePlay(vm: MainViewModel, l: org.comichao.netresult.LiveState, onBack: () -> Unit) {
    val m = l.match
    val pal = LocalPalette.current
    var confirmExit by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    BackHandler(m.started) { confirmExit = true }

    Scaffold(topBar = { BrandTopBar(stringResource(R.string.live_vs, l.opponent)) { if (m.started) confirmExit = true else { vm.liveDiscard(); onBack() } } }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("", Modifier.weight(1f))
                    m.sets.forEach { Text("${it.player}-${it.opponent}", Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                    Text("${m.gamesMe}-${m.gamesOpp}", Modifier.padding(start = 12.dp), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = pal.accent)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    PointBox(stringResource(R.string.you), m.pointLabel(true))
                    PointBox(l.opponent, m.pointLabel(false))
                }
                if (m.inTiebreak) Text(stringResource(R.string.tiebreak), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            if (!m.over) {
                Button({ vm.livePoint(true) }, Modifier.fillMaxWidth().weight(1f), shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)) {
                    Text(stringResource(R.string.point_you), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                Button({ vm.livePoint(false) }, Modifier.fillMaxWidth().weight(1f), shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text(stringResource(R.string.point_opponent, l.opponent), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Spacer(Modifier.weight(1f))
                ResultPill(org.comichao.netresult.computeResult(m.tokens()).name)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton({ vm.liveUndo() }, Modifier.weight(1f).height(52.dp), enabled = l.history.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.undo))
                }
                Button({ if (m.over) vm.liveSave(onBack) else confirmFinish = true }, Modifier.weight(1f).height(52.dp), enabled = m.tokens().isNotEmpty()) {
                    Text(stringResource(R.string.finish_match))
                }
            }
        }
    }

    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text(stringResource(R.string.finish_early_title)) },
        text = { Text(stringResource(R.string.finish_early_body)) },
        confirmButton = { TextButton({ confirmFinish = false; vm.liveSave(onBack) }) { Text(stringResource(R.string.save_game)) } },
        dismissButton = { TextButton({ confirmFinish = false }) { Text(stringResource(R.string.keep_editing)) } },
    )
    if (confirmExit) AlertDialog(
        onDismissRequest = { confirmExit = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = { TextButton({ confirmExit = false; vm.liveDiscard(); onBack() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton({ confirmExit = false }) { Text(stringResource(R.string.keep_editing)) } },
    )
}

@Composable
private fun PointBox(name: String, points: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(name, maxLines = 1, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(points, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
private fun setOverLockScreen(activity: Activity?, on: Boolean) {
    activity ?: return
    if (Build.VERSION.SDK_INT >= 27) activity.setShowWhenLocked(on)
    else if (on) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
}
