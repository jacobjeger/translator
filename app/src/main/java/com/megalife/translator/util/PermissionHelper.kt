package com.megalife.translator.util

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasCameraPermission(activity: AppCompatActivity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStorageReadPermission(activity: AppCompatActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasStorageWritePermission(activity: AppCompatActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage, no permission needed for MediaStore
        } else {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestWithRationale(
        activity: AppCompatActivity,
        rationale: String,
        launcher: ActivityResultLauncher<String>,
        permission: String
    ) {
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage(rationale)
                .setPositiveButton("Grant") { _, _ ->
                    launcher.launch(permission)
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            launcher.launch(permission)
        }
    }

    fun getStorageReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
