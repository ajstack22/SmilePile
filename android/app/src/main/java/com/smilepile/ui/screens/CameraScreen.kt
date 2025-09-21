package com.smilepile.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smilepile.camera.FlashMode
import com.smilepile.ui.theme.SmilePileTheme
import com.smilepile.ui.viewmodels.CameraViewModel

/**
 * Camera screen for photo capture with child-friendly UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    categoryId: Long = 1L, // Default category
    onNavigateBack: () -> Unit,
    onPhotoCapture: (Long) -> Unit, // Navigate to gallery with captured photo
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCameraInitialized by viewModel.isCameraInitialized.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val hasFrontCamera by viewModel.hasFrontCamera.collectAsStateWithLifecycle()
    val isUsingFrontCamera by viewModel.isUsingFrontCamera.collectAsStateWithLifecycle()

    // Permission handling
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Camera preview view
    val previewView = remember { PreviewView(context) }

    // Initialize camera when permission is granted
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !isCameraInitialized) {
            viewModel.initializeCamera(context, lifecycleOwner, previewView)
        }
    }

    // Request camera permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Handle photo capture success
    LaunchedEffect(uiState.lastCapturedPhotoId) {
        uiState.lastCapturedPhotoId?.let { photoId ->
            onPhotoCapture(photoId)
        }
    }

    SmilePileTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (!hasCameraPermission) {
                // Permission required screen
                CameraPermissionScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onNavigateBack = onNavigateBack
                )
            } else if (uiState.isLoading) {
                // Loading screen
                CameraLoadingScreen()
            } else {
                // Main camera UI
                CameraContent(
                    previewView = previewView,
                    flashMode = flashMode,
                    hasFrontCamera = hasFrontCamera,
                    isUsingFrontCamera = isUsingFrontCamera,
                    isCapturing = uiState.isCapturing,
                    onCapturePhoto = { viewModel.capturePhoto(context, categoryId) },
                    onToggleFlash = { viewModel.toggleFlashMode() },
                    onSwitchCamera = { viewModel.switchCamera(lifecycleOwner) },
                    onNavigateBack = onNavigateBack
                )
            }

            // Error snackbar
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Auto-clear error after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearError()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Success message
            if (uiState.showCaptureSuccess) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    viewModel.clearCaptureSuccess()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.Green,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Photo Saved!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    previewView: PreviewView,
    flashMode: FlashMode,
    hasFrontCamera: Boolean,
    isUsingFrontCamera: Boolean,
    isCapturing: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Flash toggle button
            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                val flashIcon = when (flashMode) {
                    FlashMode.OFF -> Icons.Default.FlashOff
                    FlashMode.AUTO -> Icons.Default.FlashAuto
                    FlashMode.ON -> Icons.Default.FlashOn
                }
                Icon(
                    flashIcon,
                    contentDescription = "Flash: $flashMode",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera switch button (only show if front camera available)
            if (hasFrontCamera) {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }

            // Capture button with animation
            CaptureButton(
                isCapturing = isCapturing,
                onCapture = onCapturePhoto
            )

            // Placeholder for symmetry
            Spacer(modifier = Modifier.size(64.dp))
        }
    }
}

@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    onCapture: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.8f else 1f,
        animationSpec = tween(200),
        label = "capture_scale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isCapturing) 360f else 0f,
        animationSpec = tween(1000),
        label = "capture_rotation"
    )

    Button(
        onClick = { if (!isCapturing) onCapture() },
        modifier = Modifier
            .size(80.dp)
            .scale(scale),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCapturing) Color.Red else Color.White,
            contentColor = Color.Black
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = !isCapturing
    ) {
        if (isCapturing) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Canvas(
                modifier = Modifier.size(60.dp)
            ) {
                drawCaptureButton(this)
            }
        }
    }
}

private fun drawCaptureButton(drawScope: DrawScope) {
    with(drawScope) {
        // Outer circle
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 2,
            style = Stroke(width = 4.dp.toPx())
        )
        // Inner circle
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 3
        )
    }
}

@Composable
private fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Camera Permission Needed",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SmilePile needs camera access to take photos. This helps you capture and organize your favorite moments!",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Allow Camera",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text(
                "Go Back",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun CameraLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 4.dp,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Starting Camera...",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}