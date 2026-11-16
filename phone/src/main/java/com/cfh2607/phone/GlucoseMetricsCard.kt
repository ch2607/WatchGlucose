package com.cfh2607.phone // 請依您實際的 package 名稱調整

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sqrt

// 定義評估結果資料結構
data class EvaluationResult(
    val mae: Float,
    val rmse: Float,
    val accuracyWithin20Pct: Float
)

// 計算 MAE 與準確率的函式
fun evaluateGlucoseAlgorithm(records: List<GlucoseRecord>): EvaluationResult {
    if (records.isEmpty()) return EvaluationResult(0f, 0f, 0f)

    var totalAbsoluteError = 0f
    var squaredErrorSum = 0f
    var withinSpecCount = 0

    for (record in records) {
        val actual = record.fingerstickGlucose.toFloat()

        // 假設從記錄中解析 PPG 特徵並進行預測（此處以您的演算法推論為代稱）
        val ppgList = record.ppgRawData.split(";").mapNotNull { it.toFloatOrNull() }
        val predicted = runGlucoseInference(ppgList).toFloat() // 請對應您實際的演算法函式

        val absoluteError = abs(predicted - actual)
        totalAbsoluteError += absoluteError
        squaredErrorSum += (predicted - actual) * (predicted - actual)

        // ISO 臨床準確度標準：±20% 或 ±20 mg/dL 內
        val allowedError = if (actual > 100) actual * 0.20f else 20f
        if (absoluteError <= allowedError) {
            withinSpecCount++
        }
    }

    val n = records.size.toFloat()
    return EvaluationResult(
        mae = totalAbsoluteError / n,
        rmse = sqrt(squaredErrorSum / n),
        accuracyWithin20Pct = (withinSpecCount / n) * 100f
    )
}
// 暫時的簡易推論函式（請依您實際的演算法替換）
fun runGlucoseInference(ppgList: List<Float>): Float {
    if (ppgList.isEmpty()) return 100f
    // 範例：根據 PPG 數據的平均值或特徵簡單計算預測血糖
    val avgPpg = ppgList.average().toFloat()
    return 90f + (avgPpg % 30f) // 示意用數值
}
// UI 卡片元件
@Composable
fun GlucoseMetricsCard(records: List<GlucoseRecord>) {
    // 計算評估指標
    val metrics = evaluateGlucoseAlgorithm(records)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "演算法效能評估 (目標 ≥80%)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "平均絕對誤差 (MAE)",
                    value = if (records.isNotEmpty()) "${String.format("%.1f", metrics.mae)} mg/dL" else "--"
                )
                MetricItem(
                    label = "臨床合格率 (±20%)",
                    value = if (records.isNotEmpty()) "${String.format("%.1f", metrics.accuracyWithin20Pct)}%" else "--"
                )
                MetricItem(
                    label = "有效樣本數",
                    value = "${records.size} 筆"
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}