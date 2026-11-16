package com.cfh2607.watchglucose

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt
import kotlin.math.roundToInt

// 獨立的歷史紀錄資料結構
data class HistoricalRecord(
    val green: Float,
    val red: Float,
    val ir: Float,
    val glucose: Double
)

object WatchGlucosePredictor {

    private const val TAG = "WatchGlucosePredictor"
    private const val PREF_NAME = "glucose_prediction_cache"
    private const val KEY_HISTORY_JSON = "history_json"

    // 統一使用單一快取清單
    private val cachedHistoryRecords = mutableListOf<HistoricalRecord>()

    /**
     * 手錶 MainActivity 呼叫此函式進行預測
     */
    fun predict(green: Float, red: Float, ir: Float): Double {
        val history = cachedHistoryRecords
        Log.d("hsieh", "WatchGlucosePredictor predict, records size = ${history.size}")

        if (history.isEmpty()) {
            val scaledGreen = green / 100000.0
            val scaledRed = red / 100000.0
            val scaledIr = ir / 100000.0
            val basePredicted = (scaledGreen * 2.5 + scaledRed * 1.2 - scaledIr * 1.0 + 100.0)
            Log.d("hsieh", "WatchGlucosePredictor history.isEmpty = true, using base formula")
            return basePredicted.coerceIn(40.0, 400.0)
        }

        // 執行 KNN 三維光學歐式距離比對
        val scoredRecords = history.map { record ->
            val dGreen = (green - record.green).toDouble()
            val dRed = (red - record.red).toDouble()
            val dIr = (ir - record.ir).toDouble()

            val distance = sqrt(dGreen * dGreen + dRed * dRed + dIr * dIr)
            Pair(record, distance)
        }

        val k = 5.coerceAtMost(scoredRecords.size)
        val nearestNeighbors = scoredRecords.sortedBy { it.second }.take(k)

        var weightedSum = 0.0
        var totalWeight = 0.0

        for ((record, distance) in nearestNeighbors) {
            val weight = 1.0 / (distance + 0.0001)
            weightedSum += record.glucose * weight
            totalWeight += weight
        }

        val predicted = if (totalWeight > 0) {
            weightedSum / totalWeight
        } else {
            nearestNeighbors.first().first.glucose
        }

        return predicted.coerceIn(40.0, 400.0)
    }

    fun predictInt(green: Float, red: Float, ir: Float): Int {
        return predict(green, red, ir).roundToInt()
    }

    /**
     * 更新本地快取並寫入 SharedPreferences
     */
    fun updateLocalHistoryCache(context: Context, records: List<HistoricalRecord>) {
        cachedHistoryRecords.clear()
        cachedHistoryRecords.addAll(records)
        try {
            val jsonArray = JSONArray()
            for (r in records) {
                val obj = JSONObject().apply {
                    put("green", r.green.toDouble())
                    put("red", r.red.toDouble())
                    put("ir", r.ir.toDouble())
                    put("glucose", r.glucose)
                }
                jsonArray.put(obj)
            }
            Log.d("hsieh", "WatchGlucosePredictor updateLocalHistoryCache saved ${records.size} records")
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("hsieh", "❌ updateLocalHistoryCache 失敗", e)
            e.printStackTrace()
        }
    }

    /**
     * 內部輔助方法：儲存預設快取
     */
    private fun saveLocalHistoryCache(context: Context) {
        try {
            val jsonArray = JSONArray()
            for (r in cachedHistoryRecords) {
                val obj = JSONObject().apply {
                    put("green", r.green.toDouble())
                    put("red", r.red.toDouble())
                    put("ir", r.ir.toDouble())
                    put("glucose", r.glucose)
                }
                jsonArray.put(obj)
            }
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Save cache error", e)
        }
    }

    /**
     * 載入本地歷史快取（唯一宣告，已強制清除舊殘留快取）
     */
    fun loadLocalHistoryCache(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // 強制清除硬碟中殘留的舊快取，徹底根絕那 7 筆假資料
            prefs.edit().remove(KEY_HISTORY_JSON).apply()

            val jsonStr = prefs.getString(KEY_HISTORY_JSON, null)

            if (!jsonStr.isNullOrEmpty()) {
                val jsonArray = JSONArray(jsonStr)
                cachedHistoryRecords.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    cachedHistoryRecords.add(
                        HistoricalRecord(
                            green = obj.getDouble("green").toFloat(),
                            red = obj.getDouble("red").toFloat(),
                            ir = obj.getDouble("ir").toFloat(),
                            glucose = obj.getDouble("glucose")
                        )
                    )
                }
                Log.d("HSIEH", "WatchGlucosePredictor loadLocalHistoryCache: 成功載入 ${cachedHistoryRecords.size} 筆快取")
            } else {
                val defaultRecords = emptyList<HistoricalRecord>()
                cachedHistoryRecords.clear()
                cachedHistoryRecords.addAll(defaultRecords)
                Log.d("HSIEH", "WatchGlucosePredictor: 快取已強制清空，目前為 0 筆資料")
            }
        } catch (e: Exception) {
            Log.e("HSIEH", "Load cache error", e)
        }
    }
}