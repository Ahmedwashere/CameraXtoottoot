package com.example.cameraxtoottoot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.cameraxtoottoot.ui.theme.CameraXtoottootTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        /** Permissions must be checked BEFORE the activity runs. */

        // Here we are defining a function, not actually calling the function
        val cameraPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    isGranted ->
                if (isGranted) {
                    Log.d("CAMERA", "onCreate: CAMERA PERMISSION GRANTED")
                } else {
                    Log.d("CAMERA", "onCreate: CAMERA PERMISSION DEEENIED!!!!")
                    throw IllegalStateException("User refused to give camera permission so we gonna" +
                            "crash the app. :) ")
                }
            }


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraXtoottootTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    when(PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.CAMERA
                        ) -> {
                            // We execute this line of code when permission to use the camera is already granted
                            Log.d("PACKAGE MANAGER", "We can implement Camera related code now")
                        }
                        else -> {
                            // We call this when we need to request the permission
                            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
                        }
                    }

                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CameraXtoottootTheme {
        Greeting("Android")
    }
}