package com.cfh2607.phone

import android.content.Context
import android.util.Log
import com.cfh2607.phone.data.MeasurementRecord // 💡 確保有匯入 MeasurementRecord
import com.cfh2607.phone.data.PhoneDatabase
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStreamReader


class PhoneDataReceiver : WearableListenerService() {

    companion object {
        private const val TAG = "PhoneDataReceiver"
        private const val SYNC_PATH = "/glucose_data_sync"
    }

    // 當手錶有 DataItem 傳過來時，Google Wearable 會自動觸發這個方法
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            if (event.dataItem.uri.path == SYNC_PATH) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val asset = dataMapItem.dataMap.getAsset("csv_file")
                val timestamp = dataMapItem.dataMap.getLong("timestamp")

                if (asset != null) {
                    handleIncomingCsvAsset(this, asset, timestamp)
                }
            }
        }
    }

    // 處理與解析手錶傳來的 CSV 數據，並存入 Room
    private fun handleIncomingCsvAsset(context: Context, asset: com.google.android.gms.wearable.Asset, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 透過 Wearable API 讀取手錶傳過來的 Asset 檔案內容
                val inputStream = Wearable.getDataClient(context).getFdForAsset(asset).await().inputStream
                val reader = InputStreamReader(inputStream)
                val lines = reader.readLines()

                var sumPredicted = 0.0
                var count = 0
                var lastGreen = 0f
                var lastRed = 0f
                var lastIr = 0f

                // 解析 CSV 資料（跳過第一行標題）
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

                    // 取得手機端的 Room 資料庫並寫入
                    val database = PhoneDatabase.getDatabase(context)
                    database.measurementDao().insertRecord(
                        MeasurementRecord( // ✅ 正確建立 Room 實體
                            timestamp = timestamp,
                            predictedGlucose = avgPredicted,
                            green = lastGreen,
                            red = lastRed,
                            ir = lastIr
                        )
                    )
                    Log.d(TAG, "✅ 成功將手錶傳來的 CSV 數據存入手機 Room！平均預測: $avgPredicted")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 解析手錶傳來的 CSV 失敗", e)
            }
        }
    }
}