package com.zoneanchor.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.zoneanchor.data.alarms.AlarmEntity
import com.zoneanchor.model.ZonedAlarm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertUpdateDeleteEmitsState() = runTest {
        val dao = database.alarmDao()
        val alarm = entity(label = "Tokyo")

        dao.upsert(alarm)
        assertThat(dao.observeAll().first()).containsExactly(alarm)

        val updated = alarm.copy(label = "Tokyo standup", minute = 45)
        dao.upsert(updated)
        assertThat(dao.observeAll().first()).containsExactly(updated)

        dao.deleteById(alarm.id)
        assertThat(dao.observeAll().first()).isEmpty()
    }

    private fun entity(label: String) = AlarmEntity(
        id = ZonedAlarm.FIRST_ID,
        label = label,
        zoneId = "Asia/Tokyo",
        hour = 9,
        minute = 30,
        daysOfWeekMask = ZonedAlarm.ALL_DAYS,
        soundMode = ZonedAlarm.SOUND_DEFAULT,
        vibrate = true,
        enabled = true,
        nextTriggerAtMillis = 123L,
    )
}
