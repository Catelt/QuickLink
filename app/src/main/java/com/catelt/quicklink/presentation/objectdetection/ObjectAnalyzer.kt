package com.catelt.quicklink.presentation.objectdetection

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetector

@OptIn(ExperimentalGetImage::class)
class ObjectAnalyzer(
    private val objectDetector: ObjectDetector,
    private val onObjectsDetected: (
        objects: List<DetectedObject>,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        croppedBitmap: Bitmap?
    ) -> Unit
) : ImageAnalysis.Analyzer {
    private var stableStartMs: Long? = null
    private var lastTrackingId: Int? = null
    private var lastCapturedTrackingId: Int? = null
    private var isFrozen = false

    fun reset() {
        stableStartMs = null
        lastTrackingId = null
        lastCapturedTrackingId = null
        isFrozen = false
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (isFrozen) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            objectDetector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    // Report objects along with the input dimensions (post rotation swap)
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val (rotatedWidth, rotatedHeight) =
                        if (rotationDegrees == 0 || rotationDegrees == 180) {
                            imageProxy.width to imageProxy.height
                        } else {
                            imageProxy.height to imageProxy.width
                        }
                    // Keep only the highest-confidence object (single detection mode)
                    val singleObject = detectedObjects
                        .sortedByDescending { it.labels.firstOrNull()?.confidence ?: 0f }
                        .take(1)

                    val target = singleObject.firstOrNull()

                    // Track stability for ~1s before capturing a crop
                    val now = SystemClock.elapsedRealtime()
                    if (target != null) {
                        val trackingId = target.trackingId
                        if (trackingId != null && trackingId == lastTrackingId) {
                            if (stableStartMs == null) stableStartMs = now
                        } else {
                            stableStartMs = now
                            lastTrackingId = trackingId
                        }
                    } else {
                        stableStartMs = null
                        lastTrackingId = null
                    }

                    val isStable = stableStartMs?.let { now - it >= STABLE_MS } == true

                    val cropBitmap =
                        if (isStable &&
                            target != null &&
                            target.trackingId != null &&
                            target.trackingId != lastCapturedTrackingId
                        ) {
                            createCrop(imageProxy, rotationDegrees, target.boundingBox)?.also {
                                lastCapturedTrackingId = target.trackingId
                                isFrozen = true // stop further detection after capture
                            }
                        } else {
                            null
                        }

                    onObjectsDetected(
                        singleObject,
                        rotatedWidth,
                        rotatedHeight,
                        rotationDegrees,
                        cropBitmap
                    )
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun createCrop(
        imageProxy: ImageProxy,
        rotationDegrees: Int,
        boundingBox: Rect
    ): Bitmap? {
        val rawBitmap = imageProxy.toBitmap()
        val rotatedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            try {
                Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true
                )
            } catch (_: Exception) {
                return null
            }
        } else {
            rawBitmap
        }

        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(rotatedBitmap.width)
        val bottom = boundingBox.bottom.coerceAtMost(rotatedBitmap.height)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return try {
            Bitmap.createBitmap(rotatedBitmap, left, top, width, height)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val STABLE_MS = 1_000L
    }
}

