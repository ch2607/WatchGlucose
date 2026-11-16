package com.cfh2607.phone

import android.content.Context
import android.util.Log
import java.io.File

object CalibrationFileUpdater {
    private const val TAG = "CalibrationFileUpdater"

    /**
     * 更新最新一筆 CSV 檔案的指尖血糖標籤
     */
    fun updateLatestCsvFingerstickGlucose(context: Context, newGlucose: Int): Boolean {
        val calibrationDir = File(context.filesDir, "calibration")
        if (!calibrationDir.exists()) return false

        // 找出最新的一個 CSV 檔案
        val files = calibrationDir.listFiles { _, name ->
            name.startsWith("glucose_calibration_") && name.endsWith(".csv")
        }?.sortedBy { it.name } ?: return false

        val latestFile = files.lastOrNull() ?: return false

        try {
            val lines = latestFile.readLines()
            if (lines.isEmpty()) return false

            val updatedLines = mutableListOf<String>()
            updatedLines.add(lines[0]) // 保留標題行 (Header)

            // 將該檔案內的所有高頻數據列之指尖血糖欄位更新為新的數值
            for (i in 1 until lines.size) {
                val parts = lines[i].split(",")
                if (parts.size >= 5) {
                    // 假設第 5 欄 (index 4) 為指尖血糖
                    val updatedRow = "${parts[0]},${parts[1]},${parts[2]},${parts[3]},$newGlucose"
                    updatedLines.add(updatedRow)
                } else {
                    updatedLines.add(lines[i])
                }
            }

            // 寫回檔案
            latestFile.writeText(updatedLines.joinToString("\n"))
            Log.d(TAG, "成功更新檔案 ${latestFile.name} 的指尖血糖標籤為: $newGlucose")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "更新 CSV 檔案失敗: ${e.message}")
            return false
        }
    }
}