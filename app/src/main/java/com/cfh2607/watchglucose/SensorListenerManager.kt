package com.cfh2607.watchglucose.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorListenerManager(
    context: Context,
    private val onSensorDataChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val targetSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    fun startListening() {
        android.util.Log.d("Hsieh", "startListening 執行中, targetSensor = $targetSensor")

        val sensor = targetSensor
        if (sensor == null) {
            android.util.Log.e("Hsieh", "錯誤：找不到目標感測器 (targetSensor 為 null)")
            return
        }


        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.values.isNotEmpty()) {
            val rawValue = event.values[0]
            android.util.Log.d("Hsieh", "收到原始感測器數值: $rawValue")

            // 過濾掉無效的 0.0 數值
            if (rawValue > 0f) {
                // 呼叫您自訂的 callback
                onSensorDataChanged(rawValue)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 可根據精確度變化進行處理
    }
}