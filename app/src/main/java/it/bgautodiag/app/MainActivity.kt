
package it.bgautodiag.app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var devices: TextView
    private lateinit var scanButton: Button

    private var scanner: BluetoothLeScanner? = null
    private var scanning = false

    private val handler = Handler(Looper.getMainLooper())
    private val foundDevices = linkedMapOf<String, String>()

    private val stopScanTask = Runnable {
        stopScan()
        status.text = if (foundDevices.isEmpty()) {
            "Ricerca terminata. Nessun dispositivo BLE trovato."
        } else {
            "Ricerca terminata. Dispositivi trovati: ${foundDevices.size}"
        }
        updateDeviceList()
        scanButton.isEnabled = true
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            runOnUiThread {
                val address = result.device.address
                val name = try {
                    result.scanRecord?.deviceName
                        ?: result.device.name
                        ?: "Dispositivo BLE senza nome"
                } catch (_: SecurityException) {
                    "Dispositivo BLE"
                }

                foundDevices[address] = "$name\n$address"
                updateDeviceList()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                scanning = false
                handler.removeCallbacks(stopScanTask)
                status.text = "Scansione fallita. Codice errore: $errorCode"
                scanButton.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "BG AutoDiag"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Ricerca adattatori diagnostici Bluetooth LE"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "Premi il pulsante per cercare gli adattatori."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 16)
        }

        scanButton = Button(this).apply {
            text = "Cerca adattatori BLE"
            isEnabled = true
            setOnClickListener {
                checkPermissionsAndScan()
            }
        }

        devices = TextView(this).apply {
            text = "Nessun dispositivo trovato."
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(0, 20, 0, 0)
        }

        val scroll = ScrollView(this).apply {
            addView(devices)
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(status)
        layout.addView(scanButton)
        layout.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layout)
    }

    private fun checkPermissionsAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            val missing = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isNotEmpty()) {
                requestPermissions(missing.toTypedArray(), REQUEST_BLUETOOTH)
                return
            }
        }

        startScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_BLUETOOTH) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                startScan()
            } else {
                status.text =
                    "Permessi Bluetooth necessari per cercare gli adattatori."
            }
        }
    }

    private fun startScan() {
        try {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter: BluetoothAdapter? = manager?.adapter

            if (adapter == null) {
                status.text = "Questo telefono non supporta Bluetooth."
                return
            }

            if (!adapter.isEnabled) {
                status.text =
                    "Attiva il Bluetooth nelle impostazioni del telefono e riprova."
                return
            }

            scanner = adapter.bluetoothLeScanner

            if (scanner == null) {
                status.text = "Scansione BLE non disponibile."
                return
            }

            foundDevices.clear()
            updateDeviceList()
            scanning = true
            scanButton.isEnabled = false
            status.text = "Ricerca BLE in corso per 10 secondi..."

            scanner?.startScan(scanCallback)
            handler.removeCallbacks(stopScanTask)
            handler.postDelayed(stopScanTask, 10_000)

        } catch (_: SecurityException) {
            scanning = false
            scanButton.isEnabled = true
            status.text = "Permesso Bluetooth mancante. Controlla le autorizzazioni."
        } catch (_: Exception) {
            scanning = false
            scanButton.isEnabled = true
            status.text = "Impossibile avviare la scansione Bluetooth."
        }
    }

    private fun stopScan() {
        if (!scanning) return

        try {
            scanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {
            // Il permesso potrebbe essere stato revocato.
        } catch (_: Exception) {
            // La scansione potrebbe essere già terminata.
        }

        scanning = false
    }

    private fun updateDeviceList() {
        devices.text = if (foundDevices.isEmpty()) {
            "Nessun dispositivo trovato."
        } else {
            foundDevices.values.joinToString("\n\n")
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopScanTask)
        stopScan()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 1001
    }
}
