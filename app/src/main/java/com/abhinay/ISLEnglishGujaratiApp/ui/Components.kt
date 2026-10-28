package com.abhinay.ISLEnglishGujaratiApp.ui

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import com.abhinay.ISLEnglishGujaratiApp.data.Sign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** True while the bottom bar is on screen; it already consumes the bottom (and keyboard) inset. */
val LocalBarShown = compositionLocalOf { false }

@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val inTabs = LocalBarShown.current
    val insets = if (inTabs) WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal) else WindowInsets.safeDrawing
    Scaffold(
        topBar = { ScreenTopBar(title, onBack, actions) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = insets,
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(top = 8.dp))

@Composable
fun StateMessage(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

/** English line plus (optionally) Gujarati with transliteration. */
@Composable
fun SignLabels(sign: Sign, showGujarati: Boolean, big: Boolean = false) {
    Column {
        Text(sign.english, style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
        if (showGujarati) {
            Text(sign.gujarati, style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
            if (sign.romanized.isNotBlank()) Text(sign.romanized, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SignRow(sign: Sign, showGujarati: Boolean, mastered: Boolean, favorite: Boolean? = null, onFavorite: () -> Unit = {}, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sign.english, style = MaterialTheme.typography.titleMedium)
                if (showGujarati) Text(sign.gujarati, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mastered) Icon(Icons.Default.Check, contentDescription = "Learned", tint = MaterialTheme.colorScheme.primary)
            if (favorite != null) IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (favorite) "Remove from board" else "Add to board")
            }
        }
    }
}

/** The sign's picture or looping silent video. Tap the video to pause or resume. */
@Composable
fun SignMedia(sign: Sign, useVideo: Boolean, modifier: Modifier = Modifier, speed: Float = 1f) {
    val desc = "Sign for ${sign.english}"
    val box = modifier.clip(RoundedCornerShape(16.dp))
    if (useVideo && sign.video != null) {
        val context = LocalContext.current
        var playing by remember(sign.id) { mutableStateOf(true) }
        var player by remember(sign.id) { mutableStateOf<MediaPlayer?>(null) }
        // Applying a speed resumes a paused player, so only do it while playing.
        LaunchedEffect(speed, player, playing) {
            if (playing) player?.let { runCatching { it.playbackParams = PlaybackParams().setSpeed(speed) } }
        }
        Box(box.semantics { contentDescription = "$desc, video. Tap to pause or play." }) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    VideoView(it).apply {
                        setVideoURI(Uri.parse("android.resource://${context.packageName}/${sign.video}"))
                        setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); player = mp }
                        setOnClickListener { if (isPlaying) { pause(); playing = false } else { start(); playing = true } }
                        start()
                    }
                },
            )
            if (!playing) Text("Paused. Tap to play.", Modifier.align(Alignment.BottomCenter).padding(8.dp), color = MaterialTheme.colorScheme.onSurface)
        }
    } else if (sign.image != null) {
        Image(painterResource(sign.image), desc, box, contentScale = ContentScale.Fit)
    } else {
        StateMessage("Media unavailable", "This sign has no picture or video.", box)
    }
}

@Composable
fun ProgressLine(done: Int, total: Int, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else done.toFloat() / total },
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "$done of $total learned" },
    )
}

@Composable
fun Tappable(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

operator fun PaddingValues.plus(o: PaddingValues): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection) =
        this@plus.calculateLeftPadding(layoutDirection) + o.calculateLeftPadding(layoutDirection)
    override fun calculateRightPadding(layoutDirection: androidx.compose.ui.unit.LayoutDirection) =
        this@plus.calculateRightPadding(layoutDirection) + o.calculateRightPadding(layoutDirection)
    override fun calculateTopPadding() = this@plus.calculateTopPadding() + o.calculateTopPadding()
    override fun calculateBottomPadding() = this@plus.calculateBottomPadding() + o.calculateBottomPadding()
}
