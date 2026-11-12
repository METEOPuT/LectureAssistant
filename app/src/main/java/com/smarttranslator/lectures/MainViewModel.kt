package com.smarttranslator.lectures

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smarttranslator.lectures.audio.AudioCaptureManager
import com.smarttranslator.lectures.audio.MicDeviceHelper
import com.smarttranslator.lectures.data.AppDatabase
import com.smarttranslator.lectures.db.entities.Lecture
import com.smarttranslator.lectures.db.entities.TranscriptSegment
import com.smarttranslator.lectures.summarizer.OpenRouterSummarizer
import com.smarttranslator.lectures.summarizer.QwenSummarizer
import com.smarttranslator.lectures.summarizer.Summarizer
import com.smarttranslator.lectures.vosk.VoskManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).lectureDao()
    private val vosk = VoskManager(app)
    private val audio = AudioCaptureManager()

    // Теперь суммаризатор выбирается динамически
    private var summarizer: Summarizer? = null
    private val scope = viewModelScope

    private val _status = MutableStateFlow<AppStatus>(AppStatus.Idle)
    val status: StateFlow<AppStatus> = _status

    private val _currentLectureId = MutableStateFlow<Long?>(null)
    val currentLectureId: StateFlow<Long?> = _currentLectureId

    val segments: StateFlow<List<TranscriptSegment>> = _currentLectureId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else dao.observeSegments(id)
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary

    val lectures: StateFlow<List<Lecture>> = dao.observeAllLectures()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var summaryJob: Job? = null
    private var lectureStartTime: Long = 0L

    fun startLecture(
        title: String,
        intervalMinutes: Int,
        preferredMicId: Int?,
        useApi: Boolean,
        apiKey: String,
        prompt: String
    ) {
        if (_status.value is AppStatus.Recording || _status.value is AppStatus.Processing) return

        scope.launch {
            _status.value = AppStatus.LoadingModel

            // 1. Инициализируем суммаризатор в зависимости от выбора
            summarizer = if (useApi) {
                OpenRouterSummarizer(apiKey = apiKey, customInstruction = prompt)
            } else {
                QwenSummarizer(customInstruction = prompt) { progress ->
                    _status.value = AppStatus.DownloadingModel(progress)
                }
            }

            // 2. Загружаем Vosk
            val load = vosk.load()
            if (load.isFailure) {
                _status.value = AppStatus.Error("Ошибка Vosk: ${load.exceptionOrNull()?.message}")
                return@launch
            }

            // 3. Создаем лекцию в БД
            val now = System.currentTimeMillis()
            lectureStartTime = now
            val id = dao.insertLecture(
                Lecture(title = title, startedAt = now, isActive = true, intervalMinutes = intervalMinutes)
            )
            _currentLectureId.value = id
            _summary.value = ""

            // 4. Запускаем запись звука
            audio.start(scope, preferredDevice = resolveMic(preferredMicId)) { buf, len ->
                if (vosk.accept(buf, len)) {
                    val text = vosk.getResult()
                    if (!text.isNullOrBlank()) {
                        scope.launch {
                            dao.insertSegment(TranscriptSegment(lectureId = id, text = text, timestamp = System.currentTimeMillis()))
                        }
                    }
                }
            }
            _status.value = AppStatus.Recording
            scheduleSummarization(id, intervalMinutes)
        }
    }

    fun stopLecture() {
        summaryJob?.cancel()
        audio.stop()
        val id = _currentLectureId.value ?: return
        _currentLectureId.value = null
        _status.value = AppStatus.Idle
        scope.launch {
            dao.getLecture(id)?.let {
                dao.updateLecture(it.copy(endedAt = System.currentTimeMillis(), isActive = false))
            }
        }
    }

    fun deleteLecture(lecture: Lecture) {
        scope.launch {
            if (_currentLectureId.value == lecture.id) stopLecture()
            dao.deleteLecture(lecture)
        }
    }

    fun openLecture(lecture: Lecture) {
        stopLecture()
        _currentLectureId.value = lecture.id
        _summary.value = lecture.summary
        _status.value = AppStatus.Idle
    }

    private fun resolveMic(preferredId: Int?): android.media.AudioDeviceInfo? {
        if (preferredId == null) return null
        return MicDeviceHelper(getApplication()).inputs().firstOrNull { it.id == preferredId }
    }

    private fun scheduleSummarization(lectureId: Long, intervalMinutes: Int) {
        summaryJob?.cancel()
        summaryJob = scope.launch {
            while (isActive) {
                delay(intervalMinutes * 60_000L)
                if (_currentLectureId.value != lectureId) break
                _status.value = AppStatus.Processing
                try {
                    val now = System.currentTimeMillis()
                    val from = (now - intervalMinutes * 60_000L - 60_000L).coerceAtLeast(lectureStartTime)
                    val chunk = dao.getSegmentsInRange(lectureId, from, now)
                    val text = chunk.joinToString(" ") { it.text }
                    if (text.isBlank()) { _status.value = AppStatus.Recording; continue }

                    // Безопасный вызов (summarizer не null, так как мы его инициализировали в startLecture)
                    val result = summarizer?.summarize(text)

                    if (result != null) {
                        result.onSuccess { addition ->
                            val lec = dao.getLecture(lectureId) ?: return@onSuccess
                            val newSum = if (lec.summary.isBlank()) addition else lec.summary + "\n\n" + addition
                            dao.updateLecture(lec.copy(summary = newSum))
                            _summary.value = newSum
                            _status.value = AppStatus.Recording
                        }.onFailure {
                            _status.value = AppStatus.Warning("Ошибка: ${it.message}")
                        }
                    }
                } finally {
                    if (_status.value is AppStatus.Processing) _status.value = AppStatus.Recording
                }
            }
        }
    }

    override fun onCleared() {
        audio.stop()
        vosk.close()
        super.onCleared()
    }
}