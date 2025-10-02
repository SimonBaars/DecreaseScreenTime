package com.simonbaars.decreasescreentime

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

class OverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentScreenTime: Long = 0
    private var popupShown = false
    private lateinit var settingsManager: SettingsManager
    
    private val screenTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenTimeService.ACTION_SCREEN_TIME_UPDATE) {
                currentScreenTime = intent.getLongExtra(ScreenTimeService.EXTRA_SCREEN_TIME, 0)
                updateOverlay()
            }
        }
    }
    
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                schedulePeriodicUpdate()
            } else if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                cancelPeriodicUpdate()
            }
        }
    }
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateOverlay()
            // Show popup based on configurable threshold and frequency
            val minutes = currentScreenTime / (60 * 1000)
            if (minutes >= settingsManager.excessiveScreenTimeThresholdMinutes && !popupShown) {
                showAnnoyingPopup()
                popupShown = true
                handler.postDelayed({ popupShown = false }, settingsManager.popupFrequencyMinutes * 60 * 1000L)
            }
            handler.postDelayed(this, settingsManager.updateIntervalSeconds * 1000L)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)
        
        // Load initial screen time from SharedPreferences
        loadInitialScreenTime()
        
        // Register receivers
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenTimeReceiver, IntentFilter(ScreenTimeService.ACTION_SCREEN_TIME_UPDATE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenTimeReceiver, IntentFilter(ScreenTimeService.ACTION_SCREEN_TIME_UPDATE))
        }
        
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOnReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOnReceiver, screenFilter)
        }
        
        schedulePeriodicUpdate()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        unregisterReceiver(screenTimeReceiver)
        unregisterReceiver(screenOnReceiver)
        cancelPeriodicUpdate()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun schedulePeriodicUpdate() {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }
    
    private fun cancelPeriodicUpdate() {
        handler.removeCallbacks(updateRunnable)
    }
    
    private fun loadInitialScreenTime() {
        val prefs = getSharedPreferences(ScreenTimeService.PREFS_NAME, Context.MODE_PRIVATE)
        currentScreenTime = prefs.getLong(ScreenTimeService.KEY_SCREEN_TIME, 0)
    }
    
    private fun updateOverlay() {
        val minutes = currentScreenTime / (60 * 1000)
        
        if (minutes >= settingsManager.overlayThresholdMinutes) {
            if (overlayView == null) {
                createOverlay()
            }
            updateOverlayContent(minutes)
            applyScreenDimming(minutes)
        } else {
            removeOverlay()
        }
    }
    
    private fun createOverlay() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        
        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            // Permission not granted, overlay won't be shown
        }
    }
    
    private fun updateOverlayContent(minutes: Long) {
        overlayView?.let { view ->
            val messageText = view.findViewById<TextView>(R.id.overlayMessage)
            val hours = minutes / 60
            val mins = minutes % 60
            
            val message = when {
                minutes >= settingsManager.excessiveScreenTimeThresholdMinutes -> getString(R.string.overlay_excessive_screen_time, hours, mins)
                minutes >= settingsManager.highScreenTimeThresholdMinutes -> getString(R.string.overlay_high_screen_time, mins)
                else -> getString(R.string.overlay_screen_time, mins)
            }
            
            messageText.text = message
            
            // Adjust text size based on thresholds
            val textSize = when {
                minutes >= settingsManager.highScreenTimeThresholdMinutes -> settingsManager.largeTextSize
                else -> settingsManager.smallTextSize
            }
            messageText.textSize = textSize
        }
    }
    
    private fun applyScreenDimming(minutes: Long) {
        if (minutes >= settingsManager.excessiveScreenTimeThresholdMinutes) {
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                
                // Progressive dimming: more time = darker
                val hoursOver = (minutes - settingsManager.excessiveScreenTimeThresholdMinutes) / 60f
                val dimAmount = (settingsManager.initialDimAmount + hoursOver * settingsManager.dimIncrementPerHour).coerceAtMost(settingsManager.maxDimAmount)
                
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                params.dimAmount = dimAmount
                
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // Ignore update errors
                }
            }
        }
    }
    
    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // View already removed
            }
            overlayView = null
        }
    }
    
    private fun showAnnoyingPopup() {
        // Can't show AlertDialog from service directly, send broadcast to MainActivity
        val intent = Intent(ACTION_SHOW_POPUP)
        sendBroadcast(intent)
    }
    
    companion object {
        const val ACTION_SHOW_POPUP = "com.simonbaars.decreasescreentime.SHOW_POPUP"
    }
}
