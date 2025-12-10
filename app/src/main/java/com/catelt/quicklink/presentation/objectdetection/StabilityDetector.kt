package com.catelt.quicklink.presentation.objectdetection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Simple accelerometer-based stability detector.
 * Calls [onStabilityChanged] with true when motion stays under the threshold,
 * false when movement spikes above the threshold.
 */
class StabilityDetector(
    context: Context,
    private val onStabilityChanged: (Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Lower threshold = more sensitive to small movements.
    private val motionThreshold = 0.5f

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var initialized = false

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        initialized = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!initialized) {
            lastX = x
            lastY = y
            lastZ = z
            initialized = true
            return
        }

        val deltaX = kotlin.math.abs(lastX - x)
        val deltaY = kotlin.math.abs(lastY - y)
        val deltaZ = kotlin.math.abs(lastZ - z)

        // If movement > threshold, we are UNSTABLE
        if (deltaX > motionThreshold || deltaY > motionThreshold || deltaZ > motionThreshold) {
            onStabilityChanged(false)
        } else {
            onStabilityChanged(true)
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // no-op
    }
}

