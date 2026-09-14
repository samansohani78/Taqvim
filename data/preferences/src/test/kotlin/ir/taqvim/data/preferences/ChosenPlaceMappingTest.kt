/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.testing.PropertyTesting
import ir.taqvim.data.preferences.proto.ChosenPlaceProto
import ir.taqvim.data.preferences.proto.PlaceSourceProto
import ir.taqvim.data.preferences.proto.UserPrefs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** T-1502: the chosen place survives the proto, is absent in older stores and unusable stored values read as none. */
class ChosenPlaceMappingTest {
    private val tehran = ChosenPlace(PlaceSource.CITY, 112931, "Tehran", Coordinates(35.6892, 51.389), "Asia/Tehran")

    @Test
    fun `places of every source round-trip through the proto and the serializer`(): Unit =
        runBlocking {
            val base = UserPreferences.defaultsFor("fa")
            val places =
                listOf(
                    tehran,
                    ChosenPlace(PlaceSource.DEVICE, null, null, Coordinates(-33.86, 151.21), "Australia/Sydney"),
                    ChosenPlace(PlaceSource.COORDINATES, null, "Kabul", Coordinates(34.53, 69.17), "Asia/Kabul"),
                )
            places.forEach { place ->
                val prefs = base.copy(place = place)
                val bytes = ByteArrayOutputStream().also { UserPrefsSerializer.writeTo(prefs.toProto(), it) }
                UserPrefsSerializer.readFrom(ByteArrayInputStream(bytes.toByteArray())).toDomain() shouldBe prefs
            }
        }

    @Test
    fun `random valid places round-trip`(): Unit =
        runBlocking {
            val zones = Arb.element(listOf("Asia/Tehran", "Europe/Berlin", "America/Los_Angeles", "UTC"))
            checkAll(
                PropertyTesting.iterations,
                Arb.int(-900_000..900_000),
                Arb.int(-1_800_000..1_800_000),
                zones,
                Arb.long(1L..10_000_000L),
            ) { latitude, longitude, zone, cityId ->
                val place =
                    ChosenPlace(
                        PlaceSource.CITY,
                        cityId,
                        "City",
                        Coordinates(latitude / 10_000.0, longitude / 10_000.0),
                        zone,
                    )
                val prefs = UserPreferences.defaultsFor("en").copy(place = place)
                prefs.toProto().toDomain() shouldBe prefs
            }
        }

    @Test
    fun `stores without a place, and places that cannot be used, read as no place`() {
        UserPreferences.defaultsFor("fa").place.shouldBeNull()
        UserPrefs
            .getDefaultInstance()
            .toDomain()
            .place
            .shouldBeNull()
        val written = UserPreferences.defaultsFor("fa").toProto()
        written.hasPlace() shouldBe false

        val valid = tehran.toProto()
        val unusable =
            listOf(
                valid.toBuilder().setSource(PlaceSourceProto.PLACE_SOURCE_UNSPECIFIED).build(),
                valid.toBuilder().setSourceValue(99).build(),
                valid.toBuilder().setLatitude(123.0).build(),
                valid.toBuilder().setLongitude(Double.NaN).build(),
                valid.toBuilder().setZoneId("").build(),
                valid.toBuilder().setZoneId("Mars/Olympus").build(),
                ChosenPlaceProto.getDefaultInstance(),
            )
        unusable.forEach { proto ->
            written
                .toBuilder()
                .setPlace(proto)
                .build()
                .toDomain()
                .place
                .shouldBeNull()
        }
        written
            .toBuilder()
            .setPlace(valid)
            .build()
            .toDomain()
            .place shouldBe tehran
    }

    @Test
    fun `a device place ignores a stored city id and a blank name reads as unknown`() {
        val stored =
            ChosenPlaceProto
                .newBuilder()
                .setSource(PlaceSourceProto.PLACE_SOURCE_DEVICE)
                .setCityId(42)
                .setName(" ")
                .setLatitude(1.0)
                .setLongitude(2.0)
                .setZoneId("UTC")
                .build()

        stored.toDomainOrNull() shouldBe ChosenPlace(PlaceSource.DEVICE, null, null, Coordinates(1.0, 2.0), "UTC")
    }

    @Test
    fun `places are validated`() {
        shouldThrow<IllegalArgumentException> { tehran.copy(cityId = null) }
        shouldThrow<IllegalArgumentException> { tehran.copy(source = PlaceSource.DEVICE) }
        shouldThrow<IllegalArgumentException> { tehran.copy(zoneId = "Nowhere/Town") }
        ChosenPlace.isKnownZone("Asia/Tehran") shouldBe true
        ChosenPlace.isKnownZone(" ") shouldBe false
    }
}
