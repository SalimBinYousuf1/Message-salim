package com.example.telephony

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false
    private var startTimeMillis = 0L

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (isRecording && recorder != null) {
                try {
                    val maxAmp = recorder?.maxAmplitude ?: 0
                    // Normalized to 0f..1f
                    _currentAmplitude.value = (maxAmp / 32767f).coerceIn(0.05f, 1f)
                    val elapsed = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
                    _recordingDurationSeconds.value = elapsed
                } catch (e: Exception) {
                    // ignore
                }
                handler.postDelayed(this, 100)
            }
        }
    }

    fun startRecording(): Boolean {
        try {
            val audioDir = File(context.cacheDir, "audio_notes").apply { mkdirs() }
            val outputFile = File(audioDir, "salim_audio_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            startTimeMillis = System.currentTimeMillis()
            _recordingDurationSeconds.value = 0
            handler.post(amplitudeRunnable)
            return true
        } catch (e: Exception) {
            cancelRecording()
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        isRecording = false
        handler.removeCallbacks(amplitudeRunnable)
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            currentOutputFile
        } catch (e: Exception) {
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        isRecording = false
        handler.removeCallbacks(amplitudeRunnable)
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // ignore
        }
        try {
            recorder?.release()
        } catch (e: Exception) {
            // ignore
        }
        recorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
        _recordingDurationSeconds.value = 0
        _currentAmplitude.value = 0f
    }
}

class AudioPlayerHelper {
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && player.duration > 0) {
                    _progressFraction.value = (player.currentPosition.toFloat() / player.duration.toFloat()).coerceIn(0f, 1f)
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun play(filePath: String, onFinished: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progressFraction.value = 0f
                    handler.removeCallbacks(progressRunnable)
                    onFinished()
                }
                start()
            }
            _isPlaying.value = true
            handler.post(progressRunnable)
        } catch (e: Exception) {
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        handler.removeCallbacks(progressRunnable)
    }

    fun resume() {
        mediaPlayer?.start()
        _isPlaying.value = true
        handler.post(progressRunnable)
    }

    fun stop() {
        handler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        _isPlaying.value = false
        _progressFraction.value = 0f
    }
}
