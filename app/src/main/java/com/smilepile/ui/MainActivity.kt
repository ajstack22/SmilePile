package com.smilepile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smilepile.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main Activity for SmilePile
 *
 * Entry point for the photo gallery application
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.d("MainActivity created")
    }
}