/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import ir.taqvim.core.model.Coordinates
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.SensorBuilder
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensorManager

/** T-1302/T-1303 sensor injection through Robolectric's `ShadowSensorManager`, and the platform magnetic model. */
@RunWith(AndroidJUnit4::class)
class PlatformSensorsTest {
    private val sensorManager =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(SensorManager::class.java)
    private val shadow = shadowOf(sensorManager)
    private val sensors = PlatformMotionSensors(sensorManager)

    @After
    fun tearDown() {
        ShadowSensorManager.reset()
    }

    private fun add(type: Int): Sensor =
        SensorBuilder
            .newBuilder()
            .setType(type)
            .build()
            .also { shadow.addSensor(it) }

    private fun send(
        sensor: Sensor,
        vararg values: Float,
        accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    ) {
        shadow.sendSensorEventToListeners(
            SensorEventBuilder.newBuilder(sensor, values).setAccuracy(accuracy).build(),
        )
    }

    private fun OrientationSample.azimuth(): Double =
        RotationMath.angles((this as OrientationSample.Reading).rotation).azimuthDegrees

    @Test
    fun rotationVectorIsPreferredAndUnregisteredWhenCollectionStops(): Unit =
        runTest {
            val rotationVector = add(Sensor.TYPE_ROTATION_VECTOR)
            add(Sensor.TYPE_ACCELEROMETER)
            add(Sensor.TYPE_MAGNETIC_FIELD)
            sensors.orientation().test {
                // A quarter turn counterclockwise about z: the top edge points west.
                send(rotationVector, 0f, 0f, 0.70710677f)
                val sample = awaitItem()
                assertEquals(270.0, sample.azimuth(), 1e-3)
                assertEquals(CompassAccuracy.HIGH, (sample as OrientationSample.Reading).accuracy)
                shadow.listeners.single().onAccuracyChanged(rotationVector, SensorManager.SENSOR_STATUS_ACCURACY_LOW)
                send(rotationVector, 0f, 0f, 0f)
                val low = awaitItem() as OrientationSample.Reading
                assertEquals(CompassAccuracy.LOW, low.accuracy)
                assertEquals(0.0, low.let { RotationMath.angles(it.rotation).azimuthDegrees }, 1e-6)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(shadow.listeners.isEmpty())
        }

    @Test
    fun accelerometerAndMagnetometerAreTheFallback(): Unit =
        runTest {
            val accelerometer = add(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = add(Sensor.TYPE_MAGNETIC_FIELD)
            sensors.orientation().test {
                send(magnetometer, -20f, 0f, -40f)
                expectNoEvents()
                send(accelerometer, 0f, 0f, 9.81f)
                assertEquals(90.0, awaitItem().azimuth(), 1e-3)
                shadow.listeners.single().onAccuracyChanged(accelerometer, SensorManager.SENSOR_STATUS_UNRELIABLE)
                shadow.listeners.single().onAccuracyChanged(magnetometer, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
                send(magnetometer, -20f, 0f, -40f)
                assertEquals(CompassAccuracy.MEDIUM, (awaitItem() as OrientationSample.Reading).accuracy)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun missingSensorsAreReported(): Unit =
        runTest {
            sensors.orientation().test {
                assertEquals(OrientationSample.Unavailable, awaitItem())
                awaitComplete()
            }
            sensors.gravity().test {
                assertEquals(GravitySample.Unavailable, awaitItem())
                awaitComplete()
            }
            add(Sensor.TYPE_ACCELEROMETER)
            sensors.orientation().test {
                assertEquals(OrientationSample.Unavailable, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun gravityComesFromTheGravitySensorOrTheAccelerometer(): Unit =
        runTest {
            val accelerometer = add(Sensor.TYPE_ACCELEROMETER)
            sensors.gravity().test {
                send(accelerometer, 0.5f, 9.7f, 0.1f)
                assertEquals(GravitySample.Reading(Vector3(0.5, 9.699999809265137, 0.10000000149011612)), awaitItem())
                shadow.listeners.single().onAccuracyChanged(accelerometer, SensorManager.SENSOR_STATUS_ACCURACY_LOW)
                cancelAndIgnoreRemainingEvents()
            }
            val gravity = add(Sensor.TYPE_GRAVITY)
            sensors.gravity().test {
                send(gravity, 0f, 0f, 9.81f)
                val reading = awaitItem() as GravitySample.Reading
                assertEquals(DeviceOrientation.FLAT, LevelMath.classify(reading.gravity))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun accuracyStatuses() {
        assertEquals(CompassAccuracy.HIGH, accuracyOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertEquals(CompassAccuracy.MEDIUM, accuracyOf(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
        assertEquals(CompassAccuracy.LOW, accuracyOf(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
        assertEquals(CompassAccuracy.UNRELIABLE, accuracyOf(SensorManager.SENSOR_STATUS_UNRELIABLE))
        assertEquals(CompassAccuracy.UNRELIABLE, accuracyOf(SensorManager.SENSOR_STATUS_NO_CONTACT))
    }

    @Test
    fun theWorldMagneticModelGivesPlausibleDeclinations() {
        val instant = Instant.parse("2026-06-21T00:00:00Z")

        fun declination(
            latitude: Double,
            longitude: Double,
        ) = PlatformDeclination.declinationDegrees(Coordinates(latitude, longitude), instant)
        // Signs and rough magnitudes only: the exact values depend on the model version in the platform.
        assertTrue(declination(35.69, 51.39) in 3.0..8.0)
        assertTrue(declination(49.28, -123.12) in 12.0..19.0)
        assertTrue(declination(-33.92, 18.42) in -30.0..-20.0)
        assertTrue(abs(declination(0.0, 0.0)) < 10.0)
    }
}
