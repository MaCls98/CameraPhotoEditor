package com.mcelis.cameraphotoeditor.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mcelis.cameraphotoeditor.R

object MyPermissionUtils {

    private var permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun checkPermissions(activity: Activity): Boolean{
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(activity, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    listPermissionsNeeded.toTypedArray(),
                    100
            )
            return false
        }
        return true
    }

    fun checkPermissions(activity: Activity, permissionRequest: String, requestCode: Int): Boolean {
        return if (ContextCompat.checkSelfPermission(
                activity,
                permissionRequest
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permissionRequest
                )
            ) {
                AlertDialog.Builder(activity)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_permission_title)
                    .setMessage(R.string.dialog_permission_description)
                    .setPositiveButton(
                        R.string.dialog_permission_accept
                    ) { _, _ ->
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(permissionRequest),
                            requestCode
                        )
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permissionRequest),
                    requestCode
                )
            }
            false
        } else {
            true
        }
    }
}