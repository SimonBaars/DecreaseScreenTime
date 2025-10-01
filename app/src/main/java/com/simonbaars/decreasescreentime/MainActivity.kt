package com.simonbaars.decreasescreentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var unlockCountText: TextView
    private lateinit var resetButton: Button
    
    private val screenUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                updateUnlockCount()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        unlockCountText = findViewById(R.id.unlockCountText)
        resetButton = findViewById(R.id.resetButton)

        resetButton.setOnClickListener {
            resetUnlockCount()
        }

        updateUnlockCount()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(screenUnlockReceiver, filter)
        updateUnlockCount()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(screenUnlockReceiver)
    }

    private fun updateUnlockCount() {
        val prefs = getSharedPreferences(ScreenUnlockReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(ScreenUnlockReceiver.KEY_UNLOCK_COUNT, 0)
        unlockCountText.text = getString(R.string.unlock_count, count)
    }

    private fun resetUnlockCount() {
        val prefs = getSharedPreferences(ScreenUnlockReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(ScreenUnlockReceiver.KEY_UNLOCK_COUNT, 0).apply()
        updateUnlockCount()
    }
}
