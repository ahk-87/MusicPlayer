package org.hyperskill.musicplayer

import android.content.ContentUris
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.TextView

class SongPlayer(val activity: MainActivity) {
    var player: MediaPlayer? = null
    val handler = Handler(Looper.getMainLooper())
    var currentTrack = SongInfo(0, "", "", 0)

    var seekBar: SeekBar? = null
    var durationTv: TextView? = null
    var totalTv: TextView? = null

    companion object {
        fun formatDuration(secs: Int) = "%02d:%02d".format(secs / 60, secs % 60)
    }

    fun updateCurrentTrack(playlist: Playlist) {
        if (currentTrack !in playlist.songs) {
            stop()
            currentTrack = playlist.songs[0]
        }
    }

    fun playOrPause() {
        player?.let {
            if (currentTrack.state == MediaState.PLAYING) {
                currentTrack.state = MediaState.PAUSED
                it.pause()
            } else {
                currentTrack.state = MediaState.PLAYING
                it.start()
            }
            activity.adapter.notify(currentTrack)
        }
    }

    fun stop() {
        player?.let {
            if (currentTrack.state != MediaState.STOPPED) {
                currentTrack.state = MediaState.STOPPED
                it.seekTo(0)
                it.pause()
                activity.adapter.notify(currentTrack)
            }
        }
    }

    fun imageButtonHandler(track: SongInfo) {
        if (currentTrack !== track) {
            stop()
            createMediaPlayer(track)
        }
        playOrPause()
    }

    fun createMediaPlayer(song: SongInfo) {
        currentTrack = song

        val uri = ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id
        )

        player?.release()
        player = MediaPlayer.create(activity, uri)
        player!!.setOnCompletionListener { stop() }

        seekBar?.let { it.progress = 0; it.max = song.duration / 1000 }
        totalTv?.text = formatDuration(song.duration / 1000)
    }

    fun registerControllers(skBar: SeekBar, durTv: TextView, totTv: TextView) {
        seekBar = skBar
        durationTv = durTv
        totalTv = totTv

        seekBar!!.setOnSeekBarChangeListener(seekBarChangeListener)

        player?.let {
            seekBar!!.max = it.duration / 1000
            totalTv!!.text = formatDuration(it.duration / 1000)
        }

        handler.post(updateControllersRun)
    }

    fun unRegisterControllers() {
        handler.removeCallbacks(updateControllersRun)
        seekBar = null
        durationTv = null
        totalTv = null
    }

    val updateControllersRun = object : Runnable {
        override fun run() {
            if (seekBar != null) {
                seekBar!!.progress = player?.currentPosition?.div(1000) ?: 0
                durationTv!!.text = formatDuration(seekBar!!.progress)
            }

            handler.postDelayed(this, 200)
        }
    }

    val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            durationTv!!.text = formatDuration(seekBar!!.progress)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
            handler.removeCallbacks(updateControllersRun)
        }

        override fun onStopTrackingTouch(p0: SeekBar?) {
            player?.seekTo(seekBar?.progress?.times(1000) ?: 0)
            handler.post(updateControllersRun)
        }
    }
}