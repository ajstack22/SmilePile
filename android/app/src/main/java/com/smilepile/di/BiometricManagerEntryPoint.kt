package com.smilepile.di

import com.smilepile.security.BiometricManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for accessing BiometricManager in Compose screens
 * where constructor injection is not available.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BiometricManagerEntryPoint {
    fun biometricManager(): BiometricManager
}
