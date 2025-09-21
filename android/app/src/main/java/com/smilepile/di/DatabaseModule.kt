package com.smilepile.di

import android.content.Context
import androidx.room.Room
import com.smilepile.data.dao.CategoryDao
import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.database.SmilePileDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 * This module is installed in the SingletonComponent, making all provided dependencies
 * application-scoped singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the SmilePile Room database instance.
     * This is a singleton that will be shared across the application.
     *
     * @param context Application context provided by Hilt
     * @return SmilePileDatabase instance
     */
    @Provides
    @Singleton
    fun provideSmilePileDatabase(
        @ApplicationContext context: Context
    ): SmilePileDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SmilePileDatabase::class.java,
            "smilepile_database"
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    /**
     * Provides PhotoDao from the database.
     *
     * @param database SmilePileDatabase instance
     * @return PhotoDao instance
     */
    @Provides
    fun providePhotoDao(database: SmilePileDatabase): PhotoDao {
        return database.photoDao()
    }

    /**
     * Provides CategoryDao from the database.
     *
     * @param database SmilePileDatabase instance
     * @return CategoryDao instance
     */
    @Provides
    fun provideCategoryDao(database: SmilePileDatabase): CategoryDao {
        return database.categoryDao()
    }
}