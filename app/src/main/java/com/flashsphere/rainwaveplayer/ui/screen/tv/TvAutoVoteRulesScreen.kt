package com.flashsphere.rainwaveplayer.ui.screen.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.autovote.v1.Condition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveAlbumCondition
import com.flashsphere.rainwaveplayer.autovote.v1.FaveSongCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RatingCondition.Operator
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition
import com.flashsphere.rainwaveplayer.autovote.v1.RequestCondition.RequestType
import com.flashsphere.rainwaveplayer.autovote.v1.Rule
import com.flashsphere.rainwaveplayer.ui.composition.LastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalLastFocused
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.item.tv.TvRuleDialog
import com.flashsphere.rainwaveplayer.ui.item.tv.TvRuleDialogButton
import com.flashsphere.rainwaveplayer.ui.saveLastFocused
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTheme
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.view.viewmodel.AutoVoteViewModel

@Composable
fun TvAutoVoteRulesScreen(viewModel: AutoVoteViewModel) {
    TvAppTheme {
        val lastFocused = rememberSaveable { mutableStateOf(LastFocused()) }
        CompositionLocalProvider(
            LocalLastFocused provides lastFocused
        ) {
            TvAutoVoteRulesScreen(
                rules = viewModel.rules,
                onAdd = viewModel::addRule,
                onEdit = viewModel::updateRule,
                onDelete = { rule, i ->
                    val rules = viewModel.rules.toList()
                    val nextItem = rules.getOrNull(i + 1)
                    if (nextItem != null) {
                        lastFocused.value = LastFocused(tag = "rule_${nextItem.id}", shouldRequestFocus = true)
                    } else if (rules.size > 1) {
                        lastFocused.value = LastFocused(tag = "rule_${rules[i - 1].id}", shouldRequestFocus = true)
                    } else {
                        lastFocused.value = LastFocused(tag = "add_btn", shouldRequestFocus = true)
                    }
                    viewModel.deleteRule(rule, i)
                },
                onReorderItem = viewModel::reorderRule,
                onReorder = viewModel::reorderRules,
            )
        }
    }
}

private const val gridColumnCount = 3

@Composable
private fun TvAutoVoteRulesScreen(
    rules: SnapshotStateList<Rule>,
    onAdd: (Rule) -> Unit,
    onEdit: (Rule, List<Condition>) -> Unit,
    onDelete: (rule: Rule, index: Int) -> Boolean,
    onReorderItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorder: () -> Unit,
) {
    val addRuleDialog = rememberSaveable { mutableStateOf(false) }
    val editRuleDialog = rememberSaveable { mutableStateOf<Rule?>(null) }

    val windowInsets = WindowInsets.ime
        .union(WindowInsets(left = 40.dp, right = 40.dp, top = 20.dp, bottom = 20.dp))

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(state = rememberLazyGridState(),
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(gridColumnCount),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = windowInsets.asPaddingValues(),
        ) {
            item(span = { GridItemSpan(gridColumnCount) }) {
                Text(text = stringResource(id = R.string.settings_auto_song_voting),
                    style = TvAppTypography.titleLarge)
            }
            item(
                span = { GridItemSpan(gridColumnCount) },
                key = "actions",
                contentType = "actions"
            ) {
                Box {
                    Button(
                        modifier = Modifier.padding(vertical = 8.dp).saveLastFocused("add_btn"),
                        onClick = { addRuleDialog.value = true }
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                        Spacer(Modifier.size(4.dp))
                        Text(text = stringResource(R.string.action_add))
                    }
                }
            }
            itemsIndexed(
                items = rules,
                key = { _, item -> item.id },
                contentType = { _, _ -> "rule" },
            ) { i, item ->
                TvRuleItemCard(
                    modifier = Modifier.animateItem().saveLastFocused("rule_${item.id}"),
                    index = i,
                    rule = item,
                    onClick = { editRuleDialog.value = item },
                )
            }
        }
        if (addRuleDialog.value) {
            TvRuleDialog(
                onDismissRequest = { addRuleDialog.value = false },
                onOkClick = {
                    onAdd(Rule(System.currentTimeMillis(), it))
                    addRuleDialog.value = false
                }
            )
        }
        editRuleDialog.value?.let { rule ->
            TvRuleDialog(
                conditions = rule.conditions,
                onDismissRequest = { editRuleDialog.value = null },
                onOkClick = {
                    onEdit(rule, it)
                    editRuleDialog.value = null
                },
                moreActions = {
                    val index = rules.indexOfFirst { it.id == rule.id }
                    if (index > 0) {
                        TvRuleDialogButton(onClick = {
                            val toIndex = index - 1
                            onReorderItem(index, toIndex)
                            onReorder()
                        }, text = stringResource(id = R.string.action_move_up))
                    }
                    if (index < rules.size - 1) {
                        TvRuleDialogButton(onClick = {
                            val toIndex = index + 1
                            if (toIndex < rules.size) {
                                onReorderItem(index, toIndex)
                                onReorder()
                            }
                        }, text = stringResource(id = R.string.action_move_down))
                    }
                    TvRuleDialogButton(onClick = {
                        onDelete(rule, index)
                        editRuleDialog.value = null
                    }, text = stringResource(id = R.string.action_delete))
                }
            )
        }
    }
}

@Composable
private fun TvRuleItemCard(
    modifier: Modifier = Modifier,
    index: Int,
    rule: Rule,
    onClick: () -> Unit,
) {
    val currentRule by rememberUpdatedState(rule)
    val currentIndex by rememberUpdatedState(index)

    Card(
        modifier = modifier,
        onClick = onClick,
    ) {
        TvRuleItem(index = currentIndex, rule = currentRule)
    }
}

@Composable
fun TvRuleItem(modifier: Modifier = Modifier, index: Int, rule: Rule) {
    val resources = LocalResources.current

    val text = remember(rule) {
        buildAnnotatedString {
            val iterator = rule.conditions.listIterator()
            while (iterator.hasNext()) {
                val condition = iterator.next()
                withStyle(style = ParagraphStyle(lineHeight = TvAppTypography.bodyLarge.fontSize)) {
                    withStyle(style = TvAppTypography.bodyLarge.toSpanStyle()) {
                        when (condition) {
                            is RequestCondition -> {
                                when (condition.requestType) {
                                    RequestType.User -> append(resources.getString(condition.requestType.fullStringResId))
                                    RequestType.Others -> append(resources.getString(condition.requestType.fullStringResId))
                                }
                            }
                            is RatingCondition -> {
                                append(resources.getString(condition.conditionType.stringResId))
                                append(" ${condition.operator.value} ${condition.rating}")
                            }
                            is FaveSongCondition, FaveAlbumCondition -> {
                                append(resources.getString(condition.conditionType.stringResId))
                            }
                        }
                        if (iterator.hasNext()) {
                            withStyle(style = TvAppTypography.bodyMedium.toSpanStyle()) {
                                append(" ")
                                append(resources.getString(R.string.auto_vote_condition_and))
                            }
                        }
                    }
                }
            }
        }
    }

    val cardWidth: Dp = LocalUiScreenConfig.current.tvCardWidth
    val cardHeight: Dp = cardWidth / 2
    Row(modifier = modifier.width(cardWidth).height(cardHeight).padding(8.dp)) {
        Text(text = "${index+1}. ", style = TvAppTypography.bodyLarge,
            lineHeight = TvAppTypography.bodyLarge.fontSize)
        Text(modifier = Modifier.weight(1F), text = text, style = TvAppTypography.bodyLarge)
    }
}

@PreviewTv
@Composable
private fun TvAutoVoteRulesScreenPreview() {
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
        ),
    ) }
    PreviewTvTheme {
        TvAutoVoteRulesScreen(
            rules = rules,
            onAdd = {},
            onEdit = { _, _ -> },
            onDelete = { _, _ -> true },
            onReorderItem = { _, _ -> },
            onReorder = {},
        )
    }
}
