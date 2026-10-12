package com.ludicindex.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.prefs.ThemeMode
import com.ludicindex.app.ui.appViewModel
import com.ludicindex.app.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onOpenImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = appViewModel { container ->
        SettingsViewModel(
            settingsStore = container.settingsStore,
            gameRepository = container.gameRepository,
            wishlistRepository = container.wishlistRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val imageQuality by viewModel.imageQuality.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearWishlistDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildBackupJson()
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray())
                        } ?: error("cannot open output stream")
                    }.isSuccess
                }
                viewModel.reportExport(ok)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    }.getOrNull()
                }
                if (text == null) {
                    viewModel.reportImportReadFailure()
                } else {
                    viewModel.importBackupJson(text)
                }
            }
        }
    }

    SettingsContent(
        state = state,
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        imageQuality = imageQuality,
        onClientIdChange = viewModel::onClientIdChange,
        onClientSecretChange = viewModel::onClientSecretChange,
        onSave = viewModel::save,
        onTest = viewModel::testConnection,
        onClearCredentials = viewModel::clearCredentials,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onImageQualityChange = viewModel::setImageQuality,
        onPasteImport = onOpenImport,
        onExport = { exportLauncher.launch(defaultBackupFileName()) },
        onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
        onClearWishlist = { showClearWishlistDialog = true },
        onDismissResult = viewModel::dismissResult,
        onClearImageCache = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching { SingletonImageLoader.get(context).diskCache?.clear() }
                }
                viewModel.reportImageCacheCleared()
            }
        },
        modifier = modifier,
    )

    if (showClearWishlistDialog) {
        AlertDialog(
            onDismissRequest = { showClearWishlistDialog = false },
            title = { Text("清空心愿单") },
            text = { Text("会删除所有心愿单条目及其标签关联，且无法撤销。建议先导出备份。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearWishlist()
                        showClearWishlistDialog = false
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearWishlistDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    imageQuality: ImageQuality,
    onClientIdChange: (String) -> Unit,
    onClientSecretChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClearCredentials: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onPasteImport: () -> Unit,
    onExport: () -> Unit,
    onImportBackup: () -> Unit,
    onClearWishlist: () -> Unit,
    onClearImageCache: () -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var secretVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("IGDB 凭据")
        Text(
            text = "IGDB 数据需要你自己的 Twitch 应用凭据。" +
                "在 dev.twitch.tv 注册应用（Client Type 选 Confidential）后，" +
                "把 Client ID 与 Client Secret 填在这里。凭据只保存在本机。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.clientId,
            onValueChange = onClientIdChange,
            label = { Text("Client ID") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.clientSecret,
            onValueChange = onClientSecretChange,
            label = { Text("Client Secret") },
            singleLine = true,
            visualTransformation = if (secretVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                TextButton(onClick = { secretVisible = !secretVisible }) {
                    Text(if (secretVisible) "隐藏" else "显示")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTest, enabled = !state.isBusy) {
                Text("测试连接")
            }
            OutlinedButton(onClick = onSave, enabled = !state.isBusy) {
                Text("保存")
            }
            if (state.hasSavedCredentials) {
                TextButton(onClick = onClearCredentials, enabled = !state.isBusy) {
                    Text("清除凭据")
                }
            }
        }

        if (state.isBusy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("正在处理…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionTitle("外观")
        Text(
            text = "主题",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    label = { Text(mode.displayLabel) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("动态取色", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Android 12+ 跟随壁纸配色",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
        }

        Text(
            text = "图片清晰度",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImageQuality.entries.forEach { quality ->
                FilterChip(
                    selected = imageQuality == quality,
                    onClick = { onImageQualityChange(quality) },
                    label = { Text(quality.displayLabel) },
                )
            }
        }

        SectionTitle("数据")
        Text(
            text = "导出会把心愿单（含标签、状态和备注）写成 JSON 文件；" +
                "导入备份会按 IGDB ID 合并，不会重复添加。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onPasteImport,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("批量导入（粘贴游戏标题）") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExport, enabled = !state.isBusy) { Text("导出心愿单") }
            OutlinedButton(onClick = onImportBackup, enabled = !state.isBusy) { Text("导入备份") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onClearImageCache, enabled = !state.isBusy) {
                Text("清空图片缓存")
            }
            TextButton(onClick = onClearWishlist, enabled = !state.isBusy) {
                Text("清空心愿单")
            }
        }

        state.result?.let { result ->
            ResultDialog(result = result, onDismiss = onDismissResult)
        }
    }
}

/**
 * Every result needs an explicit "确认". The dialog is deliberately **not dismissible**: tapping
 * outside or pressing back does nothing, so the user cannot push it aside and carry on without
 * acknowledging the outcome.
 */
@Composable
private fun ResultDialog(result: TestResult, onDismiss: () -> Unit) {
    val (title, message) = when (result) {
        TestResult.Success -> "连接成功" to "IGDB 数据可用，可以正常使用了。"
        TestResult.Saved -> "已保存" to "凭据已保存到本机。"
        TestResult.Cleared -> "已清除" to "凭据已从本机删除。"
        is TestResult.Info -> "完成" to result.message
        is TestResult.Failure -> "出错了" to result.message
    }
    AlertDialog(
        // Unreachable while both dismiss flags below are false, but the parameter is required.
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确认") }
        },
    )
}

private val ThemeMode.displayLabel: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }

private val ImageQuality.displayLabel: String
    get() = when (this) {
        ImageQuality.STANDARD -> "标准"
        ImageQuality.HIGH -> "高清"
    }

private fun defaultBackupFileName(): String =
    "ludicindex-backup-" +
        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) +
        ".json"
