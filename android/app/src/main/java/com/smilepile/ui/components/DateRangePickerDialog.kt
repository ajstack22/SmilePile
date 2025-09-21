package com.smilepile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smilepile.ui.viewmodels.DateRange
import com.smilepile.ui.viewmodels.DateRangePreset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateRangePickerDialog(
    currentDateRange: DateRange?,
    onDateRangeSelected: (DateRange?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember { mutableStateOf(currentDateRange?.preset) }
    var showCustomPicker by remember { mutableStateOf(false) }

    // Create date range picker state for custom selection
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = currentDateRange?.startDate,
        initialSelectedEndDateMillis = currentDateRange?.endDate
    )

    // Date formatter for displaying dates
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (showCustomPicker) {
                // Custom date range picker view
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Date Range",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { showCustomPicker = false }
                        ) {
                            Text("Back")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.height(400.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val startDate = dateRangePickerState.selectedStartDateMillis
                                val endDate = dateRangePickerState.selectedEndDateMillis

                                if (startDate != null && endDate != null) {
                                    onDateRangeSelected(
                                        DateRange(
                                            startDate = startDate,
                                            endDate = endDate,
                                            preset = null
                                        )
                                    )
                                } else {
                                    onDateRangeSelected(null)
                                }
                            },
                            enabled = dateRangePickerState.selectedStartDateMillis != null &&
                                    dateRangePickerState.selectedEndDateMillis != null
                        ) {
                            Text("Apply")
                        }
                    }
                }
            } else {
                // Preset selection view
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select Date Range",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current selection display
                    if (currentDateRange != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Current Selection",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentDateRange.getDisplayText(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (currentDateRange.preset == null) {
                                    Text(
                                        text = "${dateFormatter.format(Date(currentDateRange.startDate))} - ${dateFormatter.format(Date(currentDateRange.endDate))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Preset options
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateRangePreset.values().forEach { preset ->
                            FilterChip(
                                onClick = { selectedPreset = preset },
                                label = { Text(preset.displayName) },
                                selected = selectedPreset == preset,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Custom date range option
                    OutlinedButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Date Range")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDateRangeSelected(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                selectedPreset?.let { preset ->
                                    // Calculate date range for the selected preset
                                    val calendar = Calendar.getInstance()
                                    val endDate = calendar.timeInMillis

                                    val startDate = when (preset) {
                                        DateRangePreset.TODAY -> {
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            calendar.timeInMillis
                                        }
                                        DateRangePreset.THIS_WEEK -> {
                                            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            calendar.timeInMillis
                                        }
                                        DateRangePreset.THIS_MONTH -> {
                                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            calendar.timeInMillis
                                        }
                                        DateRangePreset.THIS_YEAR -> {
                                            calendar.set(Calendar.DAY_OF_YEAR, 1)
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            calendar.timeInMillis
                                        }
                                        DateRangePreset.LAST_WEEK -> {
                                            calendar.add(Calendar.WEEK_OF_YEAR, -1)
                                            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            val start = calendar.timeInMillis
                                            calendar.add(Calendar.DAY_OF_YEAR, 6)
                                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                                            calendar.set(Calendar.MINUTE, 59)
                                            calendar.set(Calendar.SECOND, 59)
                                            start
                                        }
                                        DateRangePreset.LAST_MONTH -> {
                                            calendar.add(Calendar.MONTH, -1)
                                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                                            calendar.set(Calendar.MINUTE, 0)
                                            calendar.set(Calendar.SECOND, 0)
                                            calendar.set(Calendar.MILLISECOND, 0)
                                            val start = calendar.timeInMillis
                                            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                                            calendar.set(Calendar.MINUTE, 59)
                                            calendar.set(Calendar.SECOND, 59)
                                            start
                                        }
                                    }

                                    val finalEndDate = if (preset == DateRangePreset.LAST_WEEK || preset == DateRangePreset.LAST_MONTH) {
                                        calendar.timeInMillis
                                    } else {
                                        endDate
                                    }

                                    onDateRangeSelected(
                                        DateRange(
                                            startDate = startDate,
                                            endDate = finalEndDate,
                                            preset = preset
                                        )
                                    )
                                }
                            },
                            enabled = selectedPreset != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangePresetChip(
    preset: DateRangePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = { Text(preset.displayName) },
        selected = isSelected,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}