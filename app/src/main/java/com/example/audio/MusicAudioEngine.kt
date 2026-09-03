package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import com.example.data.model.EqualizerSettings
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class MusicAudioEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _visualizerBands = MutableStateFlow(FloatArray(16) { 0.1f })
    val visualizerBands: StateFlow<FloatArray> = _visualizerBands.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    var onCompletionListener: (() -> Unit)? = null

    private var currentSong: Song? = null
    private var equalizerSettings = EqualizerSettings()

    fun updateEqualizer(settings: EqualizerSettings) {
        this.equalizerSettings = settings
        applyEqualizerSettings()
    }

    private fun applyEqualizerSettings() {
        try {
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            val minBandLevel = eq.bandLevelRange[0]
            val maxBandLevel = eq.bandLevelRange[1]
            val range = maxBandLevel - minBandLevel

            for (i in 0 until numBands) {
                val weight = when {
                    i < numBands / 3 -> equalizerSettings.bassBoost
                    i < (numBands * 2) / 3 -> equalizerSettings.vocalClarity
                    else -> equalizerSettings.treble
                }
                val targetLevel = (minBandLevel + weight * range).toInt().toShort()
                eq.setBandLevel(i.toShort(), targetLevel)
            }
        } catch (e: Exception) {
            Log.w("MusicAudioEngine", "Error applying equalizer settings", e)
        }
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            applyEqualizerSettings()
        } catch (e: Exception) {
            Log.w("MusicAudioEngine", "Hardware equalizer unavailable", e)
            equalizer = null
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            mediaPlayer?.let { mp ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val params = mp.playbackParams
                    params.speed = speed
                    mp.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w("MusicAudioEngine", "Could not set playback speed", e)
        }
    }

    fun playSong(song: Song, startFromPositionSec: Float = 0f) {
        stop()
        currentSong = song
        _currentPositionSec.value = startFromPositionSec

        if (song.uri.isBlank()) {
            Log.w("MusicAudioEngine", "Song has empty URI: ${song.title}")
            _isPlaying.value = false
            return
        }

        try {
            val uri = Uri.parse(song.uri)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context.applicationContext, uri)
                setOnPreparedListener { mp ->
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                            val params = mp.playbackParams
                            params.speed = _playbackSpeed.value
                            mp.playbackParams = params
                        }

                        if (startFromPositionSec > 0f) {
                            val targetMs = (startFromPositionSec * 1000).toInt()
                            mp.seekTo(targetMs)
                        }

                        mp.start()
                        _isPlaying.value = true
                        setupEqualizer(mp.audioSessionId)
                        startPositionTracker()
                    } catch (ex: Exception) {
                        Log.e("MusicAudioEngine", "Error starting prepared player", ex)
                        _isPlaying.value = false
                    }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionSec.value = song.durationSeconds.toFloat()
                    onCompletionListener?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MusicAudioEngine", "MediaPlayer playback error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
            }

            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e("MusicAudioEngine", "Failed to initialize media player for: ${song.title}", e)
            _isPlaying.value = false
        }
    }

    fun pause() {
        _isPlaying.value = false
        positionJob?.cancel()
        positionJob = null
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            val player = mediaPlayer
            if (player != null) {
                player.start()
                _isPlaying.value = true
                startPositionTracker()
            } else {
                currentSong?.let { playSong(it, _currentPositionSec.value) }
            }
        } catch (e: Exception) {
            Log.e("MusicAudioEngine", "Error resuming playback", e)
            currentSong?.let { playSong(it, _currentPositionSec.value) }
        }
    }

    fun seekTo(seconds: Float) {
        val song = currentSong ?: return
        val targetSec = seconds.coerceIn(0f, song.durationSeconds.toFloat())
        _currentPositionSec.value = targetSec
        try {
            mediaPlayer?.seekTo((targetSec * 1000).toInt())
        } catch (e: Exception) {
            Log.w("MusicAudioEngine", "Error seeking player", e)
        }
    }

    fun stop() {
        _isPlaying.value = false
        positionJob?.cancel()
        positionJob = null
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        _visualizerBands.value = FloatArray(16) { 0.05f }
    }

    // Trigger DJ Scratch effect when vinyl record is scratched by user
    fun triggerScratchEffect() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    val currentPos = mp.currentPosition
                    val delta = ((Math.random() - 0.5) * 600).toInt()
                    val newPos = (currentPos + delta).coerceAtLeast(0)
                    mp.seekTo(newPos)
                }
            }
        } catch (_: Exception) {}

        // Spike visualizer bands temporarily for responsive scratch feedback
        _visualizerBands.value = FloatArray(16) {
            (0.35f + (Math.random() * 0.65f).toFloat()).coerceIn(0.1f, 1.0f)
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            var tick = 0
            while (isActive && _isPlaying.value) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val currentMs = mp.currentPosition
                        _currentPositionSec.value = currentMs / 1000f

                        // Generate reactive spectrum visualizer bands based on audio energy & EQ
                        tick++
                        val baseFreq = tick * 0.2
                        val bassBoost = equalizerSettings.bassBoost * 0.35f
                        val bands = FloatArray(16) { i ->
                            val wave = (sin(baseFreq + i * 0.45) * 0.5 + 0.5).toFloat()
                            val weight = when {
                                i < 5 -> equalizerSettings.bassBoost
                                i < 11 -> equalizerSettings.vocalClarity
                                else -> equalizerSettings.treble
                            }
                            val eqFactor = (0.5f + weight).coerceIn(0.2f, 1.5f)
                            val boost = if (i < 4) bassBoost else 0f
                            (wave * 0.65f * eqFactor + boost + (Math.random() * 0.15f).toFloat()).coerceIn(0.08f, 1.0f)
                        }
                        _visualizerBands.value = bands
                    }
                } catch (_: Exception) {}
                delay(100L)
            }
        }
    }
}
