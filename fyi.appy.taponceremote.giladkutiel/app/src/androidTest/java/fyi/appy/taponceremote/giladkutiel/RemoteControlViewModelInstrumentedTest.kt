package fyi.appy.taponceremote.giladkutiel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fyi.appy.taponceremote.giladkutiel.data.db.AppDatabase
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import fyi.appy.taponceremote.giladkutiel.ui.screens.remote.ConnectionPhase
import fyi.appy.taponceremote.giladkutiel.ui.screens.remote.RemoteControlViewModel
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** test_plan scenario "Basic functions paywalled": commands dispatch to the real device with purchase state not_purchased. */
@RunWith(AndroidJUnit4::class)
class RemoteControlViewModelInstrumentedTest {
    private lateinit var server: MockWebServer
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repeat(20) { server.enqueue(MockResponse().setResponseCode(200).setBody("<device-info></device-info>")) }
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        db.close()
    }

    @Test
    fun tappingRemoteButtons_dispatchesEcpKeypressesWithoutOpeningPurchaseFlow() {
        val port = server.port
        val deviceId = kotlinx.coroutines.runBlocking {
            db.savedDeviceDao().insert(
                SavedDevice(
                    displayName = "Living Room Roku",
                    protocol = RemoteProtocol.ROKU_ECP,
                    ipAddress = "127.0.0.1",
                    port = port,
                    lastSeenAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = RemoteControlViewModel(deviceId, context, db.savedDeviceDao())

        waitUntil { viewModel.uiState.value.phase == ConnectionPhase.CONNECTED }

        val commands = listOf(
            RemoteCommand.VolumeUp,
            RemoteCommand.VolumeDown,
            RemoteCommand.VolumeMute,
            RemoteCommand.ChannelUp,
            RemoteCommand.ChannelDown,
            RemoteCommand.Power,
        )
        commands.forEach { command ->
            val latch = java.util.concurrent.CountDownLatch(1)
            viewModel.sendCommand(command) { latch.countDown() }
            assertTrue(latch.await(3, TimeUnit.SECONDS))
        }

        // Drain the initial device-info probe request before the six keypress requests.
        server.takeRequest()
        val paths = (0 until commands.size).map { server.takeRequest().path }
        assertEquals(
            listOf(
                "/keypress/VolumeUp",
                "/keypress/VolumeDown",
                "/keypress/VolumeMute",
                "/keypress/ChannelUp",
                "/keypress/ChannelDown",
                "/keypress/Power",
            ),
            paths,
        )
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
