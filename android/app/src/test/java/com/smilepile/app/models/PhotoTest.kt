package com.smilepile.app.models

import org.junit.Test
import org.junit.Assert.*

class PhotoTest {

    @Test
    fun `photo creation with all parameters should work correctly`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id",
            position = 2,
            dateAdded = 1234567890L,
            isFromAssets = false
        )

        assertEquals("photo_id", photo.id)
        assertEquals("image.jpg", photo.path)
        assertEquals("My Photo", photo.name)
        assertEquals("category_id", photo.categoryId)
        assertEquals(2, photo.position)
        assertEquals(1234567890L, photo.dateAdded)
        assertFalse(photo.isFromAssets)
    }

    @Test
    fun `photo creation with default parameters should work correctly`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id"
        )

        assertEquals("photo_id", photo.id)
        assertEquals("image.jpg", photo.path)
        assertEquals("My Photo", photo.name)
        assertEquals("category_id", photo.categoryId)
        assertEquals(0, photo.position)
        assertTrue(photo.dateAdded > 0)
        assertTrue(photo.isFromAssets)
    }

    @Test
    fun `isValid should return true for valid photo`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id"
        )

        assertTrue(photo.isValid())
    }

    @Test
    fun `isValid should return false for photo with blank id`() {
        val photo = Photo(
            id = "",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id"
        )

        assertFalse(photo.isValid())
    }

    @Test
    fun `isValid should return false for photo with blank path`() {
        val photo = Photo(
            id = "photo_id",
            path = "",
            name = "My Photo",
            categoryId = "category_id"
        )

        assertFalse(photo.isValid())
    }

    @Test
    fun `isValid should return false for photo with blank name`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "",
            categoryId = "category_id"
        )

        assertFalse(photo.isValid())
    }

    @Test
    fun `isValid should return false for photo with blank categoryId`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = ""
        )

        assertFalse(photo.isValid())
    }

    @Test
    fun `getAssetPath should add sample_images prefix when from assets and path doesn't already have it`() {
        val photo = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id",
            isFromAssets = true
        )

        assertEquals("sample_images/image.jpg", photo.getAssetPath())
    }

    @Test
    fun `getAssetPath should not add prefix when path already has sample_images`() {
        val photo = Photo(
            id = "photo_id",
            path = "sample_images/image.jpg",
            name = "My Photo",
            categoryId = "category_id",
            isFromAssets = true
        )

        assertEquals("sample_images/image.jpg", photo.getAssetPath())
    }

    @Test
    fun `getAssetPath should return original path when not from assets`() {
        val photo = Photo(
            id = "photo_id",
            path = "/storage/image.jpg",
            name = "My Photo",
            categoryId = "category_id",
            isFromAssets = false
        )

        assertEquals("/storage/image.jpg", photo.getAssetPath())
    }

    @Test
    fun `data class equality should work correctly`() {
        val photo1 = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id"
        )

        val photo2 = Photo(
            id = "photo_id",
            path = "image.jpg",
            name = "My Photo",
            categoryId = "category_id",
            dateAdded = photo1.dateAdded
        )

        assertEquals(photo1, photo2)
    }
}