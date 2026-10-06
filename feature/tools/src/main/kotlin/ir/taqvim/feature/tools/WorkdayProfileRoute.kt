/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/** The workday profiles screen bound to its [WorkdayProfileViewModel] (F-07). */
@Composable
fun WorkdayProfileRoute(
    modifier: Modifier = Modifier,
    viewModel: WorkdayProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            WorkdayProfileActions(
                onNew = viewModel::onNew,
                onEdit = viewModel::onEdit,
                onCancel = viewModel::onCancel,
                onName = viewModel::onName,
                onWeekend = viewModel::onWeekend,
                onHolidaySource = viewModel::onHolidaySource,
                onHalfDays = viewModel::onHalfDays,
                onAddLeave = viewModel::onAddLeave,
                onRemoveLeave = viewModel::onRemoveLeave,
                onLeaveDatePicked = viewModel::onLeaveDatePicked,
                onDismissPicker = viewModel::onDismissPicker,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onMakeDefault = viewModel::onMakeDefault,
            )
        }
    WorkdayProfileScreen(state, actions, modifier)
}
