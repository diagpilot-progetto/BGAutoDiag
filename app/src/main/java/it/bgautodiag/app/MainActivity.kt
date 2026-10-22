package it.bgautodiag.app

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGattCharacteristic
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
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button

    private var scanner: BluetoothLeScanner? = null
    private var scanning = false
    private var gatt: BluetoothGatt? = null

    private val handler = Handler(Looper.getMainLooper())
    private val foundDevices = linkedMapOf<String, BluetoothDevice>()

    private val stopScanTask = Runnable {
        stopScan()
        status.text = if (foundDevices.isEmpty()) {
            "Ricerca terminata. Nessun dispositivo BLE trovato."
        } else {
            "Ricerca terminata. Dispositivi trovati: ${foundDevices.size}. Tocca un dispositivo per collegarti."
        }
        scanButton.isEnabled = true
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            try {
                val device = result.device
                val address = device.address
                foundDevices[address] = device

                runOnUiThread {
                    renderDevices()
                    status.text = "Dispositivo trovato: ${foundDevices.size}"
                }
            } catch (_: SecurityException) {
                runOnUiThread {
                    status.text = "Permesso Bluetooth mancante."
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            runOnUiThread {
                scanning = false
                handler.removeCallbacks(stopScanTask)
                status.text = "Scansione fallita. Codice: $errorCode"
                scanButton.isEnabled = true
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            bluetoothGatt: BluetoothGatt,
            statusCode: Int,
            newState: Int
        ) {
            runOnUiThread {
                if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                    status.text = "Errore connessione BLE: $statusCode"
                    disconnectButton.isEnabled = false
                    return@runOnUiThread
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        status.text = "Bluetooth connesso. Ricerca servizi..."
                        try {
                            bluetoothGatt.discoverServices()
                        } catch (_: SecurityException) {
                            status.text = "Permesso Bluetooth mancante."
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        status.text = "Dispositivo disconnesso."
                        disconnectButton.isEnabled = false
                    }
                }
            }
        }

        override fun onServicesDiscovered(
            bluetoothGatt: BluetoothGatt,
            statusCode: Int
        ) {
            runOnUiThread {
                if (statusCode != BluetoothGatt.GATT_SUCCESS) {
                    status.text = "Connesso, ma lettura servizi fallita: $statusCode"
                    return@runOnUiThread
                }

                val report = StringBuilder()
                report.append("Connessione BLE riuscita.\n\n")
                report.append("Servizi e caratteristiche:\n")

                for (service in bluetoothGatt.services) {
                    report.append("\nServizio:\n${service.uuid}\n")

                    for (characteristic in service.characteristics) {
                        val properties = characteristic.properties
                        val labels = mutableListOf<String>()

                        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                            labels.add("READ")
                        }
                        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                            labels.add("WRITE")
                        }
                        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                            labels.add("WRITE_NO_RESPONSE")
                        }
                        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            labels.add("NOTIFY")
                        }
                        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                            labels.add("INDICATE")
                        }

                        report.append("  ${characteristic.uuid}\n")
                        report.append("  ${labels.joinToString(", ")}\n")
                    }
                }

                if (bluetoothGatt.services.isEmpty()) {
                    report.append("\nNessun servizio trovato.")
                }

                status.text = report.toString()
                disconnectButton.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "BG AutoDiag"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "Connessione adattatori diagnostici BLE"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }

        status = TextView(this).apply {
            text = "Premi il pulsante per cercare gli adattatori."
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(0, 12, 0, 12)
        }

        scanButton = Button(this).apply {
            text = "Cerca adattatori BLE"
            setOnClickListener {
                checkPermissionsAndScan()
            }
        }

        disconnectButton = Button(this).apply {
            text = "Disconnetti"
            isEnabled = false
            setOnClickListener {
                closeConnection()
                status.text = "Disconnesso."
            }
        }

        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this).apply {
            addView(deviceList)
        }

        layout.addView(title)
        layout.addView(description)
        layout.addView(scanButton)
        layout.addView(disconnectButton)
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_BLUETOOTH) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                startScan()
            } else {
                status.text = "Permessi Bluetooth necessari."
            }
        }
    }

    private fun startScan() {
        try {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter

            if (adapter == null || !adapter.isEnabled) {
                status.text = "Attiva il Bluetooth e riprova."
                return
            }

            closeConnection()
            foundDevices.clear()
            renderDevices()

            scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                status.text = "Scansione BLE non disponibile."
                return
            }

            scanning = true
            scanButton.isEnabled = false
            status.text = "Ricerca BLE in corso per 10 secondi..."

            scanner?.startScan(scanCallback)
            handler.removeCallbacks(stopScanTask)
            handler.postDelayed(stopScanTask, 10_000)

        } catch (_: SecurityException) {
            scanning = false
            scanButton.isEnabled = true
            status.text = "Permesso Bluetooth mancante."
        } catch (_: Exception) {
            scanning = false
            scanButton.isEnabled = true
            status.text = "Impossibile avviare la scansione."
        }
    }

    private fun renderDevices() {
        deviceList.removeAllViews()

        for ((address, device) in foundDevices) {
            val row = TextView(this).apply {
                text = try {
                    val name = device.name ?: "Dispositivo BLE"
                    "$name\n$address\nTocca per connetterti"
                } catch (_: SecurityException) {
                    "Dispositivo BLE\n$address"
                }

                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(12, 18, 12, 18)
                setBackgroundColor(0xFFEFEFEF.toInt())
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    connectToDevice(device)
                }
            }

            deviceList.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            )
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        scanButton.isEnabled = true
        closeConnection()
        status.text = "Connessione al dispositivo in corso..."
        disconnectButton.isEnabled = false

        try {
            gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(
                    this,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                device.connectGatt(this, false, gattCallback)
            }

            if (gatt == null) {
                status.text = "Impossibile avviare la connessione BLE."
            }
        } catch (_: SecurityException) {
            status.text = "Permesso Bluetooth mancante."
        } catch (_: Exception) {
            status.text = "Errore durante la connessione BLE."
        }
    }

    private fun stopScan() {
        if (!scanning) return

        try {
            scanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {
            // Permesso non disponibile.
        } catch (_: Exception) {
            // Scansione già terminata.
        }

        scanning = false
        handler.removeCallbacks(stopScanTask)
    }

    private fun closeConnection() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: SecurityException) {
            // Permesso non disponibile.
        } catch (_: Exception) {
            // Connessione già chiusa.
        }

        gatt = null
        disconnectButton.isEnabled = false
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopScanTask)
        stopScan()
        closeConnection()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_BLUETOOTH = 1001
    }
}
