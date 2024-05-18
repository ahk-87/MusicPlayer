package org.hyperskill.musicplayer

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import org.hyperskill.musicplayer.databinding.ActivityMainBinding

const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
const val GRANTED = PackageManager.PERMISSION_GRANTED
const val READ_SONGS_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var adapter: SongsAdapter
    lateinit var playlistDB: PlaylistStore

    var state = ActionState.PLAY_MUSIC
    val mediaPlayer = SongPlayer(this)

    private val allSongsPlaylist = Playlist("All Songs", listOf<SongInfo>())
    private var currentPlaylist = Playlist("", listOf<SongInfo>())
    private val playlists = mutableListOf<Playlist>(allSongsPlaylist)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SongsAdapter(currentPlaylist.songs, this)
        binding.mainSongList.adapter = adapter

        binding.mainButtonSearch.setOnClickListener { checkPermission() }

        playlistDB = PlaylistStore(this)
        playlistDB.savedPlaylists.keys.forEach {
            playlists.add(Playlist(it, listOf<SongInfo>()))
        }
    }

    /**********************************/
    /* This is the permission section */
    /**********************************/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_SONGS_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == GRANTED
        ) {
            searchSongs()
        } else {
            Toast.makeText(this, R.string.songs_not_loaded, Toast.LENGTH_SHORT).show()
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, READ_PERMISSION) == GRANTED)
            searchSongs()
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_PERMISSION)) {
            AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage("Songs can't be displayed if you don't permit reading files")
                .setPositiveButton("Grant") { _, _ -> requestPermission() }
                .setNegativeButton("Cancel", null)
                .show()
        } else
            requestPermission()
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(READ_PERMISSION), READ_SONGS_REQUEST_CODE
        )
    }

    /***********************************************************************/
    /* This section is responsible for manipulating the various  playlists */
    /***********************************************************************/

    fun searchSongs() {
        if (fillAllSongsPlaylist())
            refreshMainSongListView(allSongsPlaylist)
    }

    fun fillAllSongsPlaylist(): Boolean {
        val songsUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
        )

        val query = applicationContext.contentResolver.query(
            songsUri, projection,
            null, null, null
        )

        val songs = allSongsPlaylist.songs.toMutableList()
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                if (songs.find { it.id == id } == null) {
                    val artist = cursor.getString(artistColumn)
                    val title = cursor.getString(titleColumn)
                    val duration = cursor.getInt(durationColumn)

                    songs.add(SongInfo(id, title, artist, duration))
                }
            }

            allSongsPlaylist.songs = songs
        }

        if (allSongsPlaylist.songs.isEmpty()) {
            Snackbar.make(binding.root, "No Songs were found", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun fillPlaylist(p: Playlist) {
        val tempSongs = mutableListOf<SongInfo>()
        playlistDB.savedPlaylists[p.name]!!.forEach { id ->
            tempSongs.add(allSongsPlaylist.songs.find { song -> song.id == id }!!)
        }
        p.songs = tempSongs
    }

    fun refreshMainSongListView(newList: Playlist) {
        if (newList.songs.isEmpty())
            fillPlaylist(newList)

        if (state == ActionState.PLAY_MUSIC) {
            currentPlaylist = newList
            mediaPlayer.updateCurrentTrack(currentPlaylist)
        }

        adapter.changeList(newList)
        binding.mainSongList.adapter = adapter

        if (mediaPlayer.player == null)
            mediaPlayer.createMediaPlayer(newList.songs[0])
    }

    /*******************************************************************************/
    /* This section is responsible for the entering and exiting ADD_PLAYLIST state */
    /*******************************************************************************/

    fun showAddPlaylist() {
        state = ActionState.ADD_PLAYLIST
        adapter.changeList(allSongsPlaylist)
        binding.mainSongList.adapter = adapter
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, MainAddPlaylistFragment())
            .commit()
    }

    fun addNewPlaylist(name: String) {
        if (adapter.currentList.none { it.isSelected }) {
            Toast.makeText(this, R.string.at_least_one_song, Toast.LENGTH_SHORT).show()
        } else if (name.isBlank()) {
            Toast.makeText(this, R.string.add_playlist_name, Toast.LENGTH_SHORT).show()
        } else if (name == "All Songs") {
            Toast.makeText(this, R.string.reserved_playlist, Toast.LENGTH_SHORT).show()
        } else {
            val p = Playlist(name, adapter.currentList.filter { it.isSelected })
            playlists.find { it.name == p.name }?.let {
                playlists.remove(it)
                playlistDB.delete(it)
            }
            playlists.add(p)
            playlistDB.insert(p)
            exitAddingPlaylist()
        }
    }

    fun exitAddingPlaylist() {
        state = ActionState.PLAY_MUSIC
        adapter.clearSelection()
        adapter.changeList(currentPlaylist)
        binding.mainSongList.adapter = adapter
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainFragmentContainer, MainPlayerControllerFragment())
            .commit()
    }

    /****************************************************/
    /*            2 added functions from me             */
    /****************************************************/

    /* scroll to the selected song in case of longClick */
    /* or to the current track if you exit ADD_PLAYLIST */
    fun scroll() {
        val ind = if (state == ActionState.PLAY_MUSIC)
            currentPlaylist.songs.indexOf(mediaPlayer.currentTrack)
        else
            currentPlaylist.songs.firstOrNull() { it.isSelected }
                ?.let { currentPlaylist.songs.indexOf(it) } ?: 0
        binding.mainSongList.scrollToPosition(ind)
    }

    /* if you press back key in ADD_PLAYLIST state, return to the PLAY_MUSIC state */
    override fun onBackPressed() {
        if (state == ActionState.ADD_PLAYLIST)
            exitAddingPlaylist()
        else
            super.onBackPressed()
    }

    /****************************************************/
    /* This section handle the creation of options menu */
    /****************************************************/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.music_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection.
        return when (item.itemId) {
            R.id.mainMenuAddPlaylist -> menuAddPlaylist()
            R.id.mainMenuLoadPlaylist -> menuLoadPlaylist()
            R.id.mainMenuDeletePlaylist -> menuDeletePlaylist()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun menuAddPlaylist(): Boolean {
        if (state == ActionState.PLAY_MUSIC)
            if (allSongsPlaylist.songs.isEmpty())
                Toast.makeText(this, R.string.no_songs, Toast.LENGTH_SHORT).show()
            else
                showAddPlaylist()
        return true
    }

    private fun menuLoadPlaylist(): Boolean {
        AlertDialog.Builder(this)
            .setTitle("choose playlist to load")
            .setItems(playlists.map { it.name }.toTypedArray()) { _, pos ->
                var songsAreEmpty = allSongsPlaylist.songs.isEmpty()
                if (songsAreEmpty)
                    songsAreEmpty = fillAllSongsPlaylist().not()

                if (!songsAreEmpty) {
                    if (playlists[pos].name != adapter.currentPlaylistName)
                        refreshMainSongListView(playlists[pos])
                }
            }
            .setNegativeButton("cancel", null)
            .show()
        return true
    }

    private fun menuDeletePlaylist(): Boolean {
        AlertDialog.Builder(this)
            .setTitle("choose playlist to delete")
            .setItems(playlists.drop(1).map { it.name }.toTypedArray()) { _, pos ->
                if (playlists[pos + 1].name == adapter.currentPlaylistName) {
                    currentPlaylist = allSongsPlaylist
                    adapter.changeList(currentPlaylist)
                    binding.mainSongList.adapter = adapter
                }
                playlistDB.delete(playlists[pos + 1])
                playlists.removeAt(pos + 1)
            }
            .setNegativeButton("cancel", null)
            .show()
        return true
    }
}