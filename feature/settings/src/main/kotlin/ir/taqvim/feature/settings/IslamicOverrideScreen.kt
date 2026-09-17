/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.LanguageSpec
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.i18n.monthName
import ir.taqvim.core.model.CalendarDate
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/**
 * Official Islamic dates (ADR-0037), stateless: every Islamic date is computed unless the user switches on the bundled
 * official Iranian dates or imports a file. [onPickFile] opens the system file picker; the route reports the choice.
 */
@Composable
fun IslamicOverrideScreen(
    state: IslamicOverrideUiState,
    actions: IslamicOverrideActions,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(R.string.settings_islamic_override_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val status = state.status
            if (status == null) {
                val description = stringResource(R.string.settings_islamic_override_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                IslamicOverrideContent(state, status, actions, onPickFile)
            }
        }
    }
}

@Composable
private fun IslamicOverrideContent(
    state: IslamicOverrideUiState,
    status: IslamicOverrideStatus,
    actions: IslamicOverrideActions,
    onPickFile: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_islamic_override_intro), style = MaterialTheme.typography.bodyMedium)
        Text(
            statusText(status),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        noticeText(status)?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        SwitchItem(
            title = stringResource(R.string.settings_islamic_override_official),
            detail = stringResource(R.string.settings_islamic_override_official_detail),
            checked = status.kind == IslamicOverrideKind.OFFICIAL,
            onChange = actions.onOfficialChanged,
            enabled = !state.busy,
        )
        Button(onClick = onPickFile, enabled = !state.busy) {
            Text(stringResource(R.string.settings_islamic_override_import))
        }
        if (status.kind != IslamicOverrideKind.NONE) {
            TextButton(onClick = actions.onRemove, enabled = !state.busy) {
                Text(stringResource(R.string.settings_islamic_override_remove))
            }
        }
        OutcomeText(state)
    }
}

@Composable
private fun OutcomeText(state: IslamicOverrideUiState) {
    val result = state.importResult
    val text =
        when {
            state.changeFailed -> R.string.settings_islamic_override_change_failed
            result != null -> importText(result)
            else -> return
        }
    val isProblem = state.changeFailed || result != OverrideImportResult.IMPORTED
    Text(
        stringResource(text),
        color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun statusText(status: IslamicOverrideStatus): String {
    val first = status.firstMonth
    val last = status.lastMonth
    return when {
        status.kind == IslamicOverrideKind.NONE || status.broken || first == null || last == null -> {
            stringResource(R.string.settings_islamic_override_status_computed)
        }

        else -> {
            val label =
                if (status.kind == IslamicOverrideKind.OFFICIAL) {
                    R.string.settings_islamic_override_status_official
                } else {
                    R.string.settings_islamic_override_status_imported
                }
            stringResource(label, monthText(first, status.language), monthText(last, status.language))
        }
    }
}

@StringRes
private fun noticeText(status: IslamicOverrideStatus): Int? =
    when {
        status.broken -> R.string.settings_islamic_override_broken
        status.ended -> R.string.settings_islamic_override_ended
        else -> null
    }

/** "Ramadan 1446" in the language's month names and digits; the month number when the language has no name. */
internal fun monthText(
    date: CalendarDate,
    language: LanguageSpec,
): String {
    val year = Numerals.format(date.year.toLong(), language.numerals)
    val month = language.monthName(date) ?: Numerals.format(date.month.toLong(), language.numerals)
    return "$month $year"
}

@StringRes
internal fun importText(result: OverrideImportResult): Int =
    when (result) {
        OverrideImportResult.IMPORTED -> R.string.settings_islamic_override_imported

        OverrideImportResult.UNREADABLE -> R.string.settings_islamic_override_error_unreadable

        OverrideImportResult.TOO_LARGE -> R.string.settings_islamic_override_error_too_large

        OverrideImportResult.NOT_JSON,
        OverrideImportResult.UNSUPPORTED_VERSION,
        -> R.string.settings_islamic_override_error_format

        OverrideImportResult.TOO_FEW_MONTHS,
        OverrideImportResult.INVALID_MONTH,
        OverrideImportResult.INVALID_LENGTH,
        -> R.string.settings_islamic_override_error_months

        OverrideImportResult.MISSING_CITATION -> R.string.settings_islamic_override_error_citation
    }
