package com.cfh2607.phone.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// 💡 1. 實體類別 (Entity)
@Entity(tableName = "measurement_history")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val predictedGlucose: Double, // 手錶預測血糖值
    val cgmGlucose: Double = 0.0,   // CGM 實際值（可供比對）
    val green: Float,
    val red: Float,
    val ir: Float
)

// 💡 2. 資料存取介面 (DAO) - 直接放在同一個檔案中
@Dao
interface MeasurementDao {

    // 取得所有歷史紀錄
    @Query("SELECT * FROM measurement_history ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementRecord>>

    // 插入單筆測量紀錄
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MeasurementRecord)
}