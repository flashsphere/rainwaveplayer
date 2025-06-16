package com.flashsphere.rainwaveplayer.ui.drawer.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawerItemBorder
import androidx.tv.material3.NavigationDrawerItemColors
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerItemGlow
import androidx.tv.material3.NavigationDrawerItemScale
import androidx.tv.material3.NavigationDrawerItemShape
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceColors
import androidx.tv.material3.SurfaceDefaults

@Composable
fun NavigationDrawerScope.TvNavigationDrawerItem(
    selected: Boolean = false,
    onClick: () -> Unit,
    leadingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = NavigationDrawerItemDefaults.NavigationDrawerItemElevation,
    shape: NavigationDrawerItemShape = NavigationDrawerItemDefaults.shape(),
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    scale: NavigationDrawerItemScale = NavigationDrawerItemScale.None,
    border: NavigationDrawerItemBorder = NavigationDrawerItemDefaults.border(),
    glow: NavigationDrawerItemGlow = NavigationDrawerItemDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val animatedWidth by animateDpAsState(
        targetValue =
        if (hasFocus) {
            NavigationDrawerItemDefaults.ExpandedDrawerItemWidth
        } else {
            NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
        },
        label = "NavigationDrawerItem width open/closed state of the drawer item"
    )
    val navDrawerItemHeight =
        if (supportingContent == null) {
            NavigationDrawerItemDefaults.ContainerHeightOneLine
        } else {
            NavigationDrawerItemDefaults.ContainerHeightTwoLine
        }
    TvNavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit,
            ) {
                content()
            }
        },
        leadingContent = leadingContent?.let {
            {
                Box(Modifier.size(NavigationDrawerItemDefaults.IconSize)) { it() }
            }
        },
        trailingContent = trailingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        supportingContent = supportingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        modifier = modifier.layout { measurable, constraints ->
            val width = animatedWidth.roundToPx()
            val height = navDrawerItemHeight.roundToPx()
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = height,
                        maxHeight = height,
                    )
                )
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        },
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        interactionSource = interactionSource,
    )
}

@Composable
private fun NavigationDrawerScope.TvNavigationDrawerItem(
    selected: Boolean,
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = ListItemDefaults.TonalElevation,
    shape: NavigationDrawerItemShape,
    colors: NavigationDrawerItemColors,
    scale: NavigationDrawerItemScale,
    border: NavigationDrawerItemBorder,
    glow: NavigationDrawerItemGlow,
    interactionSource: MutableInteractionSource? = null
) {
    val semanticModifier = Modifier.semantics(mergeDescendants = true) { this.selected = selected }.then(modifier)

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = semanticModifier,
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = shape.toSelectableSurfaceShape(),
        colors = colors.toSelectableSurfaceColors(hasFocus),
        scale = scale.toSelectableSurfaceScale(),
        border = border.toSelectableSurfaceBorder(),
        glow = glow.toSelectableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        TvNavigationDrawerItemContent(
            headlineContent = headlineContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
        )
    }
}

@Composable
fun NavigationDrawerScope.TvNavigationDrawerItem(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = NavigationDrawerItemDefaults.NavigationDrawerItemElevation,
    shape: NavigationDrawerItemShape = NavigationDrawerItemDefaults.shape(),
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    border: NavigationDrawerItemBorder = NavigationDrawerItemDefaults.border(),
    glow: NavigationDrawerItemGlow = NavigationDrawerItemDefaults.glow(),
    content: @Composable () -> Unit,
) {
    val animatedWidth by animateDpAsState(
        targetValue =
        if (hasFocus) {
            NavigationDrawerItemDefaults.ExpandedDrawerItemWidth
        } else {
            NavigationDrawerItemDefaults.CollapsedDrawerItemWidth
        },
        label = "NavigationDrawerItem width open/closed state of the drawer item"
    )
    val navDrawerItemHeight =
        if (supportingContent == null) {
            NavigationDrawerItemDefaults.ContainerHeightOneLine
        } else {
            NavigationDrawerItemDefaults.ContainerHeightTwoLine
        }
    TvNavigationDrawerItem(
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit,
            ) {
                content()
            }
        },
        leadingContent = leadingContent?.let {
            {
                Box(Modifier.size(NavigationDrawerItemDefaults.IconSize)) { it() }
            }
        },
        trailingContent = trailingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        supportingContent = supportingContent?.let {
            {
                AnimatedVisibility(
                    visible = hasFocus,
                    enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                    exit = NavigationDrawerItemDefaults.ContentAnimationExit,
                ) {
                    it()
                }
            }
        },
        modifier = modifier.layout { measurable, constraints ->
            val width = animatedWidth.roundToPx()
            val height = navDrawerItemHeight.roundToPx()
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = height,
                        maxHeight = height,
                    )
                )
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        },
        tonalElevation = tonalElevation,
        shape = shape,
        colors = colors,
        border = border,
        glow = glow,
    )
}

@Composable
private fun NavigationDrawerScope.TvNavigationDrawerItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = ListItemDefaults.TonalElevation,
    shape: NavigationDrawerItemShape,
    colors: NavigationDrawerItemColors,
    border: NavigationDrawerItemBorder,
    glow: NavigationDrawerItemGlow,
) {
    Surface(
        modifier = modifier,
        tonalElevation = tonalElevation,
        shape = shape.shape,
        colors = colors.toSurfaceColors(hasFocus),
        border = border.border,
        glow = glow.glow,
    ) {
        TvNavigationDrawerItemContent(
            headlineContent = headlineContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
        )
    }
}

@Composable
private fun TvNavigationDrawerItemContent(
    headlineContent: @Composable () -> Unit,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val minHeight = minHeight(
        hasLeadingContent = leadingContent != null,
        hasSupportingContent = supportingContent != null,
        hasOverlineContent = overlineContent != null
    )
    Row(
        modifier = Modifier.defaultMinSize(minHeight = minHeight).padding(ContentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.defaultMinSize(minWidth = ListItemDefaults.IconSize, minHeight = ListItemDefaults.IconSize)
                    .graphicsLayer { alpha = LeadingContentOpacity },
                contentAlignment = Alignment.Center,
                content = leadingContent
            )
            Spacer(modifier = Modifier.padding(end = LeadingContentEndPadding))
        } else {
            Spacer(modifier = Modifier.width(8.dp).height(ListItemDefaults.IconSize))
        }

        Box(Modifier.weight(1f).align(Alignment.CenterVertically)) {
            Column {
                overlineContent?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides LocalContentColor.current.copy(
                            alpha = OverlineContentOpacity
                        )
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.labelSmall,
                            content = it
                        )
                    }
                }

                ProvideTextStyle(value = MaterialTheme.typography.titleSmall, content = headlineContent)

                supportingContent?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides LocalContentColor.current.copy(
                            alpha = SupportingContentOpacity
                        )
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.bodySmall,
                            content = it
                        )
                    }
                }
            }
        }

        trailingContent?.let {
            Box(
                modifier = Modifier.padding(start = TrailingContentStartPadding)
            ) {
                CompositionLocalProvider(LocalContentColor provides LocalContentColor.current) {
                    ProvideTextStyle(value = MaterialTheme.typography.labelSmall, content = it)
                }
            }
        }
    }
}

private val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

private const val LeadingContentOpacity = 0.8f
private const val OverlineContentOpacity = 0.6f
private const val SupportingContentOpacity = 0.8f

private val LeadingContentEndPadding = 8.dp
private val TrailingContentStartPadding = 8.dp

private val MinContainerHeight = 48.dp
private val MinContainerHeightLeadingContent = 56.dp
private val MinContainerHeightTwoLine = 64.dp
private val MinContainerHeightThreeLine = 80.dp

private fun minHeight(
    hasLeadingContent: Boolean,
    hasSupportingContent: Boolean,
    hasOverlineContent: Boolean,
): Dp {
    return when {
        hasSupportingContent && hasOverlineContent -> {
            MinContainerHeightThreeLine
        }
        hasSupportingContent || hasOverlineContent -> {
            MinContainerHeightTwoLine
        }
        hasLeadingContent -> {
            MinContainerHeightLeadingContent
        }
        else -> {
            MinContainerHeight
        }
    }
}

@Composable
private fun NavigationDrawerItemShape.toSelectableSurfaceShape() =
    SelectableSurfaceDefaults.shape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
    )

@Composable
fun NavigationDrawerItemColors.toSurfaceColors(
    doesNavigationDrawerHaveFocus: Boolean
): SurfaceColors =
    SurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = if (doesNavigationDrawerHaveFocus) contentColor else inactiveContentColor,
    )

@Composable
fun NavigationDrawerItemColors.toSelectableSurfaceColors(
    doesNavigationDrawerHaveFocus: Boolean
) =
    SelectableSurfaceDefaults.colors(
        containerColor = containerColor,
        contentColor = if (doesNavigationDrawerHaveFocus) contentColor else inactiveContentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = if (doesNavigationDrawerHaveFocus) disabledContentColor else disabledInactiveContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

fun NavigationDrawerItemScale.toSelectableSurfaceScale() =
    SelectableSurfaceDefaults.scale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
        selectedDisabledScale = disabledScale,
        focusedSelectedDisabledScale = focusedDisabledScale
    )

fun NavigationDrawerItemBorder.toSelectableSurfaceBorder() =
    SelectableSurfaceDefaults.border(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
        selectedDisabledBorder = disabledBorder,
        focusedSelectedDisabledBorder = focusedDisabledBorder
    )

fun NavigationDrawerItemGlow.toSelectableSurfaceGlow() =
    SelectableSurfaceDefaults.glow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )
