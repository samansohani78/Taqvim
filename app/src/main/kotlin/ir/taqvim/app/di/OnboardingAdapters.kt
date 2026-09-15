/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import ir.taqvim.core.i18n.LanguageTable
import ir.taqvim.data.preferences.DeviceLanguages
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.settings.OnboardingStore
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** [OnboardingStore] (T-1501) over the user preferences. */
internal class PreferencesOnboardingStore(
    private val preferences: UserPreferencesRepository,
) : OnboardingStore {
    override suspend fun complete() {
        preferences.update { it.copy(onboardingCompleted = true) }
    }
}

/** The app's own language setting of the platform (T-1501, ADR-0023). */
internal interface AppLocales {
    /** The BCP 47 tag of the app language set for Taqvim alone, or `null` when the app follows the device. */
    fun current(): String?

    /** Sets Taqvim's language to the BCP 47 [tag]. */
    fun apply(tag: String)
}

/** Android 13+: the per-app language of [LocaleManager], also shown in the system's app language settings. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class LocaleManagerAppLocales(
    private val manager: LocaleManager,
) : AppLocales {
    override fun current(): String? = manager.applicationLocales.toLanguageTags().ifEmpty { null }

    override fun apply(tag: String) {
        manager.applicationLocales = LocaleList.forLanguageTags(tag)
    }
}

/**
 * Before Android 13: the language tag kept in the app's own preferences file and applied to every activity by
 * [wrap]; [applied] tells the shown activity to recreate itself in the new language.
 */
internal class LegacyAppLocales(
    private val context: Context,
) : AppLocales {
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Tags applied, after they are stored. */
    val applied: SharedFlow<String> = changes.asSharedFlow()

    override fun current(): String? = storedTag(context)

    override fun apply(tag: String) {
        // apply() updates the in-memory preferences at once, so the recreated activity's wrap() already reads the tag.
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, tag) }
        changes.tryEmit(tag)
    }

    companion object {
        private const val FILE = "taqvim_app_locale"
        private const val KEY = "language_tag"

        private fun storedTag(context: Context): String? =
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)

        /** [base] in the stored app language before Android 13; [base] itself otherwise or without a stored tag. */
        fun wrap(base: Context): Context {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) null else storedTag(base)
            return tag?.let { wrap(base, it) } ?: base
        }

        /** [base] with its configuration in the language of [tag]. */
        fun wrap(
            base: Context,
            tag: String,
        ): Context {
            val locale = Locale.forLanguageTag(tag)
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            return base.createConfigurationContext(configuration)
        }
    }
}

/** The platform's app language: [LocaleManagerAppLocales] on Android 13+, otherwise [LegacyAppLocales]. */
internal fun platformAppLocales(context: Context): AppLocales =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LocaleManagerAppLocales(context.getSystemService(LocaleManager::class.java))
    } else {
        LegacyAppLocales(context)
    }

/** Decisions that keep the stored language and the platform's app language in step (ADR-0023). */
internal object LanguageSync {
    /**
     * The launch language to store because the app language was set outside Taqvim (the system's per-app language
     * settings), or `null` when [appLocaleTag] is unset or already means [stored].
     */
    fun adopted(
        stored: String,
        appLocaleTag: String?,
    ): String? = appLocaleTag?.let { DeviceLanguages.find(it)?.code }?.takeIf { it != stored }

    /**
     * The locale tag to set so the app shows [stored], or `null` when the app language ([appLocaleTag], or the device's
     * [deviceTag] while unset) already means it.
     */
    fun toApply(
        stored: String,
        appLocaleTag: String?,
        deviceTag: String,
    ): String? {
        val shown = DeviceLanguages.match(appLocaleTag ?: deviceTag)
        return if (shown == stored) null else LanguageTable.forCode(stored)?.localeTag
    }
}

/**
 * Keeps the app language in step with the stored one (T-1501): a language set in the system settings while Taqvim was
 * closed is adopted first (applying its defaults), then every stored language is applied to the platform.
 */
internal class AppLanguageSync(
    private val preferences: UserPreferencesRepository,
    private val locales: AppLocales,
    private val deviceTag: () -> String,
) {
    suspend fun run() {
        val stored = preferences.preferences.first().languageCode
        LanguageSync.adopted(stored, locales.current())?.let { code -> preferences.update { it.withLanguage(code) } }
        preferences.preferences.map { it.languageCode }.distinctUntilChanged().collect { code ->
            LanguageSync.toApply(code, locales.current(), deviceTag())?.let(locales::apply)
        }
    }
}

/** The device language as a BCP 47 tag. */
internal fun deviceLanguageTag(): String = Locale.getDefault().toLanguageTag()

/** The onboarding (T-1501) and app language bindings. */
val onboardingPortsModule =
    module {
        single<OnboardingStore> { PreferencesOnboardingStore(get()) }
        single { platformAppLocales(androidContext()) }
        single { AppLanguageSync(get(), get(), ::deviceLanguageTag) }
    }
