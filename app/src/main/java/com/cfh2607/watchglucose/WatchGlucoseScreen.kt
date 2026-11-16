package com.cfh2607.watchglucose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text


private val ScreenBlack = Color(0xFF000000)
private val Blue = Color(0xFF087BFF)
private val Cyan = Color(0xFF00D9E8)
private val Green = Color(0xFF31E86A)
private val Red = Color(0xFFFF4058)
private val Purple = Color(0xFFB45CFF)
private val Yellow = Color(0xFFFFC928)
private val White = Color(0xFFF5F7FF)

@Composable
fun WatchGlucoseScreen(
    connected: Boolean,
    measuring: Boolean,
    status: String,
    secondsLeft: Int,
    recordCount: Int,
    greenValue: Int,
    redValue: Int,
    irValue: Int,
    fingerstickGlucose: Int,
    calibrationMode: Boolean, // true = 校正功能模式, false = 即時預測功能模式
    predictedGlucose: Double?,
    onCalibrationModeChanged: (Boolean) -> Unit,
    onFingerstickChanged: (Int) -> Unit,
    onStartMeasurement: () -> Unit,
    onStopMeasurement: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
       // Log.d("hsieh", "WatchGlucoseScreen")
        // 1. 頂部寬版模式切換按鈕（點擊即可切換 校正模式 / 預測模式）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF172638))
                    .border(width = 1.5.dp, color = Cyan, shape = RoundedCornerShape(22.dp))
                    .clickable {
                        if (!measuring) {
                            onCalibrationModeChanged(!calibrationMode)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "⚙", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (calibrationMode) "目前模式：校正功能 (點擊切換)" else "目前模式：即時預測 (點擊切換)",
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 開始 / 停止測量按鈕
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00D9C5), Color(0xFF087BFF), Color(0xFF00B9FF))
                    )
                )
                .clickable {
                    if (measuring) {
                        onStopMeasurement()
                    } else {
                        onStartMeasurement()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (measuring) "■ 停止測量 (${secondsLeft}s)"
                else if (calibrationMode) "▶ 開始校正測量"
                else "▶ 開始即時預測",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. 根據 calibrationMode 顯示不同的專屬介面區塊
        if (calibrationMode) {
            // --- 模式 A：校正功能（顯示手指血糖調整） ---
            Log.d("hsieh", "WatchGlucoseScreen calibrationMode")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF101C2C))
                    .border(1.dp, Cyan, RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "輸入手指血糖 (校正用)",
                        color = Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$fingerstickGlucose mg/dL",
                        color = White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SmallControlButton("−") {
                            if (!measuring && fingerstickGlucose > 40) {
                                onFingerstickChanged(fingerstickGlucose - 1)
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        SmallControlButton("+") {
                            if (!measuring && fingerstickGlucose < 400) {
                                onFingerstickChanged(fingerstickGlucose + 1)
                            }
                        }
                    }
                }
            }
        } else {
            // --- 模式 B：即時預測功能（顯示預測結果） ---
           // Log.d("hsieh", "WatchGlucoseScreen 即時預測功能")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1B3B2B), Color(0xFF0F241A))
                        )
                    )
                    .border(1.dp, Green, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "即時預測血糖結果",
                        color = Green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (predictedGlucose != null) "${predictedGlucose.toInt()}" else "---",
                            color = White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "mg/dL", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. 狀態提示欄位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    measuring -> "正在測量中... ($secondsLeft 秒)"
                    status.isNotBlank() -> status
                    else -> "準備就緒"
                },
                color = if (connected) Cyan else Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun SmallControlButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF00C8FF), Color(0xFF075BFF))
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

