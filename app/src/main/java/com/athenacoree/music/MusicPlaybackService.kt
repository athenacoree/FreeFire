package com.athenacoree.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

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
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        MusicPlaybackController.release()
        super.onDestroy()
    }
}

internal object MusicPlaybackController {
    @Volatile private var controller: MediaController? = null
    private val lock = Any()
    private var connecting = false
    private val pendingActions = mutableListOf<Pair<(MediaController) -> Unit, (Result<Unit>) -> Unit>>()

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
                connection.getOrNull()?.let { controller = it }
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

    fun pause(context: Context) = connect(context) { it.pause() }
    fun resume(context: Context) = connect(context) { it.play() }
    fun next(context: Context) = connect(context) { it.seekToNextMediaItem() }
    fun previous(context: Context) = connect(context) { it.seekToPreviousMediaItem() }

    fun release() {
        synchronized(lock) {
            controller?.release()
            controller = null
            pendingActions.clear()
            connecting = false
        }
    }
}
