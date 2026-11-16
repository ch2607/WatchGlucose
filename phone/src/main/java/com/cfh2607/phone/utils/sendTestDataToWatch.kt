package com.cfh2607.phone.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

fun sendTestDataToWatch(context: Context) {
    val dataClient = Wearable.getDataClient(context)
    val putDataMapRequest = com.google.android.gms.wearable.PutDataMapRequest.create("/glucose_data_path").apply {
        dataMap.putDouble("glucose_value", 125.5)
        dataMap.putLong("timestamp", System.currentTimeMillis())
        dataMap.putString("trend", "Stable")
    }
    val request = putDataMapRequest.asPutDataRequest().setUrgent()

    dataClient.putDataItem(request)
        .addOnSuccessListener {
            Log.d("PhoneManager", "成功發送測試血糖資料到手錶: 125.5")
        }
        .addOnFailureListener { e ->
            Log.e("PhoneManager", "發送測試血糖資料失敗", e)
        }
}