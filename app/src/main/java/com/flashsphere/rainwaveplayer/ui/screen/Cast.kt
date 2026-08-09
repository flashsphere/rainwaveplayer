package com.flashsphere.rainwaveplayer.ui.screen

import android.Manifest.permission.ACCESS_LOCAL_NETWORK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.Tooltip
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min
import androidx.core.net.toUri

@Composable
fun CastButton() {
    Tooltip(stringResource(R.string.action_cast)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
            CastButtonSdk37()
        } else {
            AndroidView(
                factory = { ctx ->
                    MediaRouteButton(ctx).also {
                        CastButtonFactory.setUpMediaRouteButton(ctx, it)
                    }
                },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
@Composable
private fun CastButtonSdk37() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                ACCESS_LOCAL_NETWORK,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    var mediaRouteButton by remember { mutableStateOf<MediaRouteButton?>(null) }

    Box {
        AndroidView(
            factory = { ctx ->
                MediaRouteButton(ctx).also {
                    CastButtonFactory.setUpMediaRouteButton(ctx, it)
                    mediaRouteButton = it
                }
            },
        )

        if (!hasPermission) {
            var toast by remember { mutableStateOf<Toast?>(null) }

            LifecycleResumeEffect(Unit) {
                hasPermission = ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) ==
                    PackageManager.PERMISSION_GRANTED
                onPauseOrDispose {}
            }

            DisposableEffect(Unit) {
                onDispose {
                    toast?.cancel()
                    toast = null
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    hasPermission = true
                    mediaRouteButton?.showDialog()
                } else if (activity != null) {
                    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        ACCESS_LOCAL_NETWORK
                    )
                    if (!shouldShowRationale) {
                        runCatching {
                            activity.startActivity(Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "package:${activity.packageName}".toUri()
                            ))
                        }
                    }
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = true,
                            radius = 24.dp,
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        toast?.cancel()
                        toast = Toast.makeText(context, R.string.cast_permission_message, Toast.LENGTH_LONG)
                            .also { it.show() }
                        permissionLauncher.launch(ACCESS_LOCAL_NETWORK)
                    }
            ) {
            }
        }
    }
}

@Composable
fun CastInfo(modifier: Modifier = Modifier, castState: MutableStateFlow<String>) {
    val message = castState.collectAsStateWithLifecycle().value
    if (message.isEmpty()) return

    Surface(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .wrapContentWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyMedium,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        var maxWidthExcludingFab = constraints.maxWidth - 72.dp.roundToPx()
                        if (maxWidthExcludingFab < 0) {
                            maxWidthExcludingFab = constraints.maxWidth
                        }
                        val maxContainerWidth = min(maxWidthExcludingFab, 400.dp.roundToPx())
                        val measuredWidth = measurable.measure(constraints).width
                        val placeable = measurable.measure(
                            constraints.copy(maxWidth = min(measuredWidth, maxContainerWidth))
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun CastInfoPreview() {
    PreviewTheme {
        CastInfo(castState = MutableStateFlow("Casting to SHIELD - Playing"))
    }
}
