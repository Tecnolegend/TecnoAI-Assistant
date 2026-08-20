package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.assistant.IntentRouter
import com.example.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tecno AI", appName)
    }

    @Test
    fun testToolRegistryIntegrity() {
        val tools = ToolRegistry.allTools
        assertTrue(tools.isNotEmpty())
        assertNotNull(ToolRegistry.getTool("open_app"))
        assertNotNull(ToolRegistry.getTool("set_timer"))
        assertNotNull(ToolRegistry.getTool("set_alarm"))
        assertNotNull(ToolRegistry.getTool("call_contact"))
        assertNotNull(ToolRegistry.getTool("prepare_message"))
        assertNotNull(ToolRegistry.getTool("toggle_flashlight"))
        assertNotNull(ToolRegistry.getTool("open_settings"))
    }

    @Test
    fun testIntentRouterDeterministicParsing() {
        val router = IntentRouter()

        val openApp = router.parseFastIntent("open youtube")
        assertNotNull(openApp)
        assertEquals("open_app", openApp?.name)
        assertEquals("youtube", openApp?.parameters?.get("app_name"))

        val timer = router.parseFastIntent("set a timer for 10 minutes")
        assertNotNull(timer)
        assertEquals("set_timer", timer?.name)
        assertEquals(600, timer?.parameters?.get("seconds"))

        val flashlight = router.parseFastIntent("turn on the flashlight")
        assertNotNull(flashlight)
        assertEquals("toggle_flashlight", flashlight?.name)
        assertEquals(true, flashlight?.parameters?.get("enabled"))
    }
}
