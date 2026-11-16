package com.cfh2607.watchglucose.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SensorRecord)

    @Query("SELECT * FROM sensor_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<SensorRecord>>

    @Delete
    suspend fun deleteRecord(record: SensorRecord)

    @Query("DELETE FROM sensor_records")
    suspend fun clearAllRecords()
}