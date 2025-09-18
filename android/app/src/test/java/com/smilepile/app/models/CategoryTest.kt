package com.smilepile.app.models

import org.junit.Test
import org.junit.Assert.*

class CategoryTest {

    @Test
    fun `category creation with all parameters should work correctly`() {
        val category = Category(
            id = "test_id",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = "path/to/cover.jpg",
            description = "Test description",
            photoCount = 5,
            position = 1,
            createdAt = 1234567890L
        )

        assertEquals("test_id", category.id)
        assertEquals("test_name", category.name)
        assertEquals("Test Display Name", category.displayName)
        assertEquals("path/to/cover.jpg", category.coverImagePath)
        assertEquals("Test description", category.description)
        assertEquals(5, category.photoCount)
        assertEquals(1, category.position)
        assertEquals(1234567890L, category.createdAt)
    }

    @Test
    fun `category creation with default parameters should work correctly`() {
        val category = Category(
            id = "test_id",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = null
        )

        assertEquals("test_id", category.id)
        assertEquals("test_name", category.name)
        assertEquals("Test Display Name", category.displayName)
        assertNull(category.coverImagePath)
        assertEquals("", category.description)
        assertEquals(0, category.photoCount)
        assertEquals(0, category.position)
        assertTrue(category.createdAt > 0)
    }

    @Test
    fun `isValid should return true for valid category`() {
        val category = Category(
            id = "test_id",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = null
        )

        assertTrue(category.isValid())
    }

    @Test
    fun `isValid should return false for category with blank id`() {
        val category = Category(
            id = "",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = null
        )

        assertFalse(category.isValid())
    }

    @Test
    fun `isValid should return false for category with blank name`() {
        val category = Category(
            id = "test_id",
            name = "",
            displayName = "Test Display Name",
            coverImagePath = null
        )

        assertFalse(category.isValid())
    }

    @Test
    fun `isValid should return false for category with blank displayName`() {
        val category = Category(
            id = "test_id",
            name = "test_name",
            displayName = "",
            coverImagePath = null
        )

        assertFalse(category.isValid())
    }

    @Test
    fun `data class equality should work correctly`() {
        val category1 = Category(
            id = "test_id",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = null
        )

        val category2 = Category(
            id = "test_id",
            name = "test_name",
            displayName = "Test Display Name",
            coverImagePath = null,
            createdAt = category1.createdAt
        )

        assertEquals(category1, category2)
    }
}