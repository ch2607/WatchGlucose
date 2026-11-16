package com.cfh2607.watchglucose

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Text
import com.cfh2607.watchglucose.data.AppDatabase
import com.cfh2607.watchglucose.data.SensorRecord
import com.cfh2607.watchglucose.sensor.SensorListenerManager
import kotlinx.coroutines.launch

@Composable
fun SensorScreen(context: android.content.Context) {
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 確保只宣告一次 sensorValue
    var sensorValue by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    // 初始化資料庫與 DAO
    // 初始化資料庫與 DAO
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.sensorDao() }
   // android.util.Log.d("Hsieh", "No 成功寫入數值")
    // 建立監聽器實例，將程式碼放在這裡面：
    val sensorManager = remember {
        SensorListenerManager(context) { newValue ->
            // 過濾掉 0.0，只處理並寫入真實心率
            if (newValue > 0f) {
                sensorValue = newValue

                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        dao.insertRecord(SensorRecord(value = newValue))
                        android.util.Log.d("Hsieh", "Room DB 成功寫入數值: $newValue")
                    } catch (e: Exception) {
                        android.util.Log.e("Hsieh", "Room DB 寫入失敗: ${e.localizedMessage}")
                    }
                }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            sensorManager.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
        onDispose {
            sensorManager.stopListening()
        }
    }

    // 判斷是否觸發 72 的警戒線
    val isLow = sensorValue in 1f..72f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "即時感測值",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Text(
            text = "$sensorValue",
            fontSize = 28.sp,
            color = if (isLow) Color.Red else Color.White
        )

        if (isLow) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ 數值偏低 (<=72)",
                fontSize = 10.sp,
                color = Color.Red
            )
        }
    }
}