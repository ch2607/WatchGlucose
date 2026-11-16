package com.cfh2607.watchglucose.service

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

object WatchSyncHelper {

    // 1. 向手機發起同步請求
    suspend fun requestSyncFromPhone(context: Context) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/request_sync", byteArrayOf())
                    .await()
                Log.d("WatchSyncHelper", "已向手機發送主動同步請求 (/request_sync)")
            }
        } catch (e: Exception) {
            Log.e("WatchSyncHelper", "發送同步請求失敗", e)
        }
    }

    // 2. 當手錶完成日常感測（例如 30 秒 PPG 運算後），主動上傳數據給手機
    suspend fun sendGlucoseToPhone(context: Context, glucoseValue: Double, trend: String) {
        try {
            val payload = "$glucoseValue,$trend".toByteArray(Charsets.UTF_8)
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()

            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/glucose_data_path", payload)
                    .await()
                Log.d("WatchSyncHelper", "已成功將感測血糖數據上傳至手機: $glucoseValue")
            }
        } catch (e: Exception) {
            Log.e("WatchSyncHelper", "上傳血糖數據至手機失敗", e)
        }
    }
}