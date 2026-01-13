package com.bowlingclub.fee.domain.model

data class AppSettings(
    val clubName: String = "볼링 동호회",
    val defaultFeeAmount: Int = 10000,
    val averageGameCount: Int = 12,
    val handicapUpperLimit: Int = 50,
    val enableAutoBackup: Boolean = false
)
