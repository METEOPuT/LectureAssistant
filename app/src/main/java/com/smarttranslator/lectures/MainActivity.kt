package com.smarttranslator.lectures

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarttranslator.lectures.audio.MicDeviceHelper
import com.smarttranslator.lectures.databinding.ActivityMainBinding
import com.smarttranslator.lectures.ui.LectureListAdapter
import com.smarttranslator.lectures.ui.TranscriptAdapter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private val transcriptAdapter = TranscriptAdapter()
    private val lectureAdapter by lazy {
        LectureListAdapter(
            onPick = { lec -> onPickLecture(lec) },
            onDelete = { lec -> vm.deleteLecture(lec) }
        )
    }
    private val micHelper by lazy { MicDeviceHelper(this) }
    private val micDevices = mutableListOf<AudioDeviceInfo>()
    private var selectedMicId: Int? = null
    private var settingsExpanded = false
    private var autoScroll = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclers()
        setupSplitter()
        setupSettingsBlock()
        setupDrawer()
        setupActions()
        observe()
        requestMicPermissionIfNeeded()
    }

    private val micPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this, "Без микрофона запись невозможна", Toast.LENGTH_LONG).show()
    }
    private fun requestMicPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupRecyclers() {
        binding.transcriptRecycler.layoutManager = LinearLayoutManager(this)
        binding.transcriptRecycler.adapter = transcriptAdapter
        binding.drawerRecycler.layoutManager = LinearLayoutManager(this)
        binding.drawerRecycler.adapter = lectureAdapter

        binding.transcriptRecycler.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    autoScroll = lm.findLastVisibleItemPosition() >= rv.adapter!!.itemCount - 1
                    updateJumpToBottom()
                }
            }
        })
        binding.btnJumpToBottom.setOnClickListener {
            autoScroll = true
            binding.transcriptRecycler.scrollToPosition(transcriptAdapter.itemCount - 1)
            updateJumpToBottom()
        }
    }

    private fun updateJumpToBottom() {
        binding.btnJumpToBottom.visibility = if (autoScroll) View.GONE else View.VISIBLE
    }

    private fun setupSplitter() {
        binding.splitter.onDrag = { dy ->
            val total = binding.transcriptBlock.height + binding.notesBlock.height
            if (total != 0) {
                val newTranscript = (binding.transcriptBlock.height + dy).roundToInt()
                    .coerceIn((total * 0.2f).toInt(), (total * 0.8f).toInt())

                val params1 = binding.transcriptBlock.layoutParams as ViewGroup.LayoutParams
                params1.height = newTranscript
                binding.transcriptBlock.layoutParams = params1

                val params2 = binding.notesBlock.layoutParams as ViewGroup.LayoutParams
                params2.height = total - newTranscript
                binding.notesBlock.layoutParams = params2
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSettingsBlock() {
        val gesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dy = e2.y - e1.y
                if (dy > 60 && !settingsExpanded) { expandSettings(true); return true }
                if (dy < -60 && settingsExpanded) { expandSettings(false); return true }
                return false
            }
        })
        binding.settingsBlock.setOnTouchListener { _, ev -> gesture.onTouchEvent(ev) }
        binding.btnExpandToggle.setOnClickListener { expandSettings(!settingsExpanded) }
    }

    private fun expandSettings(expand: Boolean) {
        settingsExpanded = expand
        binding.expandableContent.visibility = if (expand) View.VISIBLE else View.GONE
        binding.btnExpandToggle.text = if (expand) "Свернуть ▲" else "Развернуть ▼"
    }

    private fun setupDrawer() {
        binding.btnOpenDrawer.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
    }

    private fun onPickLecture(lec: com.smarttranslator.lectures.db.entities.Lecture) {
        vm.openLecture(lec)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupActions() {
        // --- Настройка микрофонов ---
        micDevices.addAll(micHelper.inputs())
        val labels = micDevices.map { micHelper.label(it) }
        binding.spinnerMic.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        if (labels.isNotEmpty()) selectedMicId = micDevices[0].id
        binding.spinnerMic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMicId = micDevices.getOrNull(position)?.id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- НОВОЕ: Настройка выбора модели ---
        // --- НОВОЕ: Настройка выбора модели ---
        val models = arrayOf("Локальная Qwen 0.5B (оффлайн)", "OpenRouter API (онлайн)")
        binding.spinnerModel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, models)
        binding.spinnerModel.setSelection(1) // По умолчанию выбираем API (позиция 1)
        binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.etApiKey.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnStart.setOnClickListener {
            val interval = binding.etInterval.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 5
            val title = "Лекция " + android.text.format.DateFormat.format("dd.MM HH:mm", System.currentTimeMillis())

            val isApiSelected = binding.spinnerModel.selectedItemPosition == 1

            val apiKey = binding.etApiKey.text.toString().trim()
            val prompt = AppConfig.DEFAULT_PROMPT

            if (isApiSelected && apiKey.isBlank()) {
                Toast.makeText(this, "Введите API-ключ OpenRouter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            vm.startLecture(title, interval, selectedMicId, isApiSelected, apiKey, prompt)
        }

        binding.btnFinish.setOnClickListener {
            vm.stopLecture()
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private var currentSegments: List<com.smarttranslator.lectures.db.entities.TranscriptSegment> = emptyList()
    private var currentPartial: String? = null

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Слушаем финальные сегменты
                launch {
                    vm.segments.collect { segs ->
                        currentSegments = segs
                        transcriptAdapter.submit(currentSegments, currentPartial)

                        // Прячем заглушку, если есть хоть какой-то текст
                        binding.tvTranscriptEmpty.visibility = if (segs.isEmpty() && currentPartial.isNullOrBlank()) View.VISIBLE else View.GONE

                        // Автоскролл вниз
                        if (autoScroll && transcriptAdapter.itemCount > 0) {
                            binding.transcriptRecycler.post {
                                binding.transcriptRecycler.scrollToPosition(transcriptAdapter.itemCount - 1)
                            }
                        }
                    }
                }

                // Слушаем партиал
                launch {
                    vm.partialText.collect { text ->
                        currentPartial = text
                        transcriptAdapter.submit(currentSegments, currentPartial)

                        binding.tvTranscriptEmpty.visibility = if (currentSegments.isEmpty() && text.isNullOrBlank()) View.VISIBLE else View.GONE

                        // Автоскролл вниз при появлении партиала
                        if (autoScroll && transcriptAdapter.itemCount > 0) {
                            binding.transcriptRecycler.post {
                                binding.transcriptRecycler.scrollToPosition(transcriptAdapter.itemCount - 1)
                            }
                        }
                    }
                }

                launch { vm.summary.collect { s -> binding.notesText.text = s.ifBlank { "Здесь появится конспект…" } } }
                launch { vm.lectures.collect { lectureAdapter.submit(it) } }
                launch { vm.status.collect { st -> renderStatus(st) } }
            }
        }
    }

    private fun renderStatus(status: AppStatus) {
        val (label, colorRes) = when (status) {
            is AppStatus.Idle -> "Готов" to R.color.textSecondary
            is AppStatus.LoadingModel -> "Загрузка модели…" to R.color.warning
            is AppStatus.Recording -> "● Идёт запись" to R.color.recording
            is AppStatus.Processing -> "Обработка конспекта…" to R.color.accent
            is AppStatus.Warning -> "⚠ ${status.message}" to R.color.warning
            is AppStatus.Error -> "✕ ${status.message}" to R.color.recording
            is AppStatus.DownloadingModel -> {
                val percent = (status.progress * 100).toInt()
                "Скачивание модели: $percent%" to R.color.accent
            }
        }
        binding.tvStatus.text = label
        val color = ContextCompat.getColor(this, colorRes)
        binding.tvStatus.setTextColor(color)
        binding.dotStatus.background?.setTint(color)

        // Показываем крутящийся индикатор, если идет обработка или скачивание
        val showProgress = status is AppStatus.Processing || status is AppStatus.DownloadingModel || status is AppStatus.LoadingModel
        binding.progressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
        // Прячем цветную точку, пока крутится индикатор, чтобы не было визуального шума
        binding.dotStatus.visibility = if (showProgress) View.GONE else View.VISIBLE

        binding.btnStart.isEnabled = status is AppStatus.Idle
        binding.btnFinish.isEnabled = status is AppStatus.Recording || status is AppStatus.Warning
    }
}