/*
 * Copyright (c) 2026 Saman Sohani. All Rights Reserved.
 * Proprietary and confidential. See the LICENSE file in the repository root.
 */
package ir.taqvim.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** T-601: every exported schema (1…latest) migrates to the latest schema and validates against the entities. */
@RunWith(AndroidJUnit4::class)
class TaqvimDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TaqvimDatabase::class.java)

    @Test
    fun everySchemaMigratesToLatest(): Unit =
        (1..TaqvimMigrations.LATEST_VERSION).forEach { version ->
            val name = "migration-from-$version.db"
            helper.createDatabase(name, version).close()
            helper
                .runMigrationsAndValidate(
                    name,
                    TaqvimMigrations.LATEST_VERSION,
                    true,
                    *TaqvimMigrations.ALL.toTypedArray(),
                ).close()
        }

    @Test
    fun appDatabaseOpensTheOldestSchema() {
        helper.createDatabase(TaqvimDatabase.NAME, 1).close()
        val db = TaqvimDatabase.build(ApplicationProvider.getApplicationContext<Context>())

        runBlocking { db.diagnosticsDao().count() } shouldBe 0
        db.close()
    }
}
