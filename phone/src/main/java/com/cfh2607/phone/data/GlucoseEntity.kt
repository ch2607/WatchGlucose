package com.cfh2607.phone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ppg_table") // 💡 務必確保這裡的名稱與 Dao 查詢的 ppg_table 完全一致！
data class GlucoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val actualGlucose: Double,
    val predictedGlucose: Double,
    val predictionError: Double,
    val green: Float,
    val red: Float,
    val ir: Float,
    val activityState: String
)