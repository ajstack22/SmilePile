package com.smilepile.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smilepile.onboarding.TempCategory
import kotlinx.coroutines.delay

@Composable
fun CompletionScreen(
    categories: List<TempCategory>,
    photosImported: Int,
    pinEnabled: Boolean,
    onComplete: () -> Unit
) {
    var showCheckmark by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showCheckmark = true
        delay(500)
        showContent = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Success animation
        Box(
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showCheckmark,
                enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50) // SmilePile green
                    )
                }
            }
        }

        // Success message
        androidx.compose.animation.AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "All Set!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "SmilePile is ready to use",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Summary
        androidx.compose.animation.AnimatedVisibility(
            visible = showContent,
            enter = fadeIn()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Piles created
                    if (categories.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Layers,
                                contentDescription = null,
                                tint = Color(0xFFFF6600), // SmilePile orange
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${categories.size} piles created",
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Photos imported
                    if (photosImported > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                tint = Color(0xFF4ECDC4),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "$photosImported photos imported",
                                fontSize = 14.sp
                            )
                        }
                    }

                    // PIN enabled
                    if (pinEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF45B7D1),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "PIN protection enabled",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Start button
        androidx.compose.animation.AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it }
        ) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Start Using SmilePile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}