/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/** T-1102 (R): the athan service lifecycle, its notification, silent mode, the Fajr bypass and snooze. */
@RunWith(AndroidJUnit4::class)
class AthanServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = requireNotNull(context.getSystemService(NotificationManager::class.java))
    private val audio = requireNotNull(context.getSystemService(AudioManager::class.java))

    private fun start(intent: Intent): ServiceController<AthanService> =
        Robolectric.buildService(AthanService::class.java, intent).create().startCommand(0, 1)

    private fun play(request: AthanRequest = AthanFixtures.request()) =
        start(AthanIntents.of(context, AthanIntents.ACTION_PLAY, request))

    @Before
    fun sounds() {
        ShadowMediaPlayer.addMediaInfo(
            DataSource.toDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)),
            ShadowMediaPlayer.MediaInfo(DURATION_MS, 0),
        )
    }

    @Test
    fun theAthanPlaysInTheForegroundUntilStopped() {
        val controller = play()
        val service = controller.get()

        val notification = shadowOf(service).lastForegroundNotification.shouldNotBeNull()
        notification.channelId shouldBe AthanNotifications.CHANNEL_ID
        notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString() shouldBe "Dhuhr athan"
        notification.actions.map { it.title.toString() } shouldBe listOf("Stop", "Snooze")
        val channel = notifications.getNotificationChannel(AthanNotifications.CHANNEL_ID).shouldNotBeNull()
        channel.importance shouldBe NotificationManager.IMPORTANCE_HIGH
        channel.sound.shouldBeNull()
        val player =
            service.currentSession
                .shouldNotBeNull()
                .currentPlayer
                .shouldNotBeNull()
        shadowOf(player).state shouldBe ShadowMediaPlayer.State.STARTED

        service.onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_STOP), 0, 2)

        shadowOf(service).isStoppedBySelf shouldBe true
        shadowOf(service).isForegroundStopped shouldBe true
        service.currentSession.shouldBeNull()
        controller.destroy()
    }

    @Test
    fun theAthanStopsAfterFiveMinutesOrWhenTheSoundEnds() {
        val long = play().get()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(AthanService.MAX_DURATION.inWholeMinutes))
        shadowOf(long).isStoppedBySelf shouldBe true

        val short = play().get()
        shadowOf(
            short.currentSession
                .shouldNotBeNull()
                .currentPlayer
                .shouldNotBeNull(),
        ).invokeCompletionListener()
        shadowOf(Looper.getMainLooper()).idle()
        shadowOf(short).isStoppedBySelf shouldBe true
        short.currentSession.shouldBeNull()
    }

    @Test
    fun silentModeMutesTheAthanExceptAnAllowedFajrBypass() {
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        val muted = play().get()
        shadowOf(muted).lastForegroundNotification.shouldNotBeNull()
        muted.currentSession
            .shouldNotBeNull()
            .currentPlayer
            .shouldBeNull()

        shadowOf(notifications).setNotificationPolicyAccessGranted(true)
        val bypass = AthanFixtures.PLAYBACK.copy(bypassDndForFajr = true)
        play(AthanFixtures.request(AthanPrayer.FAJR, bypass))
            .get()
            .currentSession
            ?.currentPlayer
            .shouldNotBeNull()
        play(AthanFixtures.request(AthanPrayer.DHUHR, bypass))
            .get()
            .currentSession
            ?.currentPlayer
            .shouldBeNull()
    }

    @Test
    fun snoozeStopsAndPlaysTheSameAthanAgainLater() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val request = AthanFixtures.request()
        val controller = play(request)
        val before = Clock.System.now()

        controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request), 0, 2)

        shadowOf(controller.get()).isStoppedBySelf shouldBe true
        val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))
        val alarm = shadowOf(alarms).scheduledAlarms.single()
        val due = before + AthanSnooze.SNOOZE
        (alarm.triggerAtMs in due.toEpochMilliseconds()..(due + 5.seconds).toEpochMilliseconds()) shouldBe true
        @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
        val operation = shadowOf(requireNotNull(alarm.operation))
        operation.isForegroundService shouldBe true
        AthanIntents.requestOf(operation.savedIntent) shouldBe request
        operation.savedIntent.action shouldBe AthanIntents.ACTION_PLAY
    }

    @Test
    fun snoozeGoesThroughThePersistentSchedulerWhenTheAppProvidesIt() {
        val request = AthanFixtures.request()
        val snoozer = RecordingSnoozer()
        startKoin { modules(module { single<SnoozeScheduler> { snoozer } }) }
        try {
            val controller = play(request)
            val before = Clock.System.now()

            controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request), 0, 2)

            snoozer.done.await(5, TimeUnit.SECONDS) shouldBe true
            awaitStop(controller.get())
            val (athan, at) = snoozer.athans.single()
            athan shouldBe request.athan
            (at - before - AthanSnooze.SNOOZE < 5.seconds) shouldBe true
            val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))
            shadowOf(alarms).scheduledAlarms.isEmpty() shouldBe true
        } finally {
            stopKoin()
        }
    }

    @Test
    fun theServiceStaysUntilTheSnoozeIsStoredAndTheSoundStopsAtOnce() {
        // Review R16: the service used to stop at once while the snooze was still being written.
        val request = AthanFixtures.request()
        val snoozer = GatedSnoozer()
        startKoin { modules(module { single<SnoozeScheduler> { snoozer } }) }
        try {
            val controller = play(request)
            controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request), 0, 2)

            controller.get().currentSession.shouldBeNull()
            snoozer.started.await(5, TimeUnit.SECONDS) shouldBe true
            shadowOf(controller.get()).isStoppedBySelf shouldBe false

            snoozer.release()
            awaitStop(controller.get())
            snoozer.athans.single().first shouldBe request.athan
        } finally {
            stopKoin()
        }
    }

    @Test
    fun aSnoozeTheSchedulerCannotStoreFallsBackToASystemAlarm() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val request = AthanFixtures.request()
        val snoozer = GatedSnoozer(failure = IllegalStateException("database closed"))
        startKoin { modules(module { single<SnoozeScheduler> { snoozer } }) }
        try {
            val controller = play(request)
            controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request), 0, 2)
            snoozer.release()
            awaitStop(controller.get())

            val alarms = requireNotNull(context.getSystemService(AlarmManager::class.java))

            @Suppress("DEPRECATION") // Robolectric 4.17 exposes an alarm's PendingIntent only as a deprecated field.
            val operation = shadowOf(requireNotNull(shadowOf(alarms).scheduledAlarms.single().operation))
            AthanIntents.requestOf(operation.savedIntent) shouldBe request
        } finally {
            stopKoin()
        }
    }

    @Test
    fun anAthanStartedWhileASnoozeIsStoredKeepsPlaying() {
        val request = AthanFixtures.request()
        val snoozer = GatedSnoozer()
        startKoin { modules(module { single<SnoozeScheduler> { snoozer } }) }
        try {
            val controller = play(request)
            controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_SNOOZE, request), 0, 2)
            snoozer.started.await(5, TimeUnit.SECONDS) shouldBe true
            controller.get().onStartCommand(AthanIntents.of(context, AthanIntents.ACTION_PLAY, request), 0, 3)

            snoozer.release()
            snoozer.finished.await(5, TimeUnit.SECONDS) shouldBe true
            shadowOf(Looper.getMainLooper()).idle()

            controller.get().currentSession.shouldNotBeNull()
            shadowOf(controller.get()).isStoppedBySelf shouldBe false
        } finally {
            stopKoin()
        }
    }

    /** Runs the main looper until the service stops itself, for work that finishes on another thread. */
    private fun awaitStop(service: AthanService) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!shadowOf(service).isStoppedBySelf && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        shadowOf(service).isStoppedBySelf shouldBe true
    }

    @Test
    fun intentsWithoutAValidRequestOnlyStopTheService() {
        shadowOf(start(Intent(context, AthanService::class.java)).get()).isStoppedBySelf shouldBe true
        val bare = start(AthanIntents.of(context, AthanIntents.ACTION_PLAY)).get()
        shadowOf(bare).isStoppedBySelf shouldBe true
        bare.onBind(Intent()).shouldBeNull()
    }

    private companion object {
        const val DURATION_MS = 600_000
    }
}
