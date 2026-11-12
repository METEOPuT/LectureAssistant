package com.smarttranslator.lectures.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class AudioCaptureManager {
    private val SAMPLE_RATE = 16000
    private var audioRecord: AudioRecord? = null
    private val running = AtomicBoolean(false)
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    fun start(
        scope: CoroutineScope,
        preferredDevice: AudioDeviceInfo?,
        onChunk: (ByteArray, Int) -> Unit
    ) {
        if (running.get()) return
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf, SAMPLE_RATE / 2)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioCapture", "AudioRecord init failed")
            record.release()
            return
        }
        if (preferredDevice != null) record.preferredDevice = preferredDevice
        audioRecord = record
        record.startRecording()
        running.set(true)

        job = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(bufferSize)
            while (isActive && running.get()) {
                val read = audioRecord?.read(buf, 0, buf.size) ?: -1
                if (read > 0) onChunk(buf.copyOf(read), read)
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
    }
}