package com.smilepile.data.backup

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling various export formats and export operations
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository
) {
    companion object {
        private const val TAG = "ExportManager"
        private const val HTML_TEMPLATE_FILENAME = "gallery_template.html"
        private const val EXPORT_DIR = "exports"
        private const val THUMBNAIL_SIZE = 300 // pixels
        private const val GALLERY_COLUMNS = 4 // for HTML gallery
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = false
    }

    /**
     * Export photos in specified format
     */
    suspend fun export(
        format: ExportFormat,
        options: ExportOptions = ExportOptions(),
        progressCallback: ((ExportProgress) -> Unit)? = null
    ): Result<File> {
        return when (format) {
            ExportFormat.ZIP -> exportToZip(options, progressCallback)
            ExportFormat.JSON -> exportToJson(options, progressCallback)
            ExportFormat.HTML_GALLERY -> exportToHtmlGallery(options, progressCallback)
            ExportFormat.PDF_CATALOG -> exportToPdfCatalog(options, progressCallback)
        }
    }

    /**
     * Export to ZIP format with photos
     */
    private suspend fun exportToZip(
        options: ExportOptions,
        progressCallback: ((ExportProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(context.cacheDir, "$EXPORT_DIR/SmilePile_Export_${timestamp}.zip")
            exportFile.parentFile?.mkdirs()

            // Get filtered data
            val (categories, photos) = getFilteredData(options)
            val totalItems = photos.size + 1 // photos + metadata
            var processedItems = 0

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = totalItems,
                    processedItems = 0,
                    currentOperation = "Preparing export"
                )
            )

            ZipOutputStream(FileOutputStream(exportFile)).use { zipOut ->
                // Add metadata
                val metadata = createExportMetadata(categories, photos, options)
                val metadataEntry = ZipEntry("metadata.json")
                zipOut.putNextEntry(metadataEntry)
                zipOut.write(json.encodeToString(metadata).toByteArray())
                zipOut.closeEntry()
                processedItems++

                progressCallback?.invoke(
                    ExportProgress(
                        totalItems = totalItems,
                        processedItems = processedItems,
                        currentOperation = "Adding photos to archive"
                    )
                )

                // Add photos
                photos.forEach { photo ->
                    if (!photo.isFromAssets) {
                        val photoFile = File(photo.path)
                        if (photoFile.exists()) {
                            val entry = ZipEntry("photos/${photo.id}_${photoFile.name}")
                            zipOut.putNextEntry(entry)

                            // Apply size optimization if requested
                            if (options.optimizeSize) {
                                val optimizedBytes = optimizePhotoSize(photoFile, options.maxPhotoSize)
                                zipOut.write(optimizedBytes)
                            } else {
                                FileInputStream(photoFile).use { input ->
                                    input.copyTo(zipOut)
                                }
                            }
                            zipOut.closeEntry()
                        }
                    }
                    processedItems++

                    progressCallback?.invoke(
                        ExportProgress(
                            totalItems = totalItems,
                            processedItems = processedItems,
                            currentOperation = "Exporting photo: ${photo.name}",
                            currentFile = photo.name,
                            bytesProcessed = processedItems.toLong(),
                            totalBytes = totalItems.toLong()
                        )
                    )
                }

                // Add categories summary
                if (options.includeCategorySummary) {
                    val categorySummary = createCategorySummary(categories, photos)
                    val summaryEntry = ZipEntry("categories_summary.json")
                    zipOut.putNextEntry(summaryEntry)
                    zipOut.write(json.encodeToString(categorySummary).toByteArray())
                    zipOut.closeEntry()
                }
            }

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = totalItems,
                    processedItems = totalItems,
                    currentOperation = "Export completed"
                )
            )

            Result.success(exportFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export to ZIP", e)
            Result.failure(e)
        }
    }

    /**
     * Export to JSON format (metadata only)
     */
    private suspend fun exportToJson(
        options: ExportOptions,
        progressCallback: ((ExportProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(context.cacheDir, "$EXPORT_DIR/SmilePile_Export_${timestamp}.json")
            exportFile.parentFile?.mkdirs()

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = 1,
                    processedItems = 0,
                    currentOperation = "Generating JSON export"
                )
            )

            // Get filtered data
            val (categories, photos) = getFilteredData(options)

            // Create export data
            val exportData = ExportData(
                exportDate = System.currentTimeMillis(),
                format = ExportFormat.JSON.name,
                categories = categories.map { CategoryExport.fromCategory(it) },
                photos = photos.map { PhotoExport.fromPhoto(it, options.includePhotoMetadata) },
                statistics = createExportStatistics(categories, photos)
            )

            // Write JSON
            val jsonString = json.encodeToString(exportData)
            exportFile.writeText(jsonString)

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = 1,
                    processedItems = 1,
                    currentOperation = "JSON export completed"
                )
            )

            Result.success(exportFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export to JSON", e)
            Result.failure(e)
        }
    }

    /**
     * Export to HTML gallery format
     */
    private suspend fun exportToHtmlGallery(
        options: ExportOptions,
        progressCallback: ((ExportProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "$EXPORT_DIR/gallery_$timestamp")
            exportDir.mkdirs()

            // Create subdirectories
            val imagesDir = File(exportDir, "images")
            val thumbsDir = File(exportDir, "thumbnails")
            imagesDir.mkdirs()
            thumbsDir.mkdirs()

            // Get filtered data
            val (categories, photos) = getFilteredData(options)
            val totalItems = photos.size * 2 + 1 // photos + thumbnails + HTML
            var processedItems = 0

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = totalItems,
                    processedItems = 0,
                    currentOperation = "Creating HTML gallery"
                )
            )

            // Process photos and create thumbnails
            val galleryItems = mutableListOf<GalleryItem>()
            photos.forEach { photo ->
                if (!photo.isFromAssets) {
                    val photoFile = File(photo.path)
                    if (photoFile.exists()) {
                        // Copy original or optimized photo
                        val imageName = "${photo.id}_${photoFile.name}"
                        val imageFile = File(imagesDir, imageName)

                        if (options.optimizeSize) {
                            val optimizedBytes = optimizePhotoSize(photoFile, options.maxPhotoSize)
                            imageFile.writeBytes(optimizedBytes)
                        } else {
                            photoFile.copyTo(imageFile)
                        }
                        processedItems++

                        // Create thumbnail
                        val thumbName = "thumb_$imageName"
                        val thumbFile = File(thumbsDir, thumbName)
                        createThumbnail(photoFile, thumbFile, THUMBNAIL_SIZE)
                        processedItems++

                        // Add to gallery items
                        val category = categories.find { it.id == photo.categoryId }
                        galleryItems.add(
                            GalleryItem(
                                photoId = photo.id.toString(),
                                title = photo.name,
                                imagePath = "images/$imageName",
                                thumbPath = "thumbnails/$thumbName",
                                category = category?.displayName ?: "Uncategorized",
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(photo.createdAt))
                            )
                        )

                        progressCallback?.invoke(
                            ExportProgress(
                                totalItems = totalItems,
                                processedItems = processedItems,
                                currentOperation = "Processing: ${photo.name}",
                                currentFile = photo.name
                            )
                        )
                    }
                }
            }

            // Generate HTML
            val htmlContent = generateHtmlGallery(galleryItems, categories)
            val htmlFile = File(exportDir, "index.html")
            htmlFile.writeText(htmlContent)
            processedItems++

            // Add CSS and JavaScript
            val cssContent = generateGalleryCss()
            File(exportDir, "style.css").writeText(cssContent)

            val jsContent = generateGalleryJavaScript()
            File(exportDir, "script.js").writeText(jsContent)

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = totalItems,
                    processedItems = totalItems,
                    currentOperation = "HTML gallery created"
                )
            )

            // Create ZIP of the gallery
            val zipFile = File(context.cacheDir, "$EXPORT_DIR/SmilePile_Gallery_$timestamp.zip")
            zipDirectory(exportDir, zipFile)

            // Clean up temp directory
            exportDir.deleteRecursively()

            Result.success(zipFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export HTML gallery", e)
            Result.failure(e)
        }
    }

    /**
     * Export to PDF catalog format
     */
    private suspend fun exportToPdfCatalog(
        options: ExportOptions,
        progressCallback: ((ExportProgress) -> Unit)?
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Note: PDF generation would require additional library like iText or Apache PDFBox
            // This is a placeholder implementation
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFile = File(context.cacheDir, "$EXPORT_DIR/SmilePile_Catalog_$timestamp.pdf")
            exportFile.parentFile?.mkdirs()

            progressCallback?.invoke(
                ExportProgress(
                    totalItems = 1,
                    processedItems = 0,
                    currentOperation = "Generating PDF catalog"
                )
            )

            // For now, return an error indicating PDF support is not yet implemented
            Result.failure(UnsupportedOperationException("PDF export is not yet implemented"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export to PDF", e)
            Result.failure(e)
        }
    }

    /**
     * Get filtered data based on export options
     */
    private suspend fun getFilteredData(options: ExportOptions): Pair<List<Category>, List<Photo>> {
        val categories = if (options.selectedCategories != null) {
            categoryRepository.getAllCategories().filter { it.id in options.selectedCategories }
        } else {
            categoryRepository.getAllCategories()
        }

        val photos = photoRepository.getAllPhotos().filter { photo ->
            // Filter by categories
            (options.selectedCategories == null || photo.categoryId in options.selectedCategories) &&
            // Filter by date range
            (options.dateRangeStart == null || photo.createdAt >= options.dateRangeStart) &&
            (options.dateRangeEnd == null || photo.createdAt <= options.dateRangeEnd) &&
            // Filter by favorites (removed - no longer supported)
            true
        }

        return Pair(categories, photos)
    }

    /**
     * Create export metadata
     */
    private fun createExportMetadata(
        categories: List<Category>,
        photos: List<Photo>,
        options: ExportOptions
    ): ExportMetadata {
        return ExportMetadata(
            exportDate = System.currentTimeMillis(),
            appVersion = getAppVersion(),
            categoriesCount = categories.size,
            photosCount = photos.size,
            exportOptions = options,
            deviceInfo = DeviceInfo(
                manufacturer = android.os.Build.MANUFACTURER,
                model = android.os.Build.MODEL,
                androidVersion = android.os.Build.VERSION.RELEASE
            )
        )
    }

    /**
     * Create category summary
     */
    private fun createCategorySummary(
        categories: List<Category>,
        photos: List<Photo>
    ): List<CategorySummary> {
        return categories.map { category ->
            val categoryPhotos = photos.filter { it.categoryId == category.id }
            CategorySummary(
                categoryId = category.id,
                categoryName = category.displayName,
                photoCount = categoryPhotos.size,
                totalSize = categoryPhotos.sumOf { it.fileSize },
                oldestPhoto = categoryPhotos.minByOrNull { it.createdAt }?.createdAt,
                newestPhoto = categoryPhotos.maxByOrNull { it.createdAt }?.createdAt
            )
        }
    }

    /**
     * Create export statistics
     */
    private fun createExportStatistics(
        categories: List<Category>,
        photos: List<Photo>
    ): ExportStatistics {
        return ExportStatistics(
            totalCategories = categories.size,
            totalPhotos = photos.size,
            totalFileSize = photos.sumOf { it.fileSize },
            favoriteCount = 0, // Favorites no longer supported
            dateRange = DateRange(
                start = photos.minByOrNull { it.createdAt }?.createdAt,
                end = photos.maxByOrNull { it.createdAt }?.createdAt
            )
        )
    }

    /**
     * Optimize photo size for export
     */
    private suspend fun optimizePhotoSize(
        photoFile: File,
        maxSize: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            val outputStream = ByteArrayOutputStream()

            // Calculate new dimensions maintaining aspect ratio
            val (newWidth, newHeight) = calculateOptimalDimensions(bitmap.width, bitmap.height, maxSize)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

            bitmap.recycle()
            scaledBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            // If optimization fails, return original
            photoFile.readBytes()
        }
    }

    /**
     * Create thumbnail for photo
     */
    private suspend fun createThumbnail(
        sourceFile: File,
        destFile: File,
        size: Int
    ) = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            val (newWidth, newHeight) = calculateOptimalDimensions(bitmap.width, bitmap.height, size)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            bitmap.recycle()
            scaledBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create thumbnail", e)
        }
    }

    /**
     * Calculate optimal dimensions maintaining aspect ratio
     */
    private fun calculateOptimalDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxSize: Int
    ): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        return if (originalWidth > originalHeight) {
            val newWidth = minOf(originalWidth, maxSize)
            val newHeight = (newWidth / aspectRatio).toInt()
            Pair(newWidth, newHeight)
        } else {
            val newHeight = minOf(originalHeight, maxSize)
            val newWidth = (newHeight * aspectRatio).toInt()
            Pair(newWidth, newHeight)
        }
    }

    /**
     * Generate HTML gallery content
     */
    private fun generateHtmlGallery(
        items: List<GalleryItem>,
        categories: List<Category>
    ): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SmilePile Photo Gallery</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header>
        <h1>SmilePile Photo Gallery</h1>
        <p>Exported on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</p>
        <div class="stats">
            <span>${items.size} Photos</span>
            <span>${categories.size} Categories</span>
        </div>
    </header>

    <nav class="filters">
        <button class="filter-btn active" data-filter="all">All</button>
        ${categories.joinToString("") { cat ->
            """<button class="filter-btn" data-filter="${cat.name}">${cat.displayName}</button>"""
        }}
    </nav>

    <main class="gallery">
        ${items.joinToString("") { item ->
            """
            <div class="gallery-item" data-category="${item.category}">
                <img src="${item.thumbPath}" alt="${item.title}" loading="lazy">
                <div class="item-info">
                    <h3>${item.title}</h3>
                    <p>${item.category}</p>
                    <p>${item.date}</p>
                </div>
                <a href="${item.imagePath}" class="view-full">View Full Size</a>
            </div>
            """
        }}
    </main>

    <footer>
        <p>Generated by SmilePile App</p>
    </footer>

    <script src="script.js"></script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Generate gallery CSS
     */
    private fun generateGalleryCss(): String {
        return """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
    background: #f5f5f5;
    color: #333;
}

header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 2rem;
    text-align: center;
}

header h1 {
    margin-bottom: 0.5rem;
}

.stats {
    margin-top: 1rem;
}

.stats span {
    margin: 0 1rem;
    padding: 0.25rem 0.75rem;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 1rem;
}

.filters {
    padding: 1rem;
    text-align: center;
    background: white;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.filter-btn {
    padding: 0.5rem 1rem;
    margin: 0.25rem;
    border: 2px solid #667eea;
    background: white;
    color: #667eea;
    border-radius: 2rem;
    cursor: pointer;
    transition: all 0.3s;
}

.filter-btn:hover,
.filter-btn.active {
    background: #667eea;
    color: white;
}

.gallery {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 1.5rem;
    padding: 2rem;
}

.gallery-item {
    background: white;
    border-radius: 0.5rem;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    transition: transform 0.3s;
}

.gallery-item:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0,0,0,0.2);
}

.gallery-item img {
    width: 100%;
    height: 200px;
    object-fit: cover;
}

.item-info {
    padding: 1rem;
}

.item-info h3 {
    margin-bottom: 0.5rem;
    color: #333;
}

.item-info p {
    color: #666;
    font-size: 0.9rem;
}

.favorite {
    color: gold;
    font-size: 1.2rem;
}

.view-full {
    display: block;
    text-align: center;
    padding: 0.75rem;
    background: #667eea;
    color: white;
    text-decoration: none;
    transition: background 0.3s;
}

.view-full:hover {
    background: #764ba2;
}

footer {
    text-align: center;
    padding: 2rem;
    background: #333;
    color: white;
}

@media (max-width: 768px) {
    .gallery {
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
        gap: 1rem;
        padding: 1rem;
    }
}
        """.trimIndent()
    }

    /**
     * Generate gallery JavaScript
     */
    private fun generateGalleryJavaScript(): String {
        return """
document.addEventListener('DOMContentLoaded', function() {
    const filterButtons = document.querySelectorAll('.filter-btn');
    const galleryItems = document.querySelectorAll('.gallery-item');

    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            const filter = this.getAttribute('data-filter');

            // Update active button
            filterButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // Filter items
            galleryItems.forEach(item => {
                if (filter === 'all' || item.getAttribute('data-category') === filter) {
                    item.style.display = 'block';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    });

    // Lazy loading for images
    const images = document.querySelectorAll('img[loading="lazy"]');
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.getAttribute('src');
                observer.unobserve(img);
            }
        });
    });

    images.forEach(img => imageObserver.observe(img));
});
        """.trimIndent()
    }

    /**
     * Zip a directory
     */
    private suspend fun zipDirectory(sourceDir: File, zipFile: File) = withContext(Dispatchers.IO) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(sourceDir).path
                    val entry = ZipEntry(entryName)
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Create export intent for sharing
     */
    fun createShareIntent(exportFile: File): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = when {
                exportFile.name.endsWith(".zip") -> "application/zip"
                exportFile.name.endsWith(".json") -> "application/json"
                exportFile.name.endsWith(".pdf") -> "application/pdf"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(exportFile))
            putExtra(Intent.EXTRA_SUBJECT, "SmilePile Export")
        }
    }
}

// Data classes for export

@kotlinx.serialization.Serializable
data class ExportOptions(
    val selectedCategories: List<Long>? = null,
    val dateRangeStart: Long? = null,
    val dateRangeEnd: Long? = null,
    val favoritesOnly: Boolean = false,
    val includePhotoMetadata: Boolean = true,
    val includeCategorySummary: Boolean = true,
    val optimizeSize: Boolean = false,
    val maxPhotoSize: Int = 1920 // pixels
)

data class ExportData(
    val exportDate: Long,
    val format: String,
    val categories: List<CategoryExport>,
    val photos: List<PhotoExport>,
    val statistics: ExportStatistics
)

@kotlinx.serialization.Serializable
data class CategoryExport(
    val id: Long,
    val name: String,
    val displayName: String,
    val position: Int,
    val iconResource: String?,
    val colorHex: String?,
    val photoCount: Int = 0
) {
    companion object {
        fun fromCategory(category: Category): CategoryExport {
            return CategoryExport(
                id = category.id,
                name = category.name,
                displayName = category.displayName,
                position = category.position,
                iconResource = category.iconResource,
                colorHex = category.colorHex
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class PhotoExport(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val createdAt: Long,
    val metadata: PhotoMetadata? = null
) {
    companion object {
        fun fromPhoto(photo: Photo, includeMetadata: Boolean): PhotoExport {
            return PhotoExport(
                id = photo.id,
                name = photo.name,
                categoryId = photo.categoryId,
                createdAt = photo.createdAt,
                metadata = if (includeMetadata) {
                    PhotoMetadata(
                        fileSize = photo.fileSize,
                        width = photo.width,
                        height = photo.height,
                        path = photo.path
                    )
                } else null
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class PhotoMetadata(
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val path: String
)

@kotlinx.serialization.Serializable
data class ExportMetadata(
    val exportDate: Long,
    val appVersion: String,
    val categoriesCount: Int,
    val photosCount: Int,
    val exportOptions: ExportOptions,
    val deviceInfo: DeviceInfo
)

@kotlinx.serialization.Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String
)

@kotlinx.serialization.Serializable
data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val photoCount: Int,
    val totalSize: Long,
    val oldestPhoto: Long?,
    val newestPhoto: Long?
)

@kotlinx.serialization.Serializable
data class ExportStatistics(
    val totalCategories: Int,
    val totalPhotos: Int,
    val totalFileSize: Long,
    val favoriteCount: Int,
    val dateRange: DateRange
)

@kotlinx.serialization.Serializable
data class DateRange(
    val start: Long?,
    val end: Long?
)

data class GalleryItem(
    val photoId: String,
    val title: String,
    val imagePath: String,
    val thumbPath: String,
    val category: String,
    val date: String
)