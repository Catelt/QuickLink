package com.catelt.quicklink.presentation.objectdetection

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

@OptIn(ExperimentalGetImage::class)
class ObjectAnalyzer(
    private val onStableImage: (
        width: Int,
        height: Int,
        rotationDegrees: Int,
        capturedBitmap: Bitmap
    ) -> Unit
) : ImageAnalysis.Analyzer {
    private var isFrozen = false
    private var captureRequested = false

    fun reset() {
        isFrozen = false
        captureRequested = false
    }

    fun pauseDetection() {
        isFrozen = true
    }

    fun resumeDetection() {
        reset()
    }

    fun requestCapture() {
        captureRequested = true
        isFrozen = false
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (isFrozen) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val (rotatedWidth, rotatedHeight) =
            if (rotationDegrees == 0 || rotationDegrees == 180) {
                imageProxy.width to imageProxy.height
            } else {
                imageProxy.height to imageProxy.width
            }

        if (captureRequested) {
            createFullBitmap(imageProxy, rotationDegrees)?.let { bitmap ->
                isFrozen = true // stop further analysis after capture
                captureRequested = false
                onStableImage(rotatedWidth, rotatedHeight, rotationDegrees, bitmap)
            }
        }

        imageProxy.close()
    }

    private fun createFullBitmap(
        imageProxy: ImageProxy,
        rotationDegrees: Int
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
            } finally {
                rawBitmap.recycle()
            }
        } else {
            rawBitmap
        }

        if (rotatedBitmap.width <= MAX_DIM && rotatedBitmap.height <= MAX_DIM) return rotatedBitmap

        val scale = minOf(
            MAX_DIM.toFloat() / rotatedBitmap.width,
            MAX_DIM.toFloat() / rotatedBitmap.height
        )
        val scaledWidth = (rotatedBitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (rotatedBitmap.height * scale).toInt().coerceAtLeast(1)
        return try {
            Bitmap.createScaledBitmap(rotatedBitmap, scaledWidth, scaledHeight, true)
        } catch (_: Exception) {
            rotatedBitmap
        }
    }

    companion object {
        private const val MAX_DIM = 1920 // avoid OOM on very large crops
    }
}

