package com.cfh2607.phone // 請根據您的專案 package 名稱調整

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

// 1. 圖片中對應的結構化特徵摘要 Data Class
data class PpgFeatureSummary(
    val timestamp: Long,
    val dateString: String,
    val greenMean: Float,
    val redMean: Float,
    val irMean: Float,
    val greenSd: Float,
    val redSd: Float,
    val irSd: Float,
    val fingerstickGlucose: Int,
    val estimatedGlucose: Float // 預測血糖或校準參考值
)

// 2. 批次載入器與特徵萃取核心
object MultiFileCsvLoader {

    private const val TAG = "MultiFileCsvLoader"

    fun loadAllCalibrationTrends(context: Context): List<PpgFeatureSummary> {
        val calibrationDir = File(context.filesDir, "calibration")
        if (!calibrationDir.exists() || !calibrationDir.isDirectory) {
            Log.d(TAG, "calibration 資料夾不存在")
            return emptyList()
        }

        // 掃描所有時間戳記開頭的 CSV 檔案並依照檔名（時間先後）排序
        val files = calibrationDir.listFiles { _, name ->
            name.startsWith("glucose_calibration_") && name.endsWith(".csv")
        }?.sortedBy { it.name } ?: emptyList()

        val summaryList = mutableListOf<PpgFeatureSummary>()

        for (file in files) {
            try {
                val lines = file.readLines()
                val summary = extractFeaturesFromCsv(lines, file.name)
                if (summary != null) {
                    summaryList.add(summary)
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析檔案失敗: ${file.name}, ${e.message}")
            }
        }

        Log.d(TAG, "成功載入 ${summaryList.size} 筆歷史特徵摘要")
        return summaryList
    }

    // 核心萃取函式：將 2000+ 筆高頻數據濃縮為 Mean 與 SD 摘要
    private fun extractFeaturesFromCsv(fileContent: List<String>, fileName: String): PpgFeatureSummary? {
        if (fileContent.size <= 1) return null

        val greenList = mutableListOf<Float>()
        val redList = mutableListOf<Float>()
        val irList = mutableListOf<Float>()
        var recordedGlucose = 100

        // 從檔名解析時間戳記
        val fileNameWithoutExt = fileName.removeSuffix(".csv")
        val timestampStr = fileNameWithoutExt.removePrefix("glucose_calibration_")
        val timestamp = timestampStr.toLongOrNull() ?: System.currentTimeMillis()

        // 略過標題行，解析每一筆數據
        for (i in 1 until fileContent.size) {
            val parts = fileContent[i].split(",")
            if (parts.size >= 5) {
                parts[1].toFloatOrNull()?.let { greenList.add(it) }
                parts[2].toFloatOrNull()?.let { redList.add(it) }
                parts[3].toFloatOrNull()?.let { irList.add(it) }
                recordedGlucose = parts[4].toIntOrNull() ?: recordedGlucose
            }
        }

        if (greenList.isEmpty() || redList.isEmpty() || irList.isEmpty()) return null

        // 計算各通道的 Mean 與 SD
        val gMean = greenList.average().toFloat()
        val gSd = sqrt(greenList.map { (it - gMean) * (it - gMean) }.average().toFloat())

        val rMean = redList.average().toFloat()
        val rSd = sqrt(redList.map { (it - rMean) * (it - rMean) }.average().toFloat())

        val iMean = irList.average().toFloat()
        val iSd = sqrt(irList.map { (it - iMean) * (it - iMean) }.average().toFloat())

        // 簡單的推論或暫時以指尖血糖/特徵對應作為展示數值
        val estimated = (0.85f * gMean) - (0.30f * gSd) + 72.0f

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeFormatted = dateFormat.format(Date(timestamp))

        return PpgFeatureSummary(
            timestamp = timestamp,
            dateString = timeFormatted,
            greenMean = gMean,
            redMean = rMean,
            irMean = iMean,
            greenSd = gSd,
            redSd = rSd,
            irSd = iSd,
            fingerstickGlucose = recordedGlucose,
            estimatedGlucose = estimated.coerceIn(40f, 400f)
        )
    }
}

// 3. Stelo 風格的 Jetpack Compose 趨勢圖 UI 元件
@Composable
fun SteloTrendChart(context: Context) {
    val trendPoints = remember { MultiFileCsvLoader.loadAllCalibrationTrends(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "連續血糖趨勢 (Stelo Style)", color = Color.Green)
        Spacer(modifier = Modifier.height(12.dp))

        if (trendPoints.isEmpty()) {
            Text(text = "尚無歷史記錄，請先透過手錶進行測量", color = Color.Gray)
            return@Column
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height

            if (trendPoints.size < 2) {
                drawCircle(color = Color.Green, radius = 8f, center = Offset(width / 2f, height / 2f))
                return@Canvas
            }

            val maxVal = (trendPoints.maxOf { it.estimatedGlucose }).coerceAtLeast(200f)
            val minVal = trendPoints.minOf { it.estimatedGlucose }.coerceAtLeast(40f)
            val range = (maxVal - minVal).coerceAtLeast(50f)

            val stepX = width / (trendPoints.size - 1)
            val path = Path()

            trendPoints.forEachIndexed { index, point ->
                val x = index * stepX
                val normalizedY = (point.estimatedGlucose - minVal) / range
                val y = height - (normalizedY * height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // 繪製每個測量點的光点
                drawCircle(
                    color = Color(0xFF00FF66),
                    radius = 6f,
                    center = Offset(x, y)
                )
            }

            // 繪製綠色平滑趨勢曲線
            drawPath(
                path = path,
                color = Color(0xFF00FF66),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}