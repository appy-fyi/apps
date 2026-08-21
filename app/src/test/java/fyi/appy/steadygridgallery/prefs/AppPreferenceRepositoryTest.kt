package fyi.appy.steadygridgallery.prefs

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.appy.steadygridgallery.data.db.AppDatabase
import fyi.appy.steadygridgallery.data.db.entity.AppPreferenceEntity
import fyi.appy.steadygridgallery.data.db.entity.PreferenceKeys
import fyi.appy.steadygridgallery.data.db.entity.SortMode
import fyi.appy.steadygridgallery.data.prefs.SettingsRepository
import fyi.appy.steadygridgallery.data.prefs.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Covers the "many of the options stop working or get reset for no reason" unit scenario: settings
 * written to Room must survive recreating the repository object against the same database instance,
 * proving persistence doesn't depend on any in-memory Compose state.
 *
 * Pinned to sdk = 34: Robolectric 4.13's newest supported simulated platform is API 34, while the
 * app's real targetSdk stays 35 per the spec -- this only controls which Android runtime
 * Robolectric simulates for this JVM-only test, not the app's manifest.
 *
 * Uses runBlocking (real dispatchers) rather than runTest/TestScope: Room's Flow observers do
 * their query re-execution on Room's own query executor thread, not on a virtual-time test
 * dispatcher, so advancing a TestScheduler alone never observes the emission.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPreferenceRepositoryTest {

    @Test
    fun `settings persist across repository recreation on the same database`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = database.appPreferenceDao()
        dao.upsertPreference(AppPreferenceEntity(PreferenceKeys.THEME_MODE, ThemeMode.DARK.name, Instant.now()))
        dao.upsertPreference(AppPreferenceEntity(PreferenceKeys.GRID_CELL_DP, "160", Instant.now()))

        // stateIn(Eagerly) launches a never-completing collector in this scope, so it must be a
        // scope independent of the test's runBlocking job -- otherwise runBlocking never returns.
        val repositoryScope = CoroutineScope(Dispatchers.Default + Job())
        try {
            val repository = SettingsRepository(dao = dao, externalScope = repositoryScope)
            val settings = withTimeout(5_000) {
                repository.settings.first { it.themeMode == ThemeMode.DARK }
            }

            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(160, settings.gridCellDp)
            assertEquals(SortMode.DATE_DESC, settings.defaultSort)
        } finally {
            repositoryScope.cancel()
            database.close()
        }
    }
}
