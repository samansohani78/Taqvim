/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

/** Positive and negative cases for NoGlobalMutableState, the hard-coded text rules and PreferPredictiveBack. */
class StateTextAndBackDetectorsTest : LintDetectorTest() {
    override fun getDetector(): Detector = NoGlobalMutableStateDetector()

    override fun getIssues(): List<Issue> = listOf(NoGlobalMutableStateDetector.ISSUE)

    private fun check(
        detector: Detector,
        issues: List<Issue>,
        vararg files: TestFile,
    ): TestLintResult =
        lint()
            .detector(detector)
            .issues(*issues.toTypedArray())
            .files(*files)
            .allowMissingSdk()
            .run()

    private val textIssues = listOf(HardcodedTextDetector.NON_LATIN_TEXT, HardcodedTextDetector.COMPOSE_TEXT)

    fun testGlobalMutableStateIsReported() {
        check(
            NoGlobalMutableStateDetector(),
            listOf(NoGlobalMutableStateDetector.ISSUE),
            kotlin(
                """
                package test.pkg

                var counter = 0
                val cache = mutableMapOf<String, Int>()

                object Registry {
                    val listeners = ArrayList<String>()
                }

                class Holder {
                    companion object {
                        private var instances = 0
                    }
                }
                """,
            ).indented(),
        ).expectErrorCount(4).expectContains("[NoGlobalMutableState]")
    }

    fun testImmutableGlobalsAndInstanceStateAreClean() {
        check(
            NoGlobalMutableStateDetector(),
            listOf(NoGlobalMutableStateDetector.ISSUE),
            kotlin(
                """
                package test.pkg

                const val MAX = 3
                val names = listOf("a", "b")

                object Defaults {
                    val scales = listOf(1.0f, 2.0f)
                }

                class Counter {
                    private var value = 0
                    private val items = mutableListOf<Int>()

                    fun increment() {
                        value++
                        items += value
                    }
                }
                """,
            ).indented(),
        ).expectClean()
    }

    fun testNonLatinLiteralIsReported() {
        check(
            HardcodedTextDetector(),
            textIssues,
            kotlin(
                """
                package test.pkg

                fun greeting(): String = "سلام"
                fun year(): String = "۱۴۰۵"
                """,
            ).indented(),
        ).expectErrorCount(2).expectContains("[NoHardcodedNonLatinText]")
    }

    fun testLatinDataAndResourcesAreClean() {
        check(
            HardcodedTextDetector(),
            textIssues,
            kotlin(
                """
                package test.pkg

                fun key(): String = "ir.nowruz.1"
                fun format(value: Int): String = "%d-%02d".format(value, value)
                """,
            ).indented(),
        ).expectClean()
    }

    fun testComposeTextLiteralsAreReported() {
        check(
            HardcodedTextDetector(),
            textIssues,
            kotlin(
                """
                package test.pkg

                fun Text(text: String, modifier: Any? = null) = Unit
                fun Icon(image: Any?, contentDescription: String?) = Unit
                fun stringResource(id: Int): String = id.toString()

                fun screen(name: String) {
                    Text("Hello")
                    Text(text = "Settings")
                    Icon(null, contentDescription = "Back")
                    Text(stringResource(1))
                    Text("Hi ${'$'}name")
                    Text("")
                    Icon(null, contentDescription = null)
                }
                """,
            ).indented(),
        ).expectErrorCount(3).expectContains("[HardcodedComposeText]")
    }

    fun testLegacyBackHandlingIsReported() {
        check(
            PreferPredictiveBackDetector(),
            listOf(PreferPredictiveBackDetector.ISSUE),
            kotlin(
                """
                package test.pkg

                open class Activity {
                    open fun onBackPressed() = Unit
                    open fun onKeyDown(code: Int): Boolean = false
                }

                object KeyEvent {
                    const val KEYCODE_BACK = 4
                }

                class MainActivity : Activity() {
                    override fun onBackPressed() {
                        super.onBackPressed()
                    }

                    override fun onKeyDown(code: Int): Boolean = code == KeyEvent.KEYCODE_BACK
                }
                """,
            ).indented(),
        ).expectContains("[PreferPredictiveBack]").expectContains("KEYCODE_BACK").expectContains("onBackPressed()")
    }

    fun testManifestOptOutIsReported() {
        check(
            PreferPredictiveBackDetector(),
            listOf(PreferPredictiveBackDetector.ISSUE),
            manifest(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="test.pkg">
                    <application android:enableOnBackInvokedCallback="false" />
                </manifest>
                """,
            ).indented(),
        ).expectErrorCount(1).expectContains("[PreferPredictiveBack]")
    }

    fun testPredictiveBackFriendlyCodeIsClean() {
        check(
            PreferPredictiveBackDetector(),
            listOf(PreferPredictiveBackDetector.ISSUE),
            kotlin(
                """
                package test.pkg

                class Dispatcher {
                    fun onBackPressed(fromGesture: Boolean) = Unit
                }

                fun navigateBack(dispatcher: Dispatcher) = dispatcher.onBackPressed(fromGesture = true)
                """,
            ).indented(),
            manifest(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="test.pkg">
                    <application android:enableOnBackInvokedCallback="true" />
                </manifest>
                """,
            ).indented(),
        ).expectClean()
    }
}
