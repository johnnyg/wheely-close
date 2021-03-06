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

class MaxBotixUsbSensor(device: UsbDevice, connection: UsbDeviceConnection): DistanceSensor {

    override var handler: Handler? = null
    override var unit: DistanceUnit = DistanceUnit.Millimeter
    private var lastReadDistance: Int? = null
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

    override fun start() {
        serial?.read(callback)
    }

    override fun stop() {
        serial?.close()
    }

    private fun onNewReading(reading: Int) {
        val distance = reading / unit.multiplier
        if (lastReadDistance == null || distance != lastReadDistance) {
            handler?.obtainMessage(SUCCESSFUL_READING, distance, 0)?.apply {
                sendToTarget()
            }
            lastReadDistance = distance
        }
    }
}
