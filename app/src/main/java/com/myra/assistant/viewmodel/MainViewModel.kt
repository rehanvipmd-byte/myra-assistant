package com.myra.assistant.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModel(private val context: Context) : ViewModel() {

    private val TAG = "MainViewModel"
    private val _commandResult = androidx.lifecycle.MutableLiveData<String?>()
    val commandResult: androidx.lifecycle.LiveData<String?> = _commandResult

    private val appPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "settings" to "com.android.settings",
        "calculator" to "com.android.calculator2",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "phone" to "com.android.phone",
        "contacts" to "com.android.contacts",
        "play store" to "com.android.vending",
        "amazon" to "com.amazon.mShop.android",
        "flipkart" to "com.flipkart.android",
        "paytm" to "com.paytm",
        "phonepe" to "com.phonepe.app",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "zoom" to "us.zoom.videomeetings",
        "meet" to "com.google.android.apps.meetup",
        "teams" to "com.microsoft.teams",
        "tiktok" to "com.ss.android.ugc.trill",
        "discord" to "com.discord",
        "linkedin" to "com.linkedin.android",
        "camera" to "com.android.camera"
    )

    fun executeCommand(command: com.myra.assistant.model.AppCommand) {
        androidx.lifecycle.viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                when (command.type) {
                    com.myra.assistant.model.AppCommand.OPEN_APP -> openApp(command.params["app_name"] ?: "")
                    com.myra.assistant.model.AppCommand.CLOSE_APP -> closeApp()
                    com.myra.assistant.model.AppCommand.CALL -> makeCall(command.params["name"] ?: "")
                    com.myra.assistant.model.AppCommand.SMS -> sendSMS(command.params["name"] ?: "")
                    com.myra.assistant.model.AppCommand.WHATSAPP_MSG -> sendWhatsAppMessage(command.params["name"] ?: "")
                    com.myra.assistant.model.AppCommand.WHATSAPP_CALL -> makeWhatsAppCall(command.params["name"] ?: "")
                    com.myra.assistant.model.AppCommand.PRIME_CALL -> makePrimeCall(command.params["index"]?.toIntOrNull() ?: 0)
                    com.myra.assistant.model.AppCommand.PRIME_MSG -> sendPrimeMessage(command.params["index"]?.toIntOrNull() ?: 0)
                    com.myra.assistant.model.AppCommand.VOLUME_UP -> setVolume(true)
                    com.myra.assistant.model.AppCommand.VOLUME_DOWN -> setVolume(false)
                    com.myra.assistant.model.AppCommand.FLASHLIGHT_ON -> controlFlashlight(true)
                    com.myra.assistant.model.AppCommand.FLASHLIGHT_OFF -> controlFlashlight(false)
                    com.myra.assistant.model.AppCommand.WIFI_ON -> controlWifi(true)
                    com.myra.assistant.model.AppCommand.WIFI_OFF -> controlWifi(false)
                    com.myra.assistant.model.AppCommand.BLUETOOTH_ON -> controlBluetooth(true)
                    com.myra.assistant.model.AppCommand.BLUETOOTH_OFF -> controlBluetooth(false)
                    else -> _commandResult.postValue("Unknown command: ${command.type}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error executing command: ${e.message}")
                _commandResult.postValue("Error: ${e.message}")
            }
        }
    }

    private fun openApp(appName: String) {
        val packageName = appPackageMap[appName.lowercase()] ?: return scanInstalledApps(appName)
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                _commandResult.postValue("Opening $appName")
            } else {
                _commandResult.postValue("$appName not installed")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Failed to open $appName")
        }
    }

    private fun scanInstalledApps(appName: String) {
        try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            for (app in apps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(appName.lowercase())) {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        context.startActivity(intent)
                        _commandResult.postValue("Opening $appName")
                        return
                    }
                }
            }
            _commandResult.postValue("App '$appName' not found")
        } catch (e: Exception) {
            _commandResult.postValue("Error scanning apps: ${e.message}")
        }
    }

    private fun closeApp() {
        _commandResult.postValue("App closed")
    }

    private fun makeCall(name: String) {
        _commandResult.postValue("Calling $name")
    }

    private fun sendSMS(name: String) {
        _commandResult.postValue("SMS sent to $name")
    }

    private fun sendWhatsAppMessage(name: String) {
        _commandResult.postValue("WhatsApp message sent to $name")
    }

    private fun makeWhatsAppCall(name: String) {
        _commandResult.postValue("WhatsApp call initiated for $name")
    }

    private fun makePrimeCall(index: Int) {
        _commandResult.postValue("Prime call initiated")
    }

    private fun sendPrimeMessage(index: Int) {
        _commandResult.postValue("Prime message sent")
    }

    private fun setVolume(increase: Boolean) {
        _commandResult.postValue(if (increase) "Volume increased" else "Volume decreased")
    }

    private fun controlFlashlight(on: Boolean) {
        _commandResult.postValue(if (on) "Flashlight on" else "Flashlight off")
    }

    private fun controlWifi(on: Boolean) {
        _commandResult.postValue(if (on) "WiFi on" else "WiFi off")
    }

    private fun controlBluetooth(on: Boolean) {
        _commandResult.postValue(if (on) "Bluetooth on" else "Bluetooth off")
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
    }
}
