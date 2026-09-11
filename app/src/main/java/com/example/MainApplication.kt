package com.example

import android.app.Application
import com.google.android.material.color.DynamicColors

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Material You dynamic color support to all activities if available
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
