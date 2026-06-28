// FILE: ApplicationClass.kt
// PURPOSE: Initialize OneSignal when your app launches
// REQUIREMENT: Must be registered in AndroidManifest.xml

package com.roozbehzarei.webview // Replace with your actual package name

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import android.content.Intent
import org.json.JSONObject

class ApplicationClass : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable verbose logging to debug issues (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Replace with your 36-character App ID from Dashboard > Settings > Keys & IDs
        OneSignal.initWithContext(this, "fa35403c-dda5-465c-a5b0-7a102ad0a9a0")

        OneSignal.Notifications.addClickListener { event ->

            val url = event.notification.additionalData
                ?.optString("url")

            val intent = Intent(this, MainActivity::class.java)

            intent.putExtra("url", url)

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
        }

        // Prompt user for push notification permission
        // In production, consider using an in-app message instead for better opt-in rates
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }
}
