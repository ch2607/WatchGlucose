package com.cfh2607.watchglucose

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


fun requestSyncFromWatch(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 取得所有已連線的手機節點
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                // 發送訊息給手機，路徑為 /request_sync
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/request_sync", "sync_please".toByteArray())
                    .await()
                Log.d("WatchMessage", "已成功向手機發送同步請求")
            }
        } catch (e: Exception) {
            Log.e("WatchMessage", "發送同步請求失敗", e)
        }
    }
}