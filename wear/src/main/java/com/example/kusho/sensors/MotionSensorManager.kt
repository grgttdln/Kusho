package com.example.kusho.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/**
 * Manages motion sensors (accelerometer + gyroscope) on Wear OS and exposes
 * a circular buffer of combined [SensorSample]s for inference.
 */
class MotionSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Latest readings
    private var lastAccel: FloatArray? = null
    private var lastGyro: FloatArray? = null

    // Circular buffer for recent samples
    private val bufferCapacity = 512
    private val samples = ArrayDeque<SensorSample>(bufferCapacity)
    private val mutex = Mutex()

    @Volatile
    private var isStarted: Boolean = false

    /** Start listening to motion sensors. */
    fun start() {
        if (isStarted) return
        isStarted = true

        val sm = sensorManager ?: return

        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // If either sensor is missing, stop early.
        if (accelerometer == null || gyroscope == null) {
            isStarted = false
            return
        }

        // Use SENSOR_DELAY_GAME for ~50-100 Hz sampling suitable for motion.
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sm.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Stop listening to motion sensors. */
    fun stop() {
        if (!isStarted) return
        isStarted = false
        sensorManager?.unregisterListener(this)
        lastAccel = null
        lastGyro = null
        synchronized(samples) {
            samples.clear()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isStarted) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccel = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyro = event.values.clone()
            }
            else -> return
        }

        val acc = lastAccel
        val gyr = lastGyro

        // Only emit sample when we have both accel and gyro readings.
        if (acc != null && gyr != null) {
            val sample = SensorSample(
                timestampNanos = event.timestamp,
                ax = acc[0],
                ay = acc[1],
                az = acc[2],
                gx = gyr[0],
                gy = gyr[1],
                gz = gyr[2]
            )

            synchronized(samples) {
                if (samples.size == bufferCapacity) {
                    samples.removeFirst()
                }
                samples.addLast(sample)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op but kept for interface completeness
    }

    /**
     * Returns up to [maxSamples] most recent [SensorSample]s.
     */
    suspend fun getRecentSamples(maxSamples: Int): List<SensorSample> {
        return mutex.withLock {
            val size = samples.size
            if (size <= maxSamples) {
                samples.toList()
            } else {
                // Manually take the last [maxSamples] elements since ArrayDeque doesn't support takeLast.
                val result = ArrayList<SensorSample>(maxSamples)
                val skip = size - maxSamples
                var index = 0
                for (sample in samples) {
                    if (index++ >= skip) {
                        result.add(sample)
                    }
                }
                result
            }
        }
    }
}
