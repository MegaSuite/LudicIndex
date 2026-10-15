package com.ludicindex.app.ui.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.remote.IgdbException
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.domain.model.CompanyRole
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompanyUiState(
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/** Lists the games a developer / publisher is credited on, newest first, paged. */
class CompanyViewModel(
    private val companyId: Long,
    val companyName: String,
    val role: CompanyRole,
    private val repository: GameRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CompanyUiState())
    val state: StateFlow<CompanyUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CompanyUiState(isLoading = true)
            _state.value = try {
                val games = repository.companyGames(companyId, role, offset = 0)
                CompanyUiState(
                    games = games,
                    isLoading = false,
                    endReached = games.size < PAGE_SIZE,
                )
            } catch (e: IgdbException) {
                CompanyUiState(isLoading = false, error = e.message ?: UNKNOWN)
            } catch (e: Exception) {
                CompanyUiState(isLoading = false, error = e.message ?: UNKNOWN)
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                val more = repository.companyGames(companyId, role, offset = current.games.size)
                _state.update { state ->
                    val known = state.games.mapTo(HashSet()) { it.igdbId }
                    state.copy(
                        games = state.games + more.filter { it.igdbId !in known },
                        isLoadingMore = false,
                        endReached = more.size < PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false, endReached = true) }
            }
        }
    }

    fun retry() = load()

    private companion object {
        const val PAGE_SIZE = 30
        const val UNKNOWN = "加载失败，请稍后重试。"
    }
}
