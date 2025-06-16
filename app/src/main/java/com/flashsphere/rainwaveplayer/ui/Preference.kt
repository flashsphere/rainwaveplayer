package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
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
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.item.HorizontalSeparator
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.BottomNavPreference
import com.flashsphere.rainwaveplayer.view.uistate.model.PreferenceItemValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.ParsePosition

@Composable
private fun Preference(
    title: String,
    summary: String? = null,
    widget: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusable(true)
            .clickable(enabled = true, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1F)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            if (summary != null) {
                Text(text = summary, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        widget?.let {
            Box(
                Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .width(40.dp), contentAlignment = Alignment.CenterEnd) {
                it.invoke()
            }
        }
    }
}

@Composable
fun BasicPreference(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Preference(
        title = title,
        summary = summary,
        widget = null,
        onClick = onClick
    )
}

@Composable
fun CheckboxPreference(
    state: MutableStateFlow<Boolean>,
    title: String,
    summary: String? = null,
) {
    val value = state.collectAsStateWithLifecycle().value
    Preference(
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
fun TextPreference(
    state: TextFieldState,
    title: String,
    summary: String,
    keyboardType: KeyboardType,
    validator: (value: String) -> Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focusState = interactionSource.collectIsFocusedAsState()

    LaunchedEffect(focusState) {
        snapshotFlow { focusState.value }
            .filter { it }
            .collect {
                state.edit { selectAll() }
            }
    }

    Preference(
        title = title,
        summary = summary,
        widget = {
            BasicTextField(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .widthIn(max = 30.dp)
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.merge(AppTypography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface)),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
                    imeAction = ImeAction.Done),
                interactionSource = interactionSource,
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
                        HorizontalSeparator(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
            )
        },
        onClick = { focusRequester.requestFocus() }
    )
    ClearFocusOnKeyboardHide(focusState)
}

@Composable
fun <T> ListPreference(
    state: MutableStateFlow<T>,
    title: String,
    items: List<PreferenceItemValue<T>>
) {
    val openSelector = rememberSaveable { mutableStateOf(false) }
    val value = state.collectAsStateWithLifecycle().value
    val preferenceItem = items.find { it.value == value }

    Preference(
        title = title,
        summary = preferenceItem?.summary,
        widget = null,
        onClick = { openSelector.value = true }
    )

    if (openSelector.value) {
        PreferenceAlertDialog(
            onDismissRequest = { openSelector.value = false },
            title = { Text(text = title) },
            buttons = {
                TextButton(onClick = { openSelector.value = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            },
            content = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { i, item ->
                        key(i) {
                            ListPreferenceItem(onClick = {
                                state.value = item.value
                                openSelector.value = false
                            }) {
                                RadioButton(selected = (value == item.value), onClick = null)
                                Spacer(modifier = Modifier.width(24.dp))
                                Text(
                                    text = item.label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ListPreferenceItem(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier
        .heightIn(36.dp)
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun PreferenceAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    CustomAlertDialog(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(
                LocalContentColor provides AlertDialogDefaults.titleContentColor,
                LocalTextStyle provides LocalTextStyle.current.merge(AppTypography.bodyLarge),
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    title()
                }
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)) {
                content()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 0.dp, bottom = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                buttons()
            }
        }
    }
}

@Composable
fun PreferenceCategory(title: String) {
    Column {
        HorizontalSeparator()
        Text(modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
            text = title, style = AppTypography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)
    }
}

@Preview
@Composable
private fun BasicPreferencePreview() {
    PreviewTheme {
        Surface {
            BasicPreference(
                title = stringResource(id = R.string.settings_other_battery_usage),
                summary = stringResource(id = R.string.settings_other_battery_usage_summary),
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun CheckboxPreferencePreview() {
    PreviewTheme {
        Surface {
            CheckboxPreference(
                state = MutableStateFlow(false),
                title = stringResource(id = R.string.settings_auto_play),
                summary = stringResource(id = R.string.settings_auto_play_desc),
            )
        }
    }
}

@Preview
@Composable
private fun TextPreferencePreview() {
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
    PreviewTheme {
        Surface {
            TextPreference(
                state = rememberTextFieldState("12"),
                title = stringResource(id = R.string.settings_buffering),
                summary = stringResource(id = R.string.settings_buffering_min_buffer_desc),
                keyboardType = KeyboardType.Number,
                validator = validator,
            )
        }
    }
}

@Preview
@Composable
private fun ListPreferencePreview() {
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
    PreviewTheme {
        Surface {
            ListPreference(
                state = MutableStateFlow(items[1].value),
                title = stringResource(id = R.string.settings_btm_nav),
                items = items,
            )
        }
    }
}

@Preview
@Composable
private fun PreferenceAlertDialogPreview() {
    PreviewTheme {
        Surface {
            PreferenceAlertDialog(
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
                    TextButton(onClick = {}) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Preview
@Composable
private fun PreferenceCategoryPreview() {
    PreviewTheme {
        Surface {
            PreferenceCategory(stringResource(id = R.string.settings_auto_play))
        }
    }
}
