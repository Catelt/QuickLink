package com.catelt.quicklink.utils

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DynamicModuleLoader {
    suspend fun loadModule(context: Context, moduleName: String): Result<Unit> {
        val manager = SplitInstallManagerFactory.create(context)
        if (manager.installedModules.contains(moduleName)) return Result.success(Unit)
        val request = SplitInstallRequest.newBuilder().addModule(moduleName).build()
        return suspendCancellableCoroutine { cont ->
            manager.startInstall(request)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }
}