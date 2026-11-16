package com.cfh2607.phone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 修正此處：把 data_class 改為 data class
data class MainUiState(
    val isLoading: Boolean = false,
    val totalFiles: Int = 0,
    val modelIsReady: Boolean = false,
    val modelRSquared: Float = 0f,
    val latestPredictedGlucose: Float = 0f,
    val statusMessage: String = "尚未載入資料",
    val historySummaries: List<PpgFeatureSummary> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            val appContext = getApplication<Application>()

            // 1. 載入所有歷史特徵摘要
            val summaries = MultiFileCsvLoader.loadAllCalibrationTrends(appContext)

            // 2. 執行校準與預測更新邏輯
            val updateResult = RefreshCalibrationModelHelper.refreshAndPredict(appContext)

            // 3. 更新 UI 狀態 (修正此處的 modelIsReady 賦值)
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    totalFiles = updateResult.totalFilesLoaded,
                    modelIsReady = updateResult.modelIsReady, // 修正為從 updateResult 取得
                    modelRSquared = updateResult.modelRSquared,
                    latestPredictedGlucose = updateResult.latestPredictedGlucose,
                    statusMessage = updateResult.message,
                    historySummaries = summaries
                )
            }
        }
    }

}
