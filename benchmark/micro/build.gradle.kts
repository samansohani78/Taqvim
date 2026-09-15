plugins {
    alias(libs.plugins.taqvim.android.library)
}

// T-1801: in-process timings of widget bitmap renders and map masks (plan §9: widget render < 30 ms, map mask
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
