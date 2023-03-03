package se.hellsoft.foregroundservices.peripherals

import android.annotation.SuppressLint
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScanReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: $intent")
        val results: List<ScanResult>? = intent?.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
        results?.forEach {
            Log.d(TAG, "onReceive result: ${it.device.name}")
        }
        if (context != null) {
            BackgroundScanPeripheralService.start(context)
        }
    }

    companion object {
        private const val TAG = "ScanReceiver"
    }
}