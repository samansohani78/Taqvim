/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import ir.taqvim.core.model.JdnRange
import ir.taqvim.data.database.DeviceEventDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

/**
 * Device calendar events backed by the `device_events_cache` table (T-602). Collecting [events] refreshes the cache
 * from the provider at once and after every provider change; without `READ_CALENDAR` the result is empty and the
 * window's cache is cleared.
 */
class DeviceCalendarRepository(
    private val source: InstancesSource,
    private val dao: DeviceEventDao,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Instances touching [days], dated in the device zone read when collection starts. */
    fun events(days: JdnRange): Flow<List<DeviceEvent>> =
        channelFlow {
            val zone = zone()
            val window = DeviceEventMapping.window(days, zone)
            launch { source.changes().onStart { emit(Unit) }.collect { refresh(window) } }
            dao
                .observeInRange(window.fromEpochMillis, window.toEpochMillis)
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
            dao.deleteOverlapping(window.fromEpochMillis, window.toEpochMillis)
        }
        return result
    }
}
