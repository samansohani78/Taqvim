/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import ir.taqvim.core.model.Coordinates
import kotlin.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/** How much the magnetic readings can be trusted; LOW and UNRELIABLE ask for a figure-eight calibration. */
enum class CompassAccuracy {
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
}

/** A device orientation reading. */
sealed interface OrientationSample {
    data class Reading(
        val rotation: RotationMatrix,
        val accuracy: CompassAccuracy,
    ) : OrientationSample

    /** The device has neither a rotation-vector sensor nor an accelerometer with a magnetometer. */
    data object Unavailable : OrientationSample
}

/** A gravity reading in device coordinates (pointing up at rest). */
sealed interface GravitySample {
    data class Reading(
        val gravity: Vector3,
    ) : GravitySample

    /** The device has neither a gravity sensor nor an accelerometer. */
    data object Unavailable : GravitySample
}

/** Motion sensors behind a small interface so the compass and level logic can be tested with fakes. */
interface MotionSensors {
    fun orientation(): Flow<OrientationSample>

    fun gravity(): Flow<GravitySample>
}

/** Magnetic declination (east positive) at a place and time; true bearing = magnetic bearing + declination. */
fun interface DeclinationModel {
    fun declinationDegrees(
        place: Coordinates,
        instant: Instant,
    ): Double
}

/** The platform's World Magnetic Model implementation (`android.hardware.GeomagneticField`). */
object PlatformDeclination : DeclinationModel {
    override fun declinationDegrees(
        place: Coordinates,
        instant: Instant,
    ): Double =
        GeomagneticField(
            place.latitude.toFloat(),
            place.longitude.toFloat(),
            place.elevationMeters.toFloat(),
            instant.toEpochMilliseconds(),
        ).declination.toDouble()
}

/**
 * [MotionSensors] on `SensorManager`: the rotation-vector sensor when present, else accelerometer + magnetometer;
 * the gravity sensor when present, else the accelerometer (callers low-pass it).
 */
class PlatformMotionSensors(
    private val sensorManager: SensorManager,
) : MotionSensors {
    override fun orientation(): Flow<OrientationSample> {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        return when {
            rotationVector != null -> {
                listen(listOf(rotationVector), RotationVectorListener())
            }

            accelerometer != null && magnetometer != null -> {
                listen(listOf(accelerometer, magnetometer), GravityAndFieldListener())
            }

            else -> {
                flowOf(OrientationSample.Unavailable)
            }
        }
    }

    override fun gravity(): Flow<GravitySample> {
        val sensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                ?: return flowOf(GravitySample.Unavailable)
        return callbackFlow {
            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        trySend(GravitySample.Reading(event.vector()))
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor,
                        accuracy: Int,
                    ) = Unit
                }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensorManager.unregisterListener(listener) }
        }
    }

    private fun listen(
        sensors: List<Sensor>,
        interpreter: SampleInterpreter,
    ): Flow<OrientationSample> =
        callbackFlow {
            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        interpreter.onEvent(event.sensor.type, event.vector())?.let { trySend(it) }
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor,
                        accuracy: Int,
                    ) {
                        interpreter.onAccuracy(sensor.type, accuracy)
                    }
                }
            sensors.forEach { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            awaitClose { sensorManager.unregisterListener(listener) }
        }
}

/** Turns raw sensor events into orientation samples; holds the latest readings of one listener only. */
internal sealed class SampleInterpreter {
    protected var accuracy: CompassAccuracy = CompassAccuracy.HIGH

    abstract fun onEvent(
        sensorType: Int,
        values: Vector3,
    ): OrientationSample?

    open fun onAccuracy(
        sensorType: Int,
        status: Int,
    ) {
        accuracy = accuracyOf(status)
    }
}

internal class RotationVectorListener : SampleInterpreter() {
    override fun onEvent(
        sensorType: Int,
        values: Vector3,
    ): OrientationSample = OrientationSample.Reading(RotationMath.fromRotationVector(values), accuracy)
}

internal class GravityAndFieldListener : SampleInterpreter() {
    private var gravity: Vector3? = null
    private var field: Vector3? = null

    override fun onEvent(
        sensorType: Int,
        values: Vector3,
    ): OrientationSample? {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            gravity = values.lowPass(gravity, GRAVITY_ALPHA)
        } else {
            field = values
        }
        val rotation = gravity?.let { up -> field?.let { RotationMath.fromGravityAndField(up, it) } }
        return rotation?.let { OrientationSample.Reading(it, accuracy) }
    }

    override fun onAccuracy(
        sensorType: Int,
        status: Int,
    ) {
        if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) super.onAccuracy(sensorType, status)
    }

    private companion object {
        /** Separates gravity from linear acceleration in raw accelerometer readings. */
        const val GRAVITY_ALPHA = 0.2
    }
}

internal fun accuracyOf(status: Int): CompassAccuracy =
    when (status) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
        else -> CompassAccuracy.UNRELIABLE
    }

private fun SensorEvent.vector(): Vector3 = Vector3(values[0].toDouble(), values[1].toDouble(), values[2].toDouble())
