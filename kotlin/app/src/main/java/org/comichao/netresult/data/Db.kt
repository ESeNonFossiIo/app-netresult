package org.comichao.netresult.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Schema mirrors the Python app's SQLite tables (games / draft / prefs).
@Entity(
    tableName = "games",
    indices = [Index(value = ["opponent", "date"]), Index(value = ["date"])],
)
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val opponent: String,
    val result: String,
    val score: String?,
    val notes: String? = null,
)

@Entity(tableName = "draft")
data class Draft(
    @PrimaryKey val id: Int = 1,
    val date: String = "",
    val opponent: String = "",
    val sets: String = "",
    val notes: String = "",
)

@Entity(tableName = "prefs")
data class Pref(@PrimaryKey val key: String, val value: String)

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Game>

    @Query("SELECT DISTINCT opponent FROM games ORDER BY opponent")
    fun observeOpponents(): Flow<List<String>>

    @Insert suspend fun insert(game: Game): Long
    @Insert suspend fun insertAll(games: List<Game>)
    @Update suspend fun update(game: Game)
    @Query("DELETE FROM games WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM games") suspend fun deleteAll()

    @Query("SELECT * FROM draft WHERE id = 1")
    fun observeDraft(): Flow<Draft?>
    @Query("SELECT * FROM draft WHERE id = 1")
    suspend fun getDraft(): Draft?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveDraft(draft: Draft)
    @Query("DELETE FROM draft") suspend fun clearDraft()

    @Query("SELECT value FROM prefs WHERE `key` = :key")
    suspend fun getPref(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun setPref(pref: Pref)
}

/**
 * Users keep their games across app updates because the database lives in the app's private storage
 * and is only ever upgraded with explicit migrations (never `fallbackToDestructiveMigration`).
 * When you change an entity: bump [VERSION], add a Migration in [get], and commit the new
 * `app/schemas/.../<VERSION>.json` that Room generates (a unit test fails if it is missing).
 */
/** Add one Migration per schema bump, e.g. `object : Migration(1, 2) { ... }`. */
private val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf()

@Database(entities = [Game::class, Draft::class, Pref::class], version = AppDatabase.VERSION, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): GameDao

    companion object {
        const val VERSION = 1

        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "netresult-room.db")
                .addMigrations(*MIGRATIONS)
                .build().also { instance = it }
        }
    }
}
