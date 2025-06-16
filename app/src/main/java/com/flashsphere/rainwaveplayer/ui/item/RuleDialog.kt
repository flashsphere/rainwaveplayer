package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.autovote.AutoVoteRatingValidator
import com.flashsphere.rainwaveplayer.autovote.v1.Condition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveAlbumCondition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveSongCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition.Operator
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition.RequestType
import com.flashsphere.rainwaveplayer.autovote.v1.Rule.ConditionType
import com.flashsphere.rainwaveplayer.ui.ClearFocusOnKeyboardHide
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.rememberMutableStateListOf
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun RuleDialog(
    conditions: List<Condition> = emptyList(),
    onDismissRequest: () -> Unit,
    onOkClick: (List<Condition>) -> Unit,
) {
    val updatedConditions = rememberMutableStateListOf(conditions.ifEmpty { listOf(RequestCondition(RequestType.User)) })
    val usedConditionTypes = remember { mutableStateOf(updatedConditions.asSequence().map { it.conditionType }.toSet()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(updatedConditions) {
        snapshotFlow { updatedConditions.toList() }.collect {
            usedConditionTypes.value = it.asSequence().map { it.conditionType }.toSet()
        }
    }

    CustomAlertDialog(
        properties = DialogProperties(decorFitsSystemWindows = false),
        onDismissRequest = onDismissRequest,
        title = null,
        content = {
            Column(Modifier.verticalScroll(scrollState, reverseScrolling = true).fillMaxWidth()) {
                updatedConditions.forEachIndexed { i, c ->
                    val index by rememberUpdatedState(i)
                    key(index) {
                        val modifier = if (index > 0) {
                            Modifier.padding(top = 8.dp)
                        } else {
                            Modifier
                        }

                        if (index > 0) {
                            Text(modifier = modifier.align(Alignment.CenterHorizontally),
                                text = stringResource(R.string.auto_vote_condition_and),
                                style = AppTypography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RuleDialogItem(
                                modifier = modifier.weight(1F),
                                usedConditionTypes = usedConditionTypes.value,
                                condition = c,
                                onChange = { updatedConditions[index] = it },
                                canRemove = usedConditionTypes.value.size > 1,
                                onRemove = { updatedConditions.removeAt(index) }
                            )
                        }
                    }
                }
            }
        },
        buttons = {
            if (usedConditionTypes.value.size < ConditionType.entries.size) {
                Tooltip(stringResource(R.string.auto_vote_condition_add)) {
                    RuleDialogIconButton(
                        onClick = {
                            ConditionType.entries.firstOrNull { !usedConditionTypes.value.contains(it) }?.let {
                                updatedConditions.add(Condition.new(it))
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.auto_vote_condition_add))
                    }
                }
            }
            Spacer(Modifier.weight(1F))
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
            TextButton(onClick = { onOkClick(updatedConditions) }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
    )
}

@Composable
private fun RuleDialogIconButton(
    onClick: () -> Unit,
    colors: IconButtonColors,
    icon: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(onClick = onClick, colors = colors) {
            icon()
        }
    }
}

@Composable
private fun RuleDialogItem(
    modifier: Modifier = Modifier,
    usedConditionTypes: Set<ConditionType>,
    condition: Condition,
    onChange: (Condition) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    val conditionTypesToShow = remember(usedConditionTypes, condition) {
        ConditionType.entries.filter { it == condition.conditionType || !usedConditionTypes.contains(it) }
    }
    Column(modifier = modifier) {
        Row {
            ConditionTypeSelect(
                conditionTypes = conditionTypesToShow,
                conditionType = condition.conditionType,
                onChange = { onChange(Condition.new(it)) }
            )
            if (canRemove) {
                Spacer(Modifier.weight(1F))

                Tooltip(stringResource(R.string.auto_vote_condition_remove)) {
                    RuleDialogIconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(imageVector = Icons.Filled.Remove, contentDescription = stringResource(R.string.auto_vote_condition_remove))
                    }
                }
            }
        }

        when (condition) {
            is RequestCondition -> ConditionSongRequestInput(
                requestType = condition.requestType,
                onChange = { onChange(RequestCondition(it)) },
            )
            is RatingCondition -> ConditionSongRatingInput(
                condition = condition,
                onChange = onChange,
            )
            is FaveSongCondition -> {}
            is FaveAlbumCondition -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropDownSelect(
    modifier: Modifier = Modifier,
    item: T,
    items: List<T>,
    toText: @Composable (item: T) -> String,
    textStyle: TextStyle = AppTypography.bodyMedium,
    onChange: (T) -> Unit,
) {
    val combinedTextStyle = LocalTextStyle.current.merge(textStyle.copy(color = MaterialTheme.colorScheme.onSurface))
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(modifier = modifier, expanded = expanded, onExpandedChange = { expanded = it }) {
        BasicTextField(
            modifier = Modifier
                .clickable { expanded = true }
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = toText(item), onValueChange = {},
            singleLine = true,
            readOnly = true,
            enabled = false,
            textStyle = combinedTextStyle,
        ) { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .width(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(4.dp))
                Box(Modifier.weight(1F)) {
                    innerTextField()
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            matchTextFieldWidth = false,
        ) {
            items.forEach {
                DropdownMenuItem(
                    text = { Text(text = toText(it), style = combinedTextStyle) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    })
            }
        }
    }
}

@Composable
private fun ConditionTypeSelect(
    conditionTypes: List<ConditionType>,
    conditionType: ConditionType,
    onChange: (ConditionType) -> Unit,
) {
    Row(modifier = Modifier.width(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Type: ", style = AppTypography.bodyMedium)
        DropDownSelect(
            modifier = Modifier.weight(1F),
            item = conditionType,
            items = conditionTypes,
            toText = { stringResource(it.stringResId) },
            onChange = onChange,
        )
    }
}

@Composable
private fun ConditionSongRequestInput(
    requestType: RequestType,
    onChange: (RequestType) -> Unit,
) {
    Row(modifier = Modifier.padding(bottom = 8.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        RequestType.entries.forEachIndexed { i, item ->
            key(i) {
                if (i > 0) {
                    Spacer(Modifier.width(16.dp))
                }
                val interactionSource = remember(item) { MutableInteractionSource() }
                Row(
                    modifier = Modifier.focusProperties { canFocus = false }
                        .clickable(interactionSource = interactionSource, indication = null, onClick = { onChange(item) },),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        RadioButton(
                            selected = requestType == item,
                            onClick = { onChange(item) },
                            interactionSource = interactionSource,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(modifier = Modifier.padding(4.dp), text = stringResource(item.shortStringResId), style = AppTypography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ConditionSongRatingInput(
    condition: RatingCondition,
    onChange: (RatingCondition) -> Unit,
) {
    val updatedCondition by rememberUpdatedState(condition)
    val ratingTextState = rememberTextFieldState(AutoVoteRatingValidator.formatToString(updatedCondition.rating))
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedState = interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocusedState) {
        launch {
            snapshotFlow { isFocusedState.value }
                .filter { it }
                .collect { ratingTextState.edit { selectAll() } }
        }
    }
    LaunchedEffect(ratingTextState) {
        launch {
            snapshotFlow { ratingTextState.text }
                .map { AutoVoteRatingValidator.parseToValue(it.toString()) }
                .collect { onChange(updatedCondition.copy(rating = it)) }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        DropDownSelect(
            item = updatedCondition.operator,
            items = Operator.entries,
            toText = { it.value },
            textStyle = AppTypography.bodyLarge,
            onChange = { onChange(updatedCondition.copy(operator = it)) },
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            modifier = Modifier.weight(1F),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done),
            textStyle = LocalTextStyle.current.merge(AppTypography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface)),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = interactionSource,
            state = ratingTextState,
            inputTransformation = {
                val newText = asCharSequence()
                if (originalText != newText && !AutoVoteRatingValidator.validate(newText.toString())) {
                    revertAllChanges()
                }
            },
            decorator = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .fillMaxWidth(),
                ) {
                    Column {
                        innerTextField()
                        HorizontalSeparator(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
            }
        )
        ClearFocusOnKeyboardHide(isFocusedState)
    }
}

@Preview
@Composable
private fun AddCriteriaDialogPreview() {
    PreviewTheme {
        Surface {
            RuleDialog(
                conditions = emptyList(),
                onDismissRequest = {},
                onOkClick = {},
            )
        }
    }
}
