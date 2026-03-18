package com.megalife.translator.util

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    fun hasStorageReadPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestCameraWithRationale(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        rationale: String
    ) {
        AlertDialog.Builder(context)
            .setTitle("Permission Needed")
            .setMessage(rationale)
            .setPositiveButton("OK") { _, _ ->
                launcher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun requestStorageWithRationale(
        context: Context,
        launcher: ActivityResultLauncher<String>,
        rationale: String
    ) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        AlertDialog.Builder(context)
            .setTitle("Permission Needed")
            .setMessage(rationale)
            .setPositiveButton("OK") { _, _ ->
                launcher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
