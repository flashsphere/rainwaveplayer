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
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiSettings
import com.flashsphere.rainwaveplayer.ui.screen.RequestHistoryScreen
import com.flashsphere.rainwaveplayer.util.IntentUtils
import com.flashsphere.rainwaveplayer.view.viewmodel.UserPagedListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RequestHistoryActivity : BaseActivity() {

    private val viewModel: UserPagedListViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val station = viewModel.station.value
            ?: IntentUtils.getParcelableExtra(intent, INTENT_EXTRA_PARAM_STATION, Station::class.java)?.apply {
                viewModel.station(this)
            }
        if (station == null) {
            finish()
            return
        }

        setContent("RequestHistoryScreen") {
            val configuration = LocalConfiguration.current
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(
                LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass),
                LocalUiSettings provides UiSettings(NavigationSuiteType.None, dataStore),
            ) {
                RequestHistoryScreen(viewModel = viewModel, onBackPress = this::finish)
            }
        }
    }

    companion object {
        private const val INTENT_EXTRA_PARAM_STATION = "com.flashsphere.data.station"

        fun startActivity(context: Context, station: Station) {
            val intent = Intent(context, RequestHistoryActivity::class.java)
                .putExtra(INTENT_EXTRA_PARAM_STATION, station)
            context.startActivity(intent)
        }
    }
}
