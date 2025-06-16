package com.flashsphere.rainwaveplayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.flashsphere.rainwaveplayer.util.Strings.fromBase64
import com.flashsphere.rainwaveplayer.view.activity.StoreUserCredentialsActivity
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@Ignore("Skip media player tests")
@RunWith(AndroidJUnit4::class)
@LargeTest
class MediaPlayerTest {
    private lateinit var device: UiDevice

    @Before
    fun startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(getInstrumentation())
        assertThat(device).isNotNull()

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage = launcherPackageName
        assertThat(launcherPackage).isNotNull()
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIMEOUT)

        // Launch the app
        val context: Context = getApplicationContext()

        val testCredentials = BuildConfig.TEST_CREDENTIALS.fromBase64()
        val intent: Intent = StoreUserCredentialsActivity.getCallingIntent(context, testCredentials)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Wait for the app to appear
        val appPkg = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
        assertThat(appPkg).isNotNull()
    }

    @Test
    fun shouldStartPlayback() {
        device.wait(Until.findObject(By.res("playback_btn")), TIMEOUT).let { playBtn ->
            assertThat(playBtn).isNotNull()
            if (!playBtn.isFocused) {
                playBtn.click()
            } else {
                device.pressEnter()
            }
        }

        device.wait(Until.findObject(By.textContains("VOTE NOW")), TIMEOUT).let {
            assertThat(it).isNotNull()
        }

        /*val qsMediaControls = openNotificationPanel()
        val mediaSongTitle = qsMediaControls.getChild(UiSelector().text("Game"))
        assertThat(mediaSongTitle.waitForExists(TIMEOUT)).isTrue()*/
    }

    private val launcherPackageName: String
        /**
         * Uses package manager to find the package name of the device launcher. Usually this package
         * is "com.android.launcher" but can be different at times. This is a generic solution which
         * works on all platforms.`
         */
        get() {
            // Create launcher Intent
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)

            // Use PackageManager to get the launcher package name
            val pm = getApplicationContext<Context>().packageManager
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return resolveInfo!!.activityInfo.packageName
        }

    private fun openNotificationPanel(): UiObject {
        synchronized(device) {
            if (device.openNotification()) {
                device.wait(Until.hasObject(By.pkg("com.android.systemui")), TIMEOUT)
                val item = device.findObject(UiSelector().resourceId("com.android.systemui:id/qs_media_controls"))
                assertThat(item.waitForExists(TIMEOUT)).isTrue()
                return item
            } else {
                throw IllegalStateException("Notification panel not found")
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.flashsphere.rainwaveplayer.debug"
        private const val TIMEOUT = 10000L
    }
}
