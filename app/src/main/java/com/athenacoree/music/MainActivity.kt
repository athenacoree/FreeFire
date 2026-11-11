package com.athenacoree.music

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import android.Manifest
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil3.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

private enum class MusicFeedbackType { SUCCESS, ERROR, INFO, LOADING }
private data class MusicFeedback(val message: String, val type: MusicFeedbackType)

private enum class MusicTab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Default.Home),
    Library("Biblioteca", Icons.Default.LibraryMusic),
    Videos("Vídeos", Icons.Default.VideoLibrary),
    Playlists("Listas", Icons.Default.PlaylistPlay),
    Profile("Perfil", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MusicApp() }
    }
}

@Composable
private fun MusicApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkMode by MusicPreferences.darkMode(context).collectAsState(initial = false)
    val changeTheme: () -> Unit = { scope.launch { MusicPreferences.setDarkMode(context, !darkMode) }; Unit }
    val libraryRepository = remember { MusicLibraryRepository(MusicDatabase.getInstance(context).libraryDao()) }
    val playbackState by MusicPlaybackController.playbackState.collectAsState(initial = null)
    var feedback by remember { mutableStateOf<MusicFeedback?>(null) }
    val showFeedback: (String, MusicFeedbackType) -> Unit = { message, type ->
        feedback = MusicFeedback(message, type)
    }
    LaunchedEffect(feedback) {
        if (feedback != null && feedback?.type != MusicFeedbackType.LOADING) {
            delay(2800)
            feedback = null
        }
    }
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.Home.name) }
    var sleepTimerMinutes by rememberSaveable { mutableStateOf(0) }
    var lyricsLines by remember { mutableStateOf<List<MusicLyricsLine>>(emptyList()) }
    var playerVisualMode by rememberSaveable { mutableStateOf("normal") }
    val tab = MusicTab.valueOf(selectedTab)

    val colors = if (darkMode) {
        MusicColors(
            background = Color(0xFF101116),
            surface = Color(0xFF1B1D25),
            surfaceAlt = Color(0xFF252833),
            text = Color(0xFFF5F5F7),
            muted = Color(0xFFA6A9B5),
            accent = Color(0xFFB7F36B),
            accentText = Color(0xFF17200D)
        )
    } else {
        MusicColors(
            background = Color(0xFFF5F6F8),
            surface = Color.White,
            surfaceAlt = Color(0xFFE9EDF2),
            text = Color(0xFF171A20),
            muted = Color(0xFF737B88),
            accent = Color(0xFF5B8E31),
            accentText = Color.White
        )
    }

    MaterialTheme {
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
                when (tab) {
                    MusicTab.Home -> HomeScreen(colors, padding, libraryRepository) { selectedTab = MusicTab.Videos.name }
                    MusicTab.Library -> LibraryScreen(colors, padding, libraryRepository, showFeedback)
                    MusicTab.Videos -> CatalogBrowserScreen(colors, padding, libraryRepository, showFeedback)
                    MusicTab.Playlists -> PlaylistManagerScreen(colors, padding, libraryRepository, showFeedback)
                    MusicTab.Profile -> ProfileScreen(colors, padding, darkMode, sleepTimerMinutes, onSleepTimerChange = { sleepTimerMinutes = it }, onThemeChange = changeTheme, showFeedback = showFeedback)
                }
            }
            MusicMiniPlayer(playbackState, context, lyricsLines, onLyricsChange = { lyricsLines = it }, visualMode = playerVisualMode, onVisualModeChange = { playerVisualMode = it })
            MusicFeedbackBanner(feedback)
        }
    }
}

@Composable
private fun MusicMiniPlayer(state: MusicPlaybackUiState?, context: android.content.Context, lyrics: List<MusicLyricsLine>, onLyricsChange: (List<MusicLyricsLine>) -> Unit, visualMode: String, onVisualModeChange: (String) -> Unit) {
    if (state == null) return
    var positionMs by remember(state.title) { mutableStateOf(0L) }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer el archivo.") }
                .onSuccess { raw -> onLyricsChange(MusicLyricsParser.parse(raw)) }
        }
    }
    LaunchedEffect(state.title, state.isPlaying) {
        while (true) { positionMs = MusicPlaybackController.currentPosition(); delay(if (state.isPlaying) 100L else 350L) }
    }
    val activeIndex = lyrics.indexOfLast { it.timeMs <= positionMs }
    val activeLine = lyrics.getOrNull(activeIndex)
    val nextLine = lyrics.getOrNull(activeIndex + 1)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(color = Color(0xFF20232B), shape = RoundedCornerShape(20.dp), shadowElevation = 14.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 82.dp)) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(state.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.artist.ifBlank { "Reproduciendo ahora" }, color = Color(0xFFB6BAC4), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { MusicPlaybackController.previous(context) }, enabled = state.hasPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White) }
                        IconButton(onClick = { if (state.isPlaying) MusicPlaybackController.pause(context) else MusicPlaybackController.resume(context) }) { Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (state.isPlaying) "Pausar" else "Continuar", tint = Color(0xFFB7F36B)) }
                        IconButton(onClick = { MusicPlaybackController.next(context) }, enabled = state.hasNext) { Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("normal" to "Normal", "karaoke" to "Karaoke", "minimal" to "Minimal").forEach { (mode, label) ->
                        val selected = visualMode == mode
                        Surface(color = if (selected) Color(0xFFB7F36B) else Color(0xFF343944), shape = RoundedCornerShape(30.dp), modifier = Modifier.clickable { onVisualModeChange(mode) }) {
                            Text(label, color = if (selected) Color(0xFF17200D) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (visualMode == "karaoke") TextButton(onClick = { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream")) }) { Text("Abrir .LRC", color = Color(0xFFB7F36B), fontSize = 10.sp) }
                }
                when (visualMode) {
                    "karaoke" -> Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF151820)).padding(horizontal = 14.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(activeLine?.text ?: if (lyrics.isEmpty()) "Importa un archivo .LRC para ver la letra" else "La letra comenzará con la canción", color = Color(0xFFB7F36B), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 22.sp)
                        Text(nextLine?.text.orEmpty(), color = Color(0xFF9CA3AF), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (lyrics.isEmpty()) Text("Usa letras LRC que tengas guardadas en tu dispositivo.", color = Color(0xFF9CA3AF), fontSize = 9.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    "minimal" -> Text("♫  " + state.artist.ifBlank { state.title }, color = Color(0xFFB7F36B), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
@Composable
private fun MusicFeedbackBanner(feedback: MusicFeedback?) {
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
                Surface(color = Color(0xFF20232B), shape = RoundedCornerShape(18.dp), shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (current.type == MusicFeedbackType.LOADING) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(23.dp), color = Color.White, strokeWidth = 2.5.dp
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
                        Text(current.message, color = Color.White, fontSize = 13.sp,
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
private fun HomeScreen(colors: MusicColors, padding: PaddingValues, repository: MusicLibraryRepository, onExplore: () -> Unit) {
    val recentPlayback by repository.recentPlayback.collectAsState(initial = emptyList())
    val localTracks by repository.tracks.collectAsState(initial = emptyList())
    val favoriteTracks by repository.favorites.collectAsState(initial = emptyList())
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
            FeaturedCard(colors, onExplore)
        }
        item {
            SectionHeader("Tu actividad musical", "Biblioteca", colors)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCollection("Reproducciones", playbackCount.toString(), "♫", colors, Modifier.weight(1f))
                MiniCollection("Favoritos", favoriteTracks.size.toString(), "♥", colors, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(colors.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Escuchado recientemente", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                val recentTracks = recentPlayback.mapNotNull { entry -> localTracks.firstOrNull { it.id == entry.trackId } }
                    .distinctBy { it.id }
                if (recentTracks.isEmpty()) {
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
            SectionHeader("Explora tu biblioteca", "Abrir", colors)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                LibraryShortcut("Artistas", "Organiza por intérprete", Icons.Default.Person, colors)
                LibraryShortcut("Álbumes", "Discos y colecciones", Icons.Default.Album, colors)
                LibraryShortcut("Canciones", "Toda tu música", Icons.Default.MusicNote, colors)
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
                    listOf(Color(0xFF314A24), Color(0xFF6B923B), Color(0xFFB5D47B))
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
private fun SectionHeader(title: String, action: String, colors: MusicColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(action, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
private fun LibraryShortcut(title: String, subtitle: String, icon: ImageVector, colors: MusicColors) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(14.dp),
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
    val tracks by repository.tracks.collectAsState(initial = emptyList())
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val playbackCount by repository.playbackCount.collectAsState(initial = 0)
    val mostPlayed by repository.mostPlayed.collectAsState(initial = emptyList())
    val recentPlayback by repository.recentPlayback.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scanStatus by rememberSaveable { mutableStateOf("Primero se importan 5 archivos; luego puedes continuar por tandas.") }
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
                        "Se añadieron ${result.imported} archivos. " + if (result.hasMore) "Puedes importar los siguientes cinco." else "Exploración completada."
                }.onFailure { scanStatus = "No se pudo explorar: ${it.message ?: "error"}"; showFeedback("No se pudo explorar la biblioteca. Revisa los permisos e inténtalo de nuevo.", MusicFeedbackType.ERROR) }
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
            var imported = 0
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val displayName = context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "Archivo de audio"
                    repository.addTrack(LocalTrackEntity(
                        id = uri.toString(),
                        title = displayName.substringBeforeLast('.', displayName),
                        mediaUri = uri.toString(),
                        source = "device"
                    ))
                }.onSuccess { imported++ }
            }
            if (imported > 0) showFeedback("$imported archivo(s) añadido(s) a tu biblioteca.", MusicFeedbackType.SUCCESS)
            else showFeedback("No se pudieron añadir los archivos seleccionados.", MusicFeedbackType.ERROR)
        }
    }

    val baseTracks = if (filter == "Favoritos") favorites else tracks
    val visibleTracks = baseTracks.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true)
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
                Text("Music lee canciones y vídeos en tandas de cinco, para no cargar toda la biblioteca de golpe.", color = colors.muted, fontSize = 12.sp)
                Text(scanStatus, color = colors.muted, fontSize = 11.sp)
                Button(onClick = { scanLauncher.launch(scanPermissions) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Leer los siguientes 5 archivos")
                }
            }
        }
        if (visibleTracks.any { it.mediaUri != null && it.source != "device-video" }) {
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
        if (visibleTracks.isEmpty()) {
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
                    "${visibleTracks.size} elemento(s)",
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
            items(visibleTracks, key = { it.id }) { track ->
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
                                    val audioQueue = visibleTracks.filter { it.mediaUri != null && it.source != "device-video" }
                                        .map { Uri.parse(it.mediaUri) to it.title }
                                    val selectedIndex = audioQueue.indexOfFirst { it.first == Uri.parse(media) }.coerceAtLeast(0)
                                    MusicPlaybackController.playQueue(context, audioQueue, selectedIndex) { result ->
                                        result.onSuccess {
                                            scope.launch {
                                                runCatching { repository.recordPlayback(track) }
                                            }
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
private fun CatalogBrowserScreen(
    colors: MusicColors,
    padding: PaddingValues,
    repository: MusicLibraryRepository,
    showFeedback: (String, MusicFeedbackType) -> Unit
) {
    var kind by rememberSaveable { mutableStateOf("video") }
    var syncStatus by rememberSaveable { mutableStateOf("Catálogo guardado en este dispositivo") }
    var syncing by rememberSaveable { mutableStateOf(false) }
    val entries by repository.catalog(kind).collectAsState(initial = emptyList())
    val localMedia by repository.tracks.collectAsState(initial = emptyList())
    var sourceChoice by remember { mutableStateOf<Pair<CatalogEntryEntity, LocalTrackEntity>?>(null) }
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
                    modifier = Modifier.clickable { kind = value }
                ) {
                    Text(label, color = if (kind == value) colors.accentText else colors.muted,
                        fontSize = 12.sp, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
                }
            }
        }
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
                            { "Error al sincronizar: ${it.message?.take(180) ?: "error desconocido"}. Se conserva el catálogo descargado anteriormente." }
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
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = colors.accent, modifier = Modifier.size(42.dp))
                Text("Todavía no hay contenido", color = colors.text, fontWeight = FontWeight.SemiBold)
                Text("Actualiza el catálogo cuando tengas conexión. Lo sincronizado quedará disponible sin conexión.", color = colors.muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(entries, key = { "${kind}-${it.id}" }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                            .background(colors.surface).clickable {
                                if (kind == "video" || kind == "track") {
                                    val expectedSource = if (kind == "video") "device-video" else "device-audio"
                                    val normalized = normalizeMusicTitle(entry.title)
                                    val match = localMedia.firstOrNull { local ->
                                        val localTitle = normalizeMusicTitle(local.title)
                                        (local.source == expectedSource || (kind == "track" && local.source == "device")) && localTitle.isNotBlank() &&
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
                            AsyncImage(
                                model = if (entry.imagePath.startsWith("http")) entry.imagePath
                                    else "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/music-artwork/${entry.imagePath.trimStart('/')}",
                                contentDescription = entry.title,
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))
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
                        if (kind == "track") {
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
                        } else if (kind == "video") {
                            TextButton(onClick = {
                                val link = if (entry.provider.equals("youtube", true) && !entry.providerId.isNullOrBlank())
                                    "https://www.youtube.com/watch?v=${entry.providerId}" else entry.mediaPath
                                if (!link.isNullOrBlank()) runCatching {
                                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, link)
                                    }.let { Intent.createChooser(it, "Compartir vídeo") })
                                }
                            }) { Text("Compartir", color = colors.accent, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
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
                    } else offline.mediaUri?.let { MusicPlaybackController.play(context, Uri.parse(it), offline.title) }
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
    val localLists by repository.playlists.collectAsState(initial = emptyList())
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
                                .onFailure { message = it.message ?: "Inicia sesión para crear listas compartidas."; showFeedback("No se pudo crear la lista compartida. Comprueba tu sesión.", MusicFeedbackType.ERROR) }
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
                val recentTracks = recentPlayback.mapNotNull { entry -> localTracks.firstOrNull { it.id == entry.trackId } }
                    .distinctBy { it.id }
                val topTracks = mostPlayed.mapNotNull { stat -> localTracks.firstOrNull { it.id == stat.trackId } }
                    .distinctBy { it.id }
                val favoritePlayable = favoriteTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
                val recentPlayable = recentTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
                val topPlayable = topTracks.filter { !it.mediaUri.isNullOrBlank() && it.source != "device-video" }
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
        if (localLists.isNotEmpty() && localTracks.isNotEmpty()) {
            item { Text("Añadir canciones a «${localLists.first().name}»", color = colors.muted, fontSize = 12.sp) }
            items(localTracks, key = { "local-track-${it.id}" }) { track ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(colors.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(track.artistName.ifBlank { "Audio local" }, color = colors.muted, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { repository.addToPlaylist(localLists.first().id, track.id) }
                                .onSuccess { message = "Canción añadida a tu lista local."; showFeedback("Canción añadida a la lista.", MusicFeedbackType.SUCCESS) }
                                .onFailure { showFeedback("No se pudo añadir la canción a la lista.", MusicFeedbackType.ERROR) }
                        }
                    }) { Text("Añadir", color = colors.accent) }
                }
            }
        }
        if (localLists.isEmpty()) item { Text("Aún no tienes listas locales.", color = colors.muted, fontSize = 12.sp) }
        items(localLists, key = { it.id }) { list ->
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
                TextButton(onClick = { targetPlaylistId = list.id; invitePlaylistId = list.id }) {
                    Text("Usar esta lista", color = colors.accent, fontSize = 10.sp)
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
                            .onFailure { message = it.message ?: "No se pudo añadir. Debes ser propietario o editor."; showFeedback("No se pudo añadir la canción. Revisa los permisos.", MusicFeedbackType.ERROR) }
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
                            .onFailure { message = it.message ?: "No se pudo invitar. Comprueba que seas propietario."; showFeedback("No se pudo enviar la invitación.", MusicFeedbackType.ERROR) }
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
    val queue = playable.map { Uri.parse(it.mediaUri) to it.title }
    val start = selected.coerceIn(queue.indices)
    showFeedback("Preparando tu lista inteligente…", MusicFeedbackType.LOADING)
    MusicPlaybackController.playQueue(context, queue, start) { result ->
        result.onSuccess {
            scope.launch { runCatching { repository.recordPlayback(playable[start]) } }
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
                                        val detail = it.message?.take(180) ?: "Error desconocido"
                                        if (authMode == "login") "No se pudo iniciar sesión: $detail"
                                        else "No se pudo crear la cuenta: $detail"
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
                                onFailure = { "No se pudo conectar: ${it.message?.take(180) ?: "error desconocido"}" }
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
                                .onFailure { inboxStatus = "No se pudo cargar la bandeja: ${it.message?.take(160) ?: "error desconocido"}" }
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
                Text("Caché de portadas limitada a 48 MB", color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}
