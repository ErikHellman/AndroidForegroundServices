package se.hellsoft.foregroundservices.peripherals

import android.app.Notification
import android.app.NotificationManager
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

@RequiresApi(Build.VERSION_CODES.S)
class SampleCompanionService : CompanionDeviceService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(applicationContext)
        Log.d(TAG, "onCreate: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand: $flags $startId")

        val notification = createNotification(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDeviceAppeared(address: String) {
        super.onDeviceAppeared(address)
        Log.d(TAG, "onDeviceAppeared: $address")
        start(applicationContext)
    }

    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        Log.d(TAG, "onDeviceAppeared: $associationInfo")
        start(applicationContext)
    }

    override fun onDeviceDisappeared(address: String) {
        super.onDeviceDisappeared(address)
        Log.d(TAG, "onDeviceDisappeared: $address")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        super.onDeviceDisappeared(associationInfo)
        Log.d(TAG, "onDeviceDisappeared: $associationInfo")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val TAG = "SampleCompanionService"
        private const val NOTIFICATION_ID = 1234
        private const val NOTIFICATION_CHANNEL_ID = "peripheral_device_service"
        private const val CHANNEL_NAME = "Peripheral Device"
        private const val CHANNEL_DESCRIPTION =
            "Notifications related to a external peripheral device."
        private const val DEFAULT_NOTIFICATION_TITLE = "Peripheral Device"
        private const val DEFAULT_NOTIFICATION_DESCRIPTION =
            "This service tracks the connection to your peripheral device"

        private fun createNotificationChannel(context: Context) {
            val channel = NotificationChannelCompat
                .Builder(NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
                .setName(CHANNEL_NAME)
                .setDescription(CHANNEL_DESCRIPTION)
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }

        private fun createNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(DEFAULT_NOTIFICATION_TITLE)
                .setContentInfo(DEFAULT_NOTIFICATION_DESCRIPTION)
                .build()
        }

        fun start(context: Context) {
            context.startForegroundService(Intent(context, SampleCompanionService::class.java))
        }
    }
}
