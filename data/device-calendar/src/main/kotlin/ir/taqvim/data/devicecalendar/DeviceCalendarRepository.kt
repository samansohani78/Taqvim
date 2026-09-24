/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.DeviceEventDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/**
 * Device calendar events backed by the `device_events_cache` table (T-602). Collecting [events] refreshes the cache
 * from the provider at once and after every provider change; without `READ_CALENDAR` the result is empty and the
 * window's cache is cleared.
 *
 * The clearing does not wait for [refresh]: a collection that answers "no events, because there is no permission"
 * deletes the window's rows first, so the reader's own stream guarantees the cache is gone rather than a refresh
 * coroutine that a cancelled collection may never let finish.
 */
class DeviceCalendarRepository(
    private val source: InstancesSource,
    private val dao: DeviceEventDao,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Instances touching [days], dated in the current device zone; a zone change re-reads the shifted window. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun events(days: JdnRange): Flow<List<DeviceEvent>> =
        zones.distinctUntilChanged().flatMapLatest { zone -> eventsIn(days, zone) }

    private fun eventsIn(
        days: JdnRange,
        zone: TimeZone,
    ): Flow<List<DeviceEvent>> =
        channelFlow {
            val window = DeviceEventMapping.window(days, zone)
            launch { source.changes().onStart { emit(Unit) }.collect { refresh(window) } }
            dao
                .observeInRange(window.fromEpochMillis, window.toEpochMillis)
                // Rows the app may no longer read are deleted here, before the empty list they turn into is sent, so
                // the guarantee holds even when the collection is cancelled right after that list (T-602).
                .onEach { cached -> if (cached.isNotEmpty() && !source.hasPermission()) clear(window) }
                .map { cached ->
                    if (source.hasPermission()) {
                        cached
                            .map { DeviceEventMapping.toDeviceEvent(it, zone) }
                            .filter { DeviceEventMapping.overlaps(it.days, days) }
                    } else {
                        emptyList()
                    }
                }.distinctUntilChanged()
                .collect { send(it) }
        }

    /** Drops [window]'s cached rows; the provider's answer is no longer the app's to keep. */
    private suspend fun clear(window: InstantWindow) =
        dao.deleteOverlapping(window.fromEpochMillis, window.toEpochMillis)

    /** Re-reads [window] from the provider into the cache and returns what the provider answered. */
    suspend fun refresh(window: InstantWindow): InstancesResult {
        val result = withContext(ioDispatcher) { source.query(window) }
        // An unavailable provider leaves the cache as it is.
        if (result is InstancesResult.Rows) {
            dao.replaceOverlapping(
                window.fromEpochMillis,
                window.toEpochMillis,
                result.rows.mapNotNull(DeviceEventMapping::toCacheEntity),
            )
        } else if (result == InstancesResult.PermissionDenied) {
            clear(window)
        }
        return result
    }
}
