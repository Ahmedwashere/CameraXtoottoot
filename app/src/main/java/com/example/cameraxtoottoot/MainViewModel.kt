package com.example.cameraxtoottoot

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *  This ViewModel is used to store pose landmarker helper settings
 */
class MainViewModel : ViewModel(), PoseLandmarkerHelper.LandmarkerListener {

    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_PRESENCE_CONFIDENCE

    val currentDelegate: Int get() = _delegate
    val currentModel: Int get() = _model
    val currentMinPoseDetectionConfidence: Float
        get() =
            _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float
        get() =
            _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float
        get() =
            _minPosePresenceConfidence

    private val _poseResults = MutableStateFlow<PoseLandmarkerHelper.ResultBundle?>(null)
    val poseResults: StateFlow<PoseLandmarkerHelper.ResultBundle?> = _poseResults

    fun updatePoseResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        _poseResults.value = resultBundle
    }


    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }

    fun setModel(model: Int) {
        _model = model
    }

    override fun onError(error: String, errorCode: Int) {
        Log.e("PoseLandmarker", "Error: $error (Code: $errorCode)")
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        _poseResults.value = resultBundle
    }
}