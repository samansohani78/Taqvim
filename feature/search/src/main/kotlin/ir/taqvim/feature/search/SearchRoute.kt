/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.taqvim.core.model.Jdn
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Koin bindings of the search screen (ADR-0002). `:app` provides [SearchSettingsSource], [SearchEventSource],
 * [RecentQueriesStore] (e.g. [SessionRecentQueriesStore]) and `TodayProvider`. An optional [String] parameter is the
 * text searched when the screen opens.
 */
val searchFeatureModule: Module =
    module {
        factory<SearchTodaySource> { TickingSearchTodaySource(get()) }
        factory<SearchCatalogSource> { ResourceSearchCatalog(androidContext().resources) }
        viewModel { parameters ->
            SearchViewModel(get(), get(), get(), get(), get(), Dispatchers.Default, parameters.getOrNull<String>())
        }
    }

/** Where results lead; the app's navigation provides these. */
@Immutable
class SearchNavigation(
    val onOpenDay: (day: Jdn) -> Unit = {},
    val onOpenEvent: (kind: SearchEventKind, id: String, day: Jdn?) -> Unit = { _, _, _ -> },
    val onOpenSettings: (entry: SettingsEntry) -> Unit = {},
    val onOpenTool: (entry: ToolEntry) -> Unit = {},
) {
    /** Calls the callback for [target]. */
    fun navigate(target: SearchTarget) {
        when (target) {
            is SearchTarget.Day -> onOpenDay(target.jdn)
            is SearchTarget.Event -> onOpenEvent(target.kind, target.id, target.day)
            is SearchTarget.Settings -> onOpenSettings(target.entry)
            is SearchTarget.Tool -> onOpenTool(target.entry)
        }
    }
}

/** The search screen bound to its [SearchViewModel]; [initialQuery] is searched when the screen opens. */
@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    navigation: SearchNavigation = SearchNavigation(),
    initialQuery: String? = null,
    viewModel: SearchViewModel = koinViewModel(parameters = { parametersOf(initialQuery) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNavigation by rememberUpdatedState(navigation)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.Navigate -> currentNavigation.navigate(effect.target)
            }
        }
    }
    SearchScreen(state, viewModel::onAction, modifier)
}
