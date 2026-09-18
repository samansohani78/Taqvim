/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import ir.taqvim.data.database.DiagnosticsDao
import ir.taqvim.data.database.DiagnosticsLevel
import ir.taqvim.data.database.DiagnosticsLogEntity
import ir.taqvim.feature.about.AboutInfo
import ir.taqvim.feature.about.AboutLinks
import ir.taqvim.feature.about.DiagnosticEntry
import ir.taqvim.feature.about.DiagnosticLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** T-1504 ports: app facts follow the app language, and diagnostics rows keep their order and levels. */
class AboutAdaptersTest {
    private val build = AppBuild(versionName = "1.2.3", versionCode = 42, buildType = "release")

    @Test
    fun `app facts carry the build and follow the app language without repeats`(): Unit =
        runTest {
            val source = PreferencesAboutInfoSource(build, flowOf("fa", "fa", "en"))

            source.about().toList() shouldBe
                listOf(
                    AboutInfo("1.2.3", 42, "release", "fa", publishedLinks),
                    AboutInfo("1.2.3", 42, "release", "en", publishedLinks),
                )
        }

    @Test
    fun `problem reports are addressed to the published support address and nothing else is linked`() {
        SUPPORT_EMAIL shouldBe "support@taqvim.app"
        publishedLinks shouldBe AboutLinks(supportEmail = SUPPORT_EMAIL)
    }

    @Test
    fun `the current build comes from Gradle`() {
        val current = AppBuild.current()

        current.versionName.shouldNotBeBlank()
        current.buildType.shouldNotBeBlank()
        (current.versionCode > 0) shouldBe true
    }

    @Test
    fun `diagnostics keep the ring order, limit and every level`(): Unit =
        runTest {
            val rows =
                DiagnosticsLevel.entries.mapIndexed { index, level ->
                    DiagnosticsLogEntity(
                        id = 10L - index,
                        atEpochMillis = 1_000L - index,
                        level,
                        "tag$index",
                        "m$index",
                    )
                }
            val dao = FakeDiagnosticsDao(rows)

            RoomDiagnosticsSource(dao).recent(3).first() shouldBe
                listOf(
                    DiagnosticEntry(1_000, DiagnosticLevel.DEBUG, "tag0", "m0"),
                    DiagnosticEntry(999, DiagnosticLevel.INFO, "tag1", "m1"),
                    DiagnosticEntry(998, DiagnosticLevel.WARN, "tag2", "m2"),
                )
            DiagnosticsLevel.entries.map {
                rows
                    .first { row -> row.level == it }
                    .toEntry()
                    .level.name
            } shouldBe
                DiagnosticsLevel.entries.map { it.name }
        }

    private class FakeDiagnosticsDao(
        rows: List<DiagnosticsLogEntity>,
    ) : DiagnosticsDao {
        private val stored = MutableStateFlow(rows)

        override suspend fun insert(entry: DiagnosticsLogEntity): Long {
            stored.value = listOf(entry) + stored.value
            return entry.id
        }

        override suspend fun trimTo(keep: Int) {
            stored.value = stored.value.take(keep)
        }

        override fun observeRecent(limit: Int): Flow<List<DiagnosticsLogEntity>> = stored.map { it.take(limit) }

        override suspend fun count(): Int = stored.value.size

        override suspend fun clear() {
            stored.value = emptyList()
        }
    }
}
