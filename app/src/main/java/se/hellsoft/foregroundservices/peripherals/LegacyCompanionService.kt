package se.hellsoft.foregroundservices.peripherals

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class LegacyCompanionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

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

        intent?.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)?.let {
            // TODO Connect to the device
            Log.d(TAG, "onStartCommand: Connect to ${it.address}")
        }

        return START_REDELIVER_INTENT
    }

    companion object {
        private const val TAG = "SampleLegacyCompanionService"
        private const val NOTIFICATION_ID = 1234
        private const val NOTIFICATION_CHANNEL_ID = "peripheral_device_service"
        private const val CHANNEL_NAME = "Peripheral Device"
        private const val CHANNEL_DESCRIPTION =
            "Notifications related to a external peripheral device."
        private const val DEFAULT_NOTIFICATION_TITLE = "Peripheral Device"
        private const val DEFAULT_NOTIFICATION_DESCRIPTION =
            "This service tracks the connection to your peripheral device"
        private const val ACTION_START = "se.hellsoft.foregroundservices.peripherals.START_SERVICE"
        private const val EXTRA_DEVICE = "device"

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

        fun start(context: Context, device: BluetoothDevice) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.FOREGROUND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                context.startForegroundService(
                    Intent(
                        context,
                        LegacyCompanionService::class.java
                    ).also {
                        it.action = ACTION_START
                        it.putExtra(EXTRA_DEVICE, device)
                    })
            } else {
                Log.d(TAG, "start: Not allow to start foreground service!")
            }
        }
    }
}