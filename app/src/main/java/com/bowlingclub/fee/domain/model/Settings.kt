package com.bowlingclub.fee.domain.model

import com.bowlingclub.fee.domain.Constants

data class AppSettings(
    val clubName: String = "볼링 동호회",
    val defaultFeeAmount: Int = Constants.DEFAULT_FEE_AMOUNT,
    val averageGameCount: Int = 12,
    val handicapUpperLimit: Int = 50,
    val enableAutoBackup: Boolean = false,
    val gameFeePerGame: Int = Constants.GAME_FEE_PER_GAME
)
