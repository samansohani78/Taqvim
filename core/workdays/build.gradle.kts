plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // EventLookup/EventSource appear in the public API; holidays come from HolidayCalendar (T-303).
    api(projects.core.events)
    implementation(projects.core.calendar)
}
