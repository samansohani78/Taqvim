/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.events.ics

import ir.taqvim.data.database.TaqvimDatabase
import ir.taqvim.data.preferences.UserPreferencesRepository
import org.koin.dsl.module

/**
 * Koin bindings of iCalendar import and export (ADR-0002, ADR-0054). Other modules provide the DAOs,
 * `TaqvimDatabase`, `UserPreferencesRepository`, `ContentResolver` and `kotlin.time.Clock`.
 *
 * Both directions read and write a file the user picked through the Storage Access Framework. Nothing here opens a
 * socket: remote subscriptions were removed with the app's network access, so a `.ics` file reaches Taqvim only
 * because the user handed it one.
 */
val icsDataModule =
    module {
        single<TransactionRunner> { RoomTransactionRunner(get<TaqvimDatabase>()) }
        single { IcsDocuments(get()) }
        single { IcsImporter(get(), get(), get(), get(), IcsZones.system) }
        single {
            val preferences = get<UserPreferencesRepository>().preferences
            IcsExporter(get(), get(), { IcsZones.personalCalendars(preferences) }, get(), IcsZones.system)
        }
    }
