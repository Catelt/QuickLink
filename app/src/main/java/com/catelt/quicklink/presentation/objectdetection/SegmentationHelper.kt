package com.catelt.quicklink.presentation.objectdetection

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight helper to obtain a foreground-only bitmap from ML Kit Subject Segmentation.
 */
class SegmentationHelper {
    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    suspend fun segment(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = Tasks.await(segmenter.process(image))
            result.foregroundBitmap
        } catch (_: Exception) {
            null
        }
    }
}

