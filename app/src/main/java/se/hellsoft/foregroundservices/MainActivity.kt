package se.hellsoft.foregroundservices

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.companion.*
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import se.hellsoft.foregroundservices.peripherals.BackgroundScanPeripheralService
import se.hellsoft.foregroundservices.peripherals.ScanReceiver
import se.hellsoft.foregroundservices.ui.theme.ForegroundServiceSampleTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ForegroundServiceSampleTheme {
                val contract = ActivityResultContracts.StartIntentSenderForResult()
                launcher = rememberLauncherForActivityResult(contract) {
                    if (it.resultCode == RESULT_OK) {
                        startBackgroundScanning()
                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { startBackgroundScanning() }) {
                            Text(text = "Peripheral Device Service")
                        }
                        Button(onClick = { makeCdmAssociation() }) {
                            Text(text = "CDM Association")
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            startBackgroundScanning()
        }
    }

    fun makeCdmAssociation() {
        val address = "18:04:ED:BE:B6:0C"
        val cdm = getSystemService(CompanionDeviceManager::class.java)
        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress(address)
//            .setServiceUuid(ParcelUuid(uuid))
//            .setDeviceName("Multi-Sensor")
            .build()

        val associationRequest = AssociationRequest.Builder()
            .addDeviceFilter(
                BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(scanFilter)
                    .build()
            )
            .build()
        val cdmCallback = object : CompanionDeviceManager.Callback() {
            override fun onDeviceFound(intentSender: IntentSender) {
                super.onDeviceFound(intentSender)
            }

            override fun onAssociationPending(intentSender: IntentSender) {
                super.onAssociationPending(intentSender)
                launcher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                super.onAssociationCreated(associationInfo)
            }

            override fun onFailure(error: CharSequence?) {

            }
        }
        cdm.associate(associationRequest, cdmCallback, null)
    }

    @SuppressLint("InlinedApi")
    private fun startBackgroundScanning() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
            return
        }

        val address = "18:04:ED:BE:B6:0C"
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager.adapter
//        val uuid = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
        val uuid = UUID.fromString("f0001110-0451-b000-8000-000000000000")
        val scanFilter = ScanFilter.Builder()
            .setDeviceAddress(address)
//            .setServiceUuid(ParcelUuid(uuid))
//            .setDeviceName("Multi-Sensor")
            .build()
        Log.d(TAG, "startBackgroundScanning: Start background scanning for $adapter")

        // For testing the callback version of the scan
        val callback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                Log.d(TAG, "onScanResult: ${result?.device?.name}")
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Log.d(TAG, "onBatchScanResults: ${results?.joinToString { it.device.name }}")
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e(TAG, "onScanFailed: $errorCode")
            }
        }

        val broadcastIntent = PendingIntent.getBroadcast(
            applicationContext,
            2002,
            Intent(applicationContext, ScanReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val foregroundServiceIntent = PendingIntent.getForegroundService(
            applicationContext,
            2002,
            Intent(applicationContext, ScanReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        adapter.bluetoothLeScanner.flushPendingScanResults(callback)
        adapter.bluetoothLeScanner.stopScan(broadcastIntent)
        adapter.bluetoothLeScanner.startScan(
            listOf(scanFilter),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                .setLegacy(true)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .build(),
            broadcastIntent
        )
        BackgroundScanPeripheralService.start(applicationContext)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
