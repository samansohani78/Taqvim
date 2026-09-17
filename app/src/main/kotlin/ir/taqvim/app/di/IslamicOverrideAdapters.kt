/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.content.Context
import androidx.core.net.toUri
import ir.taqvim.core.calendar.InvalidOverrideException
import ir.taqvim.core.calendar.IslamicMonthOverrides
import ir.taqvim.core.calendar.TodayProvider
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.model.CalendarSystem
import ir.taqvim.data.preferences.IslamicOverrideOrigin
import ir.taqvim.data.preferences.IslamicOverrideSetting
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.settings.IslamicOverrideKind
import ir.taqvim.feature.settings.IslamicOverrideStatus
import ir.taqvim.feature.settings.IslamicOverrideStore
import ir.taqvim.feature.settings.OverrideImportResult
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** What reading an override file gave: its text, or why there is none. */
internal sealed interface OverrideFileText {
    data class Read(
        val text: String,
    ) : OverrideFileText

    data object TooLarge : OverrideFileText

    data object Unreadable : OverrideFileText
}

/** Reads the file at a content address, at most [OVERRIDE_MAX_BYTES] bytes. */
internal fun interface OverrideFileReader {
    suspend fun read(uri: String): OverrideFileText
}

/** Override files are small (a few kilobytes per decade); anything larger is refused unread. */
internal const val OVERRIDE_MAX_BYTES: Int = 512 * 1024

/** Up to [limit] bytes of this stream (fewer at its end). */
internal fun InputStream.readAtMost(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(READ_BUFFER_BYTES)
    var read = 0
    while (out.size() < limit && read >= 0) {
        read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read > 0) out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

private const val READ_BUFFER_BYTES = 8 * 1024

/** [OverrideFileReader] over the content resolver, off the main thread. */
internal class ContentOverrideFileReader(
    private val context: Context,
) : OverrideFileReader {
    override suspend fun read(uri: String): OverrideFileText =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri.toUri())?.use { stream ->
                    val bytes = stream.readAtMost(OVERRIDE_MAX_BYTES + 1)
                    if (bytes.size > OVERRIDE_MAX_BYTES) {
                        OverrideFileText.TooLarge
                    } else {
                        OverrideFileText.Read(bytes.decodeToString())
                    }
                }
            }.getOrNull() ?: OverrideFileText.Unreadable
        }
}

/**
 * [IslamicOverrideStore] (ADR-0037) over the user preferences: the bundled official Iranian months ([bundled]) or an
 * imported file are stored as text and parsed where they are used; [today] tells whether the covered months ended.
 */
internal class PreferencesIslamicOverrideStore(
    private val preferences: UserPreferencesRepository,
    private val reader: OverrideFileReader,
    private val today: TodayProvider,
    private val bundled: () -> String? = IslamicMonthOverrides::bundledIranOfficialText,
) : IslamicOverrideStore {
    override fun status(): Flow<IslamicOverrideStatus> =
        preferences.preferences
            .map { it.overrideStatus() }
            .distinctUntilChanged()

    override suspend fun setOfficial(enabled: Boolean) {
        val text = if (enabled) bundled() else null
        preferences.update { current ->
            when {
                text != null -> {
                    current.copy(islamicOverride = IslamicOverrideSetting(IslamicOverrideOrigin.OFFICIAL_BUNDLED, text))
                }

                current.islamicOverride.origin == IslamicOverrideOrigin.OFFICIAL_BUNDLED -> {
                    current.copy(islamicOverride = IslamicOverrideSetting.NONE)
                }

                else -> {
                    current
                }
            }
        }
    }

    override suspend fun import(uri: String): OverrideImportResult =
        when (val file = reader.read(uri)) {
            OverrideFileText.TooLarge -> {
                OverrideImportResult.TOO_LARGE
            }

            OverrideFileText.Unreadable -> {
                OverrideImportResult.UNREADABLE
            }

            is OverrideFileText.Read -> {
                val parsed = IslamicMonthOverrides.parse(file.text)
                val problem = (parsed.exceptionOrNull() as? InvalidOverrideException)?.problem
                when {
                    problem != null -> {
                        OverrideImportResult.of(problem)
                    }

                    parsed.isFailure -> {
                        OverrideImportResult.NOT_JSON
                    }

                    else -> {
                        val setting = IslamicOverrideSetting(IslamicOverrideOrigin.IMPORTED, file.text)
                        preferences.update { it.copy(islamicOverride = setting) }
                        OverrideImportResult.IMPORTED
                    }
                }
            }
        }

    override suspend fun remove() {
        preferences.update { it.copy(islamicOverride = IslamicOverrideSetting.NONE) }
    }

    private fun UserPreferences.overrideStatus(): IslamicOverrideStatus {
        val setting = islamicOverride
        val kind =
            when (setting.origin) {
                IslamicOverrideOrigin.NONE -> IslamicOverrideKind.NONE
                IslamicOverrideOrigin.OFFICIAL_BUNDLED -> IslamicOverrideKind.OFFICIAL
                IslamicOverrideOrigin.IMPORTED -> IslamicOverrideKind.IMPORTED
            }
        val overrides = setting.overrides
        return IslamicOverrideStatus(
            language = languageSpec(),
            kind = kind,
            firstMonth = overrides?.first?.let { (year, month) -> hijri(year, month) },
            lastMonth = overrides?.last?.let { (year, month) -> hijri(year, month) },
            broken = setting.isBroken,
            ended = overrides != null && overrides.table.endJdn <= today.today().value,
        )
    }

    private fun hijri(
        year: Int,
        month: Int,
    ) = CalendarDate(CalendarSystem.ISLAMIC, year, month, 1)
}
