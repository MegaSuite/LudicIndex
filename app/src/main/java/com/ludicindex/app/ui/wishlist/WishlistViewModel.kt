package com.ludicindex.app.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.WishlistFilter
import com.ludicindex.app.domain.WishlistSort
import com.ludicindex.app.domain.filterAndSort
import com.ludicindex.app.domain.model.EntryStatus
import com.ludicindex.app.domain.model.Tag
import com.ludicindex.app.domain.model.WishlistEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WishlistUiState(
    val visibleEntries: List<WishlistEntry> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val sort: WishlistSort = WishlistSort.ADDED_DESC,
    val editing: WishlistEntry? = null,
    val isLoading: Boolean = true,
) {
    val hasAnyEntry: Boolean get() = totalCount > 0
    val isFiltered: Boolean get() = query.isNotBlank() || selectedTagIds.isNotEmpty()
}

/** UI-only filter state, kept apart from the DB flows so `combine` stays a 3-way join. */
private data class FilterState(
    val query: String = "",
    val tagIds: Set<Long> = emptySet(),
    val sort: WishlistSort = WishlistSort.ADDED_DESC,
    val editingIgdbId: Long? = null,
)

class WishlistViewModel(
    private val repository: WishlistRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())

    val state: StateFlow<WishlistUiState> = combine(
        repository.observeEntries(),
        repository.observeTags(),
        filterState,
    ) { entries, tags, filter ->
        WishlistUiState(
            visibleEntries = entries.filterAndSort(
                WishlistFilter(
                    query = filter.query,
                    tagIds = filter.tagIds,
                    matchAllTags = true,
                    sort = filter.sort,
                ),
            ),
            allTags = tags,
            totalCount = entries.size,
            query = filter.query,
            selectedTagIds = filter.tagIds,
            sort = filter.sort,
            editing = entries.firstOrNull { it.igdbId == filter.editingIgdbId },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT),
        initialValue = WishlistUiState(),
    )

    fun onQueryChange(value: String) {
        filterState.value = filterState.value.copy(query = value)
    }

    fun toggleTagFilter(tagId: Long) {
        val current = filterState.value.tagIds
        filterState.value = filterState.value.copy(
            tagIds = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun clearFilters() {
        filterState.value = filterState.value.copy(query = "", tagIds = emptySet())
    }

    fun setSort(sort: WishlistSort) {
        filterState.value = filterState.value.copy(sort = sort)
    }

    fun openEditor(igdbId: Long) {
        filterState.value = filterState.value.copy(editingIgdbId = igdbId)
    }

    fun closeEditor() {
        filterState.value = filterState.value.copy(editingIgdbId = null)
    }

    fun remove(igdbId: Long) {
        viewModelScope.launch {
            repository.removeByIgdbId(igdbId)
            if (filterState.value.editingIgdbId == igdbId) closeEditor()
        }
    }

    fun setStatus(igdbId: Long, status: EntryStatus) {
        viewModelScope.launch { repository.setStatus(igdbId, status) }
    }

    fun setTags(igdbId: Long, tagNames: List<String>) {
        viewModelScope.launch { repository.setTags(igdbId, tagNames) }
    }

    fun createTag(name: String, colorArgb: Int) {
        viewModelScope.launch { repository.createTag(name, colorArgb) }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
            filterState.value = filterState.value.copy(tagIds = filterState.value.tagIds - tagId)
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
