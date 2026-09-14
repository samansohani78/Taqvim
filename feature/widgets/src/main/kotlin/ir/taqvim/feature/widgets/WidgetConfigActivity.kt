/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.i18n.TextDirection
import ir.taqvim.core.ui.theme.TaqvimTheme
import ir.taqvim.core.ui.theme.ThemeSettings
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Koin bindings of the widget framework and its widgets ([WidgetCatalog]). `:app` provides `Context`,
 * `kotlin.time.Clock`, [WidgetDataSource], [WidgetConfigStore], [WidgetTimelineSource] and [WidgetCalendarsSource],
 * and optionally [WidgetCountdownSource] (without it the countdown widget's date cannot be chosen).
 */
val widgetsFeatureModule: Module =
    module {
        WidgetCatalog.registrations.forEach { registration -> single(named(registration.kind.id)) { registration } }
        single<WidgetPrayerNames> { ResourceWidgetPrayerNames(get()) }
        single { GlanceWidgets(get(), getAll()) } binds
            arrayOf(InstalledWidgets::class, WidgetUpdater::class, WidgetKindResolver::class)
        single<WidgetWakeUpScheduler> { AlarmWidgetWakeUpScheduler(get()) }
        single<WidgetViewStore> { SharedPreferencesWidgetViewStore(get()) }
        single { WidgetRefresher(get(), get(), get(), get(), get(), get(), views = get()) }
        single { WidgetStateLoader(get(), get(), get(), views = get()) }
        viewModel { (appWidgetId: Int, kind: WidgetKind) ->
            WidgetConfigViewModel(appWidgetId, kind, get(), get(), get(), getOrNull())
        }
    }

/**
 * The launcher's widget configuration activity (`APPWIDGET_CONFIGURE`). It answers `RESULT_CANCELED` unless the user
 * saves, and finishes at once for an id that is not one of Taqvim's widgets.
 */
class WidgetConfigActivity :
    ComponentActivity(),
    KoinComponent {
    private val kinds: WidgetKindResolver by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = appWidgetId.takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }?.let { kinds.kindOf(it) }
        if (kind == null) {
            finish()
            return
        }
        setContent {
            val rtl = LocalConfiguration.current.layoutDirection == View.LAYOUT_DIRECTION_RTL
            TaqvimTheme(ThemeSettings(), if (rtl) TextDirection.RTL else TextDirection.LTR) {
                WidgetConfigRoute(
                    appWidgetId = appWidgetId,
                    kind = kind,
                    onDone = {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }
}

/** The configuration screen of widget [appWidgetId] bound to its [WidgetConfigViewModel]. */
@Composable
fun WidgetConfigRoute(
    appWidgetId: Int,
    kind: WidgetKind,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WidgetConfigViewModel =
        koinViewModel(key = "widget-config-$appWidgetId", parameters = { parametersOf(appWidgetId, kind) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val actions =
        remember(viewModel, onCancel) {
            WidgetConfigActions(
                onBackground = viewModel::onBackground,
                onTransparency = viewModel::onTransparency,
                onScale = viewModel::onScale,
                onContent = viewModel::onContent,
                onSecondaryCalendar = viewModel::onSecondaryCalendar,
                onSave = viewModel::onSave,
                onCancel = onCancel,
                countdown =
                    WidgetCountdownActions(
                        onMode = viewModel::onCountdownMode,
                        onTitle = viewModel::onCountdownTitle,
                        onCalendar = viewModel::onCountdownCalendar,
                        onDate = viewModel::onCountdownDate,
                        onRepeats = viewModel::onCountdownRepeats,
                        onOccasion = viewModel::onCountdownOccasion,
                        daysInMonth = viewModel::countdownDaysInMonth,
                    ),
            )
        }
    WidgetConfigScreen(state, actions, modifier)
}
