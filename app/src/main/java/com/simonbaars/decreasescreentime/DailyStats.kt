package com.simonbaars.decreasescreentime

data class DailyStats(
    val date: String,
    val screenTimeMs: Long,
    val unlockCount: Int
)
