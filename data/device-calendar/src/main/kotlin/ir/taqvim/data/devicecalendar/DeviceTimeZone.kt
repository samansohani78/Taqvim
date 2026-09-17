/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.devicecalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone

/**
 * The device's time zone as a stream (review I06): flows that date instants read it as an input instead of capturing
 * the zone once when collection starts, so a zone change rebuilds their day windows. The selected place's zone is a
 * separate preference and is not affected.
 */
object DeviceTimeZone {
    /** Koin qualifier of the shared, process-wide [changes] stream. */
    const val QUALIFIER: String = "deviceTimeZone"

    /** The zone at collection time, once: for callers without a [Context] and for fixed-zone tests. */
    val current: Flow<TimeZone> = flow { emit(TimeZone.currentSystemDefault()) }

    /**
     * The zone now and again after every `ACTION_TIMEZONE_CHANGED` broadcast, without repeats. The platform resets the
     * process's default zone before it delivers the broadcast, so [zone] reads the new value.
     */
    fun changes(
        context: Context,
        zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    ): Flow<TimeZone> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) trySend(zone())
                    }
                }
            val appContext = context.applicationContext
            // System broadcasts reach not-exported receivers; nothing else may send this one to the app.
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            trySend(zone())
            // The platform throws when the receiver is already gone (e.g. its process is being torn down).
            awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
        }.distinctUntilChanged()
}
