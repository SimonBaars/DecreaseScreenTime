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
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import kotlin.random.Random

class OverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentScreenTime: Long = 0
    private var annoyingAnimationsActive = false
    private lateinit var settingsManager: SettingsManager
    private val annoyingViews = mutableListOf<View>()
    private val annoyingEmojis = listOf("🥕", "🙈", "📱", "⏰", "👀", "🚫", "😴", "🌙", "🏃", "🧘")
    
    private val screenTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenTimeService.ACTION_SCREEN_TIME_UPDATE) {
                currentScreenTime = intent.getLongExtra(ScreenTimeService.EXTRA_SCREEN_TIME, 0)
                Log.d(TAG, "screenTimeReceiver: received update, screenTime=$currentScreenTime ms")
                updateOverlay()
            }
        }
    }
    
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                Log.d(TAG, "screenOnReceiver: screen turned on")
                schedulePeriodicUpdate()
                // Reload screen time and update overlay immediately when screen turns on
                loadInitialScreenTime()
                updateOverlay()
            } else if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d(TAG, "screenOnReceiver: screen turned off")
                cancelPeriodicUpdate()
            }
        }
    }
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            // Reload screen time from SharedPreferences to ensure we have the latest value
            loadInitialScreenTime()
            updateOverlay()
            // Show annoying animations on overlay when excessive threshold is reached
            val minutes = currentScreenTime / (60 * 1000)
            if (minutes >= settingsManager.excessiveScreenTimeThresholdMinutes) {
                if (!annoyingAnimationsActive) {
                    startAnnoyingAnimations()
                    annoyingAnimationsActive = true
                }
            } else {
                if (annoyingAnimationsActive) {
                    stopAnnoyingAnimations()
                    annoyingAnimationsActive = false
                }
            }
            handler.postDelayed(this, settingsManager.updateIntervalSeconds * 1000L)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: OverlayService starting")
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsManager = SettingsManager(this)
        
        // Load initial screen time from SharedPreferences
        loadInitialScreenTime()
        Log.d(TAG, "onCreate: initial screen time = $currentScreenTime ms")
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val canDrawOverlays = Settings.canDrawOverlays(this)
            Log.d(TAG, "onCreate: overlay permission granted = $canDrawOverlays")
        }
        
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
        
        // Trigger initial overlay update
        updateOverlay()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAnnoyingAnimations()
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
        
        Log.d(TAG, "updateOverlay: minutes=$minutes, threshold=${settingsManager.overlayThresholdMinutes}")
        
        if (minutes >= settingsManager.overlayThresholdMinutes) {
            // Check if we have overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Log.e(TAG, "Overlay permission not granted!")
                    return
                }
            }
            
            if (overlayView == null) {
                createOverlay()
            }
            updateOverlayContent(minutes)
        } else {
            removeOverlay()
        }
    }
    
    private fun createOverlay() {
        Log.d(TAG, "createOverlay: attempting to create overlay")
        
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
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }
        
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        
        try {
            windowManager.addView(overlayView, layoutParams)
            Log.d(TAG, "createOverlay: overlay created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "createOverlay: failed to create overlay", e)
            overlayView = null
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
            
            Log.d(TAG, "updateOverlayContent: updating message to: $message")
            messageText.text = message
            
            // Adjust text size based on thresholds
            val textSize = when {
                minutes >= settingsManager.highScreenTimeThresholdMinutes -> settingsManager.largeTextSize
                else -> settingsManager.smallTextSize
            }
            messageText.textSize = textSize
        } ?: Log.w(TAG, "updateOverlayContent: overlayView is null")
    }
    

    private fun removeOverlay() {
        overlayView?.let {
            Log.d(TAG, "removeOverlay: removing overlay")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "removeOverlay: failed to remove view", e)
            }
            overlayView = null
        }
    }
    
    private fun startAnnoyingAnimations() {
        Log.d(TAG, "startAnnoyingAnimations: starting annoying overlay animations")
        
        // Create multiple emoji views that bounce around
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Add 3-5 random emoji views
        val numEmojis = Random.nextInt(3, 6)
        for (i in 0 until numEmojis) {
            val emojiView = TextView(this).apply {
                text = annoyingEmojis.random()
                textSize = Random.nextInt(40, 80).toFloat()
                gravity = Gravity.CENTER
            }
            
            val params = WindowManager.LayoutParams(
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
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = Random.nextInt(0, screenWidth - 200)
                y = Random.nextInt(100, screenHeight / 2)
            }
            
            try {
                windowManager.addView(emojiView, params)
                annoyingViews.add(emojiView)
                
                // Start bouncing animation
                startBouncingAnimation(emojiView, params, screenWidth, screenHeight)
            } catch (e: Exception) {
                Log.e(TAG, "startAnnoyingAnimations: failed to add emoji view", e)
            }
        }
        
        // Schedule periodic emoji flashing and repositioning
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (annoyingAnimationsActive && annoyingViews.isNotEmpty()) {
                    // Randomly flash an emoji or reposition one
                    val randomView = annoyingViews.random()
                    if (Random.nextBoolean()) {
                        flashEmoji(randomView)
                    } else {
                        repositionEmoji(randomView, screenWidth, screenHeight)
                    }
                    handler.postDelayed(this, Random.nextLong(2000, 5000))
                }
            }
        }, 2000)
    }
    
    private fun startBouncingAnimation(view: View, params: WindowManager.LayoutParams, screenWidth: Int, screenHeight: Int) {
        // Create a bouncing animation
        val animation = TranslateAnimation(
            0f, Random.nextInt(-200, 200).toFloat(),
            0f, Random.nextInt(-300, 300).toFloat()
        ).apply {
            duration = Random.nextLong(2000, 4000)
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        
        view.startAnimation(animation)
    }
    
    private fun flashEmoji(view: View) {
        // Flash the emoji by changing its alpha
        view.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                if (view in annoyingViews) {
                    view.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
            .start()
    }
    
    private fun repositionEmoji(view: View, screenWidth: Int, screenHeight: Int) {
        // Reposition the emoji to a new random location
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.x = Random.nextInt(0, screenWidth - 200)
        params.y = Random.nextInt(100, screenHeight / 2)
        
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.w(TAG, "repositionEmoji: failed to update view layout", e)
        }
    }
    
    private fun stopAnnoyingAnimations() {
        Log.d(TAG, "stopAnnoyingAnimations: stopping annoying animations")
        
        // Remove all annoying emoji views
        annoyingViews.forEach { view ->
            try {
                view.clearAnimation()
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "stopAnnoyingAnimations: failed to remove view", e)
            }
        }
        annoyingViews.clear()
    }
    
    companion object {
        private const val TAG = "OverlayService"
    }
}
