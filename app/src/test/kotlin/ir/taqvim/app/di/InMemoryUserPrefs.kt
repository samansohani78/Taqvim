/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.app.di

import androidx.datastore.core.DataStore
import ir.taqvim.data.preferences.UserPreferences
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.data.preferences.proto.UserPrefs
import ir.taqvim.data.preferences.toProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [DataStore] of [UserPrefs] in memory, for adapter tests. */
internal class InMemoryUserPrefs(
    initial: UserPrefs,
) : DataStore<UserPrefs> {
    private val state = MutableStateFlow(initial)

    override val data: Flow<UserPrefs> = state

    override suspend fun updateData(transform: suspend (t: UserPrefs) -> UserPrefs): UserPrefs =
        transform(state.value).also { state.value = it }
}

/** A repository over [preferences] kept in memory. */
internal fun repositoryOf(preferences: UserPreferences): UserPreferencesRepository =
    UserPreferencesRepository(InMemoryUserPrefs(preferences.toProto()))
