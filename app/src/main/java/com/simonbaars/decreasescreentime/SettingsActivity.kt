package com.simonbaars.decreasescreentime

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var settingsManager: SettingsManager
    
    // Threshold fields
    private lateinit var overlayThresholdInput: EditText
    private lateinit var highThresholdInput: EditText
    private lateinit var excessiveThresholdInput: EditText
    
    // Popup settings
    private lateinit var popupFrequencyInput: EditText
    
    // Update interval
    private lateinit var updateIntervalInput: EditText
    
    // Text sizes
    private lateinit var smallTextSizeInput: EditText
    private lateinit var largeTextSizeInput: EditText
    
    // Dimming settings
    private lateinit var initialDimInput: EditText
    private lateinit var dimIncrementInput: EditText
    private lateinit var dimIncrementIntervalInput: EditText
    private lateinit var maxDimInput: EditText
    
    // Buttons
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        settingsManager = SettingsManager(this)
        
        // Initialize views
        overlayThresholdInput = findViewById(R.id.overlayThresholdInput)
        highThresholdInput = findViewById(R.id.highThresholdInput)
        excessiveThresholdInput = findViewById(R.id.excessiveThresholdInput)
        popupFrequencyInput = findViewById(R.id.popupFrequencyInput)
        updateIntervalInput = findViewById(R.id.updateIntervalInput)
        smallTextSizeInput = findViewById(R.id.smallTextSizeInput)
        largeTextSizeInput = findViewById(R.id.largeTextSizeInput)
        initialDimInput = findViewById(R.id.initialDimInput)
        dimIncrementInput = findViewById(R.id.dimIncrementInput)
        dimIncrementIntervalInput = findViewById(R.id.dimIncrementIntervalInput)
        maxDimInput = findViewById(R.id.maxDimInput)
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetDefaultsButton)
        
        // Load current settings
        loadSettings()
        
        // Set up button listeners
        saveButton.setOnClickListener {
            saveSettings()
        }
        
        resetButton.setOnClickListener {
            resetToDefaults()
        }
    }
    
    private fun loadSettings() {
        overlayThresholdInput.setText(settingsManager.overlayThresholdMinutes.toString())
        highThresholdInput.setText(settingsManager.highScreenTimeThresholdMinutes.toString())
        excessiveThresholdInput.setText(settingsManager.excessiveScreenTimeThresholdMinutes.toString())
        popupFrequencyInput.setText(settingsManager.popupFrequencyMinutes.toString())
        updateIntervalInput.setText(settingsManager.updateIntervalSeconds.toString())
        smallTextSizeInput.setText(settingsManager.smallTextSize.toString())
        largeTextSizeInput.setText(settingsManager.largeTextSize.toString())
        initialDimInput.setText(settingsManager.initialDimAmount.toString())
        dimIncrementInput.setText(settingsManager.dimIncrement.toString())
        dimIncrementIntervalInput.setText(settingsManager.dimIncrementIntervalMinutes.toString())
        maxDimInput.setText(settingsManager.maxDimAmount.toString())
    }
    
    private fun saveSettings() {
        try {
            settingsManager.overlayThresholdMinutes = overlayThresholdInput.text.toString().toInt()
            settingsManager.highScreenTimeThresholdMinutes = highThresholdInput.text.toString().toInt()
            settingsManager.excessiveScreenTimeThresholdMinutes = excessiveThresholdInput.text.toString().toInt()
            settingsManager.popupFrequencyMinutes = popupFrequencyInput.text.toString().toInt()
            settingsManager.updateIntervalSeconds = updateIntervalInput.text.toString().toInt()
            settingsManager.smallTextSize = smallTextSizeInput.text.toString().toFloat()
            settingsManager.largeTextSize = largeTextSizeInput.text.toString().toFloat()
            settingsManager.initialDimAmount = initialDimInput.text.toString().toFloat()
            settingsManager.dimIncrement = dimIncrementInput.text.toString().toFloat()
            settingsManager.dimIncrementIntervalMinutes = dimIncrementIntervalInput.text.toString().toInt()
            settingsManager.maxDimAmount = maxDimInput.text.toString().toFloat()
            
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid numbers for all fields", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun resetToDefaults() {
        overlayThresholdInput.setText(SettingsManager.DEFAULT_OVERLAY_THRESHOLD.toString())
        highThresholdInput.setText(SettingsManager.DEFAULT_HIGH_THRESHOLD.toString())
        excessiveThresholdInput.setText(SettingsManager.DEFAULT_EXCESSIVE_THRESHOLD.toString())
        popupFrequencyInput.setText(SettingsManager.DEFAULT_POPUP_FREQUENCY.toString())
        updateIntervalInput.setText(SettingsManager.DEFAULT_UPDATE_INTERVAL.toString())
        smallTextSizeInput.setText(SettingsManager.DEFAULT_SMALL_TEXT_SIZE.toString())
        largeTextSizeInput.setText(SettingsManager.DEFAULT_LARGE_TEXT_SIZE.toString())
        initialDimInput.setText(SettingsManager.DEFAULT_INITIAL_DIM.toString())
        dimIncrementInput.setText(SettingsManager.DEFAULT_DIM_INCREMENT.toString())
        dimIncrementIntervalInput.setText(SettingsManager.DEFAULT_DIM_INCREMENT_INTERVAL.toString())
        maxDimInput.setText(SettingsManager.DEFAULT_MAX_DIM.toString())
        
        Toast.makeText(this, "Reset to default values", Toast.LENGTH_SHORT).show()
    }
}
