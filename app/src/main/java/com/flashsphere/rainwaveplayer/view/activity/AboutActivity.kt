package com.flashsphere.rainwaveplayer.view.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.AboutScreen
import com.flashsphere.rainwaveplayer.util.Strings.fromBase64
import com.flashsphere.rainwaveplayer.view.helper.CustomTabsUtil
import com.flashsphere.rainwaveplayer.view.helper.Urls.PRIVACY_POLICY_URL
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AboutActivity : BaseActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private var versionClickCounter = 0

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent("AboutScreen") {
            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass)
            ) {
                AboutScreen(
                    onFeedbackItemClick = { handleFeedbackItemClick() },
                    onPrivacyPolicyItemClick = { handlePrivacyPolicyItemClick() },
                    onVersionItemClick = { handleVersionItemClick() },
                    onBackClick = { finish() }
                )
            }
        }
    }

    private fun handleFeedbackItemClick() {
        val deviceInfo = Build.MANUFACTURER + " " + Build.MODEL
        Timber.d("device string = %s", deviceInfo)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.feedback_email_address)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_email_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_email_body, deviceInfo, Build.VERSION.RELEASE, BuildConfig.VERSION_NAME))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.feedback_email)))
    }


    private fun handlePrivacyPolicyItemClick() {
        CustomTabsUtil.openCustomTab(this, PRIVACY_POLICY_URL)
    }

    private fun handleVersionItemClick() {
        if (BuildConfig.TEST_CREDENTIALS.isBlank() || userRepository.isLoggedIn()) {
             return
        }

        Timber.d("clicks = %d", versionClickCounter)

        if (++versionClickCounter == 10) {
            Timber.e("Test credentials accessed")
            val testCredentials = BuildConfig.TEST_CREDENTIALS.fromBase64()
            startActivity(StoreUserCredentialsActivity.getCallingIntent(this, testCredentials))
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}
