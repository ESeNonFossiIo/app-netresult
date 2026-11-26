package org.comichao.netresult.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import org.comichao.netresult.data.Draft
import org.comichao.netresult.data.Game
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

    // Doubles and the optional details of the match.
    var doubles by mutableStateOf(false)
    var partner by mutableStateOf("")
    var opponent2 by mutableStateOf("")
    var surface by mutableStateOf<String?>(null)
    var setting by mutableStateOf<String?>(null)
    var startTime by mutableStateOf<String?>(null)
    var duration by mutableStateOf("")
    var weather by mutableStateOf<String?>(null)
    var temp by mutableStateOf("")
    var venue by mutableStateOf("")

    /** Copy everything from [f] (a draft being resumed). */
    fun load(f: GameFormState) {
        date = f.date; opponent = f.opponent; notes = f.notes
        doubles = f.doubles; partner = f.partner; opponent2 = f.opponent2
        surface = f.surface; setting = f.setting; startTime = f.startTime; duration = f.duration
        weather = f.weather; temp = f.temp; venue = f.venue
        sets.clear(); sets.addAll(f.sets)
    }

    fun validSets() = sets.filter { it.valid }.map { it.token() }
    val canSave get() = opponent.isNotBlank() && (!doubles || opponent2.isNotBlank()) && validSets().isNotEmpty()
    val rawSets get() = sets.filter { it.player.isNotEmpty() || it.opp.isNotEmpty() }
        .map { "${it.player.ifEmpty { "0" }}-${it.opp.ifEmpty { "0" }}" + if (it.tiebreak) TIEBREAK_MARK else "" }

    /** Anything beyond date, opponent, sets and notes has been filled in (so the details section starts open). */
    val hasDetails get() = surface != null || setting != null || startTime != null || duration.isNotEmpty() ||
        weather != null || temp.isNotEmpty() || venue.isNotBlank()

    /** Whether the user typed anything at all (to ask before throwing a form away). */
    val dirty get() = opponent.isNotBlank() || notes.isNotBlank() || doubles || partner.isNotBlank() || opponent2.isNotBlank() ||
        hasDetails || sets.any { it.player.isNotEmpty() || it.opp.isNotEmpty() }

    fun info() = GameInfo(
        date = date, opponent = opponent.trim(), notes = notes, doubles = doubles, partner = partner.trim(), opponent2 = opponent2.trim(),
        surface = surface, setting = setting, startTime = startTime, duration = duration.toIntOrNull(),
        weather = weather, temp = temp.toIntOrNull(), venue = venue.trim(),
    )

    private fun fill(i: GameInfo) {
        doubles = i.doubles; partner = i.partner; opponent2 = i.opponent2
        surface = i.surface; setting = i.setting; startTime = i.startTime
        duration = i.duration?.toString() ?: ""; weather = i.weather; temp = i.temp?.toString() ?: ""; venue = i.venue
    }

    companion object {
        private fun sets(score: String?) = scoreToSets(score).mapNotNull { runCatching { parseSet(it) }.getOrNull() }
            .map { SetRow(it.player.toString(), it.opponent.toString(), it.tiebreak) }
            .ifEmpty { listOf(SetRow()) }

        fun from(g: Game) = GameFormState(g.date, g.opponent, g.notes ?: "", sets(g.score)).also {
            it.fill(GameInfo(g.date, g.opponent, g.notes ?: "", g.isDoubles, g.partner ?: "", g.opponent2 ?: "", g.surface, g.setting,
                g.startTime, g.duration, g.weather, g.temp, g.venue ?: ""))
        }

        fun from(d: Draft, today: String) = GameFormState(d.date.ifEmpty { today }, d.opponent, d.notes, sets(d.sets)).also {
            it.fill(GameInfo(d.date, d.opponent, d.notes, d.mode == "doubles", d.partner ?: "", d.opponent2 ?: "", d.surface, d.setting,
                d.startTime, d.duration, d.weather, d.temp, d.venue ?: ""))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameForm(state: GameFormState, opponents: List<String>, partners: List<String>, venues: List<String>, modifier: Modifier = Modifier) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(state.hasDetails) }
    val fieldShape = MaterialTheme.shapes.small
    // Anyone you have played with or against can be the partner or either opponent.
    val players = remember(opponents, partners) { allPlayers(opponents, partners) }
    val places = remember(venues) { distinctNames(venues) }   // venues used before
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionCard(stringResource(R.string.match_type)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!state.doubles, { state.doubles = false }, label = { Text(stringResource(R.string.singles)) })
                FilterChip(state.doubles, { state.doubles = true }, label = { Text(stringResource(R.string.doubles)) })
            }
        }

        SectionCard(stringResource(R.string.date) + " · " + stringResource(if (state.doubles) R.string.players else R.string.opponent)) {
            OutlinedButton({ showDate = true }, Modifier.fillMaxWidth().height(52.dp), shape = fieldShape) {
                Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(8.dp)); Text(state.date)
            }
            if (state.doubles) {
                PickerField(state.partner, { state.partner = it }, stringResource(R.string.partner), stringResource(R.string.enter_partner), players, listOf(state.opponent, state.opponent2))
                PickerField(state.opponent, { state.opponent = it }, stringResource(R.string.opponent_one), stringResource(R.string.enter_opponent), players, listOf(state.partner, state.opponent2))
                PickerField(state.opponent2, { state.opponent2 = it }, stringResource(R.string.opponent_two), stringResource(R.string.enter_opponent), players, listOf(state.partner, state.opponent))
            } else {
                PickerField(state.opponent, { state.opponent = it }, stringResource(R.string.opponent), stringResource(R.string.enter_opponent), players, emptyList())
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

        // Optional: surface, setting, time, weather and place. Folded away so that logging a match stays quick.
        SectionCard(null) {
            Row(Modifier.fillMaxWidth().clickable { detailsOpen = !detailsOpen }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.details), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Icon(if (detailsOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (detailsOpen) {
                Label(stringResource(R.string.surface))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SURFACES.forEach { k -> FilterChip(state.surface == k, { state.surface = if (state.surface == k) null else k }, label = { Text(surfaceLabel(k)) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SETTINGS.forEach { k -> FilterChip(state.setting == k, { state.setting = if (state.setting == k) null else k }, label = { Text(settingLabel(k)) }) }
                }
                Label(stringResource(R.string.start_time) + " · " + stringResource(R.string.duration_min))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ showTime = true }, Modifier.weight(1f).height(56.dp), shape = fieldShape) { Text(state.startTime ?: stringResource(R.string.set_time)) }
                    if (state.startTime != null) IconButton({ state.startTime = null }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) }
                    OutlinedTextField(state.duration, { if (it.length <= 3 && it.all(Char::isDigit)) state.duration = it }, Modifier.weight(1f), singleLine = true,
                        shape = fieldShape, label = { Text(stringResource(R.string.duration_min)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Label(stringResource(R.string.weather))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WEATHERS.forEach { k -> FilterChip(state.weather == k, { state.weather = if (state.weather == k) null else k }, label = { Text(weatherLabel(k)) }) }
                }
                OutlinedTextField(state.temp, { if (it.length <= 3 && it.all { c -> c.isDigit() || c == '-' }) state.temp = it }, Modifier.fillMaxWidth(), singleLine = true,
                    shape = fieldShape, label = { Text(stringResource(R.string.temperature)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PickerField(state.venue, { state.venue = it }, stringResource(R.string.venue), stringResource(R.string.venue_hint), places, emptyList())
            }
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
    if (showTime) {
        val (h, m) = (state.startTime ?: "18:00").split(":").map { it.toInt() }
        val picker = rememberTimePickerState(h, m, true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton({ state.startTime = "%02d:%02d".format(picker.hour, picker.minute); showTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton({ showTime = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(picker) },
        )
    }
}

/** Everyone known as an opponent or a partner, once each, in alphabetical order. */
fun allPlayers(opponents: List<String>, partners: List<String>): List<String> = distinctNames(opponents + partners)

/** The names once each (ignoring case and spaces around them), without blanks, in alphabetical order. */
fun distinctNames(names: List<String>): List<String> =
    names.filter { it.isNotBlank() }.distinctBy { it.trim().lowercase() }.sortedBy { it.lowercase() }

/**
 * Picks a name (a player, a venue) from a drop-down of the ones used before; a new name can still be typed, and the list
 * narrows as you type. Names already chosen in the other slots ([others]) are not offered again, so nobody is picked twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerField(value: String, onChange: (String) -> Unit, label: String, placeholder: String, choices: List<String>, others: List<String>) {
    var open by remember { mutableStateOf(false) }
    val taken = others.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val free = choices.filter { it.trim().lowercase() !in taken }
    val typed = value.trim().lowercase()
    // A name already chosen shows the whole list again; a half-typed one narrows it.
    val shown = if (typed.isEmpty() || free.any { it.trim().lowercase() == typed }) free else free.filter { typed in it.lowercase() }
    ExposedDropdownMenuBox(open && shown.isNotEmpty(), { open = it }) {
        OutlinedTextField(value, { onChange(it); open = true }, Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(), singleLine = true,
            shape = MaterialTheme.shapes.small, label = { Text(label) }, placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open && shown.isNotEmpty()) })
        ExposedDropdownMenu(open && shown.isNotEmpty(), { open = false }) {
            shown.forEach { name -> DropdownMenuItem({ Text(name) }, { onChange(name); open = false }) }
        }
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
