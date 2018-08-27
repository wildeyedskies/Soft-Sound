package org.mcxa.softsound

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.IBinder
import android.util.Log
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import android.app.PendingIntent
import android.app.Service
import android.os.Binder
import android.support.v4.app.NotificationCompat

class PlayerService : Service() {
    val NOTIFICATION_ID = 132
    val TAG = "Player"

    inner class PlayerBinder: Binder() {
        fun getService(): PlayerService {
            return this@PlayerService
        }
    }

    val playerBinder = PlayerBinder()

    override fun onCreate() {
        // load each player into the map
        Sound.values().forEach {
            exoPlayers[it] = initializeExoPlayer(it.file)
        }
    }

    enum class Sound(val file: String) {
        RAIN("rain.ogg"),
        THUNDER("thunderstorm.ogg"),
        FIRE("fire.ogg"),
        WATER("water.ogg"),
        WIND("wind.ogg")
    }

    private val exoPlayers = mutableMapOf<Sound, SimpleExoPlayer>()

    private fun initializeExoPlayer(soundFile: String): SimpleExoPlayer {
        // create the player
        val exoPlayer = ExoPlayerFactory.newSimpleInstance(
                DefaultRenderersFactory(this), DefaultTrackSelector()
        )

        // load the media source
        val dataSource = DefaultDataSourceFactory(this,
                Util.getUserAgent(this, this.getString(R.string.app_name)))
        val mediaSource = ExtractorMediaSource.Factory(dataSource).createMediaSource(Uri.parse("asset:///${soundFile}"))

        // load the media
        Log.d("MAIN", "loading ${soundFile}")
        exoPlayer.prepare(mediaSource)
        // loop indefinitely
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL

        return exoPlayer
    }

    override fun onUnbind(intent: Intent?): Boolean {
        // don't continue if we're not playing any sound and the main activity exits
        if(!isPlaying()) {
            stopSelf()
            Log.d(TAG, "stopping service")
        }
        return super.onUnbind(intent)
    }

    override fun onBind(intent: Intent?): IBinder {
        // return the binding interface
        return playerBinder
    }

    fun startForeground() {
        // move to the foreground if we are playing sound
        if (SDK_INT >= 24 && isPlaying()) {
            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

            val notification = NotificationCompat.Builder(this, "softsound")
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(getText(R.string.notification_message))
                    .setSmallIcon(R.drawable.ic_volume)
                    .setContentIntent(pendingIntent)
                    .build()

            Log.d(TAG, "starting foreground service")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun stopForeground() {
        // we don't need to be foreground anymore
        if (SDK_INT >= 24) {
            Log.d(TAG, "stopping foreground service")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    fun isPlaying(): Boolean {
        var playing = false
        exoPlayers.values.forEach { if (it.playWhenReady) playing = true }
        return playing
    }

    fun setVolume(sound: Sound, volume: Float) {
        exoPlayers[sound]?.volume = volume
    }

    fun getVolume(sound: Sound): Float {
        return exoPlayers[sound]?.volume ?: 0f
    }

    fun toggleSound(sound: Sound) {
        exoPlayers[sound]?.playWhenReady = !(exoPlayers[sound]?.playWhenReady ?: false)
    }
}
