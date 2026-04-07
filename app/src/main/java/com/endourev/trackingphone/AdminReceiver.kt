package com.endourev.trackingphone

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val serviceIntent = Intent(context, CameraService::class.java)
        context.startForegroundService(serviceIntent)
    }
}