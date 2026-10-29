package org.comichao.netresult.wear

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.comichao.netresult.LiveBus
import org.comichao.netresult.LiveLink
import org.comichao.netresult.LiveState
import org.comichao.netresult.WatchSession

enum class Mode { Home, Match, Warmup }

class WatchViewModel(app: Application) : AndroidViewModel(app) {
    var mode by mutableStateOf(Mode.Home); private set
    var heartRate by mutableIntStateOf(0); private set
    var kcal by mutableDoubleStateOf(0.0); private set
    var startMillis by mutableLongStateOf(0L); private set
    /** Result of the last send: null = nothing yet, true = phone has it, false = queued for later. */
    var lastSent by mutableStateOf<Boolean?>(null); private set
    var vitalsOk by mutableStateOf(true); private set
    var finishing by mutableStateOf(false); private set

    /** The match in progress, mirrored with the phone. Also set when the phone started it. */
    val live: StateFlow<LiveState?> = LiveBus.state

    private val samples = mutableListOf<Pair<Long, Int>>()
    private var lastOpponent = "Opponent"
    private val link = PhoneLink(app)
    private val vitals = Vitals(app,
        { bpm -> heartRate = bpm; samples += System.currentTimeMillis() to bpm },
        { kcal = it })

    val canUndo get() = live.value?.history?.isNotEmpty() == true

    init {
        LiveBus.load(app)
        viewModelScope.launch { link.retryPending(); LiveLink.request(app) }   // is the phone already scoring?
        viewModelScope.launch { live.collect { l -> if (l != null) lastOpponent = l.opponent } }
        viewModelScope.launch { LiveBus.ended.collect { saved -> onEndedElsewhere(saved) } }
    }

    private fun push(s: LiveState) {
        LiveBus.set(getApplication(), s)
        viewModelScope.launch { LiveLink.push(getApplication(), s) }
    }

    private fun resetVitals(at: Long) { samples.clear(); heartRate = 0; kcal = 0.0; startMillis = at; lastSent = null; finishing = false }

    fun start(newMode: Mode) {
        val now = System.currentTimeMillis()
        resetVitals(now)
        if (newMode == Mode.Match) push(LiveState(rev = now, opponent = "Opponent", startedAt = now, watch = true))
        mode = newMode
        viewModelScope.launch { vitalsOk = vitals.start() }
    }

    /** Join the match the phone (or an earlier run of this app) already has going. */
    fun join() {
        val l = live.value ?: return
        resetVitals(l.startedAt)
        push(l.withWatch())
        mode = Mode.Match
        viewModelScope.launch { vitalsOk = vitals.start() }
    }

    fun point(me: Boolean) { live.value?.let { push(it.point(me)) }; finishing = false }
    fun undo() { live.value?.let { push(it.undo()) }; finishing = false }

    /** First tap on an unfinished match asks for a second one, so a stray tap cannot end it. */
    fun finishMatch() {
        val l = live.value ?: return
        if (!l.match.over && !finishing) { finishing = true; return }
        end(WatchSession.MATCH, l.match.tokens(), l.opponent)
    }

    fun endWarmup() = end(WatchSession.WARMUP, emptyList(), "")

    fun discard() {
        val inMatch = mode == Mode.Match
        mode = Mode.Home
        viewModelScope.launch {
            vitals.stop()
            if (inMatch) { LiveBus.end(getApplication(), false); LiveLink.end(getApplication(), false) }
        }
    }

    private fun end(kind: String, sets: List<String>, opponent: String) {
        val session = WatchSession(kind, opponent.ifBlank { "Opponent" }, sets, startMillis, System.currentTimeMillis(), samples.toList(), kcal.takeIf { it > 0 })
        mode = Mode.Home
        viewModelScope.launch {
            vitals.stop()
            lastSent = link.send(session)           // the phone saves the match and closes its live match
            if (kind == WatchSession.MATCH) LiveBus.set(getApplication(), null)
        }
    }

    /** The phone finished (saved) or discarded the match: stop here too, and hand over the vitals if it was saved. */
    private fun onEndedElsewhere(saved: Boolean) {
        if (mode != Mode.Match) return
        mode = Mode.Home
        viewModelScope.launch {
            vitals.stop()
            if (saved) lastSent = link.send(WatchSession(WatchSession.MATCH, lastOpponent, emptyList(), startMillis,
                System.currentTimeMillis(), samples.toList(), kcal.takeIf { it > 0 }))
        }
    }
}
