package com.ludicindex.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ludicindex.app.domain.model.LibraryGame
import com.ludicindex.app.ui.components.CoverImage
import com.ludicindex.app.ui.components.ErrorView
import com.ludicindex.app.ui.components.LoadingView
import com.ludicindex.app.ui.format.formatDateTime

/** Grid of subscription-catalog games with in-page search and a device filter. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryCatalogContent(
    title: String,
    games: List<LibraryGame>,
    totalCount: Int,
    devices: List<String>,
    filter: LibraryFilter,
    isLoading: Boolean,
    isRefreshing: Boolean,
    lastSyncedAtMillis: Long?,
    onQueryChange: (String) -> Unit,
    onToggleDevice: (String) -> Unit,
    onClearDevices: () -> Unit,
    onClearFilter: () -> Unit,
    onRefresh: () -> Unit,
    onGameTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryToolbar(
            title = title,
            shownCount = games.size,
            totalCount = totalCount,
            lastSyncedAtMillis = lastSyncedAtMillis,
            filter = filter,
            devices = devices,
            searchExpanded = searchExpanded,
            onToggleSearch = {
                searchExpanded = !searchExpanded
                if (!searchExpanded) onQueryChange("")
            },
            onQueryChange = onQueryChange,
            onToggleDevice = onToggleDevice,
            onClearDevices = onClearDevices,
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading || (isRefreshing && totalCount == 0) -> LoadingView()

                totalCount == 0 -> ErrorView(
                    message = "还没有 $title 目录。\n点「更新目录」拉取一次；" +
                        "也可以到「设置 → 订阅目录地区」切换地区后再试。",
                    retryLabel = "更新目录",
                    onRetry = onRefresh,
                )

                games.isEmpty() -> ErrorView(
                    message = "没有符合条件的游戏。",
                    retryLabel = "清空筛选",
                    onRetry = onClearFilter,
                )

                else -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(games, key = { it.item.id }) { game ->
                            LibraryGameCard(
                                game = game,
                                onClick = { onGameTitleClick(game.item.title) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryToolbar(
    title: String,
    shownCount: Int,
    totalCount: Int,
    lastSyncedAtMillis: Long?,
    filter: LibraryFilter,
    devices: List<String>,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleDevice: (String) -> Unit,
    onClearDevices: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = subtitle(shownCount, totalCount, filter, lastSyncedAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searchExpanded) "关闭搜索" else "在库内搜索",
                )
            }
        }

        if (searchExpanded) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = onQueryChange,
                placeholder = { Text("在 $title 中搜索…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }

        if (devices.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilterChip(
                    selected = filter.devices.isEmpty(),
                    onClick = onClearDevices,
                    label = { Text("全部") },
                )
                devices.forEach { device ->
                    FilterChip(
                        selected = device in filter.devices,
                        onClick = { onToggleDevice(device) },
                        label = { Text(device) },
                    )
                }
            }
        }
    }
}

private fun subtitle(
    shownCount: Int,
    totalCount: Int,
    filter: LibraryFilter,
    lastSyncedAtMillis: Long?,
): String = buildString {
    if (filter.isActive) {
        append(shownCount).append(" / ").append(totalCount).append(" 款")
    } else {
        append(totalCount).append(" 款游戏")
    }
    formatDateTime(lastSyncedAtMillis)?.let { append(" · 更新于 ").append(it) }
}

@Composable
private fun LibraryGameCard(
    game: LibraryGame,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Box {
            CoverImage(
                url = game.item.coverUrl,
                contentDescription = game.item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT),
            )
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = game.item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = game.item.platforms.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
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

/** Catalog art (Xbox hero tiles, PS key art) is widescreen. */
private const val COVER_ASPECT = 16f / 9f
