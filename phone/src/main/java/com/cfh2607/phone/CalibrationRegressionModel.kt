package com.cfh2607.phone

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 迴歸模型計算結果資料類別
 */
data class RegressionResult(
    val intercept: Float,       // 截距 (Beta 0)
    val weights: FloatArray,    // 各特徵權重 [GreenMean, RedMean, IRMean, GreenSd, RedSd, IrSd]
    val rSquared: Float,        // 擬合優度 (R²)，可用於評估模型精準度
    val isReady: Boolean        // 校準資料點是否足夠進行有效訓練
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RegressionResult

        if (intercept != other.intercept) return false
        if (!weights.contentEquals(other.weights)) return false
        if (rSquared != other.rSquared) return false
        if (isReady != other.isReady) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intercept.hashCode()
        result = 31 * result + weights.contentHashCode()
        result = 31 * result + rSquared.hashCode()
        result = 31 * result + isReady.hashCode()
        return result
    }
}

/**
 * 個人化血糖多元線性迴歸核心運算引擎
 */
object CalibrationRegressionModel {

    /**
     * 根據歷史收集到的特徵摘要清單，訓練專屬的多元線性迴歸模型
     */
    fun trainModel(summaries: List<PpgFeatureSummary>): RegressionResult {
        // 過濾掉未輸入或無效指尖血糖的紀錄
        val validData = summaries.filter { it.fingerstickGlucose > 0 }

        // 至少需要 8 筆以上有效指尖血糖校準點，模型才具備足夠的統計穩定度
        if (validData.size < 8) {
            return RegressionResult(
                intercept = 72f,
                weights = floatArrayOf(0.85f, 0f, 0f, -0.30f, 0f, 0f),
                rSquared = 0f,
                isReady = false
            )
        }

        val n = validData.size
        val p = 6 // 特徵數量: GreenMean, RedMean, IRMean, GreenSd, RedSd, IrSd
        val cols = p + 1 // 包含截距項，共 7 個未知係數

        // 建立設計矩陣 X (n x cols) 與目標向量 y (n x 1)
        val X = Array(n) { DoubleArray(cols) }
        val y = DoubleArray(n)

        for (i in 0 until n) {
            val item = validData[i]
            X[i][0] = 1.0 // 截距項常數 1.0
            X[i][1] = item.greenMean.toDouble()
            X[i][2] = item.redMean.toDouble()
            X[i][3] = item.irMean.toDouble()
            X[i][4] = item.greenSd.toDouble()
            X[i][5] = item.redSd.toDouble()
            X[i][6] = item.irSd.toDouble()
            y[i] = item.fingerstickGlucose.toDouble()
        }

        // 透過正規方程式 (X^T * X) * Beta = X^T * y 求解
        val beta = solveNormalEquation(X, y, n, cols)

        // 計算 R² (決定係數) 評估模型的解釋力
        val rSquared = calculateRSquared(X, y, beta, n)

        return RegressionResult(
            intercept = beta[0].toFloat(),
            weights = floatArrayOf(
                beta[1].toFloat(),
                beta[2].toFloat(),
                beta[3].toFloat(),
                beta[4].toFloat(),
                beta[5].toFloat(),
                beta[6].toFloat()
            ),
            rSquared = rSquared.toFloat(),
            isReady = true
        )
    }

    /**
     * 利用訓練好的模型係數，計算當前特徵的預測血糖值
     */
    fun predictGlucose(summary: PpgFeatureSummary, result: RegressionResult): Float {
        val prediction = result.intercept +
                result.weights[0] * summary.greenMean +
                result.weights[1] * summary.redMean +
                result.weights[2] * summary.irMean +
                result.weights[3] * summary.greenSd +
                result.weights[4] * summary.redSd +
                result.weights[5] * summary.irSd

        // 將預測值限制在合理的生理範圍內 (40 ~ 400 mg/dL)
        return prediction.coerceIn(40f, 400f)
    }

    // ==========================================
    // 內部數學工具：矩陣轉置、乘法與高斯消去法
    // ==========================================

    private fun solveNormalEquation(X: Array<DoubleArray>, y: DoubleArray, n: Int, cols: Int): DoubleArray {
        val XtX = Array(cols) { DoubleArray(cols) }
        val Xty = DoubleArray(cols)

        // 計算 (X^T * X) 及 (X^T * y)
        for (i in 0 until cols) {
            for (j in 0 until cols) {
                var sum = 0.0
                for (k in 0 until n) {
                    sum += X[k][i] * X[k][j]
                }
                XtX[i][j] = sum
            }
            var sumY = 0.0
            for (k in 0 until n) {
                sumY += X[k][i] * y[k]
            }
            Xty[i] = sumY
        }

        // 增廣矩陣求解
        val augmented = Array(cols) { i -> DoubleArray(cols + 1) }
        for (i in 0 until cols) {
            for (j in 0 until cols) {
                augmented[i][j] = XtX[i][j]
            }
            augmented[i][cols] = Xty[i]
        }

        // 高斯消去法 (Gaussian Elimination with Partial Pivoting)
        for (i in 0 until cols) {
            var maxRow = i
            for (k in i + 1 until cols) {
                if (abs(augmented[k][i]) > abs(augmented[maxRow][i])) {
                    maxRow = k
                }
            }
            val temp = augmented[i]
            augmented[i] = augmented[maxRow]
            augmented[maxRow] = temp

            if (abs(augmented[i][i]) < 1e-10) continue

            for (k in i + 1 until cols) {
                val factor = augmented[k][i] / augmented[i][i]
                for (j in i..cols) {
                    augmented[k][j] -= factor * augmented[i][j]
                }
            }
        }

        // 反向代換法 (Back Substitution)
        val beta = DoubleArray(cols)
        for (i in cols - 1 downTo 0) {
            var sum = 0.0
            for (j in i + 1 until cols) {
                sum += augmented[i][j] * beta[j]
            }
            if (abs(augmented[i][i]) > 1e-10) {
                beta[i] = (augmented[i][cols] - sum) / augmented[i][i]
            } else {
                beta[i] = 0.0
            }
        }

        return beta
    }

    private fun calculateRSquared(X: Array<DoubleArray>, y: DoubleArray, beta: DoubleArray, n: Int): Double {
        var yMean = 0.0
        for (i in 0 until n) yMean += y[i]
        yMean /= n

        var ssTot = 0.0
        var ssRes = 0.0

        for (i in 0 until n) {
            var yPred = 0.0
            for (j in beta.indices) {
                yPred += X[i][j] * beta[j]
            }
            ssTot += (y[i] - yMean) * (y[i] - yMean)
            ssRes += (y[i] - yPred) * (y[i] - yPred)
        }

        return if (ssTot == 0.0) 0.0 else 1.0 - (ssRes / ssTot)
    }
}