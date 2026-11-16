package com.cfh2607.phone

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActivityLogManager {
    private const val FOLDER_NAME = "activity_logs"

    /**
     * 儲存從 Samsung Health / 手錶端同步過來的運動/走路活動資料
     * @param context Context
     * @param activityType 運動類型 (例如 "Walking", "Running", "Workout")
     * @param durationMinutes 運動持續時間（分鐘）
     * @param calories 消耗大卡
     */
    fun saveActivityLog(
        context: Context,
        activityType: String = "Walking",
        durationMinutes: Int = 30,
        calories: Float = 100f
    ) {
        try {
            val dir = File(context.filesDir, FOLDER_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timeString = dateFormat.format(Date(timestamp))

            val file = File(dir, "activity_${timestamp}.csv")
            // 寫入 CSV 格式：timestamp,timeString,activityType,durationMinutes,calories
            file.writeText("timestamp,timeString,activityType,durationMinutes,calories\n$timestamp,$timeString,$activityType,$durationMinutes,$calories")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 載入所有歷史運動記錄，供儀表板或模型特徵分析使用
     */
    data class ActivityRecord(
        val timestamp: Long,
        val timeString: String,
        val activityType: String,
        val durationMinutes: Int,
        val calories: Float
    )

    fun loadAllActivityLogs(context: Context): List<ActivityRecord> {
        val list = mutableListOf<ActivityRecord>()
        try {
            val dir = File(context.filesDir, FOLDER_NAME)
            if (!dir.exists()) return list

            val files = dir.listFiles { _, name -> name.endsWith(".csv") } ?: return list
            for (file in files) {
                val lines = file.readLines()
                if (lines.size >= 2) {
                    val parts = lines[1].split(",")
                    if (parts.size >= 5) {
                        list.add(
                            ActivityRecord(
                                timestamp = parts[0].toLongOrNull() ?: 0L,
                                timeString = parts[1],
                                activityType = parts[2],
                                durationMinutes = parts[3].toIntOrNull() ?: 0,
                                calories = parts[4].toFloatOrNull() ?: 0f
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedBy { it.timestamp }
    }
}