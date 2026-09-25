package com.jarvis.assistant.service

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.jarvis.assistant.data.InterpretResponseParams
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class ActionsServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var packageManager: PackageManager

    private lateinit var actionsService: ActionsService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(context.packageManager).thenReturn(packageManager)
        actionsService = ActionsService(context)
    }

    @Test
    fun testOpenAppSuccess() {
        runBlockingTest {
            val activityInfo = mock<android.content.pm.ActivityInfo>()
            activityInfo.packageName = "com.spotify.music"
            activityInfo.name = "com.spotify.music.MainActivity"

            val resolveInfo = mock<ResolveInfo>()
            resolveInfo.activityInfo = activityInfo
            resolveInfo.loadLabel(packageManager).returns("Spotify")

            `when`(packageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(listOf(resolveInfo))
            `when`(packageManager.getLaunchIntentForPackage("com.spotify.music"))
                .thenReturn(android.content.Intent())

            val result = actionsService.executeAction(
                "open_app",
                "Spotify",
                InterpretResponseParams(app_name = "Spotify")
            )

            assertTrue(result is ActionResult.Success)
            assertEquals("Opening Spotify.", (result as ActionResult.Success).message)
        }
    }

    @Test
    fun testOpenAppNotFound() {
        runBlockingTest {
            `when`(packageManager.queryIntentActivities(any(), anyInt()))
                .thenReturn(emptyList())

            val result = actionsService.executeAction(
                "open_app",
                "NonExistentApp",
                InterpretResponseParams(app_name = "NonExistentApp")
            )

            assertTrue(result is ActionResult.Failure)
            assertEquals("I couldn't find that app on this phone.", (result as ActionResult.Failure).message)
        }
    }

    @Test
    fun testSetAlarmParseTime24Hour() {
        runBlockingTest {
            val service = ActionsService(context)
            
            // Use reflection to test private method
            val parseTimeMethod = ActionsService::class.java.getDeclaredMethod("parseTime", String::class.java)
            parseTimeMethod.isAccessible = true

            assertEquals(7 to 0, parseTimeMethod.invoke(service, "07:00"))
            assertEquals(18 to 30, parseTimeMethod.invoke(service, "18:30"))
            assertEquals(23 to 59, parseTimeMethod.invoke(service, "23:59"))
        }
    }

    @Test
    fun testSetAlarmParseTime12Hour() {
        runBlockingTest {
            val service = ActionsService(context)
            val parseTimeMethod = ActionsService::class.java.getDeclaredMethod("parseTime", String::class.java)
            parseTimeMethod.isAccessible = true

            assertEquals(7 to 0, parseTimeMethod.invoke(service, "7am"))
            assertEquals(19 to 0, parseTimeMethod.invoke(service, "7pm"))
            assertEquals(12 to 0, parseTimeMethod.invoke(service, "12pm"))
            assertEquals(0 to 0, parseTimeMethod.invoke(service, "12am"))
            assertEquals(10 to 30, parseTimeMethod.invoke(service, "10:30am"))
        }
    }

    @Test
    fun testSetAlarmInvalidTime() {
        runBlockingTest {
            val service = ActionsService(context)
            val parseTimeMethod = ActionsService::class.java.getDeclaredMethod("parseTime", String::class.java)
            parseTimeMethod.isAccessible = true

            assertNull(parseTimeMethod.invoke(service, "invalid"))
            assertNull(parseTimeMethod.invoke(service, "25:00"))
            assertNull(parseTimeMethod.invoke(service, "13:00pm"))
        }
    }
}