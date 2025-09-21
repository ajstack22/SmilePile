package com.smilepile.di

import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.CategoryRepositoryImpl
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.data.repository.PhotoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository bindings.
 * This module binds repository interfaces to their concrete implementations.
 * Using @Binds is more efficient than @Provides for interface bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds PhotoRepository interface to PhotoRepositoryImpl implementation.
     * This allows Hilt to inject PhotoRepository wherever it's needed.
     *
     * @param photoRepositoryImpl Concrete implementation of PhotoRepository
     * @return PhotoRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository

    /**
     * Binds CategoryRepository interface to CategoryRepositoryImpl implementation.
     * This allows Hilt to inject CategoryRepository wherever it's needed.
     *
     * @param categoryRepositoryImpl Concrete implementation of CategoryRepository
     * @return CategoryRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository
}