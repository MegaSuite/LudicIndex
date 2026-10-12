@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.ludicindex.app.ui.detail

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.remote.IgdbImage
import com.ludicindex.app.domain.model.GameDetail
import com.ludicindex.app.domain.model.ReleaseStatus
import com.ludicindex.app.domain.model.RelatedGroup
import com.ludicindex.app.ui.LocalAppContainer
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.CoverImage
import com.ludicindex.app.ui.components.ErrorView
import com.ludicindex.app.ui.components.InfoRow
import com.ludicindex.app.ui.components.LoadingView
import com.ludicindex.app.ui.components.MetaChip
import com.ludicindex.app.ui.components.SectionTitle
import com.ludicindex.app.ui.format.formatCount
import com.ludicindex.app.ui.format.formatRating
import com.ludicindex.app.ui.format.formatReleaseDate
import com.ludicindex.app.ui.format.formatReleaseYear
import com.ludicindex.app.ui.format.websiteTypeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    igdbId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onGameClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GameDetailViewModel = appViewModel { container ->
        GameDetailViewModel(
            igdbId = igdbId,
            detailRepository = container.gameDetailRepository,
            wishlistRepository = container.wishlistRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageQuality by LocalAppContainer.current.settingsStore.imageQuality
        .collectAsStateWithLifecycle(initialValue = ImageQuality.STANDARD)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { text ->
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.detail?.name ?: "游戏详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.detail != null) {
                WishlistActionBar(
                    inWishlist = state.inWishlist,
                    onAdd = viewModel::addToWishlist,
                    onRemove = viewModel::removeFromWishlist,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        when (val content = state.content) {
            DetailContentState.Loading -> LoadingView(Modifier.padding(innerPadding))

            is DetailContentState.Error -> ErrorView(
                message = content.message,
                onRetry = viewModel::retry,
                secondaryLabel = if (content.missingCredentials) "去设置" else null,
                onSecondary = if (content.missingCredentials) onOpenSettings else null,
                modifier = Modifier.padding(innerPadding),
            )

            is DetailContentState.Content -> DetailBody(
                detail = content.detail,
                staleMessage = content.staleMessage,
                isRefreshing = state.isRefreshing,
                imageQuality = imageQuality,
                onRefresh = viewModel::refresh,
                onGameClick = onGameClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailBody(
    detail: GameDetail,
    staleMessage: String?,
    isRefreshing: Boolean,
    imageQuality: ImageQuality,
    onRefresh: () -> Unit,
    onGameClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var previewImageId by remember { mutableStateOf<String?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Hero(
                    detail = detail,
                    imageQuality = imageQuality,
                )
            }

            if (staleMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "当前显示的是离线缓存内容：$staleMessage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            item { MetaSection(detail = detail, imageQuality = imageQuality) }

            if (detail.relatedGroups.isNotEmpty()) {
                item {
                    RelatedContentSection(
                        groups = detail.relatedGroups,
                        imageQuality = imageQuality,
                        onGameClick = onGameClick,
                    )
                }
            }

            if (detail.summary != null || detail.storyline != null) {
                item { SummarySection(detail) }
            }

            if (detail.screenshotImageIds.isNotEmpty()) {
                item {
                    SectionBlock(title = "截图") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(detail.screenshotImageIds) { imageId ->
                                AsyncImage(
                                    model = IgdbImage.url(imageId, imageQuality.screenshotSize),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(240.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { previewImageId = imageId },
                                )
                            }
                        }
                    }
                }
            }

            if (detail.videos.isNotEmpty()) {
                item {
                    SectionBlock(title = "预告片") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(detail.videos, key = { it.videoId }) { video ->
                                Column(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clickable {
                                            context.openUrl(IgdbImage.videoWatchUrl(video.videoId))
                                        },
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = IgdbImage.videoThumbnailUrl(video.videoId),
                                            contentDescription = video.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(36.dp),
                                        )
                                    }
                                    if (video.name != null) {
                                        Text(
                                            text = video.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { FactSheet(detail) }

            if (detail.languages.isNotEmpty()) {
                item {
                    SectionBlock(title = "支持语言") {
                        ChipRow(detail.languages, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            if (detail.websites.isNotEmpty()) {
                item {
                    SectionBlock(title = "相关链接") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            detail.websites.forEach { website ->
                                MetaChip(
                                    text = websiteTypeLabel(website.type),
                                    modifier = Modifier.clickable {
                                        context.openUrl(IgdbImage.websiteUrl(website.url))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (detail.similarGames.isNotEmpty()) {
                item {
                    SectionBlock(title = "相似游戏") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(detail.similarGames, key = { it.igdbId }) { similar ->
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { onGameClick(similar.igdbId) },
                                ) {
                                    CoverImage(
                                        url = IgdbImage.coverUrl(similar.coverImageId, imageQuality),
                                        contentDescription = similar.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(3f / 4.25f)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Text(
                                        text = similar.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewImageId?.let { imageId ->
        Dialog(onDismissRequest = { previewImageId = null }) {
            AsyncImage(
                model = IgdbImage.url(imageId, ImageQuality.HIGH.screenshotSize),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
private fun Hero(detail: GameDetail, imageQuality: ImageQuality) {
    val heroImageId = detail.artworkImageIds.firstOrNull()
        ?: detail.screenshotImageIds.firstOrNull()
    val heroUrl = heroImageId?.let { IgdbImage.url(it, imageQuality.screenshotSize) }
        ?: IgdbImage.coverUrl(detail.coverImageId, imageQuality)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        CoverImage(
            url = heroUrl,
            contentDescription = detail.name,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            val subtitle = listOfNotNull(
                formatReleaseDate(detail.releaseDate),
                detail.releaseStatusLabel(),
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun MetaSection(detail: GameDetail, imageQuality: ImageQuality) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        CoverImage(
            url = IgdbImage.coverUrl(detail.coverImageId, imageQuality),
            contentDescription = detail.name,
            modifier = Modifier
                .width(88.dp)
                .height(124.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            StatRow(
                listOfNotNull(
                    formatRating(detail.totalRating ?: detail.rating)?.let { it to "用户评分" },
                    formatRating(detail.aggregatedRating)?.let { it to "媒体评分" },
                    formatCount(detail.ratingCount)?.let { it to "评分人数" },
                ),
            )
            if (detail.genres.isNotEmpty()) {
                Text(
                    text = detail.genres.joinToString("、"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                detail.platforms.take(6).forEach { MetaChip(text = it) }
            }
        }
    }
}

@Composable
private fun StatRow(stats: List<Pair<String, String>>) {
    if (stats.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        stats.forEach { (value, label) ->
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummarySection(detail: GameDetail) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        detail.summary?.let { ExpandableText(title = "简介", text = it) }
        detail.storyline?.let { ExpandableText(title = "剧情", text = it) }
    }
}

/**
 * Collapsed to a few lines by default. The toggle only shows up when the text is actually clipped,
 * so short blurbs do not get a pointless "展开" button.
 */
@Composable
private fun ExpandableText(
    title: String,
    text: String,
    collapsedMaxLines: Int = COLLAPSED_LINES,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var isClipped by remember(text) { mutableStateOf(false) }

    SectionTitle(title)
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout -> if (!expanded) isClipped = layout.hasVisualOverflow },
        modifier = Modifier.clickable(enabled = isClipped || expanded) { expanded = !expanded },
    )
    if (isClipped || expanded) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        ) {
            Text(if (expanded) "收起" else "展开全文")
        }
    }
}

private const val COLLAPSED_LINES = 4

@Composable
private fun FactSheet(detail: GameDetail) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("详细信息")
        InfoRow("开发商", detail.developers)
        InfoRow("发行商", detail.publishers)
        InfoRow("移植", detail.portingCompanies)
        InfoRow("系列", listOfNotNull(detail.collection))
        InfoRow("作品线", listOfNotNull(detail.franchise))
        InfoRow("类型", detail.genres)
        InfoRow("主题", detail.themes)
        InfoRow("游戏模式", detail.gameModes)
        InfoRow("玩家视角", detail.playerPerspectives)
        InfoRow("引擎", detail.engines)
        InfoRow("发行日期", listOfNotNull(formatReleaseDate(detail.releaseDate)))
    }
}

@Composable
private fun RelatedContentSection(
    groups: List<RelatedGroup>,
    imageQuality: ImageQuality,
    onGameClick: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val total = groups.sumOf { it.games.size }

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
        ) {
            Text(
                text = "相关内容",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起" else "展开（$total）")
            }
        }

        if (!expanded) {
            // Collapsed: show what is inside without rendering dozens of covers.
            Text(
                text = groups.joinToString("、") { "${it.title} ${it.games.size}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
        } else {
            groups.forEach { group ->
                Text(
                    text = "${group.title}（${group.games.size}）",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(group.games, key = { it.igdbId }) { game ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { onGameClick(game.igdbId) },
                        ) {
                            CoverImage(
                                url = IgdbImage.coverUrl(game.coverImageId, imageQuality),
                                contentDescription = game.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4.25f)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Text(
                                text = game.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            formatReleaseYear(game.releaseDate)?.let { year ->
                                Text(
                                    text = year,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun ChipRow(values: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        values.forEach { MetaChip(text = it) }
    }
}

@Composable
private fun WishlistActionBar(
    inWishlist: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (inWishlist) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "已在心愿单",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                OutlinedButton(onClick = onRemove) { Text("移除") }
            } else {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("加入心愿单")
                }
            }
        }
    }
}

private fun GameDetail.releaseStatusLabel(): String? = when (releaseStatus(System.currentTimeMillis() / 1000)) {
    ReleaseStatus.RELEASED -> "已发售"
    ReleaseStatus.UPCOMING -> "未发售"
    ReleaseStatus.UNKNOWN -> null
}

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
