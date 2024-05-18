package org.hyperskill.musicplayer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(
    context: Context
) : SQLiteOpenHelper(context, "musicPlayerDatabase.db", null, 1) {

    companion object {
        const val TABLE_NAME = "playlist"
        const val PLAYLIST_NAME_COl = "playlistName"
        const val SONG_ID_COL = "songId"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = 1")
        db.execSQL("PRAGMA trusted_schema = 0")
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME(\n" +
                    "   $PLAYLIST_NAME_COl TEXT NOT NULL\n" +
                    "  , $SONG_ID_COL INTEGER NOT NULL\n" +
                    ")"
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase, oldVersion: Int, newVersion: Int
    ) {
        throw UnsupportedOperationException()
    }
}

class PlaylistStore(private val context: Context) {
    private val dbHelper: SQLiteOpenHelper = DbHelper(context)
    val savedPlaylists: Map<String, List<Long>> = getAll()

    fun getAll(): Map<String, List<Long>> {
        val playlists = mutableListOf<Pair<String, Long>>()
        dbHelper.readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT * FROM ${DbHelper.TABLE_NAME}", null)
            while (cursor.moveToNext()) {
                val playlistName = cursor.getString(0)
                val songId = cursor.getLong(1)
                playlists.add(playlistName to songId)
            }
            cursor.close()
        }
        return playlists.groupBy({ it.first }, { it.second })
    }

    fun insert(playlist: Playlist) {
        dbHelper.writableDatabase.use { db ->
            playlist.songs.forEach { (id, _, _, _) ->
                val values = ContentValues().apply {
                    put(DbHelper.PLAYLIST_NAME_COl, playlist.name)
                    put(DbHelper.SONG_ID_COL, id)
                }

                db.insert(DbHelper.TABLE_NAME, null, values)
            }
        }
    }

    fun delete(playlist: Playlist) {
        val whereClause = "${DbHelper.PLAYLIST_NAME_COl} = ?"
        dbHelper.writableDatabase.use { db ->
            db.delete(DbHelper.TABLE_NAME, whereClause, arrayOf(playlist.name))
        }
    }
}