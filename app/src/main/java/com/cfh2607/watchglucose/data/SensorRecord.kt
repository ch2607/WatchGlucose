// 檔案路徑: app/src/main/java/com/cfh2607/watchglucose/data/SensorRecord.kt
package com.cfh2607.watchglucose.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_records")
data class SensorRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Float,
    val timestamp: Long = System.currentTimeMillis()
)