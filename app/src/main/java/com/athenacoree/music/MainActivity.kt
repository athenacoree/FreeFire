package com.athenacoree.music

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.collectAsState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus


private fun findActiveLyricIndex(lines: List<MusicLyricsLine>, positionMs: Long): Int {
    var low = 0
    var high = lines.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (lines[middle].timeMs <= positionMs) low = middle + 1 else high = middle
    }
    return low - 1
}

private enum class MusicFeedbackType { SUCCESS, ERROR, INFO, LOADING }
private data class MusicFeedback(val message: String, val type: MusicFeedbackType)

private fun friendlyMusicError(message: String): String {
    val clean = message.trim().ifBlank { "No hemos podido completar esta acción." }
    val softened = clean
        .replaceFirst("No se pudieron", "No hemos podido")
        .replaceFirst("No se pudo", "No hemos podido")
    return if (softened.endsWith(".")) "$softened Puedes intentarlo de nuevo en unos momentos."
    else "$softened. Puedes intentarlo de nuevo en unos momentos."
}

private enum class MusicTab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Default.Home),
    Library("Biblioteca", Icons.Default.LibraryMusic),
    Videos("Vídeos", Icons.Default.VideoLibrary),
    Playlists("Listas", Icons.Default.PlaylistPlay),
    Profile("Perfil", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    private val sharedTokenState = mutableStateOf<String?>(null)

    private fun tokenFrom(intent: Intent?): String? =
        intent?.data?.takeIf { it.scheme == "music" && it.host == "share" }?.lastPathSegment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedTokenState.value = tokenFrom(intent)
        setContent { MusicApp(sharedTokenState.value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedTokenState.value = tokenFrom(intent)
    }
}

@Composable
private fun MusicApp(sharedToken: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saveableStateHolder = rememberSaveableStateHolder()
    val darkMode by MusicPreferences.darkMode(context).collectAsState(initial = true)
    val performanceMode by MusicPreferences.performanceMode(context).collectAsState(initial = "balanced")
    val changeTheme: () -> Unit = { scope.launch { MusicPreferences.setDarkMode(context, !darkMode) }; Unit }
    val libraryRepository = remember { MusicLibraryRepository(MusicDatabase.getInstance(context).libraryDao()) }
    val playbackState by MusicPlaybackController.playbackState.collectAsState(initial = null)
    var feedback by remember { mutableStateOf<MusicFeedback?>(null) }
    val showFeedback: (String, MusicFeedbackType) -> Unit = { message, type ->
        val friendlyMessage = if (type == MusicFeedbackType.ERROR) {
            "En este momento hemos tenido un inconveniente. Estamos trabajando para solucionarlo. Puedes volver a intentarlo en unos instantes."
        } else message
        feedback = MusicFeedback(friendlyMessage, type)
    }
    LaunchedEffect(feedback) {
        if (feedback != null && feedback?.type != MusicFeedbackType.LOADING) {
            delay(2800)
            feedback = null
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.Home.name) }
    var catalogKind by rememberSaveable { mutableStateOf("video") }
    var sleepTimerMinutes by rememberSaveable { mutableStateOf(0) }
    var lyricsLines by remember { mutableStateOf<List<MusicLyricsLine>>(emptyList()) }
    var lyricsMediaId by rememberSaveable { mutableStateOf("") }
    var playerVisualMode by rememberSaveable { mutableStateOf("normal") }
    var sharedContent by remember { mutableStateOf<MusicSharedContent?>(null) }
    var sharedLoadError by remember { mutableStateOf("") }
    var showSharedDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sharedToken) {
        if (!sharedToken.isNullOrBlank()) {
            sharedContent = null
            sharedLoadError = ""
            showSharedDialog = true
            showFeedback("Abriendo contenido compartido…", MusicFeedbackType.LOADING)
            val result = runCatching {
                withContext(Dispatchers.IO) { MusicShareRepository.load(sharedToken) }
            }
            result.onSuccess {
                sharedContent = it
                showFeedback("Contenido compartido cargado.", MusicFeedbackType.SUCCESS)
            }.onFailure {
                sharedLoadError = "Este contenido ya no está disponible. Puede que el enlace haya caducado o se haya desactivado."
                showFeedback(sharedLoadError, MusicFeedbackType.ERROR)
            }
        }
    }
    LaunchedEffect(playbackState?.mediaId) { lyricsLines = emptyList() }
    val tab = MusicTab.valueOf(selectedTab)

    val colors = if (darkMode) {
        MusicColors(
            background = Color(0xFF090B12),
            surface = Color(0xFF151925),
            surfaceAlt = Color(0xFF20283A),
            text = Color(0xFFF5F3FF),
            muted = Color(0xFFA6A4B8),
            accent = Color(0xFFB69CFF),
            accentText = Color(0xFF211A35)
        )
    } else {
        MusicColors(
            background = Color(0xFFF5F6F8),
            surface = Color.White,
            surfaceAlt = Color(0xFFE9EDF2),
            text = Color(0xFF171A20),
            muted = Color(0xFF737B88),
            accent = Color(0xFF7654C8),
            accentText = Color.White
        )
    }

    val musicColorScheme = if (darkMode) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentText,
            secondary = colors.accent,
            background = colors.background,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.muted
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.accentText,
            secondary = colors.accent,
            background = colors.background,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.muted
        )
    }

    MaterialTheme(colorScheme = musicColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    MusicTopBar(
                        colors = colors,
                        darkMode = darkMode,
                        onThemeChange = changeTheme,
                        onSearch = { selectedTab = MusicTab.Library.name }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = colors.surface) {
                        MusicTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { selectedTab = item.name },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, maxLines = 1) }
                            )
                        }
                    }
                }
            ) { padding ->
                saveableStateHolder.SaveableStateProvider(tab.name) {
                    when (tab) {
                        MusicTab.Home -> HomeScreen(colors, padding, libraryRepository) { kind -> catalogKind = kind; selectedTab = MusicTab.Videos.name }
                        MusicTab.Library -> LibraryScreen(colors, padding, libraryRepository, showFeedback)
                        MusicTab.Videos -> CatalogBrowserScreen(colors, padding, libraryRepository, showFeedback, catalogKind, onKindChange = { catalogKind = it })
                        MusicTab.Playlists -> PlaylistManagerScreen(colors, padding, libraryRepository, showFeedback)
                        MusicTab.Profile -> ProfileScreen(colors, padding, darkMode, performanceMode, onPerformanceModeChange = { mode -> scope.launch { MusicPreferences.setPerformanceMode(context, mode) } }, sleepTimerMinutes = sleepTimerMinutes, onSleepTimerChange = { sleepTimerMinutes = it }, onThemeChange = changeTheme, showFeedback = showFeedback)
                    }
                }
            }
            MusicMiniPlayer(playbackState, colors, context, lyricsLines, lyricsMediaId, onLyricsChange = { mediaId, lines -> lyricsMediaId = mediaId; lyricsLines = lines }, visualMode = playerVisualMode, performanceMode = performanceMode, onVisualModeChange = { playerVisualMode = it })
            MusicFeedbackBanner(feedback, colors)
            if (showSharedDialog && !sharedToken.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { showSharedDialog = false },
                    title = {
                        Text(sharedContent?.title ?: "Contenido compartido", color = colors.text)
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            when {
                                sharedContent != null -> {
                                    Text(
                                        when (sharedContent?.type) {
                                            "artist" -> "Artista"
                                            "album" -> "Álbum"
                                            "track" -> "Canción"
                                            "video" -> "Vídeo"
                                            "playlist" -> "Lista compartida"
                                            else -> "Contenido de Music"
                                        },
                                        color = colors.accent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(sharedContent?.description.orEmpty().ifBlank { "Descubre este contenido en Music." },
                                        color = colors.muted, fontSize = 12.sp)
                                    if (sharedContent?.type == "playlist") {
                                        sharedContent?.tracks.orEmpty().forEachIndexed { index, track ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                                    .background(colors.surfaceAlt).padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text((index + 1).toString(), color = colors.accent, fontSize = 11.sp)
                                                Spacer(Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(track.title, color = colors.text, fontSize = 12.sp,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(track.artist.ifBlank { "Artista desconocido" }, color = colors.muted, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                sharedLoadError.isNotBlank() -> Text(sharedLoadError, color = colors.muted, fontSize = 12.sp)
                                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp), color = colors.accent, strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Estamos cargando los detalles…", color = colors.muted, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSharedDialog = false }) {
                            Text("Cerrar", color = colors.accent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MusicMiniPlayer(state: MusicPlaybackUiState?, colors: MusicColors, context: android.content.Context, lyrics: List<MusicLyricsLine>, lyricsMediaId: String, onLyricsChange: (String, List<MusicLyricsLine>) -> Unit, visualMode: String, performanceMode: String, onVisualModeChange: (String) -> Unit) {
    if (state == null) return
    var positionMs by remember(state.mediaId) { mutableStateOf(0L) }
    var durationMs by remember(state.mediaId) { mutableStateOf(0L) }
    var isSeeking by remember(state.mediaId) { mutableStateOf(false) }
    var expandedLyrics by rememberSaveable(state.mediaId) { mutableStateOf(false) }
    var lyricsOffsetMs by rememberSaveable(state.mediaId) { mutableStateOf(0L) }
    var showQueue by rememberSaveable(state.mediaId) { mutableStateOf(false) }
    var compactPlayer by rememberSaveable(state.mediaId) { mutableStateOf(false) }
    var compactOffsetX by rememberSaveable(state.mediaId) { mutableStateOf(0f) }
    var compactOffsetY by rememberSaveable(state.mediaId) { mutableStateOf(0f) }
    val screenConfiguration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { screenConfiguration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { screenConfiguration.screenHeightDp.dp.toPx() }
    val compactHorizontalLimit = (screenWidthPx / 2f - with(LocalDensity.current) { 145.dp.toPx() }).coerceAtLeast(0f)
    val compactVerticalLimit = (screenHeightPx - with(LocalDensity.current) { 200.dp.toPx() }).coerceAtLeast(0f)
    var showFullLyrics by rememberSaveable(state.mediaId) { mutableStateOf(false) }
    val lyricsListState = rememberLazyListState()
    val currentLyrics = if (lyricsMediaId == state.mediaId) lyrics else emptyList()
    LaunchedEffect(state.mediaId) {
        val saved = MusicLyricsStorage.load(context, state.mediaId)
        if (saved.isNotEmpty()) onLyricsChange(state.mediaId, saved)
        lyricsOffsetMs = MusicLyricsStorage.loadOffset(context, state.mediaId)
    }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val buffer = CharArray(4 * 1024)
                    val result = StringBuilder(4 * 1024)
                    while (result.length < 64 * 1024) {
                        val count = reader.read(buffer, 0, minOf(buffer.size, 64 * 1024 - result.length))
                        if (count < 0) break
                        result.append(buffer, 0, count)
                    }
                    result.toString()
                } ?: error("No se pudo leer el archivo.") }
                .onSuccess { raw ->
                    val boundedLyrics = raw.take(64 * 1024)
                    MusicLyricsStorage.save(context, state.mediaId, boundedLyrics)
                    onLyricsChange(state.mediaId, MusicLyricsParser.parse(boundedLyrics))
                }
        }
    }
    LaunchedEffect(state.mediaId, state.isPlaying, visualMode, expandedLyrics, isSeeking, performanceMode) {
        while (true) {
            durationMs = MusicPlaybackController.currentDuration()
            if (!isSeeking) positionMs = MusicPlaybackController.currentPosition()
            delay(
                when {
                    !state.isPlaying -> if (performanceMode == "battery") 1_800L else 1_000L
                    visualMode == "karaoke" && performanceMode == "performance" -> 120L
                    visualMode == "karaoke" && performanceMode == "battery" -> 360L
                    visualMode == "karaoke" -> 180L
                    performanceMode == "battery" -> 1_200L
                    performanceMode == "performance" -> 450L
                    else -> 700L
                }
            )
        }
    }
    fun timeLabel(value: Long): String {
        val seconds = (value.coerceAtLeast(0L) / 1000L)
        return "%d:%02d".format(seconds / 60L, seconds % 60L)
    }
    val lyricPositionMs = (positionMs + lyricsOffsetMs).coerceAtLeast(0L)
    val activeIndex = remember(currentLyrics, lyricPositionMs) {
        findActiveLyricIndex(currentLyrics, lyricPositionMs)
    }
    val previousLine = currentLyrics.getOrNull(activeIndex - 1)
    val activeLine = currentLyrics.getOrNull(activeIndex)
    val nextLine = currentLyrics.getOrNull(activeIndex + 1)
    LaunchedEffect(activeIndex, showFullLyrics) {
        if (showFullLyrics && activeIndex >= 0 && activeIndex < currentLyrics.size) lyricsListState.animateScrollToItem(activeIndex)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        if (!compactPlayer) {
        Surface(color = colors.surface, shape = RoundedCornerShape(20.dp), shadowElevation = 14.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 82.dp)) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!state.artworkUri.isNullOrBlank()) {
                        SubcomposeAsyncImage(model = state.artworkUri, contentDescription = "Portada", contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                    } else {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceAlt), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.title, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.artist.ifBlank { "Reproduciendo ahora" }, color = colors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { MusicPlaybackController.previous(context) }, enabled = state.hasPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = colors.text) }
                        IconButton(onClick = { if (state.isPlaying) MusicPlaybackController.pause(context) else MusicPlaybackController.resume(context) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (state.isPlaying) "Pausar" else "Continuar", tint = colors.accent) }
                        IconButton(onClick = { MusicPlaybackController.next(context) }, enabled = state.hasNext) { Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = colors.text) }
                        TextButton(onClick = { compactPlayer = true }) { Text("−", color = colors.muted, fontSize = 18.sp) }
                    }
                }
                if (!state.playbackError.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3A2529)).padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                        Text(state.playbackError, color = Color(0xFFFFDAD6), fontSize = 10.sp,
                            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { MusicPlaybackController.retry(context) }) {
                            Text("Reintentar", color = Color(0xFFFFB4AB), fontSize = 10.sp)
                        }
                    }
                }
                if (durationMs > 0L) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Slider(
                            value = positionMs.coerceIn(0L, durationMs).toFloat(),
                            onValueChange = { positionMs = it.toLong(); isSeeking = true },
                            onValueChangeFinished = { MusicPlaybackController.seekTo(positionMs); isSeeking = false },
                            valueRange = 0f..durationMs.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(timeLabel(positionMs), color = colors.muted, fontSize = 9.sp)
                            Text(timeLabel(durationMs), color = colors.muted, fontSize = 9.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("normal" to "Normal", "karaoke" to "Karaoke", "lyrics" to "Letra", "minimal" to "Minimal").forEach { (mode, label) ->
                        val selected = visualMode == mode
                        Surface(color = if (selected) colors.accent else colors.surfaceAlt, shape = RoundedCornerShape(30.dp), modifier = Modifier.clickable { onVisualModeChange(mode) }) {
                            Text(label, color = if (selected) colors.accentText else colors.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (visualMode == "karaoke" || visualMode == "lyrics") TextButton(onClick = { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Abrir .LRC", color = colors.accent, fontSize = 10.sp) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        val current = speeds.indexOfFirst { kotlin.math.abs(it - state.playbackSpeed) < 0.02f }
                        MusicPlaybackController.setPlaybackSpeed(context, speeds[(current + 1).mod(speeds.size)])
                    }) {
                        Text(state.playbackSpeed.toString() + "×", color = colors.muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = { MusicPlaybackController.toggleShuffle(context) }) {
                        Text(if (state.shuffleEnabled) "Aleatorio: sí" else "Aleatorio", color = if (state.shuffleEnabled) colors.accent else colors.muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = { MusicPlaybackController.cycleRepeat(context) }) {
                        val repeatLabel = when (state.repeatMode) { 1 -> "Repetir: una"; 2 -> "Repetir: todo"; else -> "Repetir: no" }
                        Text(repeatLabel, color = if (state.repeatMode != 0) colors.accent else colors.muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = { showQueue = true }) { Text("Cola · ${state.queue.size}", color = colors.muted, fontSize = 10.sp) }
                }
                when (visualMode) {
                    "karaoke" -> Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(colors.surfaceAlt).clickable { expandedLyrics = true }.padding(horizontal = 14.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(activeLine?.text ?: if (currentLyrics.isEmpty()) "Importa un archivo .LRC para ver la letra" else "La letra comenzará con la canción", color = colors.accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 22.sp)
                        Text(nextLine?.text.orEmpty(), color = colors.muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Toca para abrir karaoke a pantalla completa", color = colors.muted, fontSize = 9.sp)
                        if (currentLyrics.isEmpty()) Text("Usa letras LRC que tengas guardadas en tu dispositivo.", color = colors.muted, fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    "lyrics" -> Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(colors.surfaceAlt).clickable { expandedLyrics = true; showFullLyrics = true }.padding(horizontal = 14.dp, vertical = 17.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(activeLine?.text ?: if (currentLyrics.isEmpty()) "Importa un archivo .LRC" else "♪", color = colors.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 25.sp)
                        Text("LETRA · toca para ampliar", color = colors.muted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    "minimal" -> Text("♫  " + state.artist.ifBlank { state.title }, color = colors.accent, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        } else {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 14.dp,
                modifier = Modifier.width(280.dp)
                    .offset { IntOffset(compactOffsetX.roundToInt(), compactOffsetY.roundToInt()) }
                    .pointerInput(state.mediaId) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            compactOffsetX = (compactOffsetX + dragAmount.x).coerceIn(-compactHorizontalLimit, compactHorizontalLimit)
                            compactOffsetY = (compactOffsetY + dragAmount.y).coerceIn(-compactVerticalLimit, 0f)
                        }
                    }
                    .padding(vertical = 86.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⋮⋮", color = colors.muted, fontSize = 13.sp)
                    if (!state.artworkUri.isNullOrBlank()) {
                        SubcomposeAsyncImage(model = state.artworkUri, contentDescription = "Portada", contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
                    } else {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(colors.surfaceAlt), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.title, color = colors.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.artist.ifBlank { "Reproduciendo ahora" }, color = colors.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { if (state.isPlaying) MusicPlaybackController.pause(context) else MusicPlaybackController.resume(context) }) {
                        Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Reproducir o pausar", tint = colors.accent)
                    }
                    TextButton(onClick = { compactPlayer = false }) { Text("Expandir", color = colors.accent, fontSize = 10.sp) }
                }
            }
        }
    }
    if (expandedLyrics && visualMode in setOf("karaoke", "lyrics")) {
        Dialog(onDismissRequest = { expandedLyrics = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize().padding(0.dp), color = colors.background, shape = RoundedCornerShape(0.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (showFullLyrics) "LETRA COMPLETA" else "MODO KARAOKE", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(state.title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(state.artist, color = colors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showFullLyrics = !showFullLyrics }) {
                                Text(if (showFullLyrics) "Karaoke" else "Letra completa", color = colors.accent, fontSize = 11.sp)
                            }
                            TextButton(onClick = { expandedLyrics = false }) { Text("Cerrar", color = colors.accent) }
                        }
                    }
                    if (showFullLyrics) {
                        if (currentLyrics.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("Importa un archivo .LRC para mostrar la letra completa.", color = colors.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        } else {
                            LazyColumn(state = lyricsListState, modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
                                itemsIndexed(currentLyrics) { index, line ->
                                    Text(
                                        line.text,
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (index == activeIndex) Color(0xFF293421) else Color.Transparent).clickable { MusicPlaybackController.seekTo((line.timeMs - lyricsOffsetMs).coerceAtLeast(0L)) }.padding(horizontal = 14.dp, vertical = 12.dp),
                                        color = if (index == activeIndex) colors.accent else colors.muted,
                                        fontSize = if (index == activeIndex) 19.sp else 15.sp,
                                        fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(previousLine?.text.orEmpty(), color = Color(0xFF646D7B), fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 24.sp)
                            Spacer(Modifier.height(30.dp))
                            Text(activeLine?.text ?: if (currentLyrics.isEmpty()) "Importa tu archivo .LRC" else "♪", color = colors.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 34.sp)
                            Spacer(Modifier.height(30.dp))
                            Text(nextLine?.text.orEmpty(), color = colors.muted, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 24.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { lyricsOffsetMs = (lyricsOffsetMs - 500L).coerceAtLeast(-10_000L); MusicLyricsStorage.saveOffset(context, state.mediaId, lyricsOffsetMs) }) { Text("−0,5 s", color = colors.accent, fontSize = 11.sp) }
                            Text("Ajuste ${if (lyricsOffsetMs >= 0) "+" else ""}${lyricsOffsetMs / 1000.0}s", color = colors.muted, fontSize = 10.sp)
                            TextButton(onClick = { lyricsOffsetMs = (lyricsOffsetMs + 500L).coerceAtMost(10_000L); MusicLyricsStorage.saveOffset(context, state.mediaId, lyricsOffsetMs) }) { Text("+0,5 s", color = colors.accent, fontSize = 11.sp) }
                            TextButton(onClick = { lyricsOffsetMs = 0L; MusicLyricsStorage.saveOffset(context, state.mediaId, 0L) }) { Text("Reset", color = colors.muted, fontSize = 10.sp) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            IconButton(onClick = { MusicPlaybackController.previous(context) }, enabled = state.hasPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = colors.text, modifier = Modifier.size(30.dp)) }
                        IconButton(onClick = { if (state.isPlaying) MusicPlaybackController.pause(context) else MusicPlaybackController.resume(context) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Reproducir o pausar", tint = colors.accent, modifier = Modifier.size(42.dp)) }
                        IconButton(onClick = { MusicPlaybackController.next(context) }, enabled = state.hasNext) { Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = colors.text, modifier = Modifier.size(30.dp)) }
                        }
                    }
                }
            }
        }
    }
    if (showQueue) {
        Dialog(onDismissRequest = { showQueue = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                color = colors.surface,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cola de reproducción", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${state.queue.size} canciones", color = colors.muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = { MusicPlaybackController.clearQueue(context); showQueue = false }) {
                            Text("Vaciar", color = Color(0xFFFF8A80), fontSize = 11.sp)
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().height(340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        state.queue.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (item.isCurrent) Color(0xFF343D2B) else Color(0xFF292D36))
                                    .clickable { MusicPlaybackController.playQueueItem(context, item.index); showQueue = false }
                                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (item.isCurrent) "♫" else "${item.index + 1}", color = if (item.isCurrent) colors.accent else colors.muted, fontSize = 12.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = if (item.isCurrent) colors.accent else colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.artist.ifBlank { "Audio" }, color = colors.muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { MusicPlaybackController.removeQueueItem(context, item.index) }) {
                                    Text("×", color = Color(0xFFFF8A80), fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { showQueue = false }, modifier = Modifier.align(Alignment.End)) { Text("Cerrar", color = colors.accent) }
                }
            }
        }
    }
}

@Composable
private fun MusicFeedbackBanner(feedback: MusicFeedback?, colors: MusicColors) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut(),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 150.dp)
        ) {
            val current = feedback
            if (current != null) {
                val tint = when (current.type) {
                    MusicFeedbackType.SUCCESS -> Color(0xFF27965A)
                    MusicFeedbackType.ERROR -> Color(0xFFD94B4B)
                    MusicFeedbackType.INFO -> Color(0xFF397BC8)
                    MusicFeedbackType.LOADING -> Color(0xFF7A69C7)
                }
                Surface(color = colors.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (current.type == MusicFeedbackType.LOADING) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(23.dp), color = colors.accent, strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = when (current.type) {
                                    MusicFeedbackType.SUCCESS -> Icons.Default.CheckCircle
                                    MusicFeedbackType.ERROR -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null, tint = tint, modifier = Modifier.size(25.dp)
                            )
                        }
                        Text(current.message, color = colors.text, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

private data class MusicColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentText: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicTopBar(colors: MusicColors, darkMode: Boolean, onThemeChange: () -> Unit, onSearch: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accentText)
                }
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy((-3).dp)) {
                    Text("music", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = colors.text)
                    Text("TU ESPACIO SONORO", fontSize = 8.sp, letterSpacing = 1.4.sp, color = colors.muted)
                }
            }
        },
        actions = {
            IconButton(onClick = onThemeChange) {
                Icon(
                    if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar tema",
                    tint = colors.text
                )
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = colors.text)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
    )
}

@Composable
private fun HomeScreen(colors: MusicColors, padding: PaddingValues, repository: MusicLibraryRepository, onExplore: (String) -> Unit) {
    val recentTracksState by repository.recentlyPlayedTracks.collectAsState(initial = null as List<LocalTrackEntity>?)
    val recentTracks = recentTracksState.orEmpty()
    val favoriteCount by repository.favoriteCount.collectAsState(initial = 0)
    val playbackCount by repository.playbackCount.collectAsState(initial = 0)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Tu música, a tu manera.", color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Todo lo que te gusta, organizado en un solo lugar.", color = colors.muted, fontSize = 14.sp)
            }
        }
        item {
            FeaturedCard(colors) { onExplore("video") }
        }
        item {
            SectionHeader("Tu actividad musical", colors)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCollection("Reproducciones", playbackCount.toString(), "♫", colors, Modifier.weight(1f))
                MiniCollection("Favoritos", favoriteCount.toString(), "♥", colors, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(colors.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Escuchado recientemente", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (recentTracksState == null) {
                    repeat(3) { CatalogEntrySkeleton(colors) }
                } else if (recentTracks.isEmpty()) {
                    Text("Tus canciones aparecerán aquí cuando empieces a reproducirlas.", color = colors.muted, fontSize = 11.sp)
                } else {
                    recentTracks.take(4).forEach { track ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = colors.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artistName.ifBlank { "Audio local" }, color = colors.muted, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Explora tu biblioteca", colors)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LibraryShortcut("Artistas", "Organiza por intérprete", Icons.Default.Person, colors) { onExplore("artist") }
                LibraryShortcut("Álbumes", "Discos y colecciones", Icons.Default.Album, colors) { onExplore("album") }
                LibraryShortcut("Canciones", "Toda tu música", Icons.Default.MusicNote, colors) { onExplore("track") }
            }
        }
        item {
            Text(
                "Modo invitado · Tus datos locales permanecen en este dispositivo",
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FeaturedCard(colors: MusicColors, onExplore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF30224F), Color(0xFF644B9A), Color(0xFF9A82D0))
                )
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("TU BIBLIOTECA EMPIEZA AQUÍ", color = Color.White.copy(alpha = .8f), fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text("Dale espacio\na lo que te mueve.", color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onExplore() }
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Explorar música", color = Color(0xFF25351A), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.width(7.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF25351A), modifier = Modifier.size(15.dp))
            }
        }
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = .13f),
            modifier = Modifier.align(Alignment.CenterEnd).size(116.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, colors: MusicColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniCollection(title: String, subtitle: String, symbol: String, colors: MusicColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(colors.surfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = colors.accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = colors.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryShortcut(title: String, subtitle: String, icon: ImageVector, colors: MusicColors, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(colors.surfaceAlt),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = colors.accent) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = colors.muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = colors.muted, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun LibraryScreen(
    colors: MusicColors,
    padding: PaddingValues,
    repository: MusicLibraryRepository,
    showFeedback: (String, MusicFeedbackType) -> Unit
) {
    var filter by rememberSaveable { mutableStateOf("Todo") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var trackPendingRemoval by remember { mutableStateOf<LocalTrackEntity?>(null) }
    val filters = listOf("Todo", "Favoritos")
    val visibleTracks by remember(searchQuery, filter) {
        if (filter == "Favoritos") repository.searchFavorites(searchQuery) else repository.search(searchQuery)
    }.collectAsState(initial = null as List<LocalTrackEntity>?)
    val libraryTracks = visibleTracks.orEmpty()
    val playbackCount by repository.playbackCount.collectAsState(initial = 0)
    val mostPlayed by repository.mostPlayed.collectAsState(initial = emptyList())
    val recentPlayback by repository.recentPlayback.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scanStatus by rememberSaveable { mutableStateOf("La exploración importa hasta 100 archivos por tanda.") }
    val scanPermissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) {
            val audioAllowed = Build.VERSION.SDK_INT < 33 || grants[Manifest.permission.READ_MEDIA_AUDIO] == true
            val videoAllowed = Build.VERSION.SDK_INT < 33 || grants[Manifest.permission.READ_MEDIA_VIDEO] == true
            scope.launch {
                showFeedback("Preparando tu biblioteca musical…", MusicFeedbackType.LOADING)
                runCatching {
                    MusicDeviceScanner.scanNextMediaBatch(
                        context, repository,
                        includeAudio = audioAllowed,
                        includeVideo = videoAllowed
                    )
                }.onSuccess { result ->
                    showFeedback("Biblioteca actualizada correctamente.", MusicFeedbackType.SUCCESS)
                    scanStatus = if (result.imported == 0) "No quedan más archivos multimedia con los permisos concedidos." else
                        "Se añadieron ${result.imported} archivos. " + if (result.hasMore) "Puedes continuar con la siguiente tanda." else "Exploración completada."
                }.onFailure { scanStatus = "No hemos podido terminar la exploración. Comprueba los permisos e inténtalo de nuevo; tu música guardada sigue a salvo."; showFeedback("No se pudo explorar la biblioteca. Revisa los permisos e inténtalo de nuevo.", MusicFeedbackType.ERROR) }
            }
        } else {
            scanStatus = "Concede permiso para leer audio o vídeo para continuar."
            showFeedback("Se necesitan permisos para explorar los archivos del dispositivo.", MusicFeedbackType.ERROR)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            showFeedback("Añadiendo ${uris.size} archivo(s)…", MusicFeedbackType.LOADING)
            val importedTracks = withContext(Dispatchers.IO) {
                buildList {
                    uris.forEach { uri ->
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            val displayName = context.contentResolver.query(
                                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "Archivo de audio"
                            add(LocalTrackEntity(
                                id = uri.toString(),
                                title = displayName.substringBeforeLast('.', displayName),
                                mediaUri = uri.toString(),
                                source = "device"
                            ))
                        }
                    }
                }
            }
            if (importedTracks.isNotEmpty()) {
                runCatching { repository.addTracks(importedTracks) }
                    .onSuccess { showFeedback("${importedTracks.size} archivo(s) añadido(s) a tu biblioteca.", MusicFeedbackType.SUCCESS) }
                    .onFailure { showFeedback("No se pudieron guardar los archivos seleccionados.", MusicFeedbackType.ERROR) }
            } else showFeedback("No se pudieron añadir los archivos seleccionados.", MusicFeedbackType.ERROR)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Biblioteca", color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Tu colección local, disponible sin conexión.", color = colors.muted, fontSize = 13.sp)
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(colors.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tu actividad musical", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Historial guardado en este dispositivo", color = colors.muted, fontSize = 11.sp)
                    }
                    Text("$playbackCount", color = colors.accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Text("REPRODUCCIONES", color = colors.muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
                if (mostPlayed.isNotEmpty()) {
                    Text("Más escuchadas", color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    mostPlayed.take(3).forEachIndexed { index, stat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stat.title, color = colors.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stat.artistName.ifBlank { "Artista desconocido" }, color = colors.muted, fontSize = 10.sp)
                            }
                            Text("${stat.playCount}x", color = colors.muted, fontSize = 11.sp)
                        }
                    }
                } else if (recentPlayback.isEmpty()) {
                    Text("Cuando reproduzcas canciones de tu biblioteca, aparecerán aquí tus estadísticas.", color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
                } else {
                    Text("Última reproducción: ${recentPlayback.first().title}", color = colors.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar en tu biblioteca") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
        }
        item {
            Button(onClick = { filePicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir archivos del dispositivo")
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Exploración inteligente", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Music explora hasta 100 canciones y vídeos por tanda para acelerar la importación sin cargar toda la biblioteca de una vez.", color = colors.muted, fontSize = 12.sp)
                Text(scanStatus, color = colors.muted, fontSize = 11.sp)
                Button(onClick = { scanLauncher.launch(scanPermissions) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Explorar los siguientes archivos")
                }
            }
        }
        if (libraryTracks.any { it.mediaUri != null && it.source != "device-video" }) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { runCatching { MusicPlaybackController.previous(context) }.onFailure { showFeedback("No se pudo volver a la canción anterior.", MusicFeedbackType.ERROR) } }) { Text("Anterior", color = colors.accent, fontSize = 11.sp) }
                    TextButton(onClick = { runCatching { MusicPlaybackController.pause(context) }.onFailure { showFeedback("No se pudo pausar la reproducción.", MusicFeedbackType.ERROR) } }) { Text("Pausa", color = colors.text, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { runCatching { MusicPlaybackController.resume(context) }.onFailure { showFeedback("No se pudo reanudar la reproducción.", MusicFeedbackType.ERROR) } }) { Text("Continuar", color = colors.accent, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { runCatching { MusicPlaybackController.next(context) }.onFailure { showFeedback("No se pudo pasar a la siguiente canción.", MusicFeedbackType.ERROR) } }) { Text("Siguiente", color = colors.accent, fontSize = 11.sp) }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { option ->
                    val selected = filter == option
                    Surface(
                        color = if (selected) colors.accent else colors.surface,
                        shape = CircleShape,
                        modifier = Modifier.clickable { filter = option }
                    ) {
                        Text(
                            option,
                            color = if (selected) colors.accentText else colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
        if (visibleTracks == null) {
            items(4) { CatalogEntrySkeleton(colors) }
        } else if (libraryTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(colors.surface).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp))
                    Text(
                        if (filter == "Favoritos") "Todavía no tienes favoritos" else "Tu biblioteca está vacía",
                        color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp
                    )
                    Text(
                        "Añade archivos de audio que tengas en el dispositivo. Music guardará sus referencias para encontrarlos incluso sin conexión.",
                        color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp
                    )
                }
            }
        } else {
            item {
                Text(
                    "${libraryTracks.size} elemento(s)",
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
            items(libraryTracks, key = { it.id }) { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(colors.surface).clickable {
                            val media = track.mediaUri
                            if (media.isNullOrBlank()) {
                                showFeedback("Este elemento no tiene un archivo reproducible.", MusicFeedbackType.ERROR)
                            } else if (track.source == "device-video") {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(media), "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    })
                                }.onSuccess {
                                    showFeedback("Abriendo vídeo…", MusicFeedbackType.INFO)
                                }.onFailure {
                                    showFeedback("No se pudo abrir este vídeo en el dispositivo.", MusicFeedbackType.ERROR)
                                }
                            } else {
                                showFeedback("Preparando la reproducción…", MusicFeedbackType.LOADING)
                                runCatching {
                                    val audioQueue = libraryTracks.filter { it.mediaUri != null && it.source != "device-video" }
                                        .map { MusicPlaybackItem(Uri.parse(it.mediaUri), it.title, it.artistName, it.albumTitle, it.artworkUri) }
                                    val selectedIndex = audioQueue.indexOfFirst { it.uri == Uri.parse(media) }.coerceAtLeast(0)
                                    MusicPlaybackController.playQueue(context, audioQueue, selectedIndex) { result ->
                                        result.onSuccess {
                                            showFeedback("Reproduciendo " + track.title + ".", MusicFeedbackType.INFO)
                                        }.onFailure {
                                            showFeedback("No se pudo iniciar la reproducción. Comprueba el archivo e inténtalo de nuevo.", MusicFeedbackType.ERROR)
                                        }
                                    }
                                }.onFailure {
                                    showFeedback("No se pudo preparar esta canción.", MusicFeedbackType.ERROR)
                                }
                            }
                        }.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceAlt),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (track.source == "device-video") "Vídeo del dispositivo" else if (track.source == "device-audio" || track.source == "device") track.artistName.ifBlank { "Audio local" } else "Catálogo", color = colors.muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { repository.setFavorite(track.id, !track.isFavorite) }
                                .onSuccess {
                                    showFeedback(
                                        if (track.isFavorite) "Se quitó de tus favoritos." else "¡Canción guardada correctamente en favoritos!",
                                        MusicFeedbackType.SUCCESS
                                    )
                                }
                                .onFailure {
                                    showFeedback("No se pudo actualizar favoritos. Inténtalo de nuevo.", MusicFeedbackType.ERROR)
                                }
                        }
                    }) {
                        Text(if (track.isFavorite) "♥" else "♡", color = colors.accent, fontSize = 19.sp)
                    }
                    TextButton(onClick = {
                        track.mediaUri?.let { mediaUri ->
                            runCatching {
                                val uri = Uri.parse(mediaUri)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "audio/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    clipData = android.content.ClipData.newUri(
                                        context.contentResolver, track.title, uri
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir archivo de audio"))
                            }.onSuccess {
                                showFeedback("Archivo preparado para compartir.", MusicFeedbackType.SUCCESS)
                            }.onFailure {
                                showFeedback("No hemos podido preparar este archivo para compartir.", MusicFeedbackType.ERROR)
                            }
                        }
                    }, enabled = !track.mediaUri.isNullOrBlank()) {
                        Text("↗", color = colors.accent, fontSize = 17.sp)
                    }
                    TextButton(onClick = { trackPendingRemoval = track }) {
                        Text("Quitar", color = colors.muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
    trackPendingRemoval?.let { track ->
        AlertDialog(
            onDismissRequest = { trackPendingRemoval = null },
            title = { Text("Quitar canción") },
            text = { Text("¿Quieres quitar esta canción de tu biblioteca?") },
            confirmButton = {
                TextButton(onClick = {
                    trackPendingRemoval = null
                    scope.launch {
                        runCatching { repository.removeTrack(track.id) }
                            .onSuccess { showFeedback("La canción se quitó de tu biblioteca.", MusicFeedbackType.SUCCESS) }
                            .onFailure { showFeedback("No se pudo quitar la canción. Inténtalo de nuevo.", MusicFeedbackType.ERROR) }
                    }
                }) { Text("Quitar") }
            },
            dismissButton = {
                TextButton(onClick = { trackPendingRemoval = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SimpleSectionScreen(
    colors: MusicColors,
    padding: PaddingValues,
    title: String,
    description: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(36.dp))
            Text("Este espacio es tuyo", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = colors.muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun ArtworkSkeleton(modifier: Modifier = Modifier, colors: MusicColors) {
    val transition = rememberInfiniteTransition(label = "artwork-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "artwork-skeleton-alpha"
    )
    Box(modifier = modifier.background(colors.surfaceAlt.copy(alpha = alpha)))
}

@Composable
private fun ArtworkFallback(colors: MusicColors) {
    Box(
        modifier = Modifier.fillMaxSize().background(colors.surfaceAlt),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent)
    }
}

@Composable
private fun CatalogEntrySkeleton(colors: MusicColors) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(colors.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkSkeleton(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)), colors)
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            ArtworkSkeleton(Modifier.fillMaxWidth(0.72f).height(12.dp).clip(RoundedCornerShape(8.dp)), colors)
            ArtworkSkeleton(Modifier.fillMaxWidth(0.46f).height(9.dp).clip(RoundedCornerShape(8.dp)), colors)
        }
    }
}

@Composable
private fun CatalogBrowserScreen(
    colors: MusicColors,
    padding: PaddingValues,
    repository: MusicLibraryRepository,
    showFeedback: (String, MusicFeedbackType) -> Unit,
    kind: String,
    onKindChange: (String) -> Unit
) {
    var syncStatus by rememberSaveable { mutableStateOf("Catálogo guardado en este dispositivo") }
    var syncing by rememberSaveable { mutableStateOf(false) }
    var catalogQuery by rememberSaveable { mutableStateOf("") }
    val entries by repository.catalog(kind).collectAsState(initial = null as List<CatalogEntryEntity>?)
    val searchResults by repository.searchCatalog(catalogQuery.trim()).collectAsState(initial = null as List<CatalogEntryEntity>?)
    val visibleEntries = if (catalogQuery.isBlank()) entries else searchResults
    val localMedia by repository.tracks.collectAsState(initial = emptyList())
    var sourceChoice by remember { mutableStateOf<Pair<CatalogEntryEntity, LocalTrackEntity>?>(null) }
    var selectedDetail by remember { mutableStateOf<CatalogEntryEntity?>(null) }
    val detailId = selectedDetail?.id.orEmpty()
    val detailTracks by repository.catalogByArtist(detailId, "track").collectAsState(initial = emptyList())
    val detailVideos by repository.catalogByArtist(detailId, "video").collectAsState(initial = emptyList())
    val albumTracks by repository.catalogByAlbum(detailId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataSaver by MusicPreferences.dataSaver(context).collectAsState(initial = false)
    val equalizer by MusicEqualizerController.state.collectAsState()
    val kinds = listOf("video" to "Vídeos", "artist" to "Artistas", "album" to "Álbumes", "track" to "Canciones")

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
        Text("Explorar catálogo", color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            kinds.forEach { (value, label) ->
                Surface(
                    color = if (kind == value) colors.accent else colors.surface,
                    shape = CircleShape,
                    modifier = Modifier.clickable { onKindChange(value) }
                ) {
                    Text(label, color = if (kind == value) colors.accentText else colors.muted,
                        fontSize = 12.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
                }
            }
        }
        OutlinedTextField(
            value = catalogQuery,
            onValueChange = { catalogQuery = it },
            singleLine = true,
            label = { Text("Buscar artistas, álbumes, canciones o vídeos") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = {
                if (!syncing) {
                    syncing = true
                    showFeedback("Actualizando el catálogo…", MusicFeedbackType.LOADING)
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                MusicCatalogSyncRepository(repository.databaseDao).syncAll()
                            }
                        }
                        syncStatus = result.fold(
                            { "Actualizado: ${it.artists} artistas · ${it.albums} álbumes · ${it.tracks} canciones · ${it.videos} vídeos" },
                            { "No hemos podido actualizar el catálogo. Tu contenido anterior sigue guardado; puedes volver a intentarlo." }
                        )
                        if (result.isSuccess) showFeedback("Catálogo actualizado correctamente.", MusicFeedbackType.SUCCESS)
                        else showFeedback("No se pudo actualizar el catálogo. Se conserva lo descargado.", MusicFeedbackType.ERROR)
                        syncing = false
                    }
                }
            },
            enabled = !syncing,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)
        ) { Text(if (syncing) "Sincronizando…" else "Actualizar catálogo") }
        Text(syncStatus, color = colors.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (visibleEntries == null || (visibleEntries.isEmpty() && syncing)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)) {
                repeat(5) { CatalogEntrySkeleton(colors) }
            }
        } else if (visibleEntries.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = colors.accent, modifier = Modifier.size(42.dp))
                Text(if (catalogQuery.isBlank()) "Todavía no hay contenido" else "No hay resultados", color = colors.text, fontWeight = FontWeight.SemiBold)
                Text(if (catalogQuery.isBlank()) "Actualiza el catálogo cuando tengas conexión. Lo sincronizado quedará disponible sin conexión." else "Prueba con otro título o nombre de artista.", color = colors.muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(visibleEntries, key = { "${it.kind}-${it.id}" }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                            .background(colors.surface).clickable {
                                if (entry.kind == "artist" || entry.kind == "album") {
                                    selectedDetail = entry
                                } else if (entry.kind == "video" || entry.kind == "track") {
                                    val expectedSource = if (entry.kind == "video") "device-video" else "device-audio"
                                    val normalized = normalizeMusicTitle(entry.title)
                                    val match = localMedia.firstOrNull { local ->
                                        val localTitle = normalizeMusicTitle(local.title)
                                        (local.source == expectedSource || (entry.kind == "track" && local.source == "device")) && localTitle.isNotBlank() &&
                                            (localTitle == normalized || localTitle.contains(normalized) || normalized.contains(localTitle)) &&
                                            (entry.subtitle.isBlank() || local.artistName.isBlank() ||
                                                normalizeMusicTitle(local.artistName).contains(normalizeMusicTitle(entry.subtitle)) ||
                                                normalizeMusicTitle(entry.subtitle).contains(normalizeMusicTitle(local.artistName)))
                                    }
                                    if (match != null) sourceChoice = entry to match
                                    else {
                                        val url = if (entry.provider.equals("youtube", true) && !entry.providerId.isNullOrBlank())
                                            "https://www.youtube.com/watch?v=${entry.providerId}" else entry.mediaPath
                                        if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                        .onFailure { showFeedback("No se pudo abrir este contenido.", MusicFeedbackType.ERROR) }
                                    }
                                }
                            }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!dataSaver && !entry.imagePath.isNullOrBlank()) {
                            val artworkModel = remember(entry.imagePath, context) {
                                ImageRequest.Builder(context)
                                    .data(if (entry.imagePath.startsWith("http")) entry.imagePath
                                        else "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/music-artwork/${entry.imagePath.trimStart('/')}")
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(180)
                                    .build()
                            }
                            SubcomposeAsyncImage(
                                model = artworkModel,
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
                                loading = { ArtworkSkeleton(Modifier.fillMaxSize(), colors) },
                                error = { ArtworkFallback(colors) },
                                fallback = { ArtworkFallback(colors) }
                            )
                        } else {
                            Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceAlt),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.subtitle.ifBlank { entry.provider ?: "Music" }, color = colors.muted, fontSize = 11.sp, maxLines = 1)
                        }
                        if (entry.kind == "track") {
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { repository.addTrack(LocalTrackEntity(
                                        id = "catalog:${entry.id}", title = entry.title,
                                        artistName = entry.subtitle, artworkUri = entry.imagePath,
                                        source = "catalog"
                                    )) }.onSuccess { showFeedback("Canción guardada en tu biblioteca.", MusicFeedbackType.SUCCESS) }
                                        .onFailure { showFeedback("No se pudo guardar la canción.", MusicFeedbackType.ERROR) }
                                }
                            }) { Text("Guardar", color = colors.accent, fontSize = 11.sp) }
                        }
                        TextButton(onClick = {
                            scope.launch {
                                showFeedback("Preparando tu enlace compartible…", MusicFeedbackType.LOADING)
                                val tokenResult = runCatching {
                                    withContext(Dispatchers.IO) {
                                        MusicShareRepository.create(entry.kind, entry.id)
                                    }
                                }
                                tokenResult.onSuccess { token ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, MusicShareRepository.publicUrl(token))
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(shareIntent, "Compartir en Music"))
                                    }.onSuccess {
                                        showFeedback("Tu enlace está listo para compartir.", MusicFeedbackType.SUCCESS)
                                    }.onFailure {
                                        showFeedback("El enlace se creó, pero no se pudo abrir el menú para compartir.", MusicFeedbackType.ERROR)
                                    }
                                }.onFailure {
                                    showFeedback("No hemos podido crear el enlace. Comprueba tu conexión e inténtalo de nuevo.", MusicFeedbackType.ERROR)
                                }
                            }
                        }) { Text("Compartir", color = colors.accent, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
    selectedDetail?.let { detail ->
        val isArtist = detail.kind == "artist"
        val related = if (isArtist) detailTracks + detailVideos else albumTracks
        val detailArtwork = remember(detail.imagePath, context) {
            detail.imagePath?.takeIf { it.isNotBlank() }?.let { path ->
                ImageRequest.Builder(context)
                    .data(if (path.startsWith("http")) path else
                        "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/music-artwork/${path.trimStart('/')}")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(180)
                    .build()
            }
        }
        AlertDialog(
            onDismissRequest = { selectedDetail = null },
            title = { Text(detail.title, color = colors.text) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (detailArtwork != null) {
                        SubcomposeAsyncImage(
                            model = detailArtwork,
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)),
                            loading = { ArtworkSkeleton(Modifier.fillMaxSize(), colors) },
                            error = { ArtworkFallback(colors) },
                            fallback = { ArtworkFallback(colors) }
                        )
                    }
                    Text(
                        if (isArtist) detail.subtitle.ifBlank { "Perfil del artista" }
                        else "Álbum · ${detail.subtitle.ifBlank { "Artista no indicado" }}",
                        color = colors.muted,
                        fontSize = 12.sp
                    )
                    Text(
                        if (isArtist) "Canciones y vídeos relacionados" else "Canciones del álbum",
                        color = colors.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (related.isEmpty()) {
                        Text(
                            if (isArtist) "Todavía no hay canciones ni vídeos vinculados a este artista."
                            else "Todavía no hay canciones vinculadas a este álbum.",
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                    } else {
                        related.distinctBy { it.id }.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceAlt).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (item.kind == "video") Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = colors.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (item.kind == "video") "Vídeo" else "Canción", color = colors.muted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedDetail = null }) {
                        Text("Cerrar", color = colors.muted)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            showFeedback("Preparando tu enlace…", MusicFeedbackType.LOADING)
                            val tokenResult = runCatching {
                                withContext(Dispatchers.IO) {
                                    MusicShareRepository.create(detail.kind, detail.id)
                                }
                            }
                            tokenResult.onSuccess { token ->
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, MusicShareRepository.publicUrl(token))
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(send, "Compartir en Music"))
                                }.onSuccess {
                                    showFeedback("Tu enlace está listo para compartir.", MusicFeedbackType.SUCCESS)
                                }.onFailure {
                                    showFeedback("No se pudo abrir el menú para compartir.", MusicFeedbackType.ERROR)
                                }
                            }.onFailure {
                                showFeedback("No hemos podido crear el enlace. Inténtalo de nuevo.", MusicFeedbackType.ERROR)
                            }
                        }
                    }) { Text("Compartir", color = colors.accent) }
                }
            }
        )
    }
    sourceChoice?.let { (online, offline) ->
        AlertDialog(
            onDismissRequest = { sourceChoice = null },
            title = { Text("Encontré una versión en tu dispositivo") },
            text = { Text("${online.title} · ${online.subtitle.ifBlank { "Artista no indicado" }}. Elige la fuente.") },
            confirmButton = {
                TextButton(onClick = {
                    if (offline.source == "device-video") {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(offline.mediaUri), "video/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }) }
                    } else offline.mediaUri?.let { MusicPlaybackController.play(context, MusicPlaybackItem(Uri.parse(it), offline.title, offline.artistName, offline.albumTitle, offline.artworkUri)) }
                    sourceChoice = null
                }) { Text("Offline · dispositivo") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val url = if (online.provider.equals("youtube", true) && !online.providerId.isNullOrBlank())
                        "https://www.youtube.com/watch?v=${online.providerId}" else online.mediaPath
                    if (!url.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    sourceChoice = null
                }) { Text("Online") }
            }
        )
    }
}

private fun normalizeMusicTitle(value: String): String =
    value.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "").trim()

@Composable
private fun PlaylistManagerScreen(
    colors: MusicColors,
    padding: PaddingValues,
    repository: MusicLibraryRepository,
    showFeedback: (String, MusicFeedbackType) -> Unit
) {
    val localLists by repository.playlists.collectAsState(initial = null as List<LocalPlaylistEntity>?)
    val loadedLists = localLists.orEmpty()
    val localTracks by repository.tracks.collectAsState(initial = emptyList())
    val favoriteTracks by repository.favorites.collectAsState(initial = emptyList())
    val recentPlayback by repository.recentPlayback.collectAsState(initial = emptyList())
    val mostPlayed by repository.mostPlayed.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var cloudTrackId by rememberSaveable { mutableStateOf("") }
    var targetPlaylistId by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var invitePlaylistId by rememberSaveable { mutableStateOf("") }
    var inviteUserId by rememberSaveable { mutableStateOf("") }
    var cloudLists by remember { mutableStateOf<List<CloudPlaylistDto>>(emptyList()) }
    var message by rememberSaveable { mutableStateOf("Las listas locales funcionan sin cuenta.") }
    var busy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Build one indexed lookup and reuse it for all smart-playlist projections.
    val tracksById = remember(localTracks) { localTracks.associateBy { it.id } }
    val recentTracks = remember(tracksById, recentPlayback) {
        recentPlayback.mapNotNull { entry -> tracksById[entry.trackId] }.distinctBy { it.id }
    }
    val topTracks = remember(tracksById, mostPlayed) {
        mostPlayed.mapNotNull { stat -> tracksById[stat.trackId] }.distinctBy { it.id }
    }
    val favoritePlayable = remember(favoriteTracks) {
        favoriteTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
    }
    val recentPlayable = remember(recentTracks) {
        recentTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
    }
    val topPlayable = remember(topTracks) {
        topTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Tus listas", color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Crea colecciones privadas o listas colaborativas.", color = colors.muted, fontSize = 12.sp)
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Nueva lista", color = colors.text, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        runCatching { repository.createPlaylist(name, description) }
                             .onSuccess { name = ""; description = ""; message = "Lista guardada en este dispositivo."; showFeedback("¡Tu lista se creó correctamente!", MusicFeedbackType.SUCCESS) }
                            .onFailure { message = "No se pudo crear la lista."; showFeedback("No se pudo crear la lista. Inténtalo de nuevo.", MusicFeedbackType.ERROR) }
                    }
                }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Crear lista local") }
                Button(onClick = {
                    if (!busy) {
                        busy = true
                        showFeedback("Creando tu lista colaborativa…", MusicFeedbackType.LOADING)
                        scope.launch {
                            runCatching { MusicCloudPlaylistRepository.create(name, description, true) }
                                .onSuccess { name = ""; description = ""; message = "Lista colaborativa creada en la nube."; showFeedback("¡Lista colaborativa creada!", MusicFeedbackType.SUCCESS) }
                                .onFailure { message = "No hemos podido crear la lista compartida. Comprueba que has iniciado sesión y vuelve a intentarlo."; showFeedback("No se pudo crear la lista compartida. Comprueba tu sesión.", MusicFeedbackType.ERROR) }
                            busy = false
                        }
                    }
                }, enabled = name.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Creando…" else "Crear lista colaborativa")
                }
            }
        }
        item { Text(message, color = colors.muted, fontSize = 11.sp) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Listas inteligentes", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Se actualizan automáticamente con tu actividad y tus favoritos.", color = colors.muted, fontSize = 11.sp)
                SmartPlaylistCard(
                    title = "Tus favoritos",
                    subtitle = "Todas las canciones que marcaste con ♥",
                    tracks = favoritePlayable,
                    colors = colors,
                    onPlay = { selected -> playSmartPlaylist(context, favoritePlayable, selected, repository, scope, showFeedback) }
                )
                SmartPlaylistCard(
                    title = "Escuchadas recientemente",
                    subtitle = "Lo último que has reproducido",
                    tracks = recentPlayable,
                    colors = colors,
                    onPlay = { selected -> playSmartPlaylist(context, recentPlayable, selected, repository, scope, showFeedback) }
                )
                SmartPlaylistCard(
                    title = "Tus más escuchadas",
                    subtitle = "Ordenadas por número de reproducciones",
                    tracks = topPlayable,
                    colors = colors,
                    onPlay = { selected -> playSmartPlaylist(context, topPlayable, selected, repository, scope, showFeedback) }
                )
            }
        }
        item { Text("En este dispositivo", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        if (loadedLists.isNotEmpty() && localTracks.isNotEmpty()) {
            item { Text("Añadir canciones a «${loadedLists.first().name}»", color = colors.muted, fontSize = 12.sp) }
            items(localTracks, key = { "local-track-${it.id}" }) { track ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(colors.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(track.artistName.ifBlank { "Audio local" }, color = colors.muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { repository.addToPlaylist(loadedLists.first().id, track.id) }
                                .onSuccess { message = "Canción añadida a tu lista local."; showFeedback("Canción añadida a la lista.", MusicFeedbackType.SUCCESS) }
                                .onFailure { showFeedback("No se pudo añadir la canción a la lista.", MusicFeedbackType.ERROR) }
                        }
                    }) { Text("Añadir", color = colors.accent) }
                }
            }
        }
        if (localLists == null) {
            items(3) { CatalogEntrySkeleton(colors) }
        } else if (loadedLists.isEmpty()) item { Text("Aún no tienes listas locales.", color = colors.muted, fontSize = 12.sp) }
        items(loadedLists, key = { it.id }) { list ->
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(list.name, color = colors.text, fontWeight = FontWeight.SemiBold)
                    Text(list.description.ifBlank { "Lista local" }, color = colors.muted, fontSize = 11.sp)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Listas compartidas", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        showFeedback("Cargando tus listas compartidas…", MusicFeedbackType.LOADING)
                        runCatching { MusicCloudPlaylistRepository.loadMyPlaylists() }
                            .onSuccess { cloudLists = it; message = "Listas de la nube actualizadas."; showFeedback("Listas sincronizadas correctamente.", MusicFeedbackType.SUCCESS) }
                            .onFailure { message = "Inicia sesión para consultar las listas compartidas."; showFeedback("No se pudieron cargar las listas. Comprueba tu conexión.", MusicFeedbackType.ERROR) }
                    }
                }) { Text("Actualizar", color = colors.accent) }
            }
        }
        if (cloudLists.isEmpty()) item { Text("Inicia sesión y pulsa Actualizar para ver tus listas compartidas.", color = colors.muted, fontSize = 12.sp) }
        items(cloudLists, key = { "cloud-${it.id}" }) { list ->
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).padding(14.dp)) {
                Text(list.name, color = colors.text, fontWeight = FontWeight.SemiBold)
                Text(if (list.isCollaborative) "Colaborativa · ID: ${list.id}" else "Privada · ID: ${list.id}",
                    color = colors.muted, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { targetPlaylistId = list.id; invitePlaylistId = list.id }) {
                        Text("Usar esta lista", color = colors.accent, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            showFeedback("Preparando enlace de tu lista…", MusicFeedbackType.LOADING)
                            val tokenResult = runCatching {
                                withContext(Dispatchers.IO) {
                                    MusicShareRepository.create("playlist", list.id)
                                }
                            }
                            tokenResult.onSuccess { token ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, MusicShareRepository.publicUrl(token))
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir lista"))
                                }.onSuccess {
                                    showFeedback("Tu lista ya tiene un enlace para compartir.", MusicFeedbackType.SUCCESS)
                                }.onFailure {
                                    showFeedback("El enlace se creó, pero no se pudo abrir el menú para compartir.", MusicFeedbackType.ERROR)
                                }
                            }.onFailure {
                                showFeedback("No hemos podido compartir esta lista. Comprueba que tienes permisos de edición.", MusicFeedbackType.ERROR)
                            }
                        }
                    }) { Text("Compartir", color = colors.accent, fontSize = 10.sp) }
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Añadir canción a lista compartida", color = colors.text, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = targetPlaylistId, onValueChange = { targetPlaylistId = it }, label = { Text("ID de la lista") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cloudTrackId, onValueChange = { cloudTrackId = it }, label = { Text("ID de canción del catálogo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        showFeedback("Añadiendo canción a la lista…", MusicFeedbackType.LOADING)
                        runCatching { MusicCloudPlaylistRepository.addTrack(targetPlaylistId.trim(), cloudTrackId.trim()) }
                            .onSuccess { message = "Canción añadida a la lista compartida."; showFeedback("Canción añadida a la lista compartida.", MusicFeedbackType.SUCCESS) }
                            .onFailure { message = "No hemos podido añadir la canción. Comprueba que tienes permiso para editar esta lista e inténtalo de nuevo."; showFeedback("No se pudo añadir la canción. Revisa los permisos.", MusicFeedbackType.ERROR) }
                    }
                }, enabled = targetPlaylistId.isNotBlank() && cloudTrackId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Añadir canción")
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Invitar colaborador", color = colors.text, fontWeight = FontWeight.Bold)
                Text("El propietario puede invitar usando el ID de usuario de Supabase.", color = colors.muted, fontSize = 11.sp)
                OutlinedTextField(value = invitePlaylistId, onValueChange = { invitePlaylistId = it }, label = { Text("ID de la lista") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inviteUserId, onValueChange = { inviteUserId = it }, label = { Text("ID de usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        showFeedback("Enviando invitación…", MusicFeedbackType.LOADING)
                        runCatching { MusicCloudPlaylistRepository.invite(invitePlaylistId.trim(), inviteUserId.trim(), "editor") }
                            .onSuccess { message = "Colaborador añadido como editor."; showFeedback("Invitación enviada correctamente.", MusicFeedbackType.SUCCESS) }
                            .onFailure { message = "No hemos podido enviar la invitación. Comprueba que eres propietario de la lista e inténtalo de nuevo."; showFeedback("No se pudo enviar la invitación.", MusicFeedbackType.ERROR) }
                    }
                }, enabled = invitePlaylistId.isNotBlank() && inviteUserId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Invitar como editor")
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    tracks: List<LocalTrackEntity>,
    colors: MusicColors,
    onPlay: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(colors.surface).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = colors.muted, fontSize = 10.sp)
            }
            Text("${tracks.size}", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (tracks.isEmpty()) {
            Text("Aún no hay canciones para esta lista.", color = colors.muted, fontSize = 11.sp)
        } else {
            tracks.take(3).forEachIndexed { index, track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable { onPlay(index) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = colors.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artistName.ifBlank { "Audio local" }, color = colors.muted, fontSize = 9.sp)
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", tint = colors.accent, modifier = Modifier.size(18.dp))
                }
            }
            TextButton(onClick = { onPlay(0) }) {
                Text("Reproducir lista", color = colors.accent, fontSize = 11.sp)
            }
        }
    }
}

private fun playSmartPlaylist(
    context: android.content.Context,
    tracks: List<LocalTrackEntity>,
    selected: Int,
    repository: MusicLibraryRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    showFeedback: (String, MusicFeedbackType) -> Unit
) {
    val playable = tracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
    if (playable.isEmpty()) {
        showFeedback("Esta lista todavía no tiene canciones reproducibles.", MusicFeedbackType.INFO)
        return
    }
    val queue = playable.map { MusicPlaybackItem(Uri.parse(it.mediaUri), it.title, it.artistName, it.albumTitle, it.artworkUri) }
    val start = selected.coerceIn(queue.indices)
    showFeedback("Preparando tu lista inteligente…", MusicFeedbackType.LOADING)
    MusicPlaybackController.playQueue(context, queue, start) { result ->
        result.onSuccess {
            showFeedback("Reproduciendo ${playable[start].title}.", MusicFeedbackType.SUCCESS)
        }.onFailure {
            showFeedback("No se pudo reproducir esta lista.", MusicFeedbackType.ERROR)
        }
    }
}

@Composable
private fun ProfileScreen(
    colors: MusicColors,
    padding: PaddingValues,
    darkMode: Boolean,
    performanceMode: String,
    onPerformanceModeChange: (String) -> Unit,
    sleepTimerMinutes: Int,
    onSleepTimerChange: (Int) -> Unit,
    onThemeChange: () -> Unit,
    showFeedback: (String, MusicFeedbackType) -> Unit
) {
    var cloudStatus by rememberSaveable { mutableStateOf("Aún no comprobada") }
    var checkingCloud by rememberSaveable { mutableStateOf(false) }
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var authMode by rememberSaveable { mutableStateOf("login") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authMessage by rememberSaveable { mutableStateOf("") }
    var authLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataSaver by MusicPreferences.dataSaver(context).collectAsState(initial = false)
    var inbox by remember { mutableStateOf<List<MusicNotificationDto>>(emptyList()) }
    var inboxStatus by rememberSaveable { mutableStateOf("Pulsa para consultar tus notificaciones.") }
    var inboxLoading by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MusicSupabase.client.auth.sessionStatus.collect { status ->
            signedIn = status is SessionStatus.Authenticated
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Perfil y ajustes", color = colors.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.surfaceAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (signedIn) "Cuenta conectada" else "Estás usando Music como invitado",
                        color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    Text(
                        if (signedIn) "Tu sesión está vinculada a la nube" else "Puedes seguir sin registrarte",
                        color = colors.muted, fontSize = 11.sp
                    )
                }
            }

            if (signedIn) {
                Text("La cuenta está activa en este dispositivo.", color = colors.muted, fontSize = 12.sp)
                Button(
                    onClick = {
                        if (!authLoading) {
                            authLoading = true
                            scope.launch {
                                val result = runCatching {
                                    withContext(Dispatchers.IO) { MusicAuthRepository.signOut() }
                                }
                                authMessage = if (result.isSuccess) "Sesión cerrada." else "No se pudo cerrar la sesión. Inténtalo de nuevo."
                                if (result.isSuccess) showFeedback("Has cerrado sesión correctamente.", MusicFeedbackType.SUCCESS)
                                else showFeedback("No se pudo cerrar la sesión. Inténtalo de nuevo.", MusicFeedbackType.ERROR)
                                authLoading = false
                            }
                        }
                    },
                    enabled = !authLoading
                ) { Text(if (authLoading) "Un momento…" else "Cerrar sesión") }
            } else {
                Text(
                    if (authMode == "login") "Inicia sesión para sincronizar tu cuenta." else "Crea una cuenta para acceder desde otros dispositivos.",
                    color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; authMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Correo electrónico") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; authMessage = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = {
                        val cleanEmail = email.trim()
                        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || password.length < 8) {
                            authMessage = "Escribe un correo válido y una contraseña de al menos 8 caracteres."
                        } else if (!authLoading) {
                            authLoading = true
                            authMessage = ""
                            showFeedback(if (authMode == "login") "Iniciando sesión…" else "Creando tu cuenta…", MusicFeedbackType.LOADING)
                            scope.launch {
                                val result = runCatching {
                                    withContext(Dispatchers.IO) {
                                        if (authMode == "login") {
                                            MusicAuthRepository.signIn(cleanEmail, password)
                                        } else {
                                            MusicAuthRepository.signUp(cleanEmail, password)
                                        }
                                    }
                                }
                                authMessage = result.fold(
                                    onSuccess = {
                                        if (authMode == "login") "Sesión iniciada correctamente."
                                        else "Registro enviado. Si se solicita, confirma tu correo antes de iniciar sesión."
                                    },
                                    onFailure = {
                                        if (authMode == "login") "No hemos podido iniciar sesión. Revisa tus datos y vuelve a intentarlo."
                                        else "No hemos podido crear tu cuenta. Comprueba los datos e inténtalo de nuevo."
                                    }
                                )
                                if (result.isSuccess) showFeedback(
                                    if (authMode == "login") "¡Sesión iniciada correctamente!" else "Registro enviado correctamente.",
                                    MusicFeedbackType.SUCCESS
                                ) else showFeedback("No se pudo completar la operación. Revisa tus datos e inténtalo de nuevo.", MusicFeedbackType.ERROR)
                                authLoading = false
                            }
                        }
                    },
                    enabled = !authLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            authLoading -> "Conectando…"
                            authMode == "login" -> "Iniciar sesión"
                            else -> "Crear cuenta"
                        }
                    )
                }
                TextButton(
                    onClick = {
                        authMode = if (authMode == "login") "signup" else "login"
                        authMessage = ""
                    }
                ) {
                    Text(
                        if (authMode == "login") "¿No tienes cuenta? Regístrate" else "Ya tengo cuenta · Iniciar sesión",
                        color = colors.accent
                    )
                }
            }
            if (authMessage.isNotBlank()) {
                Text(authMessage, color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Text(
                "El modo invitado sigue disponible y no sube tu biblioteca personal.",
                color = colors.muted, fontSize = 10.sp, lineHeight = 15.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Conexión con la nube", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Consulta el catálogo público de Music en Supabase.", color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
            Button(
                onClick = {
                    if (!checkingCloud) {
                        checkingCloud = true
                        showFeedback("Conectando con tu catálogo…", MusicFeedbackType.LOADING)
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) { MusicCatalogRepository.loadSummary() }
                            }
                            cloudStatus = result.fold(
                                onSuccess = { "Conectado · ${it.artists} artistas · ${it.albums} álbumes · ${it.tracks} canciones · ${it.videos} vídeos" },
                                onFailure = { "No hemos podido conectar ahora mismo. Tu música local sigue disponible; inténtalo de nuevo más tarde." }
                            )
                            if (result.isSuccess) showFeedback("Conexión con el catálogo establecida.", MusicFeedbackType.SUCCESS)
                            else showFeedback("No pudimos conectar con el catálogo. Inténtalo más tarde.", MusicFeedbackType.ERROR)
                            checkingCloud = false
                        }
                    }
                },
                enabled = !checkingCloud
            ) {
                Text(if (checkingCloud) "Comprobando…" else "Comprobar conexión")
            }
            Text(cloudStatus, color = colors.muted, fontSize = 11.sp)
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bandeja de notificaciones", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (!inboxLoading) {
                        inboxLoading = true
                        showFeedback("Cargando tus notificaciones…", MusicFeedbackType.LOADING)
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { MusicNotificationRepository.loadInbox() } }
                                .onSuccess { inbox = it; inboxStatus = if (it.isEmpty()) "No hay notificaciones o debes iniciar sesión." else "${it.size} notificación(es)" }
                                .onFailure { inboxStatus = "No hemos podido cargar tus notificaciones. Puedes volver a intentarlo más tarde." }
                            if (inboxStatus.startsWith("No se pudo")) showFeedback("No se pudieron cargar las notificaciones.", MusicFeedbackType.ERROR)
                            else showFeedback("Bandeja actualizada.", MusicFeedbackType.SUCCESS)
                            inboxLoading = false
                        }
                    }
                }, enabled = !inboxLoading) { Text(if (inboxLoading) "Cargando…" else "Actualizar", color = colors.accent) }
            }
            Text(inboxStatus, color = colors.muted, fontSize = 11.sp)
            inbox.take(5).forEach { notification ->
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceAlt).padding(11.dp)) {
                    Text(notification.title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(notification.body, color = colors.muted, fontSize = 11.sp)
                    Text(notification.createdAt, color = colors.muted, fontSize = 9.sp)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Temporizador de apagado", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                if (sleepTimerMinutes > 0) "La música se pausará en $sleepTimerMinutes minutos."
                else "Programa una pausa automática mientras escuchas música.",
                color = colors.muted, fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    TextButton(onClick = {
                        onSleepTimerChange(minutes)
                        MusicPlaybackController.setSleepTimer(context, minutes) { result ->
                            onSleepTimerChange(0)
                            if (result.isSuccess) showFeedback("Temporizador finalizado: reproducción pausada.", MusicFeedbackType.SUCCESS)
                            else showFeedback("Terminó el temporizador, pero no se pudo pausar la reproducción.", MusicFeedbackType.ERROR)
                        }
                        showFeedback("La música se pausará en $minutes minutos.", MusicFeedbackType.INFO)
                    }) {
                        Text("$minutes min", color = if (sleepTimerMinutes == minutes) colors.accent else colors.text, fontSize = 11.sp)
                    }
                }
            }
            if (sleepTimerMinutes > 0) {
                TextButton(onClick = {
                    MusicPlaybackController.cancelSleepTimer()
                    onSleepTimerChange(0)
                    showFeedback("Temporizador cancelado.", MusicFeedbackType.INFO)
                }) { Text("Cancelar temporizador", color = colors.accent) }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ecualizador", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(if (equalizer.available) "Ajusta las bandas del audio en reproducción." else "Inicia una canción para activar el ecualizador del dispositivo.", color = colors.muted, fontSize = 11.sp)
                }
                Switch(checked = equalizer.enabled, onCheckedChange = { MusicEqualizerController.setEnabled(it) }, enabled = equalizer.available)
            }
            if (equalizer.available) {
                if (equalizer.presets.isNotEmpty()) {
                    Text("Preajustes", color = colors.muted, fontSize = 10.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        equalizer.presets.forEachIndexed { index, preset ->
                            TextButton(onClick = { MusicEqualizerController.applyPreset(index.toShort()) }) {
                                Text(preset, color = colors.accent, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
                equalizer.bands.forEachIndexed { index, level ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val hz = equalizer.frequenciesHz.getOrNull(index) ?: 0
                            Text(if (hz >= 1000) "${hz / 1000.0} kHz" else "$hz Hz", color = colors.text, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("${level / 100} dB", color = colors.muted, fontSize = 10.sp)
                        }
                        Slider(value = level.toFloat(), onValueChange = { MusicEqualizerController.setBand(index, it.toInt()) }, valueRange = equalizer.minLevel.toFloat()..equalizer.maxLevel.toFloat(), enabled = equalizer.enabled)
                    }
                }
            }
        }
        Text("Rendimiento", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Modo de funcionamiento", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                when (performanceMode) {
                    "performance" -> "Actualización rápida del karaoke y controles más fluidos. Puede consumir más batería."
                    "battery" -> "Reduce actualizaciones visuales para prolongar la batería."
                    else -> "Equilibrio entre fluidez, consumo y respuesta."
                },
                color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("battery" to "Ahorro", "balanced" to "Equilibrado", "performance" to "Rendimiento").forEach { (mode, label) ->
                    Surface(
                        color = if (performanceMode == mode) colors.accent else colors.surfaceAlt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).clickable { onPerformanceModeChange(mode) }
                    ) {
                        Text(
                            label,
                            color = if (performanceMode == mode) colors.accentText else colors.text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 11.dp)
                        )
                    }
                }
            }
        }
        Text("Preferencias", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(colors.surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ahorro de datos", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Reduce el consumo ocultando portadas y miniaturas", color = colors.muted, fontSize = 11.sp)
            }
            Switch(
                checked = dataSaver,
                onCheckedChange = { enabled ->
                    scope.launch { MusicPreferences.setDataSaver(context, enabled) }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).clickable { onThemeChange() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = colors.accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Apariencia", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(if (darkMode) "Modo oscuro" else "Modo claro", color = colors.muted, fontSize = 11.sp)
            }
            Text("Cambiar", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = colors.muted)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Almacenamiento y caché", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Caché de portadas limitada a 128 MB", color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}
