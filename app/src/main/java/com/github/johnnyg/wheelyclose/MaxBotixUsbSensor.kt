package com.github.johnnyg.wheelyclose

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.os.Handler
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

private const val BAUD_RATE = 57600
private const val MAX_READING_LEN = 4
private const val TAG = "MaxBotixSensor"
const val SUCCESSFUL_READING = 1
enum class DistanceUnit (val multiplier: Int) {
    Millimeter(1),
    Centimeter(10),
}

class MaxBotixUsbSensor(device: UsbDevice, connection: UsbDeviceConnection, private val handler: Handler) {

    private var lastReadDistance: Int? = null
    private var unit: DistanceUnit = DistanceUnit.Millimeter
    private val serial = UsbSerialDevice.createUsbSerialDevice(device, connection)?.apply {
        open()
        setBaudRate(BAUD_RATE)
        setDataBits(UsbSerialInterface.DATA_BITS_8)
        setStopBits(UsbSerialInterface.STOP_BITS_1)
        setParity(UsbSerialInterface.PARITY_NONE)
        setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
    }

    private val callback = object : UsbSerialInterface.UsbReadCallback {

        private val sb = StringBuilder(MAX_READING_LEN)

        override fun onReceivedData(bytes: ByteArray) {
            val string = String(bytes)
            var complete = false
            var range = 0 until string.length

            Log.v(TAG, "Read from sensor '$string'")

            if (string.startsWith('R')) {
                sb.setLength(0)
                range = 1..range.endInclusive
                Log.v(TAG, "Detected start of reading")
            }

            if (string.endsWith('\r')) {
                complete = true
                range = range.start until range.endInclusive
                Log.v(TAG, "Detected end of reading")
            }

            sb.append(string.substring(range))

            if (complete) {
                val reading = sb.toString()
                Log.v(TAG, "Complete reading: $reading")
                if (reading.isNotEmpty()) {
                    onNewReading(reading.toInt())
                }
            }
        }
    }

    fun setUnit(unit: DistanceUnit) {
        this.unit = unit
    }

    fun start() {
        serial?.read(callback)
    }

    fun stop() {
        serial?.close()
    }

    private fun onNewReading(reading: Int) {
        val distance = reading / unit.multiplier
        if (lastReadDistance == null || distance != lastReadDistance) {
            handler.obtainMessage(SUCCESSFUL_READING, distance, 0)?.apply {
                sendToTarget()
            }
            lastReadDistance = distance
        }
    }
}
