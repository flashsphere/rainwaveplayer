package com.flashsphere.rainwaveplayer.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flashsphere.rainwaveplayer.R
import com.flashsphere.rainwaveplayer.ui.theme.AppTypography

@Composable
fun LoadingItem() {
    Box(
        modifier = Modifier.fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        CircularProgressIndicator(Modifier.width(48.dp))
    }
}

@Composable
fun ErrorItem(message: String, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = AppTypography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Button(onClick = onRetryClick) {
            Text(text = stringResource(id = R.string.action_retry))
        }
    }
}
