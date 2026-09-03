package com.example.data.model

data class SystemInfo(
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val internalStorageFreeGb: Float = 0f,
    val isTorchOn: Boolean = false
) {
    val ramPercent: Int
        get() = if (ramTotalMb > 0) ((ramUsedMb.toDouble() / ramTotalMb.toDouble()) * 100).toInt() else 0
}
