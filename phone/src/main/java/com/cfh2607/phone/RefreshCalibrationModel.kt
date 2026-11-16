package com.cfh2607.phone

import android.content.Context
import android.util.Log

/**
 * 封裝校準模型重新訓練與血糖預測結果的資料類別
 */
data class CalibrationUpdateResult(
    val success: Boolean,
    val totalFilesLoaded: Int,
    val modelIsReady: Boolean,
    val modelRSquared: Float,
    val latestPredictedGlucose: Float,
    val message: String
)

object RefreshCalibrationModelHelper {

    private const val TAG = "RefreshCalibrationModel"

    /**
     * 執行完整流程：
     * 1. 載入手機 calibration/ 目錄下的所有校準檔案
     * 2. 透過多元線性迴歸模型 (OLS) 訓練個人化係數
     * 3. 計算最新一筆數據的預測血糖
     */
    fun refreshAndPredict(context: Context): CalibrationUpdateResult {
        try {
            // 1. 載入所有校準檔案與特徵摘要
            val historySummaries = MultiFileCsvLoader.loadAllCalibrationTrends(context)
            if (historySummaries.isEmpty()) {
                Log.d(TAG, "目前尚無任何歷史校準檔案")
                return CalibrationUpdateResult(
                    success = false,
                    totalFilesLoaded = 0,
                    modelIsReady = false,
                    modelRSquared = 0f,
                    latestPredictedGlucose = 0f,
                    message = "尚無歷史校準檔案"
                )
            }

            // 2. 訓練個人化多元線性迴歸模型
            val modelResult = CalibrationRegressionModel.trainModel(historySummaries)

            // 3. 取得最新一筆 PPG 特徵並進行專屬血糖預測
            val latestSummary = historySummaries.last()
            val predictedGlucose = CalibrationRegressionModel.predictGlucose(latestSummary, modelResult)

            Log.d(
                TAG,
                "模型更新成功 | 檔案數: ${historySummaries.size} | 模型就緒: ${modelResult.isReady} | R²: ${modelResult.rSquared} | 預測血糖: $predictedGlucose"
            )

            return CalibrationUpdateResult(
                success = true,
                totalFilesLoaded = historySummaries.size,
                modelIsReady = modelResult.isReady,
                modelRSquared = modelResult.rSquared,
                latestPredictedGlucose = predictedGlucose,
                message = if (modelResult.isReady) "個人化模型訓練完成" else "資料點尚不足 8 筆，目前使用預設權重"
            )

        } catch (e: Exception) {
            Log.e(TAG, "更新校準模型失敗: ${e.message}")
            return CalibrationUpdateResult(
                success = false,
                totalFilesLoaded = 0,
                modelIsReady = false,
                modelRSquared = 0f,
                latestPredictedGlucose = 0f,
                message = "錯誤: ${e.localizedMessage}"
            )
        }
    }
}