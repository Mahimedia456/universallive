package com.universallive.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val UPDATES = "universallive_updates"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            UPDATES,
            "Universal Live updates",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Broadcast, account, billing and Universal Live service updates"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
