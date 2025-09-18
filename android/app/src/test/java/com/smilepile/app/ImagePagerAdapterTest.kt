package com.smilepile.app

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class ImagePagerAdapterTest {

    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager
    private lateinit var adapter: ImagePagerAdapter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockContext = mockk()
        mockAssetManager = mockk()
        every { mockContext.assets } returns mockAssetManager
    }

    @Test
    fun `adapter returns correct item count with images`() {
        val imagePaths = listOf("sample_images/image1.png", "sample_images/image2.png")
        adapter = ImagePagerAdapter(mockContext, imagePaths)

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `adapter returns 1 item count when no images available`() {
        val imagePaths = emptyList<String>()
        adapter = ImagePagerAdapter(mockContext, imagePaths)

        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles empty image list gracefully`() {
        val imagePaths = emptyList<String>()
        adapter = ImagePagerAdapter(mockContext, imagePaths)

        assertNotNull(adapter)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles asset loading error gracefully`() {
        val imagePaths = listOf("sample_images/nonexistent.png")
        adapter = ImagePagerAdapter(mockContext, imagePaths)

        every { mockAssetManager.open(any()) } throws IOException("File not found")

        // Should not throw exception
        assertNotNull(adapter)
        assertEquals(1, adapter.itemCount)
    }
}