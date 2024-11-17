package com.example.cameraxtoottoot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.cameraxtoottoot.composables.CameraPreviewScreen
import com.example.cameraxtoottoot.ui.theme.CameraXtoottootTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cameraPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Log.d("CAMERA", "onCreate: CAMERA PERMISSION GRANTED")
                    setCameraPreview()
                } else {
                    Log.d("CAMERA", "onCreate: CAMERA PERMISSION DENIED")
                    throw IllegalStateException("User denied camera permission.")
                }
            }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                Log.d("PACKAGE MANAGER", "Camera permission already granted")
                setCameraPreview()
            }
            else -> {
                Log.d("PACKAGE MANAGER", "Requesting camera permission")
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setCameraPreview() {
        setContent {
            CameraXtoottootTheme {
                Scaffold { padding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val viewModel = MainViewModel()
                        CameraPreviewScreen(viewModel)
                    }
                }
            }
        }
    }
}
