package com.example.cameraxtoottoot.composables

import android.util.Log
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
import com.example.cameraxtoottoot.SquatQuality
import com.example.cameraxtoottoot.WorkoutResultViewModel
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mlkit.vision.pose.PoseLandmark
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun CameraPreviewForWorkoutScreen(
    viewModel: MainViewModel,
    workoutResultViewModel: WorkoutResultViewModel
) {
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

    WorkoutScreen(workoutResultViewModel = workoutResultViewModel) {
        Box(
            modifier = Modifier
                .size(380.dp, 450.dp)
                .padding(start = 0.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            AndroidView(
                { previewView },
                modifier = Modifier
                    .size(380.dp, 450.dp)
                    .padding(start = 0.dp)
                    .clip(RoundedCornerShape(30.dp)),
            )
            OverlayCanvas(viewModel = viewModel, workoutResultViewModel = workoutResultViewModel)
        }
    }
}

@Composable
fun OverlayCanvas(viewModel: MainViewModel, workoutResultViewModel: WorkoutResultViewModel) {

    //Check if we should perform analysis. If not, return
    val isAnalysisActive by workoutResultViewModel.isAnalysisActive
    if (!isAnalysisActive) {
        return
    }

    val poseResults by viewModel.poseResults.collectAsState()

    Canvas(
        modifier = Modifier
            .size(380.dp, 450.dp)
            .padding(start = 0.dp)
            .clip(RoundedCornerShape(30.dp))
    ) {
        poseResults?.let { resultBundle ->
            val landmarksList = resultBundle.results.first().landmarks()
            if (landmarksList.isNotEmpty()) {
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

                val leftHip = landmarks[PoseLandmark.LEFT_HIP]
                val leftKnee = landmarks[PoseLandmark.LEFT_KNEE]
                val leftAnkle = landmarks[PoseLandmark.LEFT_ANKLE]

                val kneeAngle = viewModel.calculateAngle(leftHip, leftKnee, leftAnkle)
                val isSquatDeepEnough = kneeAngle <= 100

                Log.d("KNEE_ANGLE", "The Knee Angle is $kneeAngle.")
                Log.d("KNEE_ANGLE", "The Squat is Deep enough: $isSquatDeepEnough")

                val rightKnee = landmarks[PoseLandmark.RIGHT_KNEE]
                val rightAnkle = landmarks[PoseLandmark.RIGHT_ANKLE]

                val isKneesCollapsing =
                    (leftKnee.x() - leftAnkle.x()) * (rightKnee.x() - rightAnkle.x()) < 0

                if (isKneesCollapsing) {
                    workoutResultViewModel.setCollapseKneeFramesCount(workoutResultViewModel.getCollapseKneeFrameCount() + 1)
                    if (workoutResultViewModel.getCollapseKneeFrameCount() >= 3) {
                        workoutResultViewModel.setKneeCollapsed(true)
                    }
                }

                Log.d("KNEE_COLLAPSING", "The Knee Collapsing: $isKneesCollapsing")

                // Add the Functionality of Updating the currentState: Standing, Squating, Etc
                workoutResultViewModel.setCurrentPosition(kneeAngle)

                /* Check if we are standing. If we are, then check the values of our Deep Squat Frames.
                * If we see 3 valid frames, then increase rep count and reset the values of out deep squat
                * and need improvement squat count. If 3 need improvement frames are see instead,
                * Then the user did not squat deep enough. Also reset knee collapse as we are on a new repetition
                */
                if (workoutResultViewModel.getCurrentPosition() == SquatQuality.STANDING) {
                    if (workoutResultViewModel.getDeepSquatFramesCount() >= 3) {
                        workoutResultViewModel.setSquatRepetitions(
                            workoutResultViewModel.getSquatRepetitions() + 1
                        )
                        workoutResultViewModel.setDeepSquatFramesCount(0)
                        workoutResultViewModel.setNeedImprovementSquatFramesCount(0)
                        Log.d(
                            "REPS_GOOD",
                            "Increased the number of repetitions to ${workoutResultViewModel.getSquatRepetitions()}"
                        )

                    } else if (workoutResultViewModel.getNeedImprovementSquatFramesCount() >= 3) {
                        workoutResultViewModel.setNeedImprovement(true)
                        workoutResultViewModel.setDeepSquatFramesCount(0)
                        workoutResultViewModel.setNeedImprovementSquatFramesCount(0)
                        workoutResultViewModel.setSquatRepetitions(
                            workoutResultViewModel.getSquatRepetitions() + 1
                        )
                        Log.d(
                            "REPS_NEED_IMPROVEMENT",
                            "Increased the number of repetitions to ${workoutResultViewModel.getSquatRepetitions()}"
                        )
                    }
                } else if (workoutResultViewModel.getCurrentPosition() == SquatQuality.DEEP_SQUAT) {
                    // Increment the deep squat frame count
                    workoutResultViewModel.setDeepSquatFramesCount(workoutResultViewModel.getDeepSquatFramesCount() + 1)

                } else if (workoutResultViewModel.getCurrentPosition() == SquatQuality.NEEDS_IMPROVEMENT) {
                    // Increment the needs improvement frame count
                    workoutResultViewModel.setNeedImprovementSquatFramesCount(workoutResultViewModel.getNeedImprovementSquatFramesCount() + 1)

                } else {
                    // No Operation Here
                    Log.d("SQUAT_POSITION", "The current position is in the NO_OPERATION_ZONE.")
                }
            }
        }
    }
}

@Composable
fun WorkoutScreen(
    workoutResultViewModel: WorkoutResultViewModel,
    event: @Composable () -> Unit,
) {

    // Add The Timer Here
    val isAnalysisActive by workoutResultViewModel.isAnalysisActive
    val remainingTime by workoutResultViewModel.remainingTime

    LaunchedEffect(Unit) {
        // Set up the 60 second time slot
        val totalTime = 60
        workoutResultViewModel.setRemainingTime(totalTime)

        // 10-second setup period
        delay(10000L)
        workoutResultViewModel.startAnalysis()

        // 30-second active analysis period
        for (time in totalTime downTo 1) {
            workoutResultViewModel.setRemainingTime(time)
            delay(1000L)
        }

        // Stop analysis after 30 seconds
        workoutResultViewModel.stopAnalysis()
    }

    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(start = 40.dp, end = 25.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    formatSeconds(workoutResultViewModel.remainingTime.value),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Call The Lambda Function Here
            event()

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
                Box(modifier = Modifier.fillMaxSize()) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .height(30.dp)
                                .width(100.dp)
                                .padding(start = 30.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.timer_icon),
                                contentDescription = "timer icon",
                                modifier = Modifier.size(30.dp)
                            )

                            /** 60 seconds will be the hard coded amount of time for a set */
                            Text(
                                "60s",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .height(30.dp)
                                .width(80.dp)
                                .padding(end = 30.dp),
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.squat_icon),
                                contentDescription = "timer icon",
                                // Order of function calls matters in the Modifier!?!
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(30.dp)
                                    .background(Color(0xFF92C851)),
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
                    Text(
                        workoutResultViewModel.getSquatRepetitions().toString(),
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }


    }
}

fun formatSeconds(seconds: Int): String {
    val numMinutes = seconds / 60
    val numSeconds = seconds % 60
    return String.format("%02d:%02d", numMinutes, numSeconds)
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun WorkoutScreenPreview() {
    val workoutResultViewModel = WorkoutResultViewModel()
    WorkoutScreen(workoutResultViewModel = workoutResultViewModel) {
        /** TODO: Replace The Image With the Camera Preview and Canvas*/
        Image(
            painter = painterResource(id = R.drawable.workout_image),
            contentDescription = "Man Squatting",
            modifier = Modifier
                .size(380.dp, 450.dp)
                .padding(start = 0.dp)
                .clip(RoundedCornerShape(30.dp)),
            contentScale = ContentScale.Crop
        )
    }
}