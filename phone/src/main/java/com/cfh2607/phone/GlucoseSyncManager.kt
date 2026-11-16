package com.cfh2607.phone

import android.content.Context
import android.util.Log
import com.cfh2607.phone.data.PhoneDatabase
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object GlucoseSyncManager {

    private const val TAG = "GlucoseSyncManager"
    private const val PERSISTENT_PATH = "/glucose_history_persistent"

    fun syncHistoryViaDataClient(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "⚡ 開始透過 DataClient 同步持久化歷史資料...")
                val database = PhoneDatabase.getDatabase(context)

                // ✅ 從 Room 資料庫取得所有歷史紀錄清單（假設 DAO 回傳 Flow<List<MeasurementRecord>>）
                val allRecords = database.measurementDao().getAllMeasurements().first()

                if (allRecords.isEmpty()) {
                    Log.d(TAG, "❌ Room 裡面是空的，沒有歷史資料可同步！")
                    return@launch
                }

                val jsonArray = JSONArray()
                for (record in allRecords) {
                    val obj = JSONObject().apply {
                        put("green", record.green.toDouble())
                        put("red", record.red.toDouble())
                        put("ir", record.ir.toDouble())
                        put("glucose", record.predictedGlucose.toDouble()) // ✅ 使用正確的欄位 predictedGlucose
                    }
                    jsonArray.put(obj)
                }
                val jsonString = jsonArray.toString()

                // 建立持久化 DataMapRequest
                val request = PutDataMapRequest.create(PERSISTENT_PATH).apply {
                    dataMap.putString("history_json", jsonString)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(context)
                    .putDataItem(request)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ 成功將 ${allRecords.size} 筆歷史資料寫入 DataClient 儲存區！")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ DataClient 寫入失敗", e)
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 同步發生例外錯誤", e)
            }
        }
    }
}