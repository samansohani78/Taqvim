plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // Calendar arithmetic and the kotlinx-datetime bridge appear in the recurrence API (T-503).
    api(projects.core.calendar)
}
