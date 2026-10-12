package com.ludicindex.app.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.remote.IgdbImage
import com.ludicindex.app.domain.WishlistSort
import com.ludicindex.app.domain.model.WishlistEntry
import com.ludicindex.app.ui.LocalAppContainer
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.CoverImage
import com.ludicindex.app.ui.components.EmptyView
import com.ludicindex.app.ui.components.LoadingView
import com.ludicindex.app.ui.components.MetaChip
import com.ludicindex.app.ui.format.formatReleaseYear

@Composable
fun WishlistScreen(
    onGameClick: (Long) -> Unit,
    onBrowseHome: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: WishlistViewModel = appViewModel { container ->
        WishlistViewModel(container.wishlistRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageQuality by LocalAppContainer.current.settingsStore.imageQuality
        .collectAsStateWithLifecycle(initialValue = ImageQuality.STANDARD)

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        WishlistHeader(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onToggleTag = viewModel::toggleTagFilter,
            onClearFilters = viewModel::clearFilters,
            onSort = viewModel::setSort,
        )

        when {
            state.isLoading -> LoadingView()

            !state.hasAnyEntry -> WishlistEmptyState(
                onBrowseHome = onBrowseHome,
                onImport = onImport,
            )

            state.visibleEntries.isEmpty() -> EmptyView(
                title = "没有符合条件的条目",
                subtitle = "换个关键词，或点「全部」清除标签筛选",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.visibleEntries, key = { it.igdbId }) { entry ->
                    SwipeToDeleteEntry(
                        entry = entry,
                        imageQuality = imageQuality,
                        onClick = { onGameClick(entry.igdbId) },
                        onEdit = { viewModel.openEditor(entry.igdbId) },
                        onDelete = { viewModel.remove(entry.igdbId) },
                    )
                }
            }
        }
    }

    state.editing?.let { entry ->
        TagEditorSheet(
            entry = entry,
            allTags = state.allTags,
            onDismiss = viewModel::closeEditor,
            onStatusChange = { viewModel.setStatus(entry.igdbId, it) },
            onTagsChange = { viewModel.setTags(entry.igdbId, it) },
            onCreateTag = viewModel::createTag,
            onDeleteTag = viewModel::deleteTag,
        )
    }
}

@Composable
private fun WishlistHeader(
    state: WishlistUiState,
    onQueryChange: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onSort: (WishlistSort) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("在心愿单中搜索…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
            SortMenu(sort = state.sort, onSort = onSort)
        }

        if (state.allTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.selectedTagIds.isEmpty(),
                        onClick = onClearFilters,
                        label = { Text("全部") },
                    )
                }
                items(state.allTags, key = { it.id }) { tag ->
                    FilterChip(
                        selected = tag.id in state.selectedTagIds,
                        onClick = { onToggleTag(tag.id) },
                        label = { Text(tag.name) },
                        leadingIcon = { ColorDot(tag.colorArgb) },
                    )
                }
            }
        }

        if (state.hasAnyEntry) {
            Text(
                text = "共 ${state.totalCount} 个条目" +
                    if (state.visibleEntries.size != state.totalCount) {
                        "，筛选出 ${state.visibleEntries.size} 个"
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun SortMenu(sort: WishlistSort, onSort: (WishlistSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(sort.displayLabel)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "排序方式")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WishlistSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayLabel) },
                    onClick = {
                        onSort(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteEntry(
    entry: WishlistEntry,
    imageQuality: ImageQuality,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        WishlistRow(
            entry = entry,
            imageQuality = imageQuality,
            onClick = onClick,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun WishlistRow(
    entry: WishlistEntry,
    imageQuality: ImageQuality,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        CoverImage(
            url = IgdbImage.coverUrl(entry.coverImageId, imageQuality),
            contentDescription = entry.name,
            modifier = Modifier
                .size(width = 56.dp, height = 74.dp)
                .clip(RoundedCornerShape(6.dp)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    formatReleaseYear(entry.releaseDate),
                    entry.status.displayLabel,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (entry.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    entry.tags.take(3).forEach { tag -> MetaChip(text = tag.name) }
                    if (entry.tags.size > 3) {
                        MetaChip(text = "+${entry.tags.size - 3}")
                    }
                }
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "编辑标签")
        }
    }
}

@Composable
private fun WishlistEmptyState(
    onBrowseHome: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = "心愿单还是空的",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "去首页看看热门游戏，或者粘贴一堆标题批量导入。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )
        Button(onClick = onBrowseHome) { Text("去首页逛逛") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onImport,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("批量导入") }
    }
}

@Composable
internal fun ColorDot(colorArgb: Int) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(colorArgb)),
    )
}
