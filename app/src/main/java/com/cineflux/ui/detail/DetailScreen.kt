package com.cineflux.ui.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.platform.LocalContext
import com.cineflux.ui.components.ActionIconButton
import coil3.compose.AsyncImage
import com.cineflux.data.model.TorrentInfo
import com.cineflux.ui.components.LoadingIndicator
import com.cineflux.ui.theme.CineFluxGold
import com.cineflux.ui.theme.CineFluxRed

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onPlayClick: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.isLoading) {
        LoadingIndicator()
        return
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val movie = state.movie ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = movie.backdropUrl ?: movie.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xEE0E0E0E), Color(0x660E0E0E)),
                            startX = 0f,
                            endX = size.width
                        )
                    )
                }
        )

        ActionIconButton(
            onClick = onBack,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 80.dp, bottom = 40.dp)
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(220.dp)
                    .height(330.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 32.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.tagline.isNotBlank()) {
                    Text(
                        text = movie.tagline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MetadataChip(Icons.Default.Star, String.format("%.1f", movie.rating), CineFluxGold)
                    if (movie.releaseDate.length >= 4) {
                        MetadataChip(Icons.Default.CalendarMonth, movie.releaseDate.take(4))
                    }
                    movie.runtime?.let {
                        MetadataChip(Icons.Default.Timer, "${it}m")
                    }
                }

                if (movie.genres.isNotEmpty()) {
                    Text(
                        text = movie.genres.joinToString(" / "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state.torrentsLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            color = CineFluxRed,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Finding best sources...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else if (movie.torrents.isNotEmpty()) {
                    TorrentQualityPicker(
                        torrents = movie.torrents,
                        selected = state.selectedTorrent,
                        onSelect = viewModel::selectTorrent
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val sel = state.selectedTorrent
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.material3.Button(
                            onClick = {
                                Log.d("CineFlux", "Download & Play clicked!")
                                Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show()
                                viewModel.startBuiltInDownload()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = CineFluxRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sel != null) "Download ${sel.quality} (${sel.size})" else "Download",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val magnet = viewModel.getMagnetUrl()
                                if (magnet != null) {
                                    Log.d("CineFlux", "Sending to Transmission")
                                    Toast.makeText(context, "Opening in Transmission...", Toast.LENGTH_SHORT).show()
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
                                        intent.setPackage("com.ap.transmission.btc")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet))
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send to Transmission",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No torrent sources found for this movie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(icon: ImageVector, text: String, tint: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.height(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun TorrentQualityPicker(
    torrents: List<TorrentInfo>,
    selected: TorrentInfo?,
    onSelect: (TorrentInfo) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        torrents.forEach { torrent ->
            val isSelected = torrent.hash == selected?.hash
            androidx.compose.material3.OutlinedButton(
                onClick = { onSelect(torrent) },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) CineFluxRed else Color.White.copy(alpha = 0.3f)
                ),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) CineFluxRed.copy(alpha = 0.15f) else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        torrent.quality,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) CineFluxRed else Color.White,
                        maxLines = 1
                    )
                    Text(
                        torrent.size,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
