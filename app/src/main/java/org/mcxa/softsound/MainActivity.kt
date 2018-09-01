package org.mcxa.softsound

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar


class MainActivity : AppCompatActivity() {
    // handle binding to the player service
    private var playerService: PlayerService? = null

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = (service as PlayerService.PlayerBinder).getService()
            // update the FAB
            if (playerService?.isPlaying() == true) fab.show() else fab.hide()
            playerService?.playerChangeListener = playerChangeListener
        }

    }

    private val playerChangeListener = {
        if (playerService?.isPlaying() == true) fab.show() else fab.hide()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        play_rain.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.RAIN)
            toggleProgressBar(rain_volume)
        }
        play_water.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.WATER)
            toggleProgressBar(water_volume)
        }
        play_storm.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.THUNDER)
            toggleProgressBar(storm_volume)
        }
        play_fire.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.FIRE)
            toggleProgressBar(fire_volume)
        }
        play_wind.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.WIND)
            toggleProgressBar(wind_volume)
        }
        play_night.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.NIGHT)
            toggleProgressBar(night_volume)
        }
        play_cat.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.PURR)
            toggleProgressBar(cat_volume)
        }

        rain_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.RAIN))
        water_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.WATER))
        storm_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.THUNDER))
        fire_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.FIRE))
        wind_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.WIND))
        night_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.NIGHT))
        cat_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.PURR))

        fab.setOnClickListener {
            playerService?.stopPlaying()
            fab.hide()
            // hide all volume bars
            arrayOf(rain_volume,water_volume,storm_volume,fire_volume,wind_volume,night_volume,cat_volume).forEach {
                bar -> bar.visibility = View.INVISIBLE
            }
        }
    }

    private fun toggleProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = if (progressBar.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    inner class VolumeChangeListener(val sound: PlayerService.Sound): SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            playerService?.setVolume(sound, (progress + 1) / 20f)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    override fun onStart() {
        super.onStart()
        val playerIntent = Intent(this, PlayerService::class.java)
        startService(playerIntent)
        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        playerService?.stopForeground()
    }

    override fun onPause() {
        playerService?.startForeground()
        super.onPause()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel("softsound", name, importance)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
