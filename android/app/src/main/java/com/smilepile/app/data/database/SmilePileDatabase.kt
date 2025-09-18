package com.smilepile.app.data.database

import androidx.room.RoomDatabase

/**
 * Room database for SmilePile app.
 * This is the main database configuration for the application.
 *
 * Database configuration is simplified for initial setup.
 * Will be expanded with proper entities and DAOs as features are implemented.
 */
abstract class SmilePileDatabase : RoomDatabase() {

    // Placeholder for future DAOs
    // abstract fun photoDao(): PhotoDao
    // abstract fun albumDao(): AlbumDao
    // abstract fun userDao(): UserDao
}