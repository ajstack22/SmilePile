package com.smilepile.app.data.database.dao

import androidx.room.*
import com.smilepile.app.data.database.entities.Album
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Album entity.
 * Contains all database queries for albums used in performance testing.
 */
@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY name ASC")
    suspend fun getAllAlbums(): List<Album>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): Album?

    @Query("SELECT * FROM albums WHERE name = :name")
    suspend fun getAlbumByName(name: String): Album?

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    @Insert
    suspend fun insertAlbum(album: Album): Long

    @Insert
    suspend fun insertAlbums(albums: List<Album>): List<Long>

    @Update
    suspend fun updateAlbum(album: Album)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Query("UPDATE albums SET photoCount = :count WHERE id = :albumId")
    suspend fun updatePhotoCount(albumId: Long, count: Int)

    // Flow-based queries for reactive UI
    @Query("SELECT * FROM albums ORDER BY dateCreated DESC")
    fun getAllAlbumsFlow(): Flow<List<Album>>

    // Performance testing queries
    @Query("SELECT * FROM albums WHERE photoCount > :minPhotoCount")
    suspend fun getAlbumsWithMinPhotos(minPhotoCount: Int): List<Album>

    @Query("SELECT * FROM albums ORDER BY photoCount DESC LIMIT :limit")
    suspend fun getTopAlbumsByPhotoCount(limit: Int): List<Album>
}