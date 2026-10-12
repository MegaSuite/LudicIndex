package com.ludicindex.app.ui.transfer

import com.ludicindex.app.MainDispatcherRule
import com.ludicindex.app.createTestDatabase
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class ImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var wishlistRepository: WishlistRepository
    private val api = FakeIgdbApi()

    private val gameRepository = GameRepository(
        IgdbClient(
            api = api,
            auth = FakeIgdbAuth(),
            json = Json { ignoreUnknownKeys = true },
            rateLimiter = RateLimiter(minIntervalMillis = 0),
        ),
    )

    @Before
    fun setUp() {
        db = createTestDatabase()
        wishlistRepository = WishlistRepository(db.wishlistDao(), db.tagDao())
    }

    @After
    fun tearDown() = db.close()

    private fun viewModel() = ImportViewModel(gameRepository, wishlistRepository)

    private fun searchResult(vararg games: String) =
        games.joinToString(prefix = "[", postfix = "]", separator = ",")

    @Test
    fun refusesToMatchWhenNothingWasPasted() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange("   \n  ")
        viewModel.startMatching()

        assertEquals(ImportPhase.INPUT, viewModel.state.value.phase)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun matchesEachLineAndPicksTheTopResult() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight\nCeleste")
        viewModel.startMatching()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ImportPhase.REVIEW, state.phase)
        assertEquals(2, state.totalCount)
        assertEquals(2, state.selectedCount)
        assertEquals(listOf("Hollow Knight", "Celeste"), state.matches.map { it.selected?.name })
        assertEquals(0, state.missingCount)
    }

    @Test
    fun marksLinesThatGotNoResult() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]")
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Zzzz Not A Game\nCeleste")
        viewModel.startMatching()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.missingCount)
        assertTrue(state.matches.first().hasNoMatch)
        assertEquals(1, state.selectedCount)
    }

    @Test
    fun offersAlternativesAndLetsTheUserPickOne() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(
            searchResult(
                """{"id":1,"name":"Hollow Knight"}""",
                """{"id":9,"name":"Hollow Knight: Silksong"}""",
            ),
        )

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight")
        viewModel.startMatching()
        advanceUntilIdle()

        val candidates = viewModel.state.value.matches.single().candidates
        assertEquals(2, candidates.size)
        // Best match first by default.
        assertEquals("Hollow Knight", viewModel.state.value.matches.single().selected?.name)

        viewModel.choose(0, candidates[1])
        advanceUntilIdle()

        assertEquals("Hollow Knight: Silksong", viewModel.state.value.matches.single().selected?.name)
    }

    @Test
    fun editingATitleAndSearchingAgainReplacesTheMatch() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]") // the original, mistyped title matches nothing
        api.enqueueJson(searchResult("""{"id":5,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Celest")
        viewModel.startMatching()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.matches.single().hasNoMatch)

        viewModel.onTitleChange(0, "Celeste")
        viewModel.research(0)
        advanceUntilIdle()

        val match = viewModel.state.value.matches.single()
        assertEquals("Celeste", match.title)
        assertEquals("Celeste", match.selected?.name)
        assertFalse(match.hasNoMatch)
        assertFalse(match.searching)
    }

    @Test
    fun researchingOneRowLeavesTheOtherRowsAlone() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))
        api.enqueueJson("[]")
        api.enqueueJson(searchResult("""{"id":2,"name":"Zelda II"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight\nZellda")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.onTitleChange(1, "Zelda II")
        viewModel.research(1)
        advanceUntilIdle()

        val matches = viewModel.state.value.matches
        assertEquals("Hollow Knight", matches[0].selected?.name)
        assertEquals("Zelda II", matches[1].selected?.name)
        assertEquals(2, viewModel.state.value.selectedCount)
    }

    @Test
    fun researchingClearsTheStaleMatchBeforeTheNewOneArrives() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Wrong Game"}"""))
        api.enqueueJson(searchResult("""{"id":2,"name":"Right Game"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("game")
        viewModel.startMatching()
        advanceUntilIdle()
        assertEquals("Wrong Game", viewModel.state.value.matches.single().selected?.name)

        viewModel.onTitleChange(0, "right game")
        viewModel.research(0)
        advanceUntilIdle()

        assertEquals("Right Game", viewModel.state.value.matches.single().selected?.name)
    }

    @Test
    fun skipAllWithoutMatchTicksOnlyTheUnmatchedRows() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson("[]")
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Zzzz Not A Game\nCeleste")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.skipAllWithoutMatch()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.matches.first().skipped)
        assertFalse(viewModel.state.value.matches.last().skipped)
        assertEquals(1, viewModel.state.value.selectedCount)
    }

    @Test
    fun committingAddsTheSelectedGames() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight\nCeleste")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.commit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ImportPhase.DONE, state.phase)
        assertEquals(2, state.outcome?.added)
        assertEquals(0, state.outcome?.alreadyThere)
        assertEquals(
            listOf("Celeste", "Hollow Knight"),
            wishlistRepository.observeEntries().first().map { it.name }.sorted(),
        )
    }

    @Test
    fun committingCountsEntriesThatWereAlreadyWishlisted() = runTest(mainDispatcherRule.testDispatcher) {
        wishlistRepository.addGame(Game(igdbId = 2, name = "Celeste"))
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Celeste")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.commit()
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.outcome?.added)
        assertEquals(1, viewModel.state.value.outcome?.alreadyThere)
        assertEquals(1, wishlistRepository.observeEntries().first().size)
    }

    @Test
    fun skippedRowsAreNotAddedButStillCounted() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))
        api.enqueueJson(searchResult("""{"id":2,"name":"Celeste"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight\nCeleste")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.setSkipped(1, true)
        viewModel.commit()
        advanceUntilIdle()

        assertEquals(ImportPhase.DONE, viewModel.state.value.phase)
        assertEquals(1, viewModel.state.value.outcome?.added)
        assertEquals(1, viewModel.state.value.outcome?.skipped)
        assertEquals(listOf("Hollow Knight"), wishlistRepository.observeEntries().first().map { it.name })
    }

    @Test
    fun committingWithEverythingSkippedReportsAnError() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))

        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.setSkipped(0, true)
        viewModel.commit()
        advanceUntilIdle()

        assertEquals(ImportPhase.REVIEW, viewModel.state.value.phase)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun resetReturnsToTheInputStep() = runTest(mainDispatcherRule.testDispatcher) {
        api.enqueueJson(searchResult("""{"id":1,"name":"Hollow Knight"}"""))
        val viewModel = viewModel()
        viewModel.onTextChange("Hollow Knight")
        viewModel.startMatching()
        advanceUntilIdle()

        viewModel.reset()

        val state = viewModel.state.value
        assertEquals(ImportPhase.INPUT, state.phase)
        assertEquals("", state.rawText)
        assertTrue(state.matches.isEmpty())
        assertNull(state.outcome)
    }
}
