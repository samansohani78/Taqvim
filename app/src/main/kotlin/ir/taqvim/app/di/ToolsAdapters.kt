/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
@file:Suppress("MatchingDeclarationName")

package ir.taqvim.app.di

import ir.taqvim.core.events.EventLookup
import ir.taqvim.core.model.Jdn
import ir.taqvim.core.nlp.AnchorLookup
import ir.taqvim.core.workdays.ShiftRotation
import ir.taqvim.core.workdays.ShiftType
import ir.taqvim.core.workdays.WorkdayCalculator
import ir.taqvim.core.workdays.WorkdayProfile
import ir.taqvim.data.database.ShiftRotationDao
import ir.taqvim.data.database.ShiftRotationEntity
import ir.taqvim.data.database.ShiftRotationRecordEntity
import ir.taqvim.data.database.WorkdayProfileDao
import ir.taqvim.data.database.WorkdayProfileEntity
import ir.taqvim.data.database.toProfile
import ir.taqvim.data.devicecalendar.DeviceTimeZone
import ir.taqvim.data.events.EventsSettings
import ir.taqvim.data.events.OfficialEventView
import ir.taqvim.data.events.SkyAstronomicalEventSource
import ir.taqvim.data.events.generated.OfficialEvents
import ir.taqvim.data.events.toEventsSettings
import ir.taqvim.data.preferences.UserPreferencesRepository
import ir.taqvim.feature.calendar.ShiftScheduleSource
import ir.taqvim.feature.tools.NamedWorkdayProfile
import ir.taqvim.feature.tools.ShiftRotationStore
import ir.taqvim.feature.tools.ToolsSettings
import ir.taqvim.feature.tools.ToolsSettingsSource
import ir.taqvim.feature.tools.WorkdayProfileStore
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [ToolsSettingsSource] (T-1400) from the stored preferences and the default workday profile (T-504): the app language,
 * the device's time zone (re-emitted when it changes, review I06), the user's computable calendars (the tools' defaults when none is) and workday arithmetic
 * over the official events when a default profile exists. The time-zone board starts empty until T-1500 stores it.
 */
internal class PreferencesToolsSettingsSource(
    private val preferences: UserPreferencesRepository,
    private val workdayProfile: Flow<WorkdayProfile?>,
    private val zones: Flow<TimeZone> = DeviceTimeZone.current,
    private val anchors: AnchorLookup? = null,
    /** The dataset view for one settings value; the workday calculator must date events as the calendar does. */
    private val viewFor: (EventsSettings) -> OfficialEventView = ::OfficialEventView,
) : ToolsSettingsSource {
    private val shared = AtomicReference<OfficialEventView?>(null)

    /**
     * The lookup for [settings], with the user's Islamic variant and official month overrides.
     *
     * It used to be one `EventLookup` built from the default calendars and kept for the life of the process, so the
     * workday calculator dated Afghan official events in the computed Iranian calendar rather than the tabular one
     * they are announced in (ADR-0010), ignored the user's official Iranian months (ADR-0037), and never noticed
     * either setting changing.
     */
    private fun lookup(settings: EventsSettings): EventLookup {
        shared.get()?.takeIf { it.settings == settings }?.let { return it.lookup }
        return viewFor(settings).also { shared.set(it) }.lookup
    }

    override fun settings(): Flow<ToolsSettings> =
        combine(
            preferences.preferences,
            workdayProfile.distinctUntilChanged(),
            zones.distinctUntilChanged(),
        ) { preferences, profile, zone ->
            ToolsSettings(
                language = preferences.languageSpec(),
                homeZone = zone,
                calendars = preferences.availableCalendars().ifEmpty { ToolsSettings.DEFAULT_CALENDARS },
                boardZones = preferences.app.timeZoneBoard,
                // Built only when a profile exists: loading the dataset for a user who keeps no workday profile
                // would be the same waste the widgets were making.
                workdays =
                    profile?.let {
                        WorkdayCalculator(lookup(preferences.toEventsSettings(homeTimeZone = zone)), it)
                    },
                anchors = anchors,
            )
        }
}

/** The stored profile marked as default, as a [WorkdayProfile]; `null` when none is. */
internal fun List<WorkdayProfileEntity>.defaultProfile(): WorkdayProfile? = firstOrNull { it.isDefault }?.toProfile()

/**
 * [WorkdayProfileStore] (F-07) over `workday_profiles`.
 *
 * The table, its DAO and its backup mapping have existed since T-504; nothing wrote to them, so `defaultProfile()`
 * was `null` for every user and the workday calculator fell back to the language's weekend. This is the writer.
 */
internal class RoomWorkdayProfileStore(
    private val dao: WorkdayProfileDao,
) : WorkdayProfileStore {
    override fun profiles(): Flow<List<NamedWorkdayProfile>> =
        dao.observeAll().map { stored ->
            stored.map { NamedWorkdayProfile(it.id, it.name, it.toProfile(), it.isDefault) }
        }

    override suspend fun save(
        id: Long?,
        name: String,
        profile: WorkdayProfile,
    ): Long {
        val entity = profile.toEntity(id ?: 0L, name, isDefault = false)
        if (id == null) {
            val newId = dao.insert(entity)
            // The first profile a user makes is the one the calculator should use; asking them to press "make
            // default" immediately afterwards would be a step with only one possible answer.
            if (dao.observeAll().first().size == 1) dao.makeDefault(newId)
            return newId
        }
        val wasDefault =
            dao
                .observeAll()
                .first()
                .firstOrNull { it.id == id }
                ?.isDefault ?: false
        dao.update(entity.copy(isDefault = wasDefault))
        return id
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun makeDefault(id: Long) = dao.makeDefault(id)
}

/** The profile as a row of `workday_profiles`. */
private fun WorkdayProfile.toEntity(
    id: Long,
    name: String,
    isDefault: Boolean,
): WorkdayProfileEntity =
    WorkdayProfileEntity(
        id = id,
        name = name,
        weekend = weekend,
        holidaySources = holidaySources,
        halfDays = halfDays,
        personalLeave = personalLeave,
        isDefault = isDefault,
    )

/**
 * [ShiftRotationStore] (F-08) over `shift_rotations` and `shift_rotation_records`.
 *
 * Both tables and their DAO shipped with T-504 and nothing wrote to them; the calendar menu's "shift work" item
 * opened a notice saying the screen did not exist. This is the writer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class RoomShiftRotationStore(
    private val dao: ShiftRotationDao,
) : ShiftRotationStore {
    override fun rotations(): Flow<List<ShiftRotation>> =
        dao.observeRotations().flatMapLatest { stored ->
            if (stored.isEmpty()) {
                flowOf(emptyList())
            } else {
                dao.observeAllRecords().map { records ->
                    val byRotation = records.groupBy { it.rotationId }
                    stored.map { rotation ->
                        rotation.toRotation(byRotation[rotation.id].orEmpty())
                    }
                }
            }
        }

    override suspend fun save(rotation: ShiftRotation): Long {
        val entity = rotation.toEntity()
        if (rotation.id == 0L) return dao.insertRotation(entity)
        dao.updateRotation(entity)
        return rotation.id
    }

    override suspend fun delete(id: Long) = dao.deleteRotation(id)

    override suspend fun setException(
        rotationId: Long,
        day: Jdn,
        shift: String?,
    ) {
        if (shift == null) {
            dao.deleteRecord(rotationId, day.value)
        } else {
            dao.upsertRecord(ShiftRotationRecordEntity(rotationId, day.value, shift))
        }
    }
}

/** The stored rotation with its day exceptions, as the engine's [ShiftRotation]. */
internal fun ShiftRotationEntity.toRotation(records: List<ShiftRotationRecordEntity>): ShiftRotation =
    ShiftRotation(
        id = id,
        name = name,
        anchor = Jdn(anchorJdn),
        pattern = pattern.map { ShiftType(it, shiftColors[it]) },
        isActive = isActive,
        exceptions = records.associate { it.jdn to it.shift },
    )

/** The rotation as a row of `shift_rotations`; the colours are keyed by label, so each type keeps its own. */
private fun ShiftRotation.toEntity(): ShiftRotationEntity =
    ShiftRotationEntity(
        id = id,
        name = name,
        anchorJdn = anchor.value,
        pattern = pattern.map { it.label },
        isActive = isActive,
        shiftColors = types.mapNotNull { type -> type.color?.let { type.label to it } }.toMap(),
    )

/** [ShiftScheduleSource] for the calendar (F-08): the same rotations the editor writes. */
internal class StoreShiftScheduleSource(
    private val store: ShiftRotationStore,
) : ShiftScheduleSource {
    override fun rotations(): Flow<List<ShiftRotation>> = store.rotations()

    override suspend fun setException(
        rotationId: Long,
        day: Jdn,
        shift: String?,
    ) = store.setException(rotationId, day, shift)
}
