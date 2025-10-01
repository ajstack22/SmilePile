package com.smilepile.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@Composable
fun PinSetupScreen(
    onPinSet: (String) -> Unit,
    onSkip: () -> Unit
) {
    var pinCode by remember { mutableStateOf("") }
    var confirmPinCode by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val pinLength = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        PinHeader(
            isConfirming = isConfirming,
            showError = showError,
            errorMessage = errorMessage,
            currentPin = if (isConfirming) confirmPinCode else pinCode
        )

        Spacer(modifier = Modifier.height(24.dp))

        PinNumberPad(
            pinLength = pinLength,
            currentPin = if (isConfirming) confirmPinCode else pinCode,
            onNumberClick = { digit ->
                addDigit(digit, if (isConfirming) confirmPinCode else pinCode, pinLength)?.let { newPin ->
                    if (isConfirming) confirmPinCode = newPin else pinCode = newPin
                }
            },
            onBackspace = {
                if (isConfirming && confirmPinCode.isNotEmpty()) {
                    confirmPinCode = confirmPinCode.dropLast(1)
                } else if (!isConfirming && pinCode.isNotEmpty()) {
                    pinCode = pinCode.dropLast(1)
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        PinActionButtons(
            isConfirming = isConfirming,
            pinCode = pinCode,
            confirmPinCode = confirmPinCode,
            pinLength = pinLength,
            onSkip = onSkip,
            onAction = {
                if (isConfirming) {
                    if (pinCode == confirmPinCode) {
                        onPinSet(pinCode)
                    } else {
                        errorMessage = "PINs don't match. Please try again."
                        showError = true
                        confirmPinCode = ""
                    }
                } else {
                    if (pinCode.length == pinLength) {
                        isConfirming = true
                        confirmPinCode = ""
                        showError = false
                    }
                }
            }
        )
    }
}

@Composable
private fun NumberButton(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun addDigit(digit: String, current: String, maxLength: Int): String? {
    return if (current.length < maxLength) {
        current + digit
    } else null
}

@Composable
private fun PinHeader(
    isConfirming: Boolean,
    showError: Boolean,
    errorMessage: String,
    currentPin: String
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = Color(0xFFFFBF00)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = if (isConfirming) "Confirm Your PIN" else "Set Up PIN Protection",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    if (isConfirming) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please enter your PIN again",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
    } else {
        Spacer(modifier = Modifier.height(32.dp))
    }

    PinDotsIndicator(4, currentPin)

    if (showError) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp)
        )
    } else {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PinDotsIndicator(pinLength: Int, currentPin: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(pinLength) { index ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < currentPin.length)
                            Color(0xFFFFBF00)
                        else Color.Gray.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun PinNumberPad(
    pinLength: Int,
    currentPin: String,
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column {
        PinDotsIndicator(pinLength, currentPin)

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 1..3) {
                        val number = row * 3 + col
                        NumberButton(number = number.toString()) {
                            onNumberClick(number.toString())
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .clickable { onBackspace() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                NumberButton(number = "0") { onNumberClick("0") }

                Spacer(modifier = Modifier.size(70.dp))
            }
        }
    }
}

@Composable
private fun PinActionButtons(
    isConfirming: Boolean,
    pinCode: String,
    confirmPinCode: String,
    pinLength: Int,
    onSkip: () -> Unit,
    onAction: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (!isConfirming) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Skip",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Button(
            onClick = onAction,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = (isConfirming && confirmPinCode.length == pinLength) ||
                      (!isConfirming && pinCode.length == pinLength),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text(
                text = if (isConfirming) "Confirm PIN" else "Set PIN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}