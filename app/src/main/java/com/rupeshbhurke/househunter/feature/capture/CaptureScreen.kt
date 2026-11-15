package com.rupeshbhurke.househunter.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rupeshbhurke.househunter.R
import com.rupeshbhurke.househunter.core.util.FieldFormat
import kotlinx.coroutines.delay
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private fun Context.has(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private suspend fun Context.cameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}

@Composable
fun CaptureScreen(
    onClose: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    var hasCamera by remember { mutableStateOf(context.has(Manifest.permission.CAMERA)) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) cameraPermission.launch(Manifest.permission.CAMERA) }

    var videoMode by rememberSaveable { mutableStateOf(viewModel.videoNote) }
    var useFront by rememberSaveable { mutableStateOf(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    LaunchedEffect(hasCamera) { if (hasCamera) provider = context.cameraProvider() }

    // Only the use cases for the current mode are bound; that works on every camera hardware level.
    LaunchedEffect(provider, videoMode, useFront) {
        val cameraProvider = provider ?: return@LaunchedEffect
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            if (videoMode) {
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)),
                    )
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                imageCapture = null
                videoCapture = capture
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } else {
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                videoCapture = null
                imageCapture = capture
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            }
        } catch (_: IllegalArgumentException) {
            // The requested camera (e.g. a front one) does not exist on this device.
            if (useFront) useFront = false
        }
    }

    LaunchedEffect(recording) {
        elapsedSec = 0
        while (recording != null) {
            delay(1_000)
            elapsedSec++
        }
    }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }
    BackHandler(enabled = recording != null) { recording?.stop() }

    fun startRecording(withAudio: Boolean) {
        val capture = videoCapture ?: return
        val file = viewModel.newFile("mp4")
        var pending = capture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
        if (withAudio && context.has(Manifest.permission.RECORD_AUDIO)) pending = pending.withAudioEnabled()
        recording = pending.start(executor) { event ->
            if (event is VideoRecordEvent.Finalize) {
                recording = null
                if (event.hasError()) {
                    file.delete()
                } else {
                    viewModel.onVideoSaved(file, event.recordingStats.recordedDurationNanos / 1_000_000)
                }
            }
        }
    }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        startRecording(granted)
    }

    fun onShutter() {
        if (!videoMode) {
            val file: File = viewModel.newFile("jpg")
            imageCapture?.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) = viewModel.onPhotoSaved(file)
                    override fun onError(exception: ImageCaptureException) {
                        file.delete()
                    }
                },
            )
        } else if (recording != null) {
            recording?.stop()
        } else if (context.has(Manifest.permission.RECORD_AUDIO)) {
            startRecording(withAudio = true)
        } else {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCamera) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.capture_permission_needed),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.capture_grant))
                }
            }
        } else {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        Row(
            modifier = Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.capture_close), tint = Color.White)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (recording != null) {
                    Text(
                        "● ${FieldFormat.duration(elapsedSec * 1_000L)}",
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else if (saved > 0) {
                    Text(
                        pluralStringResource(R.plurals.capture_saved_count, saved, saved),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            IconButton(onClick = { useFront = !useFront }, enabled = recording == null) {
                Icon(Icons.Filled.FlipCameraAndroid, contentDescription = stringResource(R.string.capture_flip), tint = Color.White)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!viewModel.videoNote) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val colors = FilterChipDefaults.filterChipColors(
                        labelColor = Color.White,
                        selectedLabelColor = Color.Black,
                        selectedContainerColor = Color.White,
                    )
                    FilterChip(
                        selected = !videoMode,
                        onClick = { videoMode = false },
                        enabled = recording == null,
                        label = { Text(stringResource(R.string.capture_photo)) },
                        colors = colors,
                    )
                    FilterChip(
                        selected = videoMode,
                        onClick = { videoMode = true },
                        enabled = recording == null,
                        label = { Text(stringResource(R.string.capture_video)) },
                        colors = colors,
                    )
                }
            }
            ShutterButton(videoMode = videoMode, recording = recording != null, onClick = ::onShutter, enabled = hasCamera)
        }
    }
}

@Composable
private fun ShutterButton(videoMode: Boolean, recording: Boolean, onClick: () -> Unit, enabled: Boolean) {
    val label = stringResource(
        when {
            recording -> R.string.capture_stop
            videoMode -> R.string.capture_record
            else -> R.string.capture_take_photo
        },
    )
    Box(
        modifier = Modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .background(if (videoMode) Color(0xFFE53935) else Color.White, if (recording) androidx.compose.foundation.shape.RoundedCornerShape(10.dp) else CircleShape)
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
    )
}
