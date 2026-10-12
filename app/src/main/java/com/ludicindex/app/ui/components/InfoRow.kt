package com.ludicindex.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** "开发商" + values, used by the detail page's fact sheet. */
@Composable
fun InfoRow(
    label: String,
    values: List<String>,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = values.joinToString("、"),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
