package org.comichao.netresult.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.comichao.netresult.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Repository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.dao()

    val games = dao.observeAll()
    val opponents = dao.observeOpponents()
    val draft = dao.observeDraft()

    suspend fun addGame(date: String, opponent: String, sets: List<String>, notes: String): Long {
        require(sets.isNotEmpty()) { "At least one set score is required." }
        return dao.insert(Game(date = date, opponent = opponent, result = computeResult(sets).name,
            score = setsToScore(sets), notes = notes))
    }

    suspend fun updateGame(old: Game, date: String, opponent: String, sets: List<String>, notes: String) {
        require(sets.isNotEmpty()) { "At least one set score is required." }
        dao.update(old.copy(date = date, opponent = opponent, result = computeResult(sets).name,
            score = setsToScore(sets), notes = notes))
    }

    suspend fun deleteGame(id: Long) = dao.delete(id)
    suspend fun resetAll() { dao.deleteAll(); dao.clearDraft() }

    suspend fun saveDraft(date: String, opponent: String, sets: List<String>, notes: String) =
        dao.saveDraft(Draft(date = date, opponent = opponent, sets = setsToScore(sets), notes = notes))
    suspend fun getDraft() = dao.getDraft()
    suspend fun clearDraft() = dao.clearDraft()

    suspend fun getPref(key: String, default: String = "") = dao.getPref(key) ?: default
    suspend fun setPref(key: String, value: String) = dao.setPref(Pref(key, value))

    // ---- Export / import (same CSV/JSON layout as the Python app) -------------------------

    suspend fun exportCsv(): String {
        val sb = StringBuilder("id,date,opponent,result,score,notes,created_at\n")
        for (g in dao.getAll()) {
            sb.append(listOf(g.id.toString(), g.date, g.opponent, g.result, g.score ?: "", g.notes ?: "", "")
                .joinToString(",") { csvEscape(it) }).append('\n')
        }
        return sb.toString()
    }

    suspend fun exportJson(): String {
        val arr = JSONArray()
        for (g in dao.getAll()) arr.put(JSONObject()
            .put("id", g.id).put("date", g.date).put("opponent", g.opponent).put("result", g.result)
            .put("score", g.score ?: "").put("notes", g.notes ?: ""))
        return arr.toString(2)
    }

    /** Returns the number of games imported (duplicates are skipped). */
    suspend fun importCsv(text: String): Int {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return 0
        val header = rows[0].map { it.trim() }
        fun col(row: List<String>, name: String) = header.indexOf(name).let { if (it in row.indices) row[it] else "" }
        return importRows(rows.drop(1).map { r ->
            listOf(col(r, "date"), col(r, "opponent"), col(r, "score"), col(r, "notes"), col(r, "result"))
        })
    }

    suspend fun importJson(text: String): Int {
        val arr = runCatching { JSONArray(text) }.getOrNull() ?: return 0
        val rows = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { o ->
            listOf(o.optString("date"), o.optString("opponent"), o.optString("score"),
                o.optString("notes"), o.optString("result", "Loss"))
        }
        return importRows(rows)
    }

    /** rows: date, opponent, score, notes, result */
    private suspend fun importRows(rows: List<List<String>>): Int {
        val seen = dao.getAll().map { key(it.date, it.opponent, it.result, it.score ?: "", it.notes ?: "") }.toMutableSet()
        val toAdd = mutableListOf<Game>()
        for ((d, o, s, n, r) in rows) {
            val date = d.trim(); val opp = o.trim(); val score = s.trim()
            if (date.isEmpty() || opp.isEmpty()) continue
            val sets = scoreToSets(score)
            val result = if (sets.isNotEmpty()) runCatching { computeResult(sets).name }.getOrNull() ?: continue
                else r.ifBlank { "Loss" }
            if (!seen.add(key(date, opp, result, score, n))) continue
            toAdd += Game(date = date, opponent = opp, result = result, score = score, notes = n)
        }
        dao.insertAll(toAdd)
        return toAdd.size
    }

    private fun key(vararg v: String) = v.toList()

    /**
     * One-time migration from the Kivy build: if its SQLite file exists in the app's private
     * storage (same package name, installed as an update), copy its games over.
     */
    suspend fun migrateLegacyDb(): Int = withContext(Dispatchers.IO) {
        if (getPref("legacy_migrated") == "1") return@withContext 0
        val file = listOf("net_result.db", "app/net_result.db").map { File(context.filesDir, it) }.firstOrNull { it.exists() }
        var count = 0
        if (file != null) runCatching {
            val old = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            old.use { c ->
                c.rawQuery("SELECT date, opponent, score, notes, result FROM games", null).use { cur ->
                    val rows = mutableListOf<List<String>>()
                    while (cur.moveToNext()) rows += (0..4).map { cur.getString(it) ?: "" }
                    count = importRows(rows)
                }
                c.rawQuery("SELECT value FROM prefs WHERE key='lang'", null).use { cur ->
                    if (cur.moveToFirst()) setPref("lang", cur.getString(0))
                }
            }
        }
        setPref("legacy_migrated", "1")
        count
    }
}

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
