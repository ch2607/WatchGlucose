package com.cfh2607.phone.service

import android.util.Log
import com.cfh2607.phone.data.PhoneDatabase
import com.cfh2607.phone.data.MeasurementRecord // ✅ 修正：引入正確的 Room Entity
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class PhoneDataListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "hsieh"
        private const val SYNC_PATH = "/glucose_data_sync"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SERVICE CREATED")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "DATA CHANGED")
        for (event in dataEvents) {
            if (event.dataItem.uri.path == SYNC_PATH) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val asset = dataMapItem.dataMap.getAsset("csv_file")

                if (asset != null) {
                    processCsvFromWatch(asset)
                }
            }
        }
    }

    private fun processCsvFromWatch(asset: com.google.android.gms.wearable.Asset) {
        val appContext = this@PhoneDataListenerService.applicationContext
        val client = com.google.android.gms.wearable.Wearable.getDataClient(this)

        client.getFdForAsset(asset).addOnSuccessListener { response ->
            response.inputStream.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                if (lines.size > 1) {
                    val dataRows = lines.drop(1)
                    val sessions = dataRows.chunked(3000)

                    Log.d(TAG, "總共偵測到 ${sessions.size} 個獨立測量區段（每段約 3000 行）")

                    for ((index, sessionLines) in sessions.withIndex()) {
                        var sumGreen = 0.0
                        var sumRed = 0.0
                        var sumIr = 0.0
                        var sampleCount = 0
                        var predictedVal = 0.0
                        val sessionTimestamp = System.currentTimeMillis() + index

                        for (line in sessionLines) {
                            val parts = line.split(",")
                            if (parts.size >= 6) {
                                sumGreen += parts[1].toDoubleOrNull() ?: 0.0
                                sumRed += parts[2].toDoubleOrNull() ?: 0.0
                                sumIr += parts[3].toDoubleOrNull() ?: 0.0
                                predictedVal = parts[5].toDoubleOrNull() ?: 0.0
                                sampleCount++
                            }
                        }

                        if (sampleCount > 0) {
                            val avgGreen = sumGreen / sampleCount
                            val avgRed = sumRed / sampleCount
                            val avgIr = sumIr / sampleCount

                            Log.d(TAG, "第 ${index + 1} 筆解析完成: 樣本數 $sampleCount, 預測值: $predictedVal")

                            // 3. 每一筆獨立存入 Room 資料庫
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = PhoneDatabase.getDatabase(appContext)

                                // ✅ 修正：使用 MeasurementRecord 實體存入 Room
                                val record = MeasurementRecord(
                                    timestamp = sessionTimestamp,
                                    predictedGlucose = predictedVal,
                                    green = avgGreen.toFloat(),
                                    red = avgRed.toFloat(),
                                    ir = avgIr.toFloat()
                                )

                                database.measurementDao().insertRecord(record)
                                Log.d(TAG, "第 ${index + 1} 筆已成功存入 Room 資料庫")
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "取得 Wearable Asset 失敗", e)
        }
    }
}