package com.github.johnnyg.wheelyclose

import android.os.Handler

const val SUCCESSFUL_READING = 1
enum class DistanceUnit (val multiplier: Int) {
    Millimeter(1),
    Centimeter(10),
}

interface DistanceSensor {
    fun start()
    fun stop()
    fun setHandler(handler: Handler)
    fun setUnit(unit: DistanceUnit)
}
