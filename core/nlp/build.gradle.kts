plugins {
    alias(libs.plugins.taqvim.jvm.library)
}

dependencies {
    api(projects.core.model)
    // CalendarArithmetic and LanguageSpec appear in the public parser API (ParseContext).
    api(projects.core.calendar)
    api(projects.core.i18n)
}
