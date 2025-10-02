package com.simonbaars.decreasescreentime

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Thresholds (in minutes)
    var overlayThresholdMinutes: Int
        get() = prefs.getInt(KEY_OVERLAY_THRESHOLD, DEFAULT_OVERLAY_THRESHOLD)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_THRESHOLD, value).apply()
    
    var highScreenTimeThresholdMinutes: Int
        get() = prefs.getInt(KEY_HIGH_THRESHOLD, DEFAULT_HIGH_THRESHOLD)
        set(value) = prefs.edit().putInt(KEY_HIGH_THRESHOLD, value).apply()
    
    var excessiveScreenTimeThresholdMinutes: Int
        get() = prefs.getInt(KEY_EXCESSIVE_THRESHOLD, DEFAULT_EXCESSIVE_THRESHOLD)
        set(value) = prefs.edit().putInt(KEY_EXCESSIVE_THRESHOLD, value).apply()
    
    // Popup settings
    var popupFrequencyMinutes: Int
        get() = prefs.getInt(KEY_POPUP_FREQUENCY, DEFAULT_POPUP_FREQUENCY)
        set(value) = prefs.edit().putInt(KEY_POPUP_FREQUENCY, value).apply()
    
    // Update interval (in seconds)
    var updateIntervalSeconds: Int
        get() = prefs.getInt(KEY_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_UPDATE_INTERVAL, value).apply()
    
    // Text sizes
    var smallTextSize: Float
        get() = prefs.getFloat(KEY_SMALL_TEXT_SIZE, DEFAULT_SMALL_TEXT_SIZE)
        set(value) = prefs.edit().putFloat(KEY_SMALL_TEXT_SIZE, value).apply()
    
    var largeTextSize: Float
        get() = prefs.getFloat(KEY_LARGE_TEXT_SIZE, DEFAULT_LARGE_TEXT_SIZE)
        set(value) = prefs.edit().putFloat(KEY_LARGE_TEXT_SIZE, value).apply()
    
    // Dimming settings
    var initialDimAmount: Float
        get() = prefs.getFloat(KEY_INITIAL_DIM, DEFAULT_INITIAL_DIM)
        set(value) = prefs.edit().putFloat(KEY_INITIAL_DIM, value).apply()
    
    var dimIncrementPerHour: Float
        get() = prefs.getFloat(KEY_DIM_INCREMENT, DEFAULT_DIM_INCREMENT)
        set(value) = prefs.edit().putFloat(KEY_DIM_INCREMENT, value).apply()
    
    var maxDimAmount: Float
        get() = prefs.getFloat(KEY_MAX_DIM, DEFAULT_MAX_DIM)
        set(value) = prefs.edit().putFloat(KEY_MAX_DIM, value).apply()
    
    companion object {
        private const val PREFS_NAME = "settings_prefs"
        
        // Keys
        private const val KEY_OVERLAY_THRESHOLD = "overlay_threshold"
        private const val KEY_HIGH_THRESHOLD = "high_threshold"
        private const val KEY_EXCESSIVE_THRESHOLD = "excessive_threshold"
        private const val KEY_POPUP_FREQUENCY = "popup_frequency"
        private const val KEY_UPDATE_INTERVAL = "update_interval"
        private const val KEY_SMALL_TEXT_SIZE = "small_text_size"
        private const val KEY_LARGE_TEXT_SIZE = "large_text_size"
        private const val KEY_INITIAL_DIM = "initial_dim"
        private const val KEY_DIM_INCREMENT = "dim_increment"
        private const val KEY_MAX_DIM = "max_dim"
        
        // Default values
        const val DEFAULT_OVERLAY_THRESHOLD = 15  // minutes
        const val DEFAULT_HIGH_THRESHOLD = 30     // minutes
        const val DEFAULT_EXCESSIVE_THRESHOLD = 60 // minutes
        const val DEFAULT_POPUP_FREQUENCY = 2     // minutes
        const val DEFAULT_UPDATE_INTERVAL = 30    // seconds
        const val DEFAULT_SMALL_TEXT_SIZE = 18f
        const val DEFAULT_LARGE_TEXT_SIZE = 24f
        const val DEFAULT_INITIAL_DIM = 0.3f      // 30%
        const val DEFAULT_DIM_INCREMENT = 0.1f    // 10% per hour
        const val DEFAULT_MAX_DIM = 0.5f          // 50%
    }
}
