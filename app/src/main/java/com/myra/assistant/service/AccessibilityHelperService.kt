package com.myra.assistant.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AccessibilityHelperService : AccessibilityService() {

    private val TAG = "AccessibilityHelper"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    fun closeCurrentApp() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    companion object {
        var instance: AccessibilityHelperService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
