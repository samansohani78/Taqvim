/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import ir.taqvim.app.BuildConfig
import ir.taqvim.data.database.DiagnosticsDao
import ir.taqvim.data.database.DiagnosticsLogEntity
import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.about.AboutInfo
import ir.taqvim.feature.about.AboutInfoSource
import ir.taqvim.feature.about.AboutLinks
import ir.taqvim.feature.about.DiagnosticEntry
import ir.taqvim.feature.about.DiagnosticLevel
import ir.taqvim.feature.about.DiagnosticsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Ports of the About screen (T-1504): the app's build facts with the app language, and the local diagnostics ring
 * (T-601). Problem reports are addressed to [SUPPORT_EMAIL]; no website, source or privacy link is published yet, so
 * those [AboutLinks] entries stay hidden.
 */
val aboutPortsModule: Module =
    module {
        single<AboutInfoSource> {
            val preferences = get<UserPreferencesRepository>()
            PreferencesAboutInfoSource(AppBuild.current(), preferences.preferences.map { it.languageCode })
        }
        single<DiagnosticsSource> { RoomDiagnosticsSource(get<TaqvimDatabase>().diagnosticsDao()) }
    }

/**
 * Where problem reports are addressed (SUPPORT.md; confirmed by the owner on 2026-09-18). It is an address, not a
 * translated string, so it is a constant rather than a string resource.
 */
internal const val SUPPORT_EMAIL = "support@taqvim.app"

/** The public links of the About page: only the support address is published so far. */
internal val publishedLinks = AboutLinks(supportEmail = SUPPORT_EMAIL)

/** The version and build type of the installed app. */
internal data class AppBuild(
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
) {
    /** One line for a crash record, e.g. `1.0.0 (10099, release)`. */
    val summary: String get() = "$versionName ($versionCode, $buildType)"

    companion object {
        /** The facts Gradle wrote into `BuildConfig` for this build. */
        fun current(): AppBuild =
            AppBuild(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong(), BuildConfig.BUILD_TYPE)
    }
}

/** [AboutInfoSource] from [build] and the app language of each [languageCodes] value. */
internal class PreferencesAboutInfoSource(
    private val build: AppBuild,
    private val languageCodes: Flow<String>,
    private val links: AboutLinks = publishedLinks,
) : AboutInfoSource {
    override fun about(): Flow<AboutInfo> =
        languageCodes.distinctUntilChanged().map { language ->
            AboutInfo(build.versionName, build.versionCode, build.buildType, language, links)
        }
}

/** [DiagnosticsSource] over the Room diagnostics ring, newest first. */
internal class RoomDiagnosticsSource(
    private val dao: DiagnosticsDao,
) : DiagnosticsSource {
    override fun recent(limit: Int): Flow<List<DiagnosticEntry>> =
        dao.observeRecent(limit).map { rows -> rows.map(DiagnosticsLogEntity::toEntry) }
}

/** A stored diagnostics row as shown on the About screen; levels are matched by name. */
internal fun DiagnosticsLogEntity.toEntry(): DiagnosticEntry =
    DiagnosticEntry(atEpochMillis, DiagnosticLevel.valueOf(level.name), tag, message)
