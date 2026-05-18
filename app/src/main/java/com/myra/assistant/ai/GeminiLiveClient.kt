package com.myra.assistant.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val context: Context,
    private val apiKey: String,
    private val modelId: String = "models/gemini-2.5-flash-native-audio-preview-12-2025",
    private val voiceName: String = "Aoede",
    private val systemPrompt: String = "",
    private val temperature: Float = 0.9f
) {

    private val TAG = "GeminiLiveClient"
    private val WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
    
    private val SESSION_RENEW_AFTER = 540000L // 9 minutes in ms
    private val KEEPALIVE_INTERVAL = 8000L // 8 seconds
    private val AUTO_RECONNECT_DELAY = 3000L // 3 seconds
    
    private var webSocket: WebSocket? = null
    private var httpClient: OkHttpClient? = null
    private var sessionStartTime = 0L
    private var lastKeepaliveTime = 0L
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isConnected = false
    private var autoReconnect = true

    // Callbacks
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onAudioReceived: ((ByteArray) -> Unit)? = null
    var onInputTranscript: ((String) -> Unit)? = null
    var onOutputTranscript: ((String) -> Unit)? = null
    var onTurnComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSetupComplete: (() -> Unit)? = null

    fun connect() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (httpClient == null) {
                        httpClient = OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(0, TimeUnit.SECONDS)
                            .build()
                    }

                    val request = Request.Builder()
                        .url(WS_URL)
                        .build()

                    val listener = GeminiWebSocketListener()
                    webSocket = httpClient!!.newWebSocket(request, listener)
                    sessionStartTime = System.currentTimeMillis()
                    lastKeepaliveTime = sessionStartTime

                    Log.d(TAG, "WebSocket connecting to $WS_URL")
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting: ${e.message}")
                    onError?.invoke("Connection failed: ${e.message}")
                }
            }
        }
    }

    fun disconnect() {
        autoReconnect = false
        scope.launch {
            withContext(Dispatchers.IO) {
                webSocket?.close(1000, "User disconnect")
                webSocket = null
                httpClient?.dispatcher?.executorService?.shutdown()
                httpClient = null
            }
        }
    }

    fun sendAudio(pcmBytes: ByteArray) {
        if (!isConnected) {
            Log.w(TAG, "WebSocket not connected, skipping audio")
            return
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val base64Audio = android.util.Base64.encodeToString(pcmBytes, android.util.Base64.NO_WRAP)
                    val message = JSONObject().apply {
                        put("realtime_input", JSONObject().apply {
                            put("media_chunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mime_type", "audio/pcm;rate=16000")
                                    put("data", base64Audio)
                                })
                            })
                        })
                    }
                    webSocket?.send(message.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending audio: ${e.message}")
                }
            }
        }
    }

    fun sendText(text: String) {
        if (!isConnected) {
            Log.w(TAG, "WebSocket not connected, queuing text: $text")
            return
        }

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val message = JSONObject().apply {
                        put("client_content", JSONObject().apply {
                            put("turns", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("role", "user")
                                    put("parts", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("text", text)
                                        })
                                    })
                                })
                            })
                            put("turn_complete", true)
                        })
                    }
                    webSocket?.send(message.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending text: ${e.message}")
                }
            }
        }
    }

    fun interrupt() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val message = JSONObject().apply {
                        put("client_content", JSONObject().apply {
                            put("turns", JSONArray())
                            put("turn_complete", true)
                        })
                    }
                    webSocket?.send(message.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending interrupt: ${e.message}")
                }
            }
        }
    }

    private fun sendSetupMessage() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val setupMessage = JSONObject().apply {
                        put("setup", JSONObject().apply {
                            put("model", modelId)
                            put("system_instruction", JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", systemPrompt)
                                    })
                                })
                            })
                            put("generation_config", JSONObject().apply {
                                put("response_modalities", JSONArray().apply {
                                    put("AUDIO")
                                })
                                put("speech_config", JSONObject().apply {
                                    put("voice_config", JSONObject().apply {
                                        put("prebuilt_voice_config", JSONObject().apply {
                                            put("voice_name", voiceName)
                                        })
                                    })
                                })
                                put("temperature", temperature)
                            })
                            put("output_audio_transcription", JSONObject())
                            put("input_audio_transcription", JSONObject())
                        })
                    }
                    webSocket?.send(setupMessage.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending setup: ${e.message}")
                }
            }
        }
    }

    private fun checkSessionRenewal() {
        val now = System.currentTimeMillis()
        if (now - sessionStartTime > SESSION_RENEW_AFTER) {
            Log.d(TAG, "Session renewal needed")
            disconnect()
            autoReconnect = true
            scope.launch {
                delay(AUTO_RECONNECT_DELAY)
                connect()
            }
        }
    }

    private fun sendKeepalive() {
        if (!isConnected) return

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Send silent PCM chunk (1024 bytes of zeros)
                    val silentChunk = ByteArray(1024)
                    val base64Silent = android.util.Base64.encodeToString(silentChunk, android.util.Base64.NO_WRAP)
                    val message = JSONObject().apply {
                        put("realtime_input", JSONObject().apply {
                            put("media_chunks", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("mime_type", "audio/pcm;rate=16000")
                                    put("data", base64Silent)
                                })
                            })
                        })
                    }
                    webSocket?.send(message.toString())
                    lastKeepaliveTime = System.currentTimeMillis()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending keepalive: ${e.message}")
                }
            }
        }
    }

    private inner class GeminiWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened")
            isConnected = true
            sendSetupMessage()
            scope.launch {
                while (isConnected) {
                    delay(KEEPALIVE_INTERVAL)
                    sendKeepalive()
                    checkSessionRenewal()
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: ${text.take(200)}...")
            scope.launch {
                parseServerMessage(text)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received binary data: ${bytes.size} bytes")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code $reason")
            isConnected = false
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            isConnected = false
            scope.launch(Dispatchers.Main) {
                onDisconnected?.invoke()
            }

            if (autoReconnect) {
                scope.launch {
                    delay(AUTO_RECONNECT_DELAY)
                    if (autoReconnect) {
                        connect()
                    }
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            isConnected = false
            scope.launch(Dispatchers.Main) {
                onError?.invoke("WebSocket error: ${t.message}")
            }

            if (autoReconnect) {
                scope.launch {
                    delay(AUTO_RECONNECT_DELAY)
                    if (autoReconnect) {
                        connect()
                    }
                }
            }
        }
    }

    private suspend fun parseServerMessage(jsonString: String) {
        withContext(Dispatchers.Default) {
            try {
                val json = JSONObject(jsonString)

                // Check for setupComplete
                if (json.has("setupComplete")) {
                    Log.d(TAG, "Setup complete")
                    scope.launch(Dispatchers.Main) {
                        onSetupComplete?.invoke()
                    }
                }

                // Parse serverContent
                if (json.has("serverContent")) {
                    val serverContent = json.getJSONObject("serverContent")

                    // Audio output (24kHz PCM)
                    if (serverContent.has("modelTurn")) {
                        val modelTurn = serverContent.getJSONObject("modelTurn")
                        if (modelTurn.has("parts")) {
                            val parts = modelTurn.getJSONArray("parts")
                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)
                                if (part.has("inlineData")) {
                                    val inlineData = part.getJSONObject("inlineData")
                                    val base64Data = inlineData.getString("data")
                                    val audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
                                    scope.launch(Dispatchers.Main) {
                                        onAudioReceived?.invoke(audioBytes)
                                    }
                                }
                            }
                        }
                    }

                    // Input transcription (what user said)
                    if (serverContent.has("inputTranscription")) {
                        val inputTrans = serverContent.getJSONObject("inputTranscription")
                        if (inputTrans.has("text")) {
                            val text = inputTrans.getString("text")
                            if (text.isNotEmpty()) {
                                scope.launch(Dispatchers.Main) {
                                    onInputTranscript?.invoke(text)
                                }
                            }
                        }
                    }

                    // Output transcription (what MYRA said)
                    if (serverContent.has("outputTranscription")) {
                        val outputTrans = serverContent.getJSONObject("outputTranscription")
                        if (outputTrans.has("text")) {
                            val text = outputTrans.getString("text")
                            if (text.isNotEmpty()) {
                                scope.launch(Dispatchers.Main) {
                                    onOutputTranscript?.invoke(text)
                                }
                            }
                        }
                    }

                    // Turn complete
                    if (serverContent.has("turnComplete") && serverContent.getBoolean("turnComplete")) {
                        scope.launch(Dispatchers.Main) {
                            onTurnComplete?.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }
    }

    fun release() {
        scope.cancel()
        disconnect()
    }
}
