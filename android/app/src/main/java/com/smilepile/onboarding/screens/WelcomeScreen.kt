package com.smilepile.onboarding.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import com.smilepile.R

// SmilePile brand colors
private val SmilePileYellow = Color(0xFFFFBF00)
private val SmilePileBlue = Color(0xFF2196F3)
private val SmilePileGreen = Color(0xFF4CAF50)
private val SmilePileOrange = Color(0xFFFF6600)
private val SmilePilePink = Color(0xFFE86082)

// Google Fonts provider for Nunito
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Nunito font family
private val nunitoFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = provider,
        weight = FontWeight.ExtraBold
    )
)

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo and title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SmilePile logo icon
            Image(
                painter = painterResource(id = R.drawable.ic_smilepile_logo),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            // Multicolored "SmilePile" text
            Text(
                text = buildAnnotatedString {
                    // "Smile" in yellow
                    withStyle(
                        style = SpanStyle(
                            color = SmilePileYellow,
                            fontFamily = nunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(4f, 4f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append("Smile")
                    }

                    // "P" in green
                    withStyle(
                        style = SpanStyle(
                            color = SmilePileGreen,
                            fontFamily = nunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(4f, 4f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append("P")
                    }

                    // "i" in blue
                    withStyle(
                        style = SpanStyle(
                            color = SmilePileBlue,
                            fontFamily = nunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(4f, 4f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append("i")
                    }

                    // "l" in orange
                    withStyle(
                        style = SpanStyle(
                            color = SmilePileOrange,
                            fontFamily = nunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(4f, 4f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append("l")
                    }

                    // "e" in pink
                    withStyle(
                        style = SpanStyle(
                            color = SmilePilePink,
                            fontFamily = nunitoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(4f, 4f),
                                blurRadius = 6f
                            )
                        )
                    ) {
                        append("e")
                    }
                },
                style = TextStyle(
                    fontFamily = nunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Text(
                text = "A safe and fun photo gallery for EVERYONE",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Features list
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(vertical = 40.dp)
        ) {
            FeatureRow(
                icon = Icons.Outlined.Layers,
                title = "Organize photos into piles",
                description = "Create colorful piles for your photos",
                tintColor = SmilePileYellow
            )

            FeatureRow(
                icon = Icons.Default.FitScreen,
                title = "Distraction-free mode",
                description = "Good for kids (and everyone else)",
                tintColor = SmilePileOrange
            )

            FeatureRow(
                icon = Icons.Default.Lock,
                title = "Optional PIN protection",
                description = "Prevent inadvertent changes",
                tintColor = SmilePileGreen
            )
        }

        // Get Started button
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmilePileBlue
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    tintColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = tintColor
        )

        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}