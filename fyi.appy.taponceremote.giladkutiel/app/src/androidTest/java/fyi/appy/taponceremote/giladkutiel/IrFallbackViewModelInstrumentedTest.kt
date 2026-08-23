package fyi.appy.taponceremote.giladkutiel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fyi.appy.taponceremote.giladkutiel.data.db.AppDatabase
import fyi.appy.taponceremote.giladkutiel.data.ir.IrProfileRepository
import fyi.appy.taponceremote.giladkutiel.data.ir.IrTransmitter
import fyi.appy.taponceremote.giladkutiel.ui.screens.ir.IrFallbackViewModel
import fyi.appy.taponceremote.giladkutiel.ui.screens.ir.IrState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** test_plan scenario "baseline parity": IR hardware presence gates transmit controls. */
@RunWith(AndroidJUnit4::class)
class IrFallbackViewModelInstrumentedTest {
    private lateinit var db: AppDatabase
    private lateinit var irProfileRepository: IrProfileRepository

    private class FakeIrTransmitter(private val hasEmitter: Boolean) : IrTransmitter {
        var transmitCallCount = 0
        var lastPattern: IntArray? = null
        override fun hasIrEmitter(): Boolean = hasEmitter
        override fun transmit(frequencyHz: Int, pattern: IntArray) {
            transmitCallCount++
            lastPattern = pattern
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        irProfileRepository = IrProfileRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun noIrHardware_disablesTransmit() {
        val fakeTransmitter = FakeIrTransmitter(hasEmitter = false)
        val viewModel = IrFallbackViewModel(fakeTransmitter, irProfileRepository, db.savedDeviceDao())

        waitUntil { viewModel.state.value is IrState.NoHardware }
        assertTrue(viewModel.state.value is IrState.NoHardware)
    }

    @Test
    fun hardwarePresent_selectingProfileAndTransmitting_callsFakeExactlyOnce() {
        val fakeTransmitter = FakeIrTransmitter(hasEmitter = true)
        val viewModel = IrFallbackViewModel(fakeTransmitter, irProfileRepository, db.savedDeviceDao())

        waitUntil { viewModel.state.value is IrState.Ready }
        val profiles = (viewModel.state.value as IrState.Ready).profiles
        assertTrue(profiles.isNotEmpty())

        viewModel.selectProfile(profiles.first())
        viewModel.sendCommand("VolumeDown")

        waitUntil { fakeTransmitter.transmitCallCount == 1 }
        assertEquals(1, fakeTransmitter.transmitCallCount)
        assertTrue(fakeTransmitter.lastPattern?.isNotEmpty() == true)
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
