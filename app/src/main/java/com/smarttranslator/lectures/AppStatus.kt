package com.smarttranslator.lectures

sealed class AppStatus {
    data object Idle : AppStatus()
    data object LoadingModel : AppStatus()
    data object Recording : AppStatus()
    data object Processing : AppStatus()
    data class DownloadingModel(val progress: Float) : AppStatus()
    data class Error(val message: String) : AppStatus()
    data class Warning(val message: String) : AppStatus()
}