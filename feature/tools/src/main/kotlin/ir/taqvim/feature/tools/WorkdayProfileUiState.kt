/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.runtime.Immutable
import ir.taqvim.core.events.EventSource
import ir.taqvim.core.model.Weekday
import ir.taqvim.core.ui.component.CalendarPickerData
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.HalfDayPolicy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** One stretch of personal leave, with its days already formatted in the user's calendar. */
@Immutable
data class LeaveRow(
    val firstJdn: Long,
    val lastJdn: Long,
    val text: String,
)

/** The profile being edited (F-07), or a new one when [id] is `null`. */
@Immutable
data class WorkdayProfileDraft(
    val id: Long? = null,
    val name: String = "",
    val weekend: Set<Weekday> = emptySet(),
    val holidaySources: Set<EventSource> = emptySet(),
    val halfDays: HalfDayPolicy = HalfDayPolicy.HALF,
    val leave: ImmutableList<LeaveRow> = persistentListOf(),
    val isDefault: Boolean = false,
) {
    /** A draft is saveable once it has a name: everything else has a sensible empty value. */
    val canSave: Boolean get() = name.isNotBlank()
}

/** Which day of a leave range the open picker is choosing. */
enum class LeaveBound {
    FIRST,
    LAST,
}

/** The workday profiles screen (F-07): the stored profiles, the one being edited and the open date picker. */
@Immutable
data class WorkdayProfileUiState(
    val loading: Boolean = true,
    val profiles: ImmutableList<NamedWorkdayProfile> = persistentListOf(),
    val names: ImmutableList<String> = persistentListOf(),
    val editing: WorkdayProfileDraft? = null,
    val picker: CalendarPickerData? = null,
    val pickingLeave: LeaveBound? = null,
    /** The first day already chosen while the second is being picked. */
    val pendingLeaveFirst: Long? = null,
)

/** What the workday profiles screen reports back. */
data class WorkdayProfileActions(
    val onNew: () -> Unit = {},
    val onEdit: (NamedWorkdayProfile) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onWeekend: (Weekday, Boolean) -> Unit = { _, _ -> },
    val onHolidaySource: (EventSource, Boolean) -> Unit = { _, _ -> },
    val onHalfDays: (HalfDayPolicy) -> Unit = {},
    val onAddLeave: () -> Unit = {},
    val onRemoveLeave: (LeaveRow) -> Unit = {},
    val onLeaveDatePicked: (DateSelection) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (NamedWorkdayProfile) -> Unit = {},
    val onMakeDefault: (NamedWorkdayProfile) -> Unit = {},
)
