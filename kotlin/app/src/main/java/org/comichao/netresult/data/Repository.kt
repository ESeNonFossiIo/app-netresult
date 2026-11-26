package org.comichao.netresult.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.comichao.netresult.*
import java.io.File

class Repository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.dao()

    val draft = dao.observeDraft()

    val games = dao.observeAll()
    /** Everyone you have played against, in singles or doubles (names are suggested when you log a match). */
    val opponents = games.map { list -> list.flatMap { listOfNotNull(it.opponent, it.opponent2?.takeIf { _ -> it.isDoubles }) }.distinct().sorted() }
    val partners = games.map { list -> list.mapNotNull { it.partner?.takeIf(String::isNotBlank) }.distinct().sorted() }
    val venues = games.map { list -> list.mapNotNull { it.venue?.takeIf(String::isNotBlank) }.distinct().sorted() }

    suspend fun addGame(info: GameInfo, sets: List<String>): Long {
        require(sets.isNotEmpty()) { "At least one set score is required." }
        return dao.insert(info.toGame(sets))
    }

    suspend fun updateGame(old: Game, info: GameInfo, sets: List<String>) {
        require(sets.isNotEmpty()) { "At least one set score is required." }
        dao.update(info.toGame(sets, old.id))
    }

    suspend fun deleteGame(id: Long) = dao.delete(id)
    suspend fun resetAll() { dao.deleteAll(); dao.clearDraft() }

    suspend fun saveDraft(info: GameInfo, rawSets: List<String>) = dao.saveDraft(Draft(
        date = info.date, opponent = info.opponent, sets = setsToScore(rawSets), notes = info.notes,
        mode = if (info.doubles) "doubles" else null, partner = info.partner.ifBlank { null }, opponent2 = info.opponent2.ifBlank { null },
        surface = info.surface, setting = info.setting, startTime = info.startTime, duration = info.duration,
        weather = info.weather, temp = info.temp, venue = info.venue.ifBlank { null }))
    suspend fun getDraft() = dao.getDraft()
    suspend fun clearDraft() = dao.clearDraft()

    suspend fun getPref(key: String, default: String = "") = dao.getPref(key) ?: default
    suspend fun setPref(key: String, value: String) = dao.setPref(Pref(key, value))

    // ---- Export / import (same CSV/JSON layout as the Python app, with the doubles and detail columns added) ----

    suspend fun exportCsv(): String = gamesToCsv(dao.getAll())
    suspend fun exportJson(): String = gamesToJson(dao.getAll())

    /** Returns the number of games imported (duplicates are skipped). */
    suspend fun importCsv(text: String): Int = importRows(parseGamesCsv(text))
    suspend fun importJson(text: String): Int = importRows(parseGamesJson(text))

    private suspend fun importRows(rows: List<ImportRow>): Int {
        val seen = dao.getAll().map { it.importKey() }.toMutableSet()
        val toAdd = rows.mapNotNull { it.toGame() }.filter { seen.add(it.importKey()) }
        dao.insertAll(toAdd)
        return toAdd.size
    }

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
                    val rows = mutableListOf<ImportRow>()
                    while (cur.moveToNext()) (0..4).map { cur.getString(it) ?: "" }.let { (d, o, sc, n, r) -> rows += ImportRow(d, o, sc, n, r) }
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
