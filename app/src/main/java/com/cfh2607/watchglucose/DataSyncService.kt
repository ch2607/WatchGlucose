package com.cfh2607.watchglucose

import android.util.Log
import com.cfh2607.watchglucose.data.AppDatabase
import com.cfh2607.watchglucose.data.SensorRecord
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DataSyncService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {
            val uri = event.dataItem.uri
            if (uri.path == "/glucose_data") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val glucoseValue = dataMapItem.dataMap.getFloat("glucose_value")
                val timestamp = dataMapItem.dataMap.getLong("timestamp")

                // 收到手機發送的血糖資料，直接寫入手錶本地的 Room 資料庫
                serviceScope.launch {
                    val database = AppDatabase.getDatabase(applicationContext)
                    val dao = database.sensorDao()
                    dao.insertRecord(
                        SensorRecord(
                            value = glucoseValue,
                            timestamp = timestamp
                        )
                    )
                    Log.d("DataSyncService", "手錶端成功接收並寫入血糖: $glucoseValue mg/dL")
                }
            }
        }
    }
}