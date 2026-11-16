package com.cfh2607.watchglucose.ai // 請依您的手錶端 package 調整

import kotlin.math.roundToInt

object WatchGlucosePredictor {

    fun predict(green: Float, red: Float, ir: Float): Double {
        // 💡 1. 特徵縮放：將巨大的光學數值除以一個比例（例如 10,000 或 100,000）
        // 實際係數可根據您的校正數據調整
        val scaledGreen = green / 100000.0
        val scaledRed = red / 100000.0
        val scaledIr = ir / 100000.0

        // 2. 套用線性預測公式（此為示範基底，可依您的機器學習模型調整）
        val predicted = (
                scaledGreen * 2.5 +
                        scaledRed * 1.2 -
                        scaledIr * 1.0 +
                        100.0
                )

        // 3. 限制在正常的生理血糖範圍（例如 40.0 ~ 400.0 mg/dL），避免數值失控
        return predicted.coerceIn(40.0, 400.0)
    }

    fun predictInt(green: Float, red: Float, ir: Float): Int {
        return predict(green, red, ir).roundToInt()
    }
}