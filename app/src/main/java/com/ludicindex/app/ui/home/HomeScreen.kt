package com.ludicindex.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.ui.LocalAppContainer
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.EmptyView
import com.ludicindex.app.ui.components.ErrorView
import com.ludicindex.app.ui.components.GameCard
import com.ludicindex.app.ui.components.GameListItem
import com.ludicindex.app.ui.components.LoadingView

@Composable
fun HomeScreen(
    onGameClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = appViewModel { container ->
        HomeViewModel(container.gameRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val container = LocalAppContainer.current
    val imageQuality by container.settingsStore.imageQuality
        .collectAsStateWithLifecycle(initialValue = ImageQuality.STANDARD)

    HomeContent(
        state = state,
        imageQuality = imageQuality,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onGameClick = onGameClick,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    imageQuality: ImageQuality,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onGameClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
        )

        Box(modifier = Modifier.weight(1f)) {
            val listState = if (state.isSearching) {
                state.search ?: HomeListState.Loading
            } else {
                state.popular
            }

            when (listState) {
                HomeListState.Loading -> LoadingView()

                HomeListState.Empty -> EmptyView(
                    title = if (state.isSearching) "没有找到匹配的游戏" else "暂时拿不到榜单",
                    subtitle = if (state.isSearching) "换个关键词试试" else "下拉刷新或稍后重试",
                )

                is HomeListState.Error -> ErrorView(
                    message = listState.message,
                    onRetry = onRetry,
                    secondaryLabel = if (listState.missingCredentials) "去设置" else null,
                    onSecondary = if (listState.missingCredentials) onOpenSettings else null,
                )

                is HomeListState.Content -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                ) {
                    if (state.isSearching) {
                        SearchResults(
                            games = listState.games,
                            imageQuality = imageQuality,
                            onGameClick = onGameClick,
                        )
                    } else {
                        PopularBoard(
                            games = listState.games,
                            imageQuality = imageQuality,
                            onGameClick = onGameClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("搜索游戏…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(Icons.Filled.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PopularBoard(
    games: List<Game>,
    imageQuality: ImageQuality,
    onGameClick: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Popular Right Now",
                subtitle = "按 IGDB PopScore 访问热度排序",
            )
        }
        items(games, key = { it.igdbId }) { game ->
            GameCard(
                game = game,
                imageQuality = imageQuality,
                onClick = { onGameClick(game.igdbId) },
            )
        }
    }
}

@Composable
private fun SearchResults(
    games: List<Game>,
    imageQuality: ImageQuality,
    onGameClick: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(games, key = { it.igdbId }) { game ->
            GameListItem(
                game = game,
                imageQuality = imageQuality,
                onClick = { onGameClick(game.igdbId) },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
