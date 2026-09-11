package com.universallive.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.universallive.app.push.NotificationChannels
import com.universallive.app.push.PushTokenStore

class UniversalLiveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)

        // Firebase is initialized by the Google Services Gradle plugin from
        // androidApp/google-services.json. No duplicate API keys or sender IDs
        // are embedded in BuildConfig.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNotBlank()) PushTokenStore.save(this, token)
                }
        }
    }
}
