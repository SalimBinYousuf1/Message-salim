package com.example.telephony

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.net.Uri
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

    private val _activeUri = MutableStateFlow<Uri?>(null)
    val activeUri: StateFlow<Uri?> = _activeUri.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying && player.duration > 0) {
                    _currentPositionMs.value = player.currentPosition
                    _durationMs.value = player.duration
                    _progressFraction.value = (player.currentPosition.toFloat() / player.duration.toFloat()).coerceIn(0f, 1f)
                    handler.postDelayed(this, 60)
                }
            }
        }
    }

    fun playUri(context: Context, uri: Uri, onFinished: () -> Unit = {}) {
        if (_activeUri.value == uri && mediaPlayer != null) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()
        try {
            _activeUri.value = uri
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                _durationMs.value = duration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply { speed = _playbackSpeed.value }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progressFraction.value = 0f
                    _currentPositionMs.value = 0
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

    fun toggleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.playbackParams = PlaybackParams().apply { speed = nextSpeed }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun seekTo(fraction: Float) {
        mediaPlayer?.let { player ->
            if (player.duration > 0) {
                val targetMs = (fraction * player.duration).toInt()
                player.seekTo(targetMs)
                _progressFraction.value = fraction.coerceIn(0f, 1f)
                _currentPositionMs.value = targetMs
            }
        }
    }

    fun play(filePath: String, onFinished: () -> Unit = {}) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                _durationMs.value = duration
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progressFraction.value = 0f
                    _currentPositionMs.value = 0
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
        _activeUri.value = null
        _isPlaying.value = false
        _progressFraction.value = 0f
        _currentPositionMs.value = 0
    }
}
