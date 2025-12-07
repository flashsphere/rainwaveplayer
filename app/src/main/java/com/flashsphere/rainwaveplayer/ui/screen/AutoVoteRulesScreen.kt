package com.flashsphere.rainwaveplayer.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.autovote.v1.Condition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveAlbumCondition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveSongCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition.Operator
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition.RequestType
import com.flashsphere.rainwaveplayer.autovote.v1.Rule
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.RuleDialog
import com.flashsphere.rainwaveplayer.ui.item.SwipeToDismissBackground
import com.flashsphere.rainwaveplayer.ui.rememberNoFlingSwipeToDismissBoxState
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.view.viewmodel.AutoVoteViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun AutoVoteRulesScreen(
    viewModel: AutoVoteViewModel,
    onBackClick: () -> Unit,
) {
    AutoVoteRulesScreen(
        rules = viewModel.rules,
        onBackClick = onBackClick,
        onAdd = viewModel::addRule,
        onEdit = viewModel::updateRule,
        onDelete = viewModel::deleteRule,
        onReorderItem = viewModel::reorderRule,
        onReorder = viewModel::reorderRules,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoVoteRulesScreen(
    rules: SnapshotStateList<Rule>,
    onBackClick: () -> Unit,
    onAdd: (Rule) -> Unit,
    onEdit: (Rule, List<Condition>) -> Unit,
    onDelete: (rule: Rule, index: Int) -> Boolean,
    onReorderItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorder: () -> Unit,
) {
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan
    val addRuleDialog = rememberSaveable { mutableStateOf(false) }
    val editRuleDialog = rememberSaveable { mutableStateOf<Rule?>(null) }

    val windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))

    AppTheme {
        AppScaffold(
            modifier = Modifier.windowInsetsPadding(windowInsets),
            appBarContent = {
                AppBarTitle(title = stringResource(id = R.string.settings_auto_song_voting))
            },
            navigationIcon = {
                BackIcon(onBackClick = onBackClick)
            },
            floatingActionButton = {
                AddFab(onClick = { addRuleDialog.value = true })
            }
        ) {
            val haptics = LocalHapticFeedback.current
            val gridState = rememberLazyGridState()
            val reorderableLazyListState = rememberReorderableLazyGridState(gridState) { from, to ->
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onReorderItem(from.index, to.index)
            }
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = GridCells.Fixed(gridColumnCount),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                itemsIndexed(
                    items = rules,
                    key = { _, item -> item.id },
                ) { i, item ->
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = item.id,
                    ) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "ReorderRequests")
                        Surface(shadowElevation = elevation) {
                            RuleItem(
                                scope = this,
                                index = i,
                                rule = item,
                                onEdit = { editRuleDialog.value = item },
                                onReorder = onReorder,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }
            if (addRuleDialog.value) {
                RuleDialog(
                    onDismissRequest = { addRuleDialog.value = false },
                    onOkClick = {
                        onAdd(Rule(System.currentTimeMillis(), it))
                        addRuleDialog.value = false
                    }
                )
            }

            editRuleDialog.value?.let { rule ->
                RuleDialog(
                    conditions = rule.conditions,
                    onDismissRequest = { editRuleDialog.value = null },
                    onOkClick = {
                        onEdit(rule, it)
                        editRuleDialog.value = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AddFab(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.action_add),
        )
    }
}

@Composable
private fun RuleItem(
    scope: ReorderableCollectionItemScope,
    index: Int,
    rule: Rule,
    onEdit: () -> Unit,
    onReorder: () -> Unit,
    onDelete: (rule: Rule, index: Int) -> Boolean,
) {
    val haptics = LocalHapticFeedback.current

    val currentRule by rememberUpdatedState(rule)
    val currentIndex by rememberUpdatedState(index)

    val dismissState = rememberNoFlingSwipeToDismissBoxState(
        positionalThreshold = { it * .2F }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeToDismissBackground(dismissState) },
        onDismiss = { onDelete(currentRule, currentIndex) },
    ) {
        Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .heightIn(min = LocalUiScreenConfig.current.listItemLineHeight)
            .height(IntrinsicSize.Min)
            .clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = with(scope) {
                Modifier
                    .width(LocalUiScreenConfig.current.listItemLineHeight)
                    .fillMaxHeight()
                    .clickable(interactionSource = null, indication = null, onClick = {})
                    .draggableHandle(onDragStarted = {
                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    }, onDragStopped = {
                        onReorder()
                    })
            }, contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Filled.DragHandle, contentDescription = null)
            }
            Row(modifier = Modifier.weight(1F).padding(LocalUiScreenConfig.current.itemPadding)) {
                Text(text = "${currentIndex+1}. ", style = AppTypography.bodyMedium,
                    lineHeight = AppTypography.bodyMedium.fontSize)
                RuleItemText(modifier = Modifier.weight(1F), rule = currentRule)
            }
        }
    }
}

@Composable
private fun RuleItemText(modifier: Modifier = Modifier, rule: Rule) {
    val context = LocalContext.current
    val text = remember(rule) {
        buildAnnotatedString {
            val iterator = rule.conditions.listIterator()
            while (iterator.hasNext()) {
                val condition = iterator.next()
                withStyle(style = ParagraphStyle(lineHeight = AppTypography.bodyMedium.fontSize)) {
                    withStyle(style = AppTypography.bodyMedium.toSpanStyle()) {
                        when (condition) {
                            is RequestCondition -> {
                                when (condition.requestType) {
                                    RequestType.User -> append(context.getString(condition.requestType.fullStringResId))
                                    RequestType.Others -> append(context.getString(condition.requestType.fullStringResId))
                                }
                            }
                            is RatingCondition -> {
                                append(context.getString(condition.conditionType.stringResId))
                                append(" ${condition.operator.value} ${condition.rating}")
                            }
                            is FaveSongCondition, FaveAlbumCondition -> {
                                append(context.getString(condition.conditionType.stringResId))
                            }
                        }
                        if (iterator.hasNext()) {
                            withStyle(style = AppTypography.bodySmall.toSpanStyle()) {
                                append(" ")
                                append(context.getString(R.string.auto_vote_condition_and))
                            }
                        }
                    }
                }
            }
        }
    }
    Text(modifier = modifier, text = text, style = AppTypography.bodyMedium)
}

@Preview
@Composable
private fun AutoVoteScreenPreview() {
    val rules = remember { mutableStateListOf(
        Rule(
            id = 1,
            conditions = listOf(
                RequestCondition(RequestType.Others),
                RatingCondition(Operator.Greater, 4.2F),
                FaveSongCondition,
                FaveAlbumCondition,
            )
        ), Rule(
            id = 2,
            conditions = listOf(
                RequestCondition(RequestType.Others),
                RatingCondition(Operator.Lesser, 3F),
                FaveAlbumCondition,
            )
        ), Rule(
            id = 3,
            conditions = listOf(
                FaveSongCondition,
            )
        ),
    ) }
    PreviewTheme {
        AutoVoteRulesScreen(
            rules = rules,
            onBackClick = {},
            onAdd = {},
            onEdit = { _, _ -> },
            onDelete = { _, _ -> true },
            onReorderItem = { _, _ -> },
            onReorder = {},
        )
    }
}

@Preview
@Composable
private fun RuleItemTextPreview() {
    PreviewTheme {
        Surface {
            RuleItemText(
                rule = Rule(
                    id = System.currentTimeMillis(),
                    conditions = listOf(
                        RequestCondition(RequestType.Others),
                        RatingCondition(Operator.Greater, 4.2F),
                        FaveSongCondition,
                        FaveAlbumCondition,
                    )
                )
            )
        }
    }
}
