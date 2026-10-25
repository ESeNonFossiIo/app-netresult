package org.comichao.netresult.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.comichao.netresult.R

@Composable
fun HomeScreen(vm: MainViewModel, onLog: () -> Unit, onResume: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit) {
    val s by vm.stats.collectAsState()
    val games by vm.games.collectAsState()
    val hasDraft by vm.hasDraft.collectAsState()
    val pal = LocalPalette.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(pal.header).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(52.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_name), Modifier.weight(1f), color = Color.White,
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(s.wins.toString(), stringResource(R.string.matches_won), WinFg, Modifier.weight(1f))
                Chip(s.draws.toString(), stringResource(R.string.matches_drawn), DrawFg, Modifier.weight(1f))
                Chip(s.losses.toString(), stringResource(R.string.matches_lost), LossFg, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(s.setsWon.toString(), stringResource(R.string.sets_won), WinFg, Modifier.weight(1f))
                Chip(s.setsLost.toString(), stringResource(R.string.sets_lost), LossFg, Modifier.weight(1f))
                Chip("${s.setWinPct}%", stringResource(R.string.set_win_pct), pal.accent, Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WinRateBar(s.winPct, pal.accent, Modifier.weight(1f))
                Text("${s.winPct}%", color = pal.accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Text("${stringResource(R.string.win_rate)} · ${s.total} ${stringResource(R.string.played).lowercase()}",
                color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onLog, Modifier.fillMaxWidth().height(60.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = pal.onAccent)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.log_game_btn).removePrefix("+ "), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
            FilledTonalButton(onHistory, Modifier.fillMaxWidth().height(54.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = .16f),
                    contentColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.view_history), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            if (hasDraft) OutlinedButton(onResume, Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Default.Edit, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.resume_draft), fontWeight = FontWeight.Bold)
            }
            if (games.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.history), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                games.take(3).forEach { GameCard(it) }
            }
        }
    }
}

@Composable
private fun Chip(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .11f)).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color.lighten(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color.White.copy(alpha = .8f), fontSize = 12.sp, maxLines = 1)
    }
}

private fun Color.lighten() = Color(red + (1 - red) * .35f, green + (1 - green) * .35f, blue + (1 - blue) * .35f)

@Composable
private fun WinRateBar(pct: Int, fill: Color, modifier: Modifier) {
    Box(modifier.height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .18f))) {
        if (pct > 0) Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(if (pct >= 50) fill else LossFg))
    }
}
