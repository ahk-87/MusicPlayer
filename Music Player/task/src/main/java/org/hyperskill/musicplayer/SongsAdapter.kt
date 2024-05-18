package org.hyperskill.musicplayer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class SongsAdapter(songs: List<SongInfo>, val activity: MainActivity) :
    ListAdapter<SongInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    var currentPlaylistName = ""

    init {
        submitList(songs)
    }

    fun changeList(newPlaylist: Playlist) {
        currentPlaylistName = newPlaylist.name
        if (activity.state == ActionState.ADD_PLAYLIST)
            currentList.forEach { it.isSelected = (it in newPlaylist.songs && it.isSelected) }

        submitList(newPlaylist.songs)
    }

    fun clearSelection() {
        currentList.forEach { it.isSelected = false }
    }

    fun notify(song: SongInfo) {
        notifyItemChanged(currentList.indexOf(song))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (activity.state == ActionState.PLAY_MUSIC) {
            val holder = PlayerSongViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_song, parent, false)
            )

            holder.playBtn.setOnClickListener {
                val selectedTrack = getItem(holder.adapterPosition)
                activity.mediaPlayer.imageButtonHandler(selectedTrack)
            }

            holder.itemView.setOnLongClickListener {
                getItem(holder.adapterPosition).isSelected = true
                activity.showAddPlaylist()
                true
            }

            return holder
        } else {
            val holder = SelectorSongViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_song_selector, parent, false)
            )

            holder.itemView.setOnClickListener {
                val checked = !holder.checkBox.isChecked
                holder.checkBox.isChecked = checked
                getItem(holder.adapterPosition).isSelected = checked

                holder.itemView.setBackgroundColor(if (checked) Color.LTGRAY else Color.WHITE)
            }

            return holder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val song = getItem(position)

        (holder as SongViewHolder).apply {
            artistTv.text = song.artist
            titleTv.text = song.title
            durationTv.text = SongPlayer.formatDuration(song.duration / 1000)
        }

        if (activity.state == ActionState.PLAY_MUSIC) {
            (holder as PlayerSongViewHolder).apply {
                playBtn.setImageResource(
                    if (song.state != MediaState.PLAYING)
                        R.drawable.ic_play else R.drawable.ic_pause
                )
            }
        } else {
            (holder as SelectorSongViewHolder).apply {
                checkBox.isChecked = song.isSelected
                itemView.setBackgroundColor(if (song.isSelected) Color.LTGRAY else Color.WHITE)
            }
        }
    }

    abstract class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val artistTv: TextView
        abstract val titleTv: TextView
        abstract val durationTv: TextView
    }

    class PlayerSongViewHolder(view: View) : SongViewHolder(view) {
        override val artistTv: TextView = view.findViewById(R.id.songItemTvArtist)
        override val titleTv: TextView = view.findViewById(R.id.songItemTvTitle)
        override val durationTv: TextView = view.findViewById(R.id.songItemTvDuration)
        val playBtn: ImageButton = view.findViewById(R.id.songItemImgBtnPlayPause)
    }

    class SelectorSongViewHolder(view: View) : SongViewHolder(view) {
        override val artistTv: TextView = view.findViewById(R.id.songSelectorItemTvArtist)
        override val titleTv: TextView = view.findViewById(R.id.songSelectorItemTvTitle)
        override val durationTv: TextView = view.findViewById(R.id.songSelectorItemTvDuration)
        val checkBox: CheckBox = view.findViewById(R.id.songSelectorItemCheckBox)
    }

    class DiffCallback : DiffUtil.ItemCallback<SongInfo>() {
        override fun areItemsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SongInfo, newItem: SongInfo): Boolean {
            return oldItem == newItem
                    && oldItem.isSelected == newItem.isSelected
                    && oldItem.state == newItem.state
        }
    }
}