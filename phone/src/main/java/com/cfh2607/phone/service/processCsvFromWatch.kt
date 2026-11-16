package com.cfh2607.phone.service

import android.content.ContentValues.TAG
import android.util.Log
import com.cfh2607.phone.data.GlucoseEntity
import com.cfh2607.phone.data.PhoneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
/*
private fun processCsvFromWatch(asset: com.google.android.gms.wearable.Asset) {
    val client = com.google.android.gms.wearable.Wearable.getDataClient(this)
    client.getFdForAsset(asset).addOnSuccessListener { response ->
        // 💡 使用 use 區塊自動關閉串流，避免資源洩漏
        response.inputStream.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            if (lines.size > 1) {
                var sumGreen = 0.0
                var sumRed = 0.0
                var sumIr = 0.0
                var sumPredicted = 0.0 // 改為累加平均，避免被最後一行覆蓋
                var sampleCount = 0
                var fingerstickVal = 0.0
                val sessionTimestamp = System.currentTimeMillis()

                // 迴圈處理這單次傳來的 3000 行資料
                for (i in 1 until lines.size) {
                    val parts = lines[i].split(",")
                    if (parts.size >= 6) {
                        sumGreen += parts[1].toDoubleOrNull() ?: 0.0
                        sumRed += parts[2].toDoubleOrNull() ?: 0.0
                        sumIr += parts[3].toDoubleOrNull() ?: 0.0
                        fingerstickVal = parts[4].toDoubleOrNull() ?: 0.0
                        sumPredicted += parts[5].toDoubleOrNull() ?: 0.0
                        sampleCount++
                    }
                }

                if (sampleCount > 0) {
                    val avgGreen = sumGreen / sampleCount
                    val avgRed = sumRed / sampleCount
                    val avgIr = sumIr / sampleCount
                    val avgPredicted = sumPredicted / sampleCount // 計算平均預測值

                    val glucoseEntity = GlucoseEntity(
                        timestamp = sessionTimestamp,
                        actualGlucose = fingerstickVal,
                        predictedGlucose = avgPredicted,
                        predictionError = abs(avgPredicted - fingerstickVal),
                        green = avgGreen.toFloat(),
                        red = avgRed.toFloat(),
                        ir = avgIr.toFloat(),
                        activityState = "Watch Calibration Synced"
                    )

                   // Log.d(TAG, "解析成功: 總共平均了 $sampleCount 筆資料，預測值: $avgPredicted")

                    // 💡 在背景寫入 Room 資料庫
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = PhoneDatabase.getDatabase(applicationContext)
                        database.glucoseDao().insert(glucoseEntity)
                        Log.d(TAG, "已成功將此筆資料存入 Room 資料庫")
                    }
                }
            }
        }
    }.addOnFailureListener { e ->
        Log.e(TAG, "取得 Wearable Asset 失敗", e)
    } */
