/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.runtime.Immutable
import ir.taqvim.core.ui.component.CalendarPickerData
import ir.taqvim.core.ui.component.DateSelection
import ir.taqvim.core.workdays.ShiftRotation
import ir.taqvim.core.workdays.ShiftType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The rotation being edited (F-08), or a new one when [id] is `null`. */
@Immutable
data class ShiftRotationDraft(
    val id: Long? = null,
    val name: String = "",
    /** The day the pattern's first entry falls on, and the text that names it in the user's calendar. */
    val anchorJdn: Long,
    val anchorText: String,
    /** The shift types the user has defined, each with its label and colour. */
    val types: ImmutableList<ShiftType> = persistentListOf(),
    /** The repeating sequence, as indices into [types]. */
    val pattern: ImmutableList<Int> = persistentListOf(),
    val isActive: Boolean = true,
    /** The type being named, when the user is adding one. */
    val newTypeLabel: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && pattern.isNotEmpty()

    /** The pattern as shift types, which is what the engine takes. */
    val patternTypes: List<ShiftType> get() = pattern.mapNotNull(types::getOrNull)
}

/** The shift work screen (F-08): the stored rotations and the one being edited. */
@Immutable
data class ShiftRotationUiState(
    val loading: Boolean = true,
    val rotations: ImmutableList<ShiftRotation> = persistentListOf(),
    val editing: ShiftRotationDraft? = null,
    val picker: CalendarPickerData? = null,
)

/** What the shift work screen reports back. */
data class ShiftRotationActions(
    val onNew: () -> Unit = {},
    val onEdit: (ShiftRotation) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onNewTypeLabel: (String) -> Unit = {},
    val onAddType: () -> Unit = {},
    val onRemoveType: (Int) -> Unit = {},
    val onTypeColor: (Int, Int) -> Unit = { _, _ -> },
    val onAppendToPattern: (Int) -> Unit = {},
    val onRemoveLastFromPattern: () -> Unit = {},
    val onPickAnchor: () -> Unit = {},
    val onAnchorPicked: (DateSelection) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
    val onActive: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: (ShiftRotation) -> Unit = {},
)

/** The colours a shift type can be given; the palette is fixed so a rotation reads the same on every theme. */
val SHIFT_COLORS: List<Int> =
    listOf(
        0xFF2E7D32.toInt(),
        0xFF1565C0.toInt(),
        0xFF6A1B9A.toInt(),
        0xFFEF6C00.toInt(),
        0xFFC62828.toInt(),
        0xFF00838F.toInt(),
    )
