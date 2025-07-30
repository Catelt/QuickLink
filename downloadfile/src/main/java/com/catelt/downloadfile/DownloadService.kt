package com.catelt.downloadfile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class DownloadService : Service() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder().build()
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val downloadJobs = mutableMapOf<Int, Job>()
    private var nextDownloadId = 1

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val FOREGROUND_SERVICE_ID = 1001

        // Intent actions
        const val ACTION_START_DOWNLOAD = "com.yourapp.download.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.yourapp.download.CANCEL_DOWNLOAD"

        // Intent extras
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"

        // Broadcast actions
        const val BROADCAST_DOWNLOAD_PROGRESS = "com.yourapp.download.PROGRESS"
        const val BROADCAST_DOWNLOAD_COMPLETE = "com.yourapp.download.COMPLETE"
        const val BROADCAST_DOWNLOAD_ERROR = "com.yourapp.download.ERROR"

        // Broadcast extras
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_INDETERMINATE = "extra_indeterminate"
        const val EXTRA_DOWNLOADED_BYTES = "extra_downloaded_bytes"

        fun startDownload(context: Context, url: String, filename: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, filename)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: Int) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val filename = intent.getStringExtra(EXTRA_FILENAME) ?: return START_NOT_STICKY

                val downloadId = nextDownloadId++
                startForeground(downloadId)
                startDownload(downloadId, url, filename)
                return START_STICKY
            }

            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1) {
                    cancelDownload(downloadId)
                }
                return START_NOT_STICKY
            }

            else -> return START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadJobs.values.forEach { it.cancel() }
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Download Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for file download operations"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground(downloadId: Int) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("File Download")
            .setContentText("Download in progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(downloadId + FOREGROUND_SERVICE_ID, notification)
    }

    private fun startDownload(downloadId: Int, url: String, filename: String) {
        val job = coroutineScope.launch {
            try {
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

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                var downloadedBytes: Long = 0
                var lastProgressUpdate = 0
                var lastUpdateTime = System.currentTimeMillis()

                // Update notification for indeterminate progress if content length is unknown
                if (!isContentLengthKnown) {
                    updateNotificationIndeterminate(downloadId, filename)
                    broadcastIndeterminateProgress(downloadId)
                }

                body.byteStream().use { inputStream ->
                    outputStream.use { output ->
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            // Check if the job has been cancelled
                            if (!isActive) {
                                throw IOException("Download cancelled")
                            }

                            output.write(buffer, 0, bytesRead)

                            downloadedBytes += bytesRead

                            val currentTime = System.currentTimeMillis()

                            if (isContentLengthKnown) {
                                // We have content length - show exact progress
                                val progress = (downloadedBytes * 100 / contentLength).toInt()
                                if (progress > lastProgressUpdate) {
                                    lastProgressUpdate = progress
                                    updateNotification(downloadId, filename, progress)
                                    broadcastProgress(downloadId, progress, url)
                                }
                            } else if (currentTime - lastUpdateTime > 1000) {
                                // No content length - update every second with downloaded size
                                lastUpdateTime = currentTime
                                updateNotificationWithBytes(downloadId, filename, downloadedBytes)
                                broadcastProgressWithBytes(downloadId, downloadedBytes, url)
                            }
                        }
                    }
                }

                // Download complete
                broadcastComplete(downloadId, filename)

                // Update notification to show completion
                val completeNotification =
                    NotificationCompat.Builder(this@DownloadService, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Download Complete")
                        .setContentText("$filename has been downloaded")
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()

                notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, completeNotification)

                // Clean up
                downloadJobs.remove(downloadId)
                if (downloadJobs.isEmpty()) {
                    stopSelf()
                }

            } catch (e: Exception) {
                // Handle error
                val errorMessage = e.message ?: "Unknown error occurred"
                broadcastError(downloadId, errorMessage)

                // Update notification to show error
                val errorNotification =
                    NotificationCompat.Builder(this@DownloadService, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Download Failed")
                        .setContentText("Failed to download $filename: $errorMessage")
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()

                notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, errorNotification)

                // Clean up
                downloadJobs.remove(downloadId)
                if (downloadJobs.isEmpty()) {
                    stopSelf()
                }
            }
        }

        downloadJobs[downloadId] = job
    }

    private fun cancelDownload(downloadId: Int) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)

        // Update notification to show cancellation
        val cancelNotification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download Cancelled")
            .setContentText("Download has been cancelled")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, cancelNotification)

        if (downloadJobs.isEmpty()) {
            stopSelf()
        }
    }

    private fun updateNotification(downloadId: Int, filename: String, progress: Int) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $filename")
            .setContentText("$progress% complete")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)
            .build()

        notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, notification)
    }

    private fun updateNotificationIndeterminate(downloadId: Int, filename: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $filename")
            .setContentText("Download in progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true) // Indeterminate progress
            .build()

        notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, notification)
    }

    private fun updateNotificationWithBytes(
        downloadId: Int,
        filename: String,
        bytesDownloaded: Long
    ) {
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

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading $filename")
            .setContentText("Downloaded: $sizeText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true) // Indeterminate progress
            .build()

        notificationManager.notify(downloadId + FOREGROUND_SERVICE_ID, notification)
    }

    private fun broadcastProgress(downloadId: Int, progress: Int, url: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_INDETERMINATE, false)
            putExtra(EXTRA_URL, url)
        }
        sendBroadcast(intent)
    }

    private fun broadcastIndeterminateProgress(downloadId: Int) {
        val intent = Intent(BROADCAST_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(EXTRA_PROGRESS, -1)
            putExtra(EXTRA_INDETERMINATE, true)
        }
        sendBroadcast(intent)
    }

    private fun broadcastProgressWithBytes(downloadId: Int, bytesDownloaded: Long, url: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(EXTRA_PROGRESS, -1)
            putExtra(EXTRA_DOWNLOADED_BYTES, bytesDownloaded)
            putExtra(EXTRA_INDETERMINATE, true)
            putExtra(EXTRA_URL, url)
        }
        sendBroadcast(intent)
    }

    private fun broadcastComplete(downloadId: Int, filename: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_COMPLETE).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(EXTRA_FILENAME, filename)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(downloadId: Int, errorMessage: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_ERROR).apply {
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        sendBroadcast(intent)
    }
}