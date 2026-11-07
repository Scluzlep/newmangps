package com.scluzlep.newmangps

import android.app.Application
import com.scluzlep.newmangps.data.push.AlarmNotificationManager

class NewmanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels on startup
        AlarmNotificationManager.createChannels(this)
    }
}

