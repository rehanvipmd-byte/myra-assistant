package com.myra.assistant.ai

import android.util.Log
import com.myra.assistant.model.AppCommand

class CommandParser {

    private val TAG = "CommandParser"

    fun parse(transcript: String): AppCommand? {
        val text = transcript.lowercase().trim()

        return when {
            // OPEN APP commands
            matchKeywords(text, listOf("youtube", "kholo", "open youtube")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "youtube"))
            
            matchKeywords(text, listOf("whatsapp", "kholo", "open whatsapp")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "whatsapp"))
            
            matchKeywords(text, listOf("instagram", "kholo", "open instagram")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "instagram"))
            
            matchKeywords(text, listOf("facebook", "kholo", "open facebook")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "facebook"))
            
            matchKeywords(text, listOf("chrome", "kholo", "open chrome")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "chrome"))
            
            matchKeywords(text, listOf("gmail", "kholo", "open gmail")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "gmail"))
            
            matchKeywords(text, listOf("maps", "kholo", "open maps")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "maps"))
            
            matchKeywords(text, listOf("spotify", "kholo", "open spotify")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "spotify"))
            
            matchKeywords(text, listOf("netflix", "kholo", "open netflix")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "netflix"))
            
            matchKeywords(text, listOf("twitter", "x", "kholo", "open")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "twitter"))
            
            matchKeywords(text, listOf("telegram", "kholo", "open telegram")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "telegram"))
            
            matchKeywords(text, listOf("settings", "kholo", "open settings")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "settings"))
            
            matchKeywords(text, listOf("calculator", "kholo", "open calculator")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "calculator"))
            
            matchKeywords(text, listOf("camera", "kholo", "open camera")) -> 
                AppCommand(AppCommand.OPEN_APP, mapOf("app_name" to "camera"))
            
            // CLOSE APP commands
            matchKeywords(text, listOf("band karo", "close", "band")) -> 
                AppCommand(AppCommand.CLOSE_APP)
            
            // PRIME CALL commands
            matchKeywords(text, listOf("close friend", "mere close friend", "best friend")) && 
            matchKeywords(text, listOf("call karo", "call", "phone")) -> 
                AppCommand(AppCommand.PRIME_CALL, mapOf("index" to "0"))
            
            matchKeywords(text, listOf("second friend", "second contact")) && 
            matchKeywords(text, listOf("call karo", "call")) -> 
                AppCommand(AppCommand.PRIME_CALL, mapOf("index" to "1"))
            
            // PRIME MSG commands
            matchKeywords(text, listOf("meri jaan", "my love", "love")) && 
            matchKeywords(text, listOf("message karo", "msg karo", "whatsapp karo")) -> 
                AppCommand(AppCommand.PRIME_MSG, mapOf("index" to "0"))
            
            // CALL commands
            matchKeywords(text, listOf("call karo", "call", "phone")) -> {
                val name = extractName(text)
                if (name != null) {
                    AppCommand(AppCommand.CALL, mapOf("name" to name))
                } else null
            }
            
            // SMS commands
            matchKeywords(text, listOf("sms", "message", "msg")) && 
            matchKeywords(text, listOf("bhejo", "karo", "send")) -> {
                val name = extractName(text)
                if (name != null) {
                    AppCommand(AppCommand.SMS, mapOf("name" to name))
                } else null
            }
            
            // WHATSAPP commands
            matchKeywords(text, listOf("whatsapp", "wa")) && 
            matchKeywords(text, listOf("call", "call karo")) -> {
                val name = extractName(text)
                if (name != null) {
                    AppCommand(AppCommand.WHATSAPP_CALL, mapOf("name" to name))
                } else null
            }
            
            matchKeywords(text, listOf("whatsapp", "wa")) && 
            matchKeywords(text, listOf("message", "msg", "bhejo")) -> {
                val name = extractName(text)
                if (name != null) {
                    AppCommand(AppCommand.WHATSAPP_MSG, mapOf("name" to name))
                } else null
            }
            
            // VOLUME commands
            matchKeywords(text, listOf("volume badhao", "volume up", "louder")) -> 
                AppCommand(AppCommand.VOLUME_UP)
            
            matchKeywords(text, listOf("volume kam karo", "volume down", "softer")) -> 
                AppCommand(AppCommand.VOLUME_DOWN)
            
            // FLASHLIGHT commands
            matchKeywords(text, listOf("torch on", "flashlight on", "light on")) -> 
                AppCommand(AppCommand.FLASHLIGHT_ON)
            
            matchKeywords(text, listOf("torch off", "flashlight off", "light off")) -> 
                AppCommand(AppCommand.FLASHLIGHT_OFF)
            
            // WIFI commands
            matchKeywords(text, listOf("wifi on", "wifi kholo", "turn on wifi")) -> 
                AppCommand(AppCommand.WIFI_ON)
            
            matchKeywords(text, listOf("wifi off", "wifi band karo", "turn off wifi")) -> 
                AppCommand(AppCommand.WIFI_OFF)
            
            // BLUETOOTH commands
            matchKeywords(text, listOf("bluetooth on", "bluetooth kholo")) -> 
                AppCommand(AppCommand.BLUETOOTH_ON)
            
            matchKeywords(text, listOf("bluetooth off", "bluetooth band karo")) -> 
                AppCommand(AppCommand.BLUETOOTH_OFF)
            
            else -> null
        }.also { command ->
            if (command != null) {
                Log.d(TAG, "Parsed command: ${command.type} with params: ${command.params}")
            }
        }
    }

    private fun matchKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword ->
            text.contains(keyword.lowercase())
        }
    }

    private fun extractName(text: String): String? {
        // Remove common command keywords
        var cleanText = text
            .replace("call karo", "")
            .replace("call", "")
            .replace("ko", "")
            .replace("sms", "")
            .replace("message", "")
            .replace("msg", "")
            .replace("bhejo", "")
            .replace("whatsapp", "")
            .replace("wa", "")
            .trim()

        return if (cleanText.isNotEmpty() && cleanText.length > 2) cleanText else null
    }
}
