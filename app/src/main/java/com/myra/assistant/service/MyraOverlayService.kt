package com.myra.assistant.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.view.WindowManager

class MyraOverlayService : Service() {

    private val TAG = "MyraOverlayService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MyraOverlayService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
