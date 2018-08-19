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
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView

private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
private const val TAG = "MainActivity"

private fun getDevice(intent: Intent) : UsbDevice?  = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

class MainActivity : AppCompatActivity() {

    private lateinit var manager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private lateinit var display: TextView
    private lateinit var handler: Handler
    private var sensor: MaxBotixUsbSensor? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                Log.v(TAG, "Received USB permission intent")
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    getDevice(intent)?.also { device ->
                        createSensor(device)
                    }
                } else {
                    synchronized(this) {
                        display.text = "Permission denied for device"
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
        display = findViewById(R.id.reading)
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    SUCCESSFUL_READING -> {
                        val distance = msg.arg1
                        val reading = "$distance cm"
                        Log.d(TAG, "Got distance reading: $reading")
                        display.text = reading
                    }
                    else -> super.handleMessage(msg)
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        sensor?.stop()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        getDevice(intent)?.also { device ->
            createSensor(device)
        }
        if (sensor == null) {
            Log.d(TAG, "No sensor, searching device list...")
            manager.deviceList.values.firstOrNull()?.also { device ->
                if (manager.hasPermission(device)) {
                    createSensor(device)
                } else {
                    Log.d(TAG, "Requesting permission for ${device.deviceName}")
                    manager.requestPermission(device, permissionIntent)
                }
            }
        }
    }

    private fun createSensor(device: UsbDevice) {
        getConnection(device)?.also { connection ->
            Log.d(TAG, "Creating sensor for ${device.deviceName}")
            sensor = MaxBotixUsbSensor(device, connection, handler)?.apply {
                start()
            }
        }
    }

    private fun getConnection(device: UsbDevice): UsbDeviceConnection? {
        var connection: UsbDeviceConnection? = null
        if (manager.hasPermission(device)) {
            connection = manager.openDevice(device)
        }
        return connection
    }
}
