package com.cfh2607.watchglucose

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray

class WatchDataReceiverService : WearableListenerService() {

    private val TAG = "WatchDataReceiver"

    // 1. 監聽 DataClient 的資料同步事件（最穩固）
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "手錶收到 DataClient 資料變更事件！")

        for (event in dataEvents) {
            val uri = event.dataItem.uri
            if (uri.path == "/glucose_data_sync") {
                try {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val jsonStr = dataMapItem.dataMap.getString("history_json")

                    if (!jsonStr.isNullOrEmpty()) {
                        val jsonArray = JSONArray(jsonStr)
                        val records = mutableListOf<HistoricalRecord>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            records.add(
                                HistoricalRecord(
                                    green = obj.getDouble("green").toFloat(),
                                    red = obj.getDouble("red").toFloat(),
                                    ir = obj.getDouble("ir").toFloat(),
                                    glucose = obj.getDouble("glucose")
                                )
                            )
                        }

                        // 寫入手錶本地快取
                        WatchGlucosePredictor.updateLocalHistoryCache(applicationContext, records)
                        Log.d(TAG, "✅ 手錶成功透過 DataClient 接收並快取了 ${records.size} 筆歷史數據！")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析 DataClient 歷史資料失敗", e)
                }
            }
        }
    }

    // 2. 備用：監聽 MessageClient 即時訊息
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/sync_glucose_history_path") {
            Log.d(TAG, "手錶收到 MessageClient 訊息")
            // 如果有需要也可以在這裡處理
        }
    }
}