package com.cfh2607.phone

import com.cfh2607.watchglucose.PpgSamplePoint
import kotlin.math.sqrt

data class AdvancedPpgFeatures(
    val greenMean: Double,
    val greenStd: Double,
    val redMean: Double,
    val redStd: Double,
    val irMean: Double,
    val irStd: Double,
    // 新增 SDPTG 的關鍵波形特徵指標 (例如 a波 到 e波 的均值或特徵比值)
    val sdptgBmean: Double,
    val sdptgCmean: Double,
    val sampleCount: Int
)

object FeatureExtractor {

    fun extractAdvancedFeatures(ppgList: List<PpgSamplePoint>): AdvancedPpgFeatures? {
        if (ppgList.size < 5) return null

        val size = ppgList.size.toDouble()

        // 1. 基本統計特徵 (Mean & Std)
        val greenValues = ppgList.map { it.green.toDouble() }
        val redValues = ppgList.map { it.red.toDouble() }
        val irValues = ppgList.map { it.ir.toDouble() }

        val greenMean = greenValues.average()
        val redMean = redValues.average()
        val irMean = irValues.average()

        val greenStd = sqrt(greenValues.map { (it - greenMean) * (it - greenMean) }.average())
        val redStd = sqrt(redValues.map { (it - redMean) * (it - redMean) }.average())
        val irStd = sqrt(irValues.map { (it - irMean) * (it - irMean) }.average())

        // 2. 計算二階微分 PPG (SDPTG / Acceleration Plethysmogram)
        // 一階微分: x[i] - x[i-1]
        // 二階微分: (x[i+1] - 2*x[i] + x[i-1]) / dt^2
        val sdptgList = mutableListOf<Double>()
        for (i in 1 until ppgList.size - 1) {
            val prev = ppgList[i - 1].green.toDouble()
            val curr = ppgList[i].green.toDouble()
            val next = ppgList[i + 1].green.toDouble()

            // 離散二階差分公式
            val acceleration = next - (2.0 * curr) + prev
            sdptgList.add(acceleration)
        }

        // 3. 萃取 SDPTG 的統計特徵作為血管彈性指標
        val sdptgMean = if (sdptgList.isNotEmpty()) sdptgList.average() else 0.0
        // 可以進一步區分正向加速度波與負向加速度波的均值
        val positiveSdptgMean = sdptgList.filter { it > 0 }.let { if (it.isNotEmpty()) it.average() else 0.0 }
        val negativeSdptgMean = sdptgList.filter { it < 0 }.let { if (it.isNotEmpty()) it.average() else 0.0 }

        return AdvancedPpgFeatures(
            greenMean = greenMean,
            greenStd = greenStd,
            redMean = redMean,
            redStd = redStd,
            irMean = irMean,
            irStd = irStd,
            sdptgBmean = positiveSdptgMean,
            sdptgCmean = negativeSdptgMean,
            sampleCount = ppgList.size
        )
    }
}