package com.rupeshbhurke.househunter.core.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Opens a database written by the very first schema with the current code, keeping its data. */
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun version1Data_survivesMigrationToTheLatestSchema() = runBlocking {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO collections (id, name, templateId, extraFieldDefsJson, createdAt) " +
                    "VALUES (1, 'Pune', 'house_hunt', '[]', 1)",
            )
            execSQL(
                "INSERT INTO entries (id, collectionId, title, status, createdAt, updatedAt) " +
                    "VALUES (1, 1, 'Flat 1', 'SHORTLISTED', 1, 1)",
            )
            execSQL(
                "INSERT INTO field_values (entryId, fieldKey, numValue) VALUES (1, 'price', 25000.0)",
            )
            close()
        }

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            dbName,
        ).openHelperFactory(FrameworkSQLiteOpenHelperFactory()).build()
        try {
            val entry = db.entryDao().get(1)!!
            assertEquals("Flat 1", entry.title)
            assertEquals(1, db.collectionDao().get(1)!!.id.toInt())
        } finally {
            db.close()
        }
    }
}
