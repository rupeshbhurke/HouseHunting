package com.rupeshbhurke.househunter.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/** Records the microphone to an AAC .m4a file. The caller must hold the RECORD_AUDIO permission. */
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    /** Returns false if the recorder could not start (e.g. the microphone is in use). */
    fun start(file: File): Boolean {
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (_: Exception) {
            r.release()
            file.delete()
            false
        }
    }

    /** Stops and returns the length in ms, or null if nothing usable was recorded (stopped instantly). */
    fun stop(): Long? {
        val r = recorder ?: return null
        recorder = null
        return try {
            r.stop()
            SystemClock.elapsedRealtime() - startedAt
        } catch (_: RuntimeException) {
            null
        } finally {
            r.release()
        }
    }

    /** Stops and throws the recording away. */
    fun cancel(file: File) {
        stop()
        file.delete()
    }
}
