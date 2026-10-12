package com.ludicindex.app.ui.detail

import com.ludicindex.app.MainDispatcherRule
import com.ludicindex.app.WITCHER_DETAIL_JSON
import com.ludicindex.app.createTestDatabase
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import com.ludicindex.app.data.repository.GameDetailRepository
import com.ludicindex.app.data.repository.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var detailRepository: GameDetailRepository
    private lateinit var wishlistRepository: WishlistRepository
    private val api = FakeIgdbApi()
    private val auth = FakeIgdbAuth()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        db = createTestDatabase()
        detailRepository = GameDetailRepository(
            client = IgdbClient(api, auth, json, RateLimiter(minIntervalMillis = 0)),
            cacheDao = db.gameCacheDao(),
            json = json,
        )
        wishlistRepository = WishlistRepository(db.wishlistDao(), db.tagDao())
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel(igdbId: Long = 1942L) = GameDetailViewModel(
        igdbId = igdbId,
        detailRepository = detailRepository,
        wishlistRepository = wishlistRepository,
    )

    @Test
    fun loadsTheDetailFromTheNetwork() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val content = viewModel.state.value.content
        assertTrue(content is DetailContentState.Content)
        assertEquals("The Witcher 3: Wild Hunt", (content as DetailContentState.Content).detail.name)
        assertFalse(content.fromCache)
    }

    @Test
    fun fallsBackToCacheWhenTheNetworkFails() = runTest(mainDispatcherRule.testDispatcher) {
        // Seed the cache first.
        api.enqueueJson(WITCHER_DETAIL_JSON)
        detailRepository.fetch(1942)

        api.enqueueIoFailure()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val content = viewModel.state.value.content as DetailContentState.Content
        assertEquals("The Witcher 3: Wild Hunt", content.detail.name)
        assertTrue(content.fromCache)
        assertTrue(content.staleMessage!!.contains("网络"))
    }

    @Test
    fun showsAnErrorWhenThereIsNoCacheAndTheNetworkFails() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueIoFailure()
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.state.value.content is DetailContentState.Error)
    }

    @Test
    fun flagsMissingCredentials() = runTest(mainDispatcherRule.testDispatcher) {
        auth.credentials = null
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val content = viewModel.state.value.content
        assertTrue(content is DetailContentState.Error)
        assertTrue((content as DetailContentState.Error).missingCredentials)
    }

    @Test
    fun addingAndRemovingTogglesTheWishlistState() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        assertFalse(viewModel.state.value.inWishlist)

        viewModel.addToWishlist()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.inWishlist)
        assertEquals("已加入心愿单", viewModel.state.value.message)

        viewModel.removeFromWishlist()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.inWishlist)
    }

    @Test
    fun addingTwiceReportsThatItWasAlreadyThere() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.addToWishlist()
        advanceUntilIdle()
        viewModel.consumeMessage()
        viewModel.addToWishlist()
        advanceUntilIdle()

        assertEquals("已经在心愿单里了", viewModel.state.value.message)
    }
}
