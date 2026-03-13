package com.cineflux.ui.player

import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cineflux.ui.theme.CineFluxRed
import kotlinx.coroutines.delay
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var useExoPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(state.filePath, state.subtitlePath) {
        val filePath = state.filePath ?: return@LaunchedEffect
        if (state.subtitleStatus == "Searching subtitles...") return@LaunchedEffect

        val file = File(filePath)
        if (!file.exists()) return@LaunchedEffect

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
        val videoUri = Uri.fromFile(file)

        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("org.videolan.vlc")
            setDataAndTypeAndNormalize(videoUri, "video/*")
            putExtra("title", state.title)
            state.subtitlePath?.let { srtPath ->
                putExtra("subtitles_location", Uri.fromFile(File(srtPath)).toString())
            }
            putExtra("from_start", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            android.util.Log.i("PlayerScreen", "Trying VLC: $filePath")
            context.startActivity(vlcIntent)
        } catch (e: Exception) {
            android.util.Log.w("PlayerScreen", "VLC failed: ${e.message}, using ExoPlayer")
            useExoPlayer = true
        }
    }

    if (useExoPlayer && state.filePath != null) {
        ExoPlayerScreen(
            filePath = state.filePath!!,
            subtitlePath = state.subtitlePath,
            title = state.title,
            onBack = onBack
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.error != null) {
                    Text(state.error ?: "", color = CineFluxRed, style = MaterialTheme.typography.titleMedium)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = CineFluxRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (state.filePath == null) "Loading..." else state.subtitleStatus,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ExoPlayerScreen(
    filePath: String,
    subtitlePath: String?,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(filePath) {
        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.fromFile(File(filePath)))
        subtitlePath?.let { srtPath ->
            val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(File(srtPath)))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
        }
        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
    }

    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })
    }

    LaunchedEffect(isPlaying) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(500)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) { delay(5000); showControls = false }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(focusRequester).focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                showControls = true
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play(); true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        exoPlayer.seekTo(exoPlayer.currentPosition + 10_000); true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)); true
                    }
                    KeyEvent.KEYCODE_BACK -> { onBack(); true }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
            modifier = Modifier.fillMaxSize()
        )
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 48.dp, vertical = 24.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (duration > 0) currentPosition.toFloat() / duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = CineFluxRed, trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(formatTime(currentPosition), style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FastRewind, "Rewind", tint = Color.White, modifier = Modifier.size(32.dp))
                        Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), Alignment.Center) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (isPlaying) "Pause" else "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Icon(Icons.Default.FastForward, "Forward", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text(formatTime(duration), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}
