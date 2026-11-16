package com.cfh2607.watchglucose.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.* // ✅ Wear OS 專屬的 Material 佈景主題

@Composable
fun WatchGlucoseTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    MaterialTheme(
        content = content
    )
}