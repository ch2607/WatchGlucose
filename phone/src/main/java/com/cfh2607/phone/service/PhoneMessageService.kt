package com.cfh2607.phone.service

import android.util.Log
import com.cfh2607.phone.data.MeasurementRecord // ✅ 修正：引入正確的 Room Entity
import com.cfh2607.phone.data.PhoneDatabase
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.wearable.MessageEvent

// 💡 Service 必須擁有無參數的預設建構子
class PhoneMessageService : WearableListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        Log.d("hsieh", "收到訊息 path=${messageEvent.path}")

        if (messageEvent.path == "/glucose_data_path") {
            val text = String(messageEvent.data, Charsets.UTF_8)
            val parts = text.split(",")

            if (parts.size >= 2) {
                val fingerstick = parts[0].toDoubleOrNull() ?: 0.0

                // ✅ 修正：建立 Room 的 MeasurementRecord 實體
                val record = MeasurementRecord(
                    timestamp = System.currentTimeMillis(),
                    predictedGlucose = fingerstick,
                    green = 0f,
                    red = 0f,
                    ir = 0f
                )

                serviceScope.launch {
                    val db = PhoneDatabase.getDatabase(applicationContext)

                    // 存入 Room 資料庫
                    db.measurementDao().insertRecord(record)
                    Log.d("hsieh", "已成功將接收到的血糖記錄存入 Room 資料庫")
                }
            }
        }
    }
}