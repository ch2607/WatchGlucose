package com.cfh2607.phone.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PredictionDao {

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertPrediction(
        prediction: PredictionEntity
    )

    @Query(
        """
        SELECT *
        FROM prediction_records
        ORDER BY timestamp DESC
        """
    )
    suspend fun getAllPredictions():
            List<PredictionEntity>

    @Query(
        """
        DELETE FROM prediction_records
        """
    )
    suspend fun deleteAll()
}