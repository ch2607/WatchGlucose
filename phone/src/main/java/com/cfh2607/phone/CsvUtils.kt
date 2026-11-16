package com.cfh2607.phone
import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.io.File

fun loadGlucoseRecords(context: Context): List<GlucoseRecord> {
    val records = mutableListOf<GlucoseRecord>()
    val file = File(context.filesDir, "calibration/glucose_calibration_data.csv")

    if (!file.exists()) return records

    try {
        val lines = file.readLines()
        if (lines.size <= 1) return records

        // 從第二行開始（跳過標題），每一行都是獨立的一筆校準紀錄！
        for (i in 1 until lines.size) {
            val line = lines[i]
            // 注意：因為 PPG_Raw_Data 欄位裡面包含引號與分號，簡單用 split 需小心，或者直接抓欄位
            val tokens = line.split(",")
            if (tokens.size >= 4) {
                val timestamp = tokens[0]
                val glucose = tokens[1].toIntOrNull() ?: 100
                val ppgCount = tokens[2].toIntOrNull() ?: 30
                val ppgRawData = tokens.drop(3).joinToString(",") // 剩下的部分保留原始字串

                records.add(
                    GlucoseRecord(
                        timestamp = timestamp,
                        fingerstickGlucose = glucose,
                        ppgCount = ppgCount,
                        ppgRawData = ppgRawData
                    )
                )
            }
        }
        Log.d("CsvDebug", "成功載入 ${records.size} 筆手指血糖校準紀錄")
    } catch (e: Exception) {
        Log.e("CsvDebug", "解析 CSV 發生錯誤: ${e.message}", e)
    }

    return records
}

// 支援雙引號包覆的 CSV 行切割器
private fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false

    for (c in line) {
        when {
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                tokens.add(sb.toString().trim())
                sb.clear()
            }
            else -> sb.append(c)
        }
    }
    tokens.add(sb.toString().trim())
    return tokens
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM watch_measurements ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<WatchMeasurementRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WatchMeasurementRecord)
}