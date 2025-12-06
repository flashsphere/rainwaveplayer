package com.flashsphere.rainwaveplayer.ui.screen

import android.widget.RatingBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.model.station.Station
import com.flashsphere.rainwaveplayer.ui.AppBarTitle
import com.flashsphere.rainwaveplayer.ui.AppError
import com.flashsphere.rainwaveplayer.ui.AppScaffold
import com.flashsphere.rainwaveplayer.ui.BackIcon
import com.flashsphere.rainwaveplayer.ui.PullToRefreshBox
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiScreenConfig
import com.flashsphere.rainwaveplayer.ui.composition.LocalUiSettings
import com.flashsphere.rainwaveplayer.ui.enterAlwaysScrollBehavior
import com.flashsphere.rainwaveplayer.ui.item.AlbumArt
import com.flashsphere.rainwaveplayer.ui.item.LibrarySongItem
import com.flashsphere.rainwaveplayer.ui.itemSpan
import com.flashsphere.rainwaveplayer.ui.navigation.AlbumDetail
import com.flashsphere.rainwaveplayer.ui.navigation.Navigator
import com.flashsphere.rainwaveplayer.ui.scrollToItem
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.flashsphere.rainwaveplayer.util.Formatter.formatRating
import com.flashsphere.rainwaveplayer.view.uistate.AlbumScreenState
import com.flashsphere.rainwaveplayer.view.uistate.event.SnackbarEvent
import com.flashsphere.rainwaveplayer.view.uistate.model.AlbumState
import com.flashsphere.rainwaveplayer.view.uistate.model.SongState
import com.flashsphere.rainwaveplayer.view.viewmodel.AlbumScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import androidx.appcompat.R as AppCompatR

@Composable
fun AlbumDetailScreen(
    navigator: Navigator,
    viewModel: AlbumScreenViewModel,
    stationFlow: StateFlow<Station?>,
    albumDetail: AlbumDetail,
) {
    val station = stationFlow.collectAsStateWithLifecycle().value ?: return
    LaunchedEffect(albumDetail) {
        viewModel.getAlbum(station, albumDetail)
    }
    LifecycleStartEffect(Unit) {
        viewModel.subscribeStateChangeEvents()
        onStopOrDispose { viewModel.unsubscribeStateChangeEvents() }
    }

    AlbumDetailScreen(
        albumScreenStateFlow = viewModel.albumScreenState,
        events = viewModel.snackbarEvents,
        onSongClick = { song -> viewModel.requestSong(song) },
        onFaveSongClick = { song -> viewModel.faveSong(song) },
        onRefresh = { viewModel.getAlbum() },
        onBackClick = { navigator.goBack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailScreen(
    albumScreenStateFlow: StateFlow<AlbumScreenState>,
    events: Flow<SnackbarEvent>,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
) {
    val albumScreenState = albumScreenStateFlow.collectAsStateWithLifecycle().value

    val scope = rememberCoroutineScope()
    val scrollToTop = remember { mutableStateOf(false) }
    val openFaqDialog = rememberSaveable { mutableStateOf(false) }
    val faqOnDismissRequest = remember {
        { openFaqDialog.value = false }
    }

    AppScaffold(
        navigationIcon = {
            BackIcon(onBackClick)
        },
        appBarContent = {
            AppBarTitle(
                title = albumScreenState.album?.name ?: "",
                onClick = { scope.launch { scrollToTop.value = true } },
            )
        },
        appBarScrollBehavior = enterAlwaysScrollBehavior(scrollToTop),
        snackbarEvents = events,
    ) {
        PullToRefreshBox(modifier = Modifier.fillMaxSize(),
            isRefreshing = albumScreenState.loading,
            onRefresh = onRefresh,
            contentAlignment = Alignment.TopCenter) {
            if (albumScreenState.loaded) {
                albumScreenState.album?.let { album ->
                    AlbumDetailList(
                        album = album,
                        openFaqDialog = { openFaqDialog.value = true },
                        onSongClick = onSongClick,
                        onFaveSongClick = onFaveSongClick,
                        scrollToTop = scrollToTop,
                    )
                }
            }
        }

        if (openFaqDialog.value) {
            CooldownFaqDialog(onDismissRequest = faqOnDismissRequest)
        }

        AlbumDetailError(
            albumScreenState = albumScreenState,
            snackbarHostState = snackbarHostState,
            onRetry = onRefresh
        )
    }
}

@Composable
private fun AlbumDetailList(
    album: AlbumState,
    openFaqDialog: () -> Unit,
    onSongClick: (song: SongState) -> Unit,
    onFaveSongClick: (song: SongState) -> Unit,
    scrollToTop: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val songs = album.songs
    val gridColumnCount = LocalUiScreenConfig.current.gridSpan

    LaunchedEffect(scrollToTop) {
        snapshotFlow { scrollToTop.value }
            .filter { it }
            .collect {
                scrollToTop.value = false
                scrollToItem(scope, gridState, 0)
            }
    }

    LazyVerticalGrid(modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = GridCells.Fixed(gridColumnCount),
        contentPadding = PaddingValues(bottom = 80.dp)) {
        item(span = itemSpan(gridColumnCount), key = album.key) {
            Box(modifier = Modifier.animateItem()) {
                AlbumInfo(album, openFaqDialog)
            }
        }
        itemsIndexed(items = songs, key = { _, item -> item.id }) { i, item ->
            Box(modifier = Modifier.animateItem()) {
                LibrarySongItem(song = item,
                    showGlobalRating = !LocalUiSettings.current.hideRatingsUntilRated,
                    onClick = onSongClick,
                    onFaveClick = onFaveSongClick)
            }
        }
    }
}

@Composable
private fun AlbumInfo(album: AlbumState, openFaqDialog: () -> Unit) {
    val itemPaddingDimen = LocalUiScreenConfig.current.itemPadding
    val margin = 16.dp

    val cooldownTextStyle = SpanStyle(fontSize = 11.sp)
    val cooldownTextSuperscriptStyle = SpanStyle(fontSize = 9.sp).copy(baselineShift = BaselineShift.Superscript)
    val cooldownText = if (album.cool) {
        buildAnnotatedString {
            withStyle(style = cooldownTextStyle) {
                append(stringResource(R.string.album_on_cooldown))
            }
            withStyle(style = cooldownTextSuperscriptStyle) {
                append("?")
            }
        }
    } else if (album.songsOnCooldown) {
        buildAnnotatedString {
            withStyle(style = cooldownTextStyle) {
                append(stringResource(R.string.some_songs_on_cooldown))
            }
            withStyle(style = cooldownTextSuperscriptStyle) {
                append("?")
            }
        }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()
        .padding(start = margin, top = margin, end = margin)) {
        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            AlbumArt(
                art = album.art,
                modifier = Modifier.size(LocalUiScreenConfig.current.albumDetailImageSize),
            )

            Column(modifier = Modifier.padding(start = itemPaddingDimen)) {
                SelectionContainer(modifier = Modifier.wrapContentSize()) {
                    Text(text = album.name, style = AppTypography.titleMedium)
                }
                if (album.ratingUser > 0F) {
                    Text(text = stringResource(R.string.rating_user, formatRating(album.ratingUser)),
                        style = AppTypography.bodyMedium,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(top = itemPaddingDimen)
                    )
                }
                if (cooldownText != null) {
                    Box(modifier = Modifier.wrapContentSize()
                        .padding(top = itemPaddingDimen)
                        .background(colorResource(id = R.color.cooldown_background))
                    ) {
                        Text(
                            text = cooldownText,
                            style = AppTypography.bodyMedium,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            modifier = Modifier.clickable(onClick = openFaqDialog),
                        )
                    }
                }
            }
        }
        AlbumRating(album = album)
    }
}

@Composable
private fun AlbumRating(album: AlbumState) {
    val itemPaddingDimen = LocalUiScreenConfig.current.itemPadding
    val formattedRating = formatRating(album.rating)

    val screenWidth = LocalUiScreenConfig.current.windowSize.width
    val histogramWidth = LocalUiScreenConfig.current.albumRatingHistogramSize
    val modifier = if (histogramWidth == 0.dp || histogramWidth >= screenWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(histogramWidth)
    }

    Row(modifier = modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Text(text = formattedRating,
            style = AppTypography.displayMedium,
            modifier = Modifier
                .wrapContentSize()
                .alignByBaseline()
                .padding(itemPaddingDimen, itemPaddingDimen, 0.dp, itemPaddingDimen)
        )
        Column(modifier = Modifier.padding(start = 16.dp).alignByBaseline()) {
            AndroidView(
                factory = { ctx ->
                    RatingBar(ctx, null, AppCompatR.attr.ratingBarStyleSmall,
                        AppCompatR.style.Widget_AppCompat_RatingBar_Small).apply {
                        setIsIndicator(true)
                        numStars = 5
                        stepSize = 0.1F
                    }
                },
                update = { view ->
                    view.rating = album.rating
                },
                modifier = Modifier.wrapContentSize()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = album.ratingCount.toString(),
                    style = AppTypography.bodySmall,
                    fontSize = 11.sp,
                    modifier = Modifier.wrapContentSize()
                )
                Spacer(Modifier.size(2.dp))
                Icon(imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(0.dp)
                )
            }
        }
        RatingHistogram(
            modifier = Modifier
                .padding(itemPaddingDimen)
                .padding(vertical = itemPaddingDimen)
                .padding(start = 8.dp),
            album = album,
        )
    }
}

@Composable
fun RatingHistogram(
    modifier: Modifier,
    album: AlbumState,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Spacer(modifier = Modifier
            .background(Color(0xFF8AC249))
            .fillMaxWidth(album.ratingDistribution.getOrDefault(5, 0F))
            .weight(0.2F)
        )
        Spacer(modifier = Modifier
            .background(Color(0xFFCCDB38))
            .fillMaxWidth(album.ratingDistribution.getOrDefault(4, 0F))
            .weight(0.2F)
        )
        Spacer(modifier = Modifier
            .background(Color(0xFFFFEA3A))
            .fillMaxWidth(album.ratingDistribution.getOrDefault(3, 0F))
            .weight(0.2F)
        )
        Spacer(modifier = Modifier
            .background(Color(0xFFFFB234))
            .fillMaxWidth(album.ratingDistribution.getOrDefault(2, 0F))
            .weight(0.2F)
        )
        Spacer(modifier = Modifier
            .background(Color(0xFFFF8B5A))
            .fillMaxWidth(album.ratingDistribution.getOrDefault(1, 0F))
            .weight(0.2F)
        )
    }
}

@Composable
private fun CooldownFaqDialog(onDismissRequest: () -> Unit = {}) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(id = R.string.faq_cooldown_what_is)) },
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = stringResource(id = R.string.faq_cooldown_blue_bg_is),
                    modifier = Modifier.padding(bottom = 16.dp))
                Text(text = stringResource(id = R.string.faq_cooldown_why_use_cooldown),
                    modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(text = stringResource(id = R.string.faq_cooldown_type_of_cooldown),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_example),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_cooldown_length),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1F))
                }
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_song),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = "One Winged Angel",
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_song_length),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                }
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_album),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = "Final Fantasy VII",
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_album_length),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                }
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_category),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = "Final Fantasy",
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_category_length),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                }
                Text(text = stringResource(id = R.string.faq_cooldown_cooldowns_depend_on),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_album_size),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_larger_album),
                        modifier = Modifier.weight(2F))
                }
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_rating),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_higher_rating),
                        modifier = Modifier.weight(2F))
                }
                Row {
                    Text(text = stringResource(id = R.string.faq_cooldown_recently_added),
                        modifier = Modifier.padding(end = 8.dp).weight(1F))
                    Text(text = stringResource(id = R.string.faq_cooldown_newer),
                        modifier = Modifier.weight(2F))
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}

@Composable
private fun AlbumDetailError(albumScreenState: AlbumScreenState,
                             snackbarHostState: SnackbarHostState,
                             onRetry: () -> Unit) {
    if (albumScreenState.error == null) return
    AppError(
        showAsSnackbar = albumScreenState.loaded,
        snackbarHostState = snackbarHostState,
        error = albumScreenState.error,
        onRetry = onRetry
    )
}

private class AlbumScreenPreviewProvider : PreviewParameterProvider<AlbumState> {
    override val values = sequenceOf(albumStateData[0], albumStateData[1])
}

@Preview
@Composable
private fun AlbumScreenPreview(
    @PreviewParameter(AlbumScreenPreviewProvider::class) album: AlbumState
) {
    PreviewTheme {
        AlbumDetailScreen(
            albumScreenStateFlow = remember { MutableStateFlow(AlbumScreenState.loaded(album)) },
            events = remember { MutableSharedFlow() },
            onSongClick = {},
            onFaveSongClick = {},
            onRefresh = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun CooldownFaqDialogPreview() {
    PreviewTheme {
        Surface {
            CooldownFaqDialog()
        }
    }
}
