package com.ludicindex.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.backup.WishlistBackupCodec
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.prefs.ThemeMode
import com.ludicindex.app.data.remote.IgdbException
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.data.repository.WishlistRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface TestResult {
    data object Success : TestResult
    data object Saved : TestResult
    data object Cleared : TestResult
    data class Info(val message: String) : TestResult
    data class Failure(val message: String) : TestResult
}

data class SettingsUiState(
    val clientId: String = "",
    val clientSecret: String = "",
    val hasSavedCredentials: Boolean = false,
    val isBusy: Boolean = false,
    val result: TestResult? = null,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val gameRepository: GameRepository,
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settingsStore.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), true)

    val imageQuality: StateFlow<ImageQuality> = settingsStore.imageQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ImageQuality.STANDARD)

    init {
        viewModelScope.launch {
            val saved = settingsStore.credentials.first()
            if (saved != null) {
                _state.update {
                    it.copy(
                        clientId = saved.clientId,
                        clientSecret = saved.clientSecret,
                        hasSavedCredentials = true,
                    )
                }
            }
        }
    }

    fun onClientIdChange(value: String) {
        _state.update { it.copy(clientId = value, result = null) }
    }

    fun onClientSecretChange(value: String) {
        _state.update { it.copy(clientSecret = value, result = null) }
    }

    fun save() {
        val current = _state.value
        if (!current.hasInput()) {
            showResult(TestResult.Failure(EMPTY_INPUT_MESSAGE))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            settingsStore.saveCredentials(current.clientId, current.clientSecret)
            _state.update { it.copy(isBusy = false, hasSavedCredentials = true) }
            showResult(TestResult.Saved)
        }
    }

    /** Saves first (so the client can use the credentials) and then makes one cheap IGDB call. */
    fun testConnection() {
        val current = _state.value
        if (!current.hasInput()) {
            showResult(TestResult.Failure(EMPTY_INPUT_MESSAGE))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, result = null) }
            settingsStore.saveCredentials(current.clientId, current.clientSecret)
            val result = try {
                gameRepository.testConnection()
                TestResult.Success
            } catch (e: IgdbException) {
                TestResult.Failure(e.message ?: UNKNOWN_MESSAGE)
            } catch (e: Exception) {
                TestResult.Failure(e.message ?: UNKNOWN_MESSAGE)
            }
            _state.update { it.copy(isBusy = false, hasSavedCredentials = true) }
            showResult(result)
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            settingsStore.clearCredentials()
            _state.value = SettingsUiState(hasSavedCredentials = false)
            showResult(TestResult.Cleared)
        }
    }

    // ---------- appearance ----------

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColor(enabled) }
    }

    fun setImageQuality(quality: ImageQuality) {
        viewModelScope.launch { settingsStore.setImageQuality(quality) }
    }

    // ---------- backup ----------

    /** Suspends so the caller can write the string to the SAF stream it owns. */
    suspend fun buildBackupJson(): String =
        WishlistBackupCodec.encode(wishlistRepository.exportBackup())

    fun reportExport(success: Boolean) {
        showResult(
            if (success) {
                TestResult.Info("心愿单已导出。")
            } else {
                TestResult.Failure("导出失败，请换一个位置重试。")
            },
        )
    }

    fun reportImportReadFailure() {
        showResult(TestResult.Failure("读取文件失败，请重新选择。"))
    }

    fun importBackupJson(text: String) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, result = null) }
            val outcome = runCatching {
                val backup = WishlistBackupCodec.decode(text)
                wishlistRepository.importBackup(backup, merge = true)
            }
            _state.update { it.copy(isBusy = false) }
            showResult(
                outcome.fold(
                    onSuccess = { stats ->
                        TestResult.Info(
                            "导入完成：新增 ${stats.added}，更新 ${stats.updated}，" +
                                "跳过 ${stats.skipped}。",
                        )
                    },
                    onFailure = { error ->
                        TestResult.Failure("导入失败：${error.message ?: "文件格式不正确"}")
                    },
                ),
            )
        }
    }

    fun clearWishlist() {
        viewModelScope.launch {
            wishlistRepository.clearWishlist()
            showResult(TestResult.Info("心愿单已清空。"))
        }
    }

    fun reportImageCacheCleared() {
        showResult(TestResult.Info("图片缓存已清空。"))
    }

    fun dismissResult() {
        _state.update { it.copy(result = null) }
    }

    /**
     * Results are shown as a dialog the user has to acknowledge. They used to be an inline card at
     * the bottom of a long scrolling page which also auto-hid after 4s — easy to miss entirely.
     */
    private fun showResult(result: TestResult) {
        _state.update { it.copy(result = result) }
    }

    private fun SettingsUiState.hasInput(): Boolean =
        clientId.isNotBlank() && clientSecret.isNotBlank()

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val EMPTY_INPUT_MESSAGE = "请先填写 Client ID 与 Client Secret。"
        const val UNKNOWN_MESSAGE = "连接失败，请稍后重试。"
    }
}
