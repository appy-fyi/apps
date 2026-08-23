package fyi.appy.taponceremote.giladkutiel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fyi.appy.taponceremote.giladkutiel.data.db.AppDatabase
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.discovery.DiscoveredDevice
import fyi.appy.taponceremote.giladkutiel.data.discovery.DiscoveryRepository
import fyi.appy.taponceremote.giladkutiel.ui.screens.discovery.DeviceDiscoveryViewModel
import fyi.appy.taponceremote.giladkutiel.ui.screens.discovery.DiscoveryScreenState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** test_plan scenario "baseline parity": empty scan, refresh, discover, select, navigate. */
@RunWith(AndroidJUnit4::class)
class DeviceDiscoveryViewModelInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private class FakeDiscoveryRepository : DiscoveryRepository {
        var nextResult: List<DiscoveredDevice> = emptyList()
        override suspend fun scan(): List<DiscoveredDevice> = nextResult
        override suspend fun probeManualIp(ip: String, port: Int): DiscoveredDevice? = null
    }

    @Test
    fun emptyScan_thenRefreshFindsDevice_thenSelectingRoutesToRemote() {
        val fakeRepo = FakeDiscoveryRepository()
        val viewModel = DeviceDiscoveryViewModel(fakeRepo, db.savedDeviceDao())

        waitUntil { viewModel.uiState.value.screenState == DiscoveryScreenState.EMPTY_NO_SAVED_OR_DISCOVERED }

        val livingRoomRoku = DiscoveredDevice(
            key = "roku:127.0.0.1:8060",
            displayName = "Living Room Roku",
            protocol = RemoteProtocol.ROKU_ECP,
            ipAddress = "127.0.0.1",
            port = 8060,
        )
        fakeRepo.nextResult = listOf(livingRoomRoku)
        viewModel.scan()

        waitUntil { viewModel.uiState.value.discovered.any { it.displayName == "Living Room Roku" } }

        runBlocking {
            viewModel.connectDiscovered(livingRoomRoku)
        }
        waitUntil { runBlocking { db.savedDeviceDao().getLastUsed() != null } }

        val lastUsed = runBlocking { db.savedDeviceDao().getLastUsed() }
        assertEquals("Living Room Roku", lastUsed?.displayName)
        assertTrue(lastUsed?.lastUsed == true)
    }

    private fun waitUntil(timeoutMillis: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        assertTrue("condition not met within timeout", condition())
    }
}
