package com.ludicindex.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.repository.LibraryRepository
import com.ludicindex.app.domain.match.TitleMatcher
import com.ludicindex.app.domain.model.LibraryGame
import com.ludicindex.app.domain.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Client-side search + device filter applied to a catalog tab (never hits the network). */
data class LibraryFilter(
    val query: String = "",
    val devices: Set<String> = emptySet(),
) {
    val isActive: Boolean get() = query.isNotBlank() || devices.isNotEmpty()
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val xbox: List<LibraryGame> = emptyList(),
    val psPlus: List<LibraryGame> = emptyList(),
    val xboxTotal: Int = 0,
    val psPlusTotal: Int = 0,
    val xboxDevices: List<String> = emptyList(),
    val psPlusDevices: List<String> = emptyList(),
    val xboxFilter: LibraryFilter = LibraryFilter(),
    val psPlusFilter: LibraryFilter = LibraryFilter(),
    val lastSyncedAtMillis: Long? = null,
    /** Non-null when a refresh partly or fully failed; shown as a dialog. */
    val message: String? = null,
)

private data class Transient(val isRefreshing: Boolean, val message: String?)

/** Backs the XGP / PS Plus sub-tabs of the home screen. */
class LibraryViewModel(
    private val repository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val xboxFilter = MutableStateFlow(LibraryFilter())
    private val psPlusFilter = MutableStateFlow(LibraryFilter())
    private var loadRequested = false

    val state: StateFlow<LibraryUiState> = combine(
        repository.observeGames(),
        repository.observeSources(),
        combine(refreshing, message) { r, m -> Transient(r, m) },
        xboxFilter,
        psPlusFilter,
    ) { games, sources, transient, xboxFilterValue, psPlusFilterValue ->
        val xboxAll = games.filter { it.platform == Platform.XBOX }
        // The PS Plus tab concatenates three catalogs (game catalog / classics / monthly); merge
        // entries that share a title so each game appears exactly once.
        val psPlusAll = games.filter { it.platform == Platform.PSN }.mergedByTitle()
        LibraryUiState(
            isLoading = false,
            isRefreshing = transient.isRefreshing,
            xbox = xboxAll.applyFilter(xboxFilterValue),
            psPlus = psPlusAll.applyFilter(psPlusFilterValue),
            xboxTotal = xboxAll.size,
            psPlusTotal = psPlusAll.size,
            xboxDevices = xboxAll.deviceOptions(),
            psPlusDevices = psPlusAll.deviceOptions(),
            xboxFilter = xboxFilterValue,
            psPlusFilter = psPlusFilterValue,
            lastSyncedAtMillis = sources.mapNotNull { it.lastSyncedAt }.maxOrNull(),
            message = transient.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        initialValue = LibraryUiState(),
    )

    /** Fetches the catalogs the first time a library tab is opened (not on app start). */
    fun ensureLoaded() {
        if (loadRequested) return
        loadRequested = true
        viewModelScope.launch {
            // Clear anything left behind by older builds before deciding whether to fetch.
            repository.pruneStaleSources()
            if (repository.observeGames().first().isEmpty()) refresh()
        }
    }

    fun refresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            message.value = null
            val report = try {
                repository.refreshSubscriptions(
                    market = settingsStore.gamePassMarket.first(),
                    psLocale = settingsStore.psPlusLocale.first(),
                )
            } catch (e: Exception) {
                message.value = e.message ?: UNKNOWN_ERROR
                refreshing.value = false
                return@launch
            }
            message.value = if (report.isComplete) null else report.failures.joinToString("\n")
            refreshing.value = false
        }
    }

    fun setQuery(platform: Platform, value: String) =
        updateFilter(platform) { it.copy(query = value) }

    fun toggleDevice(platform: Platform, device: String) = updateFilter(platform) {
        it.copy(devices = if (device in it.devices) it.devices - device else it.devices + device)
    }

    fun clearDevices(platform: Platform) = updateFilter(platform) { it.copy(devices = emptySet()) }

    fun clearFilter(platform: Platform) = updateFilter(platform) { LibraryFilter() }

    fun dismissMessage() {
        message.value = null
    }

    private fun updateFilter(platform: Platform, transform: (LibraryFilter) -> LibraryFilter) {
        when (platform) {
            Platform.PSN -> psPlusFilter.update(transform)
            else -> xboxFilter.update(transform)
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val UNKNOWN_ERROR = "更新游戏库失败，请稍后重试。"
    }
}

/** Collapses entries with the same (normalized) title into one, unioning their devices. */
private fun List<LibraryGame>.mergedByTitle(): List<LibraryGame> {
    val result = LinkedHashMap<String, LibraryGame>()
    forEach { game ->
        val key = TitleMatcher.normalize(game.item.title)
        val existing = result[key]
        result[key] = if (existing == null) {
            game
        } else {
            existing.copy(
                item = existing.item.copy(
                    platforms = (existing.item.platforms + game.item.platforms).distinct(),
                    coverUrl = existing.item.coverUrl ?: game.item.coverUrl,
                ),
            )
        }
    }
    return result.values.toList()
}

private fun List<LibraryGame>.deviceOptions(): List<String> =
    flatMap { it.item.platforms }.distinct().sorted()

private fun List<LibraryGame>.applyFilter(filter: LibraryFilter): List<LibraryGame> {
    val query = filter.query.trim()
    return filter { game ->
        (query.isEmpty() || game.item.title.contains(query, ignoreCase = true)) &&
            (filter.devices.isEmpty() || game.item.platforms.any { it in filter.devices })
    }
}
