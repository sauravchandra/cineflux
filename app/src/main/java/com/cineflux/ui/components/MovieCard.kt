package com.cineflux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.cineflux.data.model.Movie
import com.cineflux.ui.theme.CineFluxGold

private val TitleGradient = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY
)

private val CardShape = RoundedCornerShape(8.dp)
private val RatingShape = RoundedCornerShape(4.dp)
private val RatingBg = Color.Black.copy(alpha = 0.6f)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(movie.posterUrl) {
        ImageRequest.Builder(context)
            .data(movie.posterUrl)
            .size(Size(312, 468))
            .build()
    }
    val ratingText = remember(movie.rating) {
        if (movie.rating > 0) String.format("%.1f", movie.rating) else null
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .size(width = 156.dp, height = 234.dp),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        shape = CardDefaults.shape(CardShape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardShape)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(TitleGradient)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (ratingText != null) {
                Text(
                    text = ratingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = CineFluxGold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(RatingBg, RatingShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
