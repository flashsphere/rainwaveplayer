package com.flashsphere.rainwaveplayer.ui.item.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Checkbox
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemBorder
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.ListItemGlow
import androidx.tv.material3.ListItemScale
import androidx.tv.material3.ListItemShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.SelectableSurfaceColors
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.SelectableSurfaceShape
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Fave
import com.flashsphere.rainwaveplayer.ui.FaveSong
import com.flashsphere.rainwaveplayer.ui.rating.tv.TvRatingText
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTv
import com.flashsphere.rainwaveplayer.ui.screen.PreviewTvTheme
import com.flashsphere.rainwaveplayer.ui.screen.songStateData
import com.flashsphere.rainwaveplayer.ui.theme.tv.TvAppTypography
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.RequestLineState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState

@Composable
fun TvListItem(
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    scale: ListItemScale = ListItemDefaults.scale(),
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (BoxScope.() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit) = {},
) {
    ListItem(
        modifier = modifier,
        selected = false,
        onClick = onClick,
        colors = colors,
        scale = scale,
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
    )
}

@Composable
fun TvListItem(
    modifier: Modifier = Modifier,
    scale: ListItemScale = ListItemDefaults.scale(),
    text: String,
    onClick: (() -> Unit) = {},
) {
    TvListItem(
        modifier = modifier,
        onClick = onClick,
        scale = scale,
        headlineContent = {
            Text(text = text)
        },
    )
}

@Composable
fun TvAlbumListItem(modifier: Modifier = Modifier, album: AlbumState,
                    showGlobalRating: Boolean, onClick: () -> Unit) {
    val background = if (album.cool) {
        Modifier.background(colorResource(R.color.cooldown_background).copy(alpha = 0.4F))
    } else {
        Modifier
    }
    TvListItem(
        modifier = modifier.fillMaxWidth().then(background),
        onClick = onClick,
        colors = ListItemDefaults.colors(
            focusedContainerColor = if (album.cool) {
                colorResource(R.color.cooldown_background)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(modifier = Modifier.weight(1F), text = album.name)
                TvRatingText(modifier = Modifier.padding(start = 12.dp), album = album, showGlobalRating = showGlobalRating)
            }
        }
    )
}

@Composable
fun TvRequestLineListItem(modifier: Modifier = Modifier, item: RequestLineState) {
    val title = StringBuilder().append(item.position).append(". ").append(item.username).toString()
    val albumName = item.albumName ?: stringResource(id = R.string.no_song_selected)
    TvListItem(
        modifier = modifier.fillMaxWidth(),
        onClick = {},
        headlineContent = {
            Text(text = title, style = TvAppTypography.bodyLarge,
                lineHeight = TvAppTypography.bodyLarge.fontSize)
            Text(text = albumName, modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                style = TvAppTypography.bodyMedium,
                lineHeight = TvAppTypography.bodyMedium.fontSize)
            if (item.songTitle != null) {
                Text(text = item.songTitle, modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                    style = TvAppTypography.bodyMedium,
                    lineHeight = TvAppTypography.bodyMedium.fontSize)
            }
        }
    )
}

@Composable
fun TvAlbumHeaderItem(
    modifier: Modifier = Modifier,
    album: AlbumState,
    onClick: ((album: AlbumState) -> Unit)?,
) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        if (onClick != null) {
            TvListItem(
                scale = ListItemDefaults.scale(focusedScale = 1.02F),
                headlineContent = {
                    Text(text = album.name)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                        contentDescription = null
                    )
                },
                onClick = { onClick(album) },
            )
        } else {
            Text(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        HorizontalDivider()
    }
}

@Composable
fun TvSongListItem(
    modifier: Modifier = Modifier,
    faveItemModifier: Modifier = Modifier,
    songItemModifier: Modifier = Modifier,
    song: SongState,
    showGlobalRating: Boolean,
    onClick: () -> Unit,
    onFaveClick: () -> Unit,
) {
    TvSongListItem(
        modifier = modifier,
        faveItemModifier = faveItemModifier,
        songItemModifier = songItemModifier,
        song = song,
        onClick = onClick,
        onFaveClick = onFaveClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(modifier = Modifier.weight(1F), text = song.title)
            TvRatingText(modifier = Modifier.padding(start = 12.dp), song = song, showGlobalRating = showGlobalRating)
        }
    }
}

@Composable
fun TvSongListItem(
    modifier: Modifier = Modifier,
    faveItemModifier: Modifier = Modifier,
    songItemModifier: Modifier = Modifier,
    song: SongState,
    onClick: () -> Unit,
    onFaveClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val background = if (song.cool) {
        Modifier.background(colorResource(R.color.cooldown_background).copy(alpha = 0.4F))
    } else {
        Modifier
    }
    Row(modifier = modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        TvFaveListItem(
            modifier = faveItemModifier.fillMaxHeight(),
            fave = FaveSong(song.favorite.value),
            onClick = onFaveClick,
        )
        ListItem(
            modifier = songItemModifier.weight(1F).then(background),
            selected = false,
            onClick = onClick,
            colors = ListItemDefaults.colors(
                focusedContainerColor = if (song.cool) {
                    colorResource(R.color.cooldown_background)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                focusedContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            headlineContent = {
                content()
            }
        )
    }
}

@Composable
fun TvFaveListItem(
    modifier: Modifier = Modifier,
    fave: Fave,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        selected = false,
        onClick = onClick,
        shape = toSelectableSurfaceShape(ListItemDefaults.shape()),
        colors = toSelectableSurfaceColors(ListItemDefaults.colors(
            contentColor = if (fave.favorite) {
                colorResource(fave.color)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContentColor = if (fave.favorite) {
                colorResource(fave.color)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )),
        scale = toSelectableSurfaceScale(ListItemDefaults.scale()),
        border = toSelectableSurfaceBorder(ListItemDefaults.border()),
        glow = toSelectableSurfaceGlow(ListItemDefaults.glow()),
    ) {
        Box(
            modifier = Modifier.align(Alignment.Center).defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(fave.drawable),
                contentDescription = stringResource(fave.contentDescription)
            )
        }
    }
}

@Composable
fun toSelectableSurfaceShape(shape: ListItemShape): SelectableSurfaceShape =
    SelectableSurfaceDefaults.shape(
        shape = shape.shape,
        focusedShape = shape.focusedShape,
        pressedShape = shape.pressedShape,
        selectedShape = shape.selectedShape,
        disabledShape = shape.disabledShape,
        focusedSelectedShape = shape.focusedSelectedShape,
        focusedDisabledShape = shape.focusedDisabledShape,
        pressedSelectedShape = shape.pressedSelectedShape,
        selectedDisabledShape = shape.disabledShape,
        focusedSelectedDisabledShape = shape.focusedDisabledShape
    )

@Composable
fun toSelectableSurfaceColors(colors: ListItemColors): SelectableSurfaceColors =
    SelectableSurfaceDefaults.colors(
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        focusedContainerColor = colors.focusedContainerColor,
        focusedContentColor = colors.focusedContentColor,
        pressedContainerColor = colors.pressedContainerColor,
        pressedContentColor = colors.pressedContentColor,
        selectedContainerColor = colors.selectedContainerColor,
        selectedContentColor = colors.selectedContentColor,
        disabledContainerColor = colors.disabledContainerColor,
        disabledContentColor = colors.disabledContentColor,
        focusedSelectedContainerColor = colors.focusedSelectedContainerColor,
        focusedSelectedContentColor = colors.focusedSelectedContentColor,
        pressedSelectedContainerColor = colors.pressedSelectedContainerColor,
        pressedSelectedContentColor = colors.pressedSelectedContentColor
    )

fun toSelectableSurfaceScale(scale: ListItemScale) =
    SelectableSurfaceDefaults.scale(
        scale = scale.scale,
        focusedScale = scale.focusedScale,
        pressedScale = scale.pressedScale,
        selectedScale = scale.selectedScale,
        disabledScale = scale.disabledScale,
        focusedSelectedScale = scale.focusedSelectedScale,
        focusedDisabledScale = scale.focusedDisabledScale,
        pressedSelectedScale = scale.pressedSelectedScale,
        selectedDisabledScale = scale.disabledScale,
        focusedSelectedDisabledScale = scale.focusedDisabledScale
    )

fun toSelectableSurfaceBorder(border: ListItemBorder) =
    SelectableSurfaceDefaults.border(
        border = border.border,
        focusedBorder = border.focusedBorder,
        pressedBorder = border.pressedBorder,
        selectedBorder = border.selectedBorder,
        disabledBorder = border.disabledBorder,
        focusedSelectedBorder = border.focusedSelectedBorder,
        focusedDisabledBorder = border.focusedDisabledBorder,
        pressedSelectedBorder = border.pressedSelectedBorder,
        selectedDisabledBorder = border.disabledBorder,
        focusedSelectedDisabledBorder = border.focusedDisabledBorder
    )

fun toSelectableSurfaceGlow(glow: ListItemGlow) =
    SelectableSurfaceDefaults.glow(
        glow = glow.glow,
        focusedGlow = glow.focusedGlow,
        pressedGlow = glow.pressedGlow,
        selectedGlow = glow.selectedGlow,
        focusedSelectedGlow = glow.focusedSelectedGlow,
        pressedSelectedGlow = glow.pressedSelectedGlow
    )

@PreviewTv
@Composable
private fun TvListItemPreview() {
    PreviewTvTheme {
        Surface {
            TvListItem(
                onClick = {},
                modifier = Modifier,
                leadingContent = {
                    RadioButton(selected = false, onClick = {})
                },
                headlineContent = {
                    Text(text = "Some headline content")
                },
                supportingContent = {
                    Text(text = "Some summary content")
                },
                trailingContent = {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {},
                    )
                },
            )
        }
    }
}

private class TvAlbumHeaderItemPreviewProvider : PreviewParameterProvider<AlbumState> {
    override val values: Sequence<AlbumState> = sequenceOf(AlbumState(
        id = 1,
        name = songStateData[3].albumName
    ), AlbumState(
        id = 2,
        name = songStateData[4].albumName
    ))
}

@PreviewTv
@Composable
private fun TvAlbumHeaderItemPreview(@PreviewParameter(TvAlbumHeaderItemPreviewProvider::class) album: AlbumState) {
    PreviewTvTheme {
        Surface {
            TvAlbumHeaderItem(album = album, onClick = {})
        }
    }
}

private class TvRequestLineListItemPreviewProvider : PreviewParameterProvider<RequestLineState> {
    override val values = sequenceOf(
        RequestLineState(
            userId = 444,
            username = "Laharl",
            position = 1,
            albumName = null,
            songTitle = null,
        ), RequestLineState(
            userId = 333,
            username = "some username some username some username some username some username ",
            position = 2,
            albumName = "Streets of Rage",
            songTitle = "The Street of Rage",
        )
    )
}

@PreviewTv
@Composable
private fun TvRequestLineListItemPreview(@PreviewParameter(TvRequestLineListItemPreviewProvider::class) item: RequestLineState) {
    PreviewTvTheme {
        Surface {
            TvRequestLineListItem(item = item)
        }
    }
}

private class TvSongListItemPreviewProvider : PreviewParameterProvider<SongState> {
    override val values = sequenceOf(songStateData[3], songStateData[1])
}

@PreviewTv
@Composable
private fun TvSongListItemPreview(@PreviewParameter(TvSongListItemPreviewProvider::class) song: SongState) {
    PreviewTvTheme {
        Surface {
            TvSongListItem(
                song = song,
                showGlobalRating = true,
                onClick = {},
                onFaveClick = {},
            )
        }
    }
}
