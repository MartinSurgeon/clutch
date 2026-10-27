package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppPrefs
import com.example.data.local.ClutchDatabase
import com.example.data.local.SyncState
import com.example.data.remote.ClutchApi
import com.example.data.repository.RoomClutchRepository
import com.example.data.local.DailyRecordEntity
import com.example.data.local.FlockEntity
import com.example.data.local.WeeklyNoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real end-to-end test: the app's SyncManager talking to the PHP/MySQL backend on this computer.
 * Two "phones" (separate databases and settings) share one account.
 * Skipped automatically when the local server is not running.
 *
 * Server URL: system property clutch.testApiUrl, default http://localhost/clutch/backend/api
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncIntegrationTest {

    private val baseUrl = System.getProperty("clutch.testApiUrl") ?: "http://localhost/clutch/backend/api"
    private lateinit var context: Context
    private val databases = mutableListOf<ClutchDatabase>()

    private class Phone(val db: ClutchDatabase, val prefs: AppPrefs, val sync: SyncManager) {
        val dao get() = db.clutchDao()
        val repo = RoomClutchRepository(db.clutchDao())
    }

    private fun phone(name: String): Phone {
        val db = Room.inMemoryDatabaseBuilder(context, ClutchDatabase::class.java).allowMainThreadQueries().build()
        databases += db
        val prefs = AppPrefs(context, "test_prefs_$name").also { it.clearAccount() }
        val sync = SyncManager(db.clutchDao(), prefs, ClutchApi { baseUrl }, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        return Phone(db, prefs, sync)
    }

    private fun serverIsUp(): Boolean = runCatching {
        val conn = URL("$baseUrl/me.php").openConnection() as HttpURLConnection
        conn.connectTimeout = 2_000
        conn.readTimeout = 2_000
        val code = conn.responseCode
        conn.disconnect()
        code == 401
    }.getOrDefault(false)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        assumeTrue("Local Clutch backend is not running at $baseUrl", serverIsUp())
    }

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    @Test
    fun `records made on one phone restore on another and edits flow both ways`() = runBlocking {
        val phoneNumber = "059" + (1_000_000..9_999_999).random()
        val a = phone("a")
        val b = phone("b")

        // Phone A: records first, signs up later — offline work must still be backed up
        val flock = FlockEntity(id = "pen_it", name = "Pen IT", breed = "ISA Brown", hensHoused = 300, placedDate = "2026-08-01", ageWeeksAtPlacement = 18)
        a.repo.saveFlock(flock)
        a.repo.saveDailyRecord(
            DailyRecordEntity(
                id = DailyRecordEntity.buildId("pen_it", "2026-10-20"), flockId = "pen_it", date = "2026-10-20",
                liveHens = 298, eggs = 270, deaths = 2, feedKg = 34.5, avgEggWeightG = 58.0, bodyWeightG = 1820.0, uniformityPct = 81.0
            )
        )
        a.repo.saveWeeklyNote(WeeklyNoteEntity(id = "x", flockId = "pen_it", ageWeek = 29, notes = "Gumboro booster", actions = "Clean drinkers"))

        a.sync.register(phoneNumber, "Ama Test", "secret12")
        assertTrue(a.sync.status.value.signedIn)
        assertTrue("everything uploaded", a.dao.pendingFlocks().isEmpty() && a.dao.pendingRecords().isEmpty() && a.dao.pendingWeeklyNotes().isEmpty())

        // Phone B: new phone, sign in → everything comes back
        b.sync.login(phoneNumber, "secret12")
        val restored = b.dao.getRecordsForFlockOnce("pen_it")
        assertEquals(1, restored.size)
        assertEquals(270, restored.first().eggs)
        assertEquals(81.0, restored.first().uniformityPct!!, 0.001)
        assertEquals(SyncState.SYNCED, restored.first().syncState)
        assertNotNull(b.dao.getFlockById("pen_it"))
        assertEquals("Gumboro booster", b.dao.anyWeeklyNote(WeeklyNoteEntity.buildId("pen_it", 29))?.notes)

        // B corrects the egg count; A picks it up
        Thread.sleep(5)
        b.repo.saveDailyRecord(restored.first().copy(eggs = 275))
        b.sync.syncNow()
        a.sync.syncNow()
        assertEquals(275, a.dao.getRecordsForFlockOnce("pen_it").first().eggs)

        // A deletes the day; B sees it gone
        Thread.sleep(5)
        a.repo.deleteDailyRecord("pen_it", "2026-10-20")
        a.sync.syncNow()
        b.sync.syncNow()
        assertTrue(b.dao.getRecordsForFlockOnce("pen_it").isEmpty())

        // An unsent newer edit on B is not overwritten by an older server copy
        Thread.sleep(5)
        b.repo.saveFlock(b.dao.getFlockById("pen_it")!!.copy(name = "Pen IT (renamed on B)"))
        a.sync.syncNow() // nothing new from A
        b.sync.syncNow()
        a.sync.syncNow()
        assertEquals("Pen IT (renamed on B)", a.dao.getFlockById("pen_it")?.name)

        // Sign out keeps the records on the phone
        b.sync.signOut()
        assertTrue(!b.sync.status.value.signedIn)
        assertNotNull(b.dao.getFlockById("pen_it"))

        // Delete account (Google Play requirement): server copy gone, phone copy kept
        a.sync.deleteAccount("secret12")
        assertTrue(!a.sync.status.value.signedIn)
        assertNotNull(a.dao.getFlockById("pen_it"))
        val relogin = runCatching { b.sync.login(phoneNumber, "secret12") }
        assertTrue("deleted account cannot sign in", relogin.isFailure)
    }
}
