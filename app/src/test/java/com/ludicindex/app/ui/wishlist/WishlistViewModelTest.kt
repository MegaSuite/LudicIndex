package com.ludicindex.app.ui.wishlist

import com.ludicindex.app.MainDispatcherRule
import com.ludicindex.app.createTestDatabase
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.WishlistSort
import com.ludicindex.app.domain.model.EntryStatus
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class WishlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var repository: WishlistRepository

    @Before
    fun setUp() {
        db = createTestDatabase()
        repository = WishlistRepository(db.wishlistDao(), db.tagDao())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seed() {
        repository.addGame(Game(igdbId = 1, name = "Hades", releaseDate = 1_600_000_000), listOf("PC"))
        repository.addGame(Game(igdbId = 2, name = "Celeste", releaseDate = 1_500_000_000), listOf("Switch"))
        repository.addGame(Game(igdbId = 3, name = "Hollow Knight", releaseDate = 1_400_000_000), listOf("PC", "Switch"))
    }

    @Test
    fun showsEveryEntryWithItsTags() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.totalCount)
        assertEquals(3, state.visibleEntries.size)
        assertFalse(state.isLoading)
        assertTrue(state.hasAnyEntry)
    }

    @Test
    fun filtersByQuery() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onQueryChange("hollow")
        advanceUntilIdle()

        assertEquals(listOf("Hollow Knight"), viewModel.state.value.visibleEntries.map { it.name })
        assertTrue(viewModel.state.value.isFiltered)
    }

    @Test
    fun filtersByTagRequiringAllSelected() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val pc = viewModel.state.value.allTags.first { it.name == "PC" }
        val switch = viewModel.state.value.allTags.first { it.name == "Switch" }

        viewModel.toggleTagFilter(pc.id)
        advanceUntilIdle()
        assertEquals(
            listOf("Hades", "Hollow Knight"),
            viewModel.state.value.visibleEntries.map { it.name }.sorted(),
        )

        viewModel.toggleTagFilter(switch.id)
        advanceUntilIdle()
        // Only Hollow Knight carries both tags.
        assertEquals(listOf("Hollow Knight"), viewModel.state.value.visibleEntries.map { it.name })
    }

    @Test
    fun sortsByName() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setSort(WishlistSort.NAME_ASC)
        advanceUntilIdle()

        assertEquals(
            listOf("Celeste", "Hades", "Hollow Knight"),
            viewModel.state.value.visibleEntries.map { it.name },
        )
    }

    @Test
    fun clearFiltersResetsBothQueryAndTags() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onQueryChange("hades")
        viewModel.toggleTagFilter(viewModel.state.value.allTags.first().id)
        advanceUntilIdle()

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.query)
        assertTrue(state.selectedTagIds.isEmpty())
        assertEquals(3, state.visibleEntries.size)
    }

    @Test
    fun removesAnEntry() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.remove(1)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.totalCount)
        assertNull(viewModel.state.value.visibleEntries.firstOrNull { it.igdbId == 1L })
    }

    @Test
    fun changesStatus() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setStatus(1, EntryStatus.PLAYING)
        advanceUntilIdle()

        assertEquals(
            EntryStatus.PLAYING,
            viewModel.state.value.visibleEntries.first { it.igdbId == 1L }.status,
        )
    }

    @Test
    fun setTagsReplacesTheSelectionAndCreatesCustomTags() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.setTags(1, listOf("PS5", "等打折"))
        advanceUntilIdle()

        val entry = viewModel.state.value.visibleEntries.first { it.igdbId == 1L }
        assertEquals(listOf("PS5", "等打折"), entry.tags.map { it.name }.sorted())
        assertTrue(viewModel.state.value.allTags.any { it.name == "等打折" })
    }

    @Test
    fun createTagIsIdempotent() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()
        val before = viewModel.state.value.allTags.size

        viewModel.createTag("PS5", 0xFF000000.toInt())
        viewModel.createTag("PS5", 0xFF000000.toInt())
        advanceUntilIdle()

        assertEquals(before + 1, viewModel.state.value.allTags.size)
    }

    @Test
    fun deletingATagAlsoDropsItFromTheActiveFilter() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        val pc = viewModel.state.value.allTags.first { it.name == "PC" }
        viewModel.toggleTagFilter(pc.id)
        advanceUntilIdle()

        viewModel.deleteTag(pc.id)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.selectedTagIds.isEmpty())
        assertFalse(state.allTags.any { it.name == "PC" })
        // The tag is gone from the entries too, but the entries survive.
        assertEquals(3, state.visibleEntries.size)
        assertTrue(state.visibleEntries.all { entry -> entry.tags.none { it.name == "PC" } })
    }

    @Test
    fun openingAndClosingTheEditorTracksTheEntry() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.openEditor(2)
        advanceUntilIdle()
        assertEquals("Celeste", viewModel.state.value.editing?.name)

        viewModel.closeEditor()
        advanceUntilIdle()
        assertNull(viewModel.state.value.editing)
    }

    @Test
    fun removingTheEntryBeingEditedClosesTheEditor() = runTest(mainDispatcherRule.testDispatcher) {
        seed()
        val viewModel = WishlistViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.openEditor(2)
        advanceUntilIdle()
        viewModel.remove(2)
        advanceUntilIdle()

        assertNull(viewModel.state.value.editing)
    }
}
