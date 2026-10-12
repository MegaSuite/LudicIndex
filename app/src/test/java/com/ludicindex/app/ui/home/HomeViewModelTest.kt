package com.ludicindex.app.ui.home

import com.ludicindex.app.MainDispatcherRule
import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import com.ludicindex.app.data.repository.GameRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = FakeIgdbApi()
    private val auth = FakeIgdbAuth()

    private fun repository() = GameRepository(
        IgdbClient(
            api = api,
            auth = auth,
            json = Json { ignoreUnknownKeys = true },
            rateLimiter = RateLimiter(minIntervalMillis = 0),
        ),
    )

    @Test
    fun showsPopularGamesOnStart() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("""[{"game_id":2,"value":0.9,"popularity_type":1}]""")
        api.enqueueJson("""[{"id":2,"name":"Hades"}]""")

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val popular = viewModel.state.value.popular
        assertTrue(popular is HomeListState.Content)
        assertEquals(listOf("Hades"), (popular as HomeListState.Content).games.map { it.name })
        assertNull(viewModel.state.value.search)
    }

    @Test
    fun flagsMissingCredentialsSoTheUiCanOfferSettings() = runTest(mainDispatcherRule.testDispatcher) {
        auth.credentials = null

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val popular = viewModel.state.value.popular
        assertTrue(popular is HomeListState.Error)
        assertTrue((popular as HomeListState.Error).missingCredentials)
    }

    @Test
    fun surfacesIgbdErrorMessages() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueHttpError(429)
        api.enqueueHttpError(429)

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val popular = viewModel.state.value.popular
        assertTrue(popular is HomeListState.Error)
        assertTrue((popular as HomeListState.Error).message.contains("限流"))
    }

    @Test
    fun searchesOnlyOnceAfterTheDebounceWindow() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]")
        api.enqueueJson("[]")

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 300)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        val callsBeforeSearch = api.calls.size

        api.enqueueJson("""[{"id":1,"name":"Hollow Knight"}]""")
        viewModel.onQueryChange("h")
        viewModel.onQueryChange("ho")
        viewModel.onQueryChange("hollow")
        advanceTimeBy(400)
        runCurrent()

        assertEquals(callsBeforeSearch + 1, api.calls.size)
        val search = viewModel.state.value.search
        assertTrue(search is HomeListState.Content)
        assertEquals(listOf("Hollow Knight"), (search as HomeListState.Content).games.map { it.name })
    }

    @Test
    fun clearingTheQueryDropsSearchResults() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]")
        api.enqueueJson("[]")

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        api.enqueueJson("""[{"id":1,"name":"Celeste"}]""")
        viewModel.onQueryChange("celeste")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.search is HomeListState.Content)

        viewModel.clearQuery()
        advanceUntilIdle()

        assertNull(viewModel.state.value.search)
        assertTrue(!viewModel.state.value.isSearching)
    }

    @Test
    fun emptySearchResultsBecomeTheEmptyState() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]")
        api.enqueueJson("[]")

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        api.enqueueJson("[]")
        viewModel.onQueryChange("zzzz")
        advanceUntilIdle()

        assertEquals(HomeListState.Empty, viewModel.state.value.search)
    }

    @Test
    fun refreshReloadsTheBoard() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("""[{"game_id":1,"value":0.4,"popularity_type":1}]""")
        api.enqueueJson("""[{"id":1,"name":"Celeste"}]""")

        val viewModel = HomeViewModel(repository(), searchDebounceMillis = 0)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        api.enqueueJson("""[{"game_id":2,"value":0.9,"popularity_type":1}]""")
        api.enqueueJson("""[{"id":2,"name":"Hades"}]""")
        viewModel.refresh()
        advanceUntilIdle()

        val popular = viewModel.state.value.popular as HomeListState.Content
        assertEquals(listOf("Hades"), popular.games.map { it.name })
        assertTrue(!viewModel.state.value.isRefreshing)
    }
}
