package com.myra.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CallMonitorService : Service() {

    private val TAG = "CallMonitorService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CallMonitorService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
