package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.BuildConfig
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.OneLineWithSubtitleParams
import com.flashsphere.rainwaveplayer.ui.item.OneLineWithSubtitleText
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onFeedbackItemClick: () -> Unit,
    onPrivacyPolicyItemClick: () -> Unit,
    onVersionItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

    AppTheme {
        AppScaffold(
            modifier = Modifier.windowInsetsPadding(windowInsets),
            appBarContent = {
                AppBarTitle(title = stringResource(id = R.string.about))
            },
            appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            navigationIcon = {
                BackIcon(onBackClick = onBackClick)
            },
        ) {
            AboutGrid(
                onFeedbackItemClick = onFeedbackItemClick,
                onPrivacyPolicyItemClick = onPrivacyPolicyItemClick,
                onVersionItemClick = onVersionItemClick,
            )
        }
    }
}

@Composable
private fun AboutGrid(
    onFeedbackItemClick: () -> Unit,
    onPrivacyPolicyItemClick: () -> Unit,
    onVersionItemClick: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(LocalUiScreenConfig.current.gridSpan),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            FeedbackItem(onFeedbackItemClick)
        }
        item {
            OneLineWithSubtitleText(
                OneLineWithSubtitleParams(
                    text = stringResource(R.string.privacy_policy),
                    onClick = onPrivacyPolicyItemClick
                )
            )
        }
        item {
            OneLineWithSubtitleText(
                OneLineWithSubtitleParams(
                    text = stringResource(R.string.version),
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = onVersionItemClick
                )
            )
        }
    }
}

@Composable
private fun FeedbackItem(
    onFeedbackItemClick: () -> Unit,
) {
    val infoDialogOpen = remember { mutableStateOf(false) }
    OneLineWithSubtitleText(
        OneLineWithSubtitleParams(
            text = stringResource(R.string.feedback),
            onClick = { infoDialogOpen.value = true }
        )
    )

    if (infoDialogOpen.value) {
        CustomAlertDialog(
            onDismissRequest = { infoDialogOpen.value = false },
            title = null,
            content = {
                Text(text = stringResource(id = R.string.feedback_instructions),
                modifier = Modifier.padding(bottom = 16.dp))
            },
            buttons = {
                TextButton(onClick = {
                    onFeedbackItemClick()
                    infoDialogOpen.value = false
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        )
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    PreviewTheme {
        AboutScreen(
            onFeedbackItemClick = {},
            onPrivacyPolicyItemClick = {},
            onVersionItemClick = {},
            onBackClick = {})
    }
}
