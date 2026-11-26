package org.comichao.netresult.data

import org.comichao.netresult.*
import org.json.JSONArray
import org.json.JSONObject

/** One match read from an export file, before it is checked and stored. */
data class ImportRow(
    val date: String, val opponent: String, val score: String, val notes: String, val result: String,
    val mode: String = "", val partner: String = "", val opponent2: String = "",
    val surface: String = "", val setting: String = "", val startTime: String = "",
    val duration: String = "", val weather: String = "", val temp: String = "", val venue: String = "",
)

private val CSV_HEADER = listOf("id", "date", "opponent", "result", "score", "notes", "created_at",
    "mode", "partner", "opponent2", "surface", "setting", "start_time", "duration", "weather", "temp", "venue")

/** The CSV layout of the first versions, with the doubles and detail columns added at the end (older files still import). */
fun gamesToCsv(games: List<Game>): String {
    val sb = StringBuilder(CSV_HEADER.joinToString(",")).append('\n')
    for (g in games) {
        sb.append(listOf(g.id.toString(), g.date, g.opponent, g.result, g.score ?: "", g.notes ?: "", "",
            g.mode ?: "", g.partner ?: "", g.opponent2 ?: "", g.surface ?: "", g.setting ?: "", g.startTime ?: "",
            g.duration?.toString() ?: "", g.weather ?: "", g.temp?.toString() ?: "", g.venue ?: "")
            .joinToString(",") { csvEscape(it) }).append('\n')
    }
    return sb.toString()
}

fun gamesToJson(games: List<Game>): String {
    val arr = JSONArray()
    for (g in games) arr.put(JSONObject()
        .put("id", g.id).put("date", g.date).put("opponent", g.opponent).put("result", g.result)
        .put("score", g.score ?: "").put("notes", g.notes ?: "")
        .put("mode", g.mode ?: "").put("partner", g.partner ?: "").put("opponent2", g.opponent2 ?: "")
        .put("surface", g.surface ?: "").put("setting", g.setting ?: "").put("start_time", g.startTime ?: "")
        .put("duration", g.duration?.toString() ?: "").put("weather", g.weather ?: "").put("temp", g.temp?.toString() ?: "")
        .put("venue", g.venue ?: ""))
    return arr.toString(2)
}

/** Columns are found by their header name, so a file from an older version (without the new ones) reads fine. */
fun parseGamesCsv(text: String): List<ImportRow> {
    val rows = parseCsv(text)
    if (rows.isEmpty()) return emptyList()
    val header = rows[0].map { it.trim() }
    fun col(row: List<String>, name: String) = header.indexOf(name).let { if (it in row.indices) row[it] else "" }
    return rows.drop(1).map { r ->
        ImportRow(col(r, "date"), col(r, "opponent"), col(r, "score"), col(r, "notes"), col(r, "result"),
            col(r, "mode"), col(r, "partner"), col(r, "opponent2"), col(r, "surface"), col(r, "setting"),
            col(r, "start_time"), col(r, "duration"), col(r, "weather"), col(r, "temp"), col(r, "venue"))
    }
}

fun parseGamesJson(text: String): List<ImportRow> {
    val arr = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { o ->
        ImportRow(o.optString("date"), o.optString("opponent"), o.optString("score"), o.optString("notes"), o.optString("result", "Loss"),
            o.optString("mode"), o.optString("partner"), o.optString("opponent2"), o.optString("surface"), o.optString("setting"),
            o.optString("start_time"), o.optString("duration"), o.optString("weather"), o.optString("temp"), o.optString("venue"))
    }
}

/** A row as a game to store, or null if it cannot be one (no date or opponent, a score that does not parse). */
fun ImportRow.toGame(): Game? {
    val date = date.trim(); val opp = opponent.trim(); val score = score.trim()
    if (date.isEmpty() || opp.isEmpty()) return null
    val sets = scoreToSets(score)
    val res = if (sets.isNotEmpty()) runCatching { computeResult(sets).name }.getOrNull() ?: return null else result.ifBlank { "Loss" }
    val doubles = mode.trim().equals("doubles", ignoreCase = true)
    return Game(
        date = date, opponent = opp, result = res, score = score, notes = notes,
        mode = if (doubles) "doubles" else null,
        partner = partner.trim().takeIf { doubles && it.isNotEmpty() }, opponent2 = opponent2.trim().takeIf { doubles && it.isNotEmpty() },
        surface = surface.trim().lowercase().takeIf { it in SURFACES }, setting = setting.trim().lowercase().takeIf { it in SETTINGS },
        startTime = startTime.trim().takeIf { Regex("""\d{1,2}:\d{2}""").matches(it) },
        duration = duration.trim().toIntOrNull()?.takeIf { it > 0 },
        weather = weather.trim().lowercase().takeIf { it in WEATHERS }, temp = temp.trim().toIntOrNull(),
        venue = venue.trim().takeIf { it.isNotEmpty() },
    )
}

/**
 * What makes a match "the same" when importing: the old fields plus, for doubles, who played. The details (surface,
 * weather...) are left out, so importing an older file never duplicates a match whose details were added later.
 */
fun Game.importKey(): List<String> = listOf(date, opponent, result, score ?: "", notes ?: "", opponent2 ?: "", partner ?: "")

private fun csvEscape(v: String): String =
    if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

/** Minimal RFC-4180 parser (quotes, escaped quotes, newlines inside quotes). */
fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cur = StringBuilder()
    var inQuotes = false
    var i = 0
    val t = text.removePrefix("﻿")
    while (i < t.length) {
        val c = t[i]
        when {
            inQuotes && c == '"' && i + 1 < t.length && t[i + 1] == '"' -> { cur.append('"'); i++ }
            c == '"' -> inQuotes = !inQuotes
            !inQuotes && c == ',' -> { row.add(cur.toString()); cur.clear() }
            !inQuotes && (c == '\n' || c == '\r') -> {
                if (c == '\r' && i + 1 < t.length && t[i + 1] == '\n') i++
                row.add(cur.toString()); cur.clear()
                if (row.any { it.isNotEmpty() } || row.size > 1) rows.add(row)
                row = mutableListOf()
            }
            else -> cur.append(c)
        }
        i++
    }
    if (cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString()); rows.add(row) }
    return rows
}
