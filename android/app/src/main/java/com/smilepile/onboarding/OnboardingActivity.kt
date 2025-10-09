package com.smilepile.onboarding

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.smilepile.data.backup.BackupManager
import com.smilepile.data.backup.ImportStrategy
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.data.repository.PhotoRepository
import com.smilepile.security.BiometricManager
import com.smilepile.ui.theme.SmilePileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var photoRepository: PhotoRepository

    @Inject
    lateinit var biometricManager: BiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let { handleImportBackup(it) }
            }

            SmilePileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        uiState = uiState,
                        onNavigateNext = { viewModel.navigateNext() },
                        onNavigateBack = { viewModel.navigateBack() },
                        onSkip = { viewModel.skip() },
                        onCategoryAdded = { viewModel.addCategory(it) },
                        onCategoryRemoved = { viewModel.removeCategory(it) },
                        onPhotosSelected = { viewModel.setImportedPhotos(it) },
                        onPinSet = { viewModel.setPinCode(it) },
                        onBiometricToggle = { viewModel.setBiometricEnabled(it) },
                        isBiometricAvailable = biometricManager.isBiometricAvailable() == com.smilepile.security.BiometricAvailability.AVAILABLE,
                        onImportBackup = {
                            importLauncher.launch(arrayOf("application/zip", "*/*"))
                        },
                        onComplete = {
                            viewModel.completeOnboarding()
                            finish() // Close onboarding and return to main activity
                        }
                    )
                }
            }
        }
    }

    private fun handleImportBackup(uri: Uri) {
        lifecycleScope.launch {
            try {
                viewModel.startImportFlow()

                val tempFile = copyUriToTempFile(uri)
                val isZipFile = detectZipFormat(tempFile)

                // Execute import
                if (isZipFile) {
                    backupManager.importFromZip(
                        zipFile = tempFile,
                        strategy = ImportStrategy.MERGE
                    ) { _, _, _ -> }.collect { /* Track progress if needed */ }
                } else {
                    backupManager.importFromJson(
                        backupFile = tempFile,
                        strategy = ImportStrategy.MERGE
                    ).collect { /* Track progress if needed */ }
                }

                // Query database for accurate counts after import completes
                val categories = categoryRepository.getAllCategories()
                val photos = photoRepository.getAllPhotos()

                viewModel.setImportStats(categories.size, photos.size)
                viewModel.navigateNext()

                tempFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("OnboardingActivity", "Import failed", e)
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri): java.io.File {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Failed to read backup file")

        val tempFile = java.io.File(cacheDir, "import_temp_${System.currentTimeMillis()}")
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun detectZipFormat(tempFile: java.io.File): Boolean {
        return try {
            tempFile.name.endsWith(".zip") ||
            (tempFile.length() > 4 &&
             tempFile.inputStream().use { stream ->
                 val header = ByteArray(4)
                 stream.read(header)
                 header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
                 header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
             })
        } catch (e: Exception) {
            false
        }
    }
}