package com.tecnolegend.tecnoai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tecnolegend.tecnoai.assistant.IntentRouter
import com.tecnolegend.tecnoai.data.local.UserSettingsManager
import com.tecnolegend.tecnoai.tools.ToolRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        assertEquals("open_app", openApp?.toolName)
        assertEquals("youtube", openApp?.args?.get("app_name"))

        val timer = router.parseFastIntent("set a timer for 10 minutes")
        assertNotNull(timer)
        assertEquals("set_timer", timer?.toolName)
        assertEquals(600, timer?.args?.get("seconds"))

        val flashlight = router.parseFastIntent("turn on the flashlight")
        assertNotNull(flashlight)
        assertEquals("toggle_flashlight", flashlight?.toolName)
        assertEquals(true, flashlight?.args?.get("enabled"))
    }

    @Test
    fun testUserSettingsDataStorePersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = UserSettingsManager(context)

        settingsManager.setWakeWordEnabled(false)
        val updated = settingsManager.userSettingsFlow.first()
        assertEquals(false, updated.isWakeWordEnabled)

        settingsManager.setWakeWordEnabled(true)
        val reEnabled = settingsManager.userSettingsFlow.first()
        assertEquals(true, reEnabled.isWakeWordEnabled)
    }
}
