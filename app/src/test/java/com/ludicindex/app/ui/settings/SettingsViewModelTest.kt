package com.ludicindex.app.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ludicindex.app.MainDispatcherRule
import com.ludicindex.app.createTestDatabase
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.prefs.ImageQuality
import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.prefs.ThemeMode
import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import com.ludicindex.app.data.repository.GameRepository
import com.ludicindex.app.data.repository.WishlistRepository
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Settings results are dialogs the user has to confirm, so assertions use `runCurrent()` to drive
 * the ViewModel without fast-forwarding virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var wishlistRepository: WishlistRepository
    private lateinit var settingsStore: SettingsStore

    private val api = FakeIgdbApi()
    private val auth = FakeIgdbAuth()

    @Before
    fun setUp() {
        db = createTestDatabase()
        wishlistRepository = WishlistRepository(db.wishlistDao(), db.tagDao())
    }

    @After
    fun tearDown() = db.close()

    /**
     * The DataStore runs on the test dispatcher so preference writes land deterministically in
     * virtual time instead of on a real IO thread.
     */
    private fun TestScope.newViewModel(): SettingsViewModel {
        settingsStore = SettingsStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + SupervisorJob()),
            ) { File(temporaryFolder.root, "settings.preferences_pb") },
        )
        return SettingsViewModel(
            settingsStore = settingsStore,
            gameRepository = GameRepository(
                IgdbClient(
                    api = api,
                    auth = auth,
                    json = Json { ignoreUnknownKeys = true },
                    rateLimiter = RateLimiter(minIntervalMillis = 0),
                ),
            ),
            wishlistRepository = wishlistRepository,
        )
    }

    /** Keeps the stateIn-backed flows hot so `.value` is meaningful. */
    private fun TestScope.collectAll(viewModel: SettingsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.themeMode.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.dynamicColor.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.imageQuality.collect {} }
    }

    @Test
    fun exportThenImportRestoresTheWishlist() = runTest(mainDispatcherRule.testDispatcher) {
        wishlistRepository.addGame(Game(igdbId = 1942, name = "The Witcher 3"), listOf("PC"))
        val viewModel = newViewModel()
        collectAll(viewModel)

        val json = viewModel.buildBackupJson()
        wishlistRepository.clearWishlist()
        assertTrue(wishlistRepository.observeEntries().first().isEmpty())

        viewModel.importBackupJson(json)
        runCurrent()

        val entries = wishlistRepository.observeEntries().first()
        assertEquals(1, entries.size)
        assertEquals(listOf("PC"), entries.single().tags.map { it.name })
        val result = viewModel.state.value.result
        assertTrue(result is TestResult.Info)
        assertTrue((result as TestResult.Info).message.contains("新增 1"))
    }

    @Test
    fun importingTheSameBackupTwiceUpdatesInsteadOfDuplicating() = runTest(mainDispatcherRule.testDispatcher) {
        wishlistRepository.addGame(Game(igdbId = 1942, name = "The Witcher 3"))
        val viewModel = newViewModel()
        collectAll(viewModel)
        val json = viewModel.buildBackupJson()

        viewModel.importBackupJson(json)
        runCurrent()
        viewModel.dismissResult()

        viewModel.importBackupJson(json)
        runCurrent()

        assertEquals(1, wishlistRepository.observeEntries().first().size)
        val result = viewModel.state.value.result as TestResult.Info
        assertTrue(result.message.contains("新增 0"))
        assertTrue(result.message.contains("更新 1"))
    }

    @Test
    fun importingGarbageReportsAFailureInsteadOfCrashing() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = newViewModel()
        collectAll(viewModel)

        viewModel.importBackupJson("this is not json")
        runCurrent()

        val result = viewModel.state.value.result
        assertTrue(result is TestResult.Failure)
        assertTrue((result as TestResult.Failure).message.contains("导入失败"))
    }

    @Test
    fun resultsStayOnScreenUntilTheUserConfirms() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = newViewModel()
        collectAll(viewModel)

        viewModel.clearWishlist()
        runCurrent()
        assertNotNull(viewModel.state.value.result)

        // No auto-hide any more: the dialog waits for an explicit "确认".
        advanceTimeBy(60_000)
        runCurrent()
        assertNotNull(viewModel.state.value.result)

        viewModel.dismissResult()
        runCurrent()
        assertNull(viewModel.state.value.result)
    }

    @Test
    fun failuresAlsoWaitForConfirmation() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = newViewModel()
        collectAll(viewModel)

        viewModel.reportImportReadFailure()
        runCurrent()

        advanceTimeBy(60_000)
        runCurrent()
        assertNotNull(viewModel.state.value.result)

        viewModel.dismissResult()
        runCurrent()
        assertNull(viewModel.state.value.result)
    }

    @Test
    fun themeAndImageQualityChoicesReachTheStore() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = newViewModel()
        collectAll(viewModel)

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setDynamicColor(false)
        viewModel.setImageQuality(ImageQuality.HIGH)
        runCurrent()

        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        assertFalse(viewModel.dynamicColor.value)
        assertEquals(ImageQuality.HIGH, viewModel.imageQuality.value)
        assertEquals(ThemeMode.DARK, settingsStore.themeMode.first())
    }

    @Test
    fun testingTheConnectionWithoutCredentialsKeepsTheUserOnThePage() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = newViewModel()
        collectAll(viewModel)

        viewModel.testConnection()
        runCurrent()

        val result = viewModel.state.value.result
        assertTrue(result is TestResult.Failure)
        assertEquals(0, api.calls.size)
    }
}
