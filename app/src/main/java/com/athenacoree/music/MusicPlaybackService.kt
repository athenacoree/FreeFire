package com.athenacoree.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastRecordedItemKey: String? = null

    private fun recordCurrentPlayback(player: Player) {
        if (!player.isPlaying) return
        val item = player.currentMediaItem ?: return
        val uri = item.localConfiguration?.uri?.toString().orEmpty()
        if (uri.isBlank()) return
        val itemKey = "${player.currentMediaItemIndex}:$uri"
        if (lastRecordedItemKey == itemKey) return
        lastRecordedItemKey = itemKey
        val title = item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Audio" }
        val artist = item.mediaMetadata.artist?.toString().orEmpty()
        historyScope.launch {
            runCatching {
                val dao = MusicDatabase.getInstance(applicationContext).libraryDao()
                val localTrack = dao.findTrackByMediaUri(uri)
                dao.recordPlayback(
                    PlaybackHistoryEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        trackId = localTrack?.id ?: uri,
                        title = localTrack?.title ?: title,
                        artistName = localTrack?.artistName ?: artist
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            // Respect Android audio focus and pause appropriately when headphones are disconnected.
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    MusicEqualizerController.attach(player.audioSessionId)
                    recordCurrentPlayback(player)
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) MusicEqualizerController.attach(player.audioSessionId)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                lastRecordedItemKey = null
                recordCurrentPlayback(player)
            }
        })
        MusicEqualizerController.attach(player.audioSessionId)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        MusicEqualizerController.release()
        historyScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        MusicPlaybackController.release()
        super.onDestroy()
    }
}

internal data class MusicPlaybackUiState(
    val title: String,
    val artist: String,
    val mediaId: String,
    val isPlaying: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean
)

internal object MusicPlaybackController {
    @Volatile private var controller: MediaController? = null
    private val _playbackState = MutableStateFlow<MusicPlaybackUiState?>(null)
    val playbackState: StateFlow<MusicPlaybackUiState?> = _playbackState
    private fun refreshPlaybackState(player: Player) {
        val item = player.currentMediaItem
        _playbackState.value = if (item == null) null else MusicPlaybackUiState(
            title = item.mediaMetadata.title?.toString().orEmpty().ifBlank { item.mediaId.ifBlank { "Reproduciendo" } },
            artist = item.mediaMetadata.artist?.toString().orEmpty(),
            mediaId = item.localConfiguration?.uri?.toString().orEmpty().ifBlank { item.mediaId },
            isPlaying = player.isPlaying,
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem()
        )
    }
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refreshPlaybackState(player)
    }
    private val lock = Any()
    private var connecting = false
    private val pendingActions = mutableListOf<Pair<(MediaController) -> Unit, (Result<Unit>) -> Unit>>()
    private val timerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null

    private fun connect(
        context: Context,
        action: (MediaController) -> Unit,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        controller?.let { ready ->
            val result = runCatching { action(ready) }
            onComplete(result)
            return
        }

        val shouldStartConnection = synchronized(lock) {
            pendingActions += action to onComplete
            if (connecting) {
                false
            } else {
                connecting = true
                true
            }
        }
        if (!shouldStartConnection) return

        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, MusicPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val connection = runCatching { future.get() }
            val queued = synchronized(lock) {
                connecting = false
                connection.getOrNull()?.let { ready ->
                    controller = ready
                    ready.addListener(playerListener)
                    refreshPlaybackState(ready)
                }
                val actions = pendingActions.toList()
                pendingActions.clear()
                actions
            }
            connection.fold(
                onSuccess = { ready ->
                    queued.forEach { (queuedAction, callback) ->
                        callback(runCatching { queuedAction(ready) })
                    }
                },
                onFailure = { error ->
                    queued.forEach { (_, callback) -> callback(Result.failure(error)) }
                }
            )
        }, app.mainExecutor)
    }

    fun play(context: Context, uri: Uri, title: String, onComplete: (Result<Unit>) -> Unit = {}) {
        connect(context, { player ->
            val item = MediaItem.Builder().setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }, onComplete)
    }

    fun playQueue(
        context: Context,
        items: List<Pair<Uri, String>>,
        startIndex: Int = 0,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        if (items.isEmpty()) {
            onComplete(Result.failure(IllegalArgumentException("La cola está vacía.")))
            return
        }
        connect(context, { player ->
            val mediaItems = items.map { (uri, title) ->
                MediaItem.Builder().setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
            }
            player.setMediaItems(mediaItems, startIndex.coerceIn(mediaItems.indices), 0L)
            player.prepare()
            player.play()
        }, onComplete)
    }

    fun pause(context: Context, onComplete: (Result<Unit>) -> Unit = {}) =
        connect(context, { it.pause() }, onComplete)

    fun setSleepTimer(context: Context, minutes: Int, onFinished: (Result<Unit>) -> Unit) {
        sleepTimerJob?.cancel()
        sleepTimerJob = timerScope.launch {
            delay(minutes.coerceIn(1, 240) * 60_000L)
            pause(context, onFinished)
            sleepTimerJob = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }
    fun currentPosition(): Long = controller?.currentPosition ?: 0L
    fun currentDuration(): Long = controller?.duration?.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0L)) }

    fun resume(context: Context) = connect(context) { it.play() }
    fun next(context: Context) = connect(context) { it.seekToNextMediaItem() }
    fun previous(context: Context) = connect(context) { it.seekToPreviousMediaItem() }

    fun release() {
        synchronized(lock) {
            controller?.removeListener(playerListener)
            controller?.release()
            controller = null
            _playbackState.value = null
            pendingActions.clear()
            connecting = false
        }
    }
}
