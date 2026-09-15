/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.core.testing

/**
 * Marks a test that asserts a wall-clock budget (ADR-0018 addendum, T-1801). Such tests are load-sensitive, so the
 * default `test` tasks exclude them and `./gradlew timingTests` runs them alone, one module at a time.
 *
 * JUnit 5 tests use `@Tag(TimingTest.TAG)`. JUnit 4 (Robolectric) tests use `@Category(TimingTest::class)`, which
 * the Vintage engine reports as the tag [CATEGORY_TAG]. The build filters on both names.
 */
public interface TimingTest {
    public companion object {
        /** JUnit Platform tag for JUnit 5 timing tests. */
        public const val TAG: String = "timing"

        /** Tag the Vintage engine derives from `@Category(TimingTest::class)`. */
        public const val CATEGORY_TAG: String = "ir.taqvim.core.testing.TimingTest"
    }
}
