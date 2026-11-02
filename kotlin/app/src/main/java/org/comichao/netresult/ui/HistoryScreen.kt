package org.comichao.netresult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comichao.netresult.*
import org.comichao.netresult.R
import org.comichao.netresult.data.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val games by vm.games.collectAsState()
    val opponents by vm.opponents.collectAsState()
    var filter by rememberSaveable { mutableStateOf("") }
    var grouped by rememberSaveable { mutableStateOf(false) }
    // Opponents whose games are shown while grouped; every group starts collapsed.
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    var editing by remember { mutableStateOf<Game?>(null) }
    var menu by remember { mutableStateOf(false) }

    val shown = if (filter.isEmpty()) games else games.filter { it.opponent == filter }
    val stats = computeStats(shown)

    Scaffold(topBar = { BrandTopBar(stringResource(R.string.history), onBack) }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton({ menu = true }, Modifier.fillMaxWidth()) {
                        Text(if (filter.isEmpty()) stringResource(R.string.all_opponents) else filter, maxLines = 1)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text(stringResource(R.string.all_opponents)) }, { filter = ""; menu = false })
                        opponents.forEach { o -> DropdownMenuItem({ Text(o) }, { filter = o; menu = false }) }
                    }
                }
                FilterChip(grouped, { grouped = !grouped }, label = { Text(stringResource(R.string.group_by_opponent)) })
            }
            SectionCard(null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${stats.wins}", stringResource(R.string.result_win), WinFg)
                    Stat("${stats.draws}", stringResource(R.string.result_draw), DrawFg)
                    Stat("${stats.losses}", stringResource(R.string.result_loss), LossFg)
                    Stat("${stats.setsWon}–${stats.setsLost}", stringResource(R.string.sets_won), MaterialTheme.colorScheme.secondary)
                    Stat("${stats.winPct}%", stringResource(R.string.win_rate), MaterialTheme.colorScheme.secondary)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (grouped) {
                    shown.groupBy { it.opponent }.toSortedMap().forEach { (opp, list) ->
                        val open = opp in expanded
                        item(key = "h-$opp") {
                            OpponentHeader(opp, computeStats(list), open) { expanded = if (open) expanded - opp else expanded + opp }
                        }
                        if (open) items(list, key = { it.id }) { GameCard(it, showOpponent = false) { editing = it } }
                    }
                } else items(shown, key = { it.id }) { GameCard(it, showOpponent = true) { editing = it } }
            }
        }
    }

    editing?.let { g -> EditDialog(g, opponents, vm) { editing = null } }
}

/** Collapsible group header: name and win rate, W/D/L in fixed columns, and a status-coloured bar. */
@Composable
private fun OpponentHeader(opponent: String, st: Stats, open: Boolean, onClick: () -> Unit) {
    // Win rate and bar share one colour: green when ahead, red when behind, the draw colour when level.
    val tone = when {
        st.wins > st.losses -> WinFg
        st.wins < st.losses -> LossFg
        else -> DrawFg
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(opponent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("${st.winPct}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tone)
                }
                Tally(st.wins, stringResource(R.string.result_win), WinFg)
                Tally(st.draws, stringResource(R.string.result_draw), DrawFg)
                Tally(st.losses, stringResource(R.string.result_loss), LossFg)
                Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).background(tone))
        }
    }
}

/** One fixed-width W/D/L column, so the numbers line up from group to group. */
@Composable
private fun Tally(value: Int, label: String, color: androidx.compose.ui.graphics.Color) =
    Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label.take(1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

@Composable
private fun Stat(value: String, label: String, color: androidx.compose.ui.graphics.Color) =
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

@Composable
private fun EditDialog(g: Game, opponents: List<String>, vm: MainViewModel, onDone: () -> Unit) {
    val state = remember(g.id) { GameFormState.from(g.date, g.opponent, g.score, g.notes) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.history)) },
        text = { GameForm(state, opponents, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = {
            TextButton({ vm.updateGame(g, state.date, state.opponent.trim(), state.validSets(), state.notes); onDone() }, enabled = state.canSave) {
                Text(stringResource(R.string.save_game))
            }
        },
        dismissButton = {
            Row {
                IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                TextButton(onDone) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(g.opponent) },
        text = { Text(g.date + "  " + scoreToSets(g.score).joinToString(" ")) },
        confirmButton = { IconButton({ vm.deleteGame(g.id); confirmDelete = false; onDone() }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
