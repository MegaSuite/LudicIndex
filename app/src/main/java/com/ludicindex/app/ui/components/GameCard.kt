package com.ludicindex.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.remote.IgdbImage
import com.ludicindex.app.domain.model.Game

/** Grid cell used by the "Popular Right Now" board — mirrors an IGDB discover page card. */
@Composable
fun GameCard(
    game: Game,
    imageQuality: ImageQuality,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Box {
            CoverImage(
                url = IgdbImage.coverUrl(game.coverImageId, imageQuality),
                contentDescription = game.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT),
            )
            RatingBadge(
                rating = game.rating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = game.genres.firstOrNull() ?: game.platforms.firstOrNull()
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** cover_big is 264x374, i.e. roughly 3:4.25. */
private const val COVER_ASPECT = 3f / 4.25f
