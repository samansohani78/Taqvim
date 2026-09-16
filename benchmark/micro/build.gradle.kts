plugins {
    alias(libs.plugins.taqvim.android.library)
}

// T-1801: in-process timings of widget bitmap renders, Glance widget compositions and map masks (plan §9: widget render < 30 ms, map mask
// < 150 ms off-main). Instrumented tests only, run against the non-debuggable release variant; results are written
// in the AndroidX Benchmark JSON format and compared by tools/benchmark/compare_benchmarks.py (ADR-0018 addendum).
android {
    testBuildType = "release"
}

dependencies {
    androidTestImplementation(projects.core.model)
    androidTestImplementation(projects.core.ui)
    androidTestImplementation(projects.feature.map)
    androidTestImplementation(projects.feature.widgets)
    androidTestImplementation(libs.androidx.glance.appwidget)
    // TaqvimGlanceWidget is a KoinComponent; its loader is injected lazily and never read by the benchmark.
    androidTestImplementation(platform(libs.koin.bom))
    androidTestImplementation(libs.koin.core)
    androidTestImplementation(libs.kotlinx.collections.immutable)
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.kotlinx.datetime)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
