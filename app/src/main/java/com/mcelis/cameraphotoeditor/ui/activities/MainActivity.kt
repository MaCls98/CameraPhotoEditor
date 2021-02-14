package com.mcelis.cameraphotoeditor.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.mcelis.cameraphotoeditor.R
import com.mcelis.cameraphotoeditor.utils.CAMERA_PERMISSION
import com.mcelis.cameraphotoeditor.utils.MyPermissionUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        MyPermissionUtils.checkPermissions(this)
        btn_take_photo.setOnClickListener {
            takePhoto()
        }
    }

    private fun launchPhotoActivity() {
        startActivity(Intent(this, PhotoActivity::class.java))
    }

    private fun takePhoto(){
        if (checkIfDeviceHasCamera()){
            if (MyPermissionUtils.checkPermissions(this, android.Manifest.permission.CAMERA, CAMERA_PERMISSION)){
                launchPhotoActivity()
            }
        }else{
            Toast.makeText(this, getString(R.string.no_camera_feature), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfDeviceHasCamera(): Boolean {
        return baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    launchPhotoActivity()
                }else{
                    Toast.makeText(this, getString(R.string.dialog_permission_description), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}