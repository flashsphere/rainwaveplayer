package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.BasicPreference
import com.flashsphere.rainwaveplayer.ui.CheckboxPreference
import com.flashsphere.rainwaveplayer.ui.ListPreference
import com.flashsphere.rainwaveplayer.ui.PreferenceCategory
import com.flashsphere.rainwaveplayer.ui.TextPreference
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.BasicPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.CheckBoxPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.FloatPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ListPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.Preference
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceCategoryItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceType.Category
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: List<Preference>,
    events: Flow<SnackbarEvent>,
    onBackClick: () -> Unit,
) {
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan

    val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

    AppTheme {
        AppScaffold(
            modifier = Modifier.windowInsetsPadding(windowInsets),
            appBarContent = {
                AppBarTitle(title = stringResource(id = R.string.settings))
            },
            navigationIcon = {
                BackIcon(onBackClick = onBackClick)
            },
            snackbarEvents = events,
        ) {
            LazyVerticalGrid(state = rememberLazyGridState(),
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(gridColumnCount),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = preferences,
                    span = itemsSpan(gridColumnCount) { item ->
                        if (item.type == Category) {
                            GridItemSpan(gridColumnCount)
                        } else {
                            GridItemSpan(1)
                        }
                    },
                    contentType = { it.type }
                ) { item ->
                    when (item) {
                        is PreferenceCategoryItem -> {
                            PreferenceCategory(title = item.title)
                        }
                        is BasicPreferenceItem -> {
                            BasicPreference(title = item.title, summary = item.summary,
                                onClick = item.onClick)
                        }
                        is CheckBoxPreferenceItem -> {
                            CheckboxPreference(
                                state = item.state,
                                title = item.title,
                                summary = item.option?.summary
                            )
                        }
                        is ListPreferenceItem -> {
                            ListPreference(state = item.state, title = item.title, items = item.options)
                        }
                        is FloatPreferenceItem -> {
                            TextPreference(state = item.textFieldState, title = item.title, summary = item.summary,
                                keyboardType = KeyboardType.Number, validator = item.validator)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
