package com.flashsphere.rainwaveplayer.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiSettings
import com.flashsphere.rainwaveplayer.ui.screen.AllFavesScreen
import com.flashsphere.rainwaveplayer.view.viewmodel.UserPagedListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AllFavesActivity : BaseActivity() {

    private val viewModel: UserPagedListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent("AllFavesScreen") {
            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                LocalUiSettings provides UiSettings(NavigationSuiteType.None, dataStore),
            ) {
                AllFavesScreen(viewModel = viewModel, onBackPress = { finish() })
            }
        }
    }

    companion object {
        fun getCallingIntent(context: Context): Intent {
            return Intent(context, AllFavesActivity::class.java)
        }
    }
}
