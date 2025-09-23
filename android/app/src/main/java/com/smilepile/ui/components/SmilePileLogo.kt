package com.smilepile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smilepile.R
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font

// SmilePile brand colors
private val smileYellow = Color(0xFFFFEB3B)
private val pileGreen = Color(0xFF4CAF50)
private val pileBlue = Color(0xFF2196F3)
private val pileOrange = Color(0xFFFF9800)
private val pilePink = Color(0xFFE91E63)
private val darkText = Color(0xFF212121)

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

/**
 * SmilePile logo component with icon and multicolored text
 * Displays the 5-smiley icon alongside "SmilePile" text where each letter has different colors
 */
@Composable
fun SmilePileLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    fontSize: TextUnit = 24.sp,
    showIcon: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Logo icon
        if (showIcon) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_smilepile_logo),
                contentDescription = "SmilePile Logo",
                modifier = Modifier.size(iconSize),
                tint = Color.Unspecified // Use original colors from vector
            )
        }

        // Multicolored text with black outline
        Text(
            text = buildAnnotatedString {
                // "Smile" in yellow with black shadow for outline effect
                withStyle(
                    style = SpanStyle(
                        color = smileYellow,
                        fontFamily = nunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize,
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
                        color = pileGreen,
                        fontFamily = nunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize,
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
                        color = pileBlue,
                        fontFamily = nunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize,
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
                        color = pileOrange,
                        fontFamily = nunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize,
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
                        color = pilePink,
                        fontFamily = nunitoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize,
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
    }
}

/**
 * Compact version of SmilePile logo for smaller spaces
 */
@Composable
fun SmilePileLogoCompact(
    modifier: Modifier = Modifier
) {
    SmilePileLogo(
        modifier = modifier,
        iconSize = 24.dp,
        fontSize = 18.sp
    )
}