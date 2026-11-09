package com.athenacoree.music

import android.os.Bundle
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import coil.compose.AsyncImage
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.gotrue.SessionStatus

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
    var selectedTab by rememberSaveable { mutableStateOf(MusicTab.Home.name) }
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
                    MusicTab.Home -> HomeScreen(colors, padding) { selectedTab = MusicTab.Videos.name }
                    MusicTab.Library -> LibraryScreen(colors, padding, libraryRepository)
                    MusicTab.Videos -> CatalogBrowserScreen(colors, padding, libraryRepository)
                    MusicTab.Playlists -> PlaylistManagerScreen(colors, padding, libraryRepository)
                    MusicTab.Profile -> ProfileScreen(colors, padding, darkMode, onThemeChange = changeTheme)
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
private fun HomeScreen(colors: MusicColors, padding: PaddingValues, onExplore: () -> Unit) {
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
            SectionHeader("Escuchado recientemente", "Ver todo", colors)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCollection("Tus favoritos", "Tu colección personal", "♫", colors, Modifier.weight(1f))
                MiniCollection("Descubrir", "Encuentra algo nuevo", "✦", colors, Modifier.weight(1f))
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
            Text("Dale espacio\\na lo que te mueve.", color = Color.White, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
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
    repository: MusicLibraryRepository
) {
    var filter by rememberSaveable { mutableStateOf("Todo") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filters = listOf("Todo", "Favoritos")
    val tracks by repository.tracks.collectAsState(initial = emptyList())
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scanStatus by rememberSaveable { mutableStateOf("Primero se importan 5 archivos; luego puedes continuar por tandas.") }
    val scanPermissions = if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it }) scope.launch {
            runCatching { MusicDeviceScanner.scanNextMediaBatch(context, repository) }
                .onSuccess { result ->
                    scanStatus = if (result.imported == 0) "No quedan más archivos multimedia." else
                        "Se añadieron ${result.imported} archivos. " + if (result.hasMore) "Puedes importar los siguientes cinco." else "Exploración completada."
                }
                .onFailure { scanStatus = "No se pudo explorar: ${it.message ?: "error"}" }
        } else scanStatus = "Permiso para leer audio y vídeo no concedido."
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            scope.launch {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val displayName = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "Archivo de audio"
                repository.addTrack(
                    LocalTrackEntity(
                        id = uri.toString(),
                        title = displayName.substringBeforeLast('.', displayName),
                        mediaUri = uri.toString(),
                        source = "device"
                    )
                )
            }
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
                    TextButton(onClick = { MusicPlaybackController.previous(context) }) { Text("Anterior", color = colors.accent, fontSize = 11.sp) }
                    TextButton(onClick = { MusicPlaybackController.pause(context) }) { Text("Pausa", color = colors.text, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { MusicPlaybackController.resume(context) }) { Text("Continuar", color = colors.accent, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { MusicPlaybackController.next(context) }) { Text("Siguiente", color = colors.accent, fontSize = 11.sp) }
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
                            track.mediaUri?.let { media ->
                                if (track.source == "device-video") {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(media), "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        })
                                    }
                                } else {
                                    val audioQueue = visibleTracks.filter { it.mediaUri != null && it.source != "device-video" }
                                        .map { Uri.parse(it.mediaUri) to it.title }
                                    val selectedIndex = audioQueue.indexOfFirst { it.first == Uri.parse(media) }.coerceAtLeast(0)
                                    MusicPlaybackController.playQueue(context, audioQueue, selectedIndex)
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
                        scope.launch { repository.setFavorite(track.id, !track.isFavorite) }
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
                    TextButton(onClick = {
                        scope.launch { repository.removeTrack(track.id) }
                    }) { Text("Quitar", color = colors.muted, fontSize = 10.sp) }
                }
            }
        }
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
    repository: MusicLibraryRepository
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
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                MusicCatalogSyncRepository(repository.databaseDao).syncAll()
                            }
                        }
                        syncStatus = result.fold(
                            { "Actualizado: ${it.artists} artistas · ${it.albums} álbumes · ${it.tracks} canciones · ${it.videos} vídeos" },
                            { "Sin conexión. Se conserva el catálogo descargado anteriormente." }
                        )
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
                                    repository.addTrack(LocalTrackEntity(
                                        id = "catalog:${entry.id}", title = entry.title,
                                        artistName = entry.subtitle, artworkUri = entry.imagePath,
                                        source = "catalog"
                                    ))
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
    repository: MusicLibraryRepository
) {
    val localLists by repository.playlists.collectAsState(initial = emptyList())
    val localTracks by repository.tracks.collectAsState(initial = emptyList())
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
                            .onSuccess { name = ""; description = ""; message = "Lista guardada en este dispositivo." }
                            .onFailure { message = "No se pudo crear la lista." }
                    }
                }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Crear lista local") }
                Button(onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            runCatching { MusicCloudPlaylistRepository.create(name, description, true) }
                                .onSuccess { name = ""; description = ""; message = "Lista colaborativa creada en la nube." }
                                .onFailure { message = it.message ?: "Inicia sesión para crear listas compartidas." }
                            busy = false
                        }
                    }
                }, enabled = name.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Creando…" else "Crear lista colaborativa")
                }
            }
        }
        item { Text(message, color = colors.muted, fontSize = 11.sp) }
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
                            repository.addToPlaylist(localLists.first().id, track.id)
                            message = "Canción añadida a tu lista local."
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
                        runCatching { MusicCloudPlaylistRepository.loadMyPlaylists() }
                            .onSuccess { cloudLists = it; message = "Listas de la nube actualizadas." }
                            .onFailure { message = "Inicia sesión para consultar las listas compartidas." }
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
                        runCatching { MusicCloudPlaylistRepository.addTrack(targetPlaylistId.trim(), cloudTrackId.trim()) }
                            .onSuccess { message = "Canción añadida a la lista compartida." }
                            .onFailure { message = it.message ?: "No se pudo añadir. Debes ser propietario o editor." }
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
                        runCatching { MusicCloudPlaylistRepository.invite(invitePlaylistId.trim(), inviteUserId.trim(), "editor") }
                            .onSuccess { message = "Colaborador añadido como editor." }
                            .onFailure { message = it.message ?: "No se pudo invitar. Comprueba que seas propietario." }
                    }
                }, enabled = invitePlaylistId.isNotBlank() && inviteUserId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Invitar como editor")
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    colors: MusicColors,
    padding: PaddingValues,
    darkMode: Boolean,
    onThemeChange: () -> Unit
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
        MusicSupabase.client.gotrue.sessionStatus.collect { status ->
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
                                        if (authMode == "login") "No se pudo iniciar sesión. Revisa tus datos e inténtalo de nuevo."
                                        else "No se pudo crear la cuenta. Comprueba el correo e inténtalo de nuevo."
                                    }
                                )
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
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) { MusicCatalogRepository.loadSummary() }
                            }
                            cloudStatus = result.fold(
                                onSuccess = { "Conectado · ${it.artists} artistas · ${it.albums} álbumes · ${it.tracks} canciones · ${it.videos} vídeos" },
                                onFailure = { "No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo." }
                            )
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
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { MusicNotificationRepository.loadInbox() } }
                                .onSuccess { inbox = it; inboxStatus = if (it.isEmpty()) "No hay notificaciones o debes iniciar sesión." else "${it.size} notificación(es)" }
                                .onFailure { inboxStatus = "No se pudo cargar la bandeja." }
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
