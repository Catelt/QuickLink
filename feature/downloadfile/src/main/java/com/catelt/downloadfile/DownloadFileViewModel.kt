package com.catelt.downloadfile

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DownloadFileViewModel : ViewModel() {
    private val _downloadState = MutableStateFlow<DownloadFileState>(DownloadFileState.Idle)
    val downloadState: StateFlow<DownloadFileState> = _downloadState

    private val _urlText = MutableStateFlow("")
    val urlText: StateFlow<String> = _urlText

    private val _filenameText = MutableStateFlow("")
    val filenameText: StateFlow<String> = _filenameText

    fun setUrlText(value: String) {
        if (_urlText.value == value) return
        _urlText.value = value

        if (value.isNotEmpty()) {
            val extractedName = extractFilenameFromUrl(value)
            _filenameText.value = extractedName
        }
    }

    fun setFilenameText(value: String) {
        _filenameText.value = value
    }

    private var workId: UUID? = null

    fun recoverExistingDownloads(context: Context) {
        val workManager = WorkManager.getInstance(context)

        viewModelScope.launch {
            try {
                // Get all work with our download tag
                val workInfos = workManager.getWorkInfosByTag(DownloadWorker.WORK_TAG).get()

                // Filter for running or enqueued work
                val activeWorkIds = workInfos
                    .filter { workInfo ->
                        workInfo.state == WorkInfo.State.RUNNING ||
                                workInfo.state == WorkInfo.State.ENQUEUED
                    }
                    .map { it.id }
                    .toSet()

                if (activeWorkIds.isNotEmpty()) {
                    activeWorkIds.last().let { id ->
                        workId = id
                        DownloadWorker.observeWorkProgress(context, id).observeForever(::handleObserveDownloadWorker)
                    }
                }
            } catch (e: Exception) {
                // Handle error in recovery
                e.printStackTrace()
            }
        }
    }

    fun startDownload(context: Context, urlString: String, filename: String) {
        val workId = DownloadWorker.enqueueDownload(context, urlString, filename)
        this.workId = workId

        _downloadState.value = DownloadFileState.Downloading(0)

        DownloadWorker.observeWorkProgress(context, workId).observeForever(::handleObserveDownloadWorker)
    }

    private fun handleObserveDownloadWorker(workInfo: WorkInfo?) {
        workInfo ?: return
        when(workInfo.state) {
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                val isIndeterminate = workInfo.progress.getBoolean(DownloadWorker.KEY_IS_INDETERMINATE, false)
                val downloadedBytes = workInfo.progress.getLong(DownloadWorker.KEY_DOWNLOADED_BYTES, 0)
                val url = workInfo.progress.getString(DownloadWorker.KEY_URL) ?: ""
                if (url.isNotBlank()) {
                    setUrlText(url)
                }

                if (isIndeterminate) {
                    _downloadState.value = DownloadFileState.Downloading(
                        process = -1,
                        isIndeterminate = true,
                        downloadedBytes = downloadedBytes
                    )
                } else {
                    _downloadState.value = DownloadFileState.Downloading(
                        process = progress,
                        isIndeterminate = false
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                _downloadState.value = DownloadFileState.Completed
            }
            WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString(DownloadWorker.KEY_RESULT_ERROR) ?: "Unknown Error"
                _downloadState.value = DownloadFileState.Error(error)
                workId = null
            }
            WorkInfo.State.CANCELLED -> {
                _downloadState.value = DownloadFileState.Error("Cancelled")
                workId = null
            }
            else -> {}
        }
    }

    fun cancelDownload(context: Context) {
        workId?.let {
            DownloadWorker.cancelDownload(context, it)
            resetDownload(false)
        }
    }

    fun resetDownload(forceReset: Boolean) {
        _downloadState.value = DownloadFileState.Idle
        if (forceReset) {
            _urlText.value = ""
            _filenameText.value = ""
        }
    }

    // Register broadcast receiver to listen for download updates
//    private fun registerReceiver(context: Context) {
//        if (isReceiverRegistered) return
//
//        val filter = IntentFilter().apply {
//            addAction(DownloadService.BROADCAST_DOWNLOAD_PROGRESS)
//            addAction(DownloadService.BROADCAST_DOWNLOAD_COMPLETE)
//            addAction(DownloadService.BROADCAST_DOWNLOAD_ERROR)
//        }
//
//        receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                val downloadId = intent.getIntExtra(DownloadService.EXTRA_DOWNLOAD_ID, -1)
//
//                when (intent.action) {
//                    DownloadService.BROADCAST_DOWNLOAD_PROGRESS -> {
//                        val progress = intent.getIntExtra(DownloadService.EXTRA_PROGRESS, 0)
//                        val isIndeterminate =
//                            intent.getBooleanExtra(DownloadService.EXTRA_INDETERMINATE, false)
//                        val downloadedBytes =
//                            intent.getLongExtra(DownloadService.EXTRA_DOWNLOADED_BYTES, 0L)
//                        intent.getStringExtra(DownloadService.EXTRA_URL)?.let { url ->
//                            if (url.isNotEmpty()) {
//                                setUrlText(url)
//                            }
//                        }
//
//                        currentDownloadId = downloadId
//
//                        if (isIndeterminate) {
//                            _downloadState.value = DownloadFileState.Downloading(
//                                process = -1,
//                                isIndeterminate = true,
//                                downloadedBytes = downloadedBytes
//                            )
//                        } else {
//                            _downloadState.value = DownloadFileState.Downloading(
//                                process = progress,
//                                isIndeterminate = false
//                            )
//                        }
//                    }
//
//                    DownloadService.BROADCAST_DOWNLOAD_COMPLETE -> {
//                        if (downloadId == currentDownloadId) {
//                            _downloadState.value = DownloadFileState.Completed
//                            currentDownloadId = null
//                        }
//                    }
//
//                    DownloadService.BROADCAST_DOWNLOAD_ERROR -> {
//                        if (downloadId == currentDownloadId) {
//                            val errorMessage =
//                                intent.getStringExtra(DownloadService.EXTRA_ERROR_MESSAGE)
//                                    ?: "Unknown error"
//                            _downloadState.value = DownloadFileState.Error(errorMessage)
//                            currentDownloadId = null
//                        }
//                    }
//                }
//            }
//        }
//
//        ContextCompat.registerReceiver(
//            context,
//            receiver,
//            filter,
//            ContextCompat.RECEIVER_NOT_EXPORTED
//        )
//        isReceiverRegistered = true
//    }

    fun unregisterReceiver(context: Context) {
        workId?.let {
            DownloadWorker.observeWorkProgress(context, it).removeObserver(::handleObserveDownloadWorker)
        }
    }

    private fun extractFilenameFromUrl(url: String): String {
        return try {
            val uri = url.toUri()
            val path = uri.path ?: ""
            val lastSlashIndex = path.lastIndexOf('/')
            if (lastSlashIndex != -1 && lastSlashIndex < path.length - 1) {
                path.substring(lastSlashIndex + 1)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        fun createFactory(): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    DownloadFileViewModel()
                }
            }
        }
    }
}