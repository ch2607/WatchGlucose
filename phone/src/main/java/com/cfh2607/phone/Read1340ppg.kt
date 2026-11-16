package com.cfh2607.watchglucose

import android.content.Context
import android.util.Log
import java.io.File

// 定義單一高頻 PPG 取樣點的資料結構
data class PpgSamplePoint(
    val timestamp: Long,
    val green: Int,
    val red: Int,
    val ir: Int,
    val fingerstickGlucose: Int
)

object CsvUtils {
    private const val TAG = "hsieh"

    fun loadHighFrequencyPpgData(context: Context, fileName: String? = null): List<PpgSamplePoint> {
        val ppgList = mutableListOf<PpgSamplePoint>()

        // 1. 直接鎖定 calibration 子資料夾
        val calibrationDir = File(context.filesDir, "calibration")

        // 為了除錯，先把 calibration 資料夾底下的所有檔案印出來看看
        if (calibrationDir.exists() && calibrationDir.isDirectory) {
            val subFiles = calibrationDir.listFiles()
            Log.d(TAG, "calibration 資料夾底下的檔案: ${subFiles?.joinToString { it.name } ?: "空的"}")
        } else {
            Log.d(TAG, "calibration 資料夾不存在！")
            return ppgList
        }

        // 2. 尋找目標檔案
        // 2. 尋找目標檔案（明確排除舊的 summary 檔案，只抓取時間戳記高頻檔）
        val targetFile = if (!fileName.isNullOrEmpty()) {
            File(calibrationDir, fileName)
        } else {
            // 自動過濾出真正的 1340 筆高頻 CSV 檔案（排除舊的 glucose_calibration_data.csv）
            calibrationDir.listFiles { _, name ->
                name.startsWith("glucose_calibration_") && name != "glucose_calibration_data.csv" && name.endsWith(".csv")
            }?.maxByOrNull { it.lastModified() }
        }

        if (targetFile == null || !targetFile.exists()) {
            Log.d(TAG, "在 calibration 資料夾中找不到對應的 CSV 檔案")
            return ppgList
        }

        try {
            val lines = targetFile.readLines()
            Log.d(TAG, "成功讀取檔案: ${targetFile.name}, 總行數: ${lines.size} 筆")

            if (lines.size <= 1) return ppgList

            // 從第 1 行開始，讀取 1340 筆高頻數據

            for (i in 1 until lines.size) {
                val tokens = lines[i].split(",")
                if (tokens.size >= 5) {
                    val timestamp = tokens[0].toLongOrNull() ?: 0L
                    val green = tokens[1].toIntOrNull() ?: 0
                    val red = tokens[2].toIntOrNull() ?: 0
                    val ir = tokens[3].toIntOrNull() ?: 0
                    val glucose = tokens[4].toIntOrNull() ?: 100

                    // 💡 嚴格過濾：光學數值必須大於 0（排除標題行或格式錯亂產生的負數/0）
                    if (green > 0 && red > 0 && ir > 0) {
                        ppgList.add(
                            PpgSamplePoint(
                                timestamp = timestamp,
                                green = green,
                                red = red,
                                ir = ir,
                                fingerstickGlucose = glucose
                            )
                        )
                    }
                }
            }
            Log.d(TAG, "成功解析並載入 ${ppgList.size} 筆高頻 PPG 原始數據！")
        } catch (e: Exception) {
            Log.e(TAG, "解析高頻 CSV 發生錯誤: ${e.message}", e)
        }

        return ppgList
    }
}