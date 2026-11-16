package com.cfh2607.phone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stage_one_records")
data class StageOneRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fingerstickGlucose: Int, // 手指頭抽血測得的真實血糖
    val ppgGreen: Float,         // PPG Green
    val ppgRed: Float,           // PPG Red
    val ppgIr: Float,            // PPG IR
    val heartRate: Int,          // 心率
    val activityState: String,   // 活動狀態
    val timestamp: Long          // 記錄時間
)

@Dao
interface StageOneDao {
    @Query("SELECT * FROM stage_one_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<StageOneRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: StageOneRecord)
}