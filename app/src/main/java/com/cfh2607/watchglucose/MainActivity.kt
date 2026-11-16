package com.cfh2607.watchglucose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.MaterialTheme

import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.samsung.android.service.health.tracking.ConnectionListener
import com.samsung.android.service.health.tracking.HealthTracker
import com.samsung.android.service.health.tracking.HealthTrackerException
import com.samsung.android.service.health.tracking.HealthTrackingService
import com.samsung.android.service.health.tracking.data.DataPoint
import com.samsung.android.service.health.tracking.data.HealthTrackerType
import com.samsung.android.service.health.tracking.data.PpgType
import com.samsung.android.service.health.tracking.data.ValueKey
import org.json.JSONArray
import java.io.File
import com.cfh2607.watchglucose.WatchGlucosePredictor

class MainActivity : ComponentActivity(), com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "HSIEH"
        private const val MEASURE_SECONDS = 30
        private const val SYNC_PATH = "/glucose_data_sync"
    }

    private var healthTrackingService: HealthTrackingService? = null
    private var ppgTracker: HealthTracker? = null
    private val handler = Handler(Looper.getMainLooper())

    // UI States
    private var connected by mutableStateOf(false)
    private var measuring by mutableStateOf(false)
    private var status by mutableStateOf("等待感測器連線")
    private var secondsLeft by mutableIntStateOf(MEASURE_SECONDS)
    private var recordCount by mutableIntStateOf(0)
    private var greenValue by mutableIntStateOf(0)
    private var redValue by mutableIntStateOf(0)
    private var irValue by mutableIntStateOf(0)
    private var fingerstickGlucose by mutableIntStateOf(100)
    private var calibrationMode by mutableStateOf(true)
    private var currentPredictedGlucose by mutableStateOf<Double?>(null)

    private val ppgRecords = mutableListOf<PpgRecord>()

    data class PpgRecord(
        val timestamp: Long,
        val green: Int,
        val red: Int,
        val ir: Int
    )

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!measuring) return

            if (secondsLeft > 0) {
                secondsLeft--
            }

            if (secondsLeft <= 0) {
                stopMeasurement()
            } else {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                connectSamsungService()
            } else {
                status = "未取得 BODY_SENSORS 權限"
            }
        }

    private val connectionListener = object : ConnectionListener {
        override fun onConnectionSuccess() {
            runOnUiThread {
                try {
                    val service = healthTrackingService ?: return@runOnUiThread

                    val supported = service.trackingCapability.supportHealthTrackerTypes
                    if (!supported.contains(HealthTrackerType.PPG_ON_DEMAND)) {
                        connected = false
                        status = "手錶不支援 PPG_ON_DEMAND"
                        return@runOnUiThread
                    }

                    ppgTracker = service.getHealthTracker(
                        HealthTrackerType.PPG_ON_DEMAND,
                        setOf(PpgType.GREEN, PpgType.RED, PpgType.IR)
                    )

                    connected = ppgTracker != null
                    status = if (connected) "Samsung 感測器已連線" else "無法建立 PPG Tracker"
                } catch (e: Exception) {
                    connected = false
                    status = "感測器初始化失敗：${e.message}"
                    Log.e(TAG, "Tracker initialization failed", e)
                }
            }
        }

        override fun onConnectionEnded() {
            runOnUiThread {
                connected = false
                status = "感測器連線中斷"
                if (measuring) stopMeasurement()
            }
        }

        override fun onConnectionFailed(exception: HealthTrackerException) {
            runOnUiThread {
                connected = false
                status = "連線失敗：${exception.message}"
                Log.e(TAG, "Samsung sensor connection failed", exception)
            }
        }
    }

    // 💡 透過 MessageClient 即時接收手機發送過來的歷史紀錄
    override fun onMessageReceived(messageEvent: com.google.android.gms.wearable.MessageEvent) {
        if (messageEvent.path == "/glucose_message_sync") {
            try {
                val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                if (jsonStr.isNotEmpty()) {
                    val jsonArray = JSONArray(jsonStr)
                    val records = mutableListOf<HistoricalRecord>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        records.add(
                            HistoricalRecord(
                                green = obj.getDouble("green").toFloat(),
                                red = obj.getDouble("red").toFloat(),
                                ir = obj.getDouble("ir").toFloat(),
                                glucose = obj.getDouble("glucose")
                            )
                        )
                    }

                    // 寫入本地快取
                    WatchGlucosePredictor.updateLocalHistoryCache(this, records)
                    Log.d(TAG, "🎉 成功透過 MessageClient 即時收到並快取了 ${records.size} 筆歷史資料！")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 解析 MessageClient 資料失敗", e)
            }
        }
    }

    private val trackerListener = object : HealthTracker.TrackerEventListener {
        override fun onDataReceived(dataPoints: MutableList<DataPoint>) {
            var latestGreen = greenValue
            var latestRed = redValue
            var latestIr = irValue

            synchronized(ppgRecords) {
                for (point in dataPoints) {
                    val green = point.getValue(ValueKey.PpgSet.PPG_GREEN)
                    val red = point.getValue(ValueKey.PpgSet.PPG_RED)
                    val ir = point.getValue(ValueKey.PpgSet.PPG_IR)

                    ppgRecords.add(
                        PpgRecord(
                            timestamp = point.timestamp,
                            green = green,
                            red = red,
                            ir = ir
                        )
                    )

                    latestGreen = green
                    latestRed = red
                    latestIr = ir
                }

                val count = ppgRecords.size
                runOnUiThread {
                    greenValue = latestGreen
                    redValue = latestRed
                    irValue = latestIr
                    recordCount = count
                }
            }
        }

        override fun onFlushCompleted() {
            Log.d(TAG, "PPG flush completed")
        }

        override fun onError(error: HealthTracker.TrackerError) {
            runOnUiThread {
                status = "感測器錯誤：$error"
                if (measuring) stopMeasurement()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Log.d(TAG, "啟動時先從 SharedPreferences 載入舊快取")
        WatchGlucosePredictor.loadLocalHistoryCache(this)

        setContent {
            MaterialTheme {
                WatchGlucoseScreen(
                    connected = connected,
                    measuring = measuring,
                    status = status,
                    secondsLeft = secondsLeft,
                    recordCount = recordCount,
                    greenValue = greenValue,
                    redValue = redValue,
                    irValue = irValue,
                    fingerstickGlucose = fingerstickGlucose,
                    calibrationMode = calibrationMode,
                    predictedGlucose = currentPredictedGlucose,
                    onCalibrationModeChanged = { newMode ->
                        if (!measuring) calibrationMode = newMode
                    },
                    onFingerstickChanged = { newValue ->
                        fingerstickGlucose = newValue.coerceIn(40, 400)
                    },
                    onStartMeasurement = { startMeasurement() },
                    onStopMeasurement = { stopMeasurement() }
                )
            }
        }

        checkPermissionAndConnect()
    }

    private fun checkPermissionAndConnect() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            connectSamsungService()
        } else {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    private fun connectSamsungService() {
        try {
            status = "正在連接 Samsung Health Sensor Service"
            healthTrackingService = HealthTrackingService(connectionListener, this)
            healthTrackingService?.connectService()
        } catch (e: Exception) {
            connected = false
            status = "連線失敗：${e.message}"
            Log.e(TAG, "Service connection failed", e)
        }
    }

    private fun startMeasurement() {
        if (measuring) return

        val tracker = ppgTracker
        if (!connected || tracker == null) {
            status = "感測器尚未連線"
            return
        }

        synchronized(ppgRecords) {
            ppgRecords.clear()
        }

        recordCount = 0
        greenValue = 0
        redValue = 0
        irValue = 0
        secondsLeft = MEASURE_SECONDS

        try {
            handler.removeCallbacks(timerRunnable)
            tracker.setEventListener(trackerListener)

            measuring = true
            status = "正在收集 PPG 原始數據"

            handler.postDelayed(timerRunnable, 1000L)
            Log.d(TAG, "30-second PPG measurement started")
        } catch (e: Exception) {
            measuring = false
            handler.removeCallbacks(timerRunnable)
            status = "開始測量失敗：${e.message}"
            Log.e(TAG, "Start measurement failed", e)
        }
    }

    private fun stopMeasurement() {
        Log.e(TAG, "stopMeasurement")
        if (!measuring && secondsLeft == 0) return

        handler.removeCallbacks(timerRunnable)

        try {
            ppgTracker?.unsetEventListener()
        } catch (e: Exception) {
            Log.e(TAG, "Unset PPG listener failed", e)
        }

        measuring = false
        secondsLeft = 0

        val count = synchronized(ppgRecords) { ppgRecords.size }
        recordCount = count

        if (count > 0) {
            if (calibrationMode) {
                status = "校正測量完成，儲存並傳送手機"
                savePpgData()
            } else {
                status = "即時預測完成"
                calculatePredictionOnly()
            }
        } else {
            status = "測量結束，但沒有收到 PPG 資料"
        }

        Log.d(TAG, "Measurement stopped; records=$count")
    }

    private fun calculatePredictionOnly() {
        try {
            var avgGreen = 0f
            var avgRed = 0f
            var avgIr = 0f

            synchronized(ppgRecords) {
                if (ppgRecords.isNotEmpty()) {
                    avgGreen = ppgRecords.map { p -> p.green.toFloat() }.average().toFloat()
                    avgRed = ppgRecords.map { p -> p.red.toFloat() }.average().toFloat()
                    avgIr = ppgRecords.map { p -> p.ir.toFloat() }.average().toFloat()
                }
            }

            val predictedGlucose = WatchGlucosePredictor.predict(avgGreen, avgRed, avgIr)
            currentPredictedGlucose = predictedGlucose

            status = "預測完成: ${predictedGlucose.toInt()} mg/dL"
            Log.d(TAG, "Watch Prediction Only -> $predictedGlucose")
        } catch (e: Exception) {
            status = "預測計算失敗：${e.message}"
            Log.e(TAG, "Prediction error", e)
        }
    }

    private fun savePpgData() {
        try {
            val timestamp = System.currentTimeMillis()
            val fileName = "glucose_calibration_$timestamp.csv"
            val calibrationDir = File(filesDir, "calibration")
            if (!calibrationDir.exists()) {
                calibrationDir.mkdirs()
            }

            val file = File(calibrationDir, fileName)
            val glucoseValue = if (calibrationMode) fingerstickGlucose else 0

            var avgGreen = 0f
            var avgRed = 0f
            var avgIr = 0f

            synchronized(ppgRecords) {
                if (ppgRecords.isNotEmpty()) {
                    avgGreen = ppgRecords.map { p -> p.green.toFloat() }.average().toFloat()
                    avgRed = ppgRecords.map { p -> p.red.toFloat() }.average().toFloat()
                    avgIr = ppgRecords.map { p -> p.ir.toFloat() }.average().toFloat()
                }
            }

            val predictedGlucose = WatchGlucosePredictor.predict(avgGreen, avgRed, avgIr)
            currentPredictedGlucose = predictedGlucose

            file.bufferedWriter().use { writer ->
                writer.appendLine("Timestamp,Green,Red,IR,FingerstickGlucose,PredictedGlucose")
                synchronized(ppgRecords) {
                    ppgRecords.forEach { record ->
                        writer.appendLine(
                            "${record.timestamp},${record.green},${record.red},${record.ir},$glucoseValue,$predictedGlucose"
                        )
                    }
                }
            }

            status = "CSV 已儲存，預測值: ${predictedGlucose.toInt()}，傳送手機中"
            syncCsvDataToPhone(file, fileName)
        } catch (e: Exception) {
            status = "CSV 儲存失敗：${e.message}"
            Log.e(TAG, "Save CSV failed", e)
        }
    }

    private fun syncCsvDataToPhone(file: File, fileName: String) {
        try {
            val asset = Asset.createFromBytes(file.readBytes())
            val request = PutDataMapRequest.create(SYNC_PATH).apply {
                dataMap.putAsset("csv_file", asset)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("file_name", fileName)
            }.asPutDataRequest()

            request.setUrgent()

            Wearable.getDataClient(this)
                .putDataItem(request)
                .addOnSuccessListener {
                    runOnUiThread {
                        status = "測量完成，CSV 已傳送手機"
                    }
                    Log.d(TAG, "CSV sent to phone: $fileName")
                }
                .addOnFailureListener { error ->
                    runOnUiThread {
                        status = "CSV 已儲存，但傳送失敗：${error.message}"
                    }
                    Log.e(TAG, "CSV sync failed", error)
                }
        } catch (e: Exception) {
            status = "手機同步失敗：${e.message}"
            Log.e(TAG, "Sync error", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Log.d(TAG, "已註冊 MessageClient 即時監聽器")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        try {
            ppgTracker?.unsetEventListener()
            healthTrackingService?.disconnectService()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }
        super.onDestroy()
    }
}