package com.flashsphere.rainwaveplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.alertdialog.TvCustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.item.HorizontalSeparator
import com.flashsphere.rainwaveplayer.ui.item.tv.TvListItem
import com.flashsphere.rainwaveplayer.ui.item.tv.TvTextButton
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceItemValue
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParsePosition

@Composable
private fun TvPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    widget: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    TvListItem(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        headlineContent = {
            Text(text = title)
        },
        supportingContent = summary?.let {
            {
                Text(text = summary)
            }
        },
        trailingContent = widget,
    )
}

@Composable
fun TvBasicPreference(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    TvPreference(
        title = title,
        summary = summary,
        widget = null,
        onClick = onClick
    )
}

@Composable
fun TvCheckboxPreference(
    state: MutableStateFlow<Boolean>,
    title: String,
    summary: String? = null,
) {
    val value = state.collectAsStateWithLifecycle().value
    TvPreference(
        title = title,
        summary = summary,
        widget = {
            Checkbox(
                modifier = Modifier.focusable(false),
                checked = value,
                onCheckedChange = null,
            )
        },
        onClick = { state.value = !value }
    )
}

@Composable
fun TvTextPreference(
    state: TextFieldState,
    title: String,
    summary: String,
    keyboardType: KeyboardType,
    validator: (value: String) -> Boolean,
) {
    val preferenceFocusRequester = remember { FocusRequester() }

    val textFieldFocusRequester = remember { FocusRequester() }
    val textFieldInteractionSource = remember { MutableInteractionSource() }

    val textFieldIsFocused by textFieldInteractionSource.collectIsFocusedAsState()
    LaunchedEffect(textFieldIsFocused) {
        if (textFieldIsFocused) {
            state.edit { selectAll() }
        }
    }
    BackHandler(textFieldIsFocused) {
        preferenceFocusRequester.requestFocus()
    }

    TvPreference(
        modifier = Modifier.focusRequester(preferenceFocusRequester),
        title = title,
        summary = summary,
        widget = {
            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(max = 30.dp)
                    .focusRequester(textFieldFocusRequester),
                textStyle = TvAppTypography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
                    imeAction = ImeAction.Done),
                onKeyboardAction = {
                    preferenceFocusRequester.requestFocus()
                },
                interactionSource = textFieldInteractionSource,
                state = state,
                inputTransformation = {
                    val newText = asCharSequence()
                    if (originalText != newText && !validator(newText.toString())) {
                        revertAllChanges()
                    }
                },
                decorator = { innerTextField ->
                    Column(modifier = Modifier.widthIn(min = 16.dp), horizontalAlignment = Alignment.End) {
                        innerTextField()
                        HorizontalSeparator()
                    }
                }
            )
        },
        onClick = {
            textFieldFocusRequester.requestFocus()
        }
    )
}

@Composable
fun <T> TvListPreference(
    state: MutableStateFlow<T>,
    title: String,
    items: List<PreferenceItemValue<T>>
) {
    val openSelector = rememberSaveable { mutableStateOf(false) }
    val value = state.collectAsStateWithLifecycle().value
    val preferenceItem = items.find { it.value == value }

    TvPreference(
        title = title,
        summary = preferenceItem?.summary,
        widget = null,
        onClick = { openSelector.value = true }
    )

    if (openSelector.value) {
        TvPreferenceAlertDialog(
            onDismissRequest = { openSelector.value = false },
            title = { Text(text = title) },
            buttons = {
                TvTextButton(onClick = { openSelector.value = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
            content = {
                LazyColumn(
                    modifier = Modifier.focusGroup(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    items(items) { item ->
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            if (value == item.value) {
                                focusRequester.requestFocus()
                            }
                        }
                        TvListItem(
                            modifier = Modifier.focusRequester(focusRequester),
                            onClick = {
                                state.value = item.value
                                openSelector.value = false
                            },
                            leadingContent = {
                                RadioButton(selected = (value == item.value), onClick = null)
                            },
                            headlineContent = {
                                Text(text = item.label)
                            },
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun TvPreferenceAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    TvCustomAlertDialog(onDismissRequest = onDismissRequest) {
        Column {
            Box(modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
            ) {
                title()
            }
            Box(modifier = Modifier.weight(1F, fill = false)) {
                content()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                buttons()
            }
        }
    }
}

@Composable
fun TvPreferenceCategory(title: String) {
    Column {
        HorizontalSeparator()
        Text(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary)
    }
}

@PreviewTv
@Composable
private fun TvBasicPreferencePreview() {
    PreviewTvTheme {
        Surface {
            TvBasicPreference(
                title = stringResource(id = R.string.settings_other_battery_usage),
                summary = stringResource(id = R.string.settings_other_battery_usage_summary),
                onClick = {}
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvCheckboxPreferencePreview() {
    PreviewTvTheme {
        Surface {
            TvCheckboxPreference(
                state = MutableStateFlow(false),
                title = stringResource(id = R.string.settings_auto_play),
                summary = stringResource(id = R.string.settings_auto_play_desc),
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvTextPreferencePreview() {
    val validator: (value: String) -> Boolean = { value: String ->
        runCatching {
            val displayFormat = DecimalFormat("#.#").apply {
                isParseBigDecimal = true
            }
            val parsePosition = ParsePosition(0)
            val number = displayFormat.parse(value, parsePosition) as BigDecimal
            if (parsePosition.index != value.length || parsePosition.errorIndex != -1) {
                false
            } else {
                val validatedNumber = number.setScale(1, RoundingMode.HALF_DOWN).toFloat()
                validatedNumber >= 1
            }
        }.getOrElse { false }
    }
    PreviewTvTheme {
        Surface {
            TvTextPreference(
                state = rememberTextFieldState("12"),
                title = stringResource(id = R.string.settings_buffering),
                summary = stringResource(id = R.string.settings_buffering_min_buffer_desc),
                keyboardType = KeyboardType.Number,
                validator = validator,
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvListPreferencePreview() {
    val items = listOf(
        PreferenceItemValue(
            value = BottomNavPreference.Labeled.value,
            summary = stringResource(id = R.string.settings_btm_nav_labeled_desc),
            label = stringResource(id = R.string.settings_btm_nav_labeled),
        ),
        PreferenceItemValue(
            value = BottomNavPreference.Unlabeled.value,
            summary = stringResource(id = R.string.settings_btm_nav_unlabeled_desc),
            label = stringResource(id = R.string.settings_btm_nav_unlabeled),
        ),
        PreferenceItemValue(
            value = BottomNavPreference.Hidden.value,
            summary = stringResource(id = R.string.settings_btm_nav_hidden_desc),
            label = stringResource(id = R.string.settings_btm_nav_hidden),
        ),
    )
    PreviewTvTheme {
        Surface {
            TvListPreference(
                state = MutableStateFlow(items[1].value),
                title = stringResource(id = R.string.settings_btm_nav),
                items = items,
            )
        }
    }
}

@PreviewTv
@Composable
private fun PreferenceAlertDialogPreview() {
    PreviewTvTheme {
        Surface {
            TvPreferenceAlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = "Test header")
                },
                content = {
                    Column {
                        ListPreferenceItem(onClick = {}) {
                            RadioButton(selected = true, onClick = null)
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "Item 1",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ListPreferenceItem(onClick = {}) {
                            RadioButton(selected = false, onClick = null)
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "Item 2",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ListPreferenceItem(onClick = {}) {
                            RadioButton(selected = false, onClick = null)
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "Item 3",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                },
                buttons = {
                    Button(onClick = {}) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@PreviewTv
@Composable
private fun TvPreferenceCategoryPreview() {
    PreviewTvTheme {
        Surface {
            TvPreferenceCategory(stringResource(id = R.string.settings_auto_play))
        }
    }
}
