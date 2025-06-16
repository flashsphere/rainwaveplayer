package com.flashsphere.baselineprofile

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import okio.ByteString.Companion.decodeBase64
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        println("targetAppId = " + InstrumentationRegistry.getArguments().getString("targetAppId"))
        // The application id for the running build variant is read from the instrumentation arguments.
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),

            // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
            includeInStartupProfile = true
        ) {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll through your most important UI.

            // Start default activity for your app
            pressHome()
            val testCredentials = BuildConfig.TEST_CREDENTIALS.decodeBase64()!!.string(StandardCharsets.UTF_8)
            val intent = Intent().apply {
                component = ComponentName("com.flashsphere.rainwaveplayer", "com.flashsphere.rainwaveplayer.view.activity.StoreUserCredentialsActivity")
                data = testCredentials.toUri()
            }
            startActivityAndWait(intent)
            startPlayback()
        }
    }

    private fun MacrobenchmarkScope.startPlayback() {
        device.wait(Until.findObject(By.res("playback_btn")), TIMEOUT).let { playBtn ->
            checkNotNull(playBtn) { "playback_btn not found!" }
            if (!playBtn.isFocused) {
                playBtn.click()
            } else {
                device.pressEnter()
            }
        }

        device.wait(Until.findObject(By.textContains("VOTE NOW")), TIMEOUT).let {
            checkNotNull(it) { "UI element containing 'VOTE NOW' text not found!" }
        }
    }

    companion object {
        private const val TIMEOUT = 10000L
    }
}
