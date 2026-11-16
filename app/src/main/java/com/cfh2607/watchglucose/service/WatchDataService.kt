package com.cfh2607.watchglucose.service

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WatchDataService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uri = event.dataItem.uri
                // 當手機回傳 "/glucose_data" 時進行處理
                if (uri.path == "/glucose_data") {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val glucoseValue = dataMapItem.dataMap.getDouble("glucose_value")
                    val timestamp = dataMapItem.dataMap.getLong("timestamp")
                    val trend = dataMapItem.dataMap.getString("trend") ?: "Stable"

                    Log.d("WatchDataService", "手錶收到手機同步的血糖資料: $glucoseValue, 趨勢: $trend")

                    // 可以在這裡將資料存入手錶本地或更新 UI 畫面
                }
            }
        }
    }
}