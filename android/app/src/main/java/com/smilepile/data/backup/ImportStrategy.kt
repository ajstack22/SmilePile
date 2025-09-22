package com.smilepile.data.backup

/**
 * Strategy for importing backup data
 */
enum class ImportStrategy {
    /**
     * Merge imported data with existing data
     * - Categories: Update existing, add new ones
     * - Photos: Skip duplicates based on MediaStore URI, add new ones
     */
    MERGE,

    /**
     * Replace all existing data with imported data
     * - Deletes all existing categories and photos
     * - Imports all data from backup
     */
    REPLACE
}