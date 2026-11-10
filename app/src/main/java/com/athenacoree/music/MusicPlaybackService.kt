package com.athenacoree.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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
        val player = ExoPlayer.Builder(this).build().apply { setHandleAudioBecomingNoisy(true) }
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
    private val pendingActions = mutableListOf<(MediaController) -> Unit>()

    private fun connect(context: Context, action: (MediaController) -> Unit) {
        controller?.let { ready ->
            runCatching { action(ready) }
            return
        }

        val shouldStartConnection = synchronized(lock) {
            pendingActions += action
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
            val result = runCatching { future.get() }
            val queued = synchronized(lock) {
                connecting = false
                result.getOrNull()?.let { controller = it }
                val actions = if (result.isSuccess) pendingActions.toList() else emptyList()
                pendingActions.clear()
                actions
            }
            result.getOrNull()?.let { ready ->
                queued.forEach { queuedAction -> runCatching { queuedAction(ready) } }
            }
        }, app.mainExecutor)
    }

    fun play(context: Context, uri: Uri, title: String) {
        connect(context) { player ->
            val item = MediaItem.Builder().setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }
    }

    fun playQueue(context: Context, items: List<Pair<Uri, String>>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        connect(context) { player ->
            val mediaItems = items.map { (uri, title) ->
                MediaItem.Builder().setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
            }
            player.setMediaItems(mediaItems, startIndex.coerceIn(mediaItems.indices), 0L)
            player.prepare()
            player.play()
        }
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
