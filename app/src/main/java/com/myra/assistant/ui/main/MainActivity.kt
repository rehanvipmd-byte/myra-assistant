package com.myra.assistant.ui.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.ai.AudioEngine
import com.myra.assistant.ai.CommandParser
import com.myra.assistant.ai.GeminiLiveClient
import com.myra.assistant.model.AppCommand
import com.myra.assistant.service.CallMonitorService
import com.myra.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var viewModel: MainViewModel
    private lateinit var geminiLive: GeminiLiveClient
    private lateinit var audioEngine: AudioEngine
    private lateinit var commandParser: CommandParser

    private lateinit var orbView: OrbAnimationView
    private lateinit var waveformView: WaveformView
    private lateinit var chatRecycler: RecyclerView
    private lateinit var micButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var batteryText: TextView
    private lateinit var timeText: TextView
    private lateinit var redOverlay: View

    private var chatAdapter: ChatAdapter? = null
    private var isMuted = false
    private var isInCallMode = false
    private var inputBuffer = StringBuilder()
    private var outputBuffer = StringBuilder()

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
            initGeminiLive()
        } else {
            Log.e(TAG, "Some permissions denied")
        }
    }

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.myra.CALL_ENDED") {
                isInCallMode = false
                orbView.setState(OrbAnimationView.OrbState.IDLE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        viewModel = ViewModelProvider(this, MainViewModel.Factory(this)).get(MainViewModel::class.java)
        commandParser = CommandParser()
        audioEngine = AudioEngine(this)

        checkPermissions()
        startSystemServices()
        startStatusUpdates()
        registerReceiver(callEndedReceiver, IntentFilter("com.myra.CALL_ENDED"))

        // Delay initialization to ensure everything is ready
        window.decorView.post {
            postDelayed({
                initGeminiLive()
            }, 300)
        }

        handleIncomingCallIntent(intent)
    }

    private fun initViews() {
        orbView = findViewById(R.id.orbView)
        waveformView = findViewById(R.id.waveformView)
        chatRecycler = findViewById(R.id.chatRecycler)
        micButton = findViewById(R.id.micButton)
        settingsButton = findViewById(R.id.settingsButton)
        statusText = findViewById(R.id.statusText)
        batteryText = findViewById(R.id.batteryText)
        timeText = findViewById(R.id.timeText)
        redOverlay = findViewById(R.id.redOverlay)

        chatAdapter = ChatAdapter()
        chatRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false).apply {
            stackFromEnd = true
        }
        chatRecycler.adapter = chatAdapter

        micButton.setOnClickListener {
            isMuted = !isMuted
            audioEngine.setMuted(isMuted)
            micButton.setImageResource(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
        }

        micButton.setOnLongClickListener {
            geminiLive.interrupt()
            audioEngine.interrupt()
            orbView.setState(OrbAnimationView.OrbState.IDLE)
            true
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed)
        }
    }

    private fun startSystemServices() {
        ContextCompat.startForegroundService(this, Intent(this, CallMonitorService::class.java))
    }

    private fun startStatusUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                updateStatus()
                kotlinx.coroutines.delay(30000)
            }
        }
    }

    private fun updateStatus() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxMemory = runtime.maxMemory() / 1048576L
        val battery = getBatteryPercentage()
        val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        batteryText.text = "$battery%"
        timeText.text = time
    }

    private fun getBatteryPercentage(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryProperty.CHARGE_COUNTER) / 1000
    }

    private fun initGeminiLive() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val userName = prefs.getString("user_name", "Friend") ?: "Friend"
        val model = prefs.getString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025") ?: "models/gemini-2.5-flash-native-audio-preview-12-2025"
        val voice = prefs.getString("gemini_voice", "Aoede") ?: "Aoede"

        if (apiKey.isEmpty()) {
            statusText.text = "⚠️ API Key missing. Go to Settings"
            return
        }

        val systemPrompt = buildSystemPrompt(userName)
        geminiLive = GeminiLiveClient(this, apiKey, model, voice, systemPrompt)

        geminiLive.onConnected = {
            Log.d(TAG, "WebSocket connected")
            audioEngine.startRecording()
            audioEngine.startPlayback()
            statusText.text = "Sun rahi hoon... 💬"
            orbView.setState(OrbAnimationView.OrbState.LISTENING)
            postDelayed({ sendGreeting(userName) }, 600)
        }

        geminiLive.onSetupComplete = {
            Log.d(TAG, "Setup complete")
        }

        geminiLive.onAudioReceived = { audioBytes ->
            audioEngine.queueAudio(audioBytes)
        }

        geminiLive.onInputTranscript = { text ->
            inputBuffer.append(text).append(" ")
        }

        geminiLive.onOutputTranscript = { text ->
            outputBuffer.append(text).append(" ")
        }

        geminiLive.onTurnComplete = {
            val inputText = inputBuffer.toString().trim()
            val outputText = outputBuffer.toString().trim()

            if (inputText.isNotEmpty()) {
                chatAdapter?.addMessage(ChatMessage(inputText, true, System.currentTimeMillis()))
                val command = commandParser.parse(inputText)
                if (command != null) {
                    viewModel.executeCommand(command)
                }
            }
            if (outputText.isNotEmpty()) {
                chatAdapter?.addMessage(ChatMessage(outputText, false, System.currentTimeMillis()))
            }

            inputBuffer.clear()
            outputBuffer.clear()
        }

        geminiLive.onDisconnected = {
            statusText.text = "Disconnected"
            orbView.setState(OrbAnimationView.OrbState.IDLE)
        }

        geminiLive.onError = { error ->
            Log.e(TAG, "Gemini error: $error")
            statusText.text = "❌ Error: $error"
        }

        audioEngine.onAmplitudeChanged = { rms ->
            waveformView.setAmplitude(rms)
        }

        audioEngine.onSpeakingStarted = {
            orbView.setState(OrbAnimationView.OrbState.SPEAKING)
            statusText.text = "Bol rahi hoon... 💖"
            fadeRedOverlay(true)
        }

        audioEngine.onSpeakingStopped = {
            orbView.setState(OrbAnimationView.OrbState.LISTENING)
            statusText.text = "Sun rahi hoon... 💬"
            fadeRedOverlay(false)
        }

        geminiLive.connect()
    }

    private fun buildSystemPrompt(userName: String): String {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val personality = prefs.getString("personality_mode", "gf") ?: "gf"
        val date = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        return when (personality) {
            "professional" -> "You are MYRA, a professional AI assistant. Be formal, precise, and efficient. Today is $date. User: $userName. Keep responses under 2 sentences. Speak clearly and professionally."
            "assistant" -> "You are MYRA, a helpful AI assistant. Be friendly and balanced. Today is $date. User: $userName. Keep responses natural and conversational (2-3 sentences max). You're speaking aloud, so sound natural."
            else -> "You are MYRA, an AI girlfriend. Be warm, caring, and emotionally expressive. Use Hinglish (Hindi + English mix naturally). Today is $date. User: $userName. Keep responses short (2-3 sentences max) and sound natural when speaking. Use words like 'tumhara', 'haan', 'acha', 'bilkul'. Examples: 'Haan $userName! Abhi kar deti hoon 😊' or 'Arre tumne yaad kiya! Bolo kya chahiye'"
        }
    }

    private fun sendGreeting(userName: String) {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val personality = prefs.getString("personality_mode", "gf") ?: "gf"

        val greeting = when (personality) {
            "professional" -> "Good day $userName. MYRA is online and ready to assist you."
            "assistant" -> "Hello $userName! Main MYRA hoon. Kaise help karun aapki?"
            else -> "Hey $userName! Main aa gayi hoon. Kya help chahiye tumhe? 💖"
        }

        geminiLive.sendText(greeting)
    }

    private fun fadeRedOverlay(show: Boolean) {
        redOverlay.animate()
            .alpha(if (show) 0.08f else 0f)
            .setDuration(if (show) 300 else 500)
            .start()
    }

    private fun handleIncomingCallIntent(intent: Intent) {
        if (intent.getBooleanExtra("INCOMING_CALL", false)) {
            val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"
            announceCall(callerName)
        }
    }

    private fun announceCall(callerName: String) {
        isInCallMode = true
        orbView.setState(OrbAnimationView.OrbState.THINKING)
        geminiLive.sendText("Sir, $callerName ka call aa raha hai. Uthau ya reject karu?")
    }

    override fun onResume() {
        super.onResume()
        audioEngine.setMuted(false)
        waveformView.startAnimation()
    }

    override fun onPause() {
        super.onPause()
        audioEngine.setMuted(true)
        waveformView.stopAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callEndedReceiver)
        geminiLive.release()
        audioEngine.release()
    }

    companion object {
        const val REQUEST_CODE_SETTINGS = 100
    }
}
