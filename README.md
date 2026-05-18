# 🎙️ MYRA - AI Voice Assistant

**MYRA** is a complete Android AI voice companion powered by Google Gemini 2.5 Flash with real-time audio streaming, intelligent command execution, and beautiful animations.

## ✨ Features

### 🤖 AI Powered
- **Gemini 2.5 Flash Native Audio** - Real-time bidirectional audio streaming
- **WebSocket Communication** - Low-latency conversation
- **Multi-Personality Modes**:
  - 👸 Girlfriend Mode (Hinglish + emotional)
  - 💼 Professional Mode (formal & efficient)
  - 🤝 Assistant Mode (friendly & helpful)

### 🎯 Voice Commands
Control your device entirely by voice:
- **App Control**: Open/close apps (YouTube, WhatsApp, Instagram, etc.)
- **Communications**: Call, SMS, WhatsApp messages
- **Device Control**: Volume, Flashlight, WiFi, Bluetooth
- **Prime Contacts**: Quick-dial your favorite contacts

### 🎨 Beautiful UI
- **Animated Orb** - Dynamic state-based animations (Idle, Listening, Speaking, Thinking)
- **Waveform Visualizer** - Real-time audio amplitude display
- **Chat History** - See conversation history
- **Dark Theme** - Easy on eyes with red/purple accents

### 🔧 Advanced Features
- **Accessibility Service** - Deep OS integration
- **CallMonitor Service** - Call handling
- **Foreground Service** - Persistent background operation
- **Contact Management** - Smart contact resolution
- **Session Management** - 9-minute session auto-renewal

## 📋 Requirements

- **Android 8.0+** (API 26)
- **Kotlin 1.9.0+**
- **Google Gemini API Key**
- **Microphone & Speaker Permissions**

## 🚀 Installation

### 1. Clone Repository
```bash
git clone https://github.com/rehanvipmd-byte/myra-assistant.git
cd myra-assistant
```

### 2. Setup Gemini API Key

1. Get your API key from [Google AI Studio](https://aistudio.google.com/apikey)
2. Open the app → Settings
3. Paste your API key
4. Choose your personality mode
5. Save

### 3. Build & Run

```bash
# Using Android Studio
1. Open project in Android Studio
2. Build > Make Project
3. Run > Run 'app'

# Using Gradle
./gradlew build
./gradlew installDebug
```

## 🏗️ Architecture

### Core Components

**GeminiLiveClient** (`ai/GeminiLiveClient.kt`)
- WebSocket connection to Gemini API
- Real-time audio streaming (16kHz input, 24kHz output)
- Setup messaging with system prompts
- Session management & keepalive

**AudioEngine** (`ai/AudioEngine.kt`)
- Microphone recording (16kHz PCM)
- Speaker playback (24kHz PCM)
- Audio queue management
- Amplitude calculation for waveform

**CommandParser** (`ai/CommandParser.kt`)
- NLP-based command extraction
- Multi-language support (English + Hinglish)
- 40+ command patterns

**OrbAnimationView** (`ui/main/OrbAnimationView.kt`)
- Custom Canvas-based animations
- 5 states: Idle, Listening, Speaking, Thinking, Active
- Gradient shaders, particles, waves, arcs

**MainActivity** (`ui/main/MainActivity.kt`)
- Main UI orchestration
- Lifecycle management
- Permission handling
- Chat history display

## 📁 Project Structure

```
app/src/main/
├── java/com/myra/assistant/
│   ├── ai/
│   │   ├── GeminiLiveClient.kt      # WebSocket & Gemini integration
│   │   ├── AudioEngine.kt           # Mic/Speaker handling
│   │   └── CommandParser.kt         # Voice command parsing
│   ├── model/
│   │   └── AppCommand.kt            # Command data model
│   ├── service/
│   │   ├── CallMonitorService.kt
│   │   ├── AccessibilityHelperService.kt
│   │   ├── MyraOverlayService.kt
│   │   ├── PowerButtonReceiver.kt
│   │   └── BootReceiver.kt
│   ├── ui/
│   │   ├── main/
│   │   │   ├── MainActivity.kt
│   │   │   ├── OrbAnimationView.kt
│   │   │   ├── WaveformView.kt
│   │   │   ├── ChatAdapter.kt
│   │   │   └── ChatMessage.kt
│   │   └── settings/
│   │       └── SettingsActivity.kt
│   └── viewmodel/
│       └── MainViewModel.kt         # Command execution logic
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── activity_settings.xml
│   │   └── item_chat_message.xml
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── xml/
│       └── accessibility_service_config.xml
└── AndroidManifest.xml
```

## 🔌 API Integration

### Gemini WebSocket Message Format

**Setup Message**
```json
{
  "setup": {
    "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
    "generation_config": {
      "response_modalities": ["AUDIO"],
      "speech_config": {
        "voice_config": {"prebuilt_voice_config": {"voice_name": "Aoede"}}
      }
    }
  }
}
```

**Audio Input**
```json
{
  "realtime_input": {
    "media_chunks": [
      {
        "mime_type": "audio/pcm;rate=16000",
        "data": "<base64_encoded_pcm>"
      }
    ]
  }
}
```

**Text Input**
```json
{
  "client_content": {
    "turns": [{"role": "user", "parts": [{"text": "Hello"}]}],
    "turn_complete": true
  }
}
```

## ⚙️ Configuration

### SharedPreferences (myra_prefs)
```kotlin
val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)

// API Configuration
prefs.putString("api_key", "your-gemini-api-key")
prefs.putString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025")
prefs.putString("gemini_voice", "Aoede") // Voice name

// User Configuration
prefs.putString("user_name", "Friend")
prefs.putString("personality_mode", "gf") // gf, professional, assistant
```

### Available Voices
- Aoede (Female)
- Charon (Male)
- Fenrir (Male)
- Kore (Female)
- Orpheus (Male)

## 🎮 Usage Examples

### Voice Commands

**Open Apps**
- "YouTube kholo"
- "WhatsApp open karo"
- "Settings kholo"

**Make Calls**
- "Mummy ko call karo"
- "Call Raj"

**Send Messages**
- "Priya ko message bhejo"
- "WhatsApp karo close friend ko"

**Device Control**
- "Volume badhao"
- "Torch on"
- "WiFi turn on"

## 🔐 Permissions

Required permissions:
- `INTERNET` - API communication
- `RECORD_AUDIO` - Microphone access
- `READ_CONTACTS` - Contact resolution
- `CALL_PHONE` - Making calls
- `SEND_SMS` - SMS sending
- `ANSWER_PHONE_CALLS` - Incoming call handling
- `SYSTEM_ALERT_WINDOW` - Overlay access
- `MODIFY_AUDIO_SETTINGS` - Volume control
- `CHANGE_WIFI_STATE` - WiFi control
- `BLUETOOTH_ADMIN` - Bluetooth control

## 🐛 Troubleshooting

### No Audio Input
- Check microphone permission
- Ensure microphone is not muted
- Check system audio settings

### API Connection Failed
- Verify API key is correct
- Check internet connection
- Ensure API key has necessary permissions

### Commands Not Executing
- Check app installation on device
- Verify contact names in contacts app
- Enable Accessibility Service in settings

## 📊 Performance

**Audio Specs**
- Mic: 16kHz, 16-bit PCM, Mono
- Speaker: 24kHz, 16-bit PCM, Mono
- Buffer: 1024 bytes chunks
- Latency: <200ms (WebSocket)

**Memory
- Base: ~80MB
- With playback: ~150MB
- Peak (recording): ~200MB

## 🚀 Future Enhancements

- [ ] Custom wake word detection
- [ ] Offline mode with local TTS
- [ ] Multi-user support
- [ ] Custom command macros
- [ ] Integration with smart home APIs
- [ ] Cloud sync for settings
- [ ] Advanced NLP for better command parsing

## 📄 License

MIT License - See LICENSE file

## 👨‍💻 Author

**Rehan Vip** (@rehanvipmd-byte)
- GitHub: https://github.com/rehanvipmd-byte
- Email: rehanvipmd@gmail.com

## 🙏 Acknowledgments

- Google Gemini API team
- Android framework contributors
- Material Design principles

---

**Made with ❤️ for AI enthusiasts and Android developers**
