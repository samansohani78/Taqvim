/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ir.taqvim.core.i18n.Numerals
import ir.taqvim.core.ui.component.ScreenSurface
import ir.taqvim.core.ui.component.TopBar

/**
 * The first-run onboarding (T-1501), stateless: renders [state], reports user actions through [actions] and shows
 * [location] (the T-1502 location settings) on the location page.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    location: @Composable (Modifier) -> Unit = {},
) {
    val step = state.step
    val counter =
        stringResource(
            R.string.onboarding_step,
            Numerals.localizeDigits((step.ordinal + 1).toString(), state.numerals),
            Numerals.localizeDigits(OnboardingStep.entries.size.toString(), state.numerals),
        )
    ScreenSurface(
        modifier = modifier,
        topBar = { TopBar(stringResource(step.title), subtitle = counter) },
        bottomBar = { OnboardingButtons(step, actions) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                val description = stringResource(R.string.onboarding_loading)
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center).semantics { contentDescription = description },
                )
            } else {
                OnboardingPage(state, actions, location)
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    state: OnboardingUiState,
    actions: OnboardingActions,
    location: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(state.step.intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val body = Modifier.fillMaxWidth().weight(1f)
        when (state.step) {
            OnboardingStep.LANGUAGE -> LanguageList(state, actions, body)
            OnboardingStep.LOCATION -> location(body)
            OnboardingStep.EVENT_SOURCES -> SourceList(state, actions, body)
        }
    }
}

@Composable
private fun LanguageList(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier,
) {
    LazyColumn(modifier.selectableGroup()) {
        items(state.languages, key = { it.code }) { option ->
            OptionRow(
                text = option.nativeName,
                modifier =
                    Modifier
                        .testTag(OnboardingTags.language(option.code))
                        .selectable(option.selected, role = Role.RadioButton) {
                            actions.onLanguageSelected(option.code)
                        },
            ) { RadioButton(selected = option.selected, onClick = null) }
        }
    }
}

@Composable
private fun SourceList(
    state: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier,
) {
    LazyColumn(modifier) {
        items(state.sources, key = { it.source.name }) { option ->
            OptionRow(
                text = stringResource(option.label),
                modifier =
                    Modifier
                        .testTag(OnboardingTags.source(option.source.name))
                        .toggleable(option.checked, role = Role.Checkbox) { actions.onSourceToggled(option.source) },
            ) { Checkbox(checked = option.checked, onCheckedChange = null) }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    modifier: Modifier,
    control: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        control()
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OnboardingButtons(
    step: OnboardingStep,
    actions: OnboardingActions,
) {
    val last = step == OnboardingStep.entries.last()
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!last) {
            TextButton(onClick = actions.onSkip, modifier = Modifier.testTag(OnboardingTags.SKIP)) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        Box(Modifier.weight(1f))
        if (step != OnboardingStep.entries.first()) {
            OutlinedButton(onClick = actions.onBack, modifier = Modifier.testTag(OnboardingTags.BACK)) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
        Button(onClick = actions.onNext, modifier = Modifier.testTag(OnboardingTags.NEXT)) {
            Text(stringResource(if (last) R.string.onboarding_done else R.string.onboarding_next))
        }
    }
}

@get:StringRes
private val OnboardingStep.title: Int
    get() =
        when (this) {
            OnboardingStep.LANGUAGE -> R.string.onboarding_language_title
            OnboardingStep.LOCATION -> R.string.onboarding_location_title
            OnboardingStep.EVENT_SOURCES -> R.string.onboarding_sources_title
        }

@get:StringRes
private val OnboardingStep.intro: Int
    get() =
        when (this) {
            OnboardingStep.LANGUAGE -> R.string.onboarding_language_intro
            OnboardingStep.LOCATION -> R.string.onboarding_location_intro
            OnboardingStep.EVENT_SOURCES -> R.string.onboarding_sources_intro
        }
