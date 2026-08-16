package com.zenhold.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenhold.app.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migration3To4_preservesAttempts_andCreatesSessionTable() = runBlocking {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO breath_hold_records " +
                    "(id, holdDurationMillis, recoveryDurationMillis, timestamp, sessionId, attemptNumber, comfortRating, energyLevel, stressLevel, sessionNote) " +
                    "VALUES (1, 42000, 60000, 1000, 'legacy', 1, 2, 3, 2, 'test')",
            )
            close()
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build()
        val records = database.recordDao().getAll()
        assertEquals(1, records.size)
        assertEquals(42_000L, records.single().holdDurationMillis)
        assertNotNull(database.openHelper.writableDatabase.query("SELECT * FROM training_sessions").use { it.columnNames })
        database.close()
    }

    private companion object { const val TEST_DB = "migration-3-4-test" }
}
