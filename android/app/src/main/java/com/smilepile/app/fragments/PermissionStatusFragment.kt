package com.smilepile.app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smilepile.app.R
import com.smilepile.app.managers.PermissionManager
import com.smilepile.app.managers.PhotoImportManager
import com.smilepile.app.utils.PrivacyUtils

class PermissionStatusFragment : Fragment() {

    private lateinit var photoPickerStatus: TextView
    private lateinit var cameraStatus: TextView
    private lateinit var exifStatus: TextView
    private lateinit var internetStatus: TextView

    private lateinit var requestPhotoPermissionBtn: Button
    private lateinit var requestCameraPermissionBtn: Button
    private lateinit var openSettingsBtn: Button

    private lateinit var permissionManager: PermissionManager
    private lateinit var photoImportManager: PhotoImportManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.view_permission_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        permissionManager = PermissionManager(requireContext(), this)
        photoImportManager = PhotoImportManager(requireContext(), this)

        // Initialize views
        photoPickerStatus = view.findViewById(R.id.photoPickerStatus)
        cameraStatus = view.findViewById(R.id.cameraStatus)
        exifStatus = view.findViewById(R.id.exifStatus)
        internetStatus = view.findViewById(R.id.internetStatus)

        requestPhotoPermissionBtn = view.findViewById(R.id.requestPhotoPermissionBtn)
        requestCameraPermissionBtn = view.findViewById(R.id.requestCameraPermissionBtn)
        openSettingsBtn = view.findViewById(R.id.openSettingsBtn)

        setupPermissionButtons()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionButtons() {
        requestPhotoPermissionBtn.setOnClickListener {
            permissionManager.requestPhotoPickerPermissions { granted ->
                updatePermissionStatus()
                if (granted) {
                    // Permission granted - maybe show a success message
                }
            }
        }

        requestCameraPermissionBtn.setOnClickListener {
            permissionManager.requestCameraPermission { granted ->
                updatePermissionStatus()
                if (granted) {
                    // Permission granted - maybe show a success message
                }
            }
        }

        openSettingsBtn.setOnClickListener {
            openAppSettings()
        }
    }

    private fun updatePermissionStatus() {
        val permissionStatus = permissionManager.getPermissionStatus()

        // Update photo picker status
        val photoText = when {
            permissionStatus.usingModernPermissions && permissionStatus.photoPickerPermission == PermissionManager.PermissionState.GRANTED -> {
                "✓ Photo access: Granted (Android 13+)"
            }
            permissionStatus.usingModernPermissions && permissionStatus.photoPickerPermission == PermissionManager.PermissionState.DENIED -> {
                "○ Photo access: Permission needed"
            }
            !permissionStatus.usingModernPermissions && permissionStatus.photoPickerPermission == PermissionManager.PermissionState.GRANTED -> {
                "✓ Photo access: Permission granted"
            }
            else -> {
                "○ Photo access: Permission needed"
            }
        }
        photoPickerStatus.text = photoText
        updateStatusColor(photoPickerStatus)

        // Update camera status
        val cameraText = when (permissionStatus.cameraPermission) {
            PermissionManager.PermissionState.GRANTED -> "✓ Camera: Permission granted"
            PermissionManager.PermissionState.DENIED -> "○ Camera: Permission needed"
            PermissionManager.PermissionState.PERMANENTLY_DENIED -> "⚠ Camera: Denied - Settings required"
        }
        cameraStatus.text = cameraText
        updateStatusColor(cameraStatus)

        // Update button visibility
        updateButtonVisibility(permissionStatus)

        // EXIF status is always enabled
        exifStatus.text = "✓ EXIF metadata: Automatically removed"
        exifStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))

        // Check internet status
        val privacyStatus = PrivacyUtils.getPrivacyStatus(requireContext())
        internetStatus.text = if (privacyStatus.internetDisabled) {
            "✓ Internet: Disabled (child-safe)"
        } else {
            "⚠ Internet: Enabled"
        }
        internetStatus.setTextColor(
            if (privacyStatus.internetDisabled) {
                resources.getColor(android.R.color.holo_green_dark, null)
            } else {
                resources.getColor(android.R.color.holo_orange_dark, null)
            }
        )
    }

    private fun updateButtonVisibility(permissionStatus: PermissionManager.PermissionStatus) {
        // Show photo permission button if needed
        requestPhotoPermissionBtn.visibility = if (permissionStatus.photoPickerPermission == PermissionManager.PermissionState.DENIED) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Show camera permission button if needed
        requestCameraPermissionBtn.visibility = if (permissionStatus.cameraPermission == PermissionManager.PermissionState.DENIED) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Show settings button if any permission is permanently denied
        val showSettingsButton = permissionStatus.cameraPermission == PermissionManager.PermissionState.PERMANENTLY_DENIED ||
                permissionStatus.photoPickerPermission == PermissionManager.PermissionState.PERMANENTLY_DENIED

        openSettingsBtn.visibility = if (showSettingsButton) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun updateStatusColor(textView: TextView) {
        val text = textView.text.toString()
        when {
            text.startsWith("✓") -> {
                textView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
            text.startsWith("○") -> {
                textView.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            text.startsWith("⚠") -> {
                textView.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            else -> {
                textView.setTextColor(resources.getColor(android.R.color.primary_text_light, null))
            }
        }
    }

    companion object {
        fun newInstance(): PermissionStatusFragment {
            return PermissionStatusFragment()
        }
    }
}