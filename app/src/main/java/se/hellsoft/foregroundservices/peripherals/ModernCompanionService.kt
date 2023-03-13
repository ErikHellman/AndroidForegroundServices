package se.hellsoft.foregroundservices.peripherals

import android.app.Notification
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
class ModernCompanionService : CompanionDeviceService() {

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

    override fun onDeviceAppeared(address: String) {
        super.onDeviceAppeared(address)
        Log.d(TAG, "onDeviceAppeared: $address")
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val device = bluetoothManager.adapter.getRemoteDevice(address)
        start(applicationContext, device)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        Log.d(TAG, "onDeviceAppeared: $associationInfo")
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val address = associationInfo.deviceMacAddress!!.toOuiString()
        val device = bluetoothManager.adapter.getRemoteDevice(address)
        start(applicationContext, device)
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
            val intent = Intent(context, ModernCompanionService::class.java)?.also {
                it.action = ACTION_START
                it.putExtra(EXTRA_DEVICE, device)
            }
            context.startForegroundService(intent)
        }
    }
}
