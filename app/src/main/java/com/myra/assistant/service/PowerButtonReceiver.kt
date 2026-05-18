package com.myra.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PowerButtonReceiver : BroadcastReceiver() {

    private val TAG = "PowerButtonReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> Log.d(TAG, "Screen off")
            Intent.ACTION_SCREEN_ON -> Log.d(TAG, "Screen on")
        }
    }
}
