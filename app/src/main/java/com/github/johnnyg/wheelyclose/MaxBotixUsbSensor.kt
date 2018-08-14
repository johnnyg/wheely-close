package com.github.johnnyg.wheelyclose

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

private const val BAUD_RATE = 57600
private const val MAX_READING_LEN = 4
private const val TAG = "MaxBotixSensor"

class MaxBotixUsbSensor(device: UsbDevice, connection: UsbDeviceConnection) : DistanceSensor, UsbSerialInterface.UsbReadCallback {

    private val sb = StringBuilder(MAX_READING_LEN)
    private var _distance = 0;

    init {
        UsbSerialDevice.createUsbSerialDevice(device, connection)?.apply {
            open()
            setBaudRate(BAUD_RATE)
            setDataBits(UsbSerialInterface.DATA_BITS_8)
            setStopBits(UsbSerialInterface.STOP_BITS_1)
            setParity(UsbSerialInterface.PARITY_NONE)
            setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
        }?.read(this)
    }

    @Synchronized
    override fun onReceivedData(bytes: ByteArray) {
        val string = String(bytes)
        var complete = false
        var range = 0 until string.length

        Log.v(TAG, "Read from sensor '" + string + "'")

        if (string.startsWith('R')) {
            sb.setLength(0)
            range = 1..range.endInclusive
            Log.d(TAG, "Detected start of reading")
        }

        if (string.endsWith('\r')) {
            complete = true
            range = range.start until range.endInclusive
            Log.d(TAG, "Detected end of reading")
        }

        sb.append(string.substring(range))

        if (complete) {
            _distance = sb.toString().toInt()
        }
    }

    override val distance: Int
        @Synchronized get() {
            return _distance
        }
}
