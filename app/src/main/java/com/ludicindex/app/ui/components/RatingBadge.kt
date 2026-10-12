package com.ludicindex.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ludicindex.app.ui.format.formatRating

@Composable
fun RatingBadge(
    rating: Double?,
    modifier: Modifier = Modifier,
) {
    val label = formatRating(rating) ?: return
    Surface(
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
