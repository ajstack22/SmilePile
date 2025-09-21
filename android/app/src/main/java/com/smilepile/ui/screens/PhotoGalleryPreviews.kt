package com.smilepile.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smilepile.data.models.Category
import com.smilepile.data.models.Photo
import com.smilepile.ui.theme.SmilePileTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PhotoGalleryScreenPreview() {
    SmilePileTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            // Note: This is just a structural preview.
            // Real implementation requires ViewModels and data repositories
            // which are not available in previews.
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoViewerScreenPreview() {
    SmilePileTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            // Note: This is just a structural preview.
            // Real implementation requires photo data which is not available in previews.
        }
    }
}