/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.taqvim.core.calendar.toJdn
import ir.taqvim.core.calendar.toLocalDate
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.ui.component.DateSelection
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/**
 * The compass (T-1302): the filtered sensor heading (true north once a place is known), Qibla, Sun and Moon markers
 * and the Sun's path, stopped mode, and a TalkBack announcement every 15°.
 */
class CompassViewModel(
    settingsSource: CompassSettingsSource,
    sensors: MotionSensors,
    declination: DeclinationModel,
    clock: Clock,
) : ViewModel() {
    private val frozen = MutableStateFlow(false)
    private val showSunPath = MutableStateFlow(false)
    private val settings = settingsSource.settings().onStart<CompassSettings?> { emit(null) }

    private val shownHeading: Flow<HeadingReading?> =
        combine(filteredHeadings(sensors), frozen) { heading, isFrozen -> heading to isFrozen }
            .scan<Pair<HeadingReading, Boolean>, HeadingReading?>(null) { shown, (heading, isFrozen) ->
                if (isFrozen && shown is HeadingReading.Measured) shown else heading
            }

    /**
     * The moment the sky is drawn for: now, or the day and time the user is planning (T-1302).
     *
     * Only this flow changes between live and planned mode. The heading keeps coming from the sensors through
     * [shownHeading], which this does not touch, so the needle follows the device exactly as before while the Sun and
     * Moon show where they will be. `null` is live, and is the default, so a screen nobody has planned on behaves
     * identically to before this existed.
     */
    private val plannedDay = MutableStateFlow<Jdn?>(null)
    private val plannedMinute = MutableStateFlow(NOON_MINUTE)

    private val instants: Flow<Instant> =
        combine(settings, plannedDay, plannedMinute, minuteTicks(clock)) { current, day, minute, now ->
            val zone = current?.place?.timeZone
            if (day != null && zone != null) day.atMinuteOfDay(minute, zone) else now
        }

    private val sky: Flow<SkySnapshot?> =
        combine(settings, instants) { current, instant ->
            current?.place?.let { CompassStateMapper.snapshot(it, instant, declination) }
        }.onStart { emit(null) }

    private val pickerOpen = MutableStateFlow(false)

    /**
     * The latest settings, for converting a picked date with the calendar the picker was built from.
     *
     * Kept by its own collection rather than as a side effect of [planner], so a date confirmed just as the screen
     * stops being collected is still converted with the right calendar.
     */
    private var settingsNow: CompassSettings? = null

    init {
        viewModelScope.launch { settings.collect { settingsNow = it } }
    }

    private val planner: Flow<PlannerState> =
        combine(settings, plannedDay, plannedMinute, pickerOpen) { current, day, minute, open ->
            CompassStateMapper.planner(current, clock.now().toJdn(TimeZone.currentSystemDefault()), day, minute, open)
        }

    private val view: Flow<CompassInputs> =
        combine(settings, shownHeading, sky) { current, heading, snapshot -> Triple(current, heading, snapshot) }

    val uiState: StateFlow<CompassUiState> =
        combine(view.withAnnouncements(), frozen, showSunPath, planner) { (triple, announced), isFrozen, path, plan ->
            val (current, heading, snapshot) = triple
            val flags = CompassUiState(frozen = isFrozen, showSunPath = path, planner = plan)
            CompassStateMapper.map(current, heading, snapshot, announced, flags)
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CompassUiState())

    fun onToggleFrozen() {
        frozen.update { !it }
    }

    /** Moves the planned time within the planned day; ignored while live. */
    fun onPlanMinute(minuteOfDay: Int) {
        plannedMinute.value = minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)
    }

    /** Back to live: the sky follows the clock again. The heading was never paused. */
    fun onResumeLive() {
        plannedDay.value = null
        plannedMinute.value = NOON_MINUTE
    }

    fun onOpenDatePicker() {
        pickerOpen.value = true
    }

    fun onDismissDatePicker() {
        pickerOpen.value = false
    }

/** Plans the sky for [day] and closes the picker. The screen picks dates in the user's calendar, via [onDateSelected]. */
    internal fun onDatePicked(day: Jdn) {
        plannedDay.value = day
        pickerOpen.value = false
    }

    /**
     * Confirms the picker's [selection], which is a year, month and day **of the user's calendar**. It is converted
     * with that same calendar, so a Persian user picking 1 Farvardin plans Nowruz rather than 1 January.
     */
    fun onDateSelected(selection: DateSelection) {
        val calendar = settingsNow?.calendar ?: return
        val clamped = selection.day.coerceIn(1, calendar.monthLength(selection.year, selection.month))
        onDatePicked(calendar.toJdn(calendar.date(selection.year, selection.month, clamped)))
    }

    fun onToggleSunPath() {
        showSunPath.update { !it }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Where a planned day starts: midday, so the Sun is up wherever the user is planning for. */
        const val NOON_MINUTE: Int = 12 * 60
        const val MINUTES_PER_DAY: Int = 24 * 60

        /** Weight of a new heading sample: smooth at the sensor rate, still settling within about a quarter second. */
        const val HEADING_ALPHA = 0.15

        fun filteredHeadings(sensors: MotionSensors): Flow<HeadingReading> =
            sensors
                .orientation()
                .scan<OrientationSample, HeadingReading?>(null) { previous, sample ->
                    when (sample) {
                        OrientationSample.Unavailable -> {
                            HeadingReading.Unavailable
                        }

                        is OrientationSample.Reading -> {
                            val azimuth = RotationMath.angles(sample.rotation).azimuthDegrees
                            val last = (previous as? HeadingReading.Measured)?.magneticDegrees
                            HeadingReading.Measured(Angles.lowPass(last, azimuth, HEADING_ALPHA), sample.accuracy)
                        }
                    }
                }.filterNotNull()

        /** Attaches the TalkBack bucket of each true (or magnetic) heading, with hysteresis across emissions. */
        fun Flow<CompassInputs>.withAnnouncements(): Flow<Pair<CompassInputs, Int?>> =
            scan<CompassInputs, Pair<CompassInputs, Int?>>(Triple(null, null, null) to null) { (_, previous), inputs ->
                val heading = inputs.second as? HeadingReading.Measured
                val announced =
                    heading?.let {
                        val degrees = it.magneticDegrees + (inputs.third?.declinationDegrees ?: 0.0)
                        HeadingAnnouncements.next(previous, degrees)
                    } ?: previous
                inputs to announced
            }
    }
}

/** Settings, shown heading and sky snapshot of one emission. */
private typealias CompassInputs = Triple<CompassSettings?, HeadingReading?, SkySnapshot?>

private const val MINUTES_PER_HOUR = 60
private const val MILLIS_PER_MINUTE = 60_000L

/** [minuteOfDay] of this day in [zone], as an instant. */
internal fun Jdn.atMinuteOfDay(
    minuteOfDay: Int,
    zone: TimeZone,
): Instant = toLocalDate().atTime(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR).toInstant(zone)

/** [clock]'s time now and then at every following minute boundary. */
internal fun minuteTicks(clock: Clock): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilliseconds().mod(MILLIS_PER_MINUTE))
        }
    }
