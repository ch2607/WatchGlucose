package com.cfh2607.watchglucose

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt
/*
@Composable
fun WatchGlucoseScreenxx(sensorManager: SensorManager) {
    val context = LocalContext.current

    var isSettingOn by remember { mutableStateOf(false) } // ON: 校準模式 / OFF: 預測模式
    var fingerstickGlucose by remember { mutableStateOf(100) }
    var predictedGlucose by remember { mutableStateOf<Int?>(128) }

    var isMeasuring by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(30) }

    val ppgDataList = remember { mutableStateListOf<Float>() }
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.let { values ->
                    if (values.isNotEmpty()) {
                        ppgDataList.add(values[0])
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // 30 秒倒數與感測器生命週期管理
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
            ppgDataList.clear()
            val ppgSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

            try {
                ppgSensor?.let {
                    sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME)
                }

                secondsRemaining = 30
                while (secondsRemaining > 0) {
                    delay(1000L)
                    secondsRemaining--
                }

                if (isSettingOn) {
                    saveGlucosePpgPair(context, fingerstickGlucose, ppgDataList)
                } else {
                    predictedGlucose = runGlucoseInference(ppgDataList)
                }
            } finally {
                sensorManager.unregisterListener(sensorListener)
                isMeasuring = false
            }
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxSize()
            ) {

                // 上方區域：根據模式顯示內容
                Spacer(modifier = Modifier.height(4.dp))
                if (isSettingOn) {
                    Text(
                        text = "手指血糖 (校準)",
                        style = TextStyle(fontSize = 12.sp, color = Color.Yellow)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Button(
                            onClick = { if (fingerstickGlucose > 40) fingerstickGlucose-- },
                            enabled = !isMeasuring,
                            modifier = Modifier.size(36.dp),
                            colors = ButtonDefaults.secondaryButtonColors()
                        ) {
                            Text(text = "-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "$fingerstickGlucose",
                            style = TextStyle(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = { if (fingerstickGlucose < 400) fingerstickGlucose++ },
                            enabled = !isMeasuring,
                            modifier = Modifier.size(36.dp),
                            colors = ButtonDefaults.secondaryButtonColors()
                        ) {
                            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "預測血糖",
                        style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                    )
                    Text(
                        text = predictedGlucose?.let { "$it" } ?: "--",
                        style = TextStyle(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    )
                    Text(text = "mg/dL", style = TextStyle(fontSize = 10.sp, color = Color.LightGray))
                }

                // 中間區域：30 秒核心量測按鈕
                Button(
                    onClick = { if (!isMeasuring) isMeasuring = true },
                    enabled = !isMeasuring,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isMeasuring) Color.Red else Color(0xFF0059B2)
                    )
                ) {
                    Text(
                        text = if (isMeasuring) "測量中 (${secondsRemaining}s)" else "按 30 秒量測",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                // 底部區域：齒輪按鈕
                CompactButton(
                    onClick = { if (!isMeasuring) isSettingOn = !isSettingOn },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSettingOn) Color(0xFF0059B2) else Color.DarkGray
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "⚙",
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

private fun saveGlucosePpgPair(context: Context, glucose: Int, ppgData: List<Float>) {
    try {
        val timestamp = System.currentTimeMillis()
        val fileName = "glucose_calibration_$timestamp.csv" // 動態檔名

        val file = File(context.filesDir, fileName)
        val fileExists = file.exists()
        val writer = FileWriter(file, true)

        if (!fileExists) {
            writer.append("Timestamp,FingerstickGlucose,PPG_Count,PPG_Raw_Data\n")
        }

        val ppgString = ppgData.joinToString(separator = ";")
        writer.append("$timestamp,$glucose,${ppgData.size},\"$ppgString\"\n")
        writer.flush()
        writer.close()

        Log.d("Hsieh", "成功儲存數據對！檔案路徑: ${file.absolutePath}")

        // 傳入對應的 fileName 進行同步
        syncCsvDataToPhone(context, fileName)

    } catch (e: Exception) {
        Log.e("Hsieh", "儲存 CSV 發生錯誤: ${e.message}", e)
    }
}
/*
private fun syncCsvDataToPhone(context: Context, fileName: String) { // 接收檔案名稱參數
    val file = File(context.filesDir, fileName)
    if (!file.exists()) return

    val csvBytes = file.readBytes()
    val asset = com.google.android.gms.wearable.Asset.createFromBytes(csvBytes)

    val putDataMapReq = com.google.android.gms.wearable.PutDataMapRequest.create("/glucose_data_sync").apply {
        dataMap.putAsset("csv_file", asset)
        dataMap.putString("file_name", fileName) // 傳遞動態的時間戳記檔名
        dataMap.putLong("timestamp", System.currentTimeMillis())
    }

    val request = putDataMapReq.asPutDataRequest().setUrgent()

    com.google.android.gms.wearable.Wearable.getDataClient(context).putDataItem(request)
        .addOnSuccessListener {
            Log.d("hsieh", "CSV 資料成功發送至 Data Layer: $fileName")
        }
        .addOnFailureListener { e ->
            Log.e("hsieh", "CSV 發送失敗", e)
        }
}  */

private fun runGlucoseInference(ppgData: List<Float>): Int {
    if (ppgData.size < 10) return 100

    val mean = ppgData.average().toFloat()
    val variance = ppgData.map { (it - mean) * (it - mean) }.average().toFloat()
    val stdDev = sqrt(variance)

    val vpg = mutableListOf<Float>()
    for (i in 0 until ppgData.size - 1) {
        vpg.add(ppgData[i + 1] - ppgData[i])
    }

    val sdptg = mutableListOf<Float>()
    for (i in 0 until vpg.size - 1) {
        sdptg.add(vpg[i + 1] - vpg[i])
    }

    val sdptgMean = if (sdptg.isNotEmpty()) sdptg.average().toFloat() else 0f
    val sdptgStd = if (sdptg.isNotEmpty()) {
        val sMean = sdptgMean
        val sVar = sdptg.map { (it - sMean) * (it - sMean) }.average().toFloat()
        sqrt(sVar)
    } else 0f

    val wMean = 0.85f
    val wStd = -0.30f
    val wSdptgMean = 1.45f
    val wSdptgStd = -0.60f
    val intercept = 72.0f

    val estimatedGlucose = (wMean * mean) + (wStd * stdDev) +
            (wSdptgMean * sdptgMean) + (wSdptgStd * sdptgStd) + intercept

    return estimatedGlucose.toInt().coerceIn(40, 400)
}  */