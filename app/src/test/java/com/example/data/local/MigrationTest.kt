package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A farmer updating the app must keep every record. This builds a real version-1 database
 * (the schema shipped before body weight, uniformity and weekly notes), opens it with the
 * current Room database, and checks the data survived and the new columns work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    private val dbName = "migration_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createVersion1Database() {
        val helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `flocks` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `breed` TEXT NOT NULL, " +
                        "`hensHoused` INTEGER NOT NULL, `placedDate` TEXT NOT NULL, `ageWeeksAtPlacement` INTEGER NOT NULL, " +
                        "`archived` INTEGER NOT NULL, `isExample` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "`deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_records` (`id` TEXT NOT NULL, `flockId` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                        "`liveHens` INTEGER NOT NULL, `eggs` INTEGER NOT NULL, `cracked` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, " +
                        "`softShell` INTEGER NOT NULL, `deaths` INTEGER NOT NULL, `culls` INTEGER NOT NULL, `feedKg` REAL NOT NULL, " +
                        "`waterL` REAL NOT NULL, `avgEggWeightG` REAL, `maxTempC` REAL, `notes` TEXT NOT NULL, " +
                        "`enteredBy` TEXT NOT NULL, `enteredByName` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "`deleted` INTEGER NOT NULL, `syncState` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_flockId_date` ON `daily_records` (`flockId`, `date`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'version-1')")

                db.insert("flocks", null, ContentValues().apply {
                    put("id", "pen_a"); put("name", "Pen A"); put("breed", "Lohmann Brown")
                    put("hensHoused", 500); put("placedDate", "2026-07-01"); put("ageWeeksAtPlacement", 17)
                    put("archived", 0); put("isExample", 0); put("updatedAt", 1L); put("deleted", 0); put("syncState", "PENDING")
                })
                db.insert("daily_records", null, ContentValues().apply {
                    put("id", "pen_a_2026-10-20"); put("flockId", "pen_a"); put("date", "2026-10-20")
                    put("liveHens", 495); put("eggs", 452); put("cracked", 4); put("dirty", 5); put("softShell", 1)
                    put("deaths", 1); put("culls", 0); put("feedKg", 57.5); put("waterL", 108.0)
                    put("avgEggWeightG", 61.2); putNull("maxTempC"); put("notes", "Vaccinated today")
                    put("enteredBy", "local_farmer"); put("enteredByName", "Farmer"); put("updatedAt", 1L)
                    put("deleted", 0); put("syncState", "PENDING")
                })
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
        helper.close()
    }

    @Test
    fun `migrating from version 1 keeps records and adds new fields`() = runTest {
        createVersion1Database()

        val db = Room.databaseBuilder(context, ClutchDatabase::class.java, dbName)
            .addMigrations(MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        val dao = db.clutchDao()

        val flocks = dao.getAllFlocksOnce()
        assertEquals(1, flocks.size)
        assertEquals("Pen A", flocks.first().name)

        val records = dao.getRecordsForFlockOnce("pen_a")
        assertEquals(1, records.size)
        val old = records.first()
        assertEquals(452, old.eggs)
        assertEquals(61.2, old.avgEggWeightG!!, 0.001)
        assertEquals("Vaccinated today", old.notes)
        assertNull(old.bodyWeightG)
        assertNull(old.uniformityPct)

        dao.upsertRecord(old.copy(bodyWeightG = 1950.0, uniformityPct = 82.5))
        val updated = dao.getRecordsForFlockOnce("pen_a").first()
        assertEquals(1950.0, updated.bodyWeightG!!, 0.001)
        assertEquals(82.5, updated.uniformityPct!!, 0.001)

        dao.upsertWeeklyNote(
            WeeklyNoteEntity(
                id = WeeklyNoteEntity.buildId("pen_a", 32),
                flockId = "pen_a",
                ageWeek = 32,
                notes = "Hot week",
                actions = "Added extra drinkers"
            )
        )
        val notes = dao.observeAllWeeklyNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Added extra drinkers", notes.first().actions)

        db.close()
    }
}
