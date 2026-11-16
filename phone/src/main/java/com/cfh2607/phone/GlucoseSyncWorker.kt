package com.cfh2607.phone

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class GlucoseSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) { // 改成繼承 Worker

    override fun doWork(): Result {
        val latestGlucose = 115.0f
        val dataMap = PutDataMapRequest.create("/glucose_data").apply {
            dataMap.putFloat("glucose_value", latestGlucose)
            dataMap.putString("trend", "Stable")
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        // 由於背景 Worker 可以使用同步方式或透過簡單的鎖來等待，
        // 最簡單的做法是直接利用 Wearable API 的 Task 進行同步等待（限 Worker 內）：
        try {
            val task = Wearable.getDataClient(applicationContext).putDataItem(dataMap)
            com.google.android.gms.tasks.Tasks.await(task) // 使用 Google Tasks 的同步等待
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}