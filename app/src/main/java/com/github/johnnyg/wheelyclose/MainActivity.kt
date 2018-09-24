package com.github.johnnyg.wheelyclose

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.support.v4.app.NotificationManagerCompat
import java.util.*


private const val ACTION_USB_PERMISSION = "com.github.johnnyg.wheelyclose.USB_PERMISSION"
private const val TAG = "MainActivity"
private const val MIN_SAFE_DISTANCE = 90
private const val UNSAFE_DISTANCE_TEXT_COLOUR = Color.RED
private const val CHANNEL_ID = "com.github.johnnyg.wheelyclose.CHANNEL_ID"
private const val TEST_MODE = false

private fun getDevice(intent: Intent): UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

class MainActivity : AppCompatActivity() {

    private lateinit var manager: UsbManager
    private lateinit var permissionIntent: PendingIntent
    private lateinit var display: TextView
    private lateinit var handler: Handler
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var safeDistanceTextColour: Int = Color.BLACK
    private var sensor: DistanceSensor? = null
    private var notificationId = 0

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                Log.v(TAG, "Received USB permission intent")
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    getDevice(intent)?.also { device ->
                        sensor = createSensor(device)?.apply {
                            handler = handler
                            unit = DistanceUnit.Centimeter
                            start()
                        }
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
        display = findViewById(R.id.reading)
        safeDistanceTextColour = display.currentTextColor
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    SUCCESSFUL_READING -> handleNewReading(msg.arg1)
                    else -> super.handleMessage(msg)
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        createNotificationChannel()
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.notification_icon_background)
            setContentTitle(getString(R.string.notification_title))
            setAutoCancel(true)
            priority = NotificationCompat.PRIORITY_HIGH
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        sensor?.stop()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (!TEST_MODE) {
            getDevice(intent)?.also { device ->
                sensor = createSensor(device)
            }
            if (sensor == null) {
                Log.d(TAG, "No sensor, searching device list...")
                manager.deviceList.values.firstOrNull()?.also { device ->
                    if (manager.hasPermission(device)) {
                        sensor = createSensor(device)
                    } else {
                        Log.d(TAG, "Requesting permission for ${device.deviceName}")
                        manager.requestPermission(device, permissionIntent)
                    }
                }
            }
        } else {
            val distance = 100
            handler.obtainMessage(SUCCESSFUL_READING, distance, 0)?.apply {
                sendToTarget()
            }
        }
        sensor?.apply {
            handler = handler
            unit = DistanceUnit.Centimeter
            start()
        }
    }

    private fun createSensor(device: UsbDevice): DistanceSensor? {
        var sensor: DistanceSensor? = null;
        getConnection(device)?.also { connection ->
            Log.d(TAG, "Creating sensor for ${device.deviceName}")
            sensor = MaxBotixUsbSensor(device, connection)
        }
        return sensor
    }

    private fun getConnection(device: UsbDevice): UsbDeviceConnection? {
        var connection: UsbDeviceConnection? = null
        if (manager.hasPermission(device)) {
            connection = manager.openDevice(device)
        }
        return connection
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    @Synchronized
    private fun handleNewReading(distance: Int) {
        val reading = "$distance cm"
        val textColour = if (distance < MIN_SAFE_DISTANCE) UNSAFE_DISTANCE_TEXT_COLOUR else safeDistanceTextColour
        Log.d(TAG, "Got distance reading: $reading")
        display.text = reading
        if (display.currentTextColor != textColour) {
            display.setTextColor(textColour)
        }
        if (distance < 100) {
            notificationBuilder.apply {
                setContentText(reading)
                setWhen(Calendar.getInstance().timeInMillis)
            }.build().let { notification ->
                NotificationManagerCompat.from(this).run {
                    notify(notificationId, notification)
                }
            }
            notificationId++
        }
    }
}
