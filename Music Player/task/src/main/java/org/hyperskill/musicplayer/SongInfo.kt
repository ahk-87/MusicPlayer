package org.hyperskill.musicplayer

enum class MediaState { PLAYING, PAUSED, STOPPED }
enum class ActionState { PLAY_MUSIC, ADD_PLAYLIST }

data class SongInfo(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Int
) {
//    companion object {
//        fun makeSongs() = (1..10).map {
//            SongInfo(it, "title$it", "artist$it", 215_000)
//        }
//    }

    var state = MediaState.STOPPED
    var isSelected = false
}

class Playlist(
    val name: String,
    var songs: List<SongInfo>
)