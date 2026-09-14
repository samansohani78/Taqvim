/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.preferences

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.taqvim.core.model.Coordinates
import ir.taqvim.core.model.PrayerMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** T-600 (R): the DataStore file on a Robolectric device applies first-run defaults and persists updates. */
@RunWith(AndroidJUnit4::class)
class UserPreferencesRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun deleteStore() {
        context.dataStoreFile(UserPreferencesRepository.FILE_NAME).delete()
    }

    private suspend fun <T> withRepository(
        deviceLanguage: String,
        block: suspend (UserPreferencesRepository) -> T,
    ): T {
        val job = Job()
        val store =
            UserPreferencesRepository.createDataStore(
                context,
                CoroutineScope(Dispatchers.IO + job),
            ) { deviceLanguage }
        return block(UserPreferencesRepository(store)).also { job.cancelAndJoin() }
    }

    @Test
    fun firstRunUsesDeviceLanguageDefaults(): Unit =
        runBlocking {
            val first = withRepository("fa") { it.preferences.first() }

            assertEquals(UserPreferences.defaultsFor("fa"), first)
        }

    @Test
    fun updatesPersistAcrossStoreInstances(): Unit =
        runBlocking {
            withRepository("fa") { repository ->
                repository.update { it.copy(prayerMethod = PrayerMethod.JAFARI, themeMode = ThemeMode.BLACK) }
            }

            val reopened = withRepository("en") { it.preferences.first() }

            assertEquals(PrayerMethod.JAFARI, reopened.prayerMethod)
            assertEquals(ThemeMode.BLACK, reopened.themeMode)
            assertEquals("fa", reopened.languageCode)
        }

    @Test
    fun athanSettingsPersistAcrossStoreInstances(): Unit =
        runBlocking {
            val athan =
                AthanPreferences.DEFAULT.copy(
                    alerts = AthanPreferences.DEFAULT.alerts + (AthanPrayer.FAJR to AthanAlert(true, -5)),
                    sound = AthanSound("content://media/external/audio/7", "Athan"),
                    bypassDndForFajr = true,
                    volumePercent = 55,
                    useIranTime = true,
                )
            withRepository("fa") { repository -> repository.update { it.copy(athan = athan) } }

            val reopened = withRepository("en") { it.preferences.first() }

            assertEquals(athan, reopened.athan)
        }

    @Test
    fun chosenPlacePersistsAcrossStoreInstances(): Unit =
        runBlocking {
            val place = ChosenPlace(PlaceSource.COORDINATES, null, null, Coordinates(29.61, 52.53), "Asia/Tehran")
            withRepository("fa") { repository -> repository.update { it.copy(place = place) } }

            val reopened = withRepository("fa") { it.preferences.first() }

            assertEquals(place, reopened.place)
        }
}
