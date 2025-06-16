package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.TvBasicPreference
import com.flashsphere.rainwaveplayer.ui.TvCheckboxPreference
import com.flashsphere.rainwaveplayer.ui.TvListPreference
import com.flashsphere.rainwaveplayer.ui.TvPreferenceCategory
import com.flashsphere.rainwaveplayer.ui.TvTextPreference
import com.flashsphere.rainwaveplayer.ui.itemsSpan
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.BasicPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.CheckBoxPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.FloatPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.ListPreferenceItem
import com.flashsphere.rainwaveplayer.view.uistate.model.Preference
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceCategoryItem
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceType.Category

private const val gridColumnCount = 2

@Composable
fun TvSettingsScreen(
    preferences: List<Preference>,
) {
    val windowInsets = WindowInsets.ime
        .union(WindowInsets(left = 40.dp, right = 40.dp, top = 20.dp, bottom = 20.dp))

    TvAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(state = rememberLazyGridState(),
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Fixed(gridColumnCount),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = windowInsets.asPaddingValues(),
            ) {
                item(span = { GridItemSpan(gridColumnCount) }) {
                    Text(text = stringResource(id = R.string.settings),
                        style = TvAppTypography.titleLarge)
                }
                items(
                    items = preferences,
                    span = itemsSpan(gridColumnCount) { item ->
                        if (item.type == Category) {
                            GridItemSpan(gridColumnCount)
                        } else {
                            GridItemSpan(1)
                        }
                    },
                    contentType = { it.type },
                ) { item ->
                    when (item) {
                        is PreferenceCategoryItem -> {
                            TvPreferenceCategory(title = item.title)
                        }
                        is BasicPreferenceItem -> {
                            TvBasicPreference(title = item.title, summary = item.summary,
                                onClick = item.onClick)
                        }
                        is CheckBoxPreferenceItem -> {
                            TvCheckboxPreference(
                                state = item.state,
                                title = item.title,
                                summary = item.option?.summary
                            )
                        }
                        is ListPreferenceItem -> {
                            TvListPreference(state = item.state, title = item.title, items = item.options)
                        }
                        is FloatPreferenceItem -> {
                            TvTextPreference(state = item.textFieldState, title = item.title, summary = item.summary,
                                keyboardType = KeyboardType.Number, validator = item.validator)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
