package com.cfh2607.phone.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_records")
data class PredictionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,

    val actualGlucose: Double,

    val predictedGlucose: Double,

    val error: Double,

    val green: Float,

    val red: Float,

    val ir: Float
)