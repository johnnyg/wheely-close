package com.github.johnnyg.wheelyclose

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

private const val BAUD_RATE = 57600
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val reading = findViewById<TextView>(R.id.reading)
        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

        if (device != null) {
            val manager = getSystemService(Context.USB_SERVICE) as UsbManager
            val connection = manager.openDevice(device)
            UsbSerialDevice.createUsbSerialDevice(device, connection)?.apply {
                open()
                setBaudRate(BAUD_RATE)
                setDataBits(UsbSerialInterface.DATA_BITS_8)
                setStopBits(UsbSerialInterface.STOP_BITS_1)
                setParity(UsbSerialInterface.PARITY_NONE)
                setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
            }?.read { line ->
                val string = String(line)
                Log.v(TAG, "Got reading " + string)
                runOnUiThread {
                    reading.text = string
                }
            }
        } else {
            reading.text = "No device attached"
        }
    }
}
