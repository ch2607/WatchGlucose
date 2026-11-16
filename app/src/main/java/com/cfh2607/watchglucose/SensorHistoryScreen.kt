package com.cfh2607.watchglucose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Text
import com.cfh2607.watchglucose.data.SensorDao
import com.cfh2607.watchglucose.data.SensorRecord
import com.cfh2607.watchglucose.service.WatchSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SensorHistoryScreen(dao: SensorDao) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val records by dao.getAllRecordsFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    // ✅ 新增：用來儲存你手動輸入的指尖血糖值
    var inputGlucose by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 手動輸入指尖血糖框
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputGlucose.isEmpty()) {
                    Text(
                        text = "輸入指尖血糖 (mg/dL)",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputGlucose,
                    onValueChange = { inputGlucose = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. 儲存並同步按鈕（已拿掉 115.0f）
        item {
            Button(
                onClick = {
                    val measuredGlucose = inputGlucose.toFloatOrNull()
                    if (measuredGlucose != null && measuredGlucose > 0f) {
                        coroutineScope.launch(Dispatchers.IO) {
                            dao.insertRecord(
                                SensorRecord(
                                    value = measuredGlucose,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            WatchSyncHelper.sendGlucoseToPhone(
                                context,
                                measuredGlucose.toDouble(),
                                "Manual"
                            )
                            // 送出後清空輸入框
                            inputGlucose = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("記錄並同步到手機")
            }
        }

        // 3. 向手機請求同步資料按鈕
        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        WatchSyncHelper.requestSyncFromPhone(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "向手機同步資料")
            }
        }

        // 4. 清空所有歷史按鈕
        if (records.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            dao.clearAllRecords()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "清空所有歷史")
                }
            }
        }

        // 5. 顯示歷史紀錄列表
        items(records.size) { index ->
            val record = records[index]
            Card(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        dao.deleteRecord(record)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${record.value.toInt()} mg/dL")
                    Text(
                        text = java.text.SimpleDateFormat(
                            "HH:mm:ss",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(record.timestamp))
                    )
                }
            }
        }
    }
}