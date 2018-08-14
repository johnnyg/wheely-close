package com.github.johnnyg.wheelyclose

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import kotlin.concurrent.thread

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var manager: UsbManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
    }

    override fun onStart() {
        super.onStart()
        val reading = findViewById<TextView>(R.id.reading)
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

    private val device get() = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) ?: manager?.deviceList?.values?.firstOrNull()

    private fun getConnection(device: UsbDevice): UsbDeviceConnection? {
        return manager?.openDevice(device)
    }
}
