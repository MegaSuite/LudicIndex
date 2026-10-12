package com.ludicindex.app.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.remote.IgdbException
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.domain.parseGameTitles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportPhase { INPUT, MATCHING, REVIEW, DONE }

/** One pasted line plus whatever IGDB search turned up for it. */
data class ImportMatch(
    val title: String,
    val candidates: List<Game> = emptyList(),
    val selected: Game? = null,
    val searching: Boolean = true,
    val skipped: Boolean = false,
) {
    val hasNoMatch: Boolean get() = !searching && candidates.isEmpty()
}

data class ImportOutcome(
    val added: Int = 0,
    val alreadyThere: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
) {
    val total: Int get() = added + alreadyThere + skipped + failed
}

data class ImportUiState(
    val phase: ImportPhase = ImportPhase.INPUT,
    val rawText: String = "",
    val matches: List<ImportMatch> = emptyList(),
    val matchedCount: Int = 0,
    val isCommitting: Boolean = false,
    val outcome: ImportOutcome? = null,
    val error: String? = null,
) {
    val totalCount: Int get() = matches.size
    val selectedCount: Int get() = matches.count { !it.skipped && it.selected != null }
    val missingCount: Int get() = matches.count { it.hasNoMatch }
}

class ImportViewModel(
    private val gameRepository: GameRepository,
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun onTextChange(value: String) {
        _state.update { it.copy(rawText = value, error = null) }
    }

    fun startMatching() {
        val titles = parseGameTitles(_state.value.rawText)
        if (titles.isEmpty()) {
            _state.update { it.copy(error = "请先粘贴至少一行游戏标题。") }
            return
        }

        _state.update {
            it.copy(
                phase = ImportPhase.MATCHING,
                matches = titles.map { title -> ImportMatch(title) },
                matchedCount = 0,
                error = null,
                outcome = null,
            )
        }

        viewModelScope.launch {
            titles.forEachIndexed { index, title ->
                // The shared RateLimiter already spaces these out to stay under IGDB's 4 req/s.
                val candidates = search(title)

                _state.update { current ->
                    current.copy(
                        matches = current.matches.mapIndexed { i, match ->
                            if (i != index) {
                                match
                            } else {
                                match.copy(
                                    candidates = candidates,
                                    selected = candidates.firstOrNull(),
                                    searching = false,
                                )
                            }
                        },
                        matchedCount = index + 1,
                    )
                }
            }
            _state.update { it.copy(phase = ImportPhase.REVIEW) }
        }
    }

    /** Lets the user fix a mistyped title without starting the whole batch over. */
    fun onTitleChange(index: Int, value: String) {
        updateMatch(index) { it.copy(title = value) }
    }

    /** Re-runs the search for one row using its current (possibly edited) title. */
    fun research(index: Int) {
        val match = _state.value.matches.getOrNull(index) ?: return
        // Drop the stale match right away so the row shows "搜索中…" instead of the old result.
        updateMatch(index) { it.copy(searching = true, candidates = emptyList(), selected = null) }

        viewModelScope.launch {
            val candidates = search(match.title)
            updateMatch(index) {
                it.copy(
                    candidates = candidates,
                    selected = candidates.firstOrNull(),
                    searching = false,
                )
            }
        }
    }

    private suspend fun search(title: String): List<Game> =
        runCatching { gameRepository.searchGames(title, limit = CANDIDATE_LIMIT) }
            .getOrDefault(emptyList())

    fun choose(index: Int, game: Game) {
        updateMatch(index) { it.copy(selected = game, skipped = false) }
    }

    fun setSkipped(index: Int, skipped: Boolean) {
        updateMatch(index) { it.copy(skipped = skipped) }
    }

    fun skipAllWithoutMatch() {
        _state.update { current ->
            current.copy(
                matches = current.matches.map { match ->
                    if (match.hasNoMatch) match.copy(skipped = true) else match
                },
            )
        }
    }

    fun commit() {
        val selected = _state.value.matches.filter { !it.skipped }
        if (selected.isEmpty()) {
            _state.update { it.copy(error = "没有选中任何条目。") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCommitting = true, error = null) }

            var added = 0
            var alreadyThere = 0
            var failed = 0

            selected.forEach { match ->
                val game = match.selected
                if (game == null) {
                    failed++
                } else {
                    val result = runCatching { wishlistRepository.addGame(game) }
                    when {
                        result.getOrDefault(false) -> added++
                        result.isSuccess -> alreadyThere++
                        else -> failed++
                    }
                }
            }

            _state.update {
                it.copy(
                    isCommitting = false,
                    phase = ImportPhase.DONE,
                    outcome = ImportOutcome(
                        added = added,
                        alreadyThere = alreadyThere,
                        skipped = _state.value.matches.count { match -> match.skipped },
                        failed = failed,
                    ),
                )
            }
        }
    }

    /** Back to a clean slate for another batch. */
    fun reset() {
        _state.value = ImportUiState()
    }

    private fun updateMatch(index: Int, transform: (ImportMatch) -> ImportMatch) {
        _state.update { current ->
            if (index !in current.matches.indices) {
                current
            } else {
                current.copy(
                    matches = current.matches.mapIndexed { i, match ->
                        if (i == index) transform(match) else match
                    },
                )
            }
        }
    }

    private companion object {
        const val CANDIDATE_LIMIT = 5
    }
}
