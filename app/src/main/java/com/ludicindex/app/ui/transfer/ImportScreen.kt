package com.ludicindex.app.ui.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.remote.IgdbImage
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.domain.parseGameTitles
import com.ludicindex.app.ui.LocalAppContainer
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.CoverImage
import com.ludicindex.app.ui.components.SectionTitle
import com.ludicindex.app.ui.format.formatReleaseYear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ImportViewModel = appViewModel { container ->
        ImportViewModel(container.gameRepository, container.wishlistRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageQuality by LocalAppContainer.current.settingsStore.imageQuality
        .collectAsStateWithLifecycle(initialValue = ImageQuality.STANDARD)

    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            if (state.phase == ImportPhase.REVIEW) {
                ImportBottomBar(
                    selectedCount = state.selectedCount,
                    totalCount = state.totalCount,
                    isCommitting = state.isCommitting,
                    onCommit = viewModel::commit,
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state.phase) {
                ImportPhase.INPUT -> ImportInputStep(
                    rawText = state.rawText,
                    error = state.error,
                    onTextChange = viewModel::onTextChange,
                    onMatch = viewModel::startMatching,
                )

                ImportPhase.MATCHING -> ImportMatchingStep(
                    matched = state.matchedCount,
                    total = state.totalCount,
                )

                ImportPhase.REVIEW -> ImportReviewStep(
                    state = state,
                    imageQuality = imageQuality,
                    expandedIndex = expandedIndex,
                    editingIndex = editingIndex,
                    onToggleExpand = { index ->
                        expandedIndex = if (expandedIndex == index) null else index
                    },
                    onStartEdit = { index -> editingIndex = index },
                    onCancelEdit = { editingIndex = null },
                    onTitleChange = viewModel::onTitleChange,
                    onResearch = { index ->
                        editingIndex = null
                        viewModel.research(index)
                    },
                    onChoose = viewModel::choose,
                    onSkip = viewModel::setSkipped,
                    onSkipAllWithoutMatch = viewModel::skipAllWithoutMatch,
                )

                ImportPhase.DONE -> ImportDoneStep(
                    outcome = state.outcome,
                    onDone = onDone,
                    onReset = viewModel::reset,
                )
            }
        }
    }
}

@Composable
private fun ImportInputStep(
    rawText: String,
    error: String?,
    onTextChange: (String) -> Unit,
    onMatch: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "每行一个游戏标题，粘贴后会自动到 IGDB 搜索，下一步你可以逐个确认匹配结果。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = rawText,
            onValueChange = onTextChange,
            placeholder = { Text("Hollow Knight\nCeleste\nHades") },
            minLines = 8,
            maxLines = 14,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "已识别 ${parseGameTitles(rawText).size} 行",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onMatch,
            enabled = parseGameTitles(rawText).isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("开始匹配") }

        Text(
            text = "提示：IGDB 限制 4 请求/秒，标题较多时会逐个匹配，请耐心等待。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImportMatchingStep(matched: Int, total: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = "正在匹配 $matched / $total…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (total > 0) {
            LinearProgressIndicator(
                progress = { matched.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ImportReviewStep(
    state: ImportUiState,
    imageQuality: ImageQuality,
    expandedIndex: Int?,
    editingIndex: Int?,
    onToggleExpand: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onTitleChange: (Int, String) -> Unit,
    onResearch: (Int) -> Unit,
    onChoose: (Int, Game) -> Unit,
    onSkip: (Int, Boolean) -> Unit,
    onSkipAllWithoutMatch: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = buildString {
                    append("匹配完成：${state.totalCount} 行")
                    if (state.missingCount > 0) append("，${state.missingCount} 行没找到")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.missingCount > 0) {
                TextButton(onClick = onSkipAllWithoutMatch) { Text("全部跳过") }
            }
        }

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.matches.size) { index ->
                val match = state.matches[index]
                MatchRow(
                    match = match,
                    imageQuality = imageQuality,
                    expanded = expandedIndex == index,
                    editing = editingIndex == index,
                    onToggleExpand = { onToggleExpand(index) },
                    onStartEdit = { onStartEdit(index) },
                    onCancelEdit = onCancelEdit,
                    onTitleChange = { value -> onTitleChange(index, value) },
                    onResearch = { onResearch(index) },
                    onChoose = { onChoose(index, it) },
                    onSkip = { onSkip(index, it) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MatchRow(
    match: ImportMatch,
    imageQuality: ImageQuality,
    expanded: Boolean,
    editing: Boolean,
    onToggleExpand: () -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onTitleChange: (String) -> Unit,
    onResearch: () -> Unit,
    onChoose: (Game) -> Unit,
    onSkip: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (editing) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = match.title,
                        onValueChange = onTitleChange,
                        label = { Text("游戏标题") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onResearch() }),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "跳过",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Checkbox(checked = match.skipped, onCheckedChange = onSkip)
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onCancelEdit) { Text("取消") }
                    Button(
                        onClick = onResearch,
                        enabled = match.title.isNotBlank() && !match.searching,
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("重新搜索") }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (match.skipped) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onStartEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "修改标题后重新搜索",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "跳过",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Checkbox(checked = match.skipped, onCheckedChange = onSkip)
            }
        }

        when {
            match.searching -> Text(
                text = "搜索中…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            match.candidates.isEmpty() -> Text(
                text = "没有找到匹配的游戏",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            else -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = match.candidates.size > 1, onClick = onToggleExpand),
            ) {
                CoverImage(
                    url = IgdbImage.coverUrl(match.selected?.coverImageId, imageQuality),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 40.dp, height = 54.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    Text(
                        text = match.selected?.name ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        formatReleaseYear(match.selected?.releaseDate),
                        match.selected?.genres?.firstOrNull(),
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (match.candidates.size > 1) {
                    Text(
                        text = if (expanded) "收起" else "换一个（${match.candidates.size}）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (expanded && match.candidates.size > 1) {
            SectionTitle("选择正确的游戏")
            match.candidates.forEach { candidate ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChoose(candidate) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = candidate.igdbId == match.selected?.igdbId,
                        onClick = { onChoose(candidate) },
                    )
                    CoverImage(
                        url = IgdbImage.coverUrl(candidate.coverImageId, imageQuality),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 32.dp, height = 44.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = candidate.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                formatReleaseYear(candidate.releaseDate),
                                candidate.platforms.firstOrNull(),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportBottomBar(
    selectedCount: Int,
    totalCount: Int,
    isCommitting: Boolean,
    onCommit: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "已选 $selectedCount / $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (isCommitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onCommit, enabled = selectedCount > 0) {
                    Text("加入心愿单")
                }
            }
        }
    }
}

@Composable
private fun ImportDoneStep(
    outcome: ImportOutcome?,
    onDone: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = "导入完成", style = MaterialTheme.typography.titleMedium)
        if (outcome != null) {
            Text(
                text = "新增 ${outcome.added} · 已存在 ${outcome.alreadyThere} · " +
                    "跳过 ${outcome.skipped} · 失败 ${outcome.failed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = onDone,
            modifier = Modifier.padding(top = 24.dp),
        ) { Text("完成") }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("再导一批") }
    }
}
