package com.smilepile.di

import com.smilepile.utils.IImageProcessor
import com.smilepile.utils.ImageProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides image processing-related bindings.
 * This module binds image processing interfaces to their concrete implementations.
 * Using @Binds is more efficient than @Provides for interface bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProcessingModule {

    /**
     * Binds IImageProcessor interface to ImageProcessor implementation.
     * This allows Hilt to inject IImageProcessor wherever it's needed.
     *
     * @param imageProcessor Concrete implementation of IImageProcessor
     * @return IImageProcessor interface
     */
    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        imageProcessor: ImageProcessor
    ): IImageProcessor
}