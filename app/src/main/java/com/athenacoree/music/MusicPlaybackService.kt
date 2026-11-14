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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingHistoryJob: Job? = null
    private var pendingHistoryKey: String? = null
    private var lastRecordedItemKey: String? = null
    private var pendingListenedMs: Long = 0L

    private fun schedulePlaybackHistory(player: Player) {
        if (!player.isPlaying) {
            pendingHistoryJob?.cancel()
            pendingHistoryJob = null
            pendingHistoryKey = null
            return
        }
        val item = player.currentMediaItem ?: return
        val uri = item.localConfiguration?.uri?.toString().orEmpty()
        if (uri.isBlank()) return
        val itemKey = "${player.currentMediaItemIndex}:$uri"
        if (itemKey == lastRecordedItemKey || itemKey == pendingHistoryKey) return
        pendingHistoryJob?.cancel()
        pendingHistoryKey = itemKey
        val title = item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Audio" }
        val artist = item.mediaMetadata.artist?.toString().orEmpty()
        pendingHistoryJob = historyScope.launch {
            try {
                while (player.isPlaying && player.currentMediaItemIndex >= 0) {
                    val current = player.currentMediaItem ?: break
                    val currentUri = current.localConfiguration?.uri?.toString().orEmpty()
                    val currentKey = "${player.currentMediaItemIndex}:$currentUri"
                    if (currentKey != itemKey) break
                    val duration = player.duration
                    val threshold = if (duration > 0L && duration != C.TIME_UNSET) (duration / 2L).coerceIn(1_000L, 30_000L) else 30_000L
                    if (pendingListenedMs >= threshold) {
                        val dao = withContext(Dispatchers.IO) { MusicDatabase.getInstance(applicationContext).libraryDao() }
                        withContext(Dispatchers.IO) {
                            val localTrack = dao.findTrackByMediaUri(uri)
                            dao.recordPlayback(PlaybackHistoryEntity(id = java.util.UUID.randomUUID().toString(), trackId = localTrack?.id ?: uri, title = localTrack?.title ?: title, artistName = localTrack?.artistName ?: artist))
                        }
                        lastRecordedItemKey = itemKey
                        break
                    }
                    delay(1_000L)
                    if (player.isPlaying && player.currentMediaItemIndex >= 0) pendingListenedMs += 1_000L
                }
            } finally {
                if (pendingHistoryKey == itemKey) { pendingHistoryKey = null; pendingHistoryJob = null }
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
                if (isPlaying) MusicEqualizerController.attach(player.audioSessionId)
                schedulePlaybackHistory(player)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) MusicEqualizerController.attach(player.audioSessionId)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                pendingHistoryJob?.cancel()
                pendingHistoryJob = null
                pendingHistoryKey = null
                lastRecordedItemKey = null
                pendingListenedMs = 0L
                schedulePlaybackHistory(player)
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
        pendingHistoryJob?.cancel()
        historyScope.coroutineContext[Job]?.cancel()
        MusicPlaybackController.release()
        super.onDestroy()
    }
}

internal data class MusicPlaybackItem(val uri: Uri, val title: String, val artist: String = "", val albumTitle: String = "", val artworkUri: String? = null)

internal data class MusicQueueItem(val index: Int, val title: String, val artist: String, val mediaId: String, val isCurrent: Boolean)

internal data class MusicPlaybackUiState(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val mediaId: String,
    val isPlaying: Boolean,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
    val playbackSpeed: Float,
    val queue: List<MusicQueueItem>,
    val playbackError: String?
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
            artworkUri = item.mediaMetadata.artworkUri?.toString(),
            mediaId = item.localConfiguration?.uri?.toString().orEmpty().ifBlank { item.mediaId },
            isPlaying = player.isPlaying,
            hasPrevious = player.hasPreviousMediaItem(),
            hasNext = player.hasNextMediaItem(),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            playbackSpeed = player.playbackParameters.speed,
            playbackError = player.playerError?.let { error ->
                error.message?.take(180)?.ifBlank { error.errorCodeName } ?: error.errorCodeName
            },
            queue = (0 until player.mediaItemCount).map { index ->
                val queuedItem = player.getMediaItemAt(index)
                MusicQueueItem(
                    index = index,
                    title = queuedItem.mediaMetadata.title?.toString().orEmpty().ifBlank { "Pista ${index + 1}" },
                    artist = queuedItem.mediaMetadata.artist?.toString().orEmpty(),
                    mediaId = queuedItem.localConfiguration?.uri?.toString().orEmpty().ifBlank { queuedItem.mediaId },
                    isCurrent = index == player.currentMediaItemIndex
                )
            }
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

    fun play(context: Context, item: MusicPlaybackItem, onComplete: (Result<Unit>) -> Unit = {}) {
        connect(context, { player ->
            val metadata = MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist)
                .setAlbumTitle(item.albumTitle).setArtworkUri(item.artworkUri?.let { Uri.parse(it) }).build()
            player.setMediaItem(MediaItem.Builder().setUri(item.uri).setMediaMetadata(metadata).build())
            player.prepare()
            player.play()
        }, onComplete)
    }

    fun play(context: Context, uri: Uri, title: String, onComplete: (Result<Unit>) -> Unit = {}) =
        play(context, MusicPlaybackItem(uri, title), onComplete)

    fun playQueue(
        context: Context,
        items: List<MusicPlaybackItem>,
        startIndex: Int = 0,
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        if (items.isEmpty()) {
            onComplete(Result.failure(IllegalArgumentException("La cola está vacía.")))
            return
        }
        connect(context, { player ->
            val mediaItems = items.map { item ->
                val metadata = MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist)
                    .setAlbumTitle(item.albumTitle).setArtworkUri(item.artworkUri?.let(Uri::parse)).build()
                MediaItem.Builder().setUri(item.uri).setMediaMetadata(metadata).build()
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

    fun resume(context: Context) = connect(context, { it.play() })
    fun retry(context: Context, onComplete: (Result<Unit>) -> Unit = {}) = connect(context, { player ->
        player.prepare()
        player.play()
    }, onComplete)
    fun next(context: Context) = connect(context, { it.seekToNextMediaItem() })
    fun previous(context: Context) = connect(context, { it.seekToPreviousMediaItem() })
    fun playQueueItem(context: Context, index: Int) = connect(context, { player -> if (index in 0 until player.mediaItemCount) { player.seekToDefaultPosition(index); player.play() } })
    fun removeQueueItem(context: Context, index: Int) = connect(context, { player -> if (index in 0 until player.mediaItemCount) player.removeMediaItem(index) })
    fun clearQueue(context: Context) = connect(context, { it.clearMediaItems() })
    fun toggleShuffle(context: Context) = connect(context, { it.shuffleModeEnabled = !it.shuffleModeEnabled })
    fun setPlaybackSpeed(context: Context, speed: Float) = connect(context, {
        it.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    })
    fun cycleRepeat(context: Context) = connect(context, { player ->
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    })

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
