package org.comichao.netresult.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.comichao.netresult.*
import org.comichao.netresult.data.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app, AppDatabase.get(app))

    val games = repo.games.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val opponents = repo.opponents.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val hasDraft = repo.draft.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val stats = games.map { computeStats(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    private val _palette = MutableStateFlow("ball")
    val palette: StateFlow<String> = _palette

    fun setPalette(key: String) {
        _palette.value = key
        viewModelScope.launch { repo.setPref("palette", key) }
    }

    init {
        viewModelScope.launch {
            _palette.value = repo.getPref("palette", "ball")
            repo.migrateLegacyDb()
            val lang = repo.getPref("lang")
            if (lang.isNotEmpty()) applyLang(lang)
        }
    }

    fun setLang(code: String) {
        viewModelScope.launch { repo.setPref("lang", code) }
        applyLang(code)
    }

    private fun applyLang(code: String) =
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))

    fun addGame(date: String, opp: String, sets: List<String>, notes: String) = viewModelScope.launch {
        repo.addGame(date, opp, sets, notes); repo.clearDraft()
    }
    fun updateGame(old: Game, date: String, opp: String, sets: List<String>, notes: String) =
        viewModelScope.launch { repo.updateGame(old, date, opp, sets, notes) }
    fun deleteGame(id: Long) = viewModelScope.launch { repo.deleteGame(id) }
    fun resetAll() = viewModelScope.launch { repo.resetAll() }

    fun saveDraft(date: String, opp: String, sets: List<String>, notes: String) =
        viewModelScope.launch { repo.saveDraft(date, opp, sets, notes) }
    suspend fun loadDraft() = repo.getDraft()
    fun clearDraft() = viewModelScope.launch { repo.clearDraft() }

    suspend fun exportText(json: Boolean) = if (json) repo.exportJson() else repo.exportCsv()
    suspend fun importText(text: String, json: Boolean) =
        if (json) repo.importJson(text) else repo.importCsv(text)
}
