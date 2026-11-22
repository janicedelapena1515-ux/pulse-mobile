package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pulse Mobile", appName)
    }

    @Test
    fun `format utils formats bytes and timers accurately`() {
        assertEquals("0 B", FormatUtils.formatBytes(0L))
        assertEquals("1.00 KB", FormatUtils.formatBytes(1024L))
        assertEquals("1.50 MB", FormatUtils.formatBytes(1572864L))
        assertEquals("02:05", FormatUtils.formatSessionTimer(125_000L))
        assertEquals("1h 2m 5s", FormatUtils.formatUptime(3725_000L))
    }
}
