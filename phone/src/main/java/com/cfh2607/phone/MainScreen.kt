package com.cfh2607.phone

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cfh2607.phone.utils.sendTestDataToWatch


@Composable
fun MainScreen(context: Context) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "WatchGlucose 手機控制端")

            // 點擊此按鈕即可將 125.5 mg/dL 發送到手錶
            Button(onClick = {
                sendTestDataToWatch(context)
            }) {
                Text(text = "發送測試血糖到手錶 (125.5)")
            }
        }
    }
}