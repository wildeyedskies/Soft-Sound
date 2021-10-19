package org.mcxa.softsound

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Timer
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {
    // handle binding to the player service
    private var playerService: PlayerService? = null

    private var timer: Timer? = null;
    // timer duration options
    private var timerTimesHumanReadable: Array<String> = arrayOf("15 minutes", "30 minutes", "1 hour", "2 hours", "4 hours", "6 hours")
    // and their corresponding durations in ms
    private var timerTimesMilliseconds: Array<Long> = arrayOf(15*60*1000, 30*60*1000, 60*60*1000, 120*60*1000, 240*60*1000, 360*60*1000)

    private val serviceConnection = object : ServiceConnection {
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
            this.updateTimerState()
        }
        play_storm.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.STORM)
            toggleProgressBar(storm_volume)
            this.updateTimerState()
        }
        play_water.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.WATER)
            toggleProgressBar(water_volume)
            this.updateTimerState()
        }
        play_fire.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.FIRE)
            toggleProgressBar(fire_volume)
            this.updateTimerState()
        }
        play_wind.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.WIND)
            toggleProgressBar(wind_volume)
            this.updateTimerState()
        }
        play_night.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.NIGHT)
            toggleProgressBar(night_volume)
            this.updateTimerState()
        }
        play_cat.setOnClickListener {
            playerService?.toggleSound(PlayerService.Sound.PURR)
            toggleProgressBar(cat_volume)
            this.updateTimerState()
        }

        rain_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.RAIN))
        storm_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.STORM))
        water_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.WATER))
        fire_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.FIRE))
        wind_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.WIND))
        night_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.NIGHT))
        cat_volume.setOnSeekBarChangeListener(VolumeChangeListener(PlayerService.Sound.PURR))

        fab.setOnClickListener {
            this.stopPlaying()
            this.cancelTimer()
        }

        start_timer.setOnClickListener {
            this.startTimerClickHandler()
        }

        cancel_timer.setOnClickListener {
            this.cancelTimer()
        }
    }

    private fun toggleProgressBar(progressBar: ProgressBar) {
        progressBar.visibility = if (progressBar.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    private fun stopPlaying() {
        playerService?.stopPlaying()
        fab.hide()
        // hide all volume bars
        arrayOf(rain_volume, storm_volume, water_volume, fire_volume, wind_volume, night_volume, cat_volume).forEach { bar ->
            bar.visibility = View.INVISIBLE
        }
    }

    private fun updateTimerState() {
        // update the timer state in response to the user enabling/disabling a specific soundtrack
        // if no sound is playing, cancel the timer and update the button state
        // if sound is playing, only update the button state
        if (playerService == null || !(playerService!!.isPlaying()))
            this.cancelTimer()
        this.updateTimerButtonState()
    }

    private fun updateTimerButtonState() {
        // if no sound is playing, both buttons should be invisible
        if (playerService == null || !(playerService!!.isPlaying())) {
            start_timer.visibility = View.INVISIBLE
            cancel_timer.visibility = View.INVISIBLE
        }
        // sound is playing.
        // if a timer exists, the cancel button should be visible
        // otherwise, the start button should be visible
        else {
            start_timer.visibility = if (this.timer == null) View.VISIBLE else View.INVISIBLE;
            cancel_timer.visibility = if (this.timer != null) View.VISIBLE else View.INVISIBLE;
        }
    }

    private fun startTimerClickHandler() {
        // pop up a dialog asking for amount of time, and if a choice is made start the timer
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setItems(timerTimesHumanReadable) { dialog, which -> startTimer(which) }
        builder.show()
    }

    private fun startTimer(which: Int) {
        val timeMs: Long = timerTimesMilliseconds[which]
        this.timer = Timer()
        this.timer!!.schedule(timeMs) {
            runOnUiThread {
                stopPlaying()
                cancelTimer()
            }
        }
        this.updateTimerButtonState()
    }

    private fun cancelTimer() {
        this.timer?.cancel()
        this.timer = null;
        playerService?.stopForeground()
        this.updateTimerButtonState();
    }

    inner class VolumeChangeListener(private val sound: PlayerService.Sound) : SeekBar.OnSeekBarChangeListener {
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
