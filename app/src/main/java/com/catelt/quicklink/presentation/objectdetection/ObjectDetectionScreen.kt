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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
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

    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var sourceWidth by remember { mutableStateOf(0) }
    var sourceHeight by remember { mutableStateOf(0) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var analyzerRef by remember { mutableStateOf<ObjectAnalyzer?>(null) }
    var segmentedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val segmentationHelper = remember { SegmentationHelper() }
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

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                val analyzer = ObjectAnalyzer(
                    objectDetector = objectDetector,
                    onObjectsDetected = { objects, width, height, _, crop ->
                        detectedObjects = objects
                        sourceWidth = width
                        sourceHeight = height
                        if (crop != null) {
                            capturedBitmap = crop
                            segmentedBitmap = null
                            mainExecutor.execute {
                                cameraProvider?.unbindAll()
                            }
                        }
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
                    ObjectOverlay(
                        detectedObjects = detectedObjects,
                        sourceWidth = sourceWidth,
                        sourceHeight = sourceHeight,
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
                                        detectedObjects = emptyList()
                                        analyzerRef?.reset()
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
                text = "Point camera at objects to detect",
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

@Composable
fun ObjectOverlay(
    detectedObjects: List<DetectedObject>,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Avoid drawing until we know the source dimensions
        if (sourceWidth == 0 || sourceHeight == 0) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        // ML Kit boxes are in the image coordinate space; map to the preview
        val scale = kotlin.math.max(
            canvasWidth / sourceWidth,
            canvasHeight / sourceHeight
        )
        val offsetX = (canvasWidth - sourceWidth * scale) / 2f
        val offsetY = (canvasHeight - sourceHeight * scale) / 2f

        detectedObjects.forEach { obj ->
            val boundingBox = obj.boundingBox
            
            // Bounding box mapped into the view space
            val left = offsetX + boundingBox.left * scale
            val top = offsetY + boundingBox.top * scale
            val right = offsetX + boundingBox.right * scale
            val bottom = offsetY + boundingBox.bottom * scale

            // Draw bounding box
            drawRect(
                color = androidx.compose.ui.graphics.Color.Green,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )

            // Draw tracking ID if available
            obj.trackingId?.let { trackingId ->
                // Draw a small circle for tracking ID
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Red,
                    radius = 8f,
                    center = Offset(left, top)
                )
            }
        }
    }
}

