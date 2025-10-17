package com.smilepile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smilepile.ui.viewmodels.DemoModeViewModel

// Purple color for demo mode theme
private val DemoPurple = Color(0xFF9C27B0)

@Composable
fun DemoModeBanner(
    viewModel: DemoModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    if (uiState.isDemoMode) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DemoPurple)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Demo Mode - Viewing Jamie's Photos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = { showExitDialog = true },
                    enabled = !uiState.isExiting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DemoPurple
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Exit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { if (!uiState.isExiting) showExitDialog = false },
                title = {
                    Text("Exit Demo Mode?")
                },
                text = {
                    Text("This will remove all demo photos and categories. You can try demo mode again later.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.exitDemoMode()
                            showExitDialog = false
                        },
                        enabled = !uiState.isExiting,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Exit Demo")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExitDialog = false },
                        enabled = !uiState.isExiting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error dialog
        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
