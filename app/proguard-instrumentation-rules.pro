# Extra keep rules for the `benchmark` build type only (T-1200, T-1801).
#
# This variant is `initWith(release)`, so it carries the same R8 output users install, and it is what both the
# macrobenchmarks and `-Ptaqvim.testBuildType=benchmark` run against. Two things have to survive shrinking for an
# instrumentation to attach to it at all:
#
#   - androidx.tracing.Trace, which AndroidJUnitRunner.onCreate calls and which the macrobenchmark library needs to
#     emit its trace sections. The app itself never calls it, so R8 drops it and the runner dies at startup with
#     NoClassDefFoundError before a single test is collected.
#   - The AndroidX test runner's own entry points, which nothing in the app references.
#
# These are not applied to `release`: users' APK keeps shrinking exactly as before.
-keep class androidx.tracing.** { *; }
-dontwarn androidx.tracing.**
-keep class androidx.test.** { *; }
-dontwarn androidx.test.**
