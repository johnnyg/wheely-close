package com.github.johnnyg.wheelyclose

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val reading = findViewById<TextView>(R.id.reading)
        val device = getUsbDevice()

        if (device != null) {
            val connection = getConnection(device)
            val sensor: DistanceSensor = MatbotixUsbSensor(device, connection)
            reading.text = "working"
            while (true) {
                Log.i(TAG, "${sensor.distanceInMm} mm")
                Thread.sleep(1000)
            }
        } else {
            reading.text = "No device attached"
        }
    }

    fun getUsbDevice(): UsbDevice? {
        var device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        if (device == null) {
            // TODO
        }
        return device
    }

    fun getConnection(device: UsbDevice): UsbDeviceConnection {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        return manager.openDevice(device)
    }
}
