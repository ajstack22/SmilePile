package com.smilepile.di

import android.content.Context
import com.smilepile.database.SmilePileDatabase
import com.smilepile.database.dao.CategoryDao
import com.smilepile.database.dao.PhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmilePileDatabase(
        @ApplicationContext context: Context
    ): SmilePileDatabase {
        return SmilePileDatabase.getDatabase(context)
    }

    @Provides
    fun provideCategoryDao(database: SmilePileDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun providePhotoDao(database: SmilePileDatabase): PhotoDao {
        return database.photoDao()
    }
}