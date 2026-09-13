/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import ir.taqvim.data.preferences.proto.UserPrefs
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Proto serializer for [UserPrefs]; unreadable files surface as [CorruptionException]. */
object UserPrefsSerializer : Serializer<UserPrefs> {
    override val defaultValue: UserPrefs = UserPrefs.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPrefs =
        runCatching { UserPrefs.parseFrom(input) }
            .getOrElse { throw CorruptionException("UserPrefs could not be read", it) }

    override suspend fun writeTo(
        t: UserPrefs,
        output: OutputStream,
    ) = t.writeTo(output)
}

/**
 * Schema migrations for [UserPrefs] (docs/PLAN.md §4.4). Version 0 is a never-initialised store: it receives the
 * first-run defaults for [deviceLanguage] (or keeps a language that was already stored).
 */
class UserPrefsMigration(
    private val deviceLanguage: () -> String,
) : DataMigration<UserPrefs> {
    override suspend fun shouldMigrate(currentData: UserPrefs): Boolean =
        currentData.schemaVersion < CURRENT_SCHEMA_VERSION

    override suspend fun migrate(currentData: UserPrefs): UserPrefs =
        (currentData.schemaVersion until CURRENT_SCHEMA_VERSION).fold(currentData) { prefs, version ->
            step(version, prefs)
        }

    override suspend fun cleanUp(): Unit = Unit

    private fun step(
        fromVersion: Int,
        prefs: UserPrefs,
    ): UserPrefs =
        when (fromVersion) {
            0 -> {
                val language = prefs.languageCode.ifBlank { deviceLanguage() }
                UserPreferences.defaultsFor(language).toProto(schemaVersion = 1)
            }

            else -> {
                error("no migration from UserPrefs schema $fromVersion")
            }
        }

    companion object {
        /** Schema version written by this build. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/** Reads and updates [UserPreferences] stored in [dataStore]. */
class UserPreferencesRepository(
    private val dataStore: DataStore<UserPrefs>,
) {
    /** Current preferences, re-emitted after every change. */
    val preferences: Flow<UserPreferences> = dataStore.data.map { it.toDomain() }

    /** Atomically replaces the preferences with [transform] of the current value. */
    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.updateData { transform(it.toDomain()).toProto() }
    }

    companion object {
        /** File name under `files/datastore/`. */
        const val FILE_NAME: String = "user_prefs.pb"

        /** The app's preferences store; create one per process and keep it for the app's lifetime. */
        fun createDataStore(
            context: Context,
            scope: CoroutineScope,
            deviceLanguage: () -> String,
        ): DataStore<UserPrefs> =
            DataStoreFactory.create(
                serializer = UserPrefsSerializer,
                migrations = listOf(UserPrefsMigration(deviceLanguage)),
                scope = scope,
                produceFile = { context.dataStoreFile(FILE_NAME) },
            )
    }
}
