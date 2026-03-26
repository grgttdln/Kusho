package com.example.app.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes

object BackgroundMusicPlayer {

    private const val TAG = "BackgroundMusicPlayer"
    private const val VOLUME = 0.05f // Subtle background volume

    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context, @RawRes resId: Int) {
        stop() // release any previous instance
        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId).apply {
                isLooping = true
                setVolume(VOLUME, VOLUME)
                start()
            }
            Log.d(TAG, "Background music started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background music", e)
            mediaPlayer = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
                Log.d(TAG, "Background music stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
        }
        mediaPlayer = null
    }
}
