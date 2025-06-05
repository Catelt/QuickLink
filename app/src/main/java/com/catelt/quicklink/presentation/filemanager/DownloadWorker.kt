package com.catelt.quicklink.presentation.filemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID

private var channelCreated = false

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder().build()

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationId: Int = params.id.hashCode()
    private var currentForegroundInfo: ForegroundInfo? = null

    init {
        if (!channelCreated) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    FOREGROUND_NOTIFICATION_CHANNEL_ID,
                    "Downloading",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for downloading"
                }
                notificationManager.createNotificationChannel(channel)
                channelCreated = true
            }
        }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure(
            Data.Builder().putString(KEY_RESULT_ERROR, "URL is required").build()
        )
        val filename = inputData.getString(KEY_FILENAME) ?: return Result.failure(
            Data.Builder().putString(KEY_RESULT_ERROR, "Filename is required").build()
        )

        return try {
            downloadFile(url, filename)
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error occurred"
            Result.failure(
                Data.Builder().putString(KEY_RESULT_ERROR, errorMessage).build()
            )
        }
    }

    private suspend fun downloadFile(url: String, filename: String): Result {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Failed to download: ${response.code}")
            }

            val body = response.body ?: throw IOException("Response body is null")
            val contentLength = body.contentLength()
            val isContentLengthKnown = contentLength > 0

            // Create download directory if it doesn't exist
            val downloadDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDirectory.exists()) {
                downloadDirectory.mkdirs()
            }

            val outputFile = File(downloadDirectory, filename)
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes: Long = 0
            var lastProgressUpdate = 0
            var lastUpdateTime = System.currentTimeMillis()

            // Initial notification
            setForeground(createForegroundInfo(progress = 0, filename = filename))

            // Set initial progress
            if (!isContentLengthKnown) {
                setProgress(
                    Data.Builder()
                        .putInt(KEY_PROGRESS, -1)
                        .putBoolean(KEY_IS_INDETERMINATE, true)
                        .putLong(KEY_DOWNLOADED_BYTES, 0)
                        .build()
                )
            }

            body.byteStream().use { inputStream ->
                outputStream.use { output ->
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        // Check if work has been cancelled
                        if (!isActive) {
                            outputFile.delete() // Clean up partial download
                            outputStream.close()
                            return@withContext Result.failure(
                                Data.Builder().putString(KEY_RESULT_ERROR, "Download cancelled")
                                    .build()
                            )
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val currentTime = System.currentTimeMillis()

                        if (isContentLengthKnown) {
                            val progress = (downloadedBytes * 100 / contentLength).toInt()
                            if (progress > lastProgressUpdate) {
                                lastProgressUpdate = progress

                                setForeground(
                                    createForegroundInfo(
                                        progress = progress,
                                        filename = filename,
                                    )
                                )
                                setProgress(
                                    Data.Builder()
                                        .putInt(KEY_PROGRESS, progress)
                                        .putBoolean(KEY_IS_INDETERMINATE, false)
                                        .putLong(KEY_DOWNLOADED_BYTES, downloadedBytes)
                                        .putString(KEY_URL, url)
                                        .build()
                                )
                            }
                        } else if (currentTime - lastUpdateTime > 1000) {
                            lastUpdateTime = currentTime
                            setForeground(
                                createForegroundInfo(
                                    filename = filename,
                                    bytesDownloaded = downloadedBytes,
                                )
                            )

                            setProgress(
                                Data.Builder()
                                    .putInt(KEY_PROGRESS, -1)
                                    .putBoolean(KEY_IS_INDETERMINATE, true)
                                    .putLong(KEY_DOWNLOADED_BYTES, downloadedBytes)
                                    .putString(KEY_URL, url)
                                    .build()
                            )
                        }
                    }
                }
            }

            outputStream.close()

            Result.success(
                Data.Builder()
                    .putString(KEY_RESULT_FILE_PATH, outputFile.absolutePath)
                    .build()
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return currentForegroundInfo ?: createForegroundInfo()
    }

    /**
     * Creates a [ForegroundInfo] object for the download worker's ongoing notification.
     * This notification is used to keep the worker running in the foreground, indicating
     * to the user that an active download is in progress.
     */
    private fun createForegroundInfo(
        filename: String? = null,
        progress: Int = 0,
        bytesDownloaded: Long? = null
    ): ForegroundInfo {
        // Create a notification for the foreground service
        var title = "Downloading"
        if (filename != null) {
            title = "Downloading \"$filename\""
        }
        val content = if (bytesDownloaded != null) {
            // Convert bytes to more readable format
            val sizeText = when {
                bytesDownloaded < 1024 -> "$bytesDownloaded B"
                bytesDownloaded < 1024 * 1024 -> "${bytesDownloaded / 1024} KB"
                else -> String.format(
                    Locale.getDefault(),
                    "%.2f MB",
                    bytesDownloaded / (1024.0 * 1024.0)
                )
            }
            "Downloaded: $sizeText"
        } else {
            "Downloading in progress: $progress%"
        }

        val maxProgress = if (bytesDownloaded != null) 0 else 100

        val notification =
            NotificationCompat.Builder(applicationContext, FOREGROUND_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setProgress(maxProgress, progress, false)
                .build()

        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                notificationId,
                notification,
            )
        }
        currentForegroundInfo = info
        return info
    }


    companion object {
        private const val FOREGROUND_NOTIFICATION_CHANNEL_ID = "download_channel_foreground"

        // Input data keys
        const val KEY_URL = "url"
        const val KEY_FILENAME = "filename"

        // Output data keys
        const val KEY_RESULT_ERROR = "error"
        const val KEY_RESULT_FILE_PATH = "file_path"

        // Progress data keys
        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_IS_INDETERMINATE = "is_indeterminate"

        const val WORK_TAG = "download_work"

        fun enqueueDownload(
            context: Context,
            url: String,
            filename: String
        ): UUID {
            val inputData = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_FILENAME, filename)
                .build()

            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .addTag(WORK_TAG)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueue(downloadRequest)
            return downloadRequest.id
        }

        fun cancelDownload(context: Context, workId: UUID) {
            WorkManager.getInstance(context).cancelWorkById(workId)
        }

        fun cancelAllDownloads(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }

        fun observeWorkProgress(context: Context, workId: UUID) =
            WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId)
    }
}