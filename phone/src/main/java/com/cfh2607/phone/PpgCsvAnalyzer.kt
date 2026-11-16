package com.cfh2607.phone

import java.io.File
import kotlin.math.sqrt

data class PpgAnalysisResult(
    val fileName: String,
    val totalRecords: Int,
    val validRecords: Int,
    val zeroRecords: Int,
    val fingerstickGlucose: Int?,
    val startTimestamp: Long?,
    val endTimestamp: Long?,
    val durationSeconds: Double,
    val greenMean: Double?,
    val redMean: Double?,
    val irMean: Double?,
    val greenSd: Double?,
    val redSd: Double?,
    val irSd: Double?
)

private data class PpgRow(
    val timestamp: Long,
    val green: Double,
    val red: Double,
    val ir: Double,
    val glucose: Int?
)

object PpgCsvAnalyzer {

    fun analyze(file: File): PpgAnalysisResult {

        val rows = mutableListOf<PpgRow>()

        file.bufferedReader().useLines { lines ->

            val iterator = lines.iterator()

            if (!iterator.hasNext()) {
                return emptyResult(file.name)
            }

            val header = iterator.next()
                .split(",")
                .map { it.trim() }

            val timestampIndex = header.indexOfFirst {
                it.equals("Timestamp", true)
            }

            val greenIndex = header.indexOfFirst {
                it.equals("Green", true)
            }

            val redIndex = header.indexOfFirst {
                it.equals("Red", true)
            }

            val irIndex = header.indexOfFirst {
                it.equals("IR", true)
            }

            val glucoseIndex = header.indexOfFirst {
                it.equals("FingerstickGlucose", true)
            }

            if (listOf(
                    timestampIndex,
                    greenIndex,
                    redIndex,
                    irIndex
                ).any { it < 0 }
            ) {
                return emptyResult(file.name)
            }

            while (iterator.hasNext()) {

                val columns = iterator.next().split(",")

                val timestamp = columns
                    .getOrNull(timestampIndex)
                    ?.toLongOrNull() ?: continue

                val green = columns
                    .getOrNull(greenIndex)
                    ?.toDoubleOrNull() ?: continue

                val red = columns
                    .getOrNull(redIndex)
                    ?.toDoubleOrNull() ?: continue

                val ir = columns
                    .getOrNull(irIndex)
                    ?.toDoubleOrNull() ?: continue

                val glucose = columns
                    .getOrNull(glucoseIndex)
                    ?.toIntOrNull()

                rows.add(
                    PpgRow(
                        timestamp = timestamp,
                        green = green,
                        red = red,
                        ir = ir,
                        glucose = glucose
                    )
                )
            }
        }

        val validRows = rows.filter {
            it.green > 0 &&
                    it.red > 0 &&
                    it.ir > 0 &&
                    it.green.isFinite() &&
                    it.red.isFinite() &&
                    it.ir.isFinite()
        }

        fun mean(values: List<Double>): Double? {
            return if (values.isEmpty()) null
            else values.average()
        }

        fun sd(values: List<Double>): Double? {
            if (values.size < 2) return null

            val average = values.average()

            return sqrt(
                values.sumOf {
                    (it - average) * (it - average)
                } / (values.size - 1)
            )
        }

        val glucoseValues = rows.mapNotNull {
            it.glucose?.takeIf { value -> value in 40..400 }
        }

        val start = rows.minOfOrNull { it.timestamp }
        val end = rows.maxOfOrNull { it.timestamp }

        val duration = if (start != null && end != null) {
            (end - start) / 1000.0
        } else {
            0.0
        }

        return PpgAnalysisResult(
            fileName = file.name,
            totalRecords = rows.size,
            validRecords = validRows.size,
            zeroRecords = rows.size - validRows.size,
            fingerstickGlucose = glucoseValues.firstOrNull(),
            startTimestamp = start,
            endTimestamp = end,
            durationSeconds = duration,
            greenMean = mean(validRows.map { it.green }),
            redMean = mean(validRows.map { it.red }),
            irMean = mean(validRows.map { it.ir }),
            greenSd = sd(validRows.map { it.green }),
            redSd = sd(validRows.map { it.red }),
            irSd = sd(validRows.map { it.ir })
        )
    }

    private fun emptyResult(name: String) =
        PpgAnalysisResult(
            fileName = name,
            totalRecords = 0,
            validRecords = 0,
            zeroRecords = 0,
            fingerstickGlucose = null,
            startTimestamp = null,
            endTimestamp = null,
            durationSeconds = 0.0,
            greenMean = null,
            redMean = null,
            irMean = null,
            greenSd = null,
            redSd = null,
            irSd = null
        )
}