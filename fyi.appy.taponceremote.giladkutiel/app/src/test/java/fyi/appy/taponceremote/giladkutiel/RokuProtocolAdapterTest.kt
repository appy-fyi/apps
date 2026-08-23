package fyi.appy.taponceremote.giladkutiel

import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import fyi.appy.taponceremote.giladkutiel.data.remote.RokuProtocolAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** test_plan scenario "baseline parity": Roku ECP keypress requests over a MockWebServer. */
class RokuProtocolAdapterTest {
    private lateinit var server: MockWebServer
    private lateinit var adapter: RokuProtocolAdapter

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repeat(10) { server.enqueue(MockResponse().setResponseCode(200)) }
        adapter = RokuProtocolAdapter(server.url("").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun send_volumeUp_postsKeypress() = runTest {
        adapter.send(RemoteCommand.VolumeUp)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/keypress/VolumeUp", request.path)
    }

    @Test
    fun sendText_encodesEachCharacterInOrder() = runTest {
        adapter.sendText("Hi!")
        val first = server.takeRequest()
        val second = server.takeRequest()
        val third = server.takeRequest()
        assertEquals("/keypress/Lit_H", first.path)
        assertEquals("/keypress/Lit_i", second.path)
        assertEquals("/keypress/Lit_%21", third.path)
    }
}
