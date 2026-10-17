package com.powermeter.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powermeter.app.data.local.AppDatabase
import com.powermeter.app.data.local.ChallengeEntity
import com.powermeter.app.data.local.UserPreferencesRepository
import com.powermeter.app.scoring.ScoringEngine
import com.powermeter.app.sensor.MotionSensorManager
import com.powermeter.app.util.FeedbackManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ChallengePhase {
    IDLE,
    COUNTDOWN,
    ACTIVE,
    PAUSED,
    COMPLETED
}

data class ChallengeUiState(
    val phase: ChallengePhase = ChallengePhase.IDLE,
    val countdownRemaining: Int = 3,
    val durationSeconds: Int = 30,
    val secondsRemaining: Int = 30,
    val repetitionCount: Int = 0,
    val currentIntensity: Float = 0f,
    val peakIntensity: Float = 0f,
    val cadenceRpm: Float = 0f,
    val consistencyPercentage: Float = 100f,
    val liveScore: Int = 0,
    val earnedExp: Int = 0,
    val isSensorSupported: Boolean = true
)

class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val prefs = UserPreferencesRepository(application)
    private val sensorManager = MotionSensorManager(application)
    private val feedback = FeedbackManager(application)

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var sensorObserverJob: Job? = null
    private val intensityHistory = mutableListOf<Float>()

    init {
        viewModelScope.launch {
            val dur = prefs.selectedDuration.first()
            _uiState.value = _uiState.value.copy(
                durationSeconds = dur,
                secondsRemaining = dur
            )
        }
    }

    fun startChallenge() {
        viewModelScope.launch {
            val sound = prefs.isSoundEnabled.first()
            _uiState.value = _uiState.value.copy(phase = ChallengePhase.COUNTDOWN, countdownRemaining = 3)

            for (i in 3 downTo 1) {
                _uiState.value = _uiState.value.copy(countdownRemaining = i)
                feedback.playCountdownBeep(sound, isFinal = false)
                delay(1000)
            }
            feedback.playCountdownBeep(sound, isFinal = true)

            val dur = prefs.selectedDuration.first()
            val lowIntensity = prefs.isLowIntensity.first()
            intensityHistory.clear()

            _uiState.value = _uiState.value.copy(
                phase = ChallengePhase.ACTIVE,
                durationSeconds = dur,
                secondsRemaining = dur,
                repetitionCount = 0,
                liveScore = 0
            )

            sensorManager.startListening(lowIntensity)
            observeSensors()
            startTimer()
        }
    }

    private fun observeSensors() {
        sensorObserverJob?.cancel()
        sensorObserverJob = viewModelScope.launch {
            val haptic = prefs.isHapticEnabled.first()
            var prevReps = 0

            sensorManager.motionMetrics.collect { metrics ->
                if (!metrics.isSensorAvailable) {
                    _uiState.value = _uiState.value.copy(isSensorSupported = false)
                    return@collect
                }

                if (metrics.currentIntensity > 0.5f) {
                    intensityHistory.add(metrics.currentIntensity)
                }

                if (metrics.repetitionCount > prevReps) {
                    feedback.playRepHaptic(haptic)
                    prevReps = metrics.repetitionCount
                }

                val avgIntensity = if (intensityHistory.isNotEmpty()) intensityHistory.average().toFloat() else 0f
                val score = ScoringEngine.calculateScore(
                    repetitions = metrics.repetitionCount,
                    averageIntensity = avgIntensity,
                    cadenceRpm = metrics.cadenceRpm,
                    consistencyPercentage = metrics.consistencyPercentage,
                    durationSeconds = _uiState.value.durationSeconds
                )

                _uiState.value = _uiState.value.copy(
                    repetitionCount = metrics.repetitionCount,
                    currentIntensity = metrics.currentIntensity,
                    peakIntensity = metrics.peakIntensity,
                    cadenceRpm = metrics.cadenceRpm,
                    consistencyPercentage = metrics.consistencyPercentage,
                    liveScore = score
                )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.secondsRemaining > 0) {
                delay(1000)
                val remaining = _uiState.value.secondsRemaining - 1
                _uiState.value = _uiState.value.copy(secondsRemaining = remaining)
            }
            if (_uiState.value.secondsRemaining <= 0) {
                finishChallenge()
            }
        }
    }

    fun pauseChallenge() {
        if (_uiState.value.phase == ChallengePhase.ACTIVE) {
            timerJob?.cancel()
            sensorManager.stopListening()
            _uiState.value = _uiState.value.copy(phase = ChallengePhase.PAUSED)
        }
    }

    fun resumeChallenge() {
        if (_uiState.value.phase == ChallengePhase.PAUSED) {
            viewModelScope.launch {
                val lowIntensity = prefs.isLowIntensity.first()
                sensorManager.startListening(lowIntensity)
                _uiState.value = _uiState.value.copy(phase = ChallengePhase.ACTIVE)
                startTimer()
            }
        }
    }

    fun stopChallenge() {
        finishChallenge()
    }

    private fun finishChallenge() {
        timerJob?.cancel()
        sensorObserverJob?.cancel()
        sensorManager.stopListening()

        val state = _uiState.value
        val exp = ScoringEngine.calculateExpGained(state.liveScore)

        _uiState.value = state.copy(
            phase = ChallengePhase.COMPLETED,
            earnedExp = exp
        )

        viewModelScope.launch {
            if (state.repetitionCount > 0 || state.liveScore > 0) {
                db.challengeDao().insertChallenge(
                    ChallengeEntity(
                        durationSeconds = state.durationSeconds,
                        repetitionCount = state.repetitionCount,
                        score = state.liveScore,
                        peakIntensity = state.peakIntensity,
                        avgCadenceRpm = state.cadenceRpm,
                        consistencyPercentage = state.consistencyPercentage,
                        expEarned = exp
                    )
                )
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = ChallengeUiState(
            phase = ChallengePhase.IDLE,
            durationSeconds = _uiState.value.durationSeconds,
            secondsRemaining = _uiState.value.durationSeconds
        )
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.stopListening()
        feedback.release()
    }
}
