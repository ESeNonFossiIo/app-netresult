package org.comichao.netresult.wear

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import org.comichao.netresult.LiveMatch

/** Vitals need heart rate (BODY_SENSORS before Wear OS 6, READ_HEART_RATE from it) and activity recognition. */
private val sensorPermissions = arrayOf(Manifest.permission.BODY_SENSORS, Manifest.permission.ACTIVITY_RECOGNITION,
    "android.permission.health.READ_HEART_RATE")

private val Ball = Color(0xFFD5E83A)
private val Sky = Color(0xFF35B3EE)

class MainActivity : ComponentActivity() {
    /** True while the watch is in low-power ambient mode (wrist down). The app stays on screen, dimmed. */
    private var ambient by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Registering for ambient is what keeps this activity in front instead of the watch face taking over.
        lifecycle.addObserver(AmbientLifecycleObserver(this, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) { ambient = true }
            override fun onExitAmbient() { ambient = false }
        }))
        setContent {
            MaterialTheme(colors = Colors(primary = Ball, onPrimary = Color(0xFF0B1220), secondary = Sky, onSecondary = Color(0xFF0B1220))) {
                WatchApp(ambient)
            }
        }
    }
}

@Composable
private fun WatchApp(ambient: Boolean, vm: WatchViewModel = viewModel()) {
    // The score must stay in front for the whole match: no screen timeout while a session is running.
    val view = LocalView.current
    DisposableEffect(vm.mode) {
        view.keepScreenOn = vm.mode != Mode.Home
        onDispose { view.keepScreenOn = false }
    }
    when (vm.mode) {
        Mode.Home -> HomeScreen(vm)
        Mode.Match -> if (ambient) AmbientMatch(vm) else MatchScreen(vm)
        Mode.Warmup -> if (ambient) AmbientWarmup(vm) else WarmupScreen(vm)
    }
}

@Composable
private fun HomeScreen(vm: WatchViewModel) {
    val live by vm.live.collectAsState()
    var pending by remember { mutableStateOf(Mode.Home) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (pending == Mode.Match && live != null) vm.join() else vm.start(pending)   // scoring works even if the sensors were refused
    }
    fun begin(mode: Mode) { pending = mode; permissions.launch(sensorPermissions) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        live?.let { l ->
            Chip(label = { Text("Join match", fontWeight = FontWeight.Bold) },
                secondaryLabel = { Text("${l.match.pointLabel(true)}:${l.match.pointLabel(false)} · ${l.match.gamesMe}-${l.match.gamesOpp}") },
                onClick = { permissions.launch(sensorPermissions); pending = Mode.Match },
                modifier = Modifier.fillMaxWidth(), colors = ChipDefaults.primaryChipColors())
            Spacer(Modifier.height(6.dp))
        }
        if (live == null) Chip(label = { Text("Match", fontWeight = FontWeight.Bold) }, secondaryLabel = { Text("Score + vitals") },
            onClick = { begin(Mode.Match) }, modifier = Modifier.fillMaxWidth(), colors = ChipDefaults.primaryChipColors())
        if (live == null) Spacer(Modifier.height(6.dp))
        Chip(label = { Text("Warm-up", fontWeight = FontWeight.Bold) }, secondaryLabel = { Text("Vitals only") },
            onClick = { begin(Mode.Warmup) }, modifier = Modifier.fillMaxWidth(), colors = ChipDefaults.secondaryChipColors())
        when (vm.lastSent) {
            true -> Text("Sent to phone", Modifier.padding(top = 6.dp), fontSize = 11.sp, color = Ball)
            false -> Text("Phone not reached.\nWill retry.", Modifier.padding(top = 6.dp), fontSize = 11.sp, color = Color(0xFFFFB74D))
            null -> {}
        }
    }
}

@Composable
private fun MatchScreen(vm: WatchViewModel) {
    val m = vm.live.collectAsState().value?.match ?: LiveMatch()
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler { if (m.started) confirmExit = !confirmExit else vm.discard() }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        val sets = (m.sets.map { "${it.player}-${it.opponent}" } + "${m.gamesMe}-${m.gamesOpp}").joinToString("  ")
        Text(sets, fontSize = 13.sp, color = Ball, fontWeight = FontWeight.Bold)
        Text("${m.pointLabel(true)} : ${m.pointLabel(false)}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text(if (m.inTiebreak) "Tie-break" else if (vm.heartRate > 0) "♥ ${vm.heartRate}" else if (vm.vitalsOk) "♥ --" else "no sensors",
            fontSize = 11.sp, color = Color.LightGray)
        Spacer(Modifier.height(4.dp))
        if (!m.over) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ vm.point(true) }, Modifier.size(62.dp), colors = ButtonDefaults.primaryButtonColors()) { Text("Me", fontWeight = FontWeight.ExtraBold) }
            Button({ vm.point(false) }, Modifier.size(62.dp), colors = ButtonDefaults.secondaryButtonColors()) { Text("Opp", fontWeight = FontWeight.ExtraBold) }
        } else Text(if (m.setsMe > m.setsOpp) "You won" else "You lost", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        if (confirmExit) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactChip(label = { Text("Discard") }, onClick = { vm.discard() }, colors = ChipDefaults.secondaryChipColors())
            CompactChip(label = { Text("Keep") }, onClick = { confirmExit = false }, colors = ChipDefaults.primaryChipColors())
        } else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactChip(label = { Text("Undo") }, onClick = { vm.undo() }, enabled = vm.canUndo, colors = ChipDefaults.secondaryChipColors())
            CompactChip(label = { Text(if (vm.finishing) "Tap: save" else "Finish") }, onClick = { vm.finishMatch() },
                enabled = m.tokens().isNotEmpty(), colors = ChipDefaults.primaryChipColors())
        }
    }
}

@Composable
private fun WarmupScreen(vm: WatchViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val secs = ((now - vm.startMillis) / 1000).coerceAtLeast(0)
    var confirmExit by remember { mutableStateOf(false) }
    BackHandler { confirmExit = !confirmExit }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Warm-up", fontSize = 13.sp, color = Ball, fontWeight = FontWeight.Bold)
        Text("%d:%02d".format(secs / 60, secs % 60), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text((if (vm.heartRate > 0) "♥ ${vm.heartRate} bpm" else if (vm.vitalsOk) "♥ --" else "no sensors") + " · ${vm.kcal.toInt()} kcal",
            fontSize = 14.sp, color = Color.LightGray)
        Spacer(Modifier.height(6.dp))
        if (confirmExit) {
            CompactChip(label = { Text("Discard warm-up") }, onClick = { vm.discard() }, colors = ChipDefaults.secondaryChipColors())
            Spacer(Modifier.height(4.dp))
            CompactChip(label = { Text("Keep") }, onClick = { confirmExit = false }, colors = ChipDefaults.primaryChipColors())
        } else {
            // Carry on into a match without stopping the workout.
            CompactChip(label = { Text("Start match") }, onClick = { vm.startMatchFromWarmup() }, colors = ChipDefaults.primaryChipColors())
            Spacer(Modifier.height(4.dp))
            CompactChip(label = { Text("End & save") }, onClick = { vm.endWarmup() }, colors = ChipDefaults.secondaryChipColors())
        }
    }
}

// Ambient screens: black background, no buttons, only what is worth reading at a glance (and gentle on burn-in).

@Composable
private fun AmbientMatch(vm: WatchViewModel) {
    val m = vm.live.collectAsState().value?.match ?: LiveMatch()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text((m.sets.map { "${it.player}-${it.opponent}" } + "${m.gamesMe}-${m.gamesOpp}").joinToString("  "), fontSize = 14.sp, color = Color.Gray)
        Text("${m.pointLabel(true)} : ${m.pointLabel(false)}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (vm.heartRate > 0) Text("♥ ${vm.heartRate}", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun AmbientWarmup(vm: WatchViewModel) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Warm-up", fontSize = 14.sp, color = Color.Gray)
        Text(if (vm.heartRate > 0) "♥ ${vm.heartRate}" else "♥ --", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("${vm.kcal.toInt()} kcal", fontSize = 12.sp, color = Color.Gray)
    }
}
