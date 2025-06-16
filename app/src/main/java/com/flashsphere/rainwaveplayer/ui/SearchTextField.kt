package com.flashsphere.rainwaveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.ui.item.HorizontalSeparator
import com.flashsphere.rainwaveplayer.ui.screen.Preview
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    state: TextFieldState,
    clearFocusOnKeyboardHide: Boolean = true,
    onSubmit: () -> Unit,
    clearIcon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Filled.Clear,
            contentDescription = null
        )
    },
    label: @Composable () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val textFieldInteractionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (state.text.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxWidth().wrapContentSize()) {
        BasicTextField(
            state = state,
            textStyle = AppTypography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) keyboardController?.show() },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search),
            onKeyboardAction = {
                onSubmit()
                keyboardController?.hide()
            },
            interactionSource = textFieldInteractionSource,
            decorator = { innerTextField ->
                Column(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)) {

                        if (state.text.isEmpty()) {
                            label()
                        }

                        Row {
                            Box(modifier = Modifier.weight(1F)) { innerTextField() }

                            if (clearIcon != null && state.text.isNotEmpty()) {
                                Spacer(Modifier.size(4.dp))
                                Box(modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            state.clearText()
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        }
                                    )
                                ) {
                                    clearIcon()
                                }
                            }
                        }
                    }
                    HorizontalSeparator(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.5F))
                }
            }
        )
    }
    if (clearFocusOnKeyboardHide) {
        ClearFocusOnKeyboardHide(textFieldInteractionSource.collectIsFocusedAsState())
    }
}

@Composable
fun SearchTextFieldLabel(painter: Painter, text: String) {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Row {
        Icon(painter = painter, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.padding(start = 4.dp))
        Text(text = text, color = color, style = AppTypography.bodyLarge)
    }
}

private class SearchTextPreviewProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf(
        "",
        "test search",
    )
}

@Preview
@Composable
private fun SearchTextFieldPreview(
    @PreviewParameter(SearchTextPreviewProvider::class) text: String
) {
    val state = rememberTextFieldState(text)
    PreviewTheme {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            SearchTextField(state = state, onSubmit = {}) {
                SearchTextFieldLabel(
                    painter = rememberVectorPainter(Icons.Filled.Search),
                    text = "Search"
                )
            }
        }
    }
}
