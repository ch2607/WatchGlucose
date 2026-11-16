package com.cfh2607.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cfh2607.phone.data.PhoneDatabase
import kotlinx.coroutines.flow.map // 💡 確保有匯入 map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 1. 取得資料庫與 DAO
            val database = PhoneDatabase.getDatabase(this)
            val dao = database.measurementDao()

            // 2. 💡 把這段轉換程式碼放在這裡！
            val records by dao.getAllMeasurements()
                .map { list ->
                    list.map { record ->
                        WatchMeasurementRecord(
                            timestamp = record.timestamp,
                            predictedGlucose = record.predictedGlucose, // 💡 將 glucose 改為 predictedGlucose（或 WatchMeasurementRecord 實際的參數名稱）
                            green = record.green,
                            red = record.red,
                            ir = record.ir
                        )
                    }
                }
                .collectAsState(initial = emptyList())

            // 3. 傳入您的 UI 畫面元件
            GlucoseHistoryScreen(records = records)
        }
    }
}