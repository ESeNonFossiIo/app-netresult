package org.comichao.netresult.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.comichao.netresult.R
import org.comichao.netresult.scoreToSets
import org.comichao.netresult.parseSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogGameScreen(vm: MainViewModel, resume: Boolean, onBack: () -> Unit) {
    val opponents by vm.opponents.collectAsState()
    val state = remember { GameFormState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    var draftSaved by remember { mutableStateOf(false) }

    LaunchedEffect(resume) {
        if (resume) vm.loadDraft()?.let { d ->
            val f = GameFormState.from(d.date.ifEmpty { state.date }, d.opponent, d.sets, d.notes)
            state.date = f.date; state.opponent = f.opponent; state.notes = f.notes
            state.sets.clear(); state.sets.addAll(f.sets)
        }
    }
    val dirty = state.opponent.isNotBlank() || state.notes.isNotBlank() || state.sets.any { it.player.isNotEmpty() || it.opp.isNotEmpty() }
    BackHandler(dirty) { confirmDiscard = true }

    Scaffold(
        topBar = { BrandTopBar(stringResource(R.string.log_game)) { if (dirty) confirmDiscard = true else onBack() } },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button({ vm.addGame(state.date, state.opponent.trim(), state.validSets(), state.notes); onBack() },
                        Modifier.fillMaxWidth().height(54.dp), enabled = state.canSave, shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalPalette.current.accent, contentColor = LocalPalette.current.onAccent)) {
                        Text(stringResource(R.string.save_game), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Row {
                        TextButton({ vm.saveDraft(state.date, state.opponent.trim(), state.rawSets, state.notes); draftSaved = true },
                            Modifier.weight(1f), enabled = dirty) { Text(stringResource(R.string.save_draft)) }
                        TextButton({ if (dirty) confirmDiscard = true else onBack() }, Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.log_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GameForm(state, opponents)
        }
    }

    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = { TextButton({ onBack() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } },
    )
    if (draftSaved) AlertDialog(
        onDismissRequest = { draftSaved = false; onBack() },
        title = { Text(stringResource(R.string.draft_saved)) },
        text = { Text(stringResource(R.string.draft_saved_body)) },
        confirmButton = { TextButton({ draftSaved = false; onBack() }) { Text(stringResource(R.string.ok)) } },
    )
}
