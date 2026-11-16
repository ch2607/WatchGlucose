package com.cfh2607.phone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Insert
    suspend fun insert(record: GlucoseEntity) // 💡 務必確保這裡叫 insert，並且支援 suspend

    @Query("SELECT * FROM ppg_table ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<GlucoseEntity>>
}