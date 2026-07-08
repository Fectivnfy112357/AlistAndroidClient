package com.textvision.alistclient.transfer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferNotificationController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(PROGRESS_CHANNEL, "传输进度", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(RESULT_CHANNEL, "传输结果", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun showProgressSummary(activeCount: Int, percent: Int?) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val text = if (percent == null) "正在传输 $activeCount 个文件" else "正在传输 $activeCount 个文件（总进度 $percent%）"
        val notification = NotificationCompat.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Alist 传输")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(PROGRESS_ID, notification)
    }

    fun clearProgress() {
        NotificationManagerCompat.from(context).cancel(PROGRESS_ID)
    }

    companion object {
        const val PROGRESS_CHANNEL = "transfer_progress_channel"
        const val RESULT_CHANNEL = "transfer_result_channel"
        private const val PROGRESS_ID = 1001
    }
}
