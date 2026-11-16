// MeasurementRecord.kt
package com.cfh2607.phone

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_measurements")
data class WatchMeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val predictedGlucose: Double,
    val green: Float,
    val red: Float,
    val ir: Float
)