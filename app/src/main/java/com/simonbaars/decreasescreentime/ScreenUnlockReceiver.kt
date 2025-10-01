package com.simonbaars.decreasescreentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ScreenUnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT && context != null) {
            incrementUnlockCount(context)
            ensureServicesRunning(context)
        }
    }

    private fun incrementUnlockCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt(KEY_UNLOCK_COUNT, 0)
        prefs.edit().putInt(KEY_UNLOCK_COUNT, currentCount + 1).apply()
    }
    
    private fun ensureServicesRunning(context: Context) {
        // Start ScreenTimeService if not already running
        val screenTimeIntent = Intent(context, ScreenTimeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(screenTimeIntent)
        } else {
            context.startService(screenTimeIntent)
        }
        
        // Start OverlayService if not already running
        val overlayIntent = Intent(context, OverlayService::class.java)
        context.startService(overlayIntent)
    }

    companion object {
        const val PREFS_NAME = "screen_time_prefs"
        const val KEY_UNLOCK_COUNT = "unlock_count"
    }
}
