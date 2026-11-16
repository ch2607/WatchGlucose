package com.cfh2607.phone

import android.content.Context
import android.util.Log
import com.cfh2607.phone.data.MeasurementRecord // 💡 確保有匯入 MeasurementRecord
import com.cfh2607.phone.data.PhoneDatabase
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStreamReader

class PhoneDataSyncListener(private val context: Context) {

    private val TAG = "PhoneDataSync"

    fun startListening() {
        Wearable.getDataClient(context).addListener { dataEventBuffer ->
            for (event in dataEventBuffer) {
                if (event.dataItem.uri.path == "/glucose_data_sync") {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val asset = dataMapItem.dataMap.getAsset("csv_file")
                    val timestamp = dataMapItem.dataMap.getLong("timestamp")

                    if (asset != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val inputStream = Wearable.getDataClient(context).getFdForAsset(asset).await().inputStream
                                val reader = InputStreamReader(inputStream)
                                val lines = reader.readLines()

                                // 解析 CSV 內容（略過標題行第一行）
                                var sumPredicted = 0.0
                                var count = 0
                                var lastGreen = 0f
                                var lastRed = 0f
                                var lastIr = 0f

                                for (i in 1 until lines.size) {
                                    val tokens = lines[i].split(",")
                                    if (tokens.size >= 6) {
                                        lastGreen = tokens[1].toFloat()
                                        lastRed = tokens[2].toFloat()
                                        lastIr = tokens[3].toFloat()
                                        sumPredicted += tokens[5].toDouble()
                                        count++
                                    }
                                }

                                if (count > 0) {
                                    val avgPredicted = sumPredicted / count
                                    val database = PhoneDatabase.getDatabase(context)

                                    // ✅ 儲存至手機 Room 資料庫（使用 MeasurementRecord）
                                    database.measurementDao().insertRecord(
                                        MeasurementRecord(
                                            timestamp = timestamp,
                                            predictedGlucose = avgPredicted,
                                            green = lastGreen,
                                            red = lastRed,
                                            ir = lastIr
                                        )
                                    )
                                    Log.d(TAG, "✅ 成功將手錶預測紀錄存入手機 Room！平均預測: $avgPredicted")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ 解析手錶傳來的 CSV 失敗", e)
                            }
                        }
                    }
                }
            }
        }
    }
}