package com.flashsphere.rainwaveplayer.ui.sleeptimer

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.alertdialog.CustomAlertDialog
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography
import java.time.ZonedDateTime
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    show: MutableState<Boolean>,
    getExistingSleepTimer: () -> Long?,
    createSleepTimer: (hour: Int, minute: Int) -> Long,
    removeSleepTimer: () -> Unit,
) {
    if (!show.value) return

    val sleepTimerState = remember { mutableStateOf(getExistingSleepTimer()) }

    val sleepTimerEndTimeMillis = sleepTimerState.value
    if (sleepTimerEndTimeMillis == null) {
        TimePickerDialog(
            title = {
                Text(text = stringResource(id = R.string.sleep_timer),
                    style = AppTypography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp))
            },
            onDismissRequest = { show.value = false },
            onConfirmClick = { state ->
                val endTimeMillis = createSleepTimer(state.hour, state.minute)
                sleepTimerState.value = endTimeMillis
            }
        )
    } else {
        SavedSleepTimer(
            sleepTimerEndTimeMillis = sleepTimerEndTimeMillis,
            onDismissRequest = { show.value = false },
            onRemoveClick = {
                removeSleepTimer()
                show.value = false
            },
            onOkClick = { show.value = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmClick: (TimePickerState) -> Unit,
) {
    val currentTime = ZonedDateTime.now()
    val context = LocalContext.current
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )

    val clockLayout = rememberSaveable { mutableStateOf(true) }
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(IntrinsicSize.Min),
        title = null,
        content = {
            if (clockLayout.value) {
                val layoutType = TimePickerDefaults.layoutType()
                if (layoutType == TimePickerLayoutType.Horizontal) {
                    title()
                    TimePicker(state = timePickerState, layoutType = layoutType)
                } else {
                    Column {
                        title()
                        TimePicker(state = timePickerState, layoutType = layoutType)
                    }
                }
            } else {
                Column {
                    title()
                    TimeInput(state = timePickerState)
                }
            }
        },
        buttons = {
            TextButton(onClick = { clockLayout.value = !clockLayout.value }) {
                if (clockLayout.value) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.weight(1F))
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
            TextButton(onClick = { onConfirmClick(timePickerState) }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
    )
}

@Composable
private fun SavedSleepTimer(
    sleepTimerEndTimeMillis: Long,
    onDismissRequest: () -> Unit,
    onRemoveClick: () -> Unit,
    onOkClick: () -> Unit,
) {
    val context = LocalContext.current
    val sleepTimer = Date(sleepTimerEndTimeMillis)
    val formattedSleepTimerDate = DateFormat.getDateFormat(context).format(sleepTimer)
    val formattedSleepTimerTime = DateFormat.getTimeFormat(context).format(sleepTimer)

    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(id = R.string.sleep_timer))
        },
        content = {
            Text(text = stringResource(id = R.string.sleep_timer_stop, formattedSleepTimerDate, formattedSleepTimerTime),
                modifier = Modifier.padding(bottom = 16.dp))
        },
        buttons = {
            TextButton(onClick = onRemoveClick) {
                Text(text = stringResource(id = R.string.action_remove))
            }
            Spacer(Modifier.weight(1F))
            TextButton(onClick = onOkClick) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}
