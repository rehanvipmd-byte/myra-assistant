package com.myra.assistant.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myra.assistant.model.AppCommand
import com.myra.assistant.service.AccessibilityHelperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val context: Context) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _commandResult = MutableLiveData<String?>()
    val commandResult: LiveData<String?> = _commandResult

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

    fun executeCommand(command: AppCommand) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (command.type) {
                    AppCommand.OPEN_APP -> openApp(command.params["app_name"] ?: "")
                    AppCommand.CLOSE_APP -> closeApp()
                    AppCommand.CALL -> makeCall(command.params["name"] ?: "")
                    AppCommand.SMS -> sendSMS(command.params["name"] ?: "")
                    AppCommand.WHATSAPP_MSG -> sendWhatsAppMessage(command.params["name"] ?: "")
                    AppCommand.WHATSAPP_CALL -> makeWhatsAppCall(command.params["name"] ?: "")
                    AppCommand.PRIME_CALL -> makePrimeCall(command.params["index"]?.toIntOrNull() ?: 0)
                    AppCommand.PRIME_MSG -> sendPrimeMessage(command.params["index"]?.toIntOrNull() ?: 0)
                    AppCommand.VOLUME_UP -> setVolume(true)
                    AppCommand.VOLUME_DOWN -> setVolume(false)
                    AppCommand.FLASHLIGHT_ON -> controlFlashlight(true)
                    AppCommand.FLASHLIGHT_OFF -> controlFlashlight(false)
                    AppCommand.WIFI_ON -> controlWifi(true)
                    AppCommand.WIFI_OFF -> controlWifi(false)
                    AppCommand.BLUETOOTH_ON -> controlBluetooth(true)
                    AppCommand.BLUETOOTH_OFF -> controlBluetooth(false)
                    else -> _commandResult.postValue("Unknown command: ${command.type}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: ${e.message}")
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
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
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
        try {
            AccessibilityHelperService.instance?.closeCurrentApp()
            _commandResult.postValue("App closed")
        } catch (e: Exception) {
            _commandResult.postValue("Failed to close app")
        }
    }

    private fun makeCall(name: String) {
        try {
            val number = resolveContactNumber(name)
            if (number != null) {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:$number")
                context.startActivity(intent)
                _commandResult.postValue("Calling $name")
            } else {
                _commandResult.postValue("Contact not found: $name")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error making call: ${e.message}")
        }
    }

    private fun sendSMS(name: String) {
        try {
            val number = resolveContactNumber(name)
            if (number != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("smsto:$number")
                context.startActivity(intent)
                _commandResult.postValue("SMS opened for $name")
            } else {
                _commandResult.postValue("Contact not found: $name")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error sending SMS: ${e.message}")
        }
    }

    private fun sendWhatsAppMessage(name: String) {
        try {
            val number = resolveContactNumber(name)
            if (number != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://wa.me/$number")
                context.startActivity(intent)
                _commandResult.postValue("WhatsApp opened for $name")
            } else {
                _commandResult.postValue("Contact not found: $name")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error opening WhatsApp: ${e.message}")
        }
    }

    private fun makeWhatsAppCall(name: String) {
        try {
            val number = resolveContactNumber(name)
            if (number != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://wa.me/$number?text=Hello")
                context.startActivity(intent)
                _commandResult.postValue("WhatsApp call initiated for $name")
            } else {
                _commandResult.postValue("Contact not found: $name")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error initiating WhatsApp call: ${e.message}")
        }
    }

    private fun makePrimeCall(index: Int) {
        try {
            val primeContacts = loadPrimeContacts()
            if (index < primeContacts.size) {
                val contact = primeContacts[index]
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:${contact["number"]}")
                context.startActivity(intent)
                _commandResult.postValue("Calling ${contact["name"]}")
            } else {
                _commandResult.postValue("Prime contact not found")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error calling prime contact: ${e.message}")
        }
    }

    private fun sendPrimeMessage(index: Int) {
        try {
            val primeContacts = loadPrimeContacts()
            if (index < primeContacts.size) {
                val contact = primeContacts[index]
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://wa.me/${contact["number"]}")
                context.startActivity(intent)
                _commandResult.postValue("WhatsApp opened for ${contact["name"]}")
            } else {
                _commandResult.postValue("Prime contact not found")
            }
        } catch (e: Exception) {
            _commandResult.postValue("Error sending message to prime contact: ${e.message}")
        }
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

    private fun resolveContactNumber(name: String): String? {
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$name%")
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    return it.getString(numberIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact: ${e.message}")
        }
        return null
    }

    private fun loadPrimeContacts(): List<Map<String, String>> {
        val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        // TODO: Parse JSON from prefs
        return emptyList()
    }
}
