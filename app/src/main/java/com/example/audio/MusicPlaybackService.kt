package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song

interface MusicPlaybackListener {
    fun onPlay()
    fun onPause()
    fun onNext()
    fun onPrev()
    fun onSeek(positionSec: Float)
    fun onStop()
}

class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "nk_player_media_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_UPDATE = "com.example.action.UPDATE"

        var listener: MusicPlaybackListener? = null
        var isServiceRunning = false
            private set

        fun startOrUpdate(
            context: Context,
            song: Song,
            isPlaying: Boolean,
            positionSec: Float = 0f,
            speed: Float = 1.0f
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_UPDATE
                putExtra("title", song.title)
                putExtra("artist", song.artist)
                putExtra("album", song.album)
                putExtra("duration", song.durationSeconds)
                putExtra("isPlaying", isPlaying)
                putExtra("positionSec", positionSec)
                putExtra("speed", speed)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var mediaSession: MediaSession? = null
    private var currentTitle = "NK Player"
    private var currentArtist = "Playing Audio"
    private var currentAlbum = ""
    private var currentDurationSec = 0
    private var isPlaying = false
    private var currentPositionSec = 0f
    private var playbackSpeed = 1.0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        initMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NK Player Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and information for active music playback in NK Player"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "NKPlayerSession").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    listener?.onPlay()
                }

                override fun onPause() {
                    listener?.onPause()
                }

                override fun onSkipToNext() {
                    listener?.onNext()
                }

                override fun onSkipToPrevious() {
                    listener?.onPrev()
                }

                override fun onSeekTo(pos: Long) {
                    listener?.onSeek(pos / 1000f)
                }

                override fun onStop() {
                    listener?.onStop()
                    stopServiceInternal()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_PLAY -> listener?.onPlay()
            ACTION_PAUSE -> listener?.onPause()
            ACTION_NEXT -> listener?.onNext()
            ACTION_PREV -> listener?.onPrev()
            ACTION_STOP -> {
                listener?.onStop()
                stopServiceInternal()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                currentTitle = intent.getStringExtra("title") ?: "NK Player"
                currentArtist = intent.getStringExtra("artist") ?: "Music"
                currentAlbum = intent.getStringExtra("album") ?: ""
                currentDurationSec = intent.getIntExtra("duration", 0)
                isPlaying = intent.getBooleanExtra("isPlaying", false)
                currentPositionSec = intent.getFloatExtra("positionSec", 0f)
                playbackSpeed = intent.getFloatExtra("speed", 1.0f)
            }
        }

        updateMediaSession()
        val notification = buildNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}

        return START_STICKY
    }

    private fun updateMediaSession() {
        val session = mediaSession ?: return
        try {
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, currentAlbum)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, (currentDurationSec * 1000).toLong())
                .build()
            session.setMetadata(metadata)

            val stateActions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP

            val state = PlaybackState.Builder()
                .setActions(stateActions)
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    (currentPositionSec * 1000).toLong(),
                    playbackSpeed
                )
                .build()
            session.setPlaybackState(state)
        } catch (_: Exception) {}
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder.setContentTitle(currentTitle)
            .setContentText(if (currentArtist.isNotBlank()) "$currentArtist • NK Player" else "NK Player")
            .setSubText(if (currentAlbum.isNotBlank()) currentAlbum else "Now Playing")
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)

        // Previous button
        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            prevIntent
        ).build()
        builder.addAction(prevAction)

        // Play/Pause button
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) "Pause" else "Play"
        val playPauseAction = Notification.Action.Builder(
            playPauseIcon,
            playPauseText,
            playPauseIntent
        ).build()
        builder.addAction(playPauseAction)

        // Next button
        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            nextIntent
        ).build()
        builder.addAction(nextAction)

        // Attach MediaSession and compact media style
        mediaSession?.let { session ->
            val mediaStyle = Notification.MediaStyle()
                .setMediaSession(session.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            builder.style = mediaStyle
        }

        return builder.build()
    }

    private fun stopServiceInternal() {
        isServiceRunning = false
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
        } catch (_: Exception) {}
        mediaSession = null
    }
}
