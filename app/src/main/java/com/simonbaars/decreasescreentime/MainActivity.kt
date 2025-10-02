package com.simonbaars.decreasescreentime

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var unlockCountText: TextView
    private lateinit var screenTimeText: TextView
    private lateinit var resetButton: Button
    private lateinit var settingsButton: Button
    
    private val screenUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                updateUnlockCount()
            }
        }
    }
    
    private val screenTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenTimeService.ACTION_SCREEN_TIME_UPDATE) {
                val screenTime = intent.getLongExtra(ScreenTimeService.EXTRA_SCREEN_TIME, 0)
                updateScreenTime(screenTime)
            }
        }
    }
    
    private val popupReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == OverlayService.ACTION_SHOW_POPUP) {
                showAnnoyingPopup()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        unlockCountText = findViewById(R.id.unlockCountText)
        screenTimeText = findViewById(R.id.screenTimeText)
        resetButton = findViewById(R.id.resetButton)
        settingsButton = findViewById(R.id.settingsButton)

        resetButton.setOnClickListener {
            resetData()
        }
        
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        updateUnlockCount()
        updateScreenTimeFromPrefs()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
        
        // Start services
        startScreenTimeService()
        
        // Request overlay permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                startOverlayService()
            }
        } else {
            startOverlayService()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenUnlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenUnlockReceiver, filter)
        }
        
        val screenTimeFilter = IntentFilter(ScreenTimeService.ACTION_SCREEN_TIME_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenTimeReceiver, screenTimeFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenTimeReceiver, screenTimeFilter)
        }
        
        val popupFilter = IntentFilter(OverlayService.ACTION_SHOW_POPUP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(popupReceiver, popupFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(popupReceiver, popupFilter)
        }
        
        updateUnlockCount()
        updateScreenTimeFromPrefs()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenUnlockReceiver)
        unregisterReceiver(screenTimeReceiver)
        unregisterReceiver(popupReceiver)
    }

    private fun startScreenTimeService() {
        val intent = Intent(this, ScreenTimeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
    }
    
    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs overlay permission to show screen time warnings. Grant permission in the next screen.")
            .setPositiveButton("OK") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startOverlayService()
                }
            }
        }
    }

    private fun updateUnlockCount() {
        val prefs = getSharedPreferences(ScreenUnlockReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(ScreenUnlockReceiver.KEY_UNLOCK_COUNT, 0)
        unlockCountText.text = getString(R.string.unlock_count, count)
    }
    
    private fun updateScreenTimeFromPrefs() {
        val prefs = getSharedPreferences(ScreenTimeService.PREFS_NAME, Context.MODE_PRIVATE)
        val screenTime = prefs.getLong(ScreenTimeService.KEY_SCREEN_TIME, 0)
        updateScreenTime(screenTime)
    }
    
    private fun updateScreenTime(screenTimeMs: Long) {
        val minutes = screenTimeMs / (60 * 1000)
        val hours = minutes / 60
        val mins = minutes % 60
        
        val timeString = if (hours > 0) {
            getString(R.string.screen_time_hours, hours, mins)
        } else {
            getString(R.string.screen_time_minutes, mins)
        }
        
        screenTimeText.text = timeString
    }

    private fun resetData() {
        val intent = Intent(this, ScreenTimeService::class.java).apply {
            action = ScreenTimeService.ACTION_RESET_SCREEN_TIME
        }
        startService(intent)
        
        updateUnlockCount()
        updateScreenTimeFromPrefs()
    }
    
    private fun showAnnoyingPopup() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ EXCESSIVE SCREEN TIME ⚠️")
            .setMessage("You've been using your phone for over an hour today! Consider taking a break for your health and wellbeing.")
            .setCancelable(false)
            .setPositiveButton("I'll take a break") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
    }
}
