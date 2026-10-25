package org.comichao.netresult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.comichao.netresult.R
import org.comichao.netresult.data.Game
import org.comichao.netresult.scoreToSets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalPalette.current.deep, titleContentColor = Color.White, navigationIconContentColor = Color.White),
    )
}

/** Titled rounded card used to group form fields and settings. */
@Composable
fun SectionCard(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary)
            content()
        }
    }
}

@Composable
fun resultLabel(result: String) = stringResource(when (result) {
    "Win" -> R.string.result_win; "Draw" -> R.string.result_draw; else -> R.string.result_loss })

@Composable
fun ResultPill(result: String) {
    val (fg, bg) = resultColors(result)
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(resultLabel(result), Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = fg,
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GameCard(g: Game, showOpponent: Boolean = true, onClick: (() -> Unit)? = null) {
    val (fg, _) = resultColors(g.result)
    Card(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(fg))
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (showOpponent) Text(g.opponent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(scoreToSets(g.score).joinToString("   ") { it.replace("*", " TB") },
                        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    if (!g.notes.isNullOrBlank()) Text(g.notes, maxLines = 1, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResultPill(g.result)
                    Text(g.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
