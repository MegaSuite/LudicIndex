package com.ludicindex.app.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ludicindex.app.domain.model.Tag
import com.ludicindex.app.domain.model.WishlistEntry
import com.ludicindex.app.ui.components.SectionTitle

/**
 * Edits one wishlist entry: its tags and custom tags with a color.
 *
 * Tag selections are staged locally and written once when the user saves. The content scrolls so
 * the action row cannot be pushed off screen when the tag list or the palette grows.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditorSheet(
    entry: WishlistEntry,
    allTags: List<Tag>,
    platformOptions: List<String>,
    onDismiss: () -> Unit,
    onPlatformChange: (String?) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onCreateTag: (String, Int) -> Unit,
    onDeleteTag: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Keyed on the entry only, so background DB updates do not wipe the user's in-progress edits.
    var selectedNames by remember(entry.igdbId) {
        mutableStateOf(entry.tags.map { it.name }.toSet())
    }
    var manageMode by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(TAG_PALETTE.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
            )

            SectionTitle("游戏平台")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = entry.platform == null,
                    onClick = { onPlatformChange(null) },
                    label = { Text("不指定") },
                )
                platformOptions.forEach { option ->
                    FilterChip(
                        selected = entry.platform == option,
                        onClick = { onPlatformChange(option) },
                        label = { Text(option) },
                    )
                }
            }
            if (platformOptions.isEmpty()) {
                Text(
                    text = "暂无商店选项：打开一次该游戏的详情后即可选择。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // "编辑" sits opposite the "标签" heading; it swaps every chip for a deletable one.
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("标签", modifier = Modifier.weight(1f))
                TextButton(onClick = { manageMode = !manageMode }) {
                    Text(if (manageMode) "完成" else "编辑")
                }
            }

            if (allTags.isEmpty()) {
                Text(
                    text = "还没有任何标签，先在下面创建一个。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    allTags.forEach { tag ->
                        if (manageMode) {
                            DeletableTagChip(
                                name = tag.name,
                                colorArgb = tag.colorArgb,
                                onDelete = {
                                    selectedNames = selectedNames - tag.name
                                    onDeleteTag(tag.id)
                                },
                            )
                        } else {
                            FilterChip(
                                selected = tag.name in selectedNames,
                                onClick = {
                                    selectedNames = if (tag.name in selectedNames) {
                                        selectedNames - tag.name
                                    } else {
                                        selectedNames + tag.name
                                    }
                                },
                                label = { Text(tag.name) },
                                leadingIcon = { ColorDot(tag.colorArgb) },
                            )
                        }
                    }
                }
                if (manageMode) {
                    Text(
                        text = "点标签后面的 × 删除该标签（条目本身不受影响）。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            SectionTitle("新建自定义标签")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("标签名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val name = newTagName.trim()
                        if (name.isNotEmpty()) {
                            onCreateTag(name, newTagColor)
                            selectedNames = selectedNames + name
                            newTagName = ""
                        }
                    },
                    enabled = newTagName.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("添加") }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                TAG_PALETTE.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .then(
                                if (color == newTagColor) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { newTagColor = color },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Button(
                    onClick = {
                        onTagsChange(selectedNames.toList())
                        onDismiss()
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text("保存") }
            }
        }
    }
}

/** A tag rendered as a chip whose trailing "×" is the only interactive part. */
@Composable
private fun DeletableTagChip(
    name: String,
    colorArgb: Int,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 2.dp),
        ) {
            ColorDot(colorArgb)
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp),
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "删除标签 $name",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
