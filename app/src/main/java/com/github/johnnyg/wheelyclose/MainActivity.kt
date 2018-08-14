package com.github.johnnyg.wheelyclose

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import kotlin.concurrent.thread

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var manager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private lateinit var reading: TextView
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    updateReading()
                } else {
                    synchronized(this) {
                        reading.text = "Permission denied for device"
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        reading = findViewById<TextView>(R.id.reading)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    override fun onStart() {
        super.onStart()
        updateReading()
    }

    @Synchronized
    private fun updateReading(msg: String = "No device attached") {
        val device = this.device
        val connection = device?.let { dev ->
            getConnection(dev)
        }
        if (device != null && connection != null) {
            val sensor: DistanceSensor = MaxBotixUsbSensor(device, connection)
            reading.text = "Reading..."
            thread(start = true) {
                while (true) {
                    val distance = "${sensor.distance} mm"
                    Log.i(TAG, distance)
                    runOnUiThread {
                        reading.text = distance
                    }
                    Thread.sleep(500)
                }
            }
        } else {
            reading.text = "No device attached"
        }

    }

    private val device
        get() = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                ?: manager.deviceList.values.firstOrNull()

    private fun getConnection(device: UsbDevice): UsbDeviceConnection? {
        var connection: UsbDeviceConnection? = null
        if (manager.hasPermission(device)) {
            connection = manager.openDevice(device)
        } else {
            manager.requestPermission(device, permissionIntent)
        }
        return connection
    }
}
