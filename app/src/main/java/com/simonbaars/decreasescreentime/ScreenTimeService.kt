package com.simonbaars.decreasescreentime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class ScreenTimeService : Service() {
    
    private var screenOnTime: Long = 0
    private var lastScreenOnTimestamp: Long = 0
    private var isScreenOn = false
    private val handler = Handler(Looper.getMainLooper())
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isScreenOn) {
                updateScreenTime()
                handler.postDelayed(this, 30 * 1000) // Update every 30 seconds
            }
        }
    }
    
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    lastScreenOnTimestamp = System.currentTimeMillis()
                    updateScreenTime()
                    schedulePeriodicUpdates()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (isScreenOn) {
                        cancelPeriodicUpdates()
                        val sessionTime = System.currentTimeMillis() - lastScreenOnTimestamp
                        screenOnTime += sessionTime
                        saveScreenTime()
                        isScreenOn = false
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register screen state receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }
        
        loadScreenTime()
        
        // Check if screen is currently on
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (powerManager.isInteractive) {
            isScreenOn = true
            lastScreenOnTimestamp = System.currentTimeMillis()
            schedulePeriodicUpdates()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESET_SCREEN_TIME -> {
                resetScreenTime()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cancelPeriodicUpdates()
        if (isScreenOn) {
            val sessionTime = System.currentTimeMillis() - lastScreenOnTimestamp
            screenOnTime += sessionTime
            saveScreenTime()
        }
        unregisterReceiver(screenStateReceiver)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your screen time"
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Tracker")
            .setContentText("Tracking your screen usage")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
    
    private fun loadScreenTime() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        screenOnTime = prefs.getLong(KEY_SCREEN_TIME, 0)
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "")
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        
        // Reset daily at midnight
        if (lastResetDate != today) {
            screenOnTime = 0
            prefs.edit()
                .putLong(KEY_SCREEN_TIME, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(ScreenUnlockReceiver.KEY_UNLOCK_COUNT, 0)
                .apply()
        }
    }
    
    private fun saveScreenTime() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SCREEN_TIME, screenOnTime)
            .apply()
    }
    
    private fun updateScreenTime() {
        if (isScreenOn) {
            val currentSessionTime = System.currentTimeMillis() - lastScreenOnTimestamp
            val totalTime = screenOnTime + currentSessionTime
            
            // Broadcast screen time update
            val intent = Intent(ACTION_SCREEN_TIME_UPDATE).apply {
                putExtra(EXTRA_SCREEN_TIME, totalTime)
            }
            sendBroadcast(intent)
            
            // Periodically save the accumulated time to persist progress
            // Update screenOnTime and reset the session timestamp to avoid double-counting
            screenOnTime = totalTime
            lastScreenOnTimestamp = System.currentTimeMillis()
            saveScreenTime()
        }
    }
    
    private fun resetScreenTime() {
        screenOnTime = 0
        lastScreenOnTimestamp = System.currentTimeMillis()
        saveScreenTime()
        
        // Reset unlock count too
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(ScreenUnlockReceiver.KEY_UNLOCK_COUNT, 0)
            .apply()
        
        // Broadcast reset to update UI immediately
        updateScreenTime()
    }
    
    private fun schedulePeriodicUpdates() {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }
    
    private fun cancelPeriodicUpdates() {
        handler.removeCallbacks(updateRunnable)
    }
    
    companion object {
        const val CHANNEL_ID = "screen_time_channel"
        const val NOTIFICATION_ID = 1
        const val PREFS_NAME = "screen_time_prefs"
        const val KEY_SCREEN_TIME = "screen_time"
        const val KEY_LAST_RESET_DATE = "last_reset_date"
        const val ACTION_RESET_SCREEN_TIME = "com.simonbaars.decreasescreentime.RESET_SCREEN_TIME"
        const val ACTION_SCREEN_TIME_UPDATE = "com.simonbaars.decreasescreentime.SCREEN_TIME_UPDATE"
        const val EXTRA_SCREEN_TIME = "extra_screen_time"
    }
}
