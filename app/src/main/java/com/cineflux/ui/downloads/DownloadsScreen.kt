package com.cineflux.ui.downloads

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.cineflux.ui.components.ActionIconButton
import coil3.compose.AsyncImage
import com.cineflux.ui.theme.CineFluxGold
import com.cineflux.ui.theme.CineFluxRed

@Composable
fun DownloadsScreen(
    onPlayClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Downloads",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No downloads yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                items(downloads, key = { it.entity.id }) { item ->
                    DownloadRow(
                        item = item,
                        onPlay = { onPlayClick(item.entity.id) },
                        onPause = { viewModel.pause(item.entity.infoHash) },
                        onResume = { viewModel.resume(item.entity.infoHash) },
                        onRemove = { viewModel.remove(item.entity.infoHash) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.entity.posterUrl,
            contentDescription = item.entity.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = item.entity.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.entity.quality,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!item.isCompleted) {
                val progress = item.displayProgress.coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                ) {
                    val barHeight = size.height
                    val radius = barHeight / 2
                    drawRoundRect(
                        color = androidx.compose.ui.graphics.Color(0xFF2A2A2C),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                    )
                    if (progress > 0f) {
                        drawRoundRect(
                            color = androidx.compose.ui.graphics.Color(0xFFE21A22),
                            size = size.copy(width = size.width * progress),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}% · ${item.speedText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CineFluxGold
                )
            } else {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = CineFluxGold
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (item.isCompleted) {
                ActionIconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            } else {
                var lastAction by remember { mutableLongStateOf(0L) }
                val showPause = !item.isPaused

                ActionIconButton(onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastAction < 1000) return@ActionIconButton
                    lastAction = now
                    if (showPause) onPause() else onResume()
                }) {
                    Icon(
                        if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (showPause) "Pause" else "Resume"
                    )
                }
            }
            ActionIconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
