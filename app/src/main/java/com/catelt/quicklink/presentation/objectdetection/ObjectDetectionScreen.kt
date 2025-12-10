package com.catelt.quicklink.presentation.objectdetection

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.util.concurrent.Executors

@Composable
fun ObjectDetectionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var analyzerRef by remember { mutableStateOf<ObjectAnalyzer?>(null) }
    var segmentedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val segmentationHelper = remember { SegmentationHelper() }
    var stableSince by remember { mutableStateOf<Long?>(null) }
    var captureInFlight by remember { mutableStateOf(false) }
    val stabilityDetector = remember {
        StabilityDetector(context) { stable ->
            val now = System.currentTimeMillis()
            if (stable) {
                if (stableSince == null) stableSince = now
                val since = stableSince
                if (!captureInFlight && since != null && now - since >= 1_000L) {
                    captureInFlight = true
                    analyzerRef?.requestCapture()
                }
            } else {
                stableSince = null
            }
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var restartKey by remember { mutableStateOf(0) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoom by remember { mutableStateOf(1f) }
    var maxZoom by remember { mutableStateOf(1f) }

    fun bindCamera(provider: ProcessCameraProvider) {
        val pv = previewView ?: return

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = pv.surfaceProvider
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                val analyzer = ObjectAnalyzer(
                    onStableImage = { _, _, _, bitmap ->
                        capturedBitmap = bitmap
                        segmentedBitmap = null
                        mainExecutor.execute {
                            cameraProvider?.unbindAll()
                        }
                        captureInFlight = false
                        stableSince = null
                        stabilityDetector.stop()
                    }
                )
                analyzerRef = analyzer
                analysis.setAnalyzer(
                    executor,
                    analyzer
                )
            }

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis
            )
            camera = boundCamera
            boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                zoomRatio = zoomState.zoomRatio
                minZoom = zoomState.minZoomRatio
                maxZoom = zoomState.maxZoomRatio
            }
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                stabilityDetector.stop()
                analyzerRef?.reset()
                cameraProvider?.unbindAll()
                executor.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCameraPermission, restartKey) {
        if (hasCameraPermission) {
            stableSince = null
            captureInFlight = false
            stabilityDetector.start()
        } else {
            stabilityDetector.stop()
        }
    }

    // Run segmentation when a crop is captured
    LaunchedEffect(capturedBitmap) {
        segmentedBitmap = if (capturedBitmap != null) {
            segmentationHelper.segment(capturedBitmap!!)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val view = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewView = view

                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider
                            bindCamera(provider)
                        }, mainExecutor)

                        view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                LaunchedEffect(previewView, restartKey, cameraProvider, hasCameraPermission) {
                    val provider = cameraProvider ?: return@LaunchedEffect
                    if (previewView != null && hasCameraPermission) {
                        bindCamera(provider)
                    }
                }

                if (capturedBitmap == null && segmentedBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(camera, previewView) {
                                detectTapGestures { offset ->
                                    val pv = previewView ?: return@detectTapGestures
                                    val control = camera?.cameraControl ?: return@detectTapGestures
                                    val factory = pv.meteringPointFactory
                                    val point = factory.createPoint(offset.x, offset.y)
                                    val action = FocusMeteringAction.Builder(point).build()
                                    control.startFocusAndMetering(action)
                                }
                            }
                            .pointerInput(camera, zoomRatio, minZoom, maxZoom) {
                                detectTransformGestures { _, _, zoomChange, _ ->
                                    val control = camera?.cameraControl ?: return@detectTransformGestures
                                    val newZoom = (zoomRatio * zoomChange).coerceIn(minZoom, maxZoom)
                                    control.setZoomRatio(newZoom)
                                }
                            }
                    )
                } else {
                    segmentedBitmap?.let { displayBitmap ->
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Image(
                                    bitmap = displayBitmap.asImageBitmap(),
                                    contentDescription = "Captured object",
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .clip(RoundedCornerShape(32.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Button(
                                    onClick = {
                                        capturedBitmap = null
                                        segmentedBitmap = null
                                        captureInFlight = false
                                        stableSince = null
                                        stabilityDetector.start()
                                        analyzerRef?.resumeDetection()
                                        restartKey++
                                    },
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                ) {
                                    Text("Try again")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Hold camera steady to capture",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Color.LightGray,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Camera Permission Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
