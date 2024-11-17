package com.example.cameraxtoottoot.composables

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cameraxtoottoot.MainViewModel
import com.example.cameraxtoottoot.PoseLandmarkerHelper
import com.example.cameraxtoottoot.R
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors

@Composable
fun CameraPreviewScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context)
    }
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = context,
            runningMode = RunningMode.LIVE_STREAM,
            poseLandmarkerHelperListener = viewModel
        )
    }
    val isFrontCamera = true
//    val preview = Preview.Builder().build()
//    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
//    val imageAnalysis = remember {
//        ImageAnalysis.Builder().build()
//    }
    LaunchedEffect(Unit) {
        // Get the Provider and the Preview wh
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Make the ImageAnalysis Use Case
        val imageAnalysis = ImageAnalysis.Builder()
            // Non-Blocking
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysisUseCase ->
                analysisUseCase.setAnalyzer(executor) { imageProxy ->
                    poseLandmarkerHelper.detectLiveStream(imageProxy, isFrontCamera)
                }
            }

        // Take out all use cases
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    }

    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView(
            { previewView },
            modifier = Modifier
                .size(height = 500.dp, width = 400.dp)
                .align(Alignment.TopCenter)
                .padding(60.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        OverlayCanvas(viewModel = viewModel)

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF666666), RoundedCornerShape(10.dp))
                    .padding(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    progress = { return@CircularProgressIndicator 0.70f },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 20.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = Color.White,
                    color = Color.Blue
                )
            }
        }


    }
}

@Composable
fun OverlayCanvas(viewModel: MainViewModel) {
    val poseResults by viewModel.poseResults.collectAsState()

    Canvas(
        modifier = Modifier
            .size(height = 500.dp, width = 400.dp)
            .padding(60.dp)
    ) {
        poseResults?.let { resultBundle ->
            val landmarksList = resultBundle.results.first().landmarks()
            if (landmarksList.size > 0) {
                val landmarks = landmarksList.first()
                landmarks.forEach { landmark ->
                    drawCircle(
                        color = Color.Yellow,
                        center = Offset(
                            x = landmark.x() * size.width,
                            y = landmark.y() * size.height
                        ),
                        radius = 8f
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewOfWorkoutScreen() {
    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(start = 40.dp, end = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                /** TODO: Replace the countdown with the Video Model */
                Text("00:34",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White)

                Button(
                    modifier = Modifier.size(40.dp),
                    onClick = {/** TODO: Add pausing Timer Functionality */},
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonColors(containerColor = Color.Transparent,
                        contentColor = Color.Transparent,
                        disabledContentColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.pause_icon),
                        contentDescription = "Pause Icon",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            /** TODO: Replace The Image With the Camera Preview and Canvas*/
            Image(
                painter = painterResource(id = R.drawable.workout_image),
                contentDescription = "Man Squatting",
                modifier = Modifier
                    .size(380.dp, 450.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 0.dp)
                    .clip(RoundedCornerShape(30.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(15.dp))

            Column(
                modifier = Modifier
                    .size(380.dp, 240.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF712FE4), RoundedCornerShape(10.dp))
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box (modifier = Modifier.fillMaxSize()) {

                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.height(30.dp).width(100.dp).padding(start = 30.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.timer_icon),
                                contentDescription = "timer icon",
                                modifier = Modifier.size(30.dp)
                            )

                            /** 80 seconds will be the hard coded amount of time for a set */
                            Text(
                                "60s",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.height(30.dp).width(80.dp).padding(end = 30.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.squat_icon),
                                contentDescription = "timer icon",
                                // Order of function calls matters in the Modifier!?!
                                modifier = Modifier.clip(CircleShape).size(30.dp).background(Color(0xFF92C851)),
                            )

                            /** TODO: Replace With The Number Of Repetitions That the user said they would perform */
                            Text(
                                "5",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    CircularProgressIndicator(
                        progress = { return@CircularProgressIndicator 0.70f },
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.Center),
                        strokeWidth = 20.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = Color(0xFF9F85D8),
                        color = Color.White
                    )

                    /** TODO: Replace the values with a second value*/
                    Text("5",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
            }
        }


    }
}