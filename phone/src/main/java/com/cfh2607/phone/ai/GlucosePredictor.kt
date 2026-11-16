package com.cfh2607.phone.ai

import kotlin.math.roundToInt

object GlucosePredictor {

    fun predict(
        green: Float,
        red: Float,
        ir: Float
    ): Double {

        val predicted = (
                green * 0.0005 +
                        red * 0.0020 -
                        ir * 0.0003 +
                        100.0
                )

        return predicted.coerceIn(
            40.0,
            400.0
        )
    }

    fun predictInt(
        green: Float,
        red: Float,
        ir: Float
    ): Int {

        return predict(
            green,
            red,
            ir
        ).roundToInt()
    }
}