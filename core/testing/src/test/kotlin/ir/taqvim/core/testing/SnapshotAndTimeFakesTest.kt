/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SnapshotAndTimeFakesTest {
    @Test
    fun `matching snapshot passes without touching the file`(
        @TempDir dir: File,
    ) {
        val snapshot = File(dir, "format.txt").apply { writeText("# generated-by: FormatTest\nline 1\nline 2\n") }
        val modified = snapshot.lastModified()

        SnapshotVerifier(update = false).verify(snapshot, "line 1\nline 2\n\n", "FormatTest").isSuccess shouldBe true
        snapshot.lastModified() shouldBe modified
    }

    @Test
    fun `missing or different snapshots fail unless updating`(
        @TempDir dir: File,
    ) {
        val missing = File(dir, "missing.txt")
        val different = File(dir, "different.txt").apply { writeText("# generated-by: T\nline 1\nold\n") }
        val verifier = SnapshotVerifier(update = false)

        verifier
            .verify(missing, "x", "T")
            .exceptionOrNull()
            ?.message
            .orEmpty() shouldContain "is missing"
        val failure = verifier.verify(different, "line 1\nnew", "T").exceptionOrNull()
        failure.shouldBeInstanceOf<AssertionError>()
        failure.message.orEmpty() shouldContain "differs at line 3"
        missing.exists() shouldBe false
    }

    @Test
    fun `update mode writes the rendered snapshot`(
        @TempDir dir: File,
    ) {
        val snapshot = File(dir, "nested/new.txt")

        SnapshotVerifier(update = true).verify(snapshot, "value", "SnapshotTest").isSuccess shouldBe true
        snapshot.readText() shouldBe "# generated-by: SnapshotTest\nvalue\n"
    }

    @Test
    fun `update defaults to the system property`(
        @TempDir dir: File,
    ) {
        val snapshot = File(dir, "property.txt")
        System.setProperty(SnapshotVerifier.UPDATE_PROPERTY, "true")
        val result = runCatching { SnapshotVerifier().verify(snapshot, "v", "PropertyTest") }
        System.clearProperty(SnapshotVerifier.UPDATE_PROPERTY)

        result.getOrThrow().isSuccess shouldBe true
        SnapshotVerifier().verify(File(dir, "other.txt"), "v", "PropertyTest").isFailure shouldBe true
    }

    @Test
    fun `first different line handles prefixes`() {
        SnapshotVerifier.firstDifferentLine("a\nb", "a\nc") shouldBe 2
        SnapshotVerifier.firstDifferentLine("a", "a\nb") shouldBe 2
        SnapshotVerifier.firstDifferentLine("a\nb", "a\nb") shouldBe 3
    }

    @Test
    fun `fake clock only moves when told`() {
        val clock = FakeClock()

        clock.now() shouldBe FakeClock.DEFAULT_START
        clock.advanceBy(3.hours)
        clock.now() shouldBe FakeClock.DEFAULT_START + 3.hours
        clock.setTo(Instant.fromEpochSeconds(0))
        clock.now() shouldBe Instant.fromEpochSeconds(0)
        shouldThrow<IllegalArgumentException> { clock.advanceBy((-1).seconds) }.message.orEmpty() shouldContain
            "cannot move backwards"
    }

    @Test
    fun `fake time zone switches zones`() {
        val zone = FakeTimeZone()

        zone.current shouldBe TimeZones.TEHRAN
        zone.set(TimeZones.KATHMANDU)
        zone.current shouldBe TimeZones.KATHMANDU
        FakeTimeZone(TimeZone.UTC).current shouldBe TimeZones.UTC
    }

    @Test
    fun `well-known zones have the expected offsets`() {
        val instant = Instant.parse("2026-01-15T00:00:00Z")

        TimeZones.TEHRAN.offsetAt(instant) shouldBe UtcOffset(hours = 3, minutes = 30)
        TimeZones.KABUL.offsetAt(instant) shouldBe UtcOffset(hours = 4, minutes = 30)
        TimeZones.KATHMANDU.offsetAt(instant) shouldBe UtcOffset(hours = 5, minutes = 45)
        TimeZones.BERLIN.offsetAt(instant) shouldBe UtcOffset(hours = 1)
        TimeZones.LOS_ANGELES.offsetAt(instant) shouldBe UtcOffset(hours = -8)
    }
}
