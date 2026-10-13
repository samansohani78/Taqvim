# Keep rules for the instrumented-test APK when it is built against a minified variant
# (`-Ptaqvim.testBuildType=benchmark`, T-1200).
#
# Running the device suite against the R8 output is the only way to catch a widget that composes in debug and not in
# the build users install. The test APK is minified alongside it, and the test-only libraries below reference classes
# that are not on its classpath: an AppCompat drawable the Material shadow compat layer names, AutoValue annotations
# used by the accessibility checks, and a javax.lang.model annotation from ErrorProne. None is reachable at runtime,
# so warning about them only blocks the build.
-dontwarn androidx.appcompat.graphics.drawable.DrawableWrapper
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue
-dontwarn javax.lang.model.element.Modifier

# R8 would otherwise strip the tests themselves: nothing in the APK references a @Test method, so the whole
# instrumented suite becomes unreachable code and the run reports "0 tests" while the build still succeeds.
# That false green is the same class of mistake this switch exists to catch, so the tests are kept explicitly.
-keep class ir.taqvim.app.device.** { *; }
-keep class ir.taqvim.app.** extends androidx.test.runner.AndroidJUnitRunner { *; }
-keepclasseswithmembers class * { @org.junit.Test <methods>; }
-keepclasseswithmembers class * { @org.junit.Before <methods>; }
-keepclasseswithmembers class * { @org.junit.After <methods>; }
-keepclasseswithmembers class * { @org.junit.runner.RunWith <methods>; }
-keep @org.junit.runner.RunWith class *

# Without this the @Test annotations themselves are stripped, JUnit finds nothing to run, and the device job
# reports "0 tests" and a green build — a false pass that looks exactly like a real one.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod
