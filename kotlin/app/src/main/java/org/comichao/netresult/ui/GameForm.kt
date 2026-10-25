package org.comichao.netresult.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.comichao.netresult.*
import org.comichao.netresult.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SetRow(p: String = "", o: String = "", tb: Boolean = false) {
    var player by mutableStateOf(p)
    var opp by mutableStateOf(o)
    var tiebreak by mutableStateOf(tb)
    val valid get() = player.toIntOrNull() != null && opp.toIntOrNull() != null
    fun token() = formatSet(player.toInt(), opp.toInt(), tiebreak)
}

class GameFormState(date: String = LocalDate.now().toString(), opponent: String = "", notes: String = "",
                    sets: List<SetRow> = listOf(SetRow())) {
    var date by mutableStateOf(date)
    var opponent by mutableStateOf(opponent)
    var notes by mutableStateOf(notes)
    val sets = mutableStateListOf<SetRow>().apply { addAll(sets) }

    fun validSets() = sets.filter { it.valid }.map { it.token() }
    val canSave get() = opponent.isNotBlank() && validSets().isNotEmpty()
    val rawSets get() = sets.filter { it.player.isNotEmpty() || it.opp.isNotEmpty() }
        .map { "${it.player.ifEmpty { "0" }}-${it.opp.ifEmpty { "0" }}" + if (it.tiebreak) TIEBREAK_MARK else "" }

    companion object {
        fun from(date: String, opponent: String, score: String?, notes: String?) = GameFormState(
            date, opponent, notes ?: "",
            scoreToSets(score).mapNotNull { runCatching { parseSet(it) }.getOrNull() }
                .map { SetRow(it.player.toString(), it.opponent.toString(), it.tiebreak) }
                .ifEmpty { listOf(SetRow()) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameForm(state: GameFormState, opponents: List<String>, modifier: Modifier = Modifier) {
    var showDate by remember { mutableStateOf(false) }
    val fieldShape = MaterialTheme.shapes.small
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard(stringResource(R.string.date) + " · " + stringResource(R.string.opponent)) {
            OutlinedButton({ showDate = true }, Modifier.fillMaxWidth().height(52.dp), shape = fieldShape) {
                Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(8.dp)); Text(state.date)
            }
            OutlinedTextField(state.opponent, { state.opponent = it }, Modifier.fillMaxWidth(), singleLine = true, shape = fieldShape,
                label = { Text(stringResource(R.string.opponent)) }, placeholder = { Text(stringResource(R.string.enter_opponent)) })
            val suggestions = opponents.filter { it != state.opponent }
            if (suggestions.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { name -> SuggestionChip(onClick = { state.opponent = name }, label = { Text(name) }) }
            }
        }

        SectionCard(stringResource(R.string.sets_label)) {
            state.sets.forEachIndexed { i, row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${i + 1}", Modifier.width(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    ScoreField(row.player, { row.player = it }, Modifier.weight(1f))
                    Text("–", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ScoreField(row.opp, { row.opp = it }, Modifier.weight(1f))
                    FilterChip(row.tiebreak, { row.tiebreak = !row.tiebreak }, label = { Text("TB") })
                    IconButton({ if (state.sets.size > 1) state.sets.removeAt(i) else { row.player = ""; row.opp = ""; row.tiebreak = false } }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            TextButton({ state.sets.add(SetRow()) }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_set).removePrefix("+ ")) }
            Text(stringResource(R.string.tiebreak_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val valid = state.validSets()
            if (valid.isNotEmpty()) ResultPill(computeResult(valid).name)
        }

        SectionCard(stringResource(R.string.notes)) {
            OutlinedTextField(state.notes, { state.notes = it }, Modifier.fillMaxWidth(), shape = fieldShape,
                placeholder = { Text(stringResource(R.string.notes_hint)) }, minLines = 2)
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = runCatching { LocalDate.parse(state.date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let { state.date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                    showDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton({ showDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}

@Composable
private fun ScoreField(value: String, onChange: (String) -> Unit, modifier: Modifier) =
    OutlinedTextField(value, { if (it.length <= 2 && it.all(Char::isDigit)) onChange(it) }, modifier, singleLine = true,
        shape = MaterialTheme.shapes.small,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

@Composable
fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
