package com.simonbaars.decreasescreentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT && context != null) {
            incrementUnlockCount(context)
        }
    }

    private fun incrementUnlockCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_UNLOCK_COUNT, 0)
        prefs.edit().putInt(KEY_UNLOCK_COUNT, currentCount + 1).apply()
    }

    companion object {
        const val PREFS_NAME = "screen_time_prefs"
        const val KEY_UNLOCK_COUNT = "unlock_count"
    }
}
