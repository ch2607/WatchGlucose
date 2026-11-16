package com.cfh2607.phone

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlucoseDashboardScreen(
    timeInRangePct: Int = 100,
    avgGlucose: Int = 96,
    referenceRecords: List<Pair<Float, Float>> = emptyList(), // 手指扎針或 Stelo 數據 (時間 0~24h, 血糖 mg/dL)
    watchRecords: List<Pair<Float, Float>> = emptyList()     // 手錶 PPG + Offset 校準數據
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121216)) // 深色質感背景
            .padding(16.dp)
    ) {
        // 1. 頂部摘要卡片
        SummarySection(timeInRangePct = timeInRangePct, avgGlucose = avgGlucose)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 24小時血糖對比趨勢圖
        GlucoseTrendChart(referenceRecords = referenceRecords, watchRecords = watchRecords)

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 狀態資訊卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("黃金對照: 手指/Stelo", color = Color.White, fontSize = 13.sp)
                Text("藍點: 手錶估算 (+15)", color = Color(0xFF42A5F5), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SummarySection(timeInRangePct: Int, avgGlucose: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("TIME IN RANGE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("$timeInRangePct%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("FOR THE DAY", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AVERAGE GLUCOSE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$avgGlucose", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(" mg/dL", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("FOR THE DAY", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun GlucoseTrendChart(
    referenceRecords: List<Pair<Float, Float>>,
    watchRecords: List<Pair<Float, Float>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val w = size.width
            val h = size.height

            // 70 ~ 140 mg/dL 目標血糖區間背景 (50~200 mg/dL 座標轉換)
            val minG = 50f
            val maxG = 200f
            val y70 = h * (1 - (70f - minG) / (maxG - minG))
            val y140 = h * (1 - (140f - minG) / (maxG - minG))

            // 畫出綠色目標範圍
            drawRect(
                color = Color(0xFF1B382B),
                topLeft = Offset(0f, y140),
                size = Size(w, y70 - y140)
            )

            // 畫出黃金標準對照數據 (白點)
            referenceRecords.forEach { (time, value) ->
                val x = w * (time / 24f)
                val y = h * (1 - (value - minG) / (maxG - minG))
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
            }

            // 畫出手錶估算數據 (藍點) 並標註數值
            watchRecords.forEach { (time, value) ->
                val x = w * (time / 24f)
                val y = h * (1 - (value - minG) / (maxG - minG))

                // 1. 畫出藍色圓點
                drawCircle(color = Color(0xFF42A5F5), radius = 5.dp.toPx(), center = Offset(x, y))

                // 2. 在藍點正上方繪製對應數值標籤
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE // 文字顏色設為白色以便在深色背景清晰辨識
                        textSize = 26f                       // 文字大小
                        textAlign = Paint.Align.CENTER // 置中對齊
                        isAntiAlias = true
                    }
                    // 數值轉為整數顯示，並往上方偏移 25 像素避免與圓點重疊
                    drawText("${value.toInt()}", x, y - 25f, paint)
                }
            }
        }
    }
}