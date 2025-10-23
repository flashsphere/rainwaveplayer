package com.flashsphere.rainwaveplayer.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUserCredentials
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.AutoVoteRulesScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvAutoVoteRulesScreen
import com.flashsphere.rainwaveplayer.util.isTv
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoVoteRulesActivity : BaseActivity() {
    @Inject
    lateinit var userRepository: UserRepository

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isTv()) {
            setContent("AutoVoteRulesScreen") {
                val configuration = LocalConfiguration.current
                val windowSizeClass = calculateWindowSizeClass(this)

                CompositionLocalProvider(
                    LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                    LocalUserCredentials provides userRepository.getCredentials(),
                ) {
                    AutoVoteRulesScreen(
                        viewModel = hiltViewModel(),
                        onBackClick = { finish() },
                    )
                }
            }
        } else {
            setContent("TvAutoVoteRulesScreen") {
                val configuration = LocalConfiguration.current
                val windowSizeClass = calculateWindowSizeClass(this)

                CompositionLocalProvider(
                    LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                    LocalUserCredentials provides userRepository.getCredentials(),
                ) {
                    TvAutoVoteRulesScreen(
                        viewModel = hiltViewModel(),
                    )
                }
            }
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, AutoVoteRulesActivity::class.java))
        }
    }
}
