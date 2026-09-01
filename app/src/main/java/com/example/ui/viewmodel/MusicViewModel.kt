package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicAudioEngine
import com.example.data.local.AppDatabase
import com.example.data.model.AppTheme
import com.example.data.model.EqualizerSettings
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Collections

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class NavigationTab {
    NOW_PLAYING, PLAYLISTS, THEMES_LAB
}

data class MusicUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionSec: Float = 0f,
    val durationSec: Int = 0,
    val visualizerBands: FloatArray = FloatArray(16) { 0.1f },
    val playbackSpeed: Float = 1.0f,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val currentTheme: AppTheme = AppTheme.IMMERSIVE_UI,
    val equalizerSettings: EqualizerSettings = EqualizerSettings(),
    val currentTab: NavigationTab = NavigationTab.NOW_PLAYING,
    val selectedPlaylistId: String? = null,
    val searchQuery: String = "",
    val selectedGenreFilter: String? = null,
    val sleepTimerMinutesLeft: Int? = null,
    val showLyrics: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showCreatePlaylistDialog: Boolean = false,
    val showAddToPlaylistDialog: Song? = null
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(database.songDao(), database.playlistDao())
    val audioEngine = MusicAudioEngine()

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var playbackQueue: List<Song> = emptyList()
    private var originalQueue: List<Song> = emptyList()
    private var queueIndex: Int = 0
    private var sleepTimerJob: Job? = null
    private var playDurationAccumulator = 0f

    init {
        viewModelScope.launch {
            repository.ensureInitialData()
        }

        // Collect songs and initialize first song if not set
        viewModelScope.launch {
            repository.allSongs.collect { songs ->
                if (songs.isNotEmpty() && _uiState.value.currentSong == null) {
                    playbackQueue = songs
                    originalQueue = songs
                    _uiState.value = _uiState.value.copy(
                        currentSong = songs.first(),
                        durationSec = songs.first().durationSeconds
                    )
                }
            }
        }

        // Collect audio engine states
        viewModelScope.launch {
            audioEngine.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }

        viewModelScope.launch {
            audioEngine.currentPositionSec.collect { pos ->
                _uiState.value = _uiState.value.copy(currentPositionSec = pos)
                val current = _uiState.value.currentSong
                if (current != null && pos >= current.durationSeconds && _uiState.value.isPlaying) {
                    handleTrackCompletion()
                }
            }
        }

        viewModelScope.launch {
            audioEngine.visualizerBands.collect { bands ->
                _uiState.value = _uiState.value.copy(visualizerBands = bands)
            }
        }

        viewModelScope.launch {
            audioEngine.playbackSpeed.collect { speed ->
                _uiState.value = _uiState.value.copy(playbackSpeed = speed)
            }
        }
    }

    fun setTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectTheme(theme: AppTheme) {
        _uiState.value = _uiState.value.copy(currentTheme = theme)
    }

    fun playSong(song: Song, contextQueue: List<Song>? = null) {
        if (contextQueue != null) {
            originalQueue = contextQueue
            playbackQueue = if (_uiState.value.isShuffle) contextQueue.shuffled() else contextQueue
            queueIndex = playbackQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        } else {
            if (!playbackQueue.any { it.id == song.id }) {
                playbackQueue = playbackQueue + song
            }
            queueIndex = playbackQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }

        _uiState.value = _uiState.value.copy(
            currentSong = song,
            durationSec = song.durationSeconds,
            currentPositionSec = 0f
        )
        audioEngine.playSong(song, 0f)

        // Increment play count in database
        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
        }
    }

    fun togglePlayPause() {
        val song = _uiState.value.currentSong ?: return
        if (_uiState.value.isPlaying) {
            audioEngine.pause()
        } else {
            if (_uiState.value.currentPositionSec > 0f) {
                audioEngine.resume()
            } else {
                audioEngine.playSong(song, 0f)
            }
        }
    }

    fun seekTo(seconds: Float) {
        audioEngine.seekTo(seconds)
        _uiState.value = _uiState.value.copy(currentPositionSec = seconds)
    }

    fun skipNext() {
        if (playbackQueue.isEmpty()) return
        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                _uiState.value.currentSong?.let { playSong(it) }
            }
            else -> {
                queueIndex = (queueIndex + 1) % playbackQueue.size
                val nextSong = playbackQueue[queueIndex]
                playSong(nextSong)
            }
        }
    }

    fun skipPrevious() {
        if (playbackQueue.isEmpty()) return
        if (_uiState.value.currentPositionSec > 3f) {
            seekTo(0f)
            return
        }
        queueIndex = if (queueIndex - 1 < 0) playbackQueue.size - 1 else queueIndex - 1
        val prevSong = playbackQueue[queueIndex]
        playSong(prevSong)
    }

    private fun handleTrackCompletion() {
        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                _uiState.value.currentSong?.let { playSong(it) }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (queueIndex < playbackQueue.size - 1) {
                    skipNext()
                } else {
                    audioEngine.pause()
                    seekTo(0f)
                }
            }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffle
        _uiState.value = _uiState.value.copy(isShuffle = newShuffle)
        val currentSong = _uiState.value.currentSong
        playbackQueue = if (newShuffle) {
            originalQueue.shuffled()
        } else {
            originalQueue
        }
        if (currentSong != null) {
            queueIndex = playbackQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.value = _uiState.value.copy(repeatMode = nextMode)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
            if (_uiState.value.currentSong?.id == song.id) {
                _uiState.value = _uiState.value.copy(
                    currentSong = song.copy(isFavorite = !song.isFavorite)
                )
            }
        }
    }

    fun setSpeed(speed: Float) {
        audioEngine.setSpeed(speed)
    }

    fun scratchTurntable() {
        audioEngine.triggerScratchEffect()
    }

    fun updateEqualizer(settings: EqualizerSettings) {
        _uiState.value = _uiState.value.copy(equalizerSettings = settings)
        audioEngine.updateEqualizer(settings)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = null)
            return
        }
        _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = minutes)
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(60000L) // 1 minute
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = if (remaining > 0) remaining else null)
            }
            if (isActive) {
                audioEngine.pause()
                _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = null)
            }
        }
    }

    fun toggleLyrics() {
        _uiState.value = _uiState.value.copy(showLyrics = !_uiState.value.showLyrics)
    }

    fun toggleEqualizerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEqualizerDialog = show)
    }

    fun toggleCreatePlaylistDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreatePlaylistDialog = show)
    }

    fun setAddToPlaylistSong(song: Song?) {
        _uiState.value = _uiState.value.copy(showAddToPlaylistDialog = song)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setGenreFilter(genre: String?) {
        _uiState.value = _uiState.value.copy(selectedGenreFilter = genre)
    }

    fun selectPlaylist(playlistId: String?) {
        _uiState.value = _uiState.value.copy(selectedPlaylistId = playlistId)
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name, description)
            _uiState.value = _uiState.value.copy(
                showCreatePlaylistDialog = false,
                selectedPlaylistId = id
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_uiState.value.selectedPlaylistId == playlistId) {
                _uiState.value = _uiState.value.copy(selectedPlaylistId = null)
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            _uiState.value = _uiState.value.copy(showAddToPlaylistDialog = null)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: String) = repository.getSongsForPlaylist(playlistId)

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        sleepTimerJob?.cancel()
    }
}
