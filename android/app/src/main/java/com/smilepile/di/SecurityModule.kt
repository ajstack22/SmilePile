package com.smilepile.di

import com.smilepile.security.ISecurePreferencesManager
import com.smilepile.security.SecurePreferencesManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides security-related bindings.
 * This module binds security interfaces to their concrete implementations.
 * Using @Binds is more efficient than @Provides for interface bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    /**
     * Binds ISecurePreferencesManager interface to SecurePreferencesManager implementation.
     * This allows Hilt to inject ISecurePreferencesManager wherever it's needed.
     *
     * @param securePreferencesManager Concrete implementation of ISecurePreferencesManager
     * @return ISecurePreferencesManager interface
     */
    @Binds
    @Singleton
    abstract fun bindSecurePreferencesManager(
        securePreferencesManager: SecurePreferencesManager
    ): ISecurePreferencesManager
}
