package com.ludicindex.app.ui.company

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.domain.model.CompanyRole
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.ui.LocalAppContainer
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.EmptyView
import com.ludicindex.app.ui.components.ErrorView
import com.ludicindex.app.ui.components.GameListItem
import com.ludicindex.app.ui.components.LoadingView

/** "Other games by this developer / publisher", reached from the detail page's company links. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyScreen(
    companyId: Long,
    companyName: String,
    role: CompanyRole,
    onBack: () -> Unit,
    onGameClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CompanyViewModel = appViewModel(key = "company-$companyId-${role.name}") { container ->
        CompanyViewModel(
            companyId = companyId,
            companyName = companyName,
            role = role,
            repository = container.gameRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageQuality by LocalAppContainer.current.settingsStore.imageQuality
        .collectAsStateWithLifecycle(initialValue = ImageQuality.STANDARD)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = companyName.ifBlank { "公司" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = role.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingView(contentModifier)

            state.games.isEmpty() && state.error != null -> ErrorView(
                message = state.error!!,
                onRetry = viewModel::retry,
                modifier = contentModifier,
            )

            state.games.isEmpty() -> EmptyView(
                title = "没有找到该公司的游戏",
                subtitle = "IGDB 上暂时没有可展示的条目。",
                modifier = contentModifier,
            )

            else -> CompanyGameList(
                games = state.games,
                imageQuality = imageQuality,
                showLoadingMore = !state.endReached,
                onGameClick = onGameClick,
                onLoadMore = viewModel::loadMore,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun CompanyGameList(
    games: List<Game>,
    imageQuality: ImageQuality,
    showLoadingMore: Boolean,
    onGameClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState: LazyListState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            games.isNotEmpty() && last >= games.size - 3
        }
    }
    LaunchedEffect(shouldLoadMore, showLoadingMore) {
        if (shouldLoadMore && showLoadingMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(games, key = { it.igdbId }) { game ->
            GameListItem(
                game = game,
                imageQuality = imageQuality,
                onClick = { onGameClick(game.igdbId) },
            )
        }
        if (showLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}
