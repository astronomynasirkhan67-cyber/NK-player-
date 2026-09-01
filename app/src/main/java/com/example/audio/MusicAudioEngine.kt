package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MusicAudioEngine {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _visualizerBands = MutableStateFlow(FloatArray(16) { 0.1f })
    val visualizerBands: StateFlow<FloatArray> = _visualizerBands.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var currentSong: Song? = null
    private var currentSampleIndex = 0L
    private var equalizerSettings = EqualizerSettings()
    private val random = Random()

    fun updateEqualizer(settings: EqualizerSettings) {
        this.equalizerSettings = settings
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            audioTrack?.let {
                val targetSampleRate = (sampleRate * speed).toInt().coerceIn(22050, 88200)
                it.playbackRate = targetSampleRate
            }
        } catch (_: Exception) {}
    }

    fun playSong(song: Song, startFromPositionSec: Float = 0f) {
        stop()
        currentSong = song
        currentSampleIndex = (startFromPositionSec * sampleRate).toLong()
        _currentPositionSec.value = startFromPositionSec

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        ) * 2

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.let { track ->
                if (_playbackSpeed.value != 1.0f) {
                    val targetSampleRate = (sampleRate * _playbackSpeed.value).toInt().coerceIn(22050, 88200)
                    track.playbackRate = targetSampleRate
                }
                track.play()
            }
            _isPlaying.value = true

            startSynthesisLoop(song, bufferSize)
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
        } catch (_: Exception) {}
    }

    fun resume() {
        if (currentSong != null && !_isPlaying.value) {
            currentSong?.let { song ->
                try {
                    audioTrack?.play()
                    _isPlaying.value = true
                    val bufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        channelConfig,
                        audioFormat
                    ) * 2
                    startSynthesisLoop(song, bufferSize)
                } catch (e: Exception) {
                    playSong(song, _currentPositionSec.value)
                }
            }
        }
    }

    fun seekTo(seconds: Float) {
        val song = currentSong ?: return
        val targetSec = seconds.coerceIn(0f, song.durationSeconds.toFloat())
        currentSampleIndex = (targetSec * sampleRate).toLong()
        _currentPositionSec.value = targetSec
    }

    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _visualizerBands.value = FloatArray(16) { 0.05f }
    }

    // Trigger DJ Scratch effect when vinyl is manipulated
    fun triggerScratchEffect() {
        scope.launch {
            val scratchSamples = ShortArray(2048 * 2)
            var phase = 0.0
            for (i in 0 until 2048) {
                val freq = 120.0 + sin(i * 0.05) * 80.0 + random.nextDouble() * 40.0
                phase += 2 * PI * freq / sampleRate
                val sampleVal = (sin(phase) * 0.4 + (random.nextDouble() - 0.5) * 0.2) * 28000
                val shortVal = sampleVal.toInt().coerceIn(-32767, 32767).toShort()
                scratchSamples[i * 2] = shortVal
                scratchSamples[i * 2 + 1] = shortVal
            }
            try {
                audioTrack?.write(scratchSamples, 0, scratchSamples.size)
            } catch (_: Exception) {}
        }
    }

    private fun startSynthesisLoop(song: Song, bufferSize: Int) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val chunkFrames = 1024
            val pcmBuffer = ShortArray(chunkFrames * 2) // Stereo (L, R)
            val songProfile = getSongProfile(song.id)

            val bands = FloatArray(16)

            while (isActive && _isPlaying.value) {
                val totalDurationSamples = (song.durationSeconds * sampleRate).toLong()
                if (currentSampleIndex >= totalDurationSamples) {
                    // Loop or end
                    currentSampleIndex = 0L
                }

                // Generate audio waveform chunk
                for (frame in 0 until chunkFrames) {
                    val t = (currentSampleIndex + frame).toDouble() / sampleRate
                    val (left, right, bandLevels) = generateMusicFrame(t, songProfile, equalizerSettings)

                    // Apply bass, treble and volume
                    val lClamped = (left * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
                    val rClamped = (right * 32000.0).toInt().coerceIn(-32767, 32767).toShort()

                    pcmBuffer[frame * 2] = lClamped
                    pcmBuffer[frame * 2 + 1] = rClamped

                    if (frame == 0) {
                        for (b in 0 until 16) {
                            bands[b] = bandLevels[b].toFloat()
                        }
                    }
                }

                currentSampleIndex += chunkFrames
                val currentSec = (currentSampleIndex.toDouble() / sampleRate).toFloat()
                _currentPositionSec.value = currentSec

                // Visualizer smoothing update
                val prev = _visualizerBands.value
                val smoothed = FloatArray(16) { i ->
                    prev[i] * 0.55f + bands[i] * 0.45f
                }
                _visualizerBands.value = smoothed

                // Write to AudioTrack
                val written = audioTrack?.write(pcmBuffer, 0, pcmBuffer.size) ?: 0
                if (written < 0) {
                    break
                }
            }
        }
    }

    private data class SongProfile(
        val bpm: Double,
        val chordRootFrequencies: List<Double>,
        val melodyNotes: List<Double>,
        val bassStyle: Int, // 0: Synthwave, 1: Lofi, 2: Ambient, 3: Acoustic, 4: Funk
        val isAcoustic: Boolean
    )

    private fun getSongProfile(songId: String): SongProfile {
        return when (songId) {
            "s1" -> SongProfile( // Nasir Khan Anthem (EDM / Synth Pop)
                bpm = 126.0,
                chordRootFrequencies = listOf(220.0, 174.61, 261.63, 196.0), // A3, F3, C4, G3
                melodyNotes = listOf(440.0, 523.25, 659.25, 587.33, 440.0, 659.25, 783.99, 880.0),
                bassStyle = 0,
                isAcoustic = false
            )
            "s2" -> SongProfile( // Cyberpunk Night Drive
                bpm = 115.0,
                chordRootFrequencies = listOf(146.83, 174.61, 130.81, 164.81), // D3, F3, C3, E3
                melodyNotes = listOf(293.66, 349.23, 440.0, 523.25, 440.0, 392.0, 349.23, 293.66),
                bassStyle = 0,
                isAcoustic = false
            )
            "s3" -> SongProfile( // Rainy Tokyo Lo-Fi
                bpm = 82.0,
                chordRootFrequencies = listOf(164.81, 220.0, 146.83, 196.0), // E3, A3, D3, G3
                melodyNotes = listOf(329.63, 392.0, 440.0, 493.88, 392.0, 329.63, 293.66, 261.63),
                bassStyle = 1,
                isAcoustic = false
            )
            "s4" -> SongProfile( // Cosmic Nebula Reverie
                bpm = 65.0,
                chordRootFrequencies = listOf(130.81, 174.61, 196.0, 220.0), // C3, F3, G3, A3
                melodyNotes = listOf(523.25, 659.25, 783.99, 1046.5, 880.0, 659.25, 523.25, 392.0),
                bassStyle = 2,
                isAcoustic = false
            )
            "s5" -> SongProfile( // Acoustic Sunset Horizon
                bpm = 95.0,
                chordRootFrequencies = listOf(196.0, 146.83, 164.81, 174.61), // G3, D3, E3, F3
                melodyNotes = listOf(392.0, 440.0, 493.88, 587.33, 493.88, 440.0, 392.0, 329.63),
                bassStyle = 3,
                isAcoustic = true
            )
            "s6" -> SongProfile( // Funk Groove Dynamite
                bpm = 118.0,
                chordRootFrequencies = listOf(164.81, 174.61, 196.0, 220.0),
                melodyNotes = listOf(329.63, 392.0, 493.88, 587.33, 659.25, 493.88, 392.0, 329.63),
                bassStyle = 4,
                isAcoustic = false
            )
            "s7" -> SongProfile( // Midnight Jazz Lounge
                bpm = 78.0,
                chordRootFrequencies = listOf(146.83, 196.0, 130.81, 174.61),
                melodyNotes = listOf(293.66, 349.23, 440.0, 523.25, 659.25, 587.33, 440.0, 349.23),
                bassStyle = 1,
                isAcoustic = false
            )
            else -> SongProfile( // Euphoric Astral Dance
                bpm = 128.0,
                chordRootFrequencies = listOf(220.0, 261.63, 196.0, 174.61),
                melodyNotes = listOf(440.0, 523.25, 659.25, 880.0, 783.99, 659.25, 523.25, 440.0),
                bassStyle = 0,
                isAcoustic = false
            )
        }
    }

    private data class FrameOutput(val left: Double, val right: Double, val bands: DoubleArray)

    private fun generateMusicFrame(
        t: Double,
        profile: SongProfile,
        eq: EqualizerSettings
    ): FrameOutput {
        val beatDuration = 60.0 / profile.bpm
        val currentBeat = t / beatDuration
        val barIndex = (currentBeat / 4.0).toInt() % profile.chordRootFrequencies.size
        val rootFreq = profile.chordRootFrequencies[barIndex]

        val beatInBar = currentBeat % 4.0
        val sixteenthNote = (currentBeat * 4.0).toInt()

        // 1. Bass Component
        val bassFreq = rootFreq / 2.0
        val bassEnvelope = max(0.0, 1.0 - (beatInBar % 1.0) * 0.9)
        var bass = when (profile.bassStyle) {
            0 -> { // Synthwave punchy saw
                val phase = (t * bassFreq) % 1.0
                (2.0 * phase - 1.0) * 0.35 * bassEnvelope
            }
            1 -> { // Lofi warm sub
                sin(2 * PI * bassFreq * t) * 0.4 * bassEnvelope
            }
            2 -> { // Deep ambient drone
                sin(2 * PI * bassFreq * t) * 0.35 + sin(2 * PI * (bassFreq * 1.5) * t) * 0.15
            }
            3 -> { // Acoustic pluck bass
                val decay = max(0.0, 1.0 - (beatInBar % 2.0) * 0.7)
                sin(2 * PI * bassFreq * t) * 0.3 * decay
            }
            else -> { // Funk slap
                val pop = sin(2 * PI * (bassFreq * 2.0) * t) * 0.3 * bassEnvelope
                sin(2 * PI * bassFreq * t) * 0.35 * bassEnvelope + pop
            }
        }
        bass *= (0.6 + eq.bassBoost * 0.8)

        // 2. Chords / Pad Component
        val chordThird = rootFreq * 1.2599 // Major / Minor 3rd approx
        val chordFifth = rootFreq * 1.4983 // Perfect 5th
        val pad1 = sin(2 * PI * rootFreq * t) * 0.12
        val pad2 = sin(2 * PI * chordThird * t) * 0.10
        val pad3 = sin(2 * PI * chordFifth * t) * 0.08
        val chords = (pad1 + pad2 + pad3) * (0.8 + eq.vocalClarity * 0.4)

        // 3. Arpeggio / Melody
        val noteIdx = ((t * (profile.bpm / 60.0) * 2.0).toInt()) % profile.melodyNotes.size
        val melFreq = profile.melodyNotes[noteIdx]
        val melEnv = max(0.0, 1.0 - ((currentBeat * 2.0) % 1.0) * 0.85)
        var melody = (sin(2 * PI * melFreq * t) * 0.22 + sin(2 * PI * melFreq * 2 * t) * 0.08) * melEnv
        melody *= (0.6 + eq.treble * 0.7)

        // 4. Rhythm / Percussion
        var drums = 0.0
        if (profile.bassStyle != 2) { // Non ambient has drums
            // Kick on 1 and 3 (or 4 on the floor)
            val kickTime = (beatInBar % 1.0) * beatDuration
            if (kickTime < 0.1) {
                val kickDecay = max(0.0, 1.0 - kickTime / 0.1)
                val kickPitch = 120.0 * kickDecay + 45.0
                drums += sin(2 * PI * kickPitch * t) * 0.45 * kickDecay
            }

            // Snare / Clap on beat 2 and 4
            val isSnareBeat = beatInBar >= 1.0 && beatInBar < 2.0 || beatInBar >= 3.0 && beatInBar < 4.0
            if (isSnareBeat) {
                val snareTime = (beatInBar % 1.0) * beatDuration
                if (snareTime < 0.12) {
                    val snareDecay = max(0.0, 1.0 - snareTime / 0.12)
                    val noise = (random.nextDouble() - 0.5) * 0.3
                    val body = sin(2 * PI * 200.0 * t) * 0.25
                    drums += (noise + body) * snareDecay
                }
            }

            // Hi-hat on every 16th note
            val hatTime = ((currentBeat * 4.0) % 1.0) * (beatDuration / 4.0)
            if (hatTime < 0.04) {
                val hatDecay = max(0.0, 1.0 - hatTime / 0.04)
                drums += (random.nextDouble() - 0.5) * 0.12 * hatDecay
            }
        }

        // 5. Vinyl Crackle FX
        var crackle = 0.0
        if (eq.vinylCrackle || profile.bassStyle == 1) {
            if (random.nextDouble() < 0.008) {
                crackle = (random.nextDouble() - 0.5) * 0.2
            }
        }

        // Stereo Panning & 3D Spatial
        val panMelody = sin(t * 0.8) * (eq.surround3D * 0.4)
        val panChords = cos(t * 0.6) * (eq.surround3D * 0.3)

        val monoSum = (bass + chords + melody + drums + crackle) * 0.65
        val left = (bass * 0.5 + chords * (0.5 - panChords) + melody * (0.5 - panMelody) + drums * 0.5 + crackle * 0.5) * 0.7
        val right = (bass * 0.5 + chords * (0.5 + panChords) + melody * (0.5 + panMelody) + drums * 0.5 + crackle * 0.5) * 0.7

        // Band estimation for spectrum visualizer (16 bands)
        val bands = DoubleArray(16)
        bands[0] = (bass.coerceIn(0.0, 1.0) * 0.9 + (if (beatInBar % 1.0 < 0.15) 0.8 else 0.1)).coerceIn(0.1, 1.0)
        bands[1] = (bass.coerceIn(0.0, 1.0) * 0.85 + 0.1).coerceIn(0.1, 1.0)
        bands[2] = (drums.coerceIn(0.0, 1.0) * 0.9 + 0.15).coerceIn(0.1, 1.0)
        for (i in 3..7) {
            bands[i] = (chords.coerceIn(0.0, 1.0) * (1.2 - (i - 3) * 0.15) + (if (melEnv > 0.4) 0.6 else 0.15)).coerceIn(0.08, 0.95)
        }
        for (i in 8..13) {
            bands[i] = (melody.coerceIn(0.0, 1.0) * (1.5 - (i - 8) * 0.1) + (if (i % 2 == noteIdx % 2) 0.7 else 0.1)).coerceIn(0.05, 0.9)
        }
        bands[14] = ((drums * 0.6 + crackle * 0.8).coerceIn(0.0, 0.8) + 0.1).coerceIn(0.05, 0.85)
        bands[15] = (if (random.nextDouble() < 0.2) 0.5 else 0.08).coerceIn(0.05, 0.8)

        return FrameOutput(left, right, bands)
    }
}
