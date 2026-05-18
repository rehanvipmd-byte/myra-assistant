package com.myra.assistant.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

class AudioEngine(private val context: Context) {

    private val TAG = "AudioEngine"

    // Constants
    private val MIC_SAMPLE_RATE = 16000 // 16kHz
    private val SPEAKER_SAMPLE_RATE = 24000 // 24kHz
    private val CHUNK_SIZE_BYTES = 1024
    private val CHUNK_SIZE_SAMPLES_MIC = CHUNK_SIZE_BYTES / 2 // 16-bit = 2 bytes per sample
    private val CHUNK_SIZE_SAMPLES_SPEAKER = (CHUNK_SIZE_BYTES * SPEAKER_SAMPLE_RATE) / (MIC_SAMPLE_RATE * 2)

    // Audio components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlayingBackup = false
    private var isMuted = false
    private var isSpeaking = false

    // Audio queue for playback
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Callbacks
    var onAmplitudeChanged: ((Float) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingStopped: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun startRecording() {
        scope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val bufferSize = AudioRecord.getMinBufferSize(
                        MIC_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )

                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        MIC_SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )

                    if (audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord!!.startRecording()
                        isRecording = true
                        Log.d(TAG, "Recording started at $MIC_SAMPLE_RATE Hz")

                        recordAudioLoop()
                    } else {
                        Log.e(TAG, "Failed to initialize AudioRecord")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting recording: ${e.message}")
                    onError?.invoke("Mic error: ${e.message}")
                }
            }
        }
    }

    fun startPlayback() {
        scope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(
                        SPEAKER_SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()

                    audioTrack = AudioTrack(
                        audioAttributes,
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SPEAKER_SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                        bufferSize,
                        AudioTrack.MODE_STREAM,
                        AudioManager.AUDIO_SESSION_ID_GENERATE
                    )

                    audioTrack!!.play()
                    Log.d(TAG, "Playback started at $SPEAKER_SAMPLE_RATE Hz")

                    playbackLoop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting playback: ${e.message}")
                    onError?.invoke("Speaker error: ${e.message}")
                }
            }
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        Log.d(TAG, "Audio muted: $muted")
    }

    fun queueAudio(audioBytes: ByteArray) {
        audioQueue.offer(audioBytes)
        if (!isSpeaking) {
            isSpeaking = true
            onSpeakingStarted?.invoke()
        }
    }

    fun clearQueue() {
        audioQueue.clear()
    }

    fun interrupt() {
        clearQueue()
        if (isSpeaking) {
            isSpeaking = false
            onSpeakingStopped?.invoke()
        }
    }

    fun release() {
        scope.cancel()
        stopRecording()
        stopPlayback()
    }

    private fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
        }
    }

    private fun stopPlayback() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            Log.d(TAG, "Playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    private suspend fun recordAudioLoop() {
        val buffer = ShortArray(CHUNK_SIZE_SAMPLES_MIC)

        while (isRecording) {
            try {
                val readSize = audioRecord!!.read(buffer, 0, CHUNK_SIZE_SAMPLES_MIC)
                if (readSize > 0 && !isMuted && !isSpeaking) {
                    // Convert to ByteArray
                    val byteArray = ByteArray(readSize * 2)
                    for (i in 0 until readSize) {
                        byteArray[i * 2] = (buffer[i] and 0xFF).toByte()
                        byteArray[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                    }

                    // Calculate RMS for amplitude
                    val rms = calculateRMS(buffer, readSize)
                    onAmplitudeChanged?.invoke(rms)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in record loop: ${e.message}")
                break
            }
        }
    }

    private suspend fun playbackLoop() {
        val outputBuffer = ByteArray(CHUNK_SIZE_BYTES * 2)

        while (audioTrack != null) {
            try {
                if (audioQueue.isEmpty()) {
                    if (isSpeaking) {
                        isSpeaking = false
                        onSpeakingStopped?.invoke()
                    }
                    delay(100)
                } else {
                    val chunk = audioQueue.poll() ?: continue
                    if (chunk.isNotEmpty()) {
                        val bytesWritten = audioTrack!!.write(chunk, 0, chunk.size)
                        if (bytesWritten < 0) {
                            Log.e(TAG, "Error writing to AudioTrack: $bytesWritten")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in playback loop: ${e.message}")
                break
            }
        }
    }

    private fun calculateRMS(audioData: ShortArray, size: Int): Float {
        if (size == 0) return 0f

        var sum = 0.0
        for (i in 0 until size) {
            val sample = audioData[i].toDouble() / 32768.0
            sum += sample * sample
        }

        val rms = sqrt(sum / size).toFloat()
        // Normalize to 0..1 range (max short value is 32767)
        return (rms * 2).coerceIn(0f, 1f)
    }
}
