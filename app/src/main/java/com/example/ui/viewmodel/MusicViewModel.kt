package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicAudioEngine
import com.example.data.local.AppDatabase
import com.example.data.local.LocalMediaScanner
import com.example.data.model.AppTheme
import com.example.data.model.EqualizerSettings
import com.example.data.model.OverallStatistics
import com.example.data.model.PlaybackHistoryItem
import com.example.data.model.Playlist
import com.example.data.model.Song
import com.example.data.model.VideoItem
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

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class NavigationTab {
    NOW_PLAYING, PLAYLISTS, VIDEOS, STATS, THEMES_LAB
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
    val showAddToPlaylistDialog: Song? = null,
    // Video state
    val currentVideo: VideoItem? = null,
    val isVideoPlaying: Boolean = false,
    val videoPositionSec: Float = 0f,
    val selectedVideoTab: Int = 0, // 0 = Shorts, 1 = Long Videos, 2 = Favorites
    val shortsThresholdSeconds: Int = 60,
    val isScanningMedia: Boolean = false,
    val scanStatusMessage: String? = null,
    val hasStoragePermission: Boolean = false
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(
        database.songDao(),
        database.playlistDao(),
        database.videoDao(),
        database.playbackHistoryDao()
    )
    val audioEngine = MusicAudioEngine(application)
    private val mediaScanner = LocalMediaScanner(application)

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideos: StateFlow<List<VideoItem>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shorts: StateFlow<List<VideoItem>> = repository.shorts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val longVideos: StateFlow<List<VideoItem>> = repository.longVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostWatchedVideos: StateFlow<List<VideoItem>> = repository.mostWatchedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteVideos: StateFlow<List<VideoItem>> = repository.favoriteVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<PlaybackHistoryItem>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall real statistics
    val statistics: StateFlow<OverallStatistics> = combine(
        repository.allSongs,
        repository.allVideos
    ) { songs, videos ->
        var totalMusicPlays = 0
        var totalListeningTime = 0L
        var totalMusicCompletions = 0

        for (song in songs) {
            totalMusicPlays += song.playCount
            totalListeningTime += song.totalListeningTimeSeconds
            totalMusicCompletions += song.completionCount
        }

        var totalVideoPlays = 0
        var totalVideoWatchTime = 0L
        var totalVideoCompletions = 0
        var shortsCount = 0
        var longVideosCount = 0

        for (video in videos) {
            totalVideoPlays += video.playCount
            totalVideoWatchTime += video.totalWatchTimeSeconds
            totalVideoCompletions += video.completionCount
            if (video.isShort) shortsCount++ else longVideosCount++
        }

        OverallStatistics(
            totalMusicPlays = totalMusicPlays,
            totalListeningTimeSeconds = totalListeningTime,
            totalMusicCompletions = totalMusicCompletions,
            totalVideoPlays = totalVideoPlays,
            totalVideoWatchTimeSeconds = totalVideoWatchTime,
            totalVideoCompletions = totalVideoCompletions,
            totalShortsCount = shortsCount,
            totalLongVideosCount = longVideosCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallStatistics())

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var playbackQueue: List<Song> = emptyList()
    private var originalQueue: List<Song> = emptyList()
    private var queueIndex: Int = 0
    private var sleepTimerJob: Job? = null
    private var songListeningJob: Job? = null
    private var videoWatchJob: Job? = null

    init {
        audioEngine.onCompletionListener = {
            handleTrackCompletion()
        }

        viewModelScope.launch {
            repository.ensureInitialData()
        }

        // Synchronize current song with repository
        viewModelScope.launch {
            repository.allSongs.collect { songs ->
                if (songs.isEmpty()) {
                    playbackQueue = emptyList()
                    originalQueue = emptyList()
                    if (_uiState.value.currentSong != null) {
                        _uiState.value = _uiState.value.copy(
                            currentSong = null,
                            durationSec = 0,
                            currentPositionSec = 0f
                        )
                    }
                } else {
                    val current = _uiState.value.currentSong
                    if (current == null || songs.none { it.id == current.id }) {
                        val first = songs.first()
                        playbackQueue = songs
                        originalQueue = songs
                        queueIndex = 0
                        _uiState.value = _uiState.value.copy(
                            currentSong = first,
                            durationSec = first.durationSeconds
                        )
                    } else {
                        // Update current song metadata if changed
                        val updated = songs.firstOrNull { it.id == current.id }
                        if (updated != null && updated != current) {
                            _uiState.value = _uiState.value.copy(currentSong = updated)
                        }
                    }
                }
            }
        }

        // Synchronize current video with repository
        viewModelScope.launch {
            repository.allVideos.collect { videos ->
                if (videos.isEmpty()) {
                    if (_uiState.value.currentVideo != null) {
                        _uiState.value = _uiState.value.copy(
                            currentVideo = null,
                            isVideoPlaying = false
                        )
                    }
                } else {
                    val current = _uiState.value.currentVideo
                    if (current == null || videos.none { it.id == current.id }) {
                        _uiState.value = _uiState.value.copy(
                            currentVideo = videos.first()
                        )
                    }
                }
            }
        }

        // Collect audio engine states
        viewModelScope.launch {
            audioEngine.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startListeningTracker()
                } else {
                    stopListeningTracker()
                }
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

    private fun startListeningTracker() {
        songListeningJob?.cancel()
        songListeningJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val song = _uiState.value.currentSong
                if (song != null && _uiState.value.isPlaying) {
                    repository.addListeningTime(song.id, 1L)
                }
            }
        }
    }

    private fun stopListeningTracker() {
        songListeningJob?.cancel()
        songListeningJob = null
    }

    fun setTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectTheme(theme: AppTheme) {
        _uiState.value = _uiState.value.copy(currentTheme = theme)
    }

    fun playSong(song: Song, contextQueue: List<Song>? = null) {
        // Pause any active video
        if (_uiState.value.isVideoPlaying) {
            pauseVideo()
        }

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

        // Increment play count & log history
        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
            repository.logHistory(
                mediaId = song.id,
                mediaType = "MUSIC",
                title = song.title,
                subtitle = "${song.artist} • ${song.album}",
                durationSec = song.durationSeconds
            )
        }
    }

    fun togglePlayPause() {
        val song = _uiState.value.currentSong ?: return
        if (_uiState.value.isPlaying) {
            audioEngine.pause()
        } else {
            // If video was playing, pause it
            if (_uiState.value.isVideoPlaying) {
                pauseVideo()
            }
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
        val nextIndex = (queueIndex + 1) % playbackQueue.size
        queueIndex = nextIndex
        playSong(playbackQueue[nextIndex])
    }

    fun skipPrevious() {
        if (playbackQueue.isEmpty()) return
        val prevIndex = if (queueIndex - 1 < 0) playbackQueue.size - 1 else queueIndex - 1
        queueIndex = prevIndex
        playSong(playbackQueue[prevIndex])
    }

    private fun handleTrackCompletion() {
        val current = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            repository.incrementSongCompletion(current.id)
        }

        when (_uiState.value.repeatMode) {
            RepeatMode.ONE -> {
                audioEngine.seekTo(0f)
                audioEngine.playSong(current, 0f)
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (queueIndex < playbackQueue.size - 1) {
                    skipNext()
                } else {
                    audioEngine.pause()
                    audioEngine.seekTo(0f)
                }
            }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffle
        _uiState.value = _uiState.value.copy(isShuffle = newShuffle)
        val currentSong = _uiState.value.currentSong

        playbackQueue = if (newShuffle) {
            val shuffled = originalQueue.toMutableList()
            shuffled.remove(currentSong)
            shuffled.shuffle()
            if (currentSong != null) listOf(currentSong) + shuffled else shuffled
        } else {
            originalQueue
        }
        queueIndex = playbackQueue.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
    }

    fun toggleRepeat() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            RepeatMode.OFF -> RepeatMode.ALL
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

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = minutes)

        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60000)
                    remaining--
                    _uiState.value = _uiState.value.copy(sleepTimerMinutesLeft = remaining)
                }
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

    fun updateEqualizer(settings: EqualizerSettings) {
        _uiState.value = _uiState.value.copy(equalizerSettings = settings)
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

    // ==================== VIDEO METHODS ====================

    fun setSelectedVideoTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedVideoTab = tab)
    }

    fun playVideo(video: VideoItem) {
        // Pause music playback when video plays
        if (_uiState.value.isPlaying) {
            audioEngine.pause()
        }

        _uiState.value = _uiState.value.copy(
            currentVideo = video,
            isVideoPlaying = true,
            videoPositionSec = 0f
        )

        startVideoWatchTracker(video.id)

        viewModelScope.launch {
            repository.incrementWatchCount(video.id)
            repository.logHistory(
                mediaId = video.id,
                mediaType = "VIDEO",
                title = video.title,
                subtitle = "${video.artist} • ${if (video.isShort) "Short" else "Long Video"}",
                durationSec = video.durationSeconds
            )
        }
    }

    fun pauseVideo() {
        _uiState.value = _uiState.value.copy(isVideoPlaying = false)
        stopVideoWatchTracker()
    }

    fun resumeVideo() {
        if (_uiState.value.isPlaying) {
            audioEngine.pause()
        }
        _uiState.value = _uiState.value.copy(isVideoPlaying = true)
        val video = _uiState.value.currentVideo
        if (video != null) {
            startVideoWatchTracker(video.id)
        }
    }

    fun toggleVideoPlayPause() {
        if (_uiState.value.isVideoPlaying) {
            pauseVideo()
        } else {
            resumeVideo()
        }
    }

    fun setVideoPosition(seconds: Float) {
        _uiState.value = _uiState.value.copy(videoPositionSec = seconds)
    }

    fun onVideoCompleted(videoId: String) {
        viewModelScope.launch {
            repository.incrementVideoCompletion(videoId)
        }
    }

    fun toggleVideoFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleVideoFavorite(video.id, video.isFavorite)
            if (_uiState.value.currentVideo?.id == video.id) {
                _uiState.value = _uiState.value.copy(
                    currentVideo = video.copy(isFavorite = !video.isFavorite)
                )
            }
        }
    }

    private fun startVideoWatchTracker(videoId: String) {
        videoWatchJob?.cancel()
        videoWatchJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.isVideoPlaying) {
                    repository.addWatchTime(videoId, 1L)
                    val newPos = _uiState.value.videoPositionSec + 1f
                    _uiState.value = _uiState.value.copy(videoPositionSec = newPos)
                    val duration = _uiState.value.currentVideo?.durationSeconds ?: 0
                    if (duration > 0 && newPos >= duration) {
                        onVideoCompleted(videoId)
                    }
                }
            }
        }
    }

    private fun stopVideoWatchTracker() {
        videoWatchJob?.cancel()
        videoWatchJob = null
    }

    fun setShortsThreshold(seconds: Int) {
        _uiState.value = _uiState.value.copy(shortsThresholdSeconds = seconds)
    }

    private var lastScanTimestamp: Long = 0L

    fun updatePermissionStatus(isGranted: Boolean) {
        _uiState.value = _uiState.value.copy(hasStoragePermission = isGranted)
        if (isGranted) {
            scanLocalMedia(silent = false)
        }
    }

    fun onAppResume(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasStoragePermission = hasPermission)
        if (hasPermission) {
            val now = System.currentTimeMillis()
            // Rescan if 25+ seconds have elapsed or if initial scan hasn't happened
            if (now - lastScanTimestamp > 25_000L || lastScanTimestamp == 0L) {
                scanLocalMedia(silent = true)
            }
        }
    }

    fun scanLocalMedia(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanningMedia = true,
                scanStatusMessage = if (silent) null else "Scanning device storage for music and videos..."
            )

            try {
                val scannedAudio = mediaScanner.scanLocalAudio()
                val scannedVideos = mediaScanner.scanLocalVideos(_uiState.value.shortsThresholdSeconds)

                repository.syncScannedMedia(scannedAudio, scannedVideos)
                lastScanTimestamp = System.currentTimeMillis()

                val msg = if (scannedAudio.isEmpty() && scannedVideos.isEmpty()) {
                    "No media found on device storage."
                } else {
                    "Discovered ${scannedAudio.size} songs and ${scannedVideos.size} videos from storage"
                }

                _uiState.value = _uiState.value.copy(
                    isScanningMedia = false,
                    scanStatusMessage = msg
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanningMedia = false,
                    scanStatusMessage = "Scan error: ${e.message}"
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        sleepTimerJob?.cancel()
        songListeningJob?.cancel()
        videoWatchJob?.cancel()
    }
}
