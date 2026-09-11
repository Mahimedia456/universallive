package com.universallive.app.push

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.universallive.app.BuildConfig
import com.universallive.app.MainActivity
import com.universallive.app.R
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class UniversalLiveMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenStore.save(this, token)
        syncTokenIfSignedIn(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationChannels.ensure(this)

        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Universal Live"
        val body = data["body"] ?: message.notification?.body ?: "You have a new update."
        val route = data["route"] ?: "notifications"
        val notificationId = data["notificationId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_PUSH_ROUTE, route)
            putExtra(MainActivity.EXTRA_PUSH_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId?.hashCode() ?: (System.currentTimeMillis() and 0x7fffffff).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.UPDATES)
            .setSmallIcon(R.drawable.ic_stat_universal_live)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(
                notificationId?.hashCode() ?: (System.currentTimeMillis() and 0x7fffffff).toInt(),
                notification,
            )
        }
    }

    private fun syncTokenIfSignedIn(pushToken: String) {
        val sessionPrefs = getSharedPreferences("universallive_session", MODE_PRIVATE)
        val accessToken = sessionPrefs.getString("access_token", null)?.takeIf { it.isNotBlank() } ?: return
        thread(name = "ul-push-token-sync", isDaemon = true) {
            runCatching {
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                    .orEmpty().ifBlank { "android-unknown" }
                val connection = URL("${BuildConfig.UNIVERSALLIVE_API_BASE_URL.trimEnd('/')}/devices/register")
                    .openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 12000
                    connection.readTimeout = 12000
                    connection.doOutput = true
                    connection.setRequestProperty("Authorization", "Bearer $accessToken")
                    connection.setRequestProperty("Content-Type", "application/json")
                    val payload = JSONObject()
                        .put("deviceId", deviceId)
                        .put("platform", "android")
                        .put("pushToken", pushToken)
                        .put("osVersion", Build.VERSION.RELEASE)
                        .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                        .toString()
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}
