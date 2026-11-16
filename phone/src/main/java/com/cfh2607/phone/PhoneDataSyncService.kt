package com.cfh2607.phone

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.File

class PhoneDataSyncService : WearableListenerService() {

    companion object {
        private const val TAG = "GlucoseSync_Phone"
        private const val SYNC_PATH = "/glucose_data_sync"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        for (event in dataEvents) {

            if (event.type != DataEvent.TYPE_CHANGED) continue

            if (event.dataItem.uri.path != SYNC_PATH) continue

            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            val asset = dataMap.getAsset("csv_file") ?: continue

            val receivedName = dataMap.getString("file_name")
                ?: "calibration_${System.currentTimeMillis()}.csv"

            // 避免檔名包含路徑
            val safeName = File(receivedName).name
            val folder = File(filesDir, "calibration")

            if (!folder.exists()) {
                folder.mkdirs()
            }

            val outFile = File(folder, safeName)

            Wearable.getDataClient(this)
                .getFdForAsset(asset)
                .addOnSuccessListener { response ->

                    try {
                        response.inputStream.use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }

                        Log.d(
                            TAG,
                            "Calibration CSV saved: ${outFile.absolutePath}"
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "CSV save failed", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Unable to receive CSV Asset", e)
                }
        }
    }
}
