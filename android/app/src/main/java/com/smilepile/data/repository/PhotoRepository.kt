package com.smilepile.data.repository

import com.smilepile.data.models.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    suspend fun insertPhoto(photo: Photo): Long
    suspend fun insertPhotos(photos: List<Photo>)
    suspend fun updatePhoto(photo: Photo)
    suspend fun deletePhoto(photo: Photo)
    suspend fun deletePhotoById(photoId: Long)
    suspend fun getPhotoById(photoId: Long): Photo?
    suspend fun getPhotoByPath(path: String): Photo?
    suspend fun getPhotosByCategory(categoryId: Long): List<Photo>
    fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>>
    suspend fun getPhotosInCategories(categoryIds: List<Long>): List<Photo>
    fun getPhotosInCategoriesFlow(categoryIds: List<Long>): Flow<List<Photo>>
    suspend fun getAllPhotos(): List<Photo>
    fun getAllPhotosFlow(): Flow<List<Photo>>
    suspend fun deletePhotosByCategory(categoryId: Long)
    suspend fun getPhotoCount(): Int
    suspend fun getPhotoCategoryCount(categoryId: Long): Int

    // Remove from library (app only, not device)
    suspend fun removeFromLibrary(photo: Photo)
    suspend fun removeFromLibraryById(photoId: Long)
}