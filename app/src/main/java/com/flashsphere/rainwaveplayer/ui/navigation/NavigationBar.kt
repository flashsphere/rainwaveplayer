package com.flashsphere.rainwaveplayer.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import kotlin.math.roundToInt

@Composable
fun NavigationBar(
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(
            WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Bottom)),
            contentAlignment = Alignment.Center,
        ) {
            val width = if (LocalUiScreenConfig.current.widthSizeClass > WindowWidthSizeClass.Compact) {
                Modifier.widthIn(max = 480.dp)
            } else {
                Modifier
            }
            Row(
                modifier = Modifier
                    .selectableGroup()
                    .then(width),
                horizontalArrangement = Arrangement.spacedBy(NavigationBarItemHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.onPrimary,
    ),
    interactionSource: MutableInteractionSource? = null
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val styledIcon =
        @Composable {
            val color = when {
                !enabled -> colors.disabledIconColor
                selected -> colors.selectedIconColor
                else -> colors.unselectedIconColor
            }
            val iconColor by
                animateColorAsState(
                    targetValue = color,
                    animationSpec = tween(ItemAnimationDurationMillis)
                )
            // If there's a label, don't have a11y services repeat the icon description.
            val clearSemantics = label != null && (alwaysShowLabel || selected)
            Box(modifier = if (clearSemantics) Modifier.clearAndSetSemantics {} else Modifier) {
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
            }
        }

    val styledLabel: @Composable (() -> Unit)? =
        label?.let {
            @Composable {
                val color = when {
                    !enabled -> colors.disabledTextColor
                    selected -> colors.selectedTextColor
                    else -> colors.unselectedTextColor
                }
                val style = when {
                    selected -> MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    else -> MaterialTheme.typography.labelMedium
                }
                val textColor by animateColorAsState(
                    targetValue = color,
                    animationSpec = tween(ItemAnimationDurationMillis)
                )

                CompositionLocalProvider(
                    LocalContentColor provides textColor,
                    LocalTextStyle provides LocalTextStyle.current.merge(style),
                    content = label
                )
            }
        }

    var itemWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .weight(1f)
            .onSizeChanged { itemWidth = it.width },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true,
    ) {
        val animationProgress: State<Float> =
            animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(ItemAnimationDurationMillis)
            )

        // The entire item is selectable, but only the indicator pill shows the ripple. To achieve
        // this, we re-map the coordinates of the item's InteractionSource into the coordinates of
        // the indicator.
        val deltaOffset: Offset
        with(LocalDensity.current) {
            val indicatorWidth = ActiveIndicatorWidth.roundToPx()
            deltaOffset =
                Offset((itemWidth - indicatorWidth).toFloat() / 2, IndicatorVerticalOffset.toPx())
        }
        val offsetInteractionSource =
            remember(interactionSource, deltaOffset) {
                MappedInteractionSource(interactionSource, deltaOffset)
            }

        // The indicator has a width-expansion animation which interferes with the timing of the
        // ripple, which is why they are separate composables
        val indicatorRipple =
            @Composable {
                Box(
                    Modifier
                        .layoutId(IndicatorRippleLayoutIdTag)
                        .clip(CircleShape)
                        .indication(offsetInteractionSource, ripple())
                )
            }
        val indicator =
            @Composable {
                Box(
                    Modifier
                        .layoutId(IndicatorLayoutIdTag)
                        .graphicsLayer { alpha = animationProgress.value }
                        .background(
                            color = colors.selectedIndicatorColor,
                            shape = CircleShape,
                        )
                )
            }

        NavigationBarItemLayout(
            indicatorRipple = indicatorRipple,
            indicator = indicator,
            icon = styledIcon,
            label = styledLabel,
            alwaysShowLabel = alwaysShowLabel,
            animationProgress = { animationProgress.value },
        )
    }
}

@Composable
private fun NavigationBarItemLayout(
    indicatorRipple: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    alwaysShowLabel: Boolean,
    animationProgress: () -> Float,
) {
    Layout({
        indicatorRipple()
        indicator()

        Box(Modifier.layoutId(IconLayoutIdTag)) { icon() }

        if (label != null) {
            Box(
                Modifier
                    .layoutId(LabelLayoutIdTag)
                    .graphicsLayer { alpha = if (alwaysShowLabel) 1f else animationProgress() }
                    .padding(horizontal = NavigationBarItemHorizontalPadding / 2)
            ) {
                label()
            }
        }
    }) { measurables, constraints ->
        @Suppress("NAME_SHADOWING") val animationProgress = animationProgress()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val iconPlaceable =
            measurables.fastFirst { it.layoutId == IconLayoutIdTag }.measure(looseConstraints)

        val totalIndicatorWidth = iconPlaceable.width + (IndicatorHorizontalPadding * 2).roundToPx()
        val animatedIndicatorWidth = (totalIndicatorWidth * animationProgress).roundToInt()
        val indicatorHeight = iconPlaceable.height + (IndicatorVerticalPadding * 2).roundToPx()
        val indicatorRipplePlaceable =
            measurables
                .fastFirst { it.layoutId == IndicatorRippleLayoutIdTag }
                .measure(Constraints.fixed(width = totalIndicatorWidth, height = indicatorHeight))
        val indicatorPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == IndicatorLayoutIdTag }
                ?.measure(
                    Constraints.fixed(width = animatedIndicatorWidth, height = indicatorHeight)
                )

        if (label == null || !alwaysShowLabel) {
            placeIcon(iconPlaceable, indicatorRipplePlaceable, indicatorPlaceable, constraints)
        } else {
            val labelPlaceable = measurables.fastFirst { it.layoutId == LabelLayoutIdTag }
                .measure(looseConstraints)

            placeLabelAndIcon(
                labelPlaceable,
                iconPlaceable,
                indicatorRipplePlaceable,
                indicatorPlaceable,
                constraints,
                alwaysShowLabel,
                animationProgress
            )
        }
    }
}

/** Places the provided [Placeable]s in the center of the provided [constraints]. */
private fun MeasureScope.placeIcon(
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints
): MeasureResult {
    val width = constraints.maxWidth
    val height = iconPlaceable.height + (NavigationBarWithoutLabelVerticalPadding.roundToPx() * 2)

    val iconX = (width - iconPlaceable.width) / 2
    val iconY = (height - iconPlaceable.height) / 2

    val rippleX = (width - indicatorRipplePlaceable.width) / 2
    val rippleY = (height - indicatorRipplePlaceable.height) / 2

    return layout(width, height) {
        indicatorPlaceable?.let {
            val indicatorX = (width - it.width) / 2
            val indicatorY = (height - it.height) / 2
            it.placeRelative(indicatorX, indicatorY)
        }
        iconPlaceable.placeRelative(iconX, iconY)
        indicatorRipplePlaceable.placeRelative(rippleX, rippleY)
    }
}

private fun MeasureScope.placeLabelAndIcon(
    labelPlaceable: Placeable,
    iconPlaceable: Placeable,
    indicatorRipplePlaceable: Placeable,
    indicatorPlaceable: Placeable?,
    constraints: Constraints,
    alwaysShowLabel: Boolean,
    animationProgress: Float,
): MeasureResult {
    val contentHeight = NavigationBarWithLabelTopPadding.toPx() + iconPlaceable.height +
            IndicatorVerticalPadding.toPx() +
            NavigationBarIndicatorToLabelPadding.toPx() +
            labelPlaceable.height
    val contentVerticalPadding =
        ((constraints.minHeight - contentHeight) / 2).coerceAtLeast(IndicatorVerticalPadding.toPx())
    val height = contentHeight + contentVerticalPadding * 2

    // Icon (when selected) should be `contentVerticalPadding` from top
    val selectedIconY = contentVerticalPadding + NavigationBarWithLabelTopPadding.toPx()
    val unselectedIconY =
        if (alwaysShowLabel) selectedIconY else (height - iconPlaceable.height) / 2

    // How far the icon needs to move between unselected and selected states.
    val iconDistance = unselectedIconY - selectedIconY

    // The interpolated fraction of iconDistance that all placeables need to move based on
    // animationProgress.
    val offset = iconDistance * (1 - animationProgress)

    // Label should be fixed padding below icon
    val labelY =
        selectedIconY +
            iconPlaceable.height +
            IndicatorVerticalPadding.toPx() +
            NavigationBarIndicatorToLabelPadding.toPx()

    val containerWidth = constraints.maxWidth

    val labelX = (containerWidth - labelPlaceable.width) / 2
    val iconX = (containerWidth - iconPlaceable.width) / 2

    val rippleX = (containerWidth - indicatorRipplePlaceable.width) / 2
    val rippleY = selectedIconY - IndicatorVerticalPadding.toPx()

    return layout(containerWidth, height.roundToInt()) {
        indicatorPlaceable?.let {
            val indicatorX = (containerWidth - it.width) / 2
            val indicatorY = selectedIconY - IndicatorVerticalPadding.roundToPx()
            it.placeRelative(indicatorX, (indicatorY + offset).roundToInt())
        }
        if (alwaysShowLabel || animationProgress != 0f) {
            labelPlaceable.placeRelative(labelX, (labelY + offset).roundToInt())
        }
        iconPlaceable.placeRelative(iconX, (selectedIconY + offset).roundToInt())
        indicatorRipplePlaceable.placeRelative(rippleX, (rippleY + offset).roundToInt())
    }
}

private const val IndicatorRippleLayoutIdTag: String = "indicatorRipple"

private const val IndicatorLayoutIdTag: String = "indicator"

private const val IconLayoutIdTag: String = "icon"

private const val LabelLayoutIdTag: String = "label"

private val NavigationBarWithLabelTopPadding: Dp = 4.dp

private val NavigationBarWithoutLabelVerticalPadding: Dp = 12.dp

private const val ItemAnimationDurationMillis: Int = 100

private val NavigationBarItemHorizontalPadding: Dp = 4.dp

private val NavigationBarIndicatorToLabelPadding: Dp = 2.dp

private val ActiveIndicatorWidth: Dp = 64.0.dp

private val ActiveIndicatorHeight: Dp = 32.0.dp

private val IconSize: Dp = 24.0.dp

private val IndicatorHorizontalPadding: Dp =
    (ActiveIndicatorWidth - IconSize) / 2

private val IndicatorVerticalPadding: Dp =
    (ActiveIndicatorHeight - IconSize) / 2

private val IndicatorVerticalOffset: Dp = 12.dp
