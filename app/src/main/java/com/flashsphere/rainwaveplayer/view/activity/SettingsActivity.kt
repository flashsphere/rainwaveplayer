package com.flashsphere.rainwaveplayer.view.activity

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.ExecutorCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.imageLoader
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.cast.CastContextHolder
import com.flashsphere.rainwaveplayer.coroutine.launchWithDefaults
import com.flashsphere.rainwaveplayer.internal.datastore.SavedStationsStore
import com.flashsphere.rainwaveplayer.repository.UserRepository
import com.flashsphere.rainwaveplayer.service.MediaTileService
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.UiScreenConfig
import com.flashsphere.rainwaveplayer.ui.screen.SettingsScreen
import com.flashsphere.rainwaveplayer.ui.screen.tv.TvSettingsScreen
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.util.BufferMinSettingHelper
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ADD_REQUEST_TO_TOP
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.ANALYTICS
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_PLAY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_CLEAR
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_FAVE
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.AUTO_REQUEST_UNRATED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.BTM_NAV
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.BUFFER_MIN
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.CRASH_REPORTING
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.HIDE_RATING_UNTIL_RATED
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.SSL_RELAY
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.USE_OGG
import com.flashsphere.rainwaveplayer.util.PreferencesKeys.VOTE_SONG_NOTIFICATIONS
import com.flashsphere.rainwaveplayer.util.getBlocking
import com.flashsphere.rainwaveplayer.util.isTv
import com.flashsphere.rainwaveplayer.view.uistate.event.DismissSnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.MessageEvent
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.BasicPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.CheckBoxPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.FloatPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ListPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.Preference
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceCategoryItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceItemValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.skip
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var savedStationsStore: SavedStationsStore

    @Inject
    lateinit var castContextHolder: CastContextHolder

    private val snackbarEventFlow = MutableSharedFlow<SnackbarEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        SUSPEND
    )

    private lateinit var powerManager: PowerManager
    private lateinit var backPressedCallback: OnBackPressedCallback
    private lateinit var requestNotificationPermissionLauncher: ActivityResultLauncher<String>

    private var toast: Toast? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        powerManager = ContextCompat.getSystemService(this, PowerManager::class.java) as PowerManager
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                MainActivity.startActivity(this@SettingsActivity)
                finish()
            }
        }.also {
            onBackPressedDispatcher.addCallback(this, it)
        }

        if (userRepository.isLoggedIn()) {
            dataStore.data
                .drop(1)
                .onEach { castContextHolder.setCastLaunchCredentials() }
                .launchWithDefaults(lifecycleScope, "Export TV Preferences")
        }

        val preferences = createPreferences()

        if (!isTv()) {
            setContent("SettingsScreen") {
                val configuration = LocalConfiguration.current
                val windowSizeClass = calculateWindowSizeClass(this)

                CompositionLocalProvider(
                    LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass)
                ) {
                    SettingsScreen(preferences = preferences,
                        events = snackbarEventFlow,
                        onBackClick = { onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        } else {
            setContent("TvSettingsScreen") {
                val configuration = LocalConfiguration.current
                val windowSizeClass = calculateWindowSizeClass(this)

                CompositionLocalProvider(
                    LocalUiScreenConfig provides UiScreenConfig(configuration, windowSizeClass)
                ) {
                    TvSettingsScreen(
                        preferences = preferences,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        cancelToast()
        super.onDestroy()
    }

    private fun createPreferences(): List<Preference> {
        return if (!isTv()) {
            createNonTvPreferences()
        } else {
            createTvPreferences()
        }
    }

    private fun createNonTvPreferences(): List<Preference> {
        return mutableListOf<Preference>().apply {
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = AUTO_PLAY,
                title = getString(R.string.settings_auto_play),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_auto_play_desc)
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_auto_play_desc)
                    )
                )
            ))

            if (userRepository.isLoggedIn()) {
                add(PreferenceCategoryItem(title = getString(R.string.settings_ui)))
                add(ListPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = BTM_NAV,
                    title = getString(R.string.settings_btm_nav),
                    options = listOf(
                        PreferenceItemValue(
                            value = BottomNavPreference.Labeled.value,
                            label = getString(R.string.settings_btm_nav_labeled),
                            summary = getString(R.string.settings_btm_nav_labeled_desc),
                        ),
                        PreferenceItemValue(
                            value = BottomNavPreference.Unlabeled.value,
                            label = getString(R.string.settings_btm_nav_unlabeled),
                            summary = getString(R.string.settings_btm_nav_unlabeled_desc),
                        ),
                        PreferenceItemValue(
                            value = BottomNavPreference.Hidden.value,
                            label = getString(R.string.settings_btm_nav_hidden),
                            summary = getString(R.string.settings_btm_nav_hidden_desc),
                        )
                    ),
                ).also {
                    it.state.drop(1) // ignore the initial value
                        .onEach { backPressedCallback.isEnabled = true }
                        .launchIn(lifecycleScope)
                })
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = HIDE_RATING_UNTIL_RATED,
                    title = getString(R.string.settings_hide_rating_until_rated),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_hide_rating_until_rated_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_hide_rating_until_rated_desc),
                        ),
                    ),
                ).also {
                    it.state.drop(1) // ignore the initial value
                        .onEach { backPressedCallback.isEnabled = true }
                        .launchIn(lifecycleScope)
                })
                add(PreferenceCategoryItem(title = getString(R.string.settings_song_voting)))
                add(BasicPreferenceItem(
                    title = getString(R.string.settings_auto_song_voting),
                    summary = getString(R.string.settings_auto_song_voting_desc),
                    onClick = { openAutoSongVoteRules() },
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = VOTE_SONG_NOTIFICATIONS,
                    value = dataStore.getBlocking(VOTE_SONG_NOTIFICATIONS) && isNotificationPermissionGranted(),
                    title = getString(R.string.settings_voting_notification),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_voting_notification_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_voting_notification_desc),
                        ),
                    ),
                ).also { item ->
                    requestNotificationPermissionLauncher = registerForActivityResult(RequestPermission()) {
                        notificationPermissionGranted(it, item.state)
                    }
                    lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            Timber.d("Unregister activity result launcher")
                            requestNotificationPermissionLauncher.unregister()
                        }
                    })

                    item.state.drop(1) // ignore the initial value
                        .onEach { value ->
                            handleVoteSongNotificationPreferenceChanged(item.state, value)
                        }.launchIn(lifecycleScope)
                })

                add(PreferenceCategoryItem(title = getString(R.string.settings_song_requests)))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = ADD_REQUEST_TO_TOP,
                    title = getString(R.string.settings_add_request_to_top),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_add_request_to_top_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_add_request_to_top_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_FAVE,
                    title = getString(R.string.settings_auto_request_faves),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_faves_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_faves_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_UNRATED,
                    title = getString(R.string.settings_auto_request_unrated),
                    options = listOf(
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_unrated_desc),
                        ),
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_unrated_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_CLEAR,
                    title = getString(R.string.settings_auto_request_clear),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_clear_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_clear_desc),
                        ),
                    )
                ))
            }

            add(PreferenceCategoryItem(title = getString(R.string.settings_in_app_playback)))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = USE_OGG,
                title = getString(R.string.settings_stream),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_stream_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_stream_desc),
                    ),
                )
            ))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = SSL_RELAY,
                title = getString(R.string.settings_stream_https),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_stream_https_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_stream_https_desc),
                    ),
                )
            ))
            add(FloatPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = BUFFER_MIN,
                title = getString(R.string.settings_buffering),
                summary = getString(R.string.settings_buffering_min_buffer_desc),
                validator = { BufferMinSettingHelper.validateBufferSetting(it) },
                valueToString = { BufferMinSettingHelper.formatToString(it) },
                stringToValue = { BufferMinSettingHelper.parseToValue(it) },
            ))

            add(PreferenceCategoryItem(title = getString(R.string.settings_other)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(BasicPreferenceItem(
                    title = getString(R.string.settings_other_tile_add),
                    summary = getString(R.string.settings_other_tile_add_summary),
                    onClick = { requestAddTile() },
                ))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                add(BasicPreferenceItem(
                    title = getString(R.string.settings_other_battery_usage),
                    summary = getString(R.string.settings_other_battery_usage_summary),
                    onClick = { openBatteryUsage() },
                ))
            }
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = ANALYTICS,
                title = getString(R.string.settings_usage_statistics),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_usage_statistics_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_usage_statistics_desc),
                    ),
                )
            ))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = CRASH_REPORTING,
                title = getString(R.string.settings_crash_reporting),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_crash_reporting_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_crash_reporting_desc),
                    ),
                )
            ))
            add(BasicPreferenceItem(
                title = getString(R.string.settings_clear_cache),
                summary = getString(R.string.settings_clear_cache_summary),
                onClick = { clearCache() },
            ))
        }
    }

    private fun createTvPreferences(): List<Preference> {
        return mutableListOf<Preference>().apply {
            if (userRepository.isLoggedIn()) {
                add(PreferenceCategoryItem(title = getString(R.string.settings_ui)))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = HIDE_RATING_UNTIL_RATED,
                    title = getString(R.string.settings_hide_rating_until_rated),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_hide_rating_until_rated_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_hide_rating_until_rated_desc),
                        ),
                    ),
                ).also {
                    it.state.drop(1) // ignore the initial value
                        .onEach { backPressedCallback.isEnabled = true }
                        .launchIn(lifecycleScope)
                })
                add(PreferenceCategoryItem(title = getString(R.string.settings_song_voting)))
                add(BasicPreferenceItem(
                    title = getString(R.string.settings_auto_song_voting),
                    summary = getString(R.string.settings_auto_song_voting_desc),
                    onClick = { openAutoSongVoteRules() },
                ))
                add(PreferenceCategoryItem(title = getString(R.string.settings_song_requests)))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = ADD_REQUEST_TO_TOP,
                    title = getString(R.string.settings_add_request_to_top),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_add_request_to_top_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_add_request_to_top_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_FAVE,
                    title = getString(R.string.settings_auto_request_faves),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_faves_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_faves_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_UNRATED,
                    title = getString(R.string.settings_auto_request_unrated),
                    options = listOf(
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_unrated_desc),
                        ),
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_unrated_desc),
                        ),
                    )
                ))
                add(CheckBoxPreferenceItem(
                    scope = lifecycleScope,
                    dataStore = dataStore,
                    key = AUTO_REQUEST_CLEAR,
                    title = getString(R.string.settings_auto_request_clear),
                    options = listOf(
                        PreferenceItemValue(
                            value = true,
                            label = getString(R.string.settings_auto_request_clear_desc),
                        ),
                        PreferenceItemValue(
                            value = false,
                            label = getString(R.string.settings_auto_request_clear_desc),
                        ),
                    )
                ))
            }

            add(PreferenceCategoryItem(title = getString(R.string.settings_in_app_playback)))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = USE_OGG,
                title = getString(R.string.settings_stream),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_stream_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_stream_desc),
                    ),
                )
            ))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = SSL_RELAY,
                title = getString(R.string.settings_stream_https),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_stream_https_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_stream_https_desc),
                    ),
                )
            ))
            add(FloatPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = BUFFER_MIN,
                title = getString(R.string.settings_buffering),
                summary = getString(R.string.settings_buffering_min_buffer_desc),
                validator = { BufferMinSettingHelper.validateBufferSetting(it) },
                valueToString = { BufferMinSettingHelper.formatToString(it) },
                stringToValue = { BufferMinSettingHelper.parseToValue(it) },
            ))

            add(PreferenceCategoryItem(title = getString(R.string.settings_other)))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = ANALYTICS,
                title = getString(R.string.settings_usage_statistics),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_usage_statistics_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_usage_statistics_desc),
                    ),
                )
            ))
            add(CheckBoxPreferenceItem(
                scope = lifecycleScope,
                dataStore = dataStore,
                key = CRASH_REPORTING,
                title = getString(R.string.settings_crash_reporting),
                options = listOf(
                    PreferenceItemValue(
                        value = true,
                        label = getString(R.string.settings_crash_reporting_desc),
                    ),
                    PreferenceItemValue(
                        value = false,
                        label = getString(R.string.settings_crash_reporting_desc),
                    ),
                )
            ))
            add(BasicPreferenceItem(
                title = getString(R.string.settings_clear_cache),
                summary = getString(R.string.settings_clear_cache_summary),
                onClick = { clearCache() },
            ))
        }
    }

    private fun handleVoteSongNotificationPreferenceChanged(
        preferenceState: MutableStateFlow<Boolean>,
        value: Boolean
    ) {
        if (!value || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (isNotificationPermissionGranted()) {
            notificationPermissionGranted(true, preferenceState)
        } else {
            requestNotificationPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationPermissionGranted(granted: Boolean, preferenceState: MutableStateFlow<Boolean>) {
        lifecycleScope.launch { snackbarEventFlow.emit(DismissSnackbarEvent) }

        if (granted) {
            return
        }

        preferenceState.value = false
        lifecycleScope.launch {
            snackbarEventFlow.emit(MessageEvent(
                message = getString(R.string.permission_notification_not_granted),
                retry = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val notificationSettingsIntent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }

                        startActivity(notificationSettingsIntent)
                    }
                }
            ))
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddTile() {
        lifecycleScope.launch { snackbarEventFlow.emit(DismissSnackbarEvent) }

        val statusBarManager = ContextCompat.getSystemService(this, StatusBarManager::class.java) as StatusBarManager
        statusBarManager.requestAddTileService(
            ComponentName(this, MediaTileService::class.java),
            getString(R.string.app_label),
            Icon.createWithResource(this, R.drawable.ic_rainwave_64dp),
            ExecutorCompat.create(Handler(Looper.getMainLooper())),
        ) { resultCode ->
            val message = when (resultCode) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                    getString(R.string.settings_other_tile_added)
                }
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                    getString(R.string.settings_other_tile_already_added)
                }
                else -> {
                    getString(R.string.settings_other_tile_not_added, resultCode)
                }
            }
            lifecycleScope.launch {
                snackbarEventFlow.emit(MessageEvent(message = message))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun openBatteryUsage() {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = "package:${packageName}".toUri()
            startActivity(intent)
        }
    }

    private fun openAutoSongVoteRules() {
        AutoVoteRulesActivity.startActivity(this)
    }

    private fun clearCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        savedStationsStore.remove()

        cancelToast()
        toast = Toast.makeText(this, R.string.settings_cache_cleared, Toast.LENGTH_LONG)
            .also { it.show() }
    }

    private fun cancelToast() {
        toast?.cancel()
        toast = null
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}
