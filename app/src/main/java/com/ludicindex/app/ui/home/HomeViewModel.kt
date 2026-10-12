package com.ludicindex.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.remote.IgdbException
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeListState {
    data object Loading : HomeListState
    data class Content(val games: List<Game>) : HomeListState
    data object Empty : HomeListState
    data class Error(val message: String, val missingCredentials: Boolean = false) : HomeListState
}

data class HomeUiState(
    val query: String = "",
    val popular: HomeListState = HomeListState.Loading,
    /** null while the query is blank: the screen shows the board instead of results. */
    val search: HomeListState? = null,
    val isRefreshing: Boolean = false,
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val gameRepository: GameRepository,
    private val searchDebounceMillis: Long = DEFAULT_SEARCH_DEBOUNCE,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val popular = MutableStateFlow<HomeListState>(HomeListState.Loading)
    private val search = MutableStateFlow<HomeListState?>(null)
    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<HomeUiState> = combine(
        query,
        popular,
        search,
        refreshing,
    ) { query, popular, search, refreshing ->
        HomeUiState(query = query, popular = popular, search = search, isRefreshing = refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    init {
        loadPopular()
        viewModelScope.launch {
            query
                .debounce(searchDebounceMillis)
                .distinctUntilChanged()
                .collect { term -> search.value = runSearch(term) }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun loadPopular() {
        viewModelScope.launch {
            popular.value = HomeListState.Loading
            popular.value = fetchPopular()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            when {
                query.value.isNotBlank() -> search.value = runSearch(query.value)
                else -> popular.value = fetchPopular()
            }
            refreshing.value = false
        }
    }

    fun retry() {
        if (query.value.isNotBlank()) {
            viewModelScope.launch { search.value = runSearch(query.value) }
        } else {
            loadPopular()
        }
    }

    private suspend fun fetchPopular(): HomeListState = try {
        val games = gameRepository.popularRightNow()
        if (games.isEmpty()) HomeListState.Empty else HomeListState.Content(games)
    } catch (e: IgdbException) {
        e.toErrorState()
    } catch (e: Exception) {
        HomeListState.Error(e.message ?: UNKNOWN_ERROR)
    }

    private suspend fun runSearch(term: String): HomeListState? {
        if (term.isBlank()) return null
        search.value = HomeListState.Loading
        return try {
            val games = gameRepository.searchGames(term)
            if (games.isEmpty()) {
                HomeListState.Empty
            } else {
                HomeListState.Content(games)
            }
        } catch (e: IgdbException) {
            e.toErrorState()
        } catch (e: Exception) {
            HomeListState.Error(e.message ?: UNKNOWN_ERROR)
        }
    }

    private fun IgdbException.toErrorState() = HomeListState.Error(
        message = message ?: UNKNOWN_ERROR,
        missingCredentials = this is IgdbException.MissingCredentials,
    )

    private companion object {
        const val DEFAULT_SEARCH_DEBOUNCE = 300L
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val UNKNOWN_ERROR = "出错了，请稍后重试。"
    }
}
