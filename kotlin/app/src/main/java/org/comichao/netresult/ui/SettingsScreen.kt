package org.comichao.netresult.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.comichao.netresult.BuildConfig
import org.comichao.netresult.R

private val LANGS = listOf("en" to "English", "it" to "Italiano", "pt" to "Português", "es" to "Español", "el" to "Ελληνικά")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Pair<Uri, Boolean>?>(null) }
    var pendingExportJson by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.error)
    val done = stringResource(R.string.done)

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { scope.launch { message = runCatching { write(ctx, it, vm.exportText(false)); done to false }.getOrElse { (it.message ?: failed) to true } } }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { scope.launch { message = runCatching { write(ctx, it, vm.exportText(true)); done to false }.getOrElse { (it.message ?: failed) to true } } }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImport = it to pendingExportJson }
    }

    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2).ifEmpty { java.util.Locale.getDefault().language }

    Scaffold(topBar = { BrandTopBar(stringResource(R.string.settings), onBack) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(stringResource(R.string.palette)) {
                val selected by vm.palette.collectAsState()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Palettes.forEach { pal -> PaletteSwatch(pal, pal.key == selected) { vm.setPalette(pal.key) } }
                }
            }
            SectionCard(stringResource(R.string.language)) {
                var open by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(open, { open = !open }) {
                    OutlinedTextField(LANGS.firstOrNull { it.first == current }?.second ?: "English", {}, readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) })
                    ExposedDropdownMenu(open, { open = false }) {
                        LANGS.forEach { (code, name) ->
                            DropdownMenuItem({ Text(name) }, { vm.setLang(code); open = false })
                        }
                    }
                }
            }
            HealthAccessCard()
            SectionCard(stringResource(R.string.export_data)) {
                Text(stringResource(R.string.export_save_msg), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ exportCsv.launch("net_result.csv") }, Modifier.weight(1f)) { Text("CSV") }
                    FilledTonalButton({ exportJson.launch("net_result.json") }, Modifier.weight(1f)) { Text("JSON") }
                }
            }
            SectionCard(stringResource(R.string.import_data)) {
                Text(stringResource(R.string.import_pick_msg), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ pendingExportJson = false; importPicker.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) }, Modifier.weight(1f)) { Text("CSV") }
                    FilledTonalButton({ pendingExportJson = true; importPicker.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, Modifier.weight(1f)) { Text("JSON") }
                }
            }
            SectionCard(stringResource(R.string.danger_zone)) {
                Text(stringResource(R.string.delete_all_msg), style = MaterialTheme.typography.bodySmall)
                OutlinedButton({ confirmReset = true }, Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.reset_all_data)) }
            }
            Text("${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}", Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_body)) },
        confirmButton = { TextButton({ vm.resetAll(); confirmReset = false }) { Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text(stringResource(R.string.keep_data)) } },
    )
    pendingImport?.let { (uri, json) ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.confirm_import_title)) },
            text = { Text(stringResource(R.string.confirm_import_body, uri.lastPathSegment ?: "")) },
            confirmButton = {
                TextButton({
                    pendingImport = null
                    scope.launch {
                        message = runCatching {
                            val n = vm.importText(read(ctx, uri), json)
                            "$n" to false
                        }.getOrElse { (it.message ?: failed) to true }
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton({ pendingImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    message?.let { (text, isError) ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(stringResource(if (isError) R.string.error else R.string.done)) },
            text = { Text(text) },
            confirmButton = { TextButton({ message = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

private suspend fun write(ctx: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
}

private suspend fun read(ctx: Context, uri: Uri) = withContext(Dispatchers.IO) {
    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
}


@Composable
private fun PaletteSwatch(pal: Palette, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(pal.header)
            .border(if (selected) 3.dp else 1.dp, if (selected) pal.accent else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(pal.accent))
        }
        Text(stringResource(pal.nameRes), style = MaterialTheme.typography.labelMedium, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
