package com.ludicindex.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludicindex.app.data.remote.IgdbException
import com.ludicindex.app.data.repository.GameDetailRepository
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.model.GameDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailContentState {
    data object Loading : DetailContentState
    data class Content(
        val detail: GameDetail,
        val fromCache: Boolean,
        /** Set when the live refresh failed but cached data was good enough to show. */
        val staleMessage: String? = null,
    ) : DetailContentState

    data class Error(val message: String, val missingCredentials: Boolean = false) : DetailContentState
}

data class GameDetailUiState(
    val content: DetailContentState = DetailContentState.Loading,
    val inWishlist: Boolean = false,
    val isRefreshing: Boolean = false,
    val message: String? = null,
) {
    val detail: GameDetail? get() = (content as? DetailContentState.Content)?.detail
}

class GameDetailViewModel(
    private val igdbId: Long,
    private val detailRepository: GameDetailRepository,
    private val wishlistRepository: WishlistRepository,
) : ViewModel() {

    private val content = MutableStateFlow<DetailContentState>(DetailContentState.Loading)
    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val inWishlist = wishlistRepository.observeIsInWishlist(igdbId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    val state: StateFlow<GameDetailUiState> = combine(
        content,
        inWishlist,
        refreshing,
        message,
    ) { content, inWishlist, refreshing, message ->
        GameDetailUiState(
            content = content,
            inWishlist = inWishlist,
            isRefreshing = refreshing,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), GameDetailUiState())

    init {
        load()
    }

    /** Cached payload first (instant, works offline), then the live refresh. */
    fun load() {
        viewModelScope.launch {
            val cached = runCatching { detailRepository.cached(igdbId) }.getOrNull()
            if (cached != null) {
                content.value = DetailContentState.Content(cached, fromCache = true)
            }

            try {
                val fresh = detailRepository.fetch(igdbId)
                content.value = if (fresh != null) {
                    DetailContentState.Content(fresh, fromCache = false)
                } else {
                    DetailContentState.Error(NOT_FOUND)
                }
            } catch (e: IgdbException) {
                content.value = fallbackToCache(e.message ?: UNKNOWN, e is IgdbException.MissingCredentials)
            } catch (e: Exception) {
                content.value = fallbackToCache(e.message ?: UNKNOWN, missingCredentials = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            try {
                val fresh = detailRepository.fetch(igdbId)
                if (fresh != null) {
                    content.value = DetailContentState.Content(fresh, fromCache = false)
                    message.value = "已更新"
                }
            } catch (e: Exception) {
                message.value = e.message ?: UNKNOWN
            } finally {
                refreshing.value = false
            }
        }
    }

    fun retry() {
        content.value = DetailContentState.Loading
        load()
    }

    fun addToWishlist() {
        val detail = (content.value as? DetailContentState.Content)?.detail ?: return
        viewModelScope.launch {
            val added = wishlistRepository.addGame(detail.toGame())
            message.value = if (added) "已加入心愿单" else "已经在心愿单里了"
        }
    }

    fun removeFromWishlist() {
        viewModelScope.launch {
            wishlistRepository.removeByIgdbId(igdbId)
            message.value = "已从心愿单移除"
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    private fun fallbackToCache(errorMessage: String, missingCredentials: Boolean): DetailContentState {
        val cached = (content.value as? DetailContentState.Content)?.detail
        return if (cached != null) {
            DetailContentState.Content(cached, fromCache = true, staleMessage = errorMessage)
        } else {
            DetailContentState.Error(errorMessage, missingCredentials)
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val NOT_FOUND = "IGDB 上没有这条游戏记录。"
        const val UNKNOWN = "加载失败，请稍后重试。"
    }
}
