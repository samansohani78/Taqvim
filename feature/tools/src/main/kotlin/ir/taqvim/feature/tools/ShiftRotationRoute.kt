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

/** The shift work screen bound to its [ShiftRotationViewModel] (F-08). */
@Composable
fun ShiftRotationRoute(
    modifier: Modifier = Modifier,
    viewModel: ShiftRotationViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions =
        remember(viewModel) {
            ShiftRotationActions(
                onNew = viewModel::onNew,
                onEdit = viewModel::onEdit,
                onCancel = viewModel::onCancel,
                onName = viewModel::onName,
                onNewTypeLabel = viewModel::onNewTypeLabel,
                onAddType = viewModel::onAddType,
                onRemoveType = viewModel::onRemoveType,
                onTypeColor = viewModel::onTypeColor,
                onAppendToPattern = viewModel::onAppendToPattern,
                onRemoveLastFromPattern = viewModel::onRemoveLastFromPattern,
                onPickAnchor = viewModel::onPickAnchor,
                onAnchorPicked = viewModel::onAnchorPicked,
                onDismissPicker = viewModel::onDismissPicker,
                onActive = viewModel::onActive,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
            )
        }
    ShiftRotationScreen(state, actions, modifier)
}
